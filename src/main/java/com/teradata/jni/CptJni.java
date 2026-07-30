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
// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space
// Source File Name:   CptJni.java

package com.teradata.jni;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CptJni {

    public CptJni() {
    }

    /**
     * 已废弃：legacy String API。
     *
     * <p>当 {@code containCn=true} 时，本方法将输入按 UTF-16BE 取字节（含 BOM 与大量 0x00），
     * 再用 ISO-8859-1 把密文回装为 String。这条路径仅适用于"把中文字符按双字节编码"的旧存储场景
     * （例如老 Teradata/GBK 流水线）。在 UTF-8 字符集的 Hive/Doris/GBase 列里使用会由于
     * UTF-8 校验失败而导致密文被替换为 {@code ?}、解密无法还原。
     *
     * <p>新代码请使用 {@link #encryptUtf8(String, long, CptMode)} 或直接调用
     * {@link #encrypt(byte[], long, CptMode)} 字节级 API。
     *
     * @param containCn true 走 UTF-16 路径（不推荐），false 走 ISO-8859-1 路径（仅限纯 ASCII 字段）
     */
    @Deprecated
    public static String encrypt(String s, long key, byte mod, boolean containCn)
            throws UnsupportedEncodingException {
        if (containCn) {
            return new String(encrypt(s.getBytes(StandardCharsets.UTF_16), key, mod), StandardCharsets.ISO_8859_1);
        } else {
            return new String(encrypt(s.getBytes(StandardCharsets.ISO_8859_1), key, mod), StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * 已废弃：legacy String API。语义与 {@link #encrypt(String, long, byte, boolean)} 对称。
     * 新代码请使用 {@link #decryptUtf8(String, long, CptMode)}。
     */
    @Deprecated
    public static String decrypt(String s, long key, byte mod, boolean containCn)
            throws UnsupportedEncodingException {
        if (containCn) {
            return new String(decrypt(s.getBytes(StandardCharsets.ISO_8859_1), key, mod), StandardCharsets.UTF_16);
        } else {
            return new String(decrypt(s.getBytes(StandardCharsets.ISO_8859_1), key, mod), StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * UTF-8 友好的加密入口（推荐用于 UTF-8 字符集的 Hive/Doris/GBase 列）。
     *
     * <p>等价于：
     * <pre>{@code
     *   byte[] in = s.getBytes(StandardCharsets.UTF_8);
     *   byte[] out = encrypt(in, key, mode);
     *   // 密文直接以字节写回 VARCHAR/BINARY 列，不再做 String 二次编码
     * }</pre>
     *
     * <p>返回值仍是 Java String，但内容是"密文 UTF-8 字节按 ISO-8859-1 回装"。
     * 这种做法只在你必须以 Java String 类型把密文传回 UDF 框架时使用；
     * 写入列时应当用 {@link #encryptAsUtf8Bytes(String, long, CptMode)} 拿到字节再写入，
     * 避免任何中间环节的字符集往返。
     *
     * @see #encryptAsUtf8Bytes(String, long, CptMode)
     */
    public static String encryptUtf8(String s, long key, CptMode mode) {
        requireStringAndMode(s, mode);
        byte[] in = s.getBytes(StandardCharsets.UTF_8);
        byte[] out = encrypt(in, key, mode);
        return new String(out, StandardCharsets.ISO_8859_1);
    }

    /**
     * {@link #encryptUtf8(String, long, CptMode)} 的解密对称操作。
     */
    public static String decryptUtf8(String s, long key, CptMode mode) {
        requireStringAndMode(s, mode);
        byte[] in = s.getBytes(StandardCharsets.ISO_8859_1);
        byte[] out = decrypt(in, key, mode);
        return new String(out, StandardCharsets.UTF_8);
    }

    /**
     * 输入字符串（按 UTF-8 解码），返回加密后的字节。写入 VARCHAR/BINARY 列前直接使用本返回值。
     *
     * <p>这是为 Hive/Doris/GBase UDF 推荐使用的入口：
     * <ul>
     *   <li>明文→UTF-8 字节→CHAR/DIGIT/VISIBLE_ASCII 加密→等长密文字节</li>
     *   <li>密文字节可直接写入 BINARY/VARBINARY 列</li>
     *   <li>或经 Base64/Hex 编码后再写入 VARCHAR 列</li>
     * </ul>
     */
    public static byte[] encryptAsUtf8Bytes(String s, long key, CptMode mode) {
        requireStringAndMode(s, mode);
        return encrypt(s.getBytes(StandardCharsets.UTF_8), key, mode);
    }

    /**
     * {@link #encryptAsUtf8Bytes(String, long, CptMode)} 的对称操作：密文字节→UTF-8 原文。
     */
    public static byte[] decryptToUtf8Bytes(String s, long key, CptMode mode) {
        requireStringAndMode(s, mode);
        return decrypt(s.getBytes(StandardCharsets.ISO_8859_1), key, mode);
    }

    private static void requireStringAndMode(String s, CptMode mode) {
        if (s == null) {
            throw new IllegalArgumentException("input string must not be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
    }

    public static native byte[] encrypt(byte abyte0[], long key, byte mod);

    public static native byte[] decrypt(byte abyte0[], long key, byte mod);

    public static byte[] encrypt(byte[] input, long key, CptMode mode) {
        requireInputAndMode(input, mode);
        return encrypt(input, key, mode.value());
    }

    public static byte[] decrypt(byte[] input, long key, CptMode mode) {
        requireInputAndMode(input, mode);
        return decrypt(input, key, mode.value());
    }


    /**
     * 已废弃：legacy String API（多区间策略）。仅限"双字节编码"旧存储场景，
     * 不适用于 UTF-8 字符集的 Hive/Doris/GBase 列。新代码请使用
     * {@link #multiSubPolicyEncrypt(byte[], Policy, long, CptMode)} 字节级重载。
     */
    @Deprecated
    public static String multiSubPolicyEncrypt(String s, Policy policy, long key, byte mod, boolean containCn)
            throws UnsupportedEncodingException {
        if (containCn) {
            return new String(multiSubPolicyEncrypt(s.getBytes(StandardCharsets.UTF_16), policy, key, mod), StandardCharsets.ISO_8859_1);
        } else {
            return new String(multiSubPolicyEncrypt(s.getBytes(StandardCharsets.ISO_8859_1), policy, key, mod), StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * 已废弃：legacy String API（多区间策略解密）。与
     * {@link #multiSubPolicyEncrypt(String, Policy, long, byte, boolean)} 对称。
     */
    @Deprecated
    public static String multiSubPolicyDecrypt(String s, Policy policy, long key, byte mod, boolean containCn)
            throws UnsupportedEncodingException {
        if (containCn) {
            return new String(multiSubPolicyDecrypt(s.getBytes(StandardCharsets.ISO_8859_1), policy, key, mod), StandardCharsets.UTF_16);
        } else {
            return new String(multiSubPolicyDecrypt(s.getBytes(StandardCharsets.ISO_8859_1), policy, key, mod), StandardCharsets.ISO_8859_1);
        }
    }

    //public static native byte[] multiSubPolicyEncrypt(String abyte0, Policy policy, long key, byte mod);
    public static native byte[] multiSubPolicyEncrypt(byte abyte0[], Policy policy, long key, byte mod);

    public static native byte[] multiSubPolicyDecrypt(byte abyte0[], Policy policy, long key, byte mod);

    public static byte[] multiSubPolicyEncrypt(byte[] input, Policy policy, long key, CptMode mode) {
        validatePolicyInput(input, policy, mode);
        return multiSubPolicyEncrypt(input, policy, key, mode.value());
    }

    public static byte[] multiSubPolicyDecrypt(byte[] input, Policy policy, long key, CptMode mode) {
        validatePolicyInput(input, policy, mode);
        return multiSubPolicyDecrypt(input, policy, key, mode.value());
    }

    private static void requireInputAndMode(byte[] input, CptMode mode) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
    }

    private static void validatePolicyInput(byte[] input, Policy policy, CptMode mode) {
        requireInputAndMode(input, mode);
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        policy.validateForLength(input.length);
    }

    static {
        String osType = System.getProperty("os.name");
        String cpuArch = System.getProperty("os.arch", "");
        try {
            if (osType.equals("Linux")) {
                if (cpuArch.equals("amd64") || cpuArch.equals("x86_64")) {
                    load("/libTeradataCptJniAmd64.so");
                } else if (cpuArch.equals("arm64") || cpuArch.equals("aarch64")) {
                    load("/libTeradataCptJniArm64.so");
                } else {
                    throw new IOException("unsupported Linux architecture: " + cpuArch);
                }
            } else if (osType.startsWith("Windows")) {
                load("/libTeradataCptJni.dll");
            } else {
                throw new IOException("unsupported operating system: " + osType);
            }
        } catch (IOException | UnsatisfiedLinkError error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void load(String path) throws IOException {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Wrong path :" + path);
        }
        //如果存在本文件，直接加载，并返回
        File inputFile = new File(path);
        if (inputFile.exists() && inputFile.isFile()) {
            System.load(path);
            return;
        }

        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("The fileName should not be null");
        }

        String prefix = fileName.substring(0, fileName.lastIndexOf("."));
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        //创建临时文件，注意删除
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();

        byte[] buff = new byte[1024];
        int len;
        OutputStream out = new FileOutputStream(tmp);
        //从jar中读取文件流
        InputStream in = CptJni.class.getResourceAsStream(path);

        if (in == null) {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            throw new IOException("not jar call or miss lib ");
        }

        try {
            while ((len = in.read(buff)) != -1) {
                out.write(buff, 0, len);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        //加载库文件
        System.load(tmp.getAbsolutePath());

    }


    public static void main(String[] args) {
        long key = 123;
        System.out.println("==================== 数字加密 =====================");
        byte mode = 1; // 数字加密
        String originText = "foxmindTeradataBonc123";

        try {
            System.out.println("origin:" + originText);
            String encryptText = encrypt(originText, key, mode, false);
            System.out.println("encrypt:" + encryptText);
            String decryptText = decrypt(encryptText, key, mode, false);
            System.out.println("decrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("==================== 字符串加密 =====================");
        mode = 0; // 字符串加密
        originText = "foxmindTeradataBonc123";
        try {
            System.out.println("origin:" + originText);
            String encryptText = encrypt(originText, key, mode, false);
            System.out.println("encrypt:" + encryptText);
            String decryptText = decrypt(encryptText, key, mode, false);
            System.out.println("decrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        System.out.println("==================== 可见单字节加密 =====================");
        mode = 2; // 可见单字节加密
        originText = "foxmindTeradataBonc123";
        try {
            System.out.println("origin:" + originText);
            String encryptText = encrypt(originText, key, mode, false);
            System.out.println("encrypt:" + encryptText);
            String decryptText = decrypt(encryptText, key, mode, false);
            System.out.println("decrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("==================== 中文字符串加密 =====================");
        mode = 0; // 字符串加密
        originText = "中";
        try {
            System.out.println("origin:" + originText);
            System.out.printf("加密前origin：");printString(originText);
            byte[] enUtf16Bytes = originText.getBytes(StandardCharsets.UTF_16);
//            System.out.println("enUtf16Bytes.getBytes:" + enUtf16Bytes);
            System.out.printf("加密前utf16：");printByteArray(enUtf16Bytes);
            byte[] enBytes = encrypt(enUtf16Bytes, key, mode);
            System.out.printf("加密后enBytes：");printByteArray(enBytes);
            String encrypt = new String(enBytes, StandardCharsets.ISO_8859_1);
            System.out.println("encrypt:" + encrypt);


            System.out.printf("getBytesFromEncrypt:");
            printByteArray(encrypt.getBytes(StandardCharsets.ISO_8859_1));


            byte[] deBytes = decrypt(enBytes, key, mode);
            System.out.printf("解密后deBytes：");printByteArray(deBytes);
            String deUtf16Bytes = new String(deBytes, StandardCharsets.UTF_16);
            System.out.println("decrypt:" + deUtf16Bytes);
            System.out.printf("解密后deUtf16Bytes：");printString(deUtf16Bytes);

            System.out.println("deUtf16Bytes.getBytes:" + deUtf16Bytes.getBytes());

            String utf8 = new String(deBytes, StandardCharsets.UTF_8);
            System.out.println("decrypt_utf8:" + utf8);
            System.out.printf("解密后utf8：");printString(utf8);

            byte[] utf8BytesFromGBKString = getUTF8BytesFromGBKString(deUtf16Bytes);
            System.out.printf("解密后utf8Bytes：");printByteArray(utf8BytesFromGBKString);
            String s = new String(utf8BytesFromGBKString, "UTF-8");
            System.out.println(s);

//            String decryptText = decrypt(encryptText, key, mode, true);
//            System.out.println("decrypt:" + decryptText);

            System.out.println("-------------------- 2 -------------------");
            originText = "胡正阳";
            System.out.println("加密myName:" + originText);
            String enMyName = encrypt(originText, key, mode, true);
            System.out.println("en my name:" + enMyName);
            String deMyName = decrypt(enMyName, key, mode, true);
            System.out.println("de my name:" + deMyName);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("#######################################################");

        System.out.println("==================== 数字加密 =====================");
        mode = 1; // 数字加密
        originText = "foxmind_1234_text_139";

        try {
            System.out.println("origin:" + originText);

            SubPolicy subPolicy1 = new SubPolicy();
            subPolicy1.position = 8;
            subPolicy1.length = 4;
            SubPolicy subPolicy2 = new SubPolicy();
            subPolicy2.position = 18;
            subPolicy2.length = 3;

            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};


            Policy policy = new Policy();
            policy.sub_policy_num = 2;
            policy.sub_policy = subPolicies;


            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode, false);
            System.out.println("multiSubPolicyEncrypt:" + encryptText);
            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode, false);
            System.out.println("multiSubPolicyDecrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("==================== 字符串加密 =====================");
        mode = 0; // 字符串加密
        originText = "foxmindTeradataBonc123";
        try {
            System.out.println("origin:" + originText);

            SubPolicy subPolicy1 = new SubPolicy();
            subPolicy1.position = 7;
            subPolicy1.length = 8;
            SubPolicy subPolicy2 = new SubPolicy();
            subPolicy2.position = 15;
            subPolicy2.length = 4;

            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};


            Policy policy = new Policy();
            policy.sub_policy_num = 2;
            policy.sub_policy = subPolicies;


            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode, false);
            System.out.println("multiSubPolicyEncrypt:" + encryptText);
            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode, false);
            System.out.println("multiSubPolicyDecrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//
//
        System.out.println("==================== 可见单字节加密 =====================");
        mode = 2; // 可见单字节加密
        originText = "foxmindTeradataBonc123";
        try {
            System.out.println("origin:" + originText);

            SubPolicy subPolicy1 = new SubPolicy();
            subPolicy1.position = 7;
            subPolicy1.length = 8;
            SubPolicy subPolicy2 = new SubPolicy();
            subPolicy2.position = 15;
            subPolicy2.length = 4;

            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};


            Policy policy = new Policy();
            policy.sub_policy_num = 2;
            policy.sub_policy = subPolicies;


            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode, false);
            System.out.println("multiSubPolicyEncrypt:" + encryptText);
            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode, false);
            System.out.println("multiSubPolicyDecrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        System.out.println("==================== 中文字符串加密 =====================");
        mode = 0; // 字符串加密
        originText = "中文字符串加密";
        System.out.printf("Java watch:");printString(originText);
        try {
            System.out.println("origin:" + originText);

//            byte[] gb2312s = originText.getBytes("GB2312");
//            System.out.printf("加密前gb2312：");printByteArray(gb2312s);


            SubPolicy subPolicy1 = new SubPolicy();
            subPolicy1.position = 2;
            subPolicy1.length = 2;
            SubPolicy subPolicy2 = new SubPolicy();
            subPolicy2.position = 6;
            subPolicy2.length = 2;

            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};


            Policy policy = new Policy();
            policy.sub_policy_num = 2;
            policy.sub_policy = subPolicies;


//            byte[] bytes = multiSubPolicyEncrypt(gb2312s, policy, key, mode);
//            System.out.printf("加密完成后：");printByteArray(bytes);
//            System.out.println("multiSubPolicyEncrypt:" + new String(bytes));

            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode, true);
            System.out.println("multiSubPolicyEncrypt:" + encryptText);

            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode, true);
            System.out.println("multiSubPolicyDecrypt:" + decryptText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printString(String inStr) {
        byte[] inBytes = inStr.getBytes();
        StringBuffer sb = new StringBuffer();
        System.out.printf("printInJava:");
        for (int i = 0; i < inBytes.length; i++) {
            String hexChar = Integer.toHexString(inBytes[i] & 0xFF);
            System.out.printf("%s ", hexChar);
        }
        System.out.println();
    }

    private static void printByteArray(byte[] inBytes) {
        System.out.printf("printByteArray:");
        for (int i = 0; i < inBytes.length; i++) {
            String hexChar = Integer.toHexString(inBytes[i] & 0xFF);
            System.out.printf("%s ", hexChar);
        }
        System.out.println();
    }

    public static byte[] getUTF8BytesFromGBKString(String gbkStr) {
        int n = gbkStr.length();
        byte[] utfBytes = new byte[3 * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int m = gbkStr.charAt(i);
            if (m < 128 && m >= 0) {
                utfBytes[k++] = (byte) m;
                continue;
            }
            utfBytes[k++] = (byte) (0xe0 | (m >> 12));
            utfBytes[k++] = (byte) (0x80 | ((m >> 6) & 0x3f));
            utfBytes[k++] = (byte) (0x80 | (m & 0x3f));
        }
        if (k < utfBytes.length) {
            byte[] tmp = new byte[k];
            System.arraycopy(utfBytes, 0, tmp, 0, k);
            return tmp;
        }
        return utfBytes;
    }

//    private String encrypt(String originText) {
//        System.out.println("origin:" + originText);
//        System.out.printf("加密前origin：");printString(originText);
//        byte[] enUtf16Bytes = originText.getBytes(StandardCharsets.UTF_16);
////            System.out.println("enUtf16Bytes.getBytes:" + enUtf16Bytes);
//        System.out.printf("加密前utf16：");printByteArray(enUtf16Bytes);
//        byte[] enBytes = encrypt(enUtf16Bytes, key, mode);
//        System.out.printf("加密后enBytes：");printByteArray(enBytes);
//        return new String(enBytes);
//    }

}
