# Teradata CPT：大数据字段等长加密 SDK

Teradata CPT 是面向数据仓库、Hive 和批量数据处理平台的字段级隐私保护库。项目使用 C 实现 CPT（Content Position Transform）核心算法，通过 JNI 向 Java 提供完整字段和指定区间的加密、解密能力。

算法的主要目标是保持字段的字节长度及目标字符类型不变，减少对表结构、字段长度、ETL 流程和上下游数据接口的影响。

> CPT 是确定性的字符表与位置变换算法，不是 AES 等标准认证加密算法。它不提供随机 IV、完整性认证或防篡改能力；使用前应根据实际安全合规要求进行评估。

## 功能特性

- 加密前后字节长度保持一致；
- 支持完整字段加密和多个指定区间加密；
- 支持数字、可见 ASCII 和扩展单字节字符模式；
- 不属于当前模式的字符保持不变；
- C 核心接口和 Java `byte[]` 接口均支持输入中包含 `0x00`；
- Java 层提供类型安全的 `CptMode`、`Policy` 和 `SubPolicy` API；
- JAR 可携带平台原生库，并在运行时自动提取和加载；
- 提供 UTF-8 友好的 String/byte API（`encryptUtf8` / `encryptAsUtf8Bytes` / `encryptAsBase64`），可直接适配 Hive / Doris / GBase UDF；
- 提供 `com.teradata.jni.udf.CptUdf` 字节级参考实现，覆盖 BINARY 列、VARCHAR+Base64、多区间策略子段加密；
- 旧的 `containCn=true` legacy String API 已标 `@Deprecated`，避免在 UTF-8 列里误用；
- Linux AMD64 下可通过 Maven 或 Makefile 一次完成 C、JNI 和 Java 联合构建与测试。

## 项目结构

```text
teradata-cpt/
├── Makefile                         # 统一的编译、测试、打包和帮助入口
├── pom.xml                          # Maven Java 构建及 CMake 联动配置
├── README.md                        # 项目说明
├── native/                          # C/C++ 原生工程
│   ├── CMakeLists.txt               # 原生库和 CTest 构建配置
│   ├── include/
│   │   ├── cpt.h                    # CPT 核心及 JNI 辅助声明
│   │   └── com_teradata_jni_CptJni.h # JNI 方法声明
│   ├── src/
│   │   ├── cpt.c                    # CPT 加密、解密和策略实现
│   │   ├── cptjni.cpp               # Java native 方法桥接
│   │   ├── convert.cpp              # Java Policy 与 C 结构转换
│   │   └── register.cpp             # JNI 类及字段注册
│   ├── test/
│   │   ├── simple_single_test.c     # 完整字段加解密测试
│   │   ├── multiset_policy_test.c   # 多区间策略测试
│   │   └── binary_policy_test.c     # 含 0x00 数据的二进制安全测试
│   └── lib/                         # 历史或手工发布的原生库
├── src/
│   ├── main/
│   │   ├── java/com/teradata/jni/   # Java SDK、模式和策略对象
│   │   └── resources/               # Linux/Windows 平台动态库
│   └── test/java/com/teradata/jni/  # Java/JNI 兼容性和固定向量测试
└── target/                          # Maven、CMake 和打包产物（不提交）
```

当前仓库已经包含 Java SDK、C 核心、JNI 桥接和原生测试。原 `teradata-cpt-jni` 仓库不再是构建本项目的必要依赖，后续应在本仓库统一维护源码和版本。

## 算法模式

| Java 模式 | C 模式 | 处理范围 | 典型用途 |
| --- | --- | --- | --- |
| `CptMode.CHAR` | `CPT_CHAR` | `0x21`–`0xFF` | 扩展单字节文本 |
| `CptMode.DIGIT` | `CPT_INT` | 数字 `0`–`9` | 手机号、证件号、账号中的数字部分 |
| `CptMode.VISIBLE_ASCII` | `CPT_ASC` | 可见 ASCII | 需要保持可见字符类型的字段 |

模式范围之外的数据保持不变。例如 `DIGIT` 模式只改变数字，不改变字母、下划线和分隔符。

## 环境要求

Linux AMD64 联合构建需要：

- JDK 8 或更高版本，并正确设置 `JAVA_HOME`；
- Maven 3；
- CMake 3.17 或更高版本；
- 支持 C99/C++14 的 GCC/G++；
- GNU Make（使用根目录 Makefile 时）。

检查环境：

```shell
java -version
mvn -version
cmake --version
make --version
echo "$JAVA_HOME"
```

当前已验证环境为 Linux AMD64 + Java 17。Linux ARM64 和 Windows 原生库应在对应平台使用当前源码重新构建和测试后再发布。

## 快速开始

查看所有统一命令：

```shell
make help
```

执行完整 C/C++、JNI 和 Java 测试：

```shell
make test
```

生成完整 JAR：

```shell
make package
```

产物位于：

```text
target/TeradataCpt-1.0-SNAPSHOT.jar
```

在 Linux AMD64 上，Maven 的 `native` profile 默认自动激活。构建流程为：

```text
CMake 配置 → 编译原生库和 C 测试 → CTest → 编译 Java → JUnit/JNI 测试 → JAR 打包
```

## Makefile 命令

| 命令 | 说明 |
| --- | --- |
| `make help` | 显示帮助和可用目标 |
| `make compile` | 通过 Maven 构建原生库并编译 Java |
| `make build` | 清理后编译并执行全部 C/Java 测试 |
| `make test` | `make build` 的便捷别名 |
| `make package` | 执行完整测试并生成 JAR |
| `make clean` | 清理 `target/` 下的 Maven/CMake 产物 |
| `make native-build` | 只构建原生库及 C 测试程序 |
| `make native-test` | 只运行三个 CTest 测试 |
| `make native-asan` | 使用 ASan/UBSan 检查原生内存和未定义行为 |
| `make java-compile` | 跳过自动原生构建，只编译 Java |
| `make java-test` | 使用 resources 中已有动态库运行 Java/JNI 测试 |
| `make java-package` | 使用已有动态库打包 JAR |

调试版本示例：

```shell
make native-test BUILD_TYPE=Debug
```

自定义原生构建目录：

```shell
make native-build NATIVE_BUILD_DIR=/tmp/teradata-cpt-native
```

## Maven 构建

完整联合测试：

```shell
mvn clean test
```

完整打包：

```shell
mvn clean package
```

如需跳过自动原生构建，使用 `src/main/resources` 中已有动态库：

```shell
mvn -P\!native clean test
```

联合构建产生的 Linux AMD64 动态库会直接写入 `target/classes/libTeradataCptJniAmd64.so`，因此最终 JAR 使用的是本次 C/C++ 源码实时构建的原生库。

## Java 使用

### 完整字段加解密

```java
import com.teradata.jni.CptJni;
import com.teradata.jni.CptMode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

byte[] source = "foxmind_1234_text_139"
        .getBytes(StandardCharsets.ISO_8859_1);

byte[] encrypted = CptJni.encrypt(source, 123L, CptMode.DIGIT);
byte[] decrypted = CptJni.decrypt(encrypted, 123L, CptMode.DIGIT);

if (!Arrays.equals(source, decrypted)) {
    throw new IllegalStateException("round trip failed");
}
```

### 指定多个区间加解密

```java
import com.teradata.jni.Policy;
import com.teradata.jni.SubPolicy;

Policy policy = Policy.of(
        new SubPolicy(8, 4),
        new SubPolicy(18, 3)
);

byte[] encrypted = CptJni.multiSubPolicyEncrypt(
        source, policy, 123L, CptMode.DIGIT
);
byte[] decrypted = CptJni.multiSubPolicyDecrypt(
        encrypted, policy, 123L, CptMode.DIGIT
);
```

固定测试向量：

```text
输入：foxmind_1234_text_139
策略：(8, 4)、(18, 3)
密钥：123
模式：DIGIT
输出：foxmind_8847_text_125
```

### UTF-8 友好的 String / Base64 入口

```java
import com.teradata.jni.CptJni;
import com.teradata.jni.CptMode;

// 推荐 1：明确以 UTF-8 字节流进，等长密文直接拿到 byte[]
byte[] cipher = CptJni.encryptAsUtf8Bytes("张三李四", 123L, CptMode.CHAR);

// 推荐 2：直接拿到 Base64 编码的密文 String（写到 VARCHAR 列绝对安全）
String cipherBase64 = CptJni.encryptUtf8("张三李四", 123L, CptMode.CHAR);  // 见 CptUdf.encryptAsBase64
```

> `encryptAsUtf8Bytes` / `encryptUtf8` 是 UTF-8 列环境下的**唯一**正确入口。
> 不要在 UTF-8 列里使用 legacy `encrypt(String, ..., containCn=true)`，那条路径会把字符串
> 按 UTF-16BE 编码再走 ISO-8859-1 回装，密文里会带 BOM 和大量 `0x00`，写到 UTF-8 VARCHAR
> 列时会被 UTF-8 校验器替换或丢弃，导致解密失败。详见 `docs/woolly-swimming-duckling.md`。

## 对外发布与 UDF 接入

`com.teradata.jni.udf.CptUdf` 是面向 Hive / Doris / GBase 的字节级参考实现类。
本 SDK **不直接绑定任何平台的 UDF SDK**，平台适配层只需几行就能把 `CptUdf`
包装成对应平台的 UDF。

### 一、打包产物

`make package` 之后：

```shell
ls -la target/TeradataCpt-1.0-SNAPSHOT.jar
```

JAR 内部结构：

```text
META-INF/
├── MANIFEST.MF
└── maven/...
com/
└── teradata/
    └── jni/
        ├── CptJni.class
        ├── CptMode.class
        ├── Policy.class
        ├── SubPolicy.class
        └── udf/
            └── CptUdf.class
libTeradataCptJniAmd64.so    # Linux x86_64
libTeradataCptJniArm64.so    # Linux aarch64
libTeradataCptJni.dll        # Windows
```

JAR 内置三平台动态库；Java 端 `CptJni` 的静态初始化块会按 `os.name` / `os.arch`
选择对应资源并通过 `System.load` 加载，**一个 JAR 即可跨平台分发**。

### 二、UDF 接入推荐写法

**核心原则**：

1. 加密前后字节数严格相等（CPT 算法契约）；
2. **永远不要**在 UDF 适配层做 String ↔ 字节的二次转换；
3. 根据下游列类型选存储方案：
   - `BINARY` / `VARBINARY` 列：直接写密文字节，最省空间；
   - `VARCHAR(utf8)` 列 + `DIGIT` / `VISIBLE_ASCII` 模式：密文含原 UTF-8 中文，可直接写；
   - `VARCHAR(utf8)` 列 + `CHAR` 模式：必须先 Base64 编码再写。

**`CptUdf` 暴露的字节级 API**（UDF 内核，跨平台一致）：

| 方法 | 入参 | 返回 | 用途 |
| --- | --- | --- | --- |
| `encryptUtf8Field` | `byte[] utf8Bytes, long key, CptMode` | `byte[]`（等长密文） | 写到 BINARY 列 |
| `decryptUtf8Field` | `byte[] cipherBytes, long key, CptMode` | `byte[]`（原文 UTF-8 字节） | 从 BINARY 列读回 |
| `encryptAsBytes` / `decryptAsBytes` | 同上 + `Policy` | `byte[]` | 多区间策略 |
| `encryptAsBase64` | `String utf8Text, long key, CptMode` | `String`（Base64） | 写到 VARCHAR 列 |
| `decryptFromBase64` | `String base64Cipher, long key, CptMode` | `String`（原文） | 从 VARCHAR 列读回 |
| `utf8ByteRange` | `String, prefix, suffix` | `int[]{position, length}` | 计算中文子段字节区间 |

### 三、Hive UDF 适配示例

新建 Hive 侧项目，依赖 `TeradataCpt-1.0-SNAPSHOT.jar`：

```java
package com.example.hive.udf;

import com.teradata.jni.CptMode;
import com.teradata.jni.udf.CptUdf;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;

public class CptHiveEncrypt extends UDF {

    // 写入 BINARY 列：等长，无编码风险
    public BytesWritable evaluate(Text input, long key) {
        if (input == null) return null;
        byte[] out = CptUdf.encryptUtf8Field(input.getBytes(), key, CptMode.CHAR);
        return new BytesWritable(out);
    }

    // 写入 VARCHAR 列：CHAR 模式 + Base64 包装
    public Text evaluate(Text input, long key, String modeName, String outputKind) {
        if (input == null) return null;
        CptMode mode = CptMode.valueOf(modeName);
        byte[] out = CptUdf.encryptUtf8Field(input.getBytes(), key, mode);
        if ("base64".equalsIgnoreCase(outputKind)) {
            return new Text(java.util.Base64.getEncoder().encodeToString(out));
        }
        // "raw"：DIGIT / VISIBLE_ASCII 模式 + VARCHAR 列
        return new Text(out);
    }
}
```

在 Hive 中注册并使用：

```sql
-- 上传 JAR 到 HDFS
ADD JAR hdfs:///udf/jars/TeradataCpt-1.0-SNAPSHOT.jar;
ADD JAR hdfs:///udf/jars/cpt-hive-udf-1.0.jar;

CREATE TEMPORARY FUNCTION cpt_encrypt AS 'com.example.hive.udf.CptHiveEncrypt';

-- 写 BINARY 列
INSERT INTO target_table
SELECT
    cpt_encrypt(name, 123L)              AS name_cipher,
    ...
FROM source_table;

-- 写 VARCHAR 列：CHAR 模式 + Base64
INSERT INTO target_table
SELECT
    cpt_encrypt(name, 123L, 'CHAR', 'base64') AS name_cipher,
    ...
FROM source_table;
```

### 四、Doris / StarRocks UDF 适配示例

```java
package com.example.doris.udf;

import com.teradata.jni.CptMode;
import com.teradata.jni.udf.CptUdf;

import java.nio.charset.StandardCharsets;

public class CptDorisEncrypt {

    // Doris Java UDF：静态方法，签名是 evaluate(...)
    public String evaluate(String input, Long key, String modeName) {
        if (input == null) return null;
        CptMode mode = CptMode.valueOf(modeName);
        byte[] in = input.getBytes(StandardCharsets.UTF_8);
        byte[] out = CptUdf.encryptAsBytes(in, key, mode);

        // CHAR 模式密文必须 Base64 包装才能安全写到 VARCHAR(utf8)
        if (mode == CptMode.CHAR) {
            return java.util.Base64.getEncoder().encodeToString(out);
        }
        // DIGIT / VISIBLE_ASCII：密文是合法 UTF-8 字节流，可直接返回
        return new String(out, StandardCharsets.ISO_8859_1);
    }
}
```

Doris 注册：

```sql
CREATE FUNCTION cpt_encrypt(VARCHAR, BIGINT, VARCHAR)
    RETURNS STRING
    PROPERTIES (
        "file" = "http://host/jars/cpt-doris-udf-1.0.jar",
        "symbol" = "com.example.doris.udf.CptDorisEncrypt",
        "type" = "JAVA_UDF"
    );
```

### 五、GBase UDF 适配示例

GBase 8s/8a 的 Java UDF 模型与 Hive 类似（继承框架 UDF 类），同样依赖 SDK JAR，参考 Hive 示例即可。

### 六、UDF 端到端测试模式

`src/test/java/com/teradata/jni/udf/CptUdfStorageTest.java` 已经覆盖下列断言，
**`make test` 必须保持全绿**：

| 断言 | 含义 |
| --- | --- |
| `CHAR 模式密文经 UTF-8 往返后必然变化` | CHAR 模式**不能**直接写 VARCHAR 列 |
| `CHAR 模式密文 BINARY 往返后字节完全一致` | CHAR 模式可以无损写 BINARY 列 |
| `DIGIT 模式 UTF-8 VARCHAR 往返后字节完全一致` | DIGIT 模式可直接写 VARCHAR 列 |
| `VISIBLE_ASCII 模式 UTF-8 VARCHAR 往返后字节完全一致` | VISIBLE_ASCII 模式可直接写 VARCHAR 列 |
| `Base64 解码字节 = 直接加密字节` | Base64 包装层零误差 |
| `legacy containCn=true 密文经 UTF-8 往返必然变化` | 防止用户误用 legacy API |

任何新增 UDF 适配类应当复用上述断言做一次端到端验证：把 UDF 返回值当作"已加密的字节流"，经过 `new String(bytes, UTF_8)` ↔ `getBytes(UTF_8)` 往返后，再调 `CptUdf.decryptUtf8Field` 必须能还原原文。

### 七、跨平台发布前的额外校验

当前仓库 `src/main/resources/` 内置三个平台二进制，但只有 Linux AMD64 是当前 C 源码构建出来的。
ARM64 / Windows 二进制应在对应平台用同份 C 源码重新构建后再发布：

```shell
# Linux AMD64（已自动化）
cmake -S native -B target/native && cmake --build target/native
cp target/native/libTeradataCptJni.so src/main/resources/libTeradataCptJniAmd64.so

# Linux ARM64（在 ARM64 机器上或交叉编译）
cmake -S native -B target/native-arm64
cmake --build target/native-arm64
cp target/native-arm64/libTeradataCptJni.so src/main/resources/libTeradataCptJniArm64.so

# Windows（MSYS2 / Visual Studio + CMake）
cmake -S native -B target/native-win -G "Visual Studio 17 2022" -A x64
cmake --build target/native-win --config Release
cp target/native-win/Release/TeradataCptJni.dll src/main/resources/libTeradataCptJni.dll
```

发布前比对动态库 SHA-256，确保 JAR 里的就是当前 C 源码构建出来的：

```shell
sha256sum target/native/libTeradataCptJni.so \
          src/main/resources/libTeradataCptJniAmd64.so
```

两端哈希必须一致，否则发布出去的 JAR 会带旧二进制。

## 策略与编码约定

`SubPolicy.position` 和 `SubPolicy.length` 都是字节单位：

- `position` 从 0 开始；
- 最多支持 5 个子策略；
- `position + length` 不能超过输入 `byte[]` 长度；
- 核心 API 以 `byte[]` 为准，允许数据中包含 `0x00`；
- 中文及其他多字节编码的 Java 字符下标不等于字节位置；
- 调用方应先确定字段编码，再根据编码后的字节数组计算策略位置。

旧的 `byte mode` 和 String 包装接口为兼容已有调用保留。新代码建议使用 `CptMode` 和 `byte[]`，避免密文经过字符串编码、数据库字符集或 JSON 转换时发生变化。

## 测试说明

### 原生 CTest

```shell
make native-test
```

覆盖：

- `simple_single_test`：完整字段数字及字符加解密；
- `multiset_policy_test`：多个指定区间的固定输出；
- `binary_policy_test`：含 `0x00` 的二进制输入往返。

### Java/JNI 测试

```shell
make java-test
```

`CptCompatibilityTest` 验证：

- C 与 Java 固定向量一致；
- 完整字段和多策略往返；
- 含 `0x00` 的 `byte[]`；
- 越界策略抛出 `IllegalArgumentException`。

### Sanitizer

```shell
make native-asan
```

该命令使用 AddressSanitizer 和 UndefinedBehaviorSanitizer 检查原生缓冲区越界、内存错误和未定义行为，仅适用于支持 GCC/Clang Sanitizer 的类 Unix 环境。

## 原生库加载

Java SDK 会根据操作系统和 CPU 架构加载 JAR 中的对应资源：

- Linux AMD64：`libTeradataCptJniAmd64.so`
- Linux ARM64：`libTeradataCptJniArm64.so`
- Windows：`libTeradataCptJni.dll`

加载失败会在类初始化阶段抛出 `ExceptionInInitializerError`，避免延迟到 native 方法调用时才暴露问题。

JAR 内置动态库的设计支持一个 JAR 跨平台分发。UDF 接入、跨平台发布前 SHA-256 校验等
步骤详见后文[「对外发布与 UDF 接入」](#对外发布与-udf-接入)章节。

## JNI 头文件

JDK 8 之后可使用 `javac -h` 重新生成 JNI 头文件：

```shell
mkdir -p target/classes
javac -h native/include \
  -d target/classes \
  src/main/java/com/teradata/jni/*.java
```

重新生成后应检查 JNI 方法签名差异，并运行 `make test`。

## 许可证

项目使用 Apache License 2.0，详见根目录 `LICENSE`。原生目录中的许可证文件用于保留历史工程的许可说明。
