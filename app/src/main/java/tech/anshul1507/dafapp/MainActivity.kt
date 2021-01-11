package tech.anshul1507.dafapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
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

    }

    private fun startButtonFun() {
        if (isActive) {
            //change delay while Active
            stopButtonFun()
        }

        if (binding.edittextDelay.text.isNotEmpty()) {
            isActive = true
            binding.textStatus.text = "Status: Active"

            delayInSeconds = binding.edittextDelay.text.toString().toDouble()
            binding.textDelay.text = "Delay: ${delayInSeconds.toInt()}s"
            binding.edittextDelay.text.clear()

            Thread { recordAndPlay() }.start()
        } else {
            Toast.makeText(applicationContext, "Please enter some delay", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopButtonFun() {
        isActive = false
        binding.textStatus.text = "Status: Inactive"
        binding.textDelay.text = "Delay: 0s"

        audioTrack.stop()
        audioRecord.stop()
    }

    private fun recordAndPlay() {
        audioData = shortArrayOf(1024)

        val sampleCommonRate = 8000 //48 KHz [standard sample rate for audio/video]
        val recordBufferSize = 22050 //in bytes
        val playBufferSize = ((22050) * delayInSeconds).toInt() //1 sec delay

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
                Toast.makeText(this@MainActivity, "Grant us Permissions", Toast.LENGTH_SHORT)
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