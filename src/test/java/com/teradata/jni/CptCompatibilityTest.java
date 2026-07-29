package com.teradata.jni;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CptCompatibilityTest {
    private static final long KEY = 123L;

    @Test
    public void digitModeMatchesCReferenceVector() {
        byte[] input = "foxmind_1234_text_139".getBytes(StandardCharsets.ISO_8859_1);
        byte[] encrypted = CptJni.encrypt(input, KEY, CptMode.DIGIT);
        assertEquals("foxmind_8847_text_125",
                new String(encrypted, StandardCharsets.ISO_8859_1));
        assertArrayEquals(input, CptJni.decrypt(encrypted, KEY, CptMode.DIGIT));
    }

    @Test
    public void multiPolicyMatchesCReferenceVector() {
        byte[] input = "foxmind_1234_text_139".getBytes(StandardCharsets.ISO_8859_1);
        Policy policy = Policy.of(new SubPolicy(8, 4), new SubPolicy(18, 3));
        byte[] encrypted = CptJni.multiSubPolicyEncrypt(input, policy, KEY, CptMode.DIGIT);
        assertEquals("foxmind_8847_text_125",
                new String(encrypted, StandardCharsets.ISO_8859_1));
        assertArrayEquals(input,
                CptJni.multiSubPolicyDecrypt(encrypted, policy, KEY, CptMode.DIGIT));
    }

    @Test
    public void embeddedZeroRoundTripsLikeC() {
        byte[] input = new byte[]{'1', '2', 0, '3', '4'};
        Policy policy = Policy.of(new SubPolicy(0, input.length));
        byte[] encrypted = CptJni.multiSubPolicyEncrypt(input, policy, KEY, CptMode.DIGIT);
        assertArrayEquals(input,
                CptJni.multiSubPolicyDecrypt(encrypted, policy, KEY, CptMode.DIGIT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfBoundsPolicy() {
        CptJni.multiSubPolicyEncrypt(new byte[4],
                Policy.of(new SubPolicy(3, 2)), KEY, CptMode.DIGIT);
    }
}
