//
// Created by zxh on 2024/1/6.
//
#include <stdio.h>
#include <string.h>
#include "cpt.h"

int main(int argc, char *argv[]) {

    // 数值加密临时密码本
    unsigned char multiSubDigitCkey[ILEN];

    // 原始数据: foxmind_1234_text_139
    printf("origin:%s\n", argv[1]);

    int inlen = strlen(argv[1]);
    outputInstr("origin bytes:", argv[1]);
    printf("origin Length: %d\n", inlen);
    // 生成临时密码本 密钥临时采用123
    init_ckey(123, CPT_INT, ENCRYPT_KEY, multiSubDigitCkey);

    unsigned char enOut[inlen + 1];
    memset(enOut, 0, inlen);
    // 加密
    ct_encrypt(argv[1], inlen, multiSubDigitCkey, CPT_INT, enOut);
    printf("encrypt: %s --> length: %ld\n", enOut, strlen(enOut));
    outputInstr("encrypt bytes:", enOut);
    // 解密
    init_ckey(123, CPT_INT, DECRYPT_KEY, multiSubDigitCkey);
    unsigned char deOut[inlen + 1];
    memset(deOut, 0, inlen);
    ct_decrypt(enOut, inlen, multiSubDigitCkey, CPT_INT, deOut);
    printf("decrypt: %s --> length: %ld\n", deOut, strlen(deOut));
    outputInstr("decrypt bytes:", deOut);

    printf("#############################################################################\n");

    // 中文字符加密临时密码本
    unsigned char multiSubCharCkey[CLEN];

    printf("origin:%s\n", argv[2]);

    int chInlen = strlen(argv[2]);
    outputInstr("origin bytes:", argv[2]);
    printf("origin Length: %d\n", chInlen);
    // 生成临时密码本 密钥临时采用123
    init_ckey(123, CPT_CHAR, ENCRYPT_KEY, multiSubCharCkey);

    unsigned char chEnOut[chInlen + 1];
    memset(chEnOut, 0, chInlen);
    // 加密
    ct_encrypt(argv[2], chInlen, multiSubCharCkey, CPT_CHAR, chEnOut);

    printf("encrypt: %s --> length: %ld\n", chEnOut, strlen(chEnOut));
    outputInstr("encrypt bytes:", chEnOut);

    // 解密
    init_ckey(123, CPT_CHAR, DECRYPT_KEY, multiSubCharCkey);
    unsigned char chDeOut[chInlen + 1];
    memset(chDeOut, 0, chInlen);
    ct_decrypt(chEnOut, chInlen, multiSubCharCkey, CPT_CHAR, chDeOut);
    printf("decrypt: %s --> length: %ld\n", chDeOut, strlen(chDeOut));
    outputInstr("decrypt bytes:", chDeOut);

    return 0;
}
