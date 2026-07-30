package com.teradata.jni.udf;

import com.teradata.jni.CptMode;
import com.teradata.jni.Policy;
import com.teradata.jni.SubPolicy;
import com.teradata.jni.util.HexOutput;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 模拟"UDF 返回值写入 UTF-8 VARCHAR 列，再读出来"的全链路测试。
 *
 * <p>用 {@code String(bytes, UTF_8) → getBytes(UTF_8)} 来模拟数仓的 JDBC 驱动/引擎
 * 对 VARCHAR(utf8) 列做的 UTF-8 序列化与反序列化。
 * 任何通过此往返的字节流都能安全写入 UTF-8 列；任何破坏此往返的字节流都不能直接写入。
 *
 * <p>结论（详见 {@code docs/woolly-swimming-duckling.md}）：
 * <ul>
 *   <li>DIGIT / VISIBLE_ASCII 模式：密文是合法 UTF-8 字节流，可以直接写 VARCHAR(utf8) 列</li>
 *   <li>CHAR 模式：密文字节序列不一定合法 UTF-8，必须先 Base64 编码或改用 BINARY 列</li>
 *   <li>legacy String API {@code containCn=true}：含 0xFE/0xFF，必然被 UTF-8 解码破坏</li>
 * </ul>
 */
public class CptUdfStorageTest {

    private static final long KEY = 123L;
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final Charset LATIN1 = StandardCharsets.ISO_8859_1;

    // ===============================================================
    // 1. CHAR 模式：不能直接写 VARCHAR 列，必须 Base64 或 BINARY 列
    // ===============================================================

    @Test
    public void charModeUtf8CipherCannotBeWrittenDirectlyToVarcharColumn() {
        // 反例：CHAR 模式密文里每个字节单独看是合法 UTF-8 字节值（0x21-0xFF），
        // 但字节序列不一定合法 UTF-8——CHAR 模式逐字节独立查表替换，
        // 会破坏原有的多字节对齐结构。任何 >= 0x80 的字节若不与前面字节组成合法的
        // 多字节序列，UTF-8 解码器会插入 U+FFFD（EF BF BD），导致密文字节流变长、
        // 解密失败。
        String source = "山西太原数据仓库";
        byte[] utf8In = source.getBytes(UTF8);
        byte[] cipherBytes = CptUdf.encryptUtf8Field(utf8In, KEY, CptMode.CHAR);

        // 密文含 >= 0x80 的高位字节
        boolean hasHighBytes = false;
        for (byte b : cipherBytes) {
            if ((b & 0xFF) >= 0x80) {
                hasHighBytes = true;
                break;
            }
        }
        assertTrue("CHAR 模式密文应包含高位字节", hasHighBytes);

        // 模拟写入 UTF-8 VARCHAR 列：new String(bytes, UTF_8) 会插入 U+FFFD
        String varcharCell = new String(cipherBytes, UTF8);
        byte[] readBack = varcharCell.getBytes(UTF8);

        boolean lengthChanged = readBack.length != cipherBytes.length;
        boolean contentChanged = !Arrays.equals(readBack, cipherBytes);
        System.out.println("CHAR 模式密文长度=" + cipherBytes.length
                + ", UTF-8 往返后长度=" + readBack.length
                + ", lengthChanged=" + lengthChanged
                + ", contentChanged=" + contentChanged);
        assertTrue("CHAR 模式密文经 UTF-8 往返后必然变化（不能直接写 VARCHAR 列）",
                lengthChanged || contentChanged);

        // CHAR 模式 + VARCHAR 列的安全路径：Base64 编码
        String safe = CptUdf.encryptAsBase64(source, KEY, CptMode.CHAR);
        for (int i = 0; i < safe.length(); i++) {
            assertTrue("Base64 必须是 ASCII", safe.charAt(i) < 0x80);
        }
        assertEquals(source, CptUdf.decryptFromBase64(safe, KEY, CptMode.CHAR));
    }

    @Test
    public void charModeBinaryColumnRoundTripWorksWithoutEncoding() {
        // 对照组：CHAR 模式密文如果走 BINARY/VARBINARY 列（无 UTF-8 校验），完全无损。
        String source = "订单编号ABC-2026-太原客户张三";
        byte[] utf8In = source.getBytes(UTF8);
        byte[] cipherBytes = CptUdf.encryptUtf8Field(utf8In, KEY, CptMode.CHAR);
        assertEquals("CHAR 模式字节长度不变", utf8In.length, cipherBytes.length);

        // 模拟 BINARY 列：直接字节读写，不做任何字符集转换
        byte[] readBack = cipherBytes.clone();
        assertArrayEquals("BINARY 列密文直接字节往返",
                cipherBytes, readBack);

        byte[] decrypted = CptUdf.decryptUtf8Field(readBack, KEY, CptMode.CHAR);
        assertEquals(source, new String(decrypted, UTF8));
    }

    // ===============================================================
    // 2. DIGIT / VISIBLE_ASCII 模式：中文原样保留，可以直接写 VARCHAR 列
    // ===============================================================

    @Test
    public void digitModeUtf8FieldRoundTripsThroughVarcharSerialization() {
        String source = "客户张三手机号13800138000归属地北京";
        byte[] utf8In = source.getBytes(UTF8);

        byte[] cipherBytes = CptUdf.encryptUtf8Field(utf8In, KEY, CptMode.DIGIT);
        assertEquals("DIGIT 模式保持字节长度不变", utf8In.length, cipherBytes.length);

        // 中文/非数字部分应当逐字节等于原文
        String prefix = "客户张三手机号";
        byte[] prefixBytes = prefix.getBytes(UTF8);
        byte[] cipherPrefix = Arrays.copyOfRange(cipherBytes, 0, prefixBytes.length);
        assertArrayEquals("DIGIT 模式中文/非数字部分原样保留", prefixBytes, cipherPrefix);

        // UTF-8 VARCHAR 序列化往返
        String varcharCell = new String(cipherBytes, UTF8);
        byte[] readBack = varcharCell.getBytes(UTF8);
        assertArrayEquals("UTF-8 VARCHAR 写入读回字节必须与密文完全一致",
                cipherBytes, readBack);

        // 解密还原
        byte[] decrypted = CptUdf.decryptUtf8Field(readBack, KEY, CptMode.DIGIT);
        assertEquals(source, new String(decrypted, UTF8));
    }

    @Test
    public void visibleAsciiModeUtf8FieldRoundTripsThroughVarcharSerialization() {
        String source = "订单编号ABC-2026-太原";
        byte[] utf8In = source.getBytes(UTF8);

        byte[] cipherBytes = CptUdf.encryptUtf8Field(utf8In, KEY, CptMode.VISIBLE_ASCII);
        assertEquals(utf8In.length, cipherBytes.length);

        String varcharCell = new String(cipherBytes, UTF8);
        byte[] readBack = varcharCell.getBytes(UTF8);
        assertArrayEquals("UTF-8 VARCHAR 写入读回字节必须与密文完全一致",
                cipherBytes, readBack);

        byte[] decrypted = CptUdf.decryptUtf8Field(readBack, KEY, CptMode.VISIBLE_ASCII);
        assertEquals(source, new String(decrypted, UTF8));
    }

    // ===============================================================
    // 3. Base64 包装方案：CHAR 模式写 VARCHAR 的最稳方案
    // ===============================================================

    @Test
    public void base64WrappedUtf8FieldRoundTrips() {
        String source = "山西太原数据仓库ABC-2026";
        String base64Cipher = CptUdf.encryptAsBase64(source, KEY, CptMode.CHAR);

        for (int i = 0; i < base64Cipher.length(); i++) {
            char c = base64Cipher.charAt(i);
            assertTrue("Base64 字符必须是 ASCII: " + c, c < 0x80);
        }

        // 模拟列往返：Base64 字符串经 UTF-8 写出再读回，字节完全一致
        byte[] asUtf8 = base64Cipher.getBytes(UTF8);
        String readBack = new String(asUtf8, UTF8);
        assertEquals(base64Cipher, readBack);

        String plain = CptUdf.decryptFromBase64(readBack, KEY, CptMode.CHAR);
        assertEquals(source, plain);
    }

    @Test
    public void base64CipherIsByteEquivalentToDirectUtf8Cipher() {
        String source = "胡正阳123abc";
        byte[] utf8In = source.getBytes(UTF8);
        byte[] directCipher = CptUdf.encryptUtf8Field(utf8In, KEY, CptMode.CHAR);

        String base64 = CptUdf.encryptAsBase64(source, KEY, CptMode.CHAR);
        byte[] decoded = Base64.getDecoder().decode(base64);

        assertArrayEquals("Base64 解码字节必须等于直接加密字节",
                directCipher, decoded);
    }

    // ===============================================================
    // 4. 多区间策略：DIGIT 模式加密中文子段（保留边界中文，写 VARCHAR 安全）
    // ===============================================================

    @Test
    public void multiPolicyUtf8ByteRangeRoundTrip() {
        String prefix = "姓名：";
        String secret = "张三";
        String suffix = "，城市：太原";
        String source = prefix + secret + suffix;

        int[] range = CptUdf.utf8ByteRange(source, prefix, "，城市");
        assertEquals("name range length", secret.getBytes(UTF8).length, range[1]);

        Policy policy = Policy.of(new SubPolicy(range[0], range[1]));
        byte[] cipherBytes = CptUdf.encryptAsBytes(
                source.getBytes(UTF8), policy, KEY, CptMode.CHAR);

        // CHAR 模式多区间策略密文同样不能直接写 VARCHAR（参见上面 CHAR 测试）
        // 所以这里用 Base64 包装来走 VARCHAR
        String base64 = Base64.getEncoder().encodeToString(cipherBytes);
        byte[] readBack = Base64.getDecoder().decode(
                new String(base64.getBytes(UTF8), UTF8));

        byte[] decrypted = CptUdf.decryptAsBytes(readBack, policy, KEY, CptMode.CHAR);
        assertEquals(source, new String(decrypted, UTF8));
    }

    @Test
    public void multiPolicyDigitModeUtf8SubRangeRoundTripsDirectlyToVarchar() {
        // DIGIT 模式 + 多区间策略：只加密指定 UTF-8 字节区间，原 UTF-8 结构不破坏
        String source = "客户张三13800138000北京";
        int secretStart = "客户".getBytes(UTF8).length;
        int secretLen = "张三".getBytes(UTF8).length;

        Policy policy = Policy.of(new SubPolicy(secretStart, secretLen));
        byte[] cipherBytes = CptUdf.encryptAsBytes(
                source.getBytes(UTF8), policy, KEY, CptMode.DIGIT);

        // DIGIT 模式：中文不会被加密（不在字符表 0x30-0x39 内），密文里"客户张三"部分原样保留
        byte[] prefixBytes = "客户".getBytes(UTF8);
        byte[] cipherPrefix = Arrays.copyOfRange(cipherBytes, 0, prefixBytes.length);
        assertArrayEquals(prefixBytes, cipherPrefix);

        // UTF-8 VARCHAR 往返
        String varcharCell = new String(cipherBytes, UTF8);
        byte[] readBack = varcharCell.getBytes(UTF8);
        assertArrayEquals(cipherBytes, readBack);

        byte[] decrypted = CptUdf.decryptAsBytes(readBack, policy, KEY, CptMode.DIGIT);
        assertEquals(source, new String(decrypted, UTF8));
    }

    // ===============================================================
    // 5. 等长契约的硬验证
    // ===============================================================

    @Test
    public void allModesPreserveByteLengthForUtf8Chinese() {
        String source = "ABC中文123-DEF中文456";
        byte[] utf8In = source.getBytes(UTF8);
        System.out.println("UTF-8 source bytes: " + HexOutput.bytesToHex(utf8In));

        for (CptMode mode : new CptMode[]{CptMode.CHAR, CptMode.DIGIT, CptMode.VISIBLE_ASCII}) {
            byte[] enc = CptUdf.encryptUtf8Field(utf8In, KEY, mode);
            byte[] dec = CptUdf.decryptUtf8Field(enc, KEY, mode);

            assertEquals(mode + " 模式必须保持字节长度",
                    utf8In.length, enc.length);
            assertArrayEquals(mode + " 模式必须可逆还原原文", utf8In, dec);
        }
    }

    @Test
    public void cipherDoesNotEqualPlaintextInChineseSection() {
        String source = "客户张三";
        byte[] utf8In = source.getBytes(UTF8);
        byte[] cipherBytes = CptUdf.encryptUtf8Field(utf8In, KEY, CptMode.CHAR);
        assertFalse("CHAR 模式密文不应等于原文",
                Arrays.equals(utf8In, cipherBytes));
    }

    // ===============================================================
    // 6. legacy String API 反例：containCn=true 在 UTF-8 列里必然被破坏
    // ===============================================================

    @Test
    public void legacyUtf16StringApiBreaksUnderUtf8VarcharSerialization() {
        // 反例：legacy `containCn=true` 路径产生含 0xFE/0xFF 的密文，
        // 模拟写入 UTF-8 VARCHAR 列时必然被 UTF-8 解码器替换为 U+FFFD。
        String source = "张三";
        byte[] utf16Bytes = source.getBytes(StandardCharsets.UTF_16);

        byte[] cipherBytes = com.teradata.jni.CptJni.encrypt(utf16Bytes, KEY, (byte) 0);

        // legacy 路径密文里 0xFE/0xFF（BOM）经 UTF-8 解码必然被替换
        String varcharCell = new String(cipherBytes, UTF8);
        byte[] readBack = varcharCell.getBytes(UTF8);

        boolean changed = !Arrays.equals(cipherBytes, readBack);
        System.out.println("legacy 路径密文经 UTF-8 往返是否变化: " + changed
                + ", 原长度=" + cipherBytes.length + ", 后长度=" + readBack.length);
        assertTrue("legacy `containCn=true` 密文经 UTF-8 往返必然变化（密文被破坏）", changed);
    }
}