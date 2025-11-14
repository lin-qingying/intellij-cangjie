# Proto Debugger 重构架构说明

## 概述

本次重构完全重新设计了 proto_debugger 模块的架构，将原本超过3000行的单一 `DebuggerDriver` 类拆分为清晰的分层架构，提升了可维护性、可测试性和并发一致性。

## 架构分层

### 1. 传输层 (Transport Layer)
位置: `org.cangnova.cangjie.protodebugger.transport`

**核心组件:**
- `Transport` - 传输层抽象接口，统一Socket和命名管道
- `SocketTransport` - 基于TCP Socket的传输实现，使用协程和结构化并发
- `MessageBus` - 消息总线，封装请求/响应路由和广播处理
- `RequestExecutor` - 请求执行器，统一处理超时、重试和错误

**职责:**
- 管理与调试器后端的网络连接
- 序列化/反序列化 Protobuf 消息
- 提供统一的消息收发接口
- 处理超时和连接错误

**特点:**
- 使用 Kotlin 协程实现异步通信
- 支持结构化并发和资源自动清理
- 统一的异常处理机制

### 2. 服务层 (Service Layer)
位置: `org.cangnova.cangjie.protodebugger.services`

按职责拆分为6个独立服务:

#### BreakpointService
**文件:** `services/BreakpointService.kt`, `services/impl/BreakpointServiceImpl.kt`

**功能:**
- 添加/移除行断点
- 添加/移除地址断点
- 添加/移除符号断点
- 添加/移除观察点（内存监视点）

#### SteppingService
**文件:** `services/SteppingService.kt`, `services/impl/SteppingServiceImpl.kt`

**功能:**
- 继续执行 (resume)
- 中断执行 (interrupt)
- 单步进入 (stepInto)
- 单步跨过 (stepOver)
- 单步跳出 (stepOut)
- 运行到指定位置 (runTo)
- 跳转到指定位置 (jumpTo)
- 线程冻结/解冻

#### EvalService
**文件:** `services/EvalService.kt`, `services/impl/EvalServiceImpl.kt`

**功能:**
- 表达式求值
- 获取变量列表
- 获取变量子元素
- 获取变量数据和描述
- 值过滤控制

#### MemoryService
**文件:** `services/MemoryService.kt`, `services/impl/MemoryServiceImpl.kt`

**功能:**
- 读取内存区域
- 写入内存
- 内存访问权限检查

#### DisasmService
**文件:** `services/DisasmService.kt`, `services/impl/DisasmServiceImpl.kt`

**功能:**
- 反汇编函数
- 获取寄存器值
- 获取架构信息
- 反汇编风格设置

#### SessionService
**文件:** `services/SessionService.kt`, `services/impl/SessionServiceImpl.kt`

**功能:**
- 进程启动/附加/分离/终止
- 核心转储加载
- 远程调试连接
- 线程和栈帧管理
- 路径映射
- 符号文件管理
- 解释器命令执行

### 3. 门面层 (Facade Layer)
位置: `org.cangnova.cangjie.protodebugger.core`

**核心组件:**
- `DebuggerDriverFacade` - 统一的调试器入口，组合所有服务

**职责:**
- 初始化和管理所有服务实例
- 管理调试器生命周期
- 提供统一的协程作用域
- 资源清理和错误处理

**使用示例:**
```kotlin
val facade = DebuggerDriverFacade(handler, configuration, architectureType)

// 使用各个服务
facade.breakpointService.addLineBreakpoint("main.cj", 10)
facade.steppingService.stepInto(thread)
facade.evalService.evaluate(thread, frame, "x + y")

// 关闭
facade.close()
```

### 4. 协议层 (Protocol Layer)
位置: `org.cangnova.cangjie.protodebugger.protocol`

**核心组件:**
- `ProtobufMessageFactory` - Protobuf 消息构建工厂（已存在，保持不变）

**职责:**
- 提供 DSL 风格的消息构建接口
- 封装 Protobuf 消息的复杂性

### 5. 领域模型层 (Domain Model Layer)
位置: `org.cangnova.cangjie.protodebugger.data`, `execution`, `settings` 等

**组件:**
- 数据类: `LLValue`, `LLThread`, `LLFrame`, `LLBreakpoint` 等
- 执行状态: `TargetState`, `ExecutionResult`, `ExitStatus`
- 配置: `DebuggerSettings`, `DisasmOptions` 等

**特点:**
- 纯数据类，无业务逻辑
- 不可变优先
- Kotlin 风格命名（去除 `my*` 前缀）

## 并发策略

### 协程和结构化并发
- 所有异步操作使用 Kotlin 协程
- 会话级 `CoroutineScope` 管理所有异步作业
- 自动资源清理和取消传播

### 超时和重试
- `RequestExecutor` 统一处理超时
- 可配置的重试策略
- 结构化的错误处理

### 消息路由
- 使用 `Channel` 和 `Flow` 进行消息传递
- 背压控制
- 广播事件分发

## 异常处理

### 异常层次
```
TransportException (传输层异常)
├── TransportTimeoutException (超时)
├── TransportConnectionException (连接失败)
└── TransportSerializationException (序列化失败)

DebuggerCommandException (命令执行异常)
DebuggerEvaluationTimedOutException (求值超时)
DriverException (驱动异常)
```

### 错误处理策略
- 传输层: 抛出 `TransportException`
- 服务层: 转换为领域异常 (`DebuggerCommandException` 等)
- 门面层: 统一错误日志和恢复

## 与原 DebuggerDriver 的对比

### 原架构问题
1. **超大类**: `DebuggerDriver.kt` 超过3000行，职责过载
2. **高耦合**: 协议、传输、业务逻辑混在一起
3. **并发混乱**: `QueueProcessor`、`Semaphore`、协程混用
4. **难以测试**: 无法单独测试各个功能模块
5. **Java 风格**: 大量 `my*` 前缀，可变状态

### 新架构优势
1. **清晰分层**: 每层职责明确，依赖单向
2. **高内聚低耦合**: 服务独立，通过接口通信
3. **统一并发**: 全部使用协程和结构化并发
4. **易于测试**: 每个服务可独立测试
5. **Kotlin 风格**: 不可变优先，协程，扩展函数

## 迁移指南

### 从旧 DebuggerDriver 迁移

**旧代码:**
```kotlin
val driver = DebuggerDriver(handler, config, arch)
driver.addBreakpoint("main.cj", 10)
driver.stepInto(thread, false, false)
val value = driver.evaluate(thread, frame, "x")
```

**新代码:**
```kotlin
val facade = DebuggerDriverFacade(handler, config, arch)
scope.launch {
    facade.breakpointService.addLineBreakpoint("main.cj", 10)
    facade.steppingService.stepInto(thread, false, false)
    val value = facade.evalService.evaluate(thread, frame, "x")
}
```

### 注意事项
1. 所有服务方法都是 `suspend` 函数，需要在协程中调用
2. 使用 `facade.getScope()` 获取协程作用域
3. 记得调用 `facade.close()` 释放资源

## 未来扩展

### 计划中的改进
1. **事件系统**: 统一的事件追踪和遥测
2. **缓存层**: 内存数据缓存和预加载
3. **插件系统**: 支持自定义调试器扩展
4. **性能监控**: 请求耗时统计和性能基线

### 扩展点
- 新增服务: 实现对应的 `Service` 接口
- 新增传输方式: 实现 `Transport` 接口
- 自定义消息处理: 扩展 `MessageBus`

## 测试策略

### 单元测试
- 每个服务独立测试
- Mock `MessageBus` 进行隔离测试
- 测试异常处理和边界条件

### 集成测试
- 测试服务间协作
- 测试完整的调试流程
- 测试并发场景

### 端到端测试
- 真实调试器后端
- 完整的调试会话
- 性能基准测试

## 参考

- 原重构文档: `proto_debugger/重构.txt`
- 原 DebuggerDriver: `core/DebuggerDriver.kt` (保留作为参考)
- Protobuf 协议: `proto/` 目录
