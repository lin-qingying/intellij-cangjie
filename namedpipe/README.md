# Kotlin Named Pipe — IntelliJ 优化版

跨平台命名管道库，针对 IntelliJ 平台插件开发场景整合了以下三个库：

| 库 | 替代内容 | 效果 |
|---|---|---|
| **jna-platform `Kernel32`** | 手写 `Kernel32Ex` interface + `WString` 转换 | 删除 ~80 行 binding 代码 |
| **Okio** | 手写 `HandleInputStream/OutputStream` + `BufferedInputStream/OutputStream` 包装 | 统一 `source`/`sink` API，`readUtf8Line()` 直接可用 |
| **kotlinx-coroutines** | `Executors.newSingleThreadExecutor` + 回调风格 `onConnected/onError` | 改为 `suspend fun`，减少约 60% 异步代码 |
| **kotlin-logging** | 裸 `println` | 在 IntelliJ 工具窗口中正确路由日志 |

## 核心变化对比

### 异步连接（原 vs 新）

```kotlin
// ❌ 原来：Executor + 回调，约 15 行
fun waitForConnectionAsync(onConnected: () -> Unit, onError: (PipeException) -> Unit) {
    val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "pipe-server").apply { isDaemon = true }
    }
    executor.submit {
        try { waitForConnection(); onConnected() }
        catch (e: PipeException) { onError(e) }
        finally { executor.shutdown() }
    }
}

// ✅ 现在：coroutines，1 行
override suspend fun waitForConnectionAsync() =
    withContext(Dispatchers.IO) { waitForConnection() }
```

### I/O 读写（原 vs 新）

```kotlin
// ❌ 原来：手工包装 InputStream + PrintWriter
fun NamedPipe.writeLine(line: String) {
    val writer = PrintWriter(outputStream, false, Charsets.UTF_8)
    writer.println(line); writer.flush()
}

// ✅ 现在：Okio sink 直接调用
fun NamedPipe.writeLine(line: String) {
    sink.writeUtf8(line).writeUtf8("\n").emit()
}

// ✅ 也可以直接访问 Okio source，不经过扩展函数
pipe.source.readUtf8Line()
pipe.sink.writeUtf8("hello\n").emit()
```

### Windows API（原 vs 新）

```kotlin
// ❌ 原来：手写 Kernel32Ex interface + Native.load + WString 包装
internal interface Kernel32Ex : Library {
    fun CreateNamedPipeW(lpName: WString, ...): WinNT.HANDLE
    fun WaitNamedPipeW(lpNamedPipeName: WString, ...): Boolean
    // ...共 10+ 个函数声明
}

// ✅ 现在：jna-platform 官方 Kernel32，String 直接传入
val k32 = Kernel32.INSTANCE
k32.CreateNamedPipe(pipePath, ...)     // 接受 String，内部处理 WString 转换
k32.WaitNamedPipe(pipePath, timeout)
Kernel32Util.closeHandle(handle)       // 工具类封装
```

## 快速开始

### 同步 DSL
```kotlin
val factory = NamedPipeFactory.create()

// 服务端
factory.createServer(PipeConfig("echo")).serve { server ->
    server.writeLine("ECHO:" + server.readLine()!!)
}

// 客户端
factory.createClient(PipeConfig("echo")).call { client ->
    client.writeLine("ping")
    println(client.readLine())  // → ECHO:ping
}
```

### 协程 DSL
```kotlin
coroutineScope {
    launch(Dispatchers.IO) {
        factory.createServer(config).serveAsync { s ->
            s.writeLine("ECHO:" + s.readLine()!!)
        }
    }
    delay(100)
    factory.createClient(config).callAsync { c ->
        c.writeLine("ping")
        println(c.readLine())
    }
}
```

### 直接访问 Okio API
```kotlin
pipe.sink.writeUtf8("hello\n").emit()
val line = pipe.source.readUtf8Line()
val bytes = pipe.source.readByteString(1024)
```

## 依赖

```kotlin
implementation("net.java.dev.jna:jna:5.14.0")
implementation("net.java.dev.jna:jna-platform:5.14.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
implementation("com.squareup.okio:okio:3.9.0")
implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
```
