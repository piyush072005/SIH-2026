package com.isro.itantra.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Copies bundled ONNX assets into app files so sherpa-onnx can mmap them.
 */
object AssetModelUnpacker {
    fun unpackLanguage(context: Context, languageCode: String): File {
        val root = File(context.filesDir, "models")
        copyAssetDir(context, "models/stt/$languageCode", File(root, "stt/$languageCode"))
        copyAssetDir(context, "models/tts/$languageCode", File(root, "tts/$languageCode"))
        copyAssetDir(context, "models/vad", File(root, "vad"))
        return root
    }

    private fun copyAssetDir(context: Context, assetDir: String, dest: File) {
        val names = context.assets.list(assetDir) ?: return
        dest.mkdirs()
        for (name in names) {
            val assetPath = "$assetDir/$name"
            val children = context.assets.list(assetPath)
            if (children != null && children.isNotEmpty()) {
                copyAssetDir(context, assetPath, File(dest, name))
            } else {
                val out = File(dest, name)
                if (out.exists() && out.length() > 0) continue
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
