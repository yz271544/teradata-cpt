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

public class CptJni
{

    public CptJni()
    {
    }

    public static String encrypt(String s, long l, byte byte0)
            throws UnsupportedEncodingException
    {
        return new String(encrypt(s.getBytes("ISO-8859-1"), l, byte0), "ISO-8859-1");
    }

    public static String decrypt(String s, long l, byte byte0)
            throws UnsupportedEncodingException
    {
        return new String(decrypt(s.getBytes("ISO-8859-1"), l, byte0), "ISO-8859-1");
    }

    public static native byte[] encrypt(byte abyte0[], long l, byte byte0);

    public static native byte[] decrypt(byte abyte0[], long l, byte byte0);

    static
    {
        try
        {
            String osType = System.getProperty("os.name");
            /*System.out.println("library path:"+System.getProperty("java.library.path"));
            System.out.println("-------------------------");
            System.out.println(System.getProperties());*/

            try {
                String libName = "/";
                if (osType.equals("Linux")) {
                    libName = "/libTeradataCptJni.so";
                } else if (osType.startsWith("Windows")) {
                    libName = "/libTeradataCptJni.dll";
                } else {
                    throw new RuntimeException("unknown os.name:" + osType);
                }
                load(libName);
            } catch (IOException e) {
                //System.err.println("jar load failed!!!!!!!!!!!!!!!!!!");
                if (osType.equals("Linux")){
                    // 需要设置环境变量 .bashrc
                    // export LD_LIBRARY_PATH='/home/etl/iProject/TeradataCptJni/linux-amd64':${LD_LIBRARY_PATH}
                    // export PATH=${JAVA_HOME}/bin:${LD_LIBRARY_PATH}:${PATH}
                    System.loadLibrary("TeradataCptJni");
                } else if (osType.startsWith("Windows")) {
                    // 需要设置环境变量PATH=${PATH}\;D:\iProject\teradatacpt\lib
                    System.loadLibrary("libTeradataCptJni");
                } else {
                    throw new RuntimeException("unknown os.name:" + osType);
                }
            }
            //System.load("D:\\iProject\\teradatacpt\\lib\\libTeradataCptJni.dll");
        }
        catch (UnsatisfiedLinkError unsatisfiedlinkerror)
        {
            System.err.println("Cannot load libTeradataCptJni library:\n " + unsatisfiedlinkerror.toString());
        }
    }

    public static void load(String path) throws IOException {
        if(!path.startsWith("/")){
            throw new IllegalArgumentException("Wrong path :" + path);
        }
        //如果存在本文件，直接加载，并返回
        File inputFile = new File(path);
        if(inputFile.exists() && inputFile.isFile()){
            System.load(path);
            return;
        }

        String fileName = path.substring(path.lastIndexOf('/')+1);
        if(fileName == null || fileName.isEmpty()){
            throw new IllegalArgumentException("The fileName should not be null");
        }

        String prefix = fileName.substring(0, fileName.lastIndexOf(".")-1);
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
            while((len = in.read(buff)) != -1){
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

        System.out.println("==================== 字符串加密 =====================");
        mode = 0; // 字符串加密
        originText = "foxmindTeradataBonc123";
        try {
            System.out.println("origin:" + originText);
            String encryptText = encrypt(originText, key, mode);
            System.out.println("encrypt:" + encryptText);
            String decryptText = decrypt(encryptText, key, mode);
            System.out.println("decrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        System.out.println("==================== 可见单字节加密 =====================");
        mode = 2; // 可见单字节加密
        originText = "foxmindTeradataBonc123";
        try {
            System.out.println("origin:" + originText);
            String encryptText = encrypt(originText, key, mode);
            System.out.println("encrypt:" + encryptText);
            String decryptText = decrypt(encryptText, key, mode);
            System.out.println("decrypt:" + decryptText);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
