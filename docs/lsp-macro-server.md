# LSPMacroServer 详细文档

> 文件位置：`external/cangjie_compiler/src/main-macrosrv.cpp`
> 编译产物：`{sdk}/bin/LSPMacroServer` (Windows: `LSPMacroServer.exe`)

---

## 一、概述

`LSPMacroServer` 是仓颉编译器提供的**宏展开服务进程**，专为 IDE/LSP 场景下的进程隔离宏求值而设计。

与 `cjc-frontend --debug-macro`（批量文件级展开）不同，`LSPMacroServer` 作为**常驻后台进程**运行，通过**管道（Pipe）**接收宏调用请求并流式返回展开结果，具有以下优势：

| 特性 | `cjc-frontend --debug-macro` | `LSPMacroServer` |
|------|------------------------------|------------------|
| 运行模式 | 一次性进程 | 常驻服务进程 |
| 通信方式 | 文件（`.macrocall`） | 管道（二进制） |
| 粒度 | 文件级 | 宏调用级 |
| 并行支持 | 有限 | 原生支持 |
| 适用场景 | 简单展开、调试 | LSP 集成、IDE 实时展开 |

---

## 二、在整体架构中的位置

```
IDE Plugin (Kotlin)
    │
    ├── 宏编译：GeneralCommandLine → cjc --compile-macro
    │           产出: lib-macro_<pkg>.dll/.so/.dylib
    │
    ├── 宏展开（简单模式）：GeneralCommandLine → cjc-frontend --debug-macro
    │           产出: <file>.macrocall（文本文件）
    │
    └── 宏展开（服务模式）：ProcessBuilder → LSPMacroServer <handles>
                通信: 管道 + FlatBuffers 二进制消息
                │
                └── MacroEvaluation（C++ 宏求值引擎）
                        加载: lib-macro_<pkg>.dll/.so/.dylib
                        执行: 仓颉运行时（CJRuntime）
```

---

## 三、启动方式与命令行参数

### 3.1 命令格式

**Linux / macOS（5 个参数 + 可选 CJNATIVE backend）：**
```bash
LSPMacroServer <read_fd> <write_fd> <enable_parallel> <cjc_folder> <ppid>
```

**Windows（4 个参数）：**
```cmd
LSPMacroServer.exe <read_handle> <write_handle> <enable_parallel> <cjc_folder>
```

### 3.2 参数说明

| 索引 | 参数 | 类型 | 说明 |
|------|------|------|------|
| 1 | `read_fd` / `read_handle` | 整数 | 客户端→服务端管道的**读端**文件描述符（Unix fd）或句柄（Windows HANDLE 转换为整数） |
| 2 | `write_fd` / `write_handle` | 整数 | 服务端→客户端管道的**写端**文件描述符或句柄 |
| 3 | `enable_parallel` | `"0"` 或 `"1"` | 是否启用并行宏展开（`"1"` 为启用） |
| 4 | `cjc_folder` | 路径字符串 | `cjc` 可执行文件所在目录（用于定位运行时库） |
| 5 | `ppid` | 整数 | **仅 Linux/macOS**：父进程 PID，服务端每 2 秒检测一次，父进程退出则自动退出 |

### 3.3 参数校验规则

启动时校验失败则以返回码 `-1` 退出：
- `read_fd`/`write_fd` 必须是纯数字字符串
- `cjc_folder` 不能为空字符串
- Windows：通过 `GetNamedPipeInfo` 验证两端均为命名管道
- Unix：通过 `fstat` + `S_ISFIFO` 验证两端均为 FIFO 管道

---

## 四、通信协议

### 4.1 传输层：管道通信

使用**双向匿名管道**：
- **客户端 → 服务端**：客户端写入，服务端读取（`read_fd` / `hChildRead`）
- **服务端 → 客户端**：服务端写入，客户端读取（`write_fd` / `hChildWrite`）

管道缓冲区分片大小：**4096 字节**（超长消息自动分片传输）。

消息帧格式（length-prefixed framing）：
```
[ 4 bytes: payload_length (little-endian uint32) ]
[ N bytes: FlatBuffers payload ]
```

> 源码依据：`MacroProcMsger::WriteToSrvPipe` / `ReadFromSrvPipe` 使用固定分片写入。

### 4.2 序列化层：FlatBuffers

消息体使用 **FlatBuffers** 序列化，Schema 文件：
```
external/cangjie_compiler/schema/MacroMsgFormat.fbs
```

---

## 五、消息格式（FlatBuffers Schema）

### 5.1 根消息类型

```flatbuffers
table MacroMsg {
  content : MsgContent;   // 消息内容（union）
}
root_type MacroMsg;
```

`MsgContent` 是 Union，包含四种消息类型：

```flatbuffers
union MsgContent {
  defLib    : DefLib,          // 客户端→服务端：加载宏库
  multiCalls: MultiMacroCalls, // 客户端→服务端：批量宏调用请求
  macroResult: MacroResult,    // 服务端→客户端：单个宏展开结果
  exitTask  : ExitTask         // 客户端→服务端：退出信号
}
```

### 5.2 DefLib（加载宏动态库）

客户端告知服务端需要加载的宏动态库路径列表：

```flatbuffers
table DefLib {
  paths : [string];   // 动态库文件的绝对路径列表
}
```

示例路径：
- Windows: `C:\project\target\release\windows_x86_64_cjnative\lib-macro_mypackage.dll`
- Linux: `/project/target/release/linux_x86_64_cjnative/lib-macro_mypackage.so`
- macOS: `/project/target/release/darwin_x86_64_cjnative/lib-macro_mypackage.dylib`

### 5.3 MultiMacroCalls（批量宏调用请求）

客户端发送一批宏调用信息，服务端逐个展开并返回结果：

```flatbuffers
table MultiMacroCalls {
  calls : [MacroCall];   // 宏调用列表
}

table MacroCall {
  id          : IdInfo;     // 宏标识符信息
  hasAttrs    : bool;       // 是否携带属性
  args        : [Token];    // 宏参数 Token 列表
  attrs       : [Token];    // 宏属性 Token 列表
  parentNames : [string];   // 父宏上下文名称列表（assertParentContext）
  childMsges  : [ChildMsg]; // 子宏消息列表（getChildMessages）
  methodName  : string;     // 宏函数名
  packageName : string;     // 宏所在包名
  libPath     : string;     // 宏动态库路径
  begin       : Position;   // 宏调用在源码中的起始位置
  end         : Position;   // 宏调用在源码中的结束位置
}
```

### 5.4 MacroResult（宏展开结果）

服务端为每个宏调用返回一个结果：

```flatbuffers
table MacroResult {
  id            : IdInfo;       // 对应宏调用的标识符
  status        : uint8;        // 展开状态（MacroEvalStatus 枚举）
  tks           : [Token];      // 展开后的 Token 序列
  items         : [ItemInfo];   // 宏上下文 setItem 数据
  assertParents : [string];     // assertParentContext 失败的父宏名
  diags         : [Diagnostic]; // 诊断信息（错误/警告）
}
```

#### MacroEvalStatus 枚举值

| 值 | 含义 |
|----|------|
| 0 | `INIT` - 初始状态，子宏未求值 |
| 1 | `READY` - 准备求值 |
| 2 | `EVAL` - 正在求值 |
| 3 | `SUCCESS` - 展开成功 |
| 4 | `FAIL` - 展开失败 |
| 5 | `REEVAL` - 需要重新求值（展开结果中仍有宏调用） |
| 6 | `FINISH` - 完成，无需再次求值 |
| 7 | `ANNOTATION` - 需要转换为带注解的声明 |
| 8 | `REEVALFAILED` - 重新求值失败 |

### 5.5 ExitTask（退出信号）

客户端通知服务端停止运行：

```flatbuffers
struct ExitTask {
  flag : bool;   // 通常为 true
}
```

### 5.6 辅助数据类型

```flatbuffers
struct Position {
  file_id : uint32;  // 源文件 ID
  line    : int32;   // 行号（1-based）
  column  : int32;   // 列号（1-based）
}

table Token {
  kind         : uint8;    // Token 类型（TokenKind 枚举）
  value        : string;   // Token 文本值
  begin        : Position; // 起始位置
  end          : Position; // 结束位置
  delimiterNum : uint32;   // 原始字符串分隔符数量（默认 1）
}

table IdInfo {
  name : string;   // 宏标识符名称
  pos  : Position; // 位置信息
}

table Diagnostic {
  diagSeverity  : int32;   // 诊断级别（0=Note,1=Warning,2=Error）
  begin         : Position;
  end           : Position;
  errorMessage  : string;  // 错误信息
  mainHint      : string;  // 主要提示
}

table ChildMsg {
  childName : string;      // 子宏名称
  items     : [ItemInfo];  // 子宏设置的 item 列表
}

table ItemInfo {
  key   : string;         // item 键名
  value : OptionValue;    // item 值（union: string/int/bool/tokens）
}

union OptionValue {
  sValue : string,        // 字符串值
  iValue : IntValue,      // 整数值
  bValue : BoolValue,     // 布尔值
  tValue : TokensValue    // Token 列表值
}
```

---

## 六、通信流程

### 6.1 标准交互序列

```
客户端 (IDE/JVM)                    服务端 (LSPMacroServer)
      │                                      │
      │── 启动进程（传入管道句柄）───────────→│
      │                                      │ 初始化运行时
      │                                      │ ExecuteEvalSrvTask() 开始循环
      │                                      │
      │── [DefLib] 加载宏动态库 ────────────→│
      │                                      │ FindDef(): dlopen/LoadLibrary
      │                                      │ 加载并验证动态库
      │                                      │
      │── [MultiMacroCalls] 批量宏调用 ──────→│
      │                                      │ EvalMacroCall()
      │                                      │   反序列化 MacroCall
      │                                      │   调用宏函数
      │                                      │   序列化结果
      │←─ [MacroResult] 展开结果 ────────────│
      │                                      │
      │   （可多次往返）                       │
      │                                      │
      │── [ExitTask] 退出信号 ───────────────→│
      │                                      │ 关闭运行时
      │                                      │ 关闭管道
      │                                      │ 进程退出 (0)
```

### 6.2 服务端主循环

`ExecuteEvalSrvTask()` 内部循环：

```
loop:
  ReadMsgFromClient(msg)
  switch GetMacroMsgContenType(msg):
    case DefLib:
      FindDef(msg)           // 加载动态库，注册宏函数地址
    case MultiMacroCalls:
      EvalMacroCall(msg)     // 展开宏，发送 MacroResult
      EvalMacroCallsAndWaitResult()  // 等待并行展开完成
    case ExitTask:
      return                 // 退出循环
```

---

## 七、客户端（Java/Kotlin）集成方案

### 7.1 创建管道并启动进程

**Unix（使用 Java NIO Pipe）：**

```kotlin
import java.nio.channels.Pipe

// 创建两个单向管道（双向通信需要两个）
val p2c = Pipe.open()  // 客户端写 → 服务端读
val c2p = Pipe.open()  // 服务端写 → 客户端读

// 获取文件描述符（需要反射或 JNI）
val readFd  = getFd(p2c.source())   // 服务端读端 fd
val writeFd = getFd(c2p.sink())     // 服务端写端 fd

val process = ProcessBuilder(
    "${sdkPath}/bin/LSPMacroServer",
    readFd.toString(),
    writeFd.toString(),
    if (enableParallel) "1" else "0",
    "${sdkPath}/bin",
    ProcessHandle.current().pid().toString()  // ppid
).start()

// 注意：父进程持有管道的另一端
// 客户端写端：p2c.sink()   → 写入此端，服务端从 readFd 读
// 客户端读端：c2p.source() → 从此端读，服务端写入 writeFd
```

**Windows（使用命名管道，通过 JNA/JNI）：**

```kotlin
// Windows 需要创建命名管道或匿名管道句柄
// 可通过 JNA 调用 CreatePipe / CreateNamedPipe
// 将句柄数值转为字符串传给进程
```

> **注意**：Java 标准库没有直接暴露文件描述符整数给子进程继承。
> 实际集成时，推荐使用 **JNA** 调用 `pipe(2)` 系统调用，
> 或通过 `ProcessBuilder.inheritIO()` 的变体来传递描述符。

### 7.2 消息编解码（FlatBuffers）

引入依赖：
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.flatbuffers:flatbuffers-java:23.5.26")
}
```

使用编译器 schema 生成 Java 类（`flatc --java schema/MacroMsgFormat.fbs`），
然后进行序列化：

```kotlin
import com.google.flatbuffers.FlatBufferBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder

// 构建 DefLib 消息
fun buildDefLibMsg(libPaths: List<String>): ByteArray {
    val builder = FlatBufferBuilder(256)
    val pathOffsets = libPaths.map { builder.createString(it) }.toIntArray()
    val pathsVector = DefLib.createPathsVector(builder, pathOffsets)
    val defLib = DefLib.createDefLib(builder, pathsVector)
    val content = MacroMsg.createMacroMsg(builder,
        MsgContent.defLib, defLib)
    builder.finish(content)
    return builder.sizedByteArray()
}

// 发送消息（length-prefixed）
fun sendMsg(outputStream: OutputStream, payload: ByteArray) {
    val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
    lenBuf.putInt(payload.size)
    outputStream.write(lenBuf.array())
    outputStream.write(payload)
    outputStream.flush()
}

// 读取消息
fun readMsg(inputStream: InputStream): ByteArray {
    val lenBuf = inputStream.readNBytes(4)
    val len = ByteBuffer.wrap(lenBuf).order(ByteOrder.LITTLE_ENDIAN).int
    return inputStream.readNBytes(len)
}

// 解析消息类型
fun getMsgType(payload: ByteArray): Byte {
    val buf = ByteBuffer.wrap(payload)
    val msg = MacroMsg.getRootAsMacroMsg(buf)
    return msg.contentType
}
```

### 7.3 完整交互示例

```kotlin
class MacroServerClient(sdkPath: String, enableParallel: Boolean) : Closeable {

    private val process: Process
    private val writer: OutputStream
    private val reader: InputStream

    init {
        // 简化：通过 stdin/stdout 桥接（实际需要匿名管道）
        process = ProcessBuilder(
            "$sdkPath/bin/LSPMacroServer",
            /* read_fd  */ "...",
            /* write_fd */ "...",
            if (enableParallel) "1" else "0",
            "$sdkPath/bin",
            /* ppid */ ProcessHandle.current().pid().toString()
        ).start()
        writer = process.outputStream
        reader = process.inputStream
    }

    /** 第一步：加载宏动态库 */
    fun loadMacroLibs(libPaths: List<String>) {
        sendMsg(writer, buildDefLibMsg(libPaths))
        // DefLib 无返回，服务端直接加载
    }

    /** 第二步：请求宏展开 */
    fun expandMacros(calls: List<MacroCallRequest>): List<MacroResultData> {
        sendMsg(writer, buildMultiCallsMsg(calls))
        // 读取每个调用对应的 MacroResult
        return calls.map { readMacroResult() }
    }

    /** 最后：关闭服务 */
    fun shutdown() {
        sendMsg(writer, buildExitMsg())
        process.waitFor(5, TimeUnit.SECONDS)
    }

    override fun close() = shutdown()
}
```

---

## 八、与 cjc-frontend --debug-macro 的区别

| 维度 | `LSPMacroServer` | `cjc-frontend --debug-macro` |
|------|-----------------|------------------------------|
| **输入** | 通过管道接收 FlatBuffers 编码的 MacroCall | 源文件路径（命令行参数） |
| **输出** | 通过管道返回 FlatBuffers 编码的 MacroResult | 生成 `<file>.macrocall` 文本文件 |
| **生命周期** | 长期运行，处理多次请求 | 每次调用启动/退出 |
| **粒度** | 单个宏调用 | 整个文件中的所有宏 |
| **进程开销** | 一次性启动，后续无开销 | 每次展开需重新启动进程 |
| **运行时** | 常驻内存 | 每次重新初始化 |
| **使用者** | LSP 服务器内部 | IDE 插件直接调用 |

---

## 九、平台差异

### Windows

- 使用 **Windows HANDLE**（命名管道或匿名管道）
- HANDLE 转为整数：`reinterpret_cast<HANDLE>(atoi(arg))`
- 无 ppid 监控（不需要第 5 个参数）
- 管道验证：`GetNamedPipeInfo`

### Linux / macOS

- 使用 **POSIX 文件描述符**（fd，整数）
- 额外需要第 5 个参数：`ppid`（父进程 PID）
- 服务端启动独立线程，每 2 秒检测一次父进程是否存活：
  ```c
  kill(ppid, 0) != 0  // 父进程退出 → 服务端自动退出
  ```
- 管道验证：`fstat` + `S_ISFIFO`

---

## 十、生命周期与异常处理

### 正常退出

1. 客户端发送 `ExitTask` 消息
2. 服务端退出主循环
3. 服务端关闭运行时（`RuntimeInit::CloseRuntime()`）
4. 服务端关闭管道句柄
5. 进程以退出码 `0` 退出

### 异常退出

- 管道破损（`pipeError = true`）→ 服务端关闭资源并退出（退出码 `1`）
- Unix：父进程消失 → `MonitoringParentProcess` 线程检测到，强制退出（退出码 `1`）
- Windows：无父进程监控，客户端需主动管理进程生命周期

### 客户端建议

```kotlin
// 注册 JVM 关闭钩子，确保服务进程被清理
Runtime.getRuntime().addShutdownHook(Thread {
    macroServerClient.shutdown()
    process.destroyForcibly()
})
```

---

## 十一、相关源文件索引

| 文件 | 说明 |
|------|------|
| `external/cangjie_compiler/src/main-macrosrv.cpp` | 服务端入口，参数解析与管道校验 |
| `external/cangjie_compiler/include/cangjie/Macro/InvokeUtil.h` | `MacroProcMsger`（管道通信）、`RuntimeInit`（运行时管理） |
| `external/cangjie_compiler/include/cangjie/Macro/MacroEvaluation.h` | `MacroEvaluation`（宏求值引擎）、客户端/服务端发送接收方法 |
| `external/cangjie_compiler/include/cangjie/Macro/MacroEvalMsgSerializer.h` | FlatBuffers 消息序列化/反序列化 |
| `external/cangjie_compiler/include/cangjie/Macro/MacroCall.h` | `MacroCall`、`MacroEvalStatus`、`ItemInfo` 等数据结构 |
| `external/cangjie_compiler/include/cangjie/Macro/MacroCommon.h` | `MacroCollector`、`MacroFormatter` 等辅助类 |
| `external/cangjie_compiler/schema/MacroMsgFormat.fbs` | FlatBuffers 消息 Schema 定义 |
| `analysis/src/main/kotlin/org/cangnova/cangjie/macro/compiler/CompilerMacroCompilationProvider.kt` | 插件中宏编译实现（`cjc --compile-macro`） |
| `analysis/src/main/kotlin/org/cangnova/cangjie/macro/compiler/CompilerMacroExpansionProvider.kt` | 插件中宏展开实现（`cjc-frontend --debug-macro`） |
| `analysis/src/main/kotlin/org/cangnova/cangjie/macro/compiler/MacroCallFileParser.kt` | 解析 `.macrocall` 文件 |