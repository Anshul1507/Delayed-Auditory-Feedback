package tech.anshul1507.dafapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import tech.anshul1507.dafapp.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.floor


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_ASK_PERMISSIONS = 101

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack
    private lateinit var audioData: ShortArray

    private var delayInSeconds = 0.0
    private var isActive = false

    private lateinit var am: AudioManager

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
        * Initial Permissions
        * */
        requestPermission(Manifest.permission.RECORD_AUDIO)
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

        am = this.getSystemService(AUDIO_SERVICE) as AudioManager
        /*
        * UI OnClickListeners
        * */
        binding.buttonStart.setOnClickListener {
            startButtonFun()
        }

        binding.buttonStop.setOnClickListener {
            stopButtonFun()
        }

        binding.seekbarDelay.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                (seekBar.progress * 0.25).also { delayInSeconds = it }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                (progress * 0.25).also { binding.textDelay.text = "Delay: ${it}s" }
            }
        }
        )

    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun startButtonFun() {
        if (isActive) {
            //change delay while Active
            stopButtonFun()
        }

        isActive = true
        binding.textStatus.text = "Status: Active with ${delayInSeconds}s Delay"

        Thread { recordAndPlay() }.start()

    }

    private fun stopButtonFun() {
        isActive = false
        binding.textStatus.text = "Status: Inactive"

        audioTrack.stop()
        audioRecord.stop()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun recordAndPlay() {

        //handle delay values which is not supported as buffer sizes
        formatDelayValuesForValidBufferSize()

        /*
        * Code to get device's sample rate and frames per burst
        * Frames per burst -> No. of frames operated in 1 ms
        *  */
        var text: String = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val framesPerBurst = text.toInt()
        text = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val sampleRate = text.toInt()

//        Log.e("test", "sample rate" + sampleRate + "frames per burst " + framesPerBurst)

        var sampleCommonRate = sampleRate //8 KHz [standard minimal sample rate for audio/video]
        var recordBufferSize = floor(delayInSeconds / 1000 * sampleCommonRate).toInt()
        audioData = shortArrayOf(framesPerBurst.toShort())

        var playBufferSize = recordBufferSize

        if (delayInSeconds.equals(0.0)) {
            //no delay, back to 44.1 KHz where no latency occurs
            sampleCommonRate = 44100
        }
//        else {
//            ((playBufferSize * delayInSeconds).toInt()).also { playBufferSize = it }
//        }

//        var min = AudioRecord.getMinBufferSize(
//            sampleCommonRate,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//
//        var maxJitter = AudioTrack.getMinBufferSize(
//            (sampleCommonRate*delayInSeconds).toInt(),
//            AudioFormat.CHANNEL_OUT_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )

        //set up recording audio settings
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleCommonRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufferSize
        )

        //For cancelling the echo
        if (AcousticEchoCanceler.isAvailable()) {
            val echoCanceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)
            echoCanceler.enabled = true
        }

        //set up audio track/play settings
        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleCommonRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

            AudioTrack(
                attributes,
                audioFormat,
                recordBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } else {
            //support for Android KitKat and lower
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleCommonRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioRecord.startRecording()
        if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.play()
        }

        //Read from recording and setup track to play
        while (isActive) {
            val x = audioRecord.read(audioData, 0, audioData.size)
            audioTrack.write(audioData, 0, x)
        }

        //To release audio tracks and records to reduce buffer related problems
//        audioTrack.stop();
//        audioTrack.release();
//        audioRecord.stop();
//        audioRecord.release();

    }

    //Function return valid buffer size delays
    private fun formatDelayValuesForValidBufferSize() {
        /* {Delay Values} -> {Converted Delay Values}
         {0/1/2/3}.25 -> {0/1/2/3}.2
         {0/1/2/3}.50 -> {0/1/2/3}.45
         {0/1/2/3}.75 -> {0/1/2/3}.65
        */
        var decimalValue = delayInSeconds - delayInSeconds.toInt()

        when (decimalValue) {
            0.25 -> {
                decimalValue = 0.2
            }
            0.5 -> {
                decimalValue = 0.45
            }
            0.75 -> {
                decimalValue = 0.65
            }
        }
        (delayInSeconds.toInt() + decimalValue).also { delayInSeconds = it }
    }

    private fun requestPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_ASK_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                Toast.makeText(this@MainActivity, "Enjoy our app", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // Permission Denied => Kill App
                Toast.makeText(
                    this@MainActivity,
                    "Grant us Permissions",
                    Toast.LENGTH_SHORT
                )
                    .show()

                //Timer for making above toast visible to user and then kill the app.
                Timer("Permission Denied", false).schedule(
                    500
                ) {
                    if (grantResults.isNotEmpty())
                        finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}