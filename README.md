# 大数据平台/数据仓库字段等长加密 SDK

本项目通过 JNI 调用 CPT（Content Position Transform）原生算法，适用于 Hive、数据仓库和批量数据处理场景。加密前后字节长度保持不变，非目标字符保持不变。

## 模式

- `CptMode.CHAR`：处理 `0x21` 到 `0xFF`。
- `CptMode.DIGIT`：只处理数字 `0-9`，适合号码等字段。
- `CptMode.VISIBLE_ASCII`：处理可见 ASCII，输出仍是可见单字节字符。

## Java API

```java
byte[] source = "foxmind_1234_text_139".getBytes(StandardCharsets.ISO_8859_1);
byte[] encrypted = CptJni.encrypt(source, 123L, CptMode.DIGIT);
byte[] decrypted = CptJni.decrypt(encrypted, 123L, CptMode.DIGIT);

Policy policy = Policy.of(
    new SubPolicy(8, 4),
    new SubPolicy(18, 3)
);
byte[] partEncrypted = CptJni.multiSubPolicyEncrypt(
    source, policy, 123L, CptMode.DIGIT
);
```

上述部分加密的固定输出为：

```text
foxmind_1234_text_139 -> foxmind_8847_text_125
```

## 位置和长度约定

`SubPolicy.position` 和 `SubPolicy.length` 都是字节单位：

- `position` 从 0 开始；
- 最多支持 5 个子策略；
- `position + length` 不能超过输入 `byte[]` 长度；
- 核心接口以 `byte[]` 为准，支持数据中包含 `0x00`；
- 对中文或其他多字节编码，不能把 Java 字符下标直接当作字节位置。调用方应先确定编码，再按编码后的字节数组计算策略。

旧的 `byte mode` 和 String 包装接口为兼容已有调用保留，新代码建议使用 `CptMode` 和 `byte[]`。

## 原生库

JAR resources 中包含平台动态库，运行时自动提取并加载：

- Linux AMD64：`libTeradataCptJniAmd64.so`
- Linux ARM64：`libTeradataCptJniArm64.so`
- Windows：`libTeradataCptJni.dll`

当前源码改造和测试环境验证的是 Linux AMD64。ARM64 和 Windows 动态库需要在对应平台上使用相同 JNI 源码重新构建后再发布。

## 构建和测试

```shell
mvn clean test
mvn package
```

如需重新生成 JNI 头文件，JDK 8 之后使用：

```shell
javac -h jni -d target/classes src/main/java/com/teradata/jni/*.java
```

关联的原生项目：`teradata-cpt-jni`。
