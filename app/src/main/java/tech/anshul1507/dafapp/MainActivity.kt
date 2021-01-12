package tech.anshul1507.dafapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import tech.anshul1507.dafapp.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_ASK_PERMISSIONS = 101

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack
    private lateinit var audioData: ShortArray

    private var delayInSeconds = 0.0
    private var isActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
        * Initial Permissions
        * */
        requestPermission(Manifest.permission.RECORD_AUDIO)
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

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

    private fun recordAndPlay() {
        audioData = shortArrayOf(1024)

        val sampleCommonRate = 8000 //48 KHz [standard sample rate for audio/video]
        val recordBufferSize = 22050 //in bytes

        //handle delay values which is not supported as buffer sizes
        formatDelayValuesForValidBufferSize()

        val playBufferSize = ((22050) * delayInSeconds).toInt() //1 sec delay
        //todo:: handle 0 value case

        //set up recording audio settings
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleCommonRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufferSize
        )

        //set up audio track/play settings
        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleCommonRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

            AudioTrack(
                attributes,
                audioFormat,
                playBufferSize,
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
                playBufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioRecord.startRecording()
        if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.play()
        }

        //Read from recording and setup track to play
        while (isActive) {
            val sz = audioRecord.read(audioData, 0, audioData.size)
            audioTrack.write(audioData, 0, sz)
        }

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