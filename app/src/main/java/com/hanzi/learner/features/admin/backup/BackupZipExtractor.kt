package com.hanzi.learner.features.admin.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ExtractedMakemeahanziFiles(
    val graphicsFile: File,
    val dictionaryFile: File?,
)

class BackupZipExtractor(
    private val contentResolver: ContentResolver,
) {
    fun extractMakemeahanziFiles(
        uri: Uri,
        graphicsTemp: File,
        dictionaryTemp: File,
    ): ExtractedMakemeahanziFiles {
        graphicsTemp.delete()
        dictionaryTemp.delete()

        var hasGraphics = false
        var hasDict = false

        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法打开导入文件")

        inputStream.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name.replace('\\', '/')
                    when {
                        name.endsWith("/graphics.txt") || name == "graphics.txt" -> {
                            FileOutputStream(graphicsTemp).use { out -> zip.copyTo(out) }
                            hasGraphics = true
                        }

                        name.endsWith("/dictionary.txt") || name == "dictionary.txt" -> {
                            FileOutputStream(dictionaryTemp).use { out -> zip.copyTo(out) }
                            hasDict = true
                        }
                    }
                    zip.closeEntry()
                }
            }
        }

        if (!hasGraphics) throw IllegalArgumentException("ZIP 内未找到 graphics.txt")
        return ExtractedMakemeahanziFiles(
            graphicsFile = graphicsTemp,
            dictionaryFile = if (hasDict) dictionaryTemp else null,
        )
    }
}

