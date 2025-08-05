# 泛型枚举实现文档

## 概述

本文档描述了仓颉语言中泛型枚举的实现，特别是 `Option<T>` 这样的泛型枚举类型。泛型枚举允许枚举具有类型参数，提供类型安全的代数数据类型。

## 泛型枚举的特点

### 1. 类型参数支持

泛型枚举可以具有一个或多个类型参数：

```cangjie
enum Option<T> {
    Some(T),
    None
}

enum Result<T, E> {
    Success(T),
    Error(E)
}

enum Either<L, R> {
    Left(L),
    Right(R)
}
```

### 2. 类型安全

泛型枚举提供编译时类型检查：

```cangjie
let someInt: Option<Int> = Some(42)
let someString: Option<String> = Some("hello")
let none: Option<Int> = None

// 类型安全的模式匹配
match someInt {
    Some(value) => println(value),  // value 的类型是 Int
    None => println("no value")
}
```

### 3. 类型参数约束

泛型枚举可以具有类型参数约束：

```cangjie
enum Container<T: Clone> {
    Single(T),
    Multiple(Vec<T>)
}
```

## 实现架构

### 1. 枚举描述符 (EnumDescriptor)

`EnumDescriptor` 接口支持类型参数：

```kotlin
interface EnumDescriptor : ClassifierDescriptorWithTypeParameters {
    // 声明的类型参数
    override val declaredTypeParameters: List<TypeParameterDescriptor>
    
    // 获取带类型参数的成员作用域
    fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope
    
    // 获取带类型替换的成员作用域
    fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope
}
```

### 2. 枚举类型 (EnumType)

`EnumType` 类支持类型参数：

```kotlin
class EnumType(
    private val enumDescriptor: EnumDescriptor,
    typeArguments: List<TypeProjection> = emptyList(),
    override val isOption: Boolean = false,
    attributes: TypeAttributes = TypeAttributes.Empty
) : SimpleType()
```

### 3. 枚举类型构造函数 (EnumTypeConstructor)

`EnumTypeConstructor` 管理类型参数：

```kotlin
class EnumTypeConstructor(
    private val enumDescriptor: EnumDescriptor
) : TypeConstructor {
    // 类型参数声明
    override val parameters: List<TypeParameterDescriptor>
        get() = enumDescriptor.declaredTypeParameters
    
    // 创建带类型参数的枚举类型
    fun createType(arguments: List<TypeProjection>): EnumType {
        return EnumType(enumDescriptor, arguments)
    }
}
```

## 类型参数处理

### 1. 类型参数声明

```kotlin
// 创建类型参数 T
val typeParameterT = object : TypeParameterDescriptor {
    override val name: Name = Name.identifier("T")
    override val index: Int = 0
    override val upperBounds: List<CangJieType> = emptyList()
    override val variance: Variance = Variance.INVARIANT
    override val isReified: Boolean = false
    override val defaultType: CangJieType = ErrorType("T")
    // ... 其他实现
}
```

### 2. 泛型枚举创建

```kotlin
// 创建 Option<T> 枚举
val optionEnum = EnumDescriptorImpl(
    name = Name.identifier("Option"),
    declaredTypeParameters = listOf(typeParameterT),
    enumConstructors = listOf(someConstructor, noneConstructor),
    hasArguments = true,
    // ... 其他参数
)
```

### 3. 类型参数实例化

```kotlin
// 创建 Option<Int> 类型
val intType = ErrorType("Int")
val typeArgument = TypeProjectionImpl(intType)
val optionIntType = EnumType(
    enumDescriptor = optionEnum,
    typeArguments = listOf(typeArgument)
)
```

## 类型替换机制

### 1. 类型替换器

```kotlin
// 创建类型替换器：T -> Int
val substitutor = TypeSubstitutor.createByConstructorsMap(
    mapOf(typeParameterT to TypeProjectionImpl(intType))
)
```

### 2. 枚举描述符替换

```kotlin
// 执行类型替换
val substitutedEnum = optionEnum.substitute(substitutor)
```

### 3. 构造函数替换

```kotlin
// 枚举构造函数的类型替换
val substitutedConstructor = constructor.substitute(substitutor)
```

## Option 类型的特殊处理

### 1. Option 类型识别

```kotlin
override val isOptionType: Boolean
    get() = name.asString() == "Option"
```

### 2. Option 语法糖

Option 类型支持两种形式：

```cangjie
// 语法糖形式
let x: ?Int = 42

// 显式形式
let y: Option<Int> = Some(42)
```

### 3. Option 类型转换

```kotlin
// 在类型系统中统一处理
class OptionType(val innerType: CangJieType) : SimpleType() {
    override val isOption: Boolean = true
}
```

## 成员作用域管理

### 1. 未替换的成员作用域

```kotlin
override val unsubstitutedMemberScope: MemberScope = memberScope
```

### 2. 带类型参数的成员作用域

```kotlin
override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope {
    if (typeArguments.isEmpty()) {
        return unsubstitutedMemberScope
    }
    
    val typeSubstitution = TypeSubstitution.createByConstructorsMap(
        declaredTypeParameters.zip(typeArguments).toMap()
    )
    
    return getMemberScope(typeSubstitution)
}
```

### 3. 带类型替换的成员作用域

```kotlin
override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
    if (typeSubstitution.isEmpty) {
        return unsubstitutedMemberScope
    }
    
    // 创建替换后的成员作用域
    return memberScope
}
```

## 测试验证

### 1. 泛型枚举创建测试

```kotlin
@Test
fun `test generic enum descriptor creation`() {
    val optionEnum = createOptionEnum()
    
    assertEquals("Option", optionEnum.name.asString())
    assertTrue(optionEnum.hasArguments)
    assertEquals(EnumKind.OPTION, optionEnum.enumKind)
    assertTrue(optionEnum.isOptionType)
    assertEquals(1, optionEnum.declaredTypeParameters.size)
}
```

### 2. 类型参数实例化测试

```kotlin
@Test
fun `test enum type creation with type arguments`() {
    val optionIntType = createOptionIntType()
    
    assertEquals("Option", optionIntType.name.asString())
    assertEquals(EnumKind.OPTION, optionIntType.enumKind)
    assertTrue(optionIntType.isOptionType)
    assertEquals(1, optionIntType.arguments.size)
}
```

### 3. 类型替换测试

```kotlin
@Test
fun `test enum descriptor type substitution`() {
    val substitutedEnum = performTypeSubstitution()
    
    assertNotNull(substitutedEnum)
    assertTrue(substitutedEnum is EnumDescriptor)
    assertEquals("Option", substitutedEnum.name.asString())
}
```

## 使用示例

### 1. 创建泛型枚举

```kotlin
// 创建 Option<T> 枚举
val optionEnum = EnumDescriptorImpl(
    name = Name.identifier("Option"),
    declaredTypeParameters = listOf(typeParameterT),
    enumConstructors = listOf(someConstructor, noneConstructor),
    hasArguments = true,
    enumKind = EnumKind.OPTION
)
```

### 2. 创建具体类型

```kotlin
// 创建 Option<Int> 类型
val optionIntType = EnumType(
    enumDescriptor = optionEnum,
    typeArguments = listOf(TypeProjectionImpl(intType))
)
```

### 3. 类型替换

```kotlin
// 将 Option<T> 替换为 Option<Int>
val substitutor = TypeSubstitutor.createByConstructorsMap(
    mapOf(typeParameterT to TypeProjectionImpl(intType))
)
val substitutedEnum = optionEnum.substitute(substitutor)
```

## 总结

泛型枚举的实现为仓颉语言提供了强大的类型安全特性：

1. **类型参数支持**：枚举可以具有类型参数，如 `Option<T>`
2. **类型安全**：编译时类型检查确保类型安全
3. **类型替换**：支持类型参数的实例化和替换
4. **成员作用域管理**：正确处理泛型枚举的成员作用域
5. **特殊类型处理**：Option 类型的特殊处理和语法糖支持

这种实现为 IDE 插件提供了完整的泛型枚举支持，包括代码补全、类型检查和语义分析。 