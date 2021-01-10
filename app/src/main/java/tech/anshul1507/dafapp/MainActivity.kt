package tech.anshul1507.dafapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.AudioTrack
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
        isActive = true
        binding.textStatus.text = "Status: Active"

        Thread { recordAndPlay() }.start()
    }

    private fun stopButtonFun() {
        isActive = false
        binding.textStatus.text = "Status: Inactive"
        audioTrack.stop()
        audioRecord.stop()
    }

    private fun recordAndPlay() {

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