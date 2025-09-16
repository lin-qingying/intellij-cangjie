# 枚举描述符实现文档

## 概述

本文档描述了为IntelliJ IDEA仓颉语言插件实现的枚举描述符系统。该实现基于编译器的EnumDecl和EnumTy，为IDE提供了完整的枚举类型支持。

## 实现对比

### 编译器实现 vs IDE插件实现

| 特性 | 编译器 (C++) | IDE插件 (Kotlin) |
|------|-------------|------------------|
| **语言** | C++ | Kotlin |
| **目标** | 编译时类型检查 | IDE智能提示 |
| **性能** | 优化执行速度 | 优化响应速度 |
| **内存管理** | 智能指针 + RAII | JVM垃圾回收 |
| **类型系统** | 结构体导向 | 接口导向 |

### 核心差异

#### 编译器 (EnumDecl)
```cpp
struct EnumDecl : InheritableDecl {
    std::vector<OwnedPtr<Decl>> constructors;  // 构造函数
    std::vector<OwnedPtr<Decl>> members;       // 成员函数
    bool hasArguments{false};                   // 是否有关联值
    bool hasEllipsis{false};                    // 是否非穷尽性
    Position ellipsisPos;                       // 省略号位置
};
```

#### IDE插件 (EnumDescriptor)
```kotlin
interface EnumDescriptor : ClassifierDescriptorWithTypeParameters {
    val constructors: Collection<EnumConstructorDescriptor>
    val members: Collection<FunctionDescriptor>
    val hasArguments: Boolean
    val isNonExhaustive: Boolean
    val ellipsisPosition: Position?
}
```

## 实现组件

### 1. 枚举描述符接口

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/descriptors/EnumDescriptor.kt`

#### 核心功能
- **枚举构造函数管理**: 支持简单构造函数和函数构造函数
- **枚举成员管理**: 管理枚举中的函数成员
- **类型参数支持**: 支持泛型枚举
- **非穷尽性支持**: 支持带`...`的非穷尽性枚举
- **Option类型支持**: 特殊处理Option<T>语法糖

#### 示例用法

```cangjie
// 简单枚举
enum Color {
    Red,
    Green,
    Blue
}

// 带关联值的枚举
enum Result<T> {
    Success(T),
    Error(String)
}

// 非穷尽性枚举
enum NonExhaustive {
    Case1,
    Case2,
    ...  // 省略号
}
```

### 2. 枚举类型系统

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/types/EnumType.kt`

#### 类型层次
```
CangJieType
├── SimpleType
│   ├── EnumType          // 枚举类型
│   └── RefEnumType       // 引用枚举类型
└── OptionType
    └── EnumType          // Option枚举类型
```

#### 核心特性
- **类型构造函数**: `EnumTypeConstructor`
- **成员作用域**: 委托给枚举描述符
- **类型参数**: 支持泛型枚举
- **Option支持**: 支持Option<T>语法糖
- **引用类型**: 支持&Enum引用语义

### 3. 枚举构造函数描述符

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/descriptors/EnumDescriptor.kt`

#### 构造函数类型
- **简单构造函数**: 无关联值，如`Red`
- **函数构造函数**: 有关联值，如`Success(T)`

#### 示例
```kotlin
// 简单构造函数
val redConstructor = EnumConstructorDescriptor(
    name = "Red",
    hasArguments = false
)

// 函数构造函数
val successConstructor = EnumConstructorDescriptor(
    name = "Success",
    hasArguments = true,
    argumentTypes = listOf(genericTypeParameter)
)
```

### 4. 类型精化器支持

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/types/checker/CangJieTypeRefiner.kt`

#### 新增方法
```kotlin
abstract fun refineEnumType(enumType: EnumType): EnumType
```

#### 默认实现
```kotlin
override fun refineEnumType(enumType: EnumType): EnumType {
    return enumType
}
```

### 5. 访问者模式支持

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/descriptors/DeclarationDescriptorVisitor.kt`

#### 新增访问方法
```kotlin
fun visitEnumDescriptor(descriptor: EnumDescriptor, builder: D?): R
fun visitEnumConstructorDescriptor(descriptor: EnumConstructorDescriptor, builder: D?): R
```

## 实现细节

### 1. 枚举描述符实现

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/descriptors/impl/EnumDescriptorImpl.kt`

#### 核心属性
- `name`: 枚举名称
- `constructors`: 枚举构造函数列表
- `members`: 枚举成员函数列表
- `hasArguments`: 是否有关联值
- `isNonExhaustive`: 是否为非穷尽性枚举
- `enumKind`: 枚举类型（ENUM/NON_EXHAUSTIVE/OPTION）

#### 核心方法
- `getMemberScope()`: 获取成员作用域
- `getAllConstructors()`: 获取所有构造函数
- `getAllMembers()`: 获取所有成员
- `accept()`: 访问者模式支持

### 2. 枚举类型实现

**文件**: `descriptors/src/main/kotlin/cn/cangnova/cangjie/types/EnumType.kt`

#### 核心特性
- **类型构造函数**: `EnumTypeConstructor`
- **成员作用域**: 委托给枚举描述符
- **类型参数**: 支持泛型枚举
- **Option支持**: 支持Option<T>语法糖
- **引用类型**: 支持&Enum引用语义

#### 类型层次
```kotlin
class EnumType : SimpleType {
    val descriptor: EnumDescriptor
    val name: String
    val enumKind: EnumKind
    val hasArguments: Boolean
    val isNonExhaustive: Boolean
    val constructors: Collection<EnumConstructorDescriptor>
    val members: Collection<FunctionDescriptor>
}

class RefEnumType : EnumType {
    // 引用枚举类型，isOption = true
}
```

### 3. 测试覆盖

**文件**: `descriptors/src/test/kotlin/cn/cangnova/cangjie/descriptors/EnumDescriptorTest.kt`

#### 测试场景
- 简单枚举描述符创建
- 带构造函数的枚举
- 带关联值的枚举
- 非穷尽性枚举
- Option类型枚举
- 枚举构造函数描述符
- 字符串表示测试

## 与编译器的对应关系

### 数据结构映射

| 编译器 | IDE插件 | 说明 |
|--------|---------|------|
| `EnumDecl` | `EnumDescriptor` | 枚举声明 |
| `EnumTy` | `EnumType` | 枚举类型 |
| `RefEnumTy` | `RefEnumType` | 引用枚举类型 |
| `constructors` | `constructors` | 枚举构造函数 |
| `members` | `members` | 枚举成员函数 |
| `hasArguments` | `hasArguments` | 是否有关联值 |
| `hasEllipsis` | `isNonExhaustive` | 是否非穷尽性 |

### 功能映射

| 编译器功能 | IDE插件功能 | 实现方式 |
|-----------|------------|----------|
| 类型检查 | 类型推断 | 类型精化器 |
| 符号解析 | 符号查找 | 成员作用域 |
| 错误诊断 | 错误提示 | 类型验证 |
| 代码生成 | 代码补全 | 描述符访问 |

## 使用示例

### 1. 创建枚举描述符

```kotlin
val colorEnum = EnumDescriptorImpl(
    name = "Color",
    containingDeclaration = packageDescriptor,
    enumConstructors = listOf(redConstructor, greenConstructor, blueConstructor),
    enumMembers = emptyList(),
    hasArguments = false,
    isNonExhaustive = false
)
```

### 2. 创建枚举类型

```kotlin
val colorType = EnumType(
    enumDescriptor = colorEnum,
    typeArguments = emptyList(),
    isOption = false
)
```

### 3. 创建引用枚举类型

```kotlin
val colorRefType = RefEnumType(
    enumDescriptor = colorEnum,
    typeArguments = emptyList()
)
```

### 4. 类型精化

```kotlin
val refinedType = colorType.refine(typeRefiner)
```

## 扩展性

### 1. 新增枚举特性

可以通过扩展`EnumDescriptor`接口来支持新的枚举特性：

```kotlin
interface EnumDescriptor {
    // 现有特性...
    
    // 新增特性
    val customFeature: CustomFeature?
}
```

### 2. 新增类型支持

可以通过扩展类型系统来支持新的枚举类型：

```kotlin
class CustomEnumType : EnumType {
    // 自定义实现
}
```

### 3. 新增访问者方法

可以通过扩展访问者接口来支持新的访问模式：

```kotlin
interface DeclarationDescriptorVisitor<R, D> {
    // 现有方法...
    
    // 新增方法
    fun visitCustomEnumDescriptor(descriptor: CustomEnumDescriptor, data: D?): R
}
```

## 总结

通过实现完整的枚举描述符系统，IntelliJ插件现在能够：

1. **完整支持枚举语法**: 支持所有仓颉语言的枚举特性
2. **智能代码补全**: 基于枚举描述符提供准确的代码补全
3. **类型安全检查**: 基于枚举类型系统进行类型检查
4. **错误诊断**: 提供准确的错误提示和诊断
5. **重构支持**: 支持枚举相关的重构操作

该实现与编译器保持高度一致，同时针对IDE场景进行了优化，为开发者提供了优秀的开发体验。 