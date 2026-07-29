//
// JNI class and field registration.
//
#include "cpt.h"

static int find_class(JNIEnv *env, const char *name, jclass *clazz_out) {
    jclass local_class = env->FindClass(name);
    if (local_class == nullptr) {
        return -1;
    }
    *clazz_out = static_cast<jclass>(env->NewGlobalRef(local_class));
    env->DeleteLocalRef(local_class);
    return *clazz_out == nullptr ? -1 : 0;
}

static int get_field(JNIEnv *env, jclass clazz, const char *name,
                     const char *signature, jfieldID *field_out) {
    *field_out = env->GetFieldID(clazz, name, signature);
    return *field_out == nullptr ? -1 : 0;
}

static int register_sub_policy_class(JNIEnv *env) {
    if (find_class(env, "com/teradata/jni/SubPolicy", &m_jni_sub_policy_block.clazz) != 0) {
        return -1;
    }
    jclass clazz = m_jni_sub_policy_block.clazz;
    m_jni_sub_policy_block.constructor = env->GetMethodID(clazz, "<init>", "()V");
    return m_jni_sub_policy_block.constructor != nullptr &&
           get_field(env, clazz, "position", "I", &m_jni_sub_policy_block.position) == 0 &&
           get_field(env, clazz, "length", "I", &m_jni_sub_policy_block.length) == 0
           ? 0 : -1;
}

static int register_policy_class(JNIEnv *env) {
    if (find_class(env, "com/teradata/jni/Policy", &m_jni_policy_block.clazz) != 0) {
        return -1;
    }
    jclass clazz = m_jni_policy_block.clazz;
    m_jni_policy_block.constructor = env->GetMethodID(clazz, "<init>", "()V");
    return m_jni_policy_block.constructor != nullptr &&
           get_field(env, clazz, "sub_policy_num", "I", &m_jni_policy_block.sub_policy_num) == 0 &&
           get_field(env, clazz, "sub_policy", "[Lcom/teradata/jni/SubPolicy;",
                     &m_jni_policy_block.sub_policy) == 0
           ? 0 : -1;
}

int register_classes(JNIEnv *env) {
    return register_sub_policy_class(env) == 0 && register_policy_class(env) == 0
           ? 0 : -1;
}
