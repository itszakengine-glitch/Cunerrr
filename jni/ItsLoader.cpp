#include <jni.h>
#include <string>

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_MainActivity_getEncryptedKey(JNIEnv* env, jobject thiz) {
    // Retorna a chave criptografada para o MainActivity
    return env->NewStringUTF("ITS8BPVIP-O929-JE2J-0MW2-OWI2");
}

}
