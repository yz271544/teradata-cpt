#include <stdio.h>
#include <string.h>
#include "cpt.h"

int main(int argc, char *argv[]) {

    // 数值加密临时密码本
    unsigned char multiSubDigitCkey[CLEN];

    // 原始数据: foxmind_1234_text_139
    printf("origin:%s\n", argv[1]);

    // 生成临时密码本 密钥临时采用123
    init_ckey(123, CPT_INT, ENCRYPT_KEY, multiSubDigitCkey);

    // 加密声明输出变量
    unsigned char multiSubEnDigitOut[255] = {0};
    // 初始化一个加密策略
    jni_policy policy;
    NewPOLICY(&policy);

    // 初始化加密子策略1, 本例代表从第8位开始加密4位
    jni_sub_policy subPolicy1;
    NewSUB_POLICY(&subPolicy1, 8, 4);

    // 初始化加密子策略2, 本例代表从第18位开始加密3位
    jni_sub_policy subPolicy2;
    NewSUB_POLICY(&subPolicy2, 18, 3);

    // 添加两个子策略到整体策略中，目前项目仅支持5个加密子策略
    addSUB_POLICY(&policy, &subPolicy1);
    addSUB_POLICY(&policy, &subPolicy2);

    // 加密
    multi_sub_policy_ct_encrypt(argv[1], (int) strlen(argv[1]), &policy,
                                multiSubDigitCkey, CPT_INT, multiSubEnDigitOut);
    printf("multiSubEnDigitOut:%s\n", multiSubEnDigitOut);
    // Expect output:
    // foxmind_8847_text_125

    // 解密
    init_ckey(123, CPT_INT, DECRYPT_KEY, multiSubDigitCkey);
    multi_sub_policy_ct_decrypt(multiSubEnDigitOut,
                                (int) strlen((char *) multiSubEnDigitOut), &policy,
                                multiSubDigitCkey, CPT_INT, multiSubEnDigitOut);
    printf("multiSubEnDigitOut:%s\n", multiSubEnDigitOut);
    // Expect output:
    // foxmind_1234_text_139

    return 0;
}
