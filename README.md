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
