# Proto Debugger 模块架构概览

## 概述

proto_debugger 模块为仓颉语言实现了基于协议的调试系统,通过 Protocol Buffers 与外部 LLDB 兼容的后端通信,提供完整的 IDE
调试功能。

## 模块结构

核心包:

- **core/**: CangJieDebugProcess、DebuggerDriverFacade、状态管理、表达式求值器
- **breakpoint/**: 4种断点类型(行、地址、符号、观察点) + 通用基类
- **services/**: 6个服务(会话、断点、单步、求值、反汇编、内存)
- **transport/**: 协议通信(socket、命名管道)
- **data/**: 领域模型(LLDBFrame、LLDBThread、LLDBVariable 等)
- **execution/**: 状态管理和生命周期(async、exit、state)
- **location/**: 源代码位置解析和处理
- **memory/**: 内存视图和反汇编视图完整实现
- **runconfig/**: CjDebugRunner,用于 IntelliJ 集成
- **console/**: 控制台输出支持
- **output/**: 日志文件监控和读取

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
- `runToAddress(address)`: 运行到指定内存地址
- `runToLine(file, line)`: 运行到源代码指定行

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
    CreateTargetRequest {executable_path}
LaunchRequest {args, env, working_dir}
AttachRequest {pid}
DetachRequest {}
TerminateRequest {}

// 断点相关
    AddBreakpointRequest {file, line, condition, hit_count}
AddBreakpointByAddressRequest {address}
AddSymbolicBreakpointRequest {symbol, regex}
AddWatchpointRequest {address, size, type}
RemoveBreakpointRequest {breakpoint_id}
EnableBreakpointRequest {breakpoint_id, enable}

// 执行控制
    ContinueRequest {}
PauseRequest {}
StepOverRequest {thread_id}
StepIntoRequest {thread_id}
StepOutRequest {thread_id}

// 求值相关
    EvaluateRequest {expression, frame_id}
GetVariablesRequest {frame_id}
GetChildrenRequest {variable_id}
SetValueRequest {variable_id, new_value}

// 反汇编
    DisassembleRequest {mode, address, size}
GetRegistersRequest {thread_id}
SetRegisterRequest {thread_id, register_name, value}

// 内存
    ReadMemoryRequest {address, size}
WriteMemoryRequest {address, data}
```

### Response 消息类型

每个请求都有对应的响应:

```protobuf
CreateTargetResponse {status, target_id}
LaunchResponse {status, process_id}
AddBreakpointResponse {status, breakpoint}
EvaluateResponse {status, result}
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

ThreadCreatedEvent {thread}
ThreadExitedEvent {thread_id}

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

---

## 详细设计文档

### 内存视图系统设计

内存视图系统采用 Redux 风格的状态管理架构，提供十六进制内存查看和反汇编代码查看功能。

#### 架构组件

**MemoryViewFacade**

- 作为内存视图子系统的门面，协调各组件交互
- 管理 HexStore (十六进制视图) 和 DisasmStore (反汇编视图)
- 提供地址跳转、内存加载等统一接口

**MemoryStore<T: MemoryCell>**

- 基于不可变状态的存储容器
- 状态模型: `MemoryState<T>` 包含单元集合、地址映射、元数据
- 状态更新: 通过纯函数产生新状态，保证线程安全
- 事件通知: 状态变化时通知订阅者更新 UI

**MemoryCell 类型**

- `HexCell`: 十六进制字节单元
    - address: ULong - 内存地址
    - byte: Byte - 字节值
    - isModified: Boolean - 是否已修改
- `InstructionCell`: 反汇编指令单元
    - address: ULong - 指令地址
    - mnemonic: String - 助记符
    - operands: String - 操作数
    - bytes: ByteArray - 机器码

**Address & AddressRange**

- Address: 封装 64 位内存地址，提供格式化和运算
- AddressRange: 地址范围，支持包含判断、交集运算

#### 虚拟文件系统集成

**MemoryViewVirtualFileSystem**

- 继承 VirtualFileSystem，将内存视图作为虚拟文件
- 协议: `memoryview://hex/...` 和 `memoryview://disasm/...`
- 文件刷新: 监听 Store 状态变化，触发文件内容更新

**MemoryViewFile<T>**

- 关联 MemoryStore，代理其状态
- 实现 VirtualFile 接口，提供文件元数据
- 支持 IntelliJ 文件系统操作 (打开、刷新、关闭)

#### 编辑器实现

**MemoryEditor**

- 扩展 TextEditor，提供自定义编辑器实现
- 配置管道: EditorPipeline 串联各种配置器
- 支持编辑操作: 十六进制内存可编辑，反汇编只读

**EditorConfigurators**

- AddressLineNumberConfigurator: 配置地址显示
- ReadOnlyConfigurator: 配置只读模式
- HighlightingConfigurator: 配置语法高亮

**MemoryAddressLineNumberConverter**

- 实现 LineNumberConverter 接口
- 将编辑器行号转换为内存地址显示
- 格式: "0x00401000" 而非 "1"

#### 语法高亮支持

**HexdumpLanguage**

- 定义自定义语言 "Hexdump"
- 关联文件类型 HexdumpFileType
- 支持语法解析和高亮

**HexdumpLexer**

- 词法分析器，识别 TOKEN 类型
- ADDRESS: 地址部分 (0x...)
- HEX_DATA: 十六进制数据 (48 8B ...)
- ASCII: ASCII 显示部分

**HexdumpSyntaxHighlighter**

- 定义各 TOKEN 的显示样式
- ADDRESS: 灰色
- HEX_DATA: 蓝色
- ASCII: 绿色

**HexdumpFoldingBuilder**

- 实现代码折叠功能
- 支持按地址范围折叠内存块

#### UI 扩展

**MemoryViewFileIconProvider**: 提供文件图标 (内存、反汇编图标)
**MemoryViewTabTitleProvider**: 自定义标签页标题 "Memory: 0x401000"
**MemoryGutterIconProvider**: 编辑器左侧装订线图标 (断点、当前行)
**MemoryViewReaderModeMatcher**: 控制编辑器只读模式
**MemoryViewFileWritingAccessProvider**: 控制写权限

#### Actions 设计

**GoToAddressAction**

- 弹出对话框输入地址
- 跳转到指定内存地址
- 自动加载该地址附近的内存

**DisassembleFunctionAction**

- 从当前栈帧反汇编函数
- 支持源码和反汇编分屏显示
- 调用 DisasmService.disassemble()

**ViewMemoryAction**

- 从栈帧的 PC 地址打开内存视图
- 对齐到 512 字节边界
- 异步加载数据

#### 数据加载器

**MemoryCellLoader<T>**

- 抽象接口，定义数据加载契约
- `load(range: AddressRange): List<T>`

**HexCellLoader**

- 实现十六进制数据加载
- 调用 MemoryService.readMemory()
- 将字节数组转换为 HexCell 列表

**DisasmCellLoader**

- 实现反汇编数据加载
- 调用 DisasmService.disassemble()
- 将指令列表转换为 InstructionCell 列表

#### 渲染设计

**SnapshotRenderer**

- 将 MemoryState 渲染为编辑器文本
- 支持两种格式:
    - Hexdump 格式: 地址 + 十六进制 + ASCII
    - Disassembly 格式: 地址 + 助记符 + 操作数
- 维护行号到地址的映射 (用于断点、跳转)

**HexStore 渲染示例**:

```
0x00401000  48 8B 45 F8 48 89 C7 E8  H.E.H...
0x00401008  23 00 00 00 48 89 C2 48  #...H..H
```

**DisasmStore 渲染示例**:

```
0x00401000  mov    rax, qword ptr [rbp-0x8]
0x00401004  mov    rdi, rax
0x00401007  call   0x401030
```

#### 工作流程

**打开内存视图**:

1. 用户触发 ViewMemoryAction (从栈帧或菜单)
2. 获取目标地址 (PC 或用户输入)
3. 创建 MemoryViewFile 关联对应 Store
4. 通过 FileEditorManager 打开编辑器
5. 异步加载初始数据

**滚动加载数据**:

1. 用户滚动编辑器到新区域
2. EditorScrollListener 检测到滚动事件
3. 计算当前可见地址范围
4. 检查 Store 中是否已有数据
5. 如无数据，调用 Loader 异步加载
6. 加载完成后更新 State，触发 UI 刷新

**编辑内存**:

1. 用户在十六进制视图修改字节
2. DocumentListener 捕获修改事件
3. 解析修改的地址和新值
4. 调用 MemoryService.writeMemory()
5. 更新 Store 中的 Cell，标记为已修改
6. 渲染器用不同颜色显示已修改单元

**设置地址断点**:

1. 用户在反汇编视图点击行号
2. AddressBreakpointType.canPutAt() 验证可行性
3. AddressBreakpointHandler.registerBreakpoint()
4. 从 Store.getAddressForLine() 获取地址
5. 调用 BreakpointService.addAddressBreakpoint()
6. MemoryGutterIconProvider 显示断点图标

### 表达式求值设计

#### CangJieDebuggerEvaluator

实现 XDebuggerEvaluator 接口，提供表达式求值和变量查看功能。

**核心方法**:

**evaluate(expression, callback, position)**

- 求值表达式字符串
- 调用 EvalService.evaluate()
- 将 LLDBVariable 转换为 CangJieValue
- 通过 callback 返回结果给 IDE

**getExpressionRangeAtOffset(project, document, offset, sideEffectsAllowed)**

- 实现鼠标悬浮提示功能
- 从文档偏移量获取 PSI 元素
- 向上遍历 PSI 树查找可求值表达式
- 返回表达式的文本范围

**getEvaluationMode(text, startOffset, endOffset, psiFile)**

- 返回求值模式 CODE_FRAGMENT
- 支持复杂表达式求值

#### 表达式识别

**findEvaluatableExpression(element, sideEffectsAllowed)**

- 从 PSI 元素向上查找可求值的表达式节点
- 支持的表达式类型:
    - CjSimpleNameExpression: 简单变量 `x`
    - CjReferenceExpression: 属性访问 `obj.field`
    - CjArrayAccessExpression: 数组访问 `arr[0]`
    - CjBinaryExpression: 二元运算 `a + b`
    - CjConstantExpression: 常量 `42`
    - CjCallExpression: 函数调用 `func()` (仅当 sideEffectsAllowed 为 true)
- 停止条件: 遇到 CjBlockExpression 或 CjDeclaration

**副作用控制**:

- sideEffectsAllowed = false: 仅允许无副作用表达式 (悬浮提示场景)
- sideEffectsAllowed = true: 允许函数调用等有副作用操作 (Evaluate Expression 对话框)

#### 变量修改

**CangJieValueModifier**

- 实现 XValueModifier 接口
- 关联 LLDBVariable 和 DebuggerDriverFacade
- setValue() 方法实现:
    1. 解析用户输入的新值表达式
    2. 调用 EvalService.setValue(variable, newValue)
    3. 清除缓存值 (putUserData(LLDBVARIABLE_VALUE, null))
    4. 触发 UI 刷新

**EvalService.setValue() 实现**:

1. 获取 variableId
2. 构建 SetVariableValueRequest
3. 通过 MessageBus 发送请求
4. 等待 SetVariableValueResponse
5. 检查状态返回结果

#### 悬浮提示工作流程

```
1. 用户鼠标悬停在 "myVariable" 上
   ↓
2. IDE 调用 getExpressionRangeAtOffset(offset)
   ↓
3. 获取 offset 处的 PSI 元素
   ↓
4. findEvaluatableExpression() 向上查找
   ↓
5. 找到 CjSimpleNameExpression "myVariable"
   ↓
6. 返回其 TextRange
   ↓
7. IDE 提取文本 "myVariable"
   ↓
8. IDE 调用 evaluate("myVariable", callback)
   ↓
9. EvalService.evaluate() 求值
   ↓
10. 返回 LLDBVariable
    ↓
11. 转换为 CangJieValue
    ↓
12. 显示悬浮提示框
```

### 断点系统设计

#### BaseBreakpointHandler

通用断点处理器基类，封装公共逻辑。

**类型参数**:

- B: Breakpoint 类型 (XLineBreakpoint<P>)
- T: BreakpointType 类型
- P: Properties 类型

**核心方法**:

- `registerBreakpoint()`: 注册断点到调试器
- `unregisterBreakpoint()`: 从调试器移除断点
- `setEnabled()`: 启用/禁用断点

**抽象方法** (子类实现):

- `getBreakpointIdentifier()`: 获取断点唯一标识
- `isValidBreakpoint()`: 验证断点有效性
- `sendBreakpointToDebugger()`: 发送到调试器后端
- `removeBreakpointFromDebugger()`: 从后端移除
- `shouldBreakpointBeRemoved()`: 判断是否应该移除

**状态管理**:

- ideToDebuggerMap: IDE 断点到后端 ID 的映射
- 使用 ConcurrentHashMap 保证线程安全

#### 地址断点特殊处理

**AddressBreakpointHandler**

- 地址断点的 sourcePosition 始终为 null
- 使用 fileUrl + VirtualFileManager 获取文件
- 从 MemoryViewFile 的 Store 动态获取地址

**getAddressFromBreakpoint()**:

1. 通过 fileUrl 获取 VirtualFile
2. 检查是否为 MemoryViewFile
3. 获取其 store (MemoryStore<InstructionCell>)
4. 调用 store.getAddressForLine(line)
5. 返回该行对应的内存地址

**Properties 设计**:

- 只存储 hitCount (命中次数)
- 不存储 address (动态获取)
- 简化序列化和持久化

### 单步执行增强

#### 运行到光标位置

**runToAddress(address)**

- 使用场景: 反汇编视图中运行到指定指令
- 协议: RunToCursorRequest { thread_id, address }
- 实现方式: 后端使用 LLDB 的 RunToAddress API 或临时断点

**runToLine(file, line)**

- 使用场景: 源代码视图中运行到指定行
- 协议: RunToCursorRequest { thread_id, source_location }
- 实现方式: 后端设置临时断点并继续执行

**自动获取线程**:

- 从 DebuggerStateManager.getStoppedThread() 获取当前停止的线程
- 如果没有停止的线程，抛出异常

**协议设计**:

```protobuf
message RunToCursorRequest {
  uint64 thread_id = 1;
  oneof target {
    uint64 address = 2;           // 地址目标
    SourceLocation location = 3;  // 源代码目标
  }
}

message RunToCursorResponse {
  Status status = 1;
  optional uint64 temp_breakpoint_id = 2;  // 临时断点ID(如果使用)
}
```

### 传输层设计

#### ServerSocketTransport

基于 TCP Socket 的传输实现，支持协程和异步操作。

**连接建立**:

1. 创建 ServerSocket 绑定本地端口
2. 启动 Acceptor 线程等待连接
3. 后端连接后启动 Reader 线程
4. 开始消息通信

**消息格式**:

```
[4 bytes: message size (big-endian)] [N bytes: protobuf message]
```

**哈希匹配机制**:

- 请求中包含 hash 字段 (唯一标识)
- 响应中返回相同的 hash 值
- pendingResponses: Map<Long, ResponseWaiter> 存储等待的请求
- 通过 hash 匹配请求和响应，支持并发请求

**错误处理增强**:

**消息大小验证**:

- MAX_MESSAGE_SIZE = 100MB
- 防止恶意或错误的大消息

**解析错误诊断**:

```kotlin
if (size !in 1..MAX_MESSAGE_SIZE) {
    // 十六进制转储
    val hexDump = bytes.joinToString(" ") { "%02X".format(it) }
    // ASCII 转储
    val asciiDump = bytes.map {
        if (it in 32..126) it.toChar() else '.'
    }.joinToString("")

    LOG.error("Invalid size: $size\nHex: $hexDump\nASCII: $asciiDump")

    // 尝试重新同步
    buffer.position(currentPos + 1)
    continue
}
```

**缓冲区重新同步**:

- 遇到无效消息不直接中断连接
- 跳过坏字节，尝试找到下一个有效消息
- 提高协议容错能力

**线程模型**:

- Acceptor 线程: 等待后端连接
- Reader 线程: 循环读取消息
- 协程: 异步处理业务逻辑

### 日志和输出系统

#### LogOutputManager

管理调试器的各种日志输出。

**日志类型** (LogType):

- LLDB: LLDB 调试器日志
- GDB: GDB 日志 (保留)
- MI: MI 协议日志
- DAP: DAP 协议日志

**功能**:

- 配置日志文件路径
- 启动日志监控
- 输出到 IDE 控制台

#### LogFileMonitor

监控日志文件的变化并实时读取。

**实现**:

- 使用 WatchService 监控文件系统事件
- 支持 ENTRY_CREATE、ENTRY_MODIFY、ENTRY_DELETE
- 检测到修改时通知 Reader 读取新内容

**优势**:

- 高效: 操作系统级别的文件监控
- 实时: 文件变化时立即响应
- 跨平台: Java NIO 统一 API

#### LogFileReader

读取日志文件内容。

**增量读取**:

- 记录上次读取位置
- 每次只读取新增内容
- 避免重复读取

**编码处理**:

- 支持 UTF-8 编码
- 处理不同操作系统的行结束符

#### ConsoleOutputProvider

提供控制台输出功能。

**集成**:

- 实现 ConsoleView 接口
- 注册到 CangJieDebugProcess
- 接收 OutputEvent 并显示

**输出类型**:

- STDOUT: 程序标准输出 (黑色)
- STDERR: 程序错误输出 (红色)

### 位置信息处理

#### SourceLocation

封装源代码位置信息。

**字段**:

- filePath: String - 文件路径
- line: Int - 行号 (从 1 开始)
- column: Int? - 列号 (可选)

**方法**:

- toString(): 格式化为 "file.cj:42:10"
- toXSourcePosition(): 转换为 IntelliJ 的 XSourcePosition

#### LocationParser

解析各种位置字符串格式。

**支持的格式**:

- "file.cj:42" - 文件和行
- "file.cj:42:10" - 文件、行、列
- "0x401000" - 内存地址
- "main+0x10" - 符号偏移

**实现**:

- 正则表达式匹配
- 路径解析 (相对/绝对)
- 错误处理和验证

### 辅助组件

#### CangJieAlternativeSourceHandler

处理源文件不可用的情况。

**功能**:

- 查找备用源文件位置
- 支持符号文件映射
- 处理调试信息中的路径

#### CangJieValue

XValue 的完整实现，表示调试器中的值。

**功能**:

- 格式化值显示 (类型、值、摘要)
- 支持变量展开 (子项懒加载)
- 支持值修改 (通过 CangJieValueModifier)
- 提供图标和演示样式

**子项加载**:

1. 用户展开变量
2. computeChildren() 被调用
3. 调用 EvalService.getVariableChildren()
4. 分页加载子项 (PagedResult)
5. 显示到 Variables 视图

#### DebuggerProcessFactory

工厂类，负责创建和配置调试器进程。

**职责**:

- 选择传输方式 (Socket/Pipe)
- 初始化 Transport
- 创建 DebuggerDriverFacade
- 配置各种服务

#### PagedResult<T>

分页结果容器，支持增量加载。

**状态**:

- items: List<T> - 当前页的数据
- hasMore: Boolean - 是否有更多数据

**工厂方法**:

- empty(): 空结果
- complete(items): 完整结果
- partial(items): 部分结果 (还有更多)

**使用场景**:

- 大数组的子项加载
- 大集合的元素显示
- 内存块的增量加载

### 状态管理重构

#### execution/async/

**AsyncResult<T>**

- 封装异步操作结果
- 支持成功、失败、取消状态

**FutureUtils**

- Future 相关工具方法
- 超时控制、异常处理

#### execution/exit/

**ExitStatus**

- 枚举进程退出状态
- NORMAL: 正常退出 (exit code)
- SIGNALED: 信号终止 (signal name)
- ERROR: 错误终止 (error message)

#### execution/state/

**TargetState**

- 枚举调试目标状态
- Idle → Running → Paused ↔ Running → Exiting → Terminated

**StateTransitions**

- 定义合法的状态转换
- 验证转换是否允许
- 提供转换条件检查

### 设计模式应用

#### Redux 模式 (内存视图)

**原则**:

- Single Source of Truth: MemoryState 是唯一数据源
- State is Read-Only: 状态不可变
- Changes are Pure Functions: 状态更新函数无副作用

**实现**:

```kotlin
data class MemoryState<T : MemoryCell>(
    val cells: Map<ULong, T>,
    val lineToAddressMap: Map<Int, ULong>,
    val loadedRanges: List<AddressRange>
)

fun updateState(oldState: MemoryState<T>, newCells: List<T>): MemoryState<T> {
    return oldState.copy(
        cells = oldState.cells + newCells.associateBy { it.address }
    )
}
```

**优势**:

- 线程安全 (不可变)
- 易于测试 (纯函数)
- 易于调试 (状态追踪)
- 时间旅行调试 (保留历史状态)

#### 虚拟代理模式 (变量懒加载)

**实现**:

- LLDBVariable 初始不加载子项
- hasChildren 标志指示是否有子项
- 展开时调用 getChildren() 加载
- 缓存加载结果避免重复请求

**优势**:

- 减少网络请求
- 提升响应速度
- 节省内存

#### 适配器模式

**MemoryCell 适配器**:

- 统一的 MemoryCell 接口
- HexCell、InstructionCell 不同实现
- 渲染器统一处理

**LLDB 数据适配器**:

- LLDBVariable → CangJieValue
- LLDBFrame → CangJieStackFrame
- 隔离后端协议和 IDE UI

#### 建造者模式

**ProtobufFactory**:

```kotlin
fun buildRequest(block: Request.Builder.() -> Unit): Request {
    return Request.newBuilder()
        .setHash(generateHash())
        .apply(block)
        .build()
}
```

**优势**:

- 流式 API
- 类型安全
- 易于维护

### 性能优化策略

#### 内存视图优化

**按需加载**:

- 只加载可见区域的内存
- 滚动时动态加载新区域
- 释放远离可见区域的数据

**虚拟滚动**:

- 支持超大地址空间
- 不限制可滚动范围
- 动态计算滚动位置

**批量更新**:

- 合并多次状态更新
- 减少渲染次数
- 使用防抖避免频繁更新

#### 变量视图优化

**懒加载**:

- 变量子项按需加载
- 分页加载大数组
- 缓存加载结果

**表达式缓存**:

- 缓存求值结果
- 状态变化时失效
- 减少重复求值

#### 断点优化

**批量注册**:

- 启动时批量设置断点
- 减少网络往返次数

**地址缓存**:

- 缓存源码行到地址的映射
- 避免重复解析

**条件优化**:

- 按需编译条件表达式
- 缓存编译结果

#### 通信优化

**请求合并**:

- 合并相关请求
- 批量获取栈帧
- 批量获取变量

**响应缓存**:

- 缓存不变数据 (线程列表、栈帧)
- 状态变化时失效

**超时控制**:

- 每个请求设置超时
- 快速失败避免阻塞
- 支持请求取消