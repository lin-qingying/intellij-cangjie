# Proto Debugger 使用示例

本文档提供重构后的 proto_debugger 模块的使用示例。

## 基本使用

### 1. 初始化调试器

```kotlin
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration
import org.cangnova.cangjie.protodebugger.core.Handler
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType

// 创建事件处理器
val handler = object : Handler {
    override fun onInitialized(version: String, capabilities: Long) {
        println("Debugger initialized: $version")
    }

    override fun onProcessInterrupted(thread: LLThread?, frame: Model.StackFrame?) {
        println("Process stopped at ${frame?.location?.filePath}:${frame?.location?.line}")
    }

    // 实现其他事件处理方法...
}

// 创建配置
val configuration = DebuggerDriverConfiguration(
    projectPath = "/path/to/project",
    // 其他配置...
)

// 创建调试器门面
val debugger = DebuggerDriverFacade(
    handler = handler,
    configuration = configuration,
    architectureType = ArchitectureType.X86_64
)

// 等待连接
runBlocking {
    debugger.waitForConnection()
    println("Debugger connected on port ${debugger.port}")
}
```

### 2. 启动调试会话

```kotlin
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.protodebugger.util.Installer

runBlocking {
    // 创建安装器
    val installer = Installer(
        executableFile = File("/path/to/executable"),
        workingDirectory = File("/path/to/workdir")
    )

    // 加载并启动
    val inferior = debugger.sessionService.loadForLaunch(installer, "x86_64")
    val pid = inferior.start()

    println("Process started with PID: $pid")
}
```

### 3. 附加到进程

```kotlin
runBlocking {
    val processId = 12345
    val inferior = debugger.sessionService.loadForAttach(processId)
    inferior.start()

    println("Attached to process $processId")
}
```

## 断点管理

### 添加行断点

```kotlin
runBlocking {
    // 简单断点
    val result = debugger.breakpointService.addLineBreakpoint(
        path = "/path/to/source.cj",
        line = 42
    )

    println("Breakpoint added: id=${result.breakpoint.id}")

    // 条件断点
    val conditionalResult = debugger.breakpointService.addLineBreakpoint(
        path = "/path/to/source.cj",
        line = 100,
        condition = "x > 10"
    )
}
```

### 添加符号断点

```kotlin
runBlocking {
    // 函数断点
    val breakpoint = debugger.breakpointService.addSymbolicBreakpoint(
        symbolPattern = "main",
        module = null,
        condition = null
    )

    if (breakpoint != null) {
        println("Symbolic breakpoint added: id=${breakpoint.id}")
    }
}
```

### 添加观察点

```kotlin
runBlocking {
    val address = Address(0x1000)
    val watchpoint = debugger.breakpointService.addWatchpoint(
        address = address,
        size = 4,
        read = true,
        write = true,
        condition = null
    )

    println("Watchpoint added: id=${watchpoint.id}")
}
```

### 移除断点

```kotlin
runBlocking {
    val breakpointIds = listOf(1, 2, 3)
    debugger.breakpointService.removeBreakpoints(breakpointIds)

    println("Breakpoints removed")
}
```

## 执行控制

### 继续执行

```kotlin
runBlocking {
    val success = debugger.steppingService.resume()
    if (success) {
        println("Program resumed")
    }
}
```

### 单步执行

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        // 单步进入
        debugger.steppingService.stepInto(thread)

        // 单步跨过
        debugger.steppingService.stepOver(thread)

        // 单步跳出
        debugger.steppingService.stepOut(thread)
    }
}
```

### 运行到指定位置

```kotlin
runBlocking {
    // 运行到行
    debugger.steppingService.runToLine(
        path = "/path/to/source.cj",
        line = 50
    )

    // 运行到地址
    val address = Address(0x1234)
    debugger.steppingService.runToAddress(address)
}
```

### 跳转到指定位置

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        // 跳转到行
        val stopPlace = debugger.steppingService.jumpToLine(
            thread = thread,
            path = "/path/to/source.cj",
            line = 60,
            canLeaveFunction = false
        )

        println("Jumped to ${stopPlace.frame.file}:${stopPlace.frame.line}")
    }
}
```

## 表达式求值

### 求值表达式

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        val frames = debugger.sessionService.getFrames(thread, 0, 1)
        val frame = frames.firstOrNull()

        if (frame != null) {
            val value = debugger.evalService.evaluate(
                thread = thread,
                frame = frame,
                expression = "x + y"
            )

            println("Result: ${value.value}")
        }
    }
}
```

### 获取变量

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        val frames = debugger.sessionService.getFrames(thread, 0, 1)
        val frame = frames.firstOrNull()

        if (frame != null) {
            // 获取局部变量
            val variables = debugger.evalService.getVariables(thread, frame)

            variables.forEach { variable ->
                println("${variable.name} = ${variable.value}")
            }

            // 获取包含静态和全局变量
            val allVars = debugger.evalService.getVariables(
                threadId = thread.id,
                frameIndex = frame.index,
                statics = true,
                globals = true
            )
        }
    }
}
```

### 获取变量子元素

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        val frames = debugger.sessionService.getFrames(thread, 0, 1)
        val frame = frames.firstOrNull()

        if (frame != null) {
            val variables = debugger.evalService.getVariables(thread, frame)
            val arrayVar = variables.find { it.isArray }

            if (arrayVar != null) {
                // 获取数组元素
                val children = debugger.evalService.getVariableChildren(
                    value = arrayVar,
                    from = 0,
                    count = 10
                )

                println("Array has ${children.totalCount} elements")
                children.items.forEach { child ->
                    println("  [${child.name}] = ${child.value}")
                }
            }
        }
    }
}
```

## 内存操作

### 读取内存

```kotlin
runBlocking {
    val range = AddressRange(
        start = Address(0x1000),
        length = 256
    )

    val chunks = debugger.memoryService.readMemory(range)

    chunks.forEach { chunk ->
        println("Memory at ${chunk.address}: ${chunk.data.size} bytes")
        println("Readable: ${chunk.isReadable}, Writable: ${chunk.isWritable}")
    }
}
```

### 写入内存

```kotlin
runBlocking {
    if (debugger.memoryService.supportsMemoryWrite()) {
        val address = Address(0x1000)
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        debugger.memoryService.writeMemory(address, data)
        println("Memory written")
    }
}
```

## 反汇编

### 反汇编函数

```kotlin
runBlocking {
    val address = Address(0x1000)
    val fallbackRange = AddressRange(address, 256)

    val instructions = debugger.disasmService.disassembleFunction(
        address = address,
        fallbackRange = fallbackRange
    )

    instructions.forEach { inst ->
        println("${inst.address}: ${inst.mnemonic} ${inst.operands}")
    }
}
```

### 获取寄存器

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        val frames = debugger.sessionService.getFrames(thread, 0, 1)
        val frame = frames.firstOrNull()

        if (frame != null && debugger.disasmService.supportsRegisters()) {
            // 获取所有寄存器
            val registers = debugger.disasmService.getRegisters(thread, frame)

            registers.forEach { reg ->
                println("${reg.name} = ${reg.value}")
            }

            // 获取特定寄存器
            val specificRegs = debugger.disasmService.getRegisters(
                thread = thread,
                frame = frame,
                registerNames = setOf("rax", "rbx", "rcx")
            )
        }
    }
}
```

### 设置反汇编风格

```kotlin
runBlocking {
    debugger.disasmService.setDisasmFlavor(DisasmFlavor.INTEL)
    println("Disassembly flavor set to Intel")
}
```

## 线程和栈帧

### 获取线程列表

```kotlin
runBlocking {
    val threads = debugger.sessionService.getThreads()

    threads.forEach { thread ->
        println("Thread ${thread.id}: ${thread.name}")
        println("  Stopped: ${thread.isStopped}")
        println("  Stop reason: ${thread.stopReason}")
    }
}
```

### 获取栈帧

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        val frames = debugger.sessionService.getFrames(
            thread = thread,
            startFrame = 0,
            maxFrames = 20
        )

        frames.forEachIndexed { index, frame ->
            println("#$index ${frame.functionName} at ${frame.file}:${frame.line}")
        }
    }
}
```

## 路径映射

### 添加路径映射

```kotlin
runBlocking {
    // 添加路径映射
    debugger.sessionService.addPathMapping(
        index = 0,
        from = "/remote/path",
        to = "/local/path"
    )

    // 添加强制文件映射（带哈希验证）
    val hash = DebuggerSourceFileHash("SHA256", "abc123...")
    debugger.sessionService.addForcedFileMapping(
        index = 0,
        from = "/remote/file.cj",
        hash = hash,
        to = "/local/file.cj"
    )
}
```

## 符号管理

### 添加符号文件

```kotlin
runBlocking {
    val symbolsFile = File("/path/to/symbols.debug")
    val moduleFile = File("/path/to/module.so")

    debugger.sessionService.addSymbolsFile(
        symbols = symbolsFile,
        module = moduleFile
    )

    println("Symbols loaded")
}
```

## 控制台命令

### 执行解释器命令

```kotlin
runBlocking {
    val thread = debugger.getStoppedThread()
    if (thread != null) {
        val output = debugger.sessionService.executeInterpreterCommand(
            threadId = thread.id,
            frameIndex = 0,
            command = "p variable_name"
        )

        println("Command output: $output")
    }
}
```

### 命令补全

```kotlin
runBlocking {
    val completions = debugger.sessionService.completeConsoleCommand(
        command = "bre",
        pos = 3
    )

    println("Completions: ${completions.joinToString(", ")}")
}
```

## 清理资源

```kotlin
// 使用完毕后关闭调试器
debugger.close()
```

## 完整示例

```kotlin
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration
import org.cangnova.cangjie.protodebugger.core.Handler
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType
import org.cangnova.cangjie.protodebugger.util.Installer
import java.io.File

fun main() = runBlocking {
    // 创建处理器
    val handler = createDebuggerHandler()

    // 创建配置
    val configuration = DebuggerDriverConfiguration(
        projectPath = "/path/to/project"
    )

    // 创建调试器
    val debugger = DebuggerDriverFacade(
        handler = handler,
        configuration = configuration,
        architectureType = ArchitectureType.X86_64
    )

    try {
        // 等待连接
        debugger.waitForConnection()

        // 启动程序
        val installer = Installer(
            executableFile = File("/path/to/executable"),
            workingDirectory = File("/path/to/workdir")
        )
        val inferior = debugger.sessionService.loadForLaunch(installer, "x86_64")

        // 添加断点
        debugger.breakpointService.addLineBreakpoint("main.cj", 10)

        // 启动进程
        inferior.start()

        // 继续执行
        debugger.steppingService.resume()

        // 等待断点命中...
        // 事件会通过 Handler 回调处理

    } finally {
        // 清理
        debugger.close()
    }
}

fun createDebuggerHandler() = object : Handler {
    override fun onInitialized(version: String, capabilities: Long) {
        println("Debugger ready: $version")
    }

    override fun onProcessInterrupted(thread: LLThread?, frame: Model.StackFrame?) {
        println("Process stopped")
        // 处理停止事件
    }

    override fun onBreakpointHit(breakpointId: Int, thread: LLThread?, frame: Model.StackFrame?) {
        println("Breakpoint $breakpointId hit")
        // 处理断点命中
    }

    override fun onProcessExited(exitCode: Int, description: String?) {
        println("Process exited with code $exitCode")
    }

    // 实现其他必需的方法...
}
```

## 注意事项

1. **协程**: 所有服务方法都是 `suspend` 函数，必须在协程中调用
2. **资源管理**: 使用完毕后务必调用 `debugger.close()` 释放资源
3. **错误处理**: 服务方法可能抛出异常，需要适当的错误处理
4. **线程安全**: 服务层是线程安全的，可以从多个协程并发调用
5. **事件处理**: 调试器事件通过 `Handler` 回调异步通知
