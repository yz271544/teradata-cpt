/*
   Copyright [2020] [lyndon]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
   CPT(Content Position Transform)Encrypt Decrypt
   Version 0.1.1 2011-11-17
   Copyright (c) 2011 by Teradata Corporation. All Rights Reserved. 
*/
#ifndef CPT_H
#define CPT_H

#include "jni.h"

#define CLEN 223
#define ILEN 10
#define ALEN 94
#define MAX_SUB_POLICIES 5

enum CPT_MODE {
    CPT_CHAR, CPT_INT, CPT_ASC
};
enum KEY_MODE {
    ENCRYPT_KEY, DECRYPT_KEY
};


#ifdef __cplusplus
extern "C" {
#endif

typedef struct jni_sub_policy_t {
    int position;
    int length;
} jni_sub_policy;

typedef struct jni_policy_t {
    int sub_policy_num;
    jni_sub_policy subPolicy[MAX_SUB_POLICIES];
} jni_policy;


typedef struct sub_policy_block_t {
    jclass clazz;
    jfieldID position;
    jfieldID length;
    jmethodID constructor;
} sub_policy_block;

typedef struct policy_block_t {
    jclass clazz;
    jfieldID sub_policy_num;
    jfieldID sub_policy;
    jmethodID constructor;
} policy_block;

extern policy_block m_jni_policy_block;
extern sub_policy_block m_jni_sub_policy_block;


typedef struct {
    int position;
    int length;
} SubPolicy;

typedef struct {
    int sub_policy_num;
    SubPolicy *subPolicy[5];
} Policy;

//void NewSUB_POLICY(SubPolicy *subPolicy, int position, int length);
//
//void NewPOLICY(Policy *policy);
//
//void NewPOLICYByNum(Policy *policy, int n);
//
//void addSUB_POLICY(Policy *policy, SubPolicy *subPolicy);
//
//void outputPolicyDetail(Policy *policy);

void NewSUB_POLICY(jni_sub_policy* subPolicy, int position, int length);

void NewPOLICY(jni_policy* policy);

void addSUB_POLICY(jni_policy* policy, jni_sub_policy* subPolicy);

void outputPolicyDetail(jni_policy* policy);

void init_ckey(const unsigned long inkey, int cptmode, int keymod, unsigned char *ckey);

void ct_encrypt(const unsigned char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr);

void ct_decrypt(const unsigned char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr);

void auto_ct_encrypt(unsigned const char *instr, unsigned char *ckey, int cptmode, unsigned char *outstr);

void auto_ct_decrypt(unsigned const char *instr, unsigned char *ckey, int cptmode, unsigned char *outstr);

void substr_ct_encrypt(unsigned const char *instr, int startpos, int inlen, unsigned char *ckey, int cptmode,
                       unsigned char *outstr);

int multi_sub_policy_ct_encrypt(const unsigned char *instr, int inlen, const jni_policy *policy,
                                unsigned char *ckey, int cptmode, unsigned char *outstr);

int multi_sub_policy_ct_decrypt(const unsigned char *instr, int inlen, const jni_policy *policy,
                                unsigned char *ckey, int cptmode, unsigned char *outstr);


// 注册
int register_classes(JNIEnv *env);

// C结构体转Java类
jobject policy_c_to_java(JNIEnv *env, jni_policy *jni_policy);

// Java类转C结构体
int policy_java_to_c(JNIEnv *env, jobject policy_in, jni_policy *jni_policy_out);

void outputInstr(unsigned const char *mark, unsigned const char *instr);
#ifdef __cplusplus
}
#endif

#endif /* CPT_H */



