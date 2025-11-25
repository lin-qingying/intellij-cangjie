# Proto Debugger 模块架构概览

## 概述

proto_debugger 模块为仓颉语言实现了基于协议的调试系统,通过 Protocol Buffers 与外部 LLDB 兼容的后端通信,提供完整的 IDE 调试功能。

## 模块结构

核心包:
- **core/**: CangJieDebugProcess、DebuggerDriverFacade、状态管理
- **breakpoint/**: 4种断点类型(行、地址、符号、观察点)
- **services/**: 6个服务(会话、断点、单步、求值、反汇编、内存)
- **transport/**: 协议通信(socket、命名管道)
- **data/**: 领域模型(LLDBFrame、LLDBThread、LLDBVariable 等)
- **execution/**: 状态管理和生命周期
- **runconfig/**: CjDebugRunner,用于 IntelliJ 集成

## 协议设计

### 请求-响应模式
- **基于哈希的匹配**: IDE 为请求生成唯一哈希值
- **后端返回相同哈希**: 响应携带相同哈希值
- **支持并发请求处理**: 通过哈希关联实现

### 消息类型
- **request.proto**: CreateTarget、Launch、Attach、单步、断点操作、变量访问
- **response.proto**: 所有请求类型的响应
- **event.proto**: 异步事件(ProcessStopped、ProcessRunning、ProcessExited 等)
- **model.proto**: 数据模型(Thread、Frame、Variable、Value、Register、Breakpoint 等)

## 核心组件

### CangJieDebugProcess (XDebugProcess)
- IntelliJ 调试的主入口点
- 管理生命周期(启动、停止、暂停、恢复)
- 注册断点处理器
- 管理控制台输出

### DebuggerDriverFacade
- 中央编排器,聚合所有 6 个服务
- 提供统一的调试接口
- 管理源文件验证
- 处理命令执行

### DebuggerStateManager
- 状态的单一数据源(Single Source of Truth)
- 跟踪: currentState、stoppedThread、capabilities、初始化状态
- 状态枚举: Idle(空闲) -> Running(运行) -> Paused(暂停) <-> Running -> Exiting(退出中) -> Terminated(已终止)

### 传输层
- 抽象的 Transport 接口及其实现:
  - **ServerSocketTransport** (TCP)
  - **WindowsPipe** (Windows 命名管道)
- **MessageBus**: 通过哈希处理请求-响应路由
- **EventBroadcastHandler**: 路由异步事件

## 服务(6个主要服务)

1. **SessionService**: 进程生命周期(启动、附加、线程、栈帧)
2. **BreakpointService**: 行/地址/符号/观察点断点
3. **SteppingService**: 步入/步过/步出、继续、暂停
4. **EvalService**: 表达式求值、变量访问/修改
5. **DisasmService**: 反汇编(4种模式)、寄存器
6. **MemoryService**: 内存读写操作

所有服务使用 `suspend fun` 实现异步操作。

## 断点系统

4种断点类型,每种都有 Type 和 Handler:
- **LineBreakpointType/Handler**: 源代码行断点
- **AddressBreakpointType/Handler**: 内存地址断点
- **SymbolicBreakpointType/Handler**: 函数/符号断点(支持正则表达式)
- **WatchpointBreakpointType/Handler**: 内存观察点(读/写)

### 生命周期:
1. 用户设置断点
2. Handler 创建 AddBreakpointRequest
3. 后端解析地址
4. 返回 AddBreakpointResponse
5. Handler 更新 IDE
6. 命中时:发送 ProcessStopped 事件(异步)
7. IDE 停止并更新视图

## IntelliJ 集成

- **CangJieDebugProcess** 继承 XDebugProcess
- **CangJieStackFrame** 继承 XStackFrame
- **CangJieSuspendContext** 继承 XSuspendContext
- **CangJieValue** 继承 XValue
- **LineBreakpointType/Handler** 继承 XLineBreakpointType/Handler
- **AddressBreakpointType/Handler** 继承类似接口
- **SymbolicBreakpointType/Handler** 继承类似接口
- **WatchpointBreakpointType/Handler** 继承类似接口

## 数据流转

```
后端响应 -> 领域模型(LLDB*) -> IntelliJ UI 模型(CangJie*) -> IntelliJ UI
```

**示例: 来自后端的变量**
- LLDBVariable (来自响应的领域模型)
- CangJieValue (IntelliJ UI 包装器)
- 变量视图显示

## 事件示例

### 设置断点
用户点击行号 -> LineBreakpointHandler -> AddBreakpointRequest -> 后端解析 -> AddBreakpointResponse -> 在 IDE 中标记行

### 断点命中
执行停止 -> ProcessStopped 事件 -> DebuggerHandler -> 状态更新 -> IDE 暂停

### 表达式求值
用户悬停变量 -> EvaluateRequest -> 后端求值 -> EvaluateResponse -> 显示值

## 关键设计模式

- **门面模式(Facade)**: DebuggerDriverFacade 聚合服务
- **状态机模式(State Machine)**: Target 状态转换
- **观察者模式(Observer)**: IntelliJ 回调
- **命令模式(Command)**: Protobuf 消息请求
- **适配器模式(Adapter)**: LLDB 数据到 IntelliJ UI 模型
- **策略模式(Strategy)**: 多种 Transport 实现
- **工厂模式(Factory)**: 消息和服务创建

## 线程安全

- **DebuggerStateManager**: 使用 @Volatile 确保原子操作
- **ExecutionStackCache**: 使用 ConcurrentHashMap
- **所有服务操作**: 使用 `suspend fun` 实现异步
- **MessageBus**: 基于哈希的路由处理并发请求

## 配置

**DebuggerSettings**:
- showHexValues(显示十六进制值)
- maxStringLength(最大字符串长度)
- maxArrayElements(最大数组元素数)
- expandPrivateMembers(展开私有成员)
- architecture(架构)

**DisasmOptions**:
- showMachineCode(显示机器码)
- flavor(intel/att 语法)
- symbolizeAddresses(符号化地址)

## 错误处理

**异常层次结构**:
- TransportException (基类)
  - TransportTimeoutException (超时异常)
  - TransportConnectionException (连接异常)
- DebuggerCommandException (调试器命令异常)

所有响应都包含 Status,带有成功标志和消息。

## 依赖关系

**proto_debugger 依赖于**:
- plugin (注册)
- psi (语法)
- common、messages、toolchain、cangjie-project

**外部依赖**:
- IntelliJ Platform
- Protocol Buffers 3.24.4
- Kotlin Coroutines

## 总结

Proto_debugger 提供:
1. 基于协议的 LLDB 后端通信
2. 6 个专业化服务
3. 完整的 IntelliJ 集成
4. 4 种断点类型
5. 表达式求值
6. 底层调试(反汇编、寄存器、内存)
7. 异步/协程操作
8. 健壮的状态管理
9. 多种传输方式
10. 全面的错误处理

**优势**: 清晰的架构、关注点分离、可扩展性、可维护性。

## 详细功能说明

### 1. 会话管理(SessionService)

**核心操作**:
- `createTarget(executablePath)`: 创建调试目标
- `launch(args, env, workingDir)`: 启动进程
- `attach(pid)`: 附加到现有进程
- `getThreads()`: 获取所有线程
- `getFrames(threadId)`: 获取线程的栈帧
- `detach()`: 分离调试器
- `terminate()`: 终止进程

**数据模型**:
- **LLDBThread**: 线程信息(id、name、stopReason、frames)
- **LLDBFrame**: 栈帧信息(index、function、file、line、address)

### 2. 断点管理(BreakpointService)

**核心操作**:
- `addBreakpoint(file, line)`: 添加行断点
- `addBreakpoint(address)`: 添加地址断点
- `addSymbolicBreakpoint(symbol, regex)`: 添加符号断点
- `addWatchpoint(address, size, type)`: 添加观察点
- `removeBreakpoint(id)`: 删除断点
- `enableBreakpoint(id, enable)`: 启用/禁用断点
- `setCondition(id, condition)`: 设置断点条件
- `setHitCount(id, count)`: 设置命中计数

**数据模型**:
- **LLDBBreakpoint**: 断点信息(id、type、location、enabled、hitCount、condition)
- **LLDBWatchpoint**: 观察点信息(id、address、size、type、hitCount)

### 3. 单步执行(SteppingService)

**核心操作**:
- `continue()`: 继续执行
- `pause()`: 暂停执行
- `stepOver(threadId)`: 步过
- `stepInto(threadId)`: 步入
- `stepOut(threadId)`: 步出
- `runToLocation(file, line)`: 运行到指定位置

**状态转换**:
```
Paused -> continue() -> Running
Running -> pause() -> Paused
Paused -> stepOver() -> Running -> (自动停止) -> Paused
```

### 4. 表达式求值(EvalService)

**核心操作**:
- `evaluate(expression, frameId)`: 求值表达式
- `getVariables(frameId)`: 获取局部变量
- `getChildren(variableId)`: 获取变量子项
- `setValue(variableId, newValue)`: 修改变量值
- `getGlobalVariables()`: 获取全局变量

**数据模型**:
- **LLDBVariable**: 变量信息(name、type、value、valueId、hasChildren)
- **LLDBValue**: 值信息(summary、type、numChildren、isPointer)

### 5. 反汇编(DisasmService)

**核心操作**:
- `disassemble(mode, ...)`: 反汇编
  - **Mode.FUNCTION**: 反汇编整个函数
  - **Mode.ADDRESS_RANGE**: 反汇编地址范围
  - **Mode.PC_CONTEXT**: 反汇编当前 PC 附近
  - **Mode.FRAME**: 反汇编栈帧
- `getRegisters(threadId)`: 获取寄存器
- `setRegister(threadId, regName, value)`: 设置寄存器值

**数据模型**:
- **LLDBDisasmInstruction**: 指令信息(address、mnemonic、operands、bytes、comment)
- **LLDBRegister**: 寄存器信息(name、value、size、type)
- **LLDBRegisterGroup**: 寄存器组(name、registers)

### 6. 内存操作(MemoryService)

**核心操作**:
- `readMemory(address, size)`: 读取内存
- `writeMemory(address, data)`: 写入内存
- `searchMemory(pattern, startAddr, endAddr)`: 搜索内存

**数据模型**:
- **LLDBMemoryHunk**: 内存块(address、data、size)

## 协议消息详解

### Request 消息类型

```protobuf
// 会话相关
CreateTargetRequest { executable_path }
LaunchRequest { args, env, working_dir }
AttachRequest { pid }
DetachRequest {}
TerminateRequest {}

// 断点相关
AddBreakpointRequest { file, line, condition, hit_count }
AddBreakpointByAddressRequest { address }
AddSymbolicBreakpointRequest { symbol, regex }
AddWatchpointRequest { address, size, type }
RemoveBreakpointRequest { breakpoint_id }
EnableBreakpointRequest { breakpoint_id, enable }

// 执行控制
ContinueRequest {}
PauseRequest {}
StepOverRequest { thread_id }
StepIntoRequest { thread_id }
StepOutRequest { thread_id }

// 求值相关
EvaluateRequest { expression, frame_id }
GetVariablesRequest { frame_id }
GetChildrenRequest { variable_id }
SetValueRequest { variable_id, new_value }

// 反汇编
DisassembleRequest { mode, address, size }
GetRegistersRequest { thread_id }
SetRegisterRequest { thread_id, register_name, value }

// 内存
ReadMemoryRequest { address, size }
WriteMemoryRequest { address, data }
```

### Response 消息类型

每个请求都有对应的响应:
```protobuf
CreateTargetResponse { status, target_id }
LaunchResponse { status, process_id }
AddBreakpointResponse { status, breakpoint }
EvaluateResponse { status, result }
// ... 等等
```

### Event 消息类型

```protobuf
ProcessStoppedEvent {
  thread_id
  stop_reason  // BREAKPOINT, STEP, SIGNAL, etc.
  breakpoint_id
  signal_name
}

ProcessRunningEvent {
  thread_id
}

ProcessExitedEvent {
  exit_code
  description
}

BreakpointChangedEvent {
  breakpoint_id
  change_type  // ADDED, REMOVED, MODIFIED, RESOLVED
  breakpoint
}

ThreadCreatedEvent { thread }
ThreadExitedEvent { thread_id }

OutputEvent {
  type  // STDOUT, STDERR
  text
}
```

## 扩展点

### 添加新的断点类型

1. 创建新的 `XBreakpointType` 实现
2. 创建对应的 `XBreakpointHandler` 实现
3. 在 `CangJieDebugProcess` 中注册处理器
4. 在后端实现相应的协议支持

### 添加新的服务

1. 在 `services/` 下创建服务接口
2. 在 `services/impl/` 下创建实现
3. 在 `DebuggerDriverFacade` 中聚合新服务
4. 扩展 protobuf 协议添加新的请求/响应

### 添加新的传输方式

1. 实现 `Transport` 接口
2. 在配置中添加传输方式选择
3. 在 `DebuggerDriverFacade` 初始化时选择相应传输

## 性能优化

### 1. 缓存机制
- **ExecutionStackCache**: 缓存栈帧信息
- **变量懒加载**: 只在展开时加载子项

### 2. 异步操作
- 所有 I/O 操作使用协程
- 避免阻塞 UI 线程

### 3. 批量操作
- 批量获取变量而非逐个请求
- 批量设置断点

### 4. 超时控制
- 所有请求都有超时机制
- 防止无限等待

## 调试流程示例

### 完整调试会话流程

```
1. 用户点击"调试"按钮
   ↓
2. CjDebugRunner.execute()
   ↓
3. CangJieDebugProcess.sessionInitialized()
   ↓
4. SessionService.createTarget(executable)
   ↓
5. SessionService.launch(args, env)
   ↓
6. ProcessRunningEvent 触发
   ↓
7. 用户在第 42 行设置断点
   ↓
8. LineBreakpointHandler.registerBreakpoint()
   ↓
9. BreakpointService.addBreakpoint(file, 42)
   ↓
10. AddBreakpointResponse 返回 (id=1, address=0x4000)
    ↓
11. IDE 显示断点标记
    ↓
12. 程序执行到 0x4000
    ↓
13. ProcessStoppedEvent(reason=BREAKPOINT, bp_id=1)
    ↓
14. DebuggerStateManager.state = Paused
    ↓
15. SessionService.getThreads() 获取所有线程
    ↓
16. SessionService.getFrames(thread_id) 获取栈帧
    ↓
17. IDE 显示调用栈
    ↓
18. 用户悬停变量 "x"
    ↓
19. EvalService.evaluate("x", frame_id)
    ↓
20. EvaluateResponse 返回值
    ↓
21. IDE 显示变量值
    ↓
22. 用户点击"步过"
    ↓
23. SteppingService.stepOver(thread_id)
    ↓
24. ProcessRunningEvent 触发
    ↓
25. ProcessStoppedEvent(reason=STEP)
    ↓
26. IDE 更新到下一行
    ↓
27. 用户点击"继续"
    ↓
28. SteppingService.continue()
    ↓
29. ProcessRunningEvent 触发
    ↓
30. 程序正常退出
    ↓
31. ProcessExitedEvent(exit_code=0)
    ↓
32. DebuggerStateManager.state = Terminated
    ↓
33. 调试会话结束
```

## 架构优势

### 1. 分层清晰
- **传输层**: 只负责消息收发
- **协议层**: 只负责序列化/反序列化
- **服务层**: 实现具体调试功能
- **UI层**: IntelliJ 集成

### 2. 松耦合
- 服务之间通过 Facade 访问
- UI 与后端通过领域模型隔离
- 传输方式可插拔

### 3. 易测试
- 每个服务可独立测试
- 可 Mock Transport 进行单元测试
- 协议层可独立验证

### 4. 易扩展
- 新增服务不影响现有服务
- 新增断点类型只需实现接口
- 新增传输方式只需实现 Transport

### 5. 高性能
- 协程异步操作
- 缓存机制减少重复请求
- 并发请求通过哈希路由

## 未来改进方向

1. **性能监控**: 添加性能指标收集
2. **日志增强**: 更详细的调试日志
3. **错误恢复**: 更智能的错误恢复策略
4. **协议版本**: 支持协议版本协商
5. **断点优化**: 断点自动迁移(源码修改后)
6. **表达式缓存**: 缓存常用表达式求值结果
7. **远程调试**: 支持远程机器调试
8. **多进程调试**: 同时调试多个进程