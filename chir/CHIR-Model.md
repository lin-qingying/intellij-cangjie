# CHIR 模型规范

CHIR（Cangjie High-level Intermediate Representation）是仓颉编译器的高级中间表示。

---

## 1. Package（包）

包是编译单位，包含一个完整包的所有声明。

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 完整包名（如 "std.core"） |
| accessLevel | AccessLevel | 包访问级别 |
| globalVars | List\<GlobalVar\> | 全局变量列表 |
| globalFuncs | List\<Func\> | 全局函数列表 |
| packageInitFunc | Func? | 包初始化函数 |
| packageLiteralInitFunc | Func? | 全局字面量初始化函数 |
| structs | List\<StructDef\> | 结构体定义 |
| classes | List\<ClassDef\> | 类定义 |
| enums | List\<EnumDef\> | 枚举定义 |
| extends | List\<ExtendDef\> | 扩展定义 |
| importedVarAndFuncs | List\<ImportedValue\> | 导入的变量和函数 |
| importedStructs | List\<StructDef\> | 导入的结构体 |
| importedClasses | List\<ClassDef\> | 导入的类 |
| importedEnums | List\<EnumDef\> | 导入的枚举 |
| importedExtends | List\<ExtendDef\> | 导入的扩展 |

---

## 2. Value（值）

Value 是所有值的基类。

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 值的类型 |
| identifier | String | 值的标识符 |
| attributes | AttributeInfo | 属性信息 |
| users | List\<Expression\> | 使用该值的表达式 |
| annoInfo | AnnoInfo | 注解信息 |

### 2.1 Parameter（参数）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 参数类型 |
| identifier | String | 参数标识符 |
| ownerFunc | Func? | 所属函数 |
| ownerLambda | Lambda? | 所属 Lambda |

### 2.2 LocalVar（局部变量）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 变量类型 |
| identifier | String | 变量标识符 |
| expr | Expression | 初始化表达式 |
| isRetValue | Bool | 是否为返回值 |
| ownerBlockGroup | BlockGroup? | 所属块组 |

### 2.3 GlobalVar（全局变量）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 变量类型 |
| identifier | String | 变量标识符 |
| srcCodeIdentifier | String | 源代码中的变量名 |
| packageName | String | 所属包名 |
| declaredParent | CustomTypeDef? | 声明的父类型（静态成员变量） |
| initializer | LiteralValue? | 字面量初始化器 |
| initFunc | Func? | 初始化函数 |

---

## 3. Func（函数）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | FuncType | 函数类型 |
| identifier | String | 函数标识符 |
| srcCodeIdentifier | String | 源代码中的函数名 |
| packageName | String | 所属包名 |
| funcKind | FuncKind | 函数种类 |
| declaredParent | CustomTypeDef? | 声明的父类型 |
| genericTypeParams | List\<GenericType\> | 泛型类型参数 |
| body | BlockGroup | 函数体 |
| parameters | List\<Parameter\> | 参数列表 |
| retValue | LocalVar? | 返回值 |

### FuncKind（函数种类）

| 值 | 说明 |
|----|------|
| DEFAULT | 默认函数 |
| GETTER | Getter 方法 |
| SETTER | Setter 方法 |
| LAMBDA | Lambda 函数 |
| CLASS_CONSTRUCTOR | 类构造函数 |
| PRIMAL_CLASS_CONSTRUCTOR | 初级类构造函数 |
| STRUCT_CONSTRUCTOR | 结构体构造函数 |
| PRIMAL_STRUCT_CONSTRUCTOR | 初级结构体构造函数 |
| GLOBALVAR_INIT | 全局变量初始化函数 |
| FINALIZER | 析构函数 |
| MAIN_ENTRY | 主函数入口 |
| ANNOFACTORY_FUNC | 注解工厂函数 |
| MACRO_FUNC | 宏函数 |
| DEFAULT_PARAMETER_FUNC | 默认参数函数 |
| INSTANCEVAR_INIT | 实例变量初始化函数 |

---

## 4. Block（基本块）

| 字段 | 类型 | 说明 |
|------|------|------|
| identifier | String | 块标识符 |
| exprs | List\<Expression\> | 表达式列表 |
| predecessors | List\<Block\> | 前驱块列表 |
| exceptions | List\<ClassType\>? | 异常信息（Landing Pad） |
| parentGroup | BlockGroup | 所属块组 |

**说明：**
- 块中最后一个表达式必须是 Terminator
- exceptions 为 null 表示普通块
- exceptions 为空列表表示捕获所有异常
- exceptions 非空表示捕获特定异常

---

## 5. BlockGroup（块组）

| 字段 | 类型 | 说明 |
|------|------|------|
| identifier | String | 块组标识符 |
| blocks | List\<Block\> | 基本块列表 |
| entryBlock | Block | 入口块 |
| ownerFunc | Func? | 所属函数 |
| ownerExpression | Expression? | 所属表达式（If/Loop/Lambda） |

---

## 6. Type（类型）

### 6.1 基础类型

| 类型 | 说明 |
|------|------|
| Int8, Int16, Int32, Int64, IntNative | 有符号整数 |
| UInt8, UInt16, UInt32, UInt64, UIntNative | 无符号整数 |
| Float16, Float32, Float64 | 浮点数 |
| Rune | Unicode 字符 |
| Bool | 布尔值 |
| Unit | 单位类型 |
| Nothing | 永不返回类型 |
| Void | 空类型 |

### 6.2 复合类型

#### TupleType（元组）

| 字段 | 类型 | 说明 |
|------|------|------|
| elementTypes | List\<Type\> | 元素类型列表 |

#### FuncType（函数类型）

| 字段 | 类型 | 说明 |
|------|------|------|
| paramTypes | List\<Type\> | 参数类型列表 |
| returnType | Type | 返回类型 |
| hasVarArg | Bool | 是否有可变参数 |
| isCFunc | Bool | 是否为 C 函数 |

#### RawArrayType（原始数组）

| 字段 | 类型 | 说明 |
|------|------|------|
| elementType | Type | 元素类型 |
| dims | UInt | 维度数 |

#### VArrayType（值数组）

| 字段 | 类型 | 说明 |
|------|------|------|
| elementType | Type | 元素类型 |
| size | Int64 | 数组大小 |

### 6.3 引用类型

#### RefType（引用）

| 字段 | 类型 | 说明 |
|------|------|------|
| baseType | Type | 基础类型 |
| refDims | UInt8 | 引用深度 |

#### BoxType（装箱类型）

| 字段 | 类型 | 说明 |
|------|------|------|
| baseType | Type | 基础类型 |

### 6.4 其他类型

#### GenericType（泛型参数）

| 字段 | 类型 | 说明 |
|------|------|------|
| srcCodeIdentifier | String | 源代码标识符 |
| upperBounds | List\<Type\> | 上界约束 |

#### CPointer（C 指针）

| 字段 | 类型 | 说明 |
|------|------|------|
| elementType | Type | 元素类型 |

---

## 7. CustomTypeDef（自定义类型定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| srcCodeIdentifier | String | 源代码标识符 |
| identifier | String | 标识符 |
| packageName | String | 所属包名 |
| attributes | AttributeInfo | 属性信息 |
| genericTypeParams | List\<GenericType\> | 泛型参数 |
| type | CustomType | 类型对象 |
| methods | List\<FuncBase\> | 成员方法 |
| staticMemberVars | List\<GlobalVarBase\> | 静态成员变量 |
| instanceVars | List\<MemberVarInfo\> | 实例成员变量 |
| implementedInterfaceTys | List\<ClassType\> | 实现的接口 |
| superTypesInCurDef | List\<ClassType\> | 直接父类型 |

### 7.1 ClassDef（类定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| （继承 CustomTypeDef） | | |
| isClass | Bool | 是否为类（否则为接口） |
| isAbstract | Bool | 是否为抽象类 |
| isAnnotation | Bool | 是否为注解 |
| superClassTy | ClassType? | 父类类型 |
| abstractMethods | List\<AbstractMethodInfo\> | 抽象方法列表 |

### 7.2 StructDef（结构体定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| （继承 CustomTypeDef） | | |
| isC | Bool | 是否为 C 结构体 |

### 7.3 EnumDef（枚举定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| （继承 CustomTypeDef） | | |
| nonExhaustive | Bool | 是否为非穷举枚举 |
| ctors | List\<EnumCtorInfo\> | 枚举构造器列表 |

#### EnumCtorInfo

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 构造器名 |
| funcType | FuncType | 构造器函数类型 |

### 7.4 ExtendDef（扩展定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| （继承 CustomTypeDef） | | |
| extendedType | Type | 被扩展的类型 |

### 7.5 MemberVarInfo（成员变量信息）

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 成员名 |
| type | Type | 成员类型 |
| attributeInfo | AttributeInfo | 属性 |
| initializerFunc | FuncBase? | 初始化函数 |
| isReadOnly | Bool | 是否只读 |
| isConst | Bool | 是否常量 |

---

## 8. Expression（表达式）

### 8.1 一元表达式

| 表达式 | 操作数 | 说明 |
|--------|--------|------|
| Neg | operand | 取反 |
| Not | operand | 逻辑非 |
| BitNot | operand | 位反 |

### 8.2 二元表达式

| 表达式 | 操作数 | 说明 |
|--------|--------|------|
| Add | lhs, rhs | 加法 |
| Sub | lhs, rhs | 减法 |
| Mul | lhs, rhs | 乘法 |
| Div | lhs, rhs | 除法 |
| Mod | lhs, rhs | 取模 |
| Exp | lhs, rhs | 幂运算 |
| LShift | lhs, rhs | 左移 |
| RShift | lhs, rhs | 右移 |
| BitAnd | lhs, rhs | 位与 |
| BitOr | lhs, rhs | 位或 |
| BitXor | lhs, rhs | 位异或 |
| And | lhs, rhs | 逻辑与 |
| Or | lhs, rhs | 逻辑或 |
| LT | lhs, rhs | 小于 |
| LE | lhs, rhs | 小于等于 |
| GT | lhs, rhs | 大于 |
| GE | lhs, rhs | 大于等于 |
| Equal | lhs, rhs | 等于 |
| NotEqual | lhs, rhs | 不等于 |

### 8.3 Constant（常量）

| 字段 | 类型 | 说明 |
|------|------|------|
| value | LiteralValue | 字面量值 |

支持：Bool, Int, UInt, Float, Rune, String, Unit, Null

### 8.4 内存操作

#### Allocate（分配）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 要分配的类型 |

#### Load（加载）

| 字段 | 类型 | 说明 |
|------|------|------|
| location | Value | 被加载的引用 |

#### Store（存储）

| 字段 | 类型 | 说明 |
|------|------|------|
| value | Value | 要存储的值 |
| location | Value | 目标引用 |

#### GetElementRef（获取元素引用）

| 字段 | 类型 | 说明 |
|------|------|------|
| location | Value | 基引用 |
| path | List\<UInt64\> | 路径索引 |

#### GetElementByName（按名称获取元素）

| 字段 | 类型 | 说明 |
|------|------|------|
| location | Value | 基引用 |
| names | List\<String\> | 成员名称路径 |

#### StoreElementRef（存储到元素）

| 字段 | 类型 | 说明 |
|------|------|------|
| value | Value | 要存储的值 |
| location | Value | 基引用 |
| path | List\<UInt64\> | 路径索引 |

#### StoreElementByName（按名称存储到元素）

| 字段 | 类型 | 说明 |
|------|------|------|
| value | Value | 要存储的值 |
| location | Value | 基引用 |
| names | List\<String\> | 成员名称路径 |

### 8.5 函数调用

#### Apply（函数应用）

| 字段 | 类型 | 说明 |
|------|------|------|
| callee | Value | 被调用函数 |
| args | List\<Value\> | 参数列表 |
| instantiatedTypeArgs | List\<Type\> | 实例化泛型参数 |
| thisType | Type? | this 的类型 |
| isSuperCall | Bool | 是否为 super 调用 |

#### Invoke（虚方法调用）

| 字段 | 类型 | 说明 |
|------|------|------|
| object | Value | 调用对象 |
| methodName | String | 方法名 |
| methodType | FuncType | 方法类型 |
| args | List\<Value\> | 参数列表 |
| genericTypeParams | List\<GenericType\> | 泛型参数 |
| instantiatedTypeArgs | List\<Type\> | 实例化参数 |

#### InvokeStatic（静态方法调用）

| 字段 | 类型 | 说明 |
|------|------|------|
| rttiValue | Value | RTTI 值 |
| methodName | String | 方法名 |
| methodType | FuncType | 方法类型 |
| args | List\<Value\> | 参数列表 |

### 8.6 类型操作

#### TypeCast（类型转换）

| 字段 | 类型 | 说明 |
|------|------|------|
| sourceValue | Value | 源值 |
| sourceTy | Type | 源类型 |
| targetTy | Type | 目标类型 |

#### InstanceOf（实例检查）

| 字段 | 类型 | 说明 |
|------|------|------|
| object | Value | 要测试的对象 |
| type | Type | 测试的类型 |

#### Box（装箱）

| 字段 | 类型 | 说明 |
|------|------|------|
| sourceValue | Value | 源值 |
| sourceTy | Type | 源类型 |
| targetTy | Type | 目标类型 |

#### UnBox（拆箱）

| 字段 | 类型 | 说明 |
|------|------|------|
| sourceValue | Value | 源值 |
| sourceTy | Type | 源类型 |
| targetTy | Type | 目标类型 |

### 8.7 复合结构

#### Tuple（元组）

| 字段 | 类型 | 说明 |
|------|------|------|
| elementValues | List\<Value\> | 元素值列表 |
| elementTypes | List\<Type\> | 元素类型列表 |

#### Field（字段访问）

| 字段 | 类型 | 说明 |
|------|------|------|
| base | Value | 基值 |
| path | List\<UInt64\> | 字段路径 |

#### FieldByName（按名称访问字段）

| 字段 | 类型 | 说明 |
|------|------|------|
| base | Value | 基值 |
| names | List\<String\> | 字段名路径 |

### 8.8 控制流表达式

#### If（条件表达式）

| 字段 | 类型 | 说明 |
|------|------|------|
| condition | Value | 条件值 |
| trueBranch | BlockGroup | 真分支块组 |
| falseBranch | BlockGroup | 假分支块组 |

#### Loop（循环表达式）

| 字段 | 类型 | 说明 |
|------|------|------|
| loopBody | BlockGroup | 循环体块组 |

#### ForInRange（范围循环）

| 字段 | 类型 | 说明 |
|------|------|------|
| inductionVar | Value | 循环归纳变量 |
| loopCondVar | Value | 循环条件变量 |
| body | BlockGroup | 循环体 |
| latch | BlockGroup | 延迟块组 |
| cond | BlockGroup | 条件块组 |

#### ForInIter（迭代器循环）

| 字段 | 类型 | 说明 |
|------|------|------|
| inductionVar | Value | 循环归纳变量 |
| body | BlockGroup | 循环体 |

### 8.9 Lambda（Lambda 表达式）

| 字段 | 类型 | 说明 |
|------|------|------|
| identifier | String | 标识符 |
| srcCodeIdentifier | String | 源代码标识符 |
| funcType | FuncType | 函数类型 |
| isLocalFunc | Bool | 是否为局部函数 |
| genericTypeParams | List\<GenericType\> | 泛型参数 |
| body | BlockGroup | Lambda 体 |
| parameters | List\<Parameter\> | 参数列表 |
| retValue | LocalVar? | 返回值 |
| capturedVariables | List\<Value\> | 捕获的变量 |

### 8.10 数组操作

#### RawArrayAllocate（原始数组分配）

| 字段 | 类型 | 说明 |
|------|------|------|
| elementType | Type | 元素类型 |
| size | Value | 数组大小 |

#### RawArrayLiteralInit（字面量初始化）

| 字段 | 类型 | 说明 |
|------|------|------|
| rawArray | Value | 原始数组 |
| elements | List\<Value\> | 元素列表 |

#### VArray（值数组）

| 字段 | 类型 | 说明 |
|------|------|------|
| elements | List\<Value\> | 元素列表 |
| size | Int64 | 数组大小 |

### 8.11 其他表达式

#### GetRTTI（获取运行时类型信息）

| 字段 | 类型 | 说明 |
|------|------|------|
| operand | Value | 对象值 |

#### GetRTTIStatic（获取静态 RTTI）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 类型 |

#### Intrinsic（内置函数调用）

| 字段 | 类型 | 说明 |
|------|------|------|
| intrinsicKind | IntrinsicKind | 内置函数类型 |
| args | List\<Value\> | 参数列表 |
| instantiatedTypeArgs | List\<Type\> | 实例化泛型参数 |

#### Spawn（生成并发任务）

| 字段 | 类型 | 说明 |
|------|------|------|
| closure | Value | 闭包 |
| spawnArg | Value? | 生成参数 |

#### GetException（获取异常）

无额外字段，用于在 catch 块中获取异常对象。

---

## 9. Terminator（终止指令）

### 9.1 基本控制流

#### GoTo（无条件跳转）

| 字段 | 类型 | 说明 |
|------|------|------|
| destination | Block | 目标块 |

#### Branch（条件分支）

| 字段 | 类型 | 说明 |
|------|------|------|
| condition | Value | 条件值 |
| trueBlock | Block | 真分支目标 |
| falseBlock | Block | 假分支目标 |

#### MultiBranch（多路分支）

| 字段 | 类型 | 说明 |
|------|------|------|
| condition | Value | 条件值 |
| caseVals | List\<UInt64\> | 情况值列表 |
| successors | List\<Block\> | 对应目标块 |
| defaultBlock | Block | 默认目标块 |

#### Exit（函数退出）

无额外字段。

### 9.2 异常处理

#### RaiseException（抛出异常）

| 字段 | 类型 | 说明 |
|------|------|------|
| exceptionValue | Value | 异常对象 |
| exceptionBlock | Block? | 异常处理块 |

#### ApplyWithException（带异常的函数应用）

| 字段 | 类型 | 说明 |
|------|------|------|
| callee | Value | 被调用函数 |
| args | List\<Value\> | 参数列表 |
| successBlock | Block | 成功分支 |
| errorBlock | Block | 异常分支 |

#### InvokeWithException（带异常的虚方法调用）

| 字段 | 类型 | 说明 |
|------|------|------|
| object | Value | 对象 |
| methodName | String | 方法名 |
| args | List\<Value\> | 参数列表 |
| successBlock | Block | 成功分支 |
| errorBlock | Block | 异常分支 |

#### TypeCastWithException（带异常的类型转换）

| 字段 | 类型 | 说明 |
|------|------|------|
| sourceValue | Value | 源值 |
| sourceTy | Type | 源类型 |
| targetTy | Type | 目标类型 |
| successBlock | Block | 成功分支 |
| errorBlock | Block | 异常分支 |

#### AllocateWithException（带异常的分配）

| 字段 | 类型 | 说明 |
|------|------|------|
| type | Type | 类型 |
| successBlock | Block | 成功分支 |
| errorBlock | Block | 异常分支 |

---

## 10. 模型层次关系

```
Package
├── GlobalVar
├── Func
│   ├── Parameter
│   └── BlockGroup (body)
│       └── Block
│           ├── Expression
│           │   ├── LocalVar (result)
│           │   └── BlockGroup (If/Loop/Lambda)
│           └── Terminator
├── ClassDef
│   ├── MemberVarInfo
│   ├── Func (methods)
│   └── AbstractMethodInfo
├── StructDef
│   ├── MemberVarInfo
│   └── Func (methods)
├── EnumDef
│   ├── EnumCtorInfo
│   └── Func (methods)
└── ExtendDef
    └── Func (methods)
```