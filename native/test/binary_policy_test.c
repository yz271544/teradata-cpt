#include <stdio.h>
#include <string.h>

#include "cpt.h"

static int assert_bytes(const char *name, const unsigned char *actual,
                        const unsigned char *expected, int length) {
    if (memcmp(actual, expected, length) == 0) {
        return 0;
    }

    fprintf(stderr, "%s mismatch\nexpected:", name);
    for (int i = 0; i < length; ++i) {
        fprintf(stderr, " %02x", expected[i]);
    }
    fprintf(stderr, "\nactual:  ");
    for (int i = 0; i < length; ++i) {
        fprintf(stderr, " %02x", actual[i]);
    }
    fprintf(stderr, "\n");
    return 1;
}

int main(void) {
    static const unsigned char input[] = {'1', '2', 0x00, '3', '4'};
    const int input_length = (int) sizeof(input);
    unsigned char encrypted[sizeof(input)] = {0};
    unsigned char decrypted[sizeof(input)] = {0};
    unsigned char key[CLEN];

    jni_policy policy;
    NewPOLICY(&policy);
    jni_sub_policy sub_policy;
    NewSUB_POLICY(&sub_policy, 0, input_length);
    addSUB_POLICY(&policy, &sub_policy);

    init_ckey(123, CPT_INT, ENCRYPT_KEY, key);
    multi_sub_policy_ct_encrypt(input, input_length, &policy, key, CPT_INT,
                                encrypted);

    init_ckey(123, CPT_INT, DECRYPT_KEY, key);
    multi_sub_policy_ct_decrypt(encrypted, input_length, &policy, key, CPT_INT,
                                decrypted);

    return assert_bytes("embedded-zero round trip", decrypted, input,
                        input_length);
}
