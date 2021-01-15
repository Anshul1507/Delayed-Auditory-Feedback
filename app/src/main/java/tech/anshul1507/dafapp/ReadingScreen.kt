package tech.anshul1507.dafapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tech.anshul1507.dafapp.databinding.ActivityReadingScreenBinding

class ReadingScreen : AppCompatActivity() {

    private lateinit var binding: ActivityReadingScreenBinding
    private lateinit var audioSource: AudioBufferManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val delay = (intent.extras?.get("delay").toString()
            .toDouble() * 2000).toInt()  //convert to ms [2000 => 2 channels]

        binding.buttonStopBack.setOnClickListener {
            stopBackFun()
        }
        binding.textStatus.text = "Status: Active with ${(delay / 2000.0)}s Delay"

        audioSource = AudioBufferManager(delay)
        audioSource.start()

    }

    private fun stopBackFun() {
        audioSource.interrupt()
        finish()
    }

    override fun onBackPressed() {
        //Stop audio service when user press home back button
        super.onBackPressed()
        audioSource.interrupt()
    }
}