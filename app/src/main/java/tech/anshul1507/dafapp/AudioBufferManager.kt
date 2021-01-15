package tech.anshul1507.dafapp

import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.IOException
import java.util.*


class AudioBufferManager(delay_time: Int) : Thread() {

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack
    private var SAMPLE_RATE = 0
    private var buffersize = 0
    private var started = true
    private var delay: Double = delay_time.toDouble()
    private var validSampleRates = intArrayOf(8000, 11025, 16000, 22050, 44100)

    init {
        // Prepare the AudioRecord & AudioTrack
        try {
            //Find the best supported sample rate
            for (_rate in validSampleRates) {  // add the rates you wish to check against
                val _bufferSize = AudioRecord.getMinBufferSize(
                    _rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (_bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                    // buffer size is valid, Sample _rate supported
                    SAMPLE_RATE = _rate
                    buffersize = _bufferSize
                    Log.i(
                        "LOG_TAG",
                        "Recording sample _rate: $SAMPLE_RATE with buffer size: $buffersize"
                    )
                }
            }

            val _audioTrackSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            Log.i("LOG_TAG", "Final sample rate: $SAMPLE_RATE with buffer size: $buffersize")
            Log.i("LOG_TAG", "Initializing Audio Record and Audio Playing objects")
            Log.i("LOG_TAG", "Delay time is: $delay ms")

            //Set up the recorder and the player
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffersize * 1
            )

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

                AudioTrack(
                    attributes,
                    audioFormat,
                    _audioTrackSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
            } else {
                //support for Android KitKat and lower
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    _audioTrackSize,
                    AudioTrack.MODE_STREAM
                )
            }

            MainActivity.AudioSessionID = audioTrack.audioSessionId

            if (AcousticEchoCanceler.isAvailable()) {
                //To cancel the echo, API > +26 supported this Canceler
                val echoCanceler = AcousticEchoCanceler.create(MainActivity.AudioSessionID)
                echoCanceler.enabled = true
            }
            audioTrack.audioSessionId
            audioTrack.playbackRate = SAMPLE_RATE
        } catch (t: Throwable) {
            Log.e(
                "LOG_TAG",
                "Initializing Audio Record and Play objects Failed ${t.localizedMessage}"
            )
        }
    }


    override fun run() {
        //Execution body of thread
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val buffer = ByteArray(buffersize)
        val circularByteBuffer = CircularByteBuffer(SAMPLE_RATE * 10) //extra spacing for edges
        //Add an offset to the circular buffer
        var emptySamples = (SAMPLE_RATE * (delay / 1000)).toInt() //ms to secs

        if (emptySamples % 2 != 0) {
            //odd emptySamples value produce weird noise, make it even :)
            emptySamples++
        }
        Log.i("LOG_TAG", "Empty Sample: $emptySamples")

        val emptyBuf = ByteArray(emptySamples)
        Arrays.fill(emptyBuf, Byte.MIN_VALUE)
        try {
            circularByteBuffer.outputStream.write(emptyBuf, 0, emptySamples)
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        audioRecord.startRecording()
        audioTrack.play()

        while (started) {
            // start recording and playing back
            try {
                val minSpace = audioRecord.read(buffer, 0, buffersize)
                //Read the byte array data to the circular buffer
                circularByteBuffer.outputStream.write(buffer, 0, minSpace)
                //Read the beginning of the circular buffer to the normal byte array until one sample rate of content
                circularByteBuffer.inputStream.read(buffer, 0, minSpace)
                //Play the byte array content
                audioTrack.write(buffer, 0, minSpace)
            } catch (e: Exception) {
                e.printStackTrace()
                started = false
            } finally {
                //If the user has clicked stop
                if (interrupted()) {
                    started = false
                }
            }
        }

        //Clearing out the bases :)
        audioRecord.stop()
        audioRecord.release()
        audioTrack.stop()
        Log.i("LOG_TAG", "Stage is yours")
        return
    }

    fun close() {
        started = false
        audioRecord.release()
    }
}