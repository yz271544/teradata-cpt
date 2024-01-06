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

    public static String encrypt(String s, long key, byte mod)
            throws UnsupportedEncodingException {
        return new String(encrypt(s.getBytes(StandardCharsets.UTF_16), key, mod), StandardCharsets.ISO_8859_1);
    }

    public static String decrypt(String s, long key, byte mod)
            throws UnsupportedEncodingException {
        return new String(decrypt(s.getBytes(StandardCharsets.ISO_8859_1), key, mod), StandardCharsets.UTF_16);
    }

    public static native byte[] encrypt(byte abyte0[], long key, byte mod);

    public static native byte[] decrypt(byte abyte0[], long key, byte mod);


    public static String multiSubPolicyEncrypt(String s, Policy policy, long key, byte mod)
            throws UnsupportedEncodingException {
        return new String(multiSubPolicyEncrypt(s.getBytes(StandardCharsets.UTF_16), policy, key, mod), StandardCharsets.ISO_8859_1);
    }

    public static String multiSubPolicyDecrypt(String s, Policy policy, long key, byte mod)
            throws UnsupportedEncodingException {
        return new String(multiSubPolicyDecrypt(s.getBytes(StandardCharsets.ISO_8859_1), policy, key, mod), StandardCharsets.UTF_16);
    }

    //public static native byte[] multiSubPolicyEncrypt(String abyte0, Policy policy, long key, byte mod);
    public static native byte[] multiSubPolicyEncrypt(byte abyte0[], Policy policy, long key, byte mod);

    public static native byte[] multiSubPolicyDecrypt(byte abyte0[], Policy policy, long key, byte mod);

    static {
        //System.getProperties().forEach((k, v) -> System.out.println(k + " --> " + v));

        String osType = System.getProperty("os.name");
        String osVersion = System.getProperty("sun.os.patch.level");//得到操作系统版本
        String cpuArch = "";

        try {
            /*System.out.println("library path:"+System.getProperty("java.library.path"));
            System.out.println("-------------------------");
            System.out.println(System.getProperties());*/
            //System.err.println("jar load failed!!!!!!!!!!!!!!!!!!");

            if (osType.equals("Linux")) {
                cpuArch = System.getProperty("os.arch");//得到CPU系统信息
                System.out.println("osType:" + osType + " osVersion:" + osVersion + " cpuArch:" + cpuArch);
                // 需要设置环境变量 .bashrc
                // export LD_LIBRARY_PATH='/home/etl/iProject/TeradataCptJni/linux-amd64':${LD_LIBRARY_PATH}
                // export PATH=${JAVA_HOME}/bin:${LD_LIBRARY_PATH}:${PATH}
                if (cpuArch.equals("amd64")) {
                    System.loadLibrary("TeradataCptJniAmd64");
                } else if (cpuArch.equals("arm64")) {
                    System.loadLibrary("TeradataCptJniArm64");
                } else {
                    throw new RuntimeException("unknown cpu.isalist:" + cpuArch);
                }
            } else if (osType.startsWith("Windows")) {
                cpuArch = System.getProperty("sun.cpu.isalist");//得到CPU系统信息
                System.out.println("osType:" + osType + " osVersion:" + osVersion + " cpuArch:" + cpuArch);
                // 需要设置环境变量PATH=${PATH}\;D:\iProject\teradatacpt\lib
                System.loadLibrary("libTeradataCptJni");
            } else {
                throw new RuntimeException("unknown os.name:" + osType);
            }
        } catch (UnsatisfiedLinkError unsatisfiedlinkerror) {
            String libName = "/";
            if (osType.equals("Linux")) {
                if (cpuArch.equals("amd64")) {
                    libName = "/libTeradataCptJniAmd64.so";
                } else if (cpuArch.equals("arm64")) {
                    libName = "/libTeradataCptJniArm64.so";
                } else {
                    throw new RuntimeException("unknown cpu.isalist:" + cpuArch);
                }
            } else if (osType.startsWith("Windows")) {
                libName = "/libTeradataCptJni.dll";
            } else {
                throw new RuntimeException("unknown os.name:" + osType);
            }
            try {
                load(libName);
            } catch (IOException ioException) {
                System.err.println("Cannot load libTeradataCptJni library:\n " + unsatisfiedlinkerror.toString());
            }
        }
        //System.load("D:\\iProject\\teradatacpt\\lib\\libTeradataCptJni.dll");
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

        String prefix = fileName.substring(0, fileName.lastIndexOf(".") - 1);
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        //创建临时文件，注意删除
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();
        System.out.println(tmp.getAbsolutePath());

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
            String encryptText = encrypt(originText, key, mode);
            System.out.println("encrypt:" + encryptText);
            String decryptText = decrypt(encryptText, key, mode);
            System.out.println("decrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

//        System.out.println("==================== 字符串加密 =====================");
//        mode = 0; // 字符串加密
//        originText = "foxmindTeradataBonc123";
//        try {
//            System.out.println("origin:" + originText);
//            String encryptText = encrypt(originText, key, mode);
//            System.out.println("encrypt:" + encryptText);
//            String decryptText = decrypt(encryptText, key, mode);
//            System.out.println("decrypt:" + decryptText);
//
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }


//        System.out.println("==================== 可见单字节加密 =====================");
//        mode = 2; // 可见单字节加密
//        originText = "foxmindTeradataBonc123";
//        try {
//            System.out.println("origin:" + originText);
//            String encryptText = encrypt(originText, key, mode);
//            System.out.println("encrypt:" + encryptText);
//            String decryptText = decrypt(encryptText, key, mode);
//            System.out.println("decrypt:" + decryptText);
//
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

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

//            System.out.println("deUtf16Bytes.getBytes:" + deUtf16Bytes.getBytes());
//
//            String utf8 = new String(deBytes, StandardCharsets.UTF_8);
//            System.out.println("decrypt_utf8:" + utf8);
//            System.out.printf("解密后utf8：");printString(utf8);
//
//            byte[] utf8BytesFromGBKString = getUTF8BytesFromGBKString(deUtf16Bytes);
//            System.out.printf("解密后utf8Bytes：");printByteArray(utf8BytesFromGBKString);
//            String s = new String(utf8BytesFromGBKString, "UTF-8");
//            System.out.println(s);

//            String decryptText = decrypt(encryptText, key, mode);
//            System.out.println("decrypt:" + decryptText);

//            System.out.println("-------------------- 2 -------------------");
            originText = "胡正阳";
            System.out.println("加密myName:" + originText);
            String enMyName = encrypt(originText, key, mode);
            System.out.println("en my name:" + enMyName);
            String deMyName = decrypt(enMyName, key, mode);
            System.out.println("de my name:" + deMyName);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("#######################################################");

//        System.out.println("==================== 数字加密 =====================");
//        mode = 1; // 数字加密
//        originText = "foxmind_1234_text_139";
//
//        try {
//            System.out.println("origin:" + originText);
//
//            SubPolicy subPolicy1 = new SubPolicy();
//            subPolicy1.position = 8;
//            subPolicy1.length = 4;
//            SubPolicy subPolicy2 = new SubPolicy();
//            subPolicy2.position = 18;
//            subPolicy2.length = 3;
//
//            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};
//
//
//            Policy policy = new Policy();
//            policy.sub_policy_num = 2;
//            policy.sub_policy = subPolicies;
//
//
//            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode);
//            System.out.println("multiSubPolicyEncrypt:" + encryptText);
//            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode);
//            System.out.println("multiSubPolicyDecrypt:" + decryptText);
//
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("==================== 字符串加密 =====================");
//        mode = 0; // 字符串加密
//        originText = "foxmindTeradataBonc123";
//        try {
//            System.out.println("origin:" + originText);
//
//            SubPolicy subPolicy1 = new SubPolicy();
//            subPolicy1.position = 7;
//            subPolicy1.length = 8;
//            SubPolicy subPolicy2 = new SubPolicy();
//            subPolicy2.position = 15;
//            subPolicy2.length = 4;
//
//            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};
//
//
//            Policy policy = new Policy();
//            policy.sub_policy_num = 2;
//            policy.sub_policy = subPolicies;
//
//
//            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode);
//            System.out.println("multiSubPolicyEncrypt:" + encryptText);
//            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode);
//            System.out.println("multiSubPolicyDecrypt:" + decryptText);
//
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//
//        System.out.println("==================== 可见单字节加密 =====================");
//        mode = 2; // 可见单字节加密
//        originText = "foxmindTeradataBonc123";
//        try {
//            System.out.println("origin:" + originText);
//
//            SubPolicy subPolicy1 = new SubPolicy();
//            subPolicy1.position = 7;
//            subPolicy1.length = 8;
//            SubPolicy subPolicy2 = new SubPolicy();
//            subPolicy2.position = 15;
//            subPolicy2.length = 4;
//
//            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};
//
//
//            Policy policy = new Policy();
//            policy.sub_policy_num = 2;
//            policy.sub_policy = subPolicies;
//
//
//            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode);
//            System.out.println("multiSubPolicyEncrypt:" + encryptText);
//            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode);
//            System.out.println("multiSubPolicyDecrypt:" + decryptText);
//
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//
//        System.out.println("==================== 中文字符串加密 =====================");
//        mode = 0; // 字符串加密
//        originText = "中文字符串加密";
//        System.out.printf("Java watch:");printString(originText);
//        try {
//            System.out.println("origin:" + originText);
//
////            byte[] gb2312s = originText.getBytes("GB2312");
////            System.out.printf("加密前gb2312：");printByteArray(gb2312s);
//
//
//            SubPolicy subPolicy1 = new SubPolicy();
//            subPolicy1.position = 2;
//            subPolicy1.length = 2;
//            SubPolicy subPolicy2 = new SubPolicy();
//            subPolicy2.position = 6;
//            subPolicy2.length = 2;
//
//            SubPolicy[] subPolicies = new SubPolicy[]{subPolicy1, subPolicy2};
//
//
//            Policy policy = new Policy();
//            policy.sub_policy_num = 2;
//            policy.sub_policy = subPolicies;
//
//
////            byte[] bytes = multiSubPolicyEncrypt(gb2312s, policy, key, mode);
////            System.out.printf("加密完成后：");printByteArray(bytes);
////            System.out.println("multiSubPolicyEncrypt:" + new String(bytes));
//
//            String encryptText = multiSubPolicyEncrypt(originText, policy, key, mode);
//            System.out.println("multiSubPolicyEncrypt:" + encryptText);
//
//            String decryptText = multiSubPolicyDecrypt(encryptText, policy, key, mode);
//            System.out.println("multiSubPolicyDecrypt:" + decryptText);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
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
