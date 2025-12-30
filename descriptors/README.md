# Descriptors 模块

## 一、模块概述

Descriptors 模块是仓颉语言 IntelliJ 插件的**语义模型核心**，负责将源代码的语法结构（PSI）转换为语义模型（Descriptors），为代码分析、类型检查、引用解析等上层功能提供基础支持。


```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            IDE 功能层                                        │
│            代码补全 │ 引用解析 │ 重构 │ 检查 │ 导航 │ 类型推断               │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ 查询语义信息
                                    │
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Descriptors 模块 (本模块)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Descriptors │  │   Types     │  │   Scopes    │  │   Deserialization   │ │
│  │  声明描述符  │  │  类型系统   │  │  作用域解析  │  │   元数据反序列化     │ │
│  │  (~170文件) │  │  (~70文件)  │  │  (~50文件)  │  │     (32文件)        │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ 构建 / 反序列化
┌───────────────────────────────────┴─────────────────────────────────────────┐
│              PSI 模块                              Metadata 模块             │
│            (语法树结构)                          (.cjo 元数据文件)            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心概念

### 2.1 什么是 Descriptor？

Descriptor（描述符）是程序元素的**语义抽象表示**，与 PSI（语法树节点）形成互补：

```
┌──────────────────────────────────────────────────────────────────┐
│                        源代码文件                                 │
│  class MyClass<T> {                                              │
│      func process(item: T): String { ... }                       │
│  }                                                               │
└──────────────────────────────────────────────────────────────────┘
                    │                           │
        ┌───────────┴───────────┐   ┌───────────┴───────────┐
        ▼                       ▼   ▼                       ▼
┌───────────────────┐      ┌───────────────────────────────────────┐
│       PSI         │      │            Descriptor                 │
├───────────────────┤      ├───────────────────────────────────────┤
│ • 语法结构        │      │ • 语义信息                             │
│ • 文本位置        │      │ • 类型关系                             │
│ • 空白和注释      │      │ • 继承层次                             │
│ • 仅源代码        │      │ • 源代码 + 二进制库                    │
├───────────────────┤      ├───────────────────────────────────────┤
│ 用途:             │      │ 用途:                                 │
│ 语法高亮、格式化   │      │ 类型检查、引用解析、代码补全           │
└───────────────────┘      └───────────────────────────────────────┘
```

### 2.2 核心特性对比

| 特性 | PSI | Descriptor |
|------|-----|------------|
| **关注点** | 语法结构、文本位置 | 语义信息、类型关系 |
| **生命周期** | 随编辑器变化 | 可缓存、可序列化 |
| **数据来源** | 仅源代码 | 源代码 + 二进制库 |
| **主要用途** | 语法高亮、格式化 | 类型检查、引用解析 |
| **可变性** | 可变 | 不可变（替换产生新实例） |

---

## 三、模块架构

### 3.1 目录结构总览

```
descriptors/
│
├── src/main/kotlin/org/cangnova/cangjie/
│   │
│   ├── builtins/                    # 内置库管理 (6 文件)
│   │   ├── CangJieBuiltIns.kt           # 内置库入口，提供基本类型访问
│   │   ├── BuiltInsLoader.kt            # 内置库加载器接口
│   │   ├── BuiltInsPackageFragment.kt   # 内置库包片段接口
│   │   ├── UnsignedType.kt              # 无符号类型处理
│   │   ├── basic/                       # 基本类型 (Int, Bool, String...)
│   │   │   └── BasicTypesPackageFragmentProvider.kt
│   │   └── builtinstype/                # 内置类型 (Array, Option...)
│   │       └── BuiltinsTypesPackageFragmentProvider.kt
│   │
│   ├── descriptors/                 # 核心描述符系统 (~170 文件)
│   │   ├── [核心接口]                   # DeclarationDescriptor, Named 等
│   │   ├── [分类器]                     # ClassDescriptor, TypeParameterDescriptor 等
│   │   ├── [可调用]                     # FunctionDescriptor, PropertyDescriptor 等
│   │   ├── [模块/包]                    # ModuleDescriptor, PackageFragmentDescriptor 等
│   │   ├── annotations/             # 注解系统 (9 文件)
│   │   ├── extend/                  # 扩展描述符 (2 文件)
│   │   ├── impl/                    # 具体实现 (~70 文件)
│   │   ├── macro/                   # 宏描述符 (2 文件)
│   │   └── synthetic/               # 合成描述符 (1 文件)
│   │
│   ├── resolve/                     # 符号解析系统 (~50 文件)
│   │   ├── [工具类]                     # DescriptorUtils, OverridingUtil 等
│   │   ├── scopes/                  # 作用域实现 (25 文件)
│   │   │   ├── [核心作用域]              # MemberScope, ResolutionScope 等
│   │   │   ├── [特殊作用域]              # BuiltInsMemberScope, FunctionClassScope 等
│   │   │   ├── receivers/           # 接收者 (8 文件)
│   │   │   └── extend/              # 扩展作用域 (1 文件)
│   │   ├── constants/               # 常量系统 (8 文件)
│   │   ├── call/inference/          # 类型推断 (1 文件)
│   │   └── extend/                  # 扩展管理 (2 文件)
│   │
│   ├── types/                       # 类型系统 (~70 文件)
│   │   ├── [类型核心]                   # CangJieType, TypeConstructor 等
│   │   ├── [类型操作]                   # TypeSubstitutor, TypeUtils 等
│   │   ├── checker/                 # 类型检查器 (13 文件)
│   │   ├── error/                   # 错误类型 (11 文件)
│   │   └── expressions/             # 表达式类型
│   │
│   ├── renderer/                    # 描述符渲染 (4 文件)
│   ├── incremental/                 # 增量编译支持
│   └── util/                        # 工具函数
│
└── deserialization/                 # 反序列化子模块 (32 文件)
    └── src/main/kotlin/.../deserialization/
        ├── [反序列化器]                 # ClassDeserializer, TypeDeserializer 等
        ├── [数据查找]                   # ClassDataFinder, FullIdFinder 等
        ├── builtins/                # 内置库反序列化 (2 文件)
        └── descriptors/             # 反序列化描述符 (9 文件)
```

### 3.2 描述符继承体系

```mermaid
classDiagram
    direction TB

    class DeclarationDescriptor {
        <<interface>>
        +name: Name
        +containingDeclaration: DeclarationDescriptor?
        +original: DeclarationDescriptor
        +accept(visitor, data): R
    }

    class Named {
        <<interface>>
        +name: Name
    }

    class Annotated {
        <<interface>>
        +annotations: Annotations
    }

    DeclarationDescriptor --|> Named
    DeclarationDescriptor --|> Annotated

    class ModuleDescriptor {
        <<interface>>
        +projectDescriptor: ProjectDescriptor
        +builtIns: CangJieBuiltIns
        +getPackage(fqName): PackageViewDescriptor
        +getSubPackagesOf(fqName): Collection~FqName~
    }

    class PackageFragmentDescriptor {
        <<interface>>
        +fqName: FqName
        +getMemberScope(): MemberScope
    }

    class PackageViewDescriptor {
        <<interface>>
        +fqName: FqName
        +module: ModuleDescriptor
        +memberScope: MemberScope
    }

    class ClassifierDescriptor {
        <<interface>>
        +typeConstructor: TypeConstructor
        +defaultType: SimpleType
    }

    class ClassDescriptor {
        <<interface>>
        +kind: ClassKind
        +modality: Modality
        +visibility: Visibility
        +constructors: Collection
        +unsubstitutedMemberScope: MemberScope
        +superTypes: Collection~CangJieType~
    }

    class TypeParameterDescriptor {
        <<interface>>
        +variance: Variance
        +upperBounds: List~CangJieType~
        +index: Int
    }

    class TypeAliasDescriptor {
        <<interface>>
        +underlyingType: SimpleType
        +expandedType: SimpleType
    }

    class CallableDescriptor {
        <<interface>>
        +returnType: CangJieType?
        +typeParameters: List
        +valueParameters: List
         
    }

    class FunctionDescriptor {
        <<interface>>
        +isSuspend: Boolean
        +isOperator: Boolean
        +isInfix: Boolean
        +isInline: Boolean
    }

    class PropertyDescriptor {
        <<interface>>
        +isVar: Boolean
        +isConst: Boolean
        +getter: PropertyGetterDescriptor?
        +setter: PropertySetterDescriptor?
    }

    class ConstructorDescriptor {
        <<interface>>
        +constructedClass: ClassDescriptor
        +isPrimary: Boolean
    }

    DeclarationDescriptor <|-- ModuleDescriptor
    DeclarationDescriptor <|-- PackageFragmentDescriptor
    DeclarationDescriptor <|-- PackageViewDescriptor
    DeclarationDescriptor <|-- ClassifierDescriptor
    DeclarationDescriptor <|-- CallableDescriptor

    ClassifierDescriptor <|-- ClassDescriptor
    ClassifierDescriptor <|-- TypeParameterDescriptor
    ClassifierDescriptor <|-- TypeAliasDescriptor

    CallableDescriptor <|-- FunctionDescriptor
    CallableDescriptor <|-- PropertyDescriptor
    FunctionDescriptor <|-- ConstructorDescriptor
```

### 3.3 类型系统架构

```mermaid
graph TB
    subgraph "类型表示层"
        CangJieType["CangJieType<br/>(sealed class)"]
        SimpleType["SimpleType<br/>简单类型"]
        FlexibleType["FlexibleType<br/>弹性类型 [下界..上界]"]
        UnwrappedType["UnwrappedType<br/>解包类型"]
    end

    subgraph "类型构造层"
        TypeConstructor["TypeConstructor<br/>类型构造器"]
        ClassTypeConstructor["ClassTypeConstructorImpl<br/>类类型"]
        PrimitiveTypeConstructor["PrimitiveTypeConstructor<br/>基本类型"]
        FunctionTypeConstructor["FunctionTypeConstructor<br/>函数类型"]
        TupleTypeConstructor["TupleTypeConstructor<br/>元组类型"]
    end

    subgraph "类型操作层"
        TypeSubstitutor["TypeSubstitutor<br/>类型替换器"]
        TypeProjection["TypeProjection<br/>类型投影"]
        Variance["Variance<br/>方差 (in/out/*)"]
    end

    subgraph "类型检查层"
        TypeChecker["CangJieTypeChecker<br/>类型检查器"]
        TypeCheckingProcedure["TypeCheckingProcedure<br/>检查流程"]
        SubtypeCheck["子类型检查"]
        EqualityCheck["相等性检查"]
    end

    CangJieType --> SimpleType
    CangJieType --> FlexibleType
    CangJieType --> UnwrappedType

    SimpleType --> TypeConstructor
    TypeConstructor --> ClassTypeConstructor
    TypeConstructor --> PrimitiveTypeConstructor
    TypeConstructor --> FunctionTypeConstructor
    TypeConstructor --> TupleTypeConstructor

    TypeChecker --> TypeCheckingProcedure
    TypeCheckingProcedure --> SubtypeCheck
    TypeCheckingProcedure --> EqualityCheck
    TypeCheckingProcedure --> TypeSubstitutor
```

### 3.4 作用域解析架构

```mermaid
flowchart TB
    subgraph "解析入口"
        Query["符号查询<br/>Name: 'myFunction'"]
    end

    subgraph "作用域链 (按优先级)"
        LocalScope["LocalScope<br/>局部作用域<br/>(函数内变量)"]
        MemberScope["MemberScope<br/>成员作用域<br/>(类成员)"]
        ExtendScope["ExtendMemberScope<br/>扩展作用域<br/>(扩展方法)"]
        PackageScope["PackageScope<br/>包作用域<br/>(同包声明)"]
        ImportScope["ImportScope<br/>导入作用域<br/>(显式导入)"]
        BuiltInsScope["BuiltInsMemberScope<br/>内置作用域<br/>(标准库)"]
    end

    subgraph "特殊作用域"
        StaticScope["StaticMemberScope<br/>静态成员"]
        InstanceScope["InstanceMemberScope<br/>实例成员"]
        InnerScope["InnerClassesScopeWrapper<br/>内部类"]
        ChainedScope["ChainedMemberScope<br/>链式聚合"]
    end

    subgraph "解析结果"
        Found["找到: Collection&lt;DeclarationDescriptor&gt;"]
        NotFound["未找到: 空集合"]
    end

    Query --> LocalScope
    LocalScope -->|未找到| MemberScope
    MemberScope -->|未找到| ExtendScope
    ExtendScope -->|未找到| PackageScope
    PackageScope -->|未找到| ImportScope
    ImportScope -->|未找到| BuiltInsScope

    LocalScope -->|找到| Found
    MemberScope -->|找到| Found
    ExtendScope -->|找到| Found
    PackageScope -->|找到| Found
    ImportScope -->|找到| Found
    BuiltInsScope -->|找到| Found
    BuiltInsScope -->|未找到| NotFound

    MemberScope -.-> StaticScope
    MemberScope -.-> InstanceScope
    MemberScope -.-> InnerScope
    ChainedScope -.-> MemberScope
```

---

## 四、核心组件详解

### 4.1 Descriptors 包 - 声明描述符

#### 4.1.1 核心接口层

| 接口 | 职责 | 关键属性/方法 |
|------|------|---------------|
| `DeclarationDescriptor` | 所有描述符的根接口 | `name`, `containingDeclaration`, `original` |
| `Named` | 具有名称的元素 | `name: Name` |
| `Annotated` | 可添加注解的元素 | `annotations: Annotations` |
| `ValidateableDescriptor` | 可验证的描述符 | `validate()` |

#### 4.1.2 分类器描述符

```
ClassifierDescriptor (分类器基接口)
    │
    ├── ClassDescriptor (类/接口/结构体)
    │   ├── kind: ClassKind (CLASS, INTERFACE, ENUM, STRUCT...)
    │   ├── modality: Modality (FINAL, OPEN, ABSTRACT, SEALED)
    │   ├── constructors: Collection<ClassConstructorDescriptor>
    │   ├── unsubstitutedMemberScope: MemberScope
    │   └── superTypes: Collection<CangJieType>
    │
    ├── TypeParameterDescriptor (类型参数 <T>)
    │   ├── variance: Variance (IN, OUT, INVARIANT)
    │   ├── upperBounds: List<CangJieType>
    │   └── index: Int
    │
    └── TypeAliasDescriptor (类型别名 typealias)
        ├── underlyingType: SimpleType
        └── expandedType: SimpleType
```

#### 4.1.3 可调用描述符

```
CallableDescriptor (可调用基接口)
    │
    ├── FunctionDescriptor (函数)
    │   ├── valueParameters: List<ValueParameterDescriptor>
    │   ├── returnType: CangJieType?
    │   ├── typeParameters: List<TypeParameterDescriptor>
    │   ├── isSuspend / isOperator / isInfix / isInline
    │   │
    │   └── ConstructorDescriptor (构造函数)
    │       ├── constructedClass: ClassDescriptor
    │       └── isPrimary: Boolean
    │
    └── PropertyDescriptor (属性)
        ├── isVar: Boolean
        ├── isConst: Boolean
        ├── getter: PropertyGetterDescriptor?
        └── setter: PropertySetterDescriptor?
```

#### 4.1.4 模块与包描述符

```
项目/模块层次结构:

ProjectDescriptor (项目)
    │
    └── ModuleDescriptor (模块)
            │
            ├── PackageViewDescriptor (包视图 - 聚合视图)
            │   └── memberScope: MemberScope
            │
            └── PackageFragmentDescriptor (包片段 - 单个来源)
                └── getMemberScope(): MemberScope
```

#### 4.1.5 实现类分布 (impl 目录)

| 类别 | 主要实现类 | 数量 |
|------|-----------|------|
| **类相关** | `ClassDescriptorImpl`, `EnumDescriptorImpl`, `PrimitiveClassDescriptor`, `TupleClassDescriptor`, `FunctionClassDescriptor` | ~15 |
| **函数相关** | `SimpleFunctionDescriptorImpl`, `FunctionDescriptorImpl`, `AnonymousFunctionDescriptor`, `ClassConstructorDescriptorImpl` | ~8 |
| **属性相关** | `PropertyDescriptorImpl`, `PropertyGetterDescriptorImpl`, `PropertySetterDescriptorImpl`, `LocalVariableDescriptor` | ~10 |
| **参数相关** | `ValueParameterDescriptorImpl`, `ReceiverParameterDescriptorImpl`, `TypeParameterDescriptorImpl` | ~8 |
| **模块/包相关** | `ModuleDescriptorImpl`, `PackageFragmentDescriptorImpl`, `LazyPackageViewDescriptorImpl` | ~10 |
| **其他** | `DeclarationDescriptorImpl`, `TypeAliasConstructorDescriptorImpl` 等 | ~20 |

### 4.2 Types 包 - 类型系统

#### 4.2.1 类型层次

```
CangJieType (sealed class - 所有类型的基类)
    │
    ├── SimpleType (简单类型)
    │   ├── constructor: TypeConstructor     # 类型构造器
    │   ├── arguments: List<TypeProjection>  # 类型参数
    │   ├── isMarkedNullable: Boolean       # 可空标记
    │   └── memberScope: MemberScope         # 成员作用域
    │
    ├── FlexibleType (弹性类型)
    │   ├── lowerBound: SimpleType          # 下界
    │   └── upperBound: SimpleType          # 上界
    │
    └── UnwrappedType (解包类型)
```

#### 4.2.2 类型构造器体系

```
TypeConstructor (类型构造器接口)
    │
    ├── ClassTypeConstructorImpl         # 普通类类型
    ├── PrimitiveTypeConstructor         # 基本类型 (Int, String...)
    ├── EnumTypeConstructor              # 枚举类型
    ├── FunctionTypeConstructor          # 函数类型 (P1, P2) -> R
    ├── TupleTypeConstructor             # 元组类型 (T1, T2, ...)
    ├── BuiltInsTypeConstructor          # 内置类型
    ├── IntersectionTypeConstructor      # 交集类型 A & B
    └── ErrorTypeConstructor             # 错误类型
```

#### 4.2.3 类型检查器

```mermaid
sequenceDiagram
    participant Client as 调用方
    participant Checker as CangJieTypeChecker
    participant Procedure as TypeCheckingProcedure
    participant Substitutor as TypeSubstitutor

    Client->>Checker: isSubtypeOf(type1, type2)
    Checker->>Procedure: 执行检查流程

    alt 需要类型替换
        Procedure->>Substitutor: substitute(type)
        Substitutor-->>Procedure: 替换后的类型
    end

    Procedure->>Procedure: 比较类型构造器
    Procedure->>Procedure: 递归检查类型参数
    Procedure-->>Checker: 检查结果
    Checker-->>Client: true/false
```

#### 4.2.4 错误类型系统 (60+ 种错误类型)

| 错误类别 | 示例 | 说明 |
|----------|------|------|
| **未解析类型** | `UNRESOLVED_TYPE`, `UNRESOLVED_CLASS_TYPE` | 无法解析的类型引用 |
| **返回类型错误** | `RETURN_TYPE_FOR_FUNCTION`, `IMPLICIT_RETURN_TYPE` | 函数返回类型问题 |
| **循环类型** | `RECURSIVE_TYPE`, `CYCLIC_SUPERTYPES` | 类型定义循环 |
| **类型推断** | `UNINFERRED_TYPE_VARIABLE`, `UNINFERRED_LAMBDA_PARAMETER` | 无法推断的类型 |
| **反序列化** | `CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER` | 元数据加载失败 |

### 4.3 Resolve 包 - 符号解析

#### 4.3.1 作用域类型

| 作用域 | 用途 | 实现类 |
|--------|------|--------|
| **成员作用域** | 类/包的成员查找 | `MemberScopeImpl` |
| **静态作用域** | 静态成员查找 | `StaticMemberScope` |
| **实例作用域** | 实例成员查找 | `InstanceMemberScope` |
| **内置作用域** | 内置类型成员 | `BuiltInsMemberScope` |
| **基本类型作用域** | Int/String 等的成员 | `PrimitiveMemberScope` |
| **函数类型作用域** | 函数类型的 invoke | `FunctionClassScope` |
| **元组作用域** | 元组的成员访问 | `TupleClassScope` |
| **扩展作用域** | 扩展方法 | `ExtendMemberScope` |
| **链式作用域** | 多作用域聚合 | `ChainedMemberScope` |
| **替换作用域** | 类型参数替换后 | `SubstitutingScope` |
| **错误作用域** | 错误处理 | `ErrorScope` |

#### 4.3.2 接收者类型

```
ReceiverValue (接收者值接口)
    │
    ├── ImplicitReceiver           # 隐式接收者 (this)
    │   ├── ImplicitClassReceiver  # 类的隐式接收者
    │   └── ContextClassReceiver   # 上下文接收者
    │
    ├── ExtensionReceiver          # 扩展接收者
    │
    └── TransientReceiver          # 临时接收者
```

#### 4.3.3 常量系统

```
CompileTimeConstant (编译时常量)
    │
    ├── IntValue, LongValue, ShortValue, ByteValue
    ├── UIntValue, ULongValue, UShortValue, UByteValue
    ├── FloatValue, DoubleValue
    ├── BoolValue
    ├── StringValue, RuneValue
    ├── NullValue
    └── ArrayValue, EnumValue
```

### 4.4 Deserialization 子模块 - 元数据反序列化

#### 4.4.1 反序列化流程

```mermaid
flowchart TB
    subgraph "输入"
        CJO[".cjo 文件<br/>(FlatBuffers 格式)"]
    end

    subgraph "组件容器"
        Components["DeserializationComponents<br/>────────────────<br/>• StorageManager<br/>• ModuleDescriptor<br/>• ClassDeserializer<br/>• TypeDeserializer<br/>• PackageFragmentProvider"]
    end

    subgraph "数据查找"
        ClassFinder["ClassDataFinder<br/>查找类元数据"]
        ExtendFinder["ExtendDataFinder<br/>查找扩展元数据"]
        FullIdFinder["FullIdFinder<br/>完整ID查找"]
    end

    subgraph "反序列化器"
        ClassDeser["ClassDeserializer<br/>类反序列化"]
        EnumDeser["EnumDeserializer<br/>枚举反序列化"]
        TypeDeser["TypeDeserializer<br/>类型反序列化"]
        DeclDeser["DeclarationDeserializer<br/>声明反序列化"]
    end

    subgraph "输出描述符"
        DeserClass["DeserializedClassDescriptor"]
        DeserEnum["DeserializedEnumDescriptor"]
        DeserExtend["DeserializedExtendDescriptor"]
        DeserMember["DeserializedMemberDescriptor"]
    end

    CJO --> Components
    Components --> ClassFinder
    Components --> ExtendFinder
    Components --> FullIdFinder

    ClassFinder --> ClassDeser
    ClassFinder --> EnumDeser
    ClassDeser --> TypeDeser
    EnumDeser --> TypeDeser

    ClassDeser --> DeserClass
    EnumDeser --> DeserEnum
    DeclDeser --> DeserExtend
    DeclDeser --> DeserMember
```

#### 4.4.2 核心组件

| 组件 | 职责 |
|------|------|
| `DeserializationComponents` | 反序列化系统的全局配置和组件容器 |
| `DeserializationContext` | 反序列化过程中的局部上下文 |
| `ClassDeserializer` | 类的反序列化 |
| `EnumDeserializer` | 枚举的反序列化 |
| `TypeDeserializer` | 类型的反序列化 |
| `DeclarationDeserializer` | 通用声明的反序列化 |
| `BuiltInSerializerFlatbuffers` | 内置库文件定位和加载 |

---

## 五、设计模式与原则

### 5.1 分离语法与语义

```
┌─────────────────┐
│   源代码文本     │
└────────┬────────┘
         │ 词法/语法分析
         ▼
┌─────────────────┐
│      PSI        │  ◄── 语法层：结构和位置
└────────┬────────┘
         │ 语义分析
         ▼
┌─────────────────┐
│   Descriptors   │  ◄── 语义层：类型和含义
└─────────────────┘
```

**优势**：
- PSI 变化时，Descriptor 可增量更新
- 二进制库无需源代码，直接反序列化为 Descriptor
- 语义分析独立于具体语法

### 5.2 延迟计算 (Lazy Evaluation)

```mermaid
stateDiagram-v2
    [*] --> 未初始化
    未初始化 --> 计算中: 首次访问
    计算中 --> 已缓存: 计算完成
    已缓存 --> 已缓存: 再次访问(直接返回)
```

- 属性（类型、成员列表等）仅在首次访问时计算
- 使用 `StorageManager` 管理缓存
- 避免不必要的计算开销
- 支持递归类型的处理

### 5.3 不可变性 (Immutability)

```
┌───────────────────────────────────────┐
│          原始 Descriptor              │
│   class Box<T> { ... }               │
└───────────────────────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     ▼             ▼             ▼
┌─────────┐  ┌─────────┐  ┌─────────┐
│ Box<Int>│  │Box<Str> │  │Box<Bool>│
└─────────┘  └─────────┘  └─────────┘
   (类型参数替换产生新的描述符实例)
```

- Descriptor 创建后不可修改
- 类型参数替换产生新实例
- 保证线程安全
- 便于缓存和共享

### 5.4 访问者模式 (Visitor Pattern)

```kotlin
interface DeclarationDescriptorVisitor<R, D> {
    fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: D): R
    fun visitClassDescriptor(descriptor: ClassDescriptor, data: D): R
    fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: D): R
    fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: D): R
    fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: D): R
    fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: D): R
    // ... 更多访问方法
}
```

### 5.5 统一的二进制/源码模型

```mermaid
flowchart LR
    subgraph "来源1: 源代码"
        Source[".cj 源文件"]
        PSI["PSI 树"]
        SourceDesc["源码 Descriptor"]
    end

    subgraph "来源2: 二进制库"
        Binary[".cjo 文件"]
        Deser["反序列化"]
        BinaryDesc["二进制 Descriptor"]
    end

    subgraph "统一接口"
        API["Descriptor API<br/>────────────<br/>• ClassDescriptor<br/>• FunctionDescriptor<br/>• TypeDescriptor"]
    end

    Source --> PSI --> SourceDesc --> API
    Binary --> Deser --> BinaryDesc --> API
```

---

## 六、关键工作流

### 6.1 类型检查流程

```mermaid
sequenceDiagram
    participant Source as 源代码
    participant PSI as PSI树
    participant Resolver as 解析器
    participant Desc as Descriptor
    participant TypeSys as 类型系统
    participant Checker as 类型检查器
    participant Reporter as 错误报告

    Source->>PSI: 解析
    PSI->>Resolver: 解析引用
    Resolver->>Desc: 创建/查找描述符
    Desc->>TypeSys: 获取类型信息
    TypeSys->>Checker: 类型检查

    alt 类型错误
        Checker->>Reporter: 报告错误
    else 类型正确
        Checker-->>Source: 验证通过
    end
```

### 6.2 符号解析流程

```mermaid
sequenceDiagram
    participant Code as 代码位置
    participant Scope as 作用域链
    participant Local as LocalScope
    participant Member as MemberScope
    participant Package as PackageScope
    participant BuiltIn as BuiltInsScope

    Code->>Scope: 查找符号 "foo"
    Scope->>Local: 查找局部变量

    alt 找到
        Local-->>Code: 返回描述符
    else 未找到
        Local->>Member: 继续查找

        alt 找到
            Member-->>Code: 返回描述符
        else 未找到
            Member->>Package: 继续查找

            alt 找到
                Package-->>Code: 返回描述符
            else 未找到
                Package->>BuiltIn: 继续查找
                BuiltIn-->>Code: 返回描述符或空
            end
        end
    end
```

### 6.3 反序列化流程

```mermaid
sequenceDiagram
    participant Client as 调用方
    participant Loader as BuiltInsLoader
    participant Finder as ClassDataFinder
    participant Deser as ClassDeserializer
    participant TypeDeser as TypeDeserializer
    participant Cache as StorageManager

    Client->>Loader: 加载类 "std.collection.ArrayList"
    Loader->>Cache: 检查缓存

    alt 缓存命中
        Cache-->>Client: 返回缓存的描述符
    else 缓存未命中
        Loader->>Finder: 查找类元数据
        Finder-->>Loader: 返回 FlatBuffer 数据
        Loader->>Deser: 反序列化类
        Deser->>TypeDeser: 反序列化类型
        TypeDeser-->>Deser: 返回类型
        Deser-->>Loader: 返回类描述符
        Loader->>Cache: 缓存描述符
        Cache-->>Client: 返回描述符
    end
```

---

## 七、内置类型支持

### 7.1 基本类型

| 类型 | 描述符类 | 说明 |
|------|----------|------|
| `Int8`, `Int16`, `Int32`, `Int64` | `PrimitiveClassDescriptor` | 有符号整数 |
| `UInt8`, `UInt16`, `UInt32`, `UInt64` | `PrimitiveClassDescriptor` | 无符号整数 |
| `Float16`, `Float32`, `Float64` | `PrimitiveClassDescriptor` | 浮点数 |
| `Bool` | `PrimitiveClassDescriptor` | 布尔类型 |
| `Rune` | `PrimitiveClassDescriptor` | Unicode 字符 |
| `String` | `PrimitiveClassDescriptor` | 字符串 |
| `Unit` | `PrimitiveClassDescriptor` | 单元类型 |
| `Nothing` | `PrimitiveClassDescriptor` | 底类型 |

### 7.2 特殊类型

| 类型 | 描述 |
|------|------|
| `Any` | 所有类型的父类型 |
| `Nothing` | 所有类型的子类型（底类型） |
| `Option<T>` | 可空类型，等价于 `T?` |
| `Array<T>` | 数组类型 |
| `Function<P..., R>` | 函数类型 |
| `Tuple<T...>` | 元组类型 |

### 7.3 标准库包结构

```
std
├── core          # 核心类型
├── collection    # 集合类型
│   └── concurrent
├── io            # 输入输出
├── fs            # 文件系统
├── net           # 网络
├── sync          # 并发同步
├── time          # 时间日期
├── math          # 数学函数
├── regex         # 正则表达式
├── reflect       # 反射
├── unittest      # 单元测试
└── ...           # 更多模块
```

---

## 八、与其他模块的关系

```mermaid
graph TB
    subgraph "依赖本模块的模块"
        Analysis["analysis<br/>语义分析"]
        PSIModule["psi<br/>PSI 扩展"]
        Completion["IDE 补全"]
        References["引用解析"]
        Inspections["代码检查"]
        Refactoring["重构"]
    end

    subgraph "本模块"
        Descriptors["descriptors"]
        Deserialization["deserialization"]
    end

    subgraph "本模块依赖的模块"
        Common["common<br/>名称、FqName"]
        Metadata["metadata<br/>FlatBuffers 格式"]
        Util["util<br/>工具函数"]
        Toolchain["toolchain<br/>SDK 管理"]
    end

    Analysis --> Descriptors
    PSIModule --> Descriptors
    Completion --> Descriptors
    References --> Descriptors
    Inspections --> Descriptors
    Refactoring --> Descriptors

    Descriptors --> Common
    Descriptors --> Metadata
    Descriptors --> Util
    Deserialization --> Toolchain
    Deserialization --> Descriptors
```

---

## 九、扩展指南

### 9.1 添加新的描述符类型

1. 在 `descriptors/` 下定义接口，继承 `DeclarationDescriptor`
2. 在 `descriptors/impl/` 下创建实现类
3. 在 `DeclarationDescriptorVisitor` 中添加访问方法
4. 如需反序列化支持，在 `deserialization/` 中添加反序列化器

### 9.2 添加新的内置类型

1. 在 `common` 模块的 `StandardNames` 中定义名称常量
2. 在 `CangJieBuiltIns` 中添加访问方法
3. 在 `BuiltinsTypesPackageFragmentProvider` 中注册

### 9.3 添加新的作用域类型

1. 实现 `MemberScope` 或继承 `MemberScopeImpl`
2. 实现 `getContributedFunctions()` 和 `getContributedVariables()`
3. 在适当的描述符中使用新作用域

---

## 十、文件统计

| 目录 | 文件数 | 说明 |
|------|--------|------|
| `builtins/` | 6 | 内置库管理 |
| `descriptors/` | ~170 | 核心描述符系统 |
| ├── `annotations/` | 9 | 注解系统 |
| ├── `extend/` | 2 | 扩展系统 |
| ├── `impl/` | ~70 | 具体实现 |
| ├── `macro/` | 2 | 宏系统 |
| └── `synthetic/` | 1 | 合成元素 |
| `resolve/` | ~50 | 符号解析 |
| ├── `scopes/` | 25 | 作用域系统 |
| ├── `constants/` | 8 | 常量系统 |
| └── 其他 | 17 | 工具函数 |
| `types/` | ~70 | 类型系统 |
| ├── `checker/` | 13 | 类型检查 |
| ├── `error/` | 11 | 错误类型 |
| └── 其他 | ~46 | 类型操作 |
| `renderer/` | 4 | 描述符渲染 |
| `deserialization/` | 32 | 反序列化 |
| **总计** | **~292** | |