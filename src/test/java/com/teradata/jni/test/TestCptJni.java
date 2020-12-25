package com.teradata.jni.test;

import org.junit.Test;

public class TestCptJni {

    @Test
    public void TestGetFileNameFromPath() {

        String path = "/bak/libTeradataCptJni.dll";

        String fileName = path.substring(path.lastIndexOf('/')+1);

        System.out.println(fileName);

        String prefix = fileName.substring(0, fileName.lastIndexOf(".")-1);
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        System.out.println("prefix:" + prefix);
        System.out.println("suffix:" + suffix);
    }

    @Test
    public void TestJniVersion() {

        
    }
}
