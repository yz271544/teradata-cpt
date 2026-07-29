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
   Version 1.0.1 2011-11-17
   Copyright (c) 2011 by Teradata Corporation. All Rights Reserved. 
   
*/
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include "cpt.h"

#define I_OFFSET 0x30
#define C_OFFSET 0x21
#define A_OFFSET 0x21

unsigned const char CT[] = {
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30,
        0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50,
        0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40,
        0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70,
        0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b, 0x5c, 0x5d, 0x5e, 0x5f, 0x60,
        0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x79, 0x78, 0x7a, 0x7b, 0x7c, 0x7d, 0x7e, 0x7f, 0x80,
        0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, 0xa0,
        0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8e, 0x8b, 0x8c, 0x8d, 0x8a, 0x8f, 0x90,
        0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad, 0xae, 0xaf, 0xb0,
        0xc1, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf, 0xd0,
        0xb1, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xbc, 0xbb, 0xbe, 0xbd, 0xbf, 0xc0,
        0xe1, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xeb, 0xec, 0xed, 0xee, 0xef, 0xf0,
        0xd1, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xdb, 0xdc, 0xdd, 0xde, 0xdf, 0xe0,
        0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff
};

/*unsigned const char IT[] = {0x39,0x38,0x37,0x36,0x35,0x34,0x33,0x32,0x31,0x30};		*/

unsigned const char IT[] = {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39};
unsigned const char AT[] = {
        0x3a, 0x32, 0x39, 0x34, 0x35, 0x3f, 0x37, 0x38, 0x33, 0x31, 0x3b, 0x3c, 0x3d, 0x3e, 0x36, 0x40,
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30,
        0x51, 0x52, 0x53, 0x5f, 0x55, 0x56, 0x59, 0x58, 0x57, 0x5c, 0x5b, 0x5a, 0x5d, 0x5e, 0x54, 0x60,
        0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50,
        0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x7b, 0x7c, 0x7d, 0x7e,
        0x68, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x61, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70
};

policy_block m_jni_policy_block;
sub_policy_block m_jni_sub_policy_block;

void init_ckey(const unsigned long inkey, int cptmode, int keymod, unsigned char *ckey) {
    unsigned char *T;
    short Tlen;
    unsigned char offset;
    int *mod;
    int i;

    if (cptmode == CPT_INT) {
        Tlen = ILEN;
        T = (unsigned char *) malloc(Tlen * sizeof(char));
        memcpy(T, IT, Tlen);
        offset = I_OFFSET;
    } else if (cptmode == CPT_CHAR) {
        Tlen = CLEN;
        T = (unsigned char *) malloc(Tlen * sizeof(char));
        memcpy(T, CT, Tlen);
        offset = C_OFFSET;
    } else {
        Tlen = ALEN;
        T = (unsigned char *) malloc(Tlen * sizeof(char));
        memcpy(T, AT, Tlen);
        offset = A_OFFSET;
    }

    mod = (int *) malloc(Tlen * sizeof(int));
    if (keymod == ENCRYPT_KEY) {
        for (i = 0; i < Tlen; i++) {
            mod[i] = inkey % (Tlen - i);
            ckey[i] = T[mod[i]];
            memmove(T + mod[i], T + mod[i] + 1, Tlen - 1 - i - mod[i]);
        }
    } else if (keymod == DECRYPT_KEY) {
        for (i = 0; i < Tlen; i++) {
            mod[i] = inkey % (Tlen - i);
            ckey[T[mod[i]] - offset] = i + offset;
            memmove(T + mod[i], T + mod[i] + 1, Tlen - 1 - i - mod[i]);
        }
    }
    free(mod);
    free(T);
}

void ctl_encrypt(unsigned const char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    int i;
    int offset;
    int len;

    if (cptmode == CPT_INT) {
        offset = I_OFFSET;
        len = ILEN;
    } else if (cptmode == CPT_CHAR) {
        offset = C_OFFSET;
        len = CLEN;
    } else {
        offset = A_OFFSET;
        len = ALEN;
    }

    for (i = 0; i < inlen; i++) {
        if (instr[i] < offset || instr[i] > offset + len - 1) {
            outstr[i] = instr[i];
        } else {
            if (i == 0) {
                outstr[i] = ckey[instr[i] - offset];
            } else if (outstr[i - 1] < offset || outstr[i - 1] > offset + len - 1) {
                outstr[i] = ckey[instr[i] - offset];
            } else {
                outstr[i] = ckey[(outstr[i - 1] + instr[i] - offset * 2) % len];
            }
        }
    }
    outstr[inlen] = '\0';
}

void ctr_encrypt(const unsigned char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    int i;
    int offset;
    int len;

    if (cptmode == CPT_INT) {
        offset = I_OFFSET;
        len = ILEN;
    } else if (cptmode == CPT_CHAR) {
        offset = C_OFFSET;
        len = CLEN;
    } else {
        offset = A_OFFSET;
        len = ALEN;
    }

    for (i = inlen - 1; i >= 0; i--) {
        if (instr[i] < offset || instr[i] > offset + len - 1) {
            outstr[i] = instr[i];
        } else {
            if (i == inlen - 1) {
                outstr[i] = ckey[instr[i] - offset];
            } else if (outstr[i + 1] < offset || outstr[i + 1] > offset + len - 1) {
                outstr[i] = ckey[instr[i] - offset];
            } else {
                outstr[i] = ckey[(outstr[i + 1] + instr[i] - offset * 2) % len];
            }
        }
    }
    outstr[inlen] = '\0';
}

void ctl_decrypt(const unsigned char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    int i;
    int offset;
    int len;

    if (cptmode == CPT_INT) {
        offset = I_OFFSET;
        len = ILEN;
    } else if (cptmode == CPT_CHAR) {
        offset = C_OFFSET;
        len = CLEN;
    } else {
        offset = A_OFFSET;
        len = ALEN;
    }

    for (i = 0; i < inlen; i++) {
        if (instr[i] < offset || instr[i] > offset + len - 1) {
            outstr[i] = instr[i];
        } else {
            if (i == 0) {
                outstr[i] = ckey[instr[i] - offset];
            } else if (instr[i - 1] < offset || instr[i - 1] > offset + len - 1) {
                outstr[i] = ckey[instr[i] - offset];
            } else {
                if (ckey[instr[i] - offset] >= instr[i - 1]) {
                    outstr[i] = ckey[instr[i] - offset] - instr[i - 1] + offset;
                } else {
                    outstr[i] = ckey[instr[i] - offset] - instr[i - 1] + offset + len;
                }
            }
        }
    }
    outstr[inlen] = '\0';
}

void ctr_decrypt(unsigned const char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    int i;
    int offset;
    int len;

    if (cptmode == CPT_INT) {
        offset = I_OFFSET;
        len = ILEN;
    } else if (cptmode == CPT_CHAR) {
        offset = C_OFFSET;
        len = CLEN;
    } else {
        offset = A_OFFSET;
        len = ALEN;
    }

    for (i = inlen - 1; i >= 0; i--) {
        if (instr[i] < offset || instr[i] > offset + len - 1) {
            outstr[i] = instr[i];
        } else {
            if (i == inlen - 1) {
                outstr[i] = ckey[instr[i] - offset];
            } else if (instr[i + 1] < offset || instr[i + 1] > offset + len - 1) {
                outstr[i] = ckey[instr[i] - offset];
            } else {
                if (ckey[instr[i] - offset] >= instr[i + 1]) {
                    outstr[i] = ckey[instr[i] - offset] - instr[i + 1] + offset;
                } else {
                    outstr[i] = ckey[instr[i] - offset] - instr[i + 1] + offset + len;
                }
            }
        }
    }
    outstr[inlen] = '\0';
}

void ct_encrypt(unsigned const char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    unsigned char *ctl = (unsigned char *) malloc(((size_t) inlen + 1) * sizeof(char));

    ctl_encrypt(instr, inlen, ckey, cptmode, ctl);
//    printf("1---: %s\n", ctl);
//    outputInstr("1---: bytes:", ctl);
    ctr_encrypt(ctl, inlen, ckey, cptmode, outstr);

    free(ctl);
}

void ct_decrypt(unsigned const char *instr, int inlen, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    unsigned char *ctr = (unsigned char *) malloc(((size_t) inlen + 1) * sizeof(char));

    ctr_decrypt(instr, inlen, ckey, cptmode, ctr);
//    printf("1---: %s\n", ctr);
//    outputInstr("1---: bytes:", ctr);
    ctl_decrypt(ctr, inlen, ckey, cptmode, outstr);
    free(ctr);
}

void auto_ct_encrypt(unsigned const char *instr, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    int inlen = strlen(instr);
    unsigned char *ctl = (unsigned char *) malloc(inlen * sizeof(char));

    ctl_encrypt(instr, inlen, ckey, cptmode, ctl);
    ctr_encrypt(ctl, inlen, ckey, cptmode, outstr);

    free(ctl);
}

void auto_ct_decrypt(unsigned const char *instr, unsigned char *ckey, int cptmode, unsigned char *outstr) {
    int inlen = strlen(instr);
    unsigned char *ctr = (unsigned char *) malloc(inlen * sizeof(char));

    ctr_decrypt(instr, inlen, ckey, cptmode, ctr);
    ctl_decrypt(ctr, inlen, ckey, cptmode, outstr);
    free(ctr);
}

void substr_ct_encrypt(unsigned const char *instr, int startpos, int inlen, unsigned char *ckey, int cptmode,
                       unsigned char *outstr) {
    unsigned char *ctl = (unsigned char *) malloc(inlen * sizeof(char));

    unsigned char *tempout = (unsigned char *) malloc(inlen * sizeof(char));
    unsigned char *substr = (unsigned char *) malloc(inlen * sizeof(char));
    memcpy(outstr, instr, strlen(instr));

    memcpy(substr, &instr[startpos], inlen);
    ctl_encrypt(substr, inlen, ckey, cptmode, ctl);
    ctr_encrypt(ctl, inlen, ckey, cptmode, tempout);
    memcpy(&outstr[startpos], tempout, inlen);

    free(ctl);
    free(tempout);
    free(substr);
}

// ++ 优先级高  先执行*to = * from 然后加进行自加
void copy_str2(unsigned const char *from, unsigned char *to) {

    for (; *from != '\0';) {
        *to++ = *from++;
    }
    *to = '\0';//注意最后要记得拷贝'\0'
}


static int validate_policy(const jni_policy *policy, int inlen) {
    if (policy == NULL || inlen < 0 || policy->sub_policy_num < 0 ||
        policy->sub_policy_num > MAX_SUB_POLICIES) {
        return -1;
    }
    for (int i = 0; i < policy->sub_policy_num; ++i) {
        int position = policy->subPolicy[i].position;
        int length = policy->subPolicy[i].length;
        if (position < 0 || length < 0 || position > inlen ||
            length > inlen - position) {
            return -1;
        }
    }
    return 0;
}

int multi_sub_policy_ct_encrypt(const unsigned char *instr, int inlen, const jni_policy *policy,
                                unsigned char *ckey, int cptmode, unsigned char *outstr) {
    if (instr == NULL || ckey == NULL || outstr == NULL ||
        validate_policy(policy, inlen) != 0) {
        return -1;
    }
    memcpy(outstr, instr, (size_t) inlen);

    for (int i = 0; i < policy->sub_policy_num; i++) {
        int cPos = policy->subPolicy[i].position;
        int cLen = policy->subPolicy[i].length;
        unsigned char *ctr = (unsigned char *) malloc((size_t) cLen + 1);
        unsigned char *tempout = (unsigned char *) malloc((size_t) cLen + 1);
        if (ctr == NULL || tempout == NULL) {
            free(ctr);
            free(tempout);
            return -1;
        }
        ctl_encrypt(&instr[cPos], cLen, ckey, cptmode, ctr);
        ctr_encrypt(ctr, cLen, ckey, cptmode, tempout);
        memcpy(&outstr[cPos], tempout, (size_t) cLen);
        free(ctr);
        free(tempout);
    }
    return 0;
}

int multi_sub_policy_ct_decrypt(const unsigned char *instr, int inlen, const jni_policy *policy,
                                unsigned char *ckey, int cptmode, unsigned char *outstr) {
    if (instr == NULL || ckey == NULL || outstr == NULL ||
        validate_policy(policy, inlen) != 0) {
        return -1;
    }
    memcpy(outstr, instr, (size_t) inlen);

    for (int i = 0; i < policy->sub_policy_num; i++) {
        int cPos = policy->subPolicy[i].position;
        int cLen = policy->subPolicy[i].length;
        unsigned char *ctl = (unsigned char *) malloc((size_t) cLen + 1);
        unsigned char *tempout = (unsigned char *) malloc((size_t) cLen + 1);
        if (ctl == NULL || tempout == NULL) {
            free(ctl);
            free(tempout);
            return -1;
        }
        ctr_decrypt(&instr[cPos], cLen, ckey, cptmode, ctl);
        ctl_decrypt(ctl, cLen, ckey, cptmode, tempout);
        memcpy(&outstr[cPos], tempout, (size_t) cLen);
        free(ctl);
        free(tempout);
    }
    return 0;
}


//结构体函数
void NewSUB_POLICY(jni_sub_policy* subPolicy, int position, int length) {
    subPolicy->position = position;
    subPolicy->length = length;
}

void NewPOLICY(jni_policy *policy) {
    policy->sub_policy_num = 0;
}

void addSUB_POLICY(jni_policy* policy, jni_sub_policy* subPolicy) {
//    policy->subPolicy[policy->sub_policy_num] = (SubPolicy*) malloc(sizeof(SubPolicy));
    policy->subPolicy[policy->sub_policy_num] = *subPolicy;
//    memcpy(policy->subPolicy[policy->sub_policy_num], subPolicy, sizeof(SubPolicy));
//    memcpy(&(policy->subPolicy[policy->sub_policy_num]->position), &(subPolicy->position), sizeof(int));
//    memcpy(&(policy->subPolicy[policy->sub_policy_num]->length), &(subPolicy->length), sizeof(int));
    policy->sub_policy_num = policy->sub_policy_num + 1;
}

void outputPolicyDetail(jni_policy *policy) {
    for (int i = 0; i < policy->sub_policy_num; i++) {
        printf("id: %d, position=%d,length=%d\n", i, policy->subPolicy[i].position, policy->subPolicy[i].length);
    }
}

/*

//结构体函数
void NewSUB_POLICY(SubPolicy *subPolicy, int position, int length) {
    subPolicy->position = position;
    subPolicy->length = length;
}

void NewPOLICY(Policy *policy) {
    policy->sub_policy_num = 0;
}

void NewPOLICYByNum(Policy *policy, int n) {
    policy->sub_policy_num = n;
    for (int i = 0; i < n; i++) {
        policy->subPolicy[i] = (SubPolicy*) malloc(sizeof(SubPolicy));
        memset(policy, '\0', sizeof(SubPolicy));
    }
}

void addSUB_POLICY(Policy *policy, SubPolicy *subPolicy) {
//    policy->subPolicy[policy->sub_policy_num] = (SubPolicy*) malloc(sizeof(SubPolicy));
    policy->subPolicy[policy->sub_policy_num] = subPolicy;
//    memcpy(policy->subPolicy[policy->sub_policy_num], subPolicy, sizeof(SubPolicy));
//    memcpy(&(policy->subPolicy[policy->sub_policy_num]->position), &(subPolicy->position), sizeof(int));
//    memcpy(&(policy->subPolicy[policy->sub_policy_num]->length), &(subPolicy->length), sizeof(int));
    policy->sub_policy_num = policy->sub_policy_num + 1;
}

void outputPolicyDetail(Policy *policy) {
    for (int i = 0; i < policy->sub_policy_num; i++) {
        printf("id: %d, position=%d,length=%d\n", i, policy->subPolicy[i]->position, policy->subPolicy[i]->length);
    }
}
*/

void outputInstr(unsigned const char *mark, unsigned const char *instr) {
    printf("%s: ", mark);
    int i = 0;
    while (instr[i] != '\0') {
        printf("%02x ", instr[i]);
        i++;
    }
    printf("\n");
}
