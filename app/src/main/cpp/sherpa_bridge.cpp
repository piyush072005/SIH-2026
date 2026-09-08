#include <jni.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "itantra-sherpa", __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_isro_itantra_audio_nativebridge_SherpaJni_nativeRuntimeName(JNIEnv *env, jclass) {
    LOGI("sherpa-onnx JNI bridge loaded (Kotlin AAR is the primary runtime)");
    return env->NewStringUTF("sherpa-onnx");
}
