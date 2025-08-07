package com.example.dreamtracker.feature.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        stop()
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".m4a"
        val outDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val out = File(outDir, fileName)
        outputFile = out

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioSamplingRate(44100)
        rec.setAudioEncodingBitRate(128_000)
        rec.setOutputFile(out.absolutePath)
        rec.prepare()
        rec.start()
        recorder = rec
        return out
    }

    fun stop(): File? {
        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            val file = outputFile
            recorder = null
            outputFile = null
            file
        } catch (_: Exception) {
            recorder = null
            outputFile = null
            null
        }
    }

    fun currentOutputFile(): File? = outputFile
}