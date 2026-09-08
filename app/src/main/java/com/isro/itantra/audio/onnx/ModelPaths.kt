package com.isro.itantra.audio.onnx

data class ModelPaths(
    val languageCode: String,
    val sttDir: String,
    val ttsDir: String,
    val vadPath: String,
) {
    val sttInt8: String get() = "$sttDir/model.int8.onnx"
    val sttTokens: String get() = "$sttDir/tokens.txt"
    val ttsModel: String get() = "$ttsDir/model.onnx"
    val ttsTokens: String get() = "$ttsDir/tokens.txt"
    val ttsDataDir: String get() = "$ttsDir/espeak-ng-data"
}
