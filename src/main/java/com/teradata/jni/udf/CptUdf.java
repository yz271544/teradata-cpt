/*
Copyright [2020] [lyndon]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
*/
package com.teradata.jni.udf;

import com.teradata.jni.CptJni;
import com.teradata.jni.CptMode;
import com.teradata.jni.Policy;
import com.teradata.jni.SubPolicy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * CPT 作为 Hive / Doris / GBase UDF 时的字节级参考实现。
 *
 * <p>本类不绑定任何具体平台的 UDF SDK（{@code org.apache.hadoop.hive.ql.exec.UDF}、
 * {@code org.apache.doris.udf.UDF}、{@code com.gbase.jdbc.udf.UDF} 等），
 * 只把"原始字节 → 加密/解密字节 → 写到存储层"的字节级契约固定下来，
 * 平台适配层只需在 5–10 行内把这几个方法包成对应平台的 UDF 类。
 *
 * <h2>设计原则</h2>
 * <ol>
 *   <li>所有方法输入输出都是 {@code byte[]}，避免任何编码转换</li>
 *   <li>加密前后字节长度严格相等（CPT 的算法契约）</li>
 *   <li>DIGIT / VISIBLE_ASCII 模式下 UTF-8 高字节原样透传，UTF-8 解码合法性保持</li>
 *   <li>CHAR 模式密文是合法 UTF-8 字节流，但仍可经 Base64/Hex 编码再写 VARCHAR 列</li>
 * </ol>
 *
 * <h2>典型用法</h2>
 *
 * <h3>Hive</h3>
 * <pre>{@code
 *   public class CptEncryptUdf extends UDF {
 *       public Text evaluate(Text input, long key) {
 *           if (input == null) return null;
 *           byte[] in = input.getBytes();
 *           byte[] out = CptUdf.encryptAsBytes(in, key, CptMode.CHAR);
 *           return new Text(Base64.getEncoder().encode(out));   // 写到 VARCHAR
 *       }
 *   }
 * }</pre>
 *
 * <h3>Doris</h3>
 * <pre>{@code
 *   public class CptEncryptUdf implements UDF {
 *       public String evaluate(String input, Long key) {
 *           byte[] in = input.getBytes(StandardCharsets.UTF_8);
 *           byte[] out = CptUdf.encryptAsBytes(in, key, CptMode.CHAR);
 *           return Base64.getEncoder().encodeToString(out);
 *       }
 *   }
 * }</pre>
 */
public final class CptUdf {

    private CptUdf() {
    }

    // ---------------------------------------------------------------
    // 1. 字节级 API：UDF 内核，整个 SDK 只暴露这层
    // ---------------------------------------------------------------

    /**
     * 加密。等长返回，{@code out.length == in.length}。
     */
    public static byte[] encryptAsBytes(byte[] in, long key, CptMode mode) {
        requireNonNull(in, "input");
        requireNonNull(mode, "mode");
        return CptJni.encrypt(in, key, mode);
    }

    /**
     * 解密。等长返回，{@code out.length == in.length}。
     */
    public static byte[] decryptAsBytes(byte[] in, long key, CptMode mode) {
        requireNonNull(in, "input");
        requireNonNull(mode, "mode");
        return CptJni.decrypt(in, key, mode);
    }

    /**
     * 多区间策略加密。等长返回。
     */
    public static byte[] encryptAsBytes(byte[] in, Policy policy, long key, CptMode mode) {
        requireNonNull(in, "input");
        requireNonNull(policy, "policy");
        requireNonNull(mode, "mode");
        return CptJni.multiSubPolicyEncrypt(in, policy, key, mode);
    }

    /**
     * 多区间策略解密。等长返回。
     */
    public static byte[] decryptAsBytes(byte[] in, Policy policy, long key, CptMode mode) {
        requireNonNull(in, "input");
        requireNonNull(policy, "policy");
        requireNonNull(mode, "mode");
        return CptJni.multiSubPolicyDecrypt(in, policy, key, mode);
    }

    // ---------------------------------------------------------------
    // 2. UTF-8 字符串直通：UDF 最常见的入参形态
    // ---------------------------------------------------------------

    /**
     * 把一个 Java String 当作 UTF-8 文本加密，返回等长密文字节。
     *
     * <p>UDF 适配层通常这样用：
     * <pre>{@code
     *   public Text evaluate(Text input, long key) {
     *       byte[] in = input.getBytes();                      // Hive Text 默认 UTF-8
     *       byte[] out = CptUdf.encryptUtf8Field(in, key, CptMode.CHAR);
     *       return new Text(out);                              // 写 BINARY 列
     *       // 或 return new Text(Base64.getEncoder().encode(out));   // 写 VARCHAR
     *   }
     * }</pre>
     */
    public static byte[] encryptUtf8Field(byte[] utf8Bytes, long key, CptMode mode) {
        return encryptAsBytes(utf8Bytes, key, mode);
    }

    /**
     * {@link #encryptUtf8Field(byte[], long, CptMode)} 的对称操作。
     */
    public static byte[] decryptUtf8Field(byte[] cipherBytes, long key, CptMode mode) {
        return decryptAsBytes(cipherBytes, key, mode);
    }

    // ---------------------------------------------------------------
    // 3. Base64 友好包装：用于必须写 VARCHAR 列的场景
    // ---------------------------------------------------------------

    /**
     * 加密并 Base64 编码。返回值只含 ASCII 字符，
     * 可以无歧义地存入任意 UTF-8 VARCHAR 列。
     *
     * <p>代价：长度膨胀约 4/3。列宽要相应调大。
     */
    public static String encryptAsBase64(String utf8Text, long key, CptMode mode) {
        requireNonNull(utf8Text, "utf8Text");
        byte[] in = utf8Text.getBytes(StandardCharsets.UTF_8);
        byte[] out = encryptAsBytes(in, key, mode);
        return Base64.getEncoder().encodeToString(out);
    }

    /**
     * {@link #encryptAsBase64(String, long, CptMode)} 的对称操作。
     */
    public static String decryptFromBase64(String base64Cipher, long key, CptMode mode) {
        requireNonNull(base64Cipher, "base64Cipher");
        byte[] in = Base64.getDecoder().decode(base64Cipher);
        byte[] out = decryptAsBytes(in, key, mode);
        return new String(out, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------
    // 4. 半角字符处理：策略位置的计算工具
    // ---------------------------------------------------------------

    /**
     * 计算 UTF-8 文本中目标子串对应的字节区间 [position, position+length)。
     * 区间按 UTF-8 字节序列计算，与 {@link SubPolicy#position} / {@link SubPolicy#length}
     * 的"字节偏移"语义一致。
     *
     * <p>典型用法：加密一个固定结构字段中的敏感子段（如"姓名：xxx，城市：yyy"中的姓名）：
     * <pre>{@code
     *   String source = "姓名：张三，城市：太原";
     *   int[] nameRange = CptUdf.utf8ByteRange(source, "姓名：", "，城市");
     *   Policy policy = Policy.of(new SubPolicy(nameRange[0], nameRange[1]));
     *   byte[] enc = CptUdf.encryptAsBytes(source.getBytes(UTF_8), policy, key, CptMode.CHAR);
     * }</pre>
     */
    public static int[] utf8ByteRange(String source, String prefix, String suffix) {
        requireNonNull(source, "source");
        requireNonNull(prefix, "prefix");
        requireNonNull(suffix, "suffix");
        int prefixEnd = source.indexOf(prefix);
        if (prefixEnd < 0) {
            throw new IllegalArgumentException("prefix not found: " + prefix);
        }
        int secretStart = prefixEnd + prefix.length();
        int suffixStart = source.indexOf(suffix, secretStart);
        if (suffixStart < 0) {
            throw new IllegalArgumentException("suffix not found after prefix: " + suffix);
        }
        int secretByteStart = source.substring(0, secretStart).getBytes(StandardCharsets.UTF_8).length;
        int suffixByteStart = source.substring(0, suffixStart).getBytes(StandardCharsets.UTF_8).length;
        return new int[]{secretByteStart, suffixByteStart - secretByteStart};
    }

    // ---------------------------------------------------------------
    // 5. 校验：帮 UDF 适配层快速暴露问题原因
    // ---------------------------------------------------------------

    /**
     * 校验密文是不是 UTF-8 合法字节流（用于 CHAR 模式写入 VARCHAR 前的健全性检查）。
     */
    public static boolean isValidUtf8(byte[] bytes) {
        requireNonNull(bytes, "bytes");
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            if (b < 0x80) {
                i++;
            } else if ((b & 0xE0) == 0xC0) {
                if (i + 1 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if ((b & 0xF0) == 0xE0) {
                if (i + 2 >= bytes.length
                        || (bytes[i + 1] & 0xC0) != 0x80
                        || (bytes[i + 2] & 0xC0) != 0x80) return false;
                i += 3;
            } else if ((b & 0xF8) == 0xF0) {
                if (i + 3 >= bytes.length
                        || (bytes[i + 1] & 0xC0) != 0x80
                        || (bytes[i + 2] & 0xC0) != 0x80
                        || (bytes[i + 3] & 0xC0) != 0x80) return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}