package com.example.dreamtracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportImportManager(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val dreamDao = db.dreamDao()
    private val moshi = Moshi.Builder().build()

    suspend fun exportToFile(): File {
        val dreams = dreamDao.getAll()
        val type = Types.newParameterizedType(List::class.java, Dream::class.java)
        val adapter = moshi.adapter<List<Dream>>(type)
        val json = adapter.toJson(dreams)
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val name = "dreams_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".json"
        val file = File(dir, name)
        file.sink().buffer().use { it.writeUtf8(json) }
        return file
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться снами"))
    }

    suspend fun importFromFile(file: File): Int {
        val type = Types.newParameterizedType(List::class.java, Dream::class.java)
        val adapter = moshi.adapter<List<Dream>>(type)
        val json = file.source().buffer().use { it.readUtf8() }
        val list = adapter.fromJson(json).orEmpty()
        var count = 0
        for (d in list) {
            // Сохраняем как новые записи (id игнорируем)
            val copy = d.copy(id = 0)
            dreamDao.upsert(copy)
            count++
        }
        return count
    }
}