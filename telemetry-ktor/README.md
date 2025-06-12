# CangJie 遥测服务器

这个模块实现了CangJie插件的遥测数据收集服务器，基于Ktor框架构建。

## 功能

- 接收来自CangJie插件的遥测数据
- 存储遥测数据到SQLite数据库
- 提供数据分析接口
- 包含测试数据生成工具

## 运行服务器

```bash
./gradlew run
```

服务器默认在端口8880启动。

## 遥测测试数据生成

此模块包含一个测试数据生成器，可用于测试遥测系统或提供开发和演示数据。

### 使用Gradle任务生成测试数据

```bash
# 使用默认选项生成测试数据
./gradlew generateTestData

# 生成指定数量的测试数据点
./gradlew generateTestData -PappArgs="--count=10"

# 生成多样化的测试数据
./gradlew generateTestData -PappArgs="--diverse"

# 指定输出文件路径
./gradlew generateTestData -PappArgs="--output=custom_data.json"
```

### 使用脚本生成测试数据

#### Windows用户

```bash
# 查看帮助信息
generate-test-data.bat --help

# 生成默认测试数据
generate-test-data.bat

# 生成多样化测试数据
generate-test-data.bat --diverse
```

#### Linux/Mac用户

```bash
# 查看帮助信息
./generate-test-data.sh --help

# 生成默认测试数据
./generate-test-data.sh

# 生成多样化测试数据
./generate-test-data.sh --diverse
```

### 在代码中使用测试数据生成器

```kotlin
// 导入测试数据生成器
import cn.cangnova.testdata.TelemetryTestData

// 生成简单的测试数据（默认5个数据点）
val simplePayload = TelemetryTestData.createTestPayload()

// 生成指定数量的测试数据点
val customPayload = TelemetryTestData.createTestPayload(10)

// 生成多样化的测试数据
val diversePayload = TelemetryTestData.createDiverseTestPayload()
```

## API文档

API文档可通过以下URL访问：

```
http://localhost:8880/documentation
```

## 开发

### 依赖项

- Kotlin 2.1.10
- Ktor 3.1.3
- Exposed (SQL框架)
- SQLite JDBC