#include <jni.h>
#include <string>

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_MainActivity_validateKeyNative(JNIEnv* env, jobject thiz, jstring input_key) {
    if (input_key == NULL) {
        return JNI_FALSE;
    }

    const char* native_str = env->GetStringUTFChars(input_key, nullptr);
    std::string key(native_str);
    env->ReleaseStringUTFChars(input_key, native_str);

    // Valida se a chave inserida é igual à chave criptografada do loader
    return (key == "ITS8BPVIP-O929-JE2J-0MW2-OWI2") ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_example_MainActivity_getLibraryStatus(JNIEnv* env, jobject thiz) {
    return env->NewStringUTF("ItsLGL Carregada - Tela de Lógica Moderna Configurada");
}

}
