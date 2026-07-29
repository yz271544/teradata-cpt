//
// Java Policy to native conversion.
//
#include "jni.h"
#include "cpt.h"

int policy_java_to_c(JNIEnv *env, jobject policy_in, jni_policy *policy_out) {
    if (policy_in == nullptr || policy_out == nullptr) {
        return -1;
    }

    jobjectArray sub_policy_array = static_cast<jobjectArray>(
            env->GetObjectField(policy_in, m_jni_policy_block.sub_policy));
    if (sub_policy_array == nullptr || env->ExceptionCheck()) {
        return -1;
    }

    jsize length = env->GetArrayLength(sub_policy_array);
    jint declared_length = env->GetIntField(policy_in, m_jni_policy_block.sub_policy_num);
    if (length < 0 || length > MAX_SUB_POLICIES || declared_length != length) {
        env->DeleteLocalRef(sub_policy_array);
        return -1;
    }

    policy_out->sub_policy_num = length;
    for (jsize i = 0; i < length; ++i) {
        jobject sub_policy = env->GetObjectArrayElement(sub_policy_array, i);
        if (sub_policy == nullptr || env->ExceptionCheck()) {
            if (sub_policy != nullptr) {
                env->DeleteLocalRef(sub_policy);
            }
            env->DeleteLocalRef(sub_policy_array);
            return -1;
        }
        policy_out->subPolicy[i].position =
                env->GetIntField(sub_policy, m_jni_sub_policy_block.position);
        policy_out->subPolicy[i].length =
                env->GetIntField(sub_policy, m_jni_sub_policy_block.length);
        env->DeleteLocalRef(sub_policy);
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(sub_policy_array);
            return -1;
        }
    }
    env->DeleteLocalRef(sub_policy_array);
    return 0;
}
