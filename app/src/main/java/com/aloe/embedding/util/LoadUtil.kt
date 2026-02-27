package com.aloe.embedding.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object LoadUtil {
    fun copyFromAssetsToFilesDir(context: Context, fileName: String): String {
        val outputFile = File(context.filesDir, fileName)
        if (outputFile.exists()) return outputFile.absolutePath

        context.assets.open(fileName).use { inputStream: InputStream ->
            FileOutputStream(outputFile).use { outputStream: OutputStream ->
                inputStream.copyTo(out = outputStream)
            }
        }
        return outputFile.absolutePath
    }
}
