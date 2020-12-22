# 大数据平台/数据仓库，数据加密SDK

## 相关程序
[teradata-cpt-jni](https://gitee.com/Hu-Lyndon/teradata-cpt-jni)

## Build

```shell script
javah -d ./jni -cp target/classes com.teradata.jni.CptJni
```

## Set environment
PATH=%PATH%;D:\iProject\TeradataCpt\lib

## Run
1. Click Ctrl + Shift + F10 or Run CptJni.main

2. java -jar TeradataCpt-1.0-SNAPSHOT.jar

```shell script
==================== 数字加密 =====================
origin:foxmindTeradataBonc123
encrypt:foxmindTeradataBonc218
decrypt:foxmindTeradataBonc123
==================== 字符串加密 =====================
origin:foxmindTeradataBonc123
encrypt:c?w??,?pWgD?ó?tà?dCò9?
decrypt:foxmindTeradataBonc123
==================== 可见单字节加密 =====================
origin:foxmindTeradataBonc123
encrypt:HD*bNEGDK34S}(wZEWc)J|
decrypt:foxmindTeradataBonc123
```