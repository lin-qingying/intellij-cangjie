# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个为IntelliJ平台开发的仓颉语言插件,提供语法高亮、代码补全、格式化、调试支持和LSP集成等功能。

## 构建和开发命令

### 基本构建命令

```bash
./gradlew build              # 构建项目
./gradlew :plugin:buildPlugin  # 构建插件发布包
./gradlew :plugin:runIde     # 运行IDE实例进行调试
./gradlew test               # 运行所有测试
```

### 平台版本切换

项目支持多个IntelliJ平台版本(241, 242, 243, 251)。通过环境变量`platformVersion`切换:

```bash
# 在gradle.properties中设置platformVersion=243
# 或使用对应的gradle-243.properties等文件
./gradlew -PplatformVersion=243 build
```

### 发布插件

```bash
./gradlew :plugin:publishPlugin  # 需要设置PUBLISH_TOKEN环境变量
```

## 核心架构

### 模块依赖层次

```
plugin (入口)
  ├─ psi (语法树定义)
  ├─ descriptors (类型描述符系统)
  │   └─ deserialization (反序列化子模块)
  ├─ metadata (元数据格式定义)
  ├─ deserialization (二进制反序列化)
  ├─ analysis (分析模块)
  │   └─ decompiler-to-psi (反编译器)
  ├─ common (通用代码)
  ├─ util (工具类)
  ├─ icon (图标资源)
  ├─ messages (国际化)
  ├─ notifications (通知系统)
  ├─ toolchain (工具链集成)
  ├─ dap-debugger (DAP调试支持)
  └─ lsp4ij (LSP支持)
```

### deserialization模块

负责二进制的反序列化,已从proto迁移到flatbuffers:

- 不再需要nameResolver,直接使用`Decl.identifier`
- 使用`org.cangnova.cangjie.metadata.model`包中的flatbuffers类型
- 文件内不应包含proto字样,所有proto相关代码已重构为flatbuffers

#### DeserializedMemberScope

管理反序列化的作用域(
`deserialization/src/main/kotlin/org/cangnova/cangjie/serialization/deserialization/descriptors/DeserializedMemberScope.kt`):

- 已完成从proto到flatbuffers的重构
- 变量名已更新: functionProtos → functionDecls, variableProtos → variableDecls
- 注释中的"ProtoBuf"已更新为"flatbuffer"

### metadata模块

声明元数据格式,定义了flatbuffers的schema和模型类。

### analysis/decompiler-to-psi

将仓颉语言的编译后文件(如.cjo文件或元数据文件)反编译成PSI树结构,使IntelliJ能够理解和分析仓颉代码,提供IDE功能支持。

### descriptors模块

类型描述符体系,当前接口设计存在问题:

#### 类型层次

- **ClassifierDescriptor**: 所有可以成为类型的接口(class/struct/类型参数/类型别名),与cangjieType直接关联,不一定可继承或有作用域
- **ClassDescriptor**: struct/class/interface共用,有作用域信息,可继承其他类型
- **EnumDescriptor**: enum类型,与ClassDescriptor同级,因enum特殊性单独定义
- **ExtendDescriptor**: 扩展声明,不与cangjieType关联,但可继承、有作用域,拥有declaredTypeParameters
  ```cangjie
  extend Type <: 接口1, 接口2 {
      func 方法1(){}
  }
  ```

#### 已知问题

某些位置需要处理作用域(如
`descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/impl/ModuleAwareClassDescriptor.kt`)和重写(如
`descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/OverridingUtil.kt`),但接口设计混乱导致某些位置无法访问必要的属性或方法。

## 开发注意事项

### 代码迁移

在处理deserialization相关代码时:

1. 移除所有nameResolver的使用
2. 将proto相关命名改为对应的flatbuffer命名
3. 使用`org.cangnova.cangjie.metadata.model`包中的类型
4. 更新注释,移除proto字样

### 平台版本特定代码

部分代码根据IDE版本有不同实现,位于`src/main/{platformVersion}/`目录下。构建系统会自动包含对应版本的源码。

### 测试

测试资源位于`src/test/resources/`,`bin/`目录中的二进制文件会被复制到测试资源中。