package com.teradata.jni;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 中文及中英文混合字段的 JNI 加解密测试。
 *
 * 策略的位置和长度均按编码后的字节计算，而不是按 Java 字符下标计算。
 */
public class CptChineseTest {
    private static final long KEY = 123L;

    @Test
    public void utf8ChineseBytesRoundTripInCharMode() {
        String source = "山西太原数据仓库";
        byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = CptJni.encrypt(sourceBytes, KEY, CptMode.CHAR);
        byte[] decrypted = CptJni.decrypt(encrypted, KEY, CptMode.CHAR);

        assertEquals("CHAR 模式必须保持 UTF-8 字节长度不变",
                sourceBytes.length, encrypted.length);
        assertFalse("中文 UTF-8 字节加密后不应与原文完全相同",
                java.util.Arrays.equals(sourceBytes, encrypted));
        assertArrayEquals("中文 UTF-8 字节必须能够完整还原", sourceBytes, decrypted);
        assertEquals("解密字节应恢复为原中文文本",
                source, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    public void utf16ChineseStringCompatibilityApiRoundTrips() throws Exception {
        String source = "胡大美在偷偷地开发数据加密程序";

        String encrypted = CptJni.encrypt(source, KEY, (byte) 0, true);
        String decrypted = CptJni.decrypt(encrypted, KEY, (byte) 0, true);

        assertEquals("兼容 String API 应恢复中文原文", source, decrypted);
        assertEquals("ISO-8859-1 密文长度应等于 UTF-16 字节长度",
                source.getBytes(StandardCharsets.UTF_16).length, encrypted.length());
    }

    @Test
    public void digitModeOnlyEncryptsNumbersInChineseUtf8Field() {
        String source = "客户张三手机号13800138000归属地北京";
        byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = CptJni.encrypt(sourceBytes, KEY, CptMode.DIGIT);
        byte[] decrypted = CptJni.decrypt(encrypted, KEY, CptMode.DIGIT);

        assertEquals("DIGIT 模式不得改变字段字节长度",
                sourceBytes.length, encrypted.length);
        assertEquals("中文及非数字部分应保持不变",
                "客户张三手机号", new String(encrypted, 0,
                        "客户张三手机号".getBytes(StandardCharsets.UTF_8).length,
                        StandardCharsets.UTF_8));
        assertFalse("手机号数字应被变换",
                java.util.Arrays.equals(sourceBytes, encrypted));
        assertArrayEquals("中英文混合字段必须能够完整解密", sourceBytes, decrypted);
    }

    @Test
    public void multiPolicyEncryptsSelectedChineseUtf8ByteRanges() {
        String prefix = "姓名：";
        String secret = "张三";
        String suffix = "，城市：太原";
        String source = prefix + secret + suffix;
        byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);
        int secretPosition = prefix.getBytes(StandardCharsets.UTF_8).length;
        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        Policy policy = Policy.of(new SubPolicy(secretPosition, secretLength));

        byte[] encrypted = CptJni.multiSubPolicyEncrypt(
                sourceBytes, policy, KEY, CptMode.CHAR);
        byte[] decrypted = CptJni.multiSubPolicyDecrypt(
                encrypted, policy, KEY, CptMode.CHAR);
        assertEquals(sourceBytes.length, encrypted.length);
        assertArrayEquals("姓名前缀不能被修改",
                prefix.getBytes(StandardCharsets.UTF_8),
                java.util.Arrays.copyOfRange(encrypted, 0, secretPosition));
        assertArrayEquals("姓名后缀不能被修改",
                suffix.getBytes(StandardCharsets.UTF_8),
                java.util.Arrays.copyOfRange(encrypted,
                        secretPosition + secretLength, encrypted.length));
        assertFalse("指定的中文姓名字节区间应被加密",
                java.util.Arrays.equals(
                        secret.getBytes(StandardCharsets.UTF_8),
                        java.util.Arrays.copyOfRange(encrypted,
                                secretPosition, secretPosition + secretLength)));
        assertArrayEquals("多策略中文字段必须能够完整还原", sourceBytes, decrypted);
        assertEquals(source, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    public void visibleAsciiModeLeavesChineseUtf8BytesUntouchedButEncryptsAscii() {
        String source = "订单编号ABC-2026-太原";
        byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = CptJni.encrypt(sourceBytes, KEY, CptMode.VISIBLE_ASCII);
        byte[] decrypted = CptJni.decrypt(encrypted, KEY, CptMode.VISIBLE_ASCII);

        assertEquals(sourceBytes.length, encrypted.length);
        assertFalse("ASCII 内容应被加密", java.util.Arrays.equals(sourceBytes, encrypted));
        assertArrayEquals("可见 ASCII 模式应完整恢复中英文混合字段", sourceBytes, decrypted);
        assertEquals(source, new String(decrypted, StandardCharsets.UTF_8));
    }
}
