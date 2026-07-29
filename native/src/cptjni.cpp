/*
   Copyright [2020] [lyndon]
   Licensed under the Apache License, Version 2.0.
*/
#include <vector>

#include "jni.h"
#include "com_teradata_jni_CptJni.h"
#include "cpt.h"

static void throw_exception(JNIEnv *env, const char *class_name, const char *message) {
    jclass exception_class = env->FindClass(class_name);
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message);
        env->DeleteLocalRef(exception_class);
    }
}

static bool valid_mode(jbyte mode) {
    return mode == CPT_CHAR || mode == CPT_INT || mode == CPT_ASC;
}

static jbyteArray transform(JNIEnv *env, jbyteArray input, jlong key, jbyte mode,
                            bool encrypting) {
    if (input == nullptr) {
        throw_exception(env, "java/lang/IllegalArgumentException", "input must not be null");
        return nullptr;
    }
    if (!valid_mode(mode)) {
        throw_exception(env, "java/lang/IllegalArgumentException", "mode must be 0, 1, or 2");
        return nullptr;
    }

    jsize length = env->GetArrayLength(input);
    std::vector<jbyte> in(static_cast<size_t>(length));
    std::vector<jbyte> out(static_cast<size_t>(length) + 1);
    if (length > 0) {
        env->GetByteArrayRegion(input, 0, length, in.data());
        if (env->ExceptionCheck()) {
            return nullptr;
        }
    }

    unsigned char ckey[CLEN];
    init_ckey(static_cast<unsigned long>(key), mode,
              encrypting ? ENCRYPT_KEY : DECRYPT_KEY, ckey);
    if (encrypting) {
        ct_encrypt(reinterpret_cast<unsigned char *>(in.data()), length, ckey, mode,
                   reinterpret_cast<unsigned char *>(out.data()));
    } else {
        ct_decrypt(reinterpret_cast<unsigned char *>(in.data()), length, ckey, mode,
                   reinterpret_cast<unsigned char *>(out.data()));
    }

    jbyteArray result = env->NewByteArray(length);
    if (result != nullptr && length > 0) {
        env->SetByteArrayRegion(result, 0, length, out.data());
    }
    return result;
}

static jbyteArray transform_policy(JNIEnv *env, jbyteArray input, jobject policy_object,
                                   jlong key, jbyte mode, bool encrypting) {
    if (input == nullptr || policy_object == nullptr) {
        throw_exception(env, "java/lang/IllegalArgumentException",
                        "input and policy must not be null");
        return nullptr;
    }
    if (!valid_mode(mode)) {
        throw_exception(env, "java/lang/IllegalArgumentException", "mode must be 0, 1, or 2");
        return nullptr;
    }

    jni_policy policy = {};
    if (policy_java_to_c(env, policy_object, &policy) != 0 || env->ExceptionCheck()) {
        if (!env->ExceptionCheck()) {
            throw_exception(env, "java/lang/IllegalArgumentException", "invalid policy");
        }
        return nullptr;
    }

    jsize length = env->GetArrayLength(input);
    std::vector<jbyte> in(static_cast<size_t>(length));
    std::vector<jbyte> out(static_cast<size_t>(length) + 1);
    if (length > 0) {
        env->GetByteArrayRegion(input, 0, length, in.data());
        if (env->ExceptionCheck()) {
            return nullptr;
        }
    }

    unsigned char ckey[CLEN];
    init_ckey(static_cast<unsigned long>(key), mode,
              encrypting ? ENCRYPT_KEY : DECRYPT_KEY, ckey);
    int status = encrypting
            ? multi_sub_policy_ct_encrypt(reinterpret_cast<unsigned char *>(in.data()),
                                          length, &policy, ckey, mode,
                                          reinterpret_cast<unsigned char *>(out.data()))
            : multi_sub_policy_ct_decrypt(reinterpret_cast<unsigned char *>(in.data()),
                                          length, &policy, ckey, mode,
                                          reinterpret_cast<unsigned char *>(out.data()));
    if (status != 0) {
        throw_exception(env, "java/lang/IllegalArgumentException",
                        "policy exceeds input byte length or native allocation failed");
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(length);
    if (result != nullptr && length > 0) {
        env->SetByteArrayRegion(result, 0, length, out.data());
    }
    return result;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    return register_classes(env) == 0 ? JNI_VERSION_1_6 : JNI_ERR;
}

JNIEXPORT jbyteArray JNICALL Java_com_teradata_jni_CptJni_encrypt(
        JNIEnv *env, jclass clazz, jbyteArray input, jlong key, jbyte mode) {
    (void) clazz;
    return transform(env, input, key, mode, true);
}

JNIEXPORT jbyteArray JNICALL Java_com_teradata_jni_CptJni_decrypt(
        JNIEnv *env, jclass clazz, jbyteArray input, jlong key, jbyte mode) {
    (void) clazz;
    return transform(env, input, key, mode, false);
}

JNIEXPORT jbyteArray JNICALL Java_com_teradata_jni_CptJni_multiSubPolicyEncrypt(
        JNIEnv *env, jclass clazz, jbyteArray input, jobject policy, jlong key, jbyte mode) {
    (void) clazz;
    return transform_policy(env, input, policy, key, mode, true);
}

JNIEXPORT jbyteArray JNICALL Java_com_teradata_jni_CptJni_multiSubPolicyDecrypt(
        JNIEnv *env, jclass clazz, jbyteArray input, jobject policy, jlong key, jbyte mode) {
    (void) clazz;
    return transform_policy(env, input, policy, key, mode, false);
}
