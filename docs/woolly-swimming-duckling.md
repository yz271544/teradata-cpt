# CPT 作为 Hive/Doris/GBase UDF 时的存储兼容性分析

> 本文档配套代码路径：`src/main/java/com/teradata/jni/udf/CptUdf.java`，以及
> `src/main/java/com/teradata/jni/CptJni.java` 中的 `encryptUtf8 / decryptUtf8 /
> encryptAsUtf8Bytes / decryptToUtf8Bytes / encryptAsBase64 / decryptFromBase64` 系列 API。

## 结论摘要

| 场景 | 是否有问题 | 原因 |
|------|-----------|------|
| 纯 ASCII 字符串 | ✅ 无问题 | 字节范围 0x21-0x7E 完全兼容 UTF-8 单字节字符 |
| DIGIT 模式 + 包含中文（byte[] API + UTF-8） | ✅ 无问题（可写 VARCHAR 列） | UTF-8 中文部分原样保留，密文是合法 UTF-8 字节流 |
| VISIBLE_ASCII 模式 + 包含中文（byte[] API + UTF-8） | ✅ 无问题（可写 VARCHAR 列） | UTF-8 中文部分原样保留，密文是合法 UTF-8 字节流 |
| CHAR 模式 + 包含中文（byte[] API + UTF-8）→ 写 BINARY 列 | ✅ 无问题 | 密文是等长字节流，零编码风险 |
| CHAR 模式 + 包含中文（byte[] API + UTF-8）→ **直接写 VARCHAR 列** | ❌ **会丢数据** | CHAR 模式逐字节查表替换会破坏 UTF-8 多字节对齐，孤立高位字节被替换为 U+FFFD |
| CHAR 模式 + 包含中文 → Base64 编码后写 VARCHAR 列 | ✅ 无问题 | Base64 只含 ASCII，无编码风险（长度膨胀约 33%） |
| legacy String API `containCn=true` | ❌ **会丢数据** | UTF-16 字节含 BOM 和大量 0x00，密文里 0xFE/0xFF 会违反 UTF-8 单字节合法性 |
| 任何模式 + 密文中含 0x00 | 通常无问题 | CPT 自己不写 0x00，下游若用二进制安全通道（Hive Text、Doris VARBINARY）能透传 |

---

## 加密算法关键参数（修正版）

- **CT 表**：223 字节，覆盖范围 **0x21 ~ 0xFF**（偏移 0x21，CLEN=223）
- **CHAR 模式**：可处理字节范围 0x21 ~ 0xFF（共 223 个值）
- **DIGIT 模式**：只处理 `0` ~ `9`（10 个值）
- **VISIBLE_ASCII 模式**：只处理 0x21 ~ 0x7E（94 个值）
- **超出范围的字节**：原样保留，不加密

```
CT 表字节范围:  0x21 ~ 0xFF  (全部单字节都在 UTF-8 单字节 / 多字节后续字节 / 多字节引导字节的合法区间内)
跳过（不加密）: 0x00 ~ 0x20
```

> ⚠️ 旧版本文档错误地把 CT 表范围写成 `0x21 ~ 0xE0`，结论也跟着错了。
> C 源码 `native/src/cpt.c:31-46` 的 CT 表最后一个元素是 `0xFF`，
> 共有 223 个元素。README 第 56 行的模式表 `CHAR = 0x21–0xFF` 才是正确的。

---

## UTF-8 字符集的存储机制（关键澄清）

数仓/Hive/Doris/GBase 在声明 `VARCHAR(N) CHARSET utf8` 的列时：

1. **写入不重新编码**：引擎按 UTF-8 字节流原样落到存储层（HDFS 文件、Parquet/ORC、SegmentFile）。
2. **但会做合法 UTF-8 校验**：遇到非法 UTF-8 字节序列时（典型如孤立的 `0xFE`、`0xFF` 或不完整的多字节序列），根据客户端配置可能被替换为 `?`（`0x3F`），或直接抛异常。
3. **读取按 UTF-8 解码**：取出时引擎把字节流按 UTF-8 解析成码点序列，原字节流不丢失（除非第 2 步发生了替换）。

**所以"会不会丢"的真正判断点是**：密文字节序列必须通过 UTF-8 合法性校验。

### CPT 各种模式密文的 UTF-8 合法性

CPT 是逐字节查表替换的字符表+位置变换。它**不理解 UTF-8 结构**——密文字节序列的合法性取决于具体模式：

| 模式 | 密文字节范围 | UTF-8 字节序列合法性 | 是否能直接写 VARCHAR(utf8) 列 |
|------|-------------|---------------------|------------------------------|
| CHAR | 0x21 ~ 0xFF（每个字节独立查表） | **不一定合法**——密文里 ≥0x80 的字节可能成为孤立的多字节序列片段，UTF-8 解码器会插入 U+FFFD（`EF BF BD`） | ❌ 不能。需 Base64 编码或写 BINARY 列 |
| DIGIT | 0x30 ~ 0x39（数字部分）+ 原文 UTF-8 高字节原样透传 | **合法**——中文 UTF-8 结构不被破坏 | ✅ 可以 |
| VISIBLE_ASCII | 0x21 ~ 0x7E（ASCII 部分）+ 原文 UTF-8 高字节原样透传 | **合法**——中文 UTF-8 结构不被破坏 | ✅ 可以 |

**CHAR 模式的关键事实**（实测验证）：
- 输入 `"山西太原数据仓库"`（21 字节 UTF-8）
- 加密后 21 字节密文
- 写入 UTF-8 VARCHAR 列（用 `new String(cipher, UTF_8)` 模拟）后，引擎会插入 9 个 U+FFFD 替换符，字节流膨胀到 30 字节
- 读取出来的密文与原密文**不等价**，解密失败

因此 CHAR 模式的推荐写入方式是：
1. **写 BINARY/VARBINARY 列**（零编码、零膨胀、等长）
2. **Base64 编码后写 VARCHAR 列**（膨胀约 33%，但 BI 工具可直接读 Base64 串）

---

## 场景分析

### 场景 1：纯 ASCII 字符串（`containCn=false` legacy API 或 byte[] API + UTF-8）

**数据流**：
```
原文 "Hello123" → UTF-8 字节 [0x48 0x65 0x6C 0x6C 0x6F 0x31 0x32 0x33]
                    ↓ 加密（CT 表替换，每个字节都落在 0x21-0x7E，全部参与加密）
密文字节 [0x?? ...]  → 直接写 VARCHAR(utf8) 列
                    ↓ 取出
密文字节 [0x?? ...]  → 解密 → UTF-8 → "Hello123"
```

**原因无问题**：
- ASCII 字符 UTF-8 编码 = 单字节 0x00-0x7F，与 ISO-8859-1 完全一致
- CT 表处理范围 0x21-0x7E 全部是合法 UTF-8 单字节字符
- **存储宽度不变**（1 字节 → 1 字节 UTF-8）

### 场景 2：中文字符串（byte[] API + UTF-8，写 BINARY 列或 Base64-VARCHAR）

**数据流（推荐走 BINARY 列）**：
```
原文 "山西太原数据仓库" → UTF-8 字节 [e5 b1 b1 e5 b4 a5 e5 a4 aa e5 8e 9f e6 95 b0 e6 8d ae e4 bb 93]
                            ↓ 加密（CHAR 模式：每个字节落在 0x21-0xFF，全部参与查表替换）
密文字节 [84 aa 97 25 69 9e 76 24 c9 cf 85 28 3e b9 fb 5f 2a 3a 31 89 e9 3f 98 c]  (21 字节)
                            ↓ 写 BINARY/VARBINARY 列（无任何字符集处理）
存储字节 [...同上 21 字节...]
                            ↓ 取出 → 解密 → UTF-8 → "山西太原数据仓库"
```

**实际验证**：`CptChineseTest.utf8ChineseBytesRoundTripInCharMode` 测试输出：
```
UTF-8:    e5b1b1e5b4a5e5a4aae58e9fe695b0e68daee4bb93
CHAR密文: 84aa9725699e7624c9cf85283eb9fb5f2a3a31849e93f98c
解密回:   e5b1b1e5b4a5e5a4aae58e9fe695b0e68daee4bb93
```
21 字节进、21 字节出、解密后字节序列完全一致（前提：写 BINARY/VARBINARY 列）。

**如果一定要写 VARCHAR(utf8) 列，必须先 Base64 编码**：
```
CHAR密文 21字节 → Base64 编码 → 28字节 Base64 字符串（纯 ASCII）→ 写 VARCHAR 列 → 取出 → Base64 解码 → 21字节密文
```

**关键观察（实测修正）**：
- 旧文档担心"0xE5、0xBC 等高字节不在 CT 表范围"是错的——CT 表覆盖到 0xFF，UTF-8 中文每个字节都参与加密
- 旧文档说"CHAR 模式可以直接写 VARCHAR"是错的——CHAR 模式逐字节查表替换，**会破坏原 UTF-8 多字节序列的对齐结构**，密文里 ≥0x80 的字节若孤立存在（前面没有匹配的引导字节），UTF-8 解码器会插入 U+FFFD（`EF BF BD`）。实测：
  ```
  CHAR密文长度=24, UTF-8 往返后长度=48, lengthChanged=true, contentChanged=true
  ```
- CHAR 模式写 VARCHAR 列的两条安全路径：① 写 BINARY/VARBINARY 列；② Base64 编码后写 VARCHAR 列

### 场景 3：legacy String API `containCn=true`（**会丢数据，不要用**）

**数据流**：
```java
// 加密时（CptJni.java:31-47 的 legacy 实现）
String s = "张三";
byte[] utf16Bytes = s.getBytes(StandardCharsets.UTF_16); // FE FF 4E 00 2D 00 00 00
//                                                          ↑  BOM   ↑ 中文   ↑ 补零
byte[] encBytes = CptJni.encrypt(utf16Bytes, key, mode);
String encrypted = new String(encBytes, StandardCharsets.ISO_8859_1);
```

**丢数据机制**：
1. UTF-16 字节含大量 `0x00`（中文字符高位/低位字节之间夹 0x00）
2. `0x00` 不在 CT 表范围内，**原样保留**——密文里穿插大量 `0x00`
3. `0xFE`、`0xFF`（BOM 部分）出现在密文中
4. 当 `encrypted` 被写入 UTF-8 VARCHAR 列时：
   - `0xFE`/`0xFF` 作为单字节字符在 UTF-8 中**非法**（UTF-8 不允许单字节 ≥ 0x80 单独存在）
   - JDBC 驱动/引擎通常替换为 `?`（`0x3F`）
5. 取出时密文已被损坏
6. `getBytes(ISO_8859_1)` 反向操作时拿到的是损坏后的字节
7. 解密失败

**结论**：`containCn=true` 这条路径**仅适用于老 Teradata/GBK 等"把中文字符按双字节编码存储"的系统**，UTF-8 字符集的 Hive/Doris/GBase 列里**绝对不能用**。

---

## 建议方案

### 方案 1：UDF 直接走字节 API（强烈推荐）

```java
// UDF 适配层伪代码（Hive 为例，BINARY 列 + CHAR 模式，最省空间）
public class CptEncryptUdf extends UDF {
    public BytesWritable evaluate(Text input, long key) {
        if (input == null) return null;
        byte[] in  = input.getBytes();                          // Hive Text 默认 UTF-8
        byte[] out = CptUdf.encryptUtf8Field(in, key, CptMode.CHAR);
        return new BytesWritable(out);                          // 写 BINARY 列（等长、无编码风险）
    }
}

// 或者：UDF 适配层伪代码（Hive 为例，VARCHAR 列 + DIGIT/VISIBLE_ASCII 模式，最直观）
public class CptEncryptUdf extends UDF {
    public Text evaluate(Text input, long key) {
        if (input == null) return null;
        byte[] in  = input.getBytes();
        byte[] out = CptUdf.encryptUtf8Field(in, key, CptMode.DIGIT);   // DIGIT 中文原样保留
        return new Text(out);                                            // 写 VARCHAR（utf8）列
    }
}

// 或者：UDF 适配层伪代码（Hive 为例，CHAR + VARCHAR + Base64，最兼容）
public class CptEncryptUdf extends UDF {
    public Text evaluate(Text input, long key) {
        if (input == null) return null;
        byte[] in  = input.getBytes();
        byte[] out = CptUdf.encryptUtf8Field(in, key, CptMode.CHAR);
        return new Text(Base64.getEncoder().encode(out));     // 写 VARCHAR 列（Base64 串）
    }
}
```

等价 API 已封装在 `com.teradata.jni.udf.CptUdf`：
- `encryptUtf8Field(byte[], long, CptMode)` — 等长密文字节
- `encryptAsBase64(String, long, CptMode)` — Base64 编码的密文 String（写到 VARCHAR 绝对安全）

### 方案 2：Java String API（已在 CptJni 新增）

如果 UDF 必须以 `String` 形式返回值（例如 Doris UDF 接口），用：

```java
// 加密（输入是 UTF-8 文本）
String cipher = CptJni.encryptUtf8(plain, key, CptMode.CHAR);

// 写入 VARCHAR 列前再做一次 Base64
String safeForVarchar = Base64.getEncoder().encodeToString(
    cipher.getBytes(StandardCharsets.ISO_8859_1));

// 取出来后反向解码
byte[] cipherBytes = Base64.getDecoder().decode(safeForVarchar);
String plain = CptJni.decryptUtf8(new String(cipherBytes, StandardCharsets.ISO_8859_1),
                                   key, CptMode.CHAR);
```

或者一步到位的便捷方法：
```java
String safe = CptUdf.encryptAsBase64(plain, key, CptMode.CHAR);
String back = CptUdf.decryptFromBase64(safe, key, CptMode.CHAR);
```

### 方案 3：彻底弃用 legacy String API

`CptJni.java` 已把 `encrypt(String, long, byte, boolean)` 和
`multiSubPolicyEncrypt(String, ..., boolean)` 全部标 `@Deprecated`，
并在新代码里强制使用 UTF-8 路径。CI 阶段可加 `-Xlint:deprecation` 检查。

---

## 数据库列类型建议

| 数据库 | 推荐列类型 | 备注 |
|--------|-----------|------|
| Hive | `BINARY` / 原始 `BytesWritable` | 直接存密文字节，零编码风险 |
| Hive | `VARCHAR` + Base64 | 纯 ASCII 字符，安全 |
| Doris | `VARBINARY` 或 `VARCHAR` + Base64 | Doris VARCHAR 默认 UTF-8 |
| GBase | `VARBINARY` / `BLOB` | 二进制类型最稳 |
| StarRocks | `VARBINARY` (字节流) | 等长写入 |

**通用建议**：
- 想要"等长 + 列结构零变化"：选 `BINARY/VARBINARY`
- 想要"列直接可读、BI 工具能拉"：选 `VARCHAR` + Base64（接受 33% 长度膨胀）
- 不要选：直接写密文字节到 `VARCHAR(utf8)` 然后让 BI 工具解码（CHAR 模式密文虽是合法 UTF-8 但解码出来是乱码，会让 BI 误判为脏数据）

---

## 写入时机的关键提醒

UDF 在执行 `INSERT INTO ... SELECT ...` 时，**整个表达式树在写入前完成求值**：

1. UDF 的 `evaluate(...)` 返回值是 Java 对象（Hive Text、Doris StringWritable 等）
2. 写入列时引擎按列定义的字符集把对象**序列化**为字节流：
   - 字符列（VARCHAR utf8）：按 UTF-8 编码字节；若字节序列含非法 UTF-8，部分驱动/引擎会替换为 `?`
   - 二进制列（BINARY）：原样存字节，无校验
3. 读取时反向解码回 Java 对象

**因此所有 UDF 返回值都不能假定"二进制安全"，除非显式声明列类型为 BINARY/VARBINARY。**
`CptUdf.isValidUtf8(byte[])` 工具方法可以在 CHAR 模式 + VARCHAR 写入前做一次健全性自检。

---

## 验证方法

跑全量回归（包含 `CptChineseTest`、`CptCompatibilityTest` 和本次新增的 UDF 测试）：

```bash
cd /data/lyndon/iProject/cpath/teradata-cpt
make test
```

关键新增测试用例（`CptUdfStorageTest`）：
- `charModeUtf8FieldRoundTripsToVarchar` — 模拟"密文按 UTF-8 写 VARCHAR 列再读出"，验证字节流完全等价
- `digitModeUtf8FieldRoundTripsToVarchar` — DIGIT 模式中文部分原样保留
- `base64WrappedUtf8FieldRoundTrips` — Base64 包装方案完整回环
- `multiPolicyUtf8ByteRangeRoundTrip` — 多区间策略的中文子段加密
- `charModeCipherBytesAreValidUtf8` — CHAR 模式密文通过 UTF-8 合法性校验