#include <jni.h>
#include <string>
#include "sentencepiece/src/sentencepiece_processor.h"

sentencepiece::SentencePieceProcessor processor;


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_aloe_embedding_embedder_GemmaEmbedder_loadTokenizerModel(JNIEnv *env, jobject thiz, jstring path) {
    const char *modelPath = env->GetStringUTFChars(path, nullptr);
    const auto status = processor.Load(modelPath);
    env->ReleaseStringUTFChars(path, modelPath);

    const bool isModelLoaded = status.ok();
    return isModelLoaded ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_aloe_embedding_embedder_GemmaEmbedder_nativeTokenize(JNIEnv *env, jobject thiz, jstring text) {
    const char *inputText = env->GetStringUTFChars(text, nullptr);
    std::vector<std::string> pieces;
    processor.Encode(inputText, &pieces);
    env->ReleaseStringUTFChars(text, inputText);

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(pieces.size(), stringClass, nullptr);

    for (int i = 0; i < pieces.size(); i++) {
        jstring piece = env->NewStringUTF(pieces[i].c_str());
        env->SetObjectArrayElement(result, i, piece);
        env->DeleteLocalRef(piece);
    }

    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_aloe_embedding_embedder_GemmaEmbedder_nativeEncode(JNIEnv *env, jobject thiz, jstring text) {
    const char *inputText = env->GetStringUTFChars(text, nullptr);
    std::vector<int> ids;
    processor.Encode(inputText, &ids);
    env->ReleaseStringUTFChars(text, inputText);

    jintArray result = env->NewIntArray(ids.size());
    env->SetIntArrayRegion(result, 0, ids.size(), ids.data());

    return result;
}
