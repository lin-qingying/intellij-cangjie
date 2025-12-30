# 仓颉语义分析系统测试计划

## 目录

1. [概述](#概述)
2. [语义分析流程](#语义分析流程)
3. [测试框架架构](#测试框架架构)
4. [测试覆盖范围](#测试覆盖范围)
5. [测试用例设计](#测试用例设计)

---

## 概述

本文档描述了仓颉语言插件语义分析系统的完整测试计划。基于对现有代码和仓颉语言文档的分析，提供系统性的测试覆盖。

### 语义分析系统架构

```
IDE 文件请求
  ↓
CangJieCacheService (缓存服务)
  ↓
AnalyzerFacade (分析器门面)
  ↓
ResolverForProject (项目解析器)
  ↓
ResolverForModule (模块解析器)
  ↓
LazyTopDownAnalyzer (自顶向下分析器)
  ↓
BindingContext (绑定上下文) + ModuleDescriptor (模块描述符)
```

### 核心组件

- **AnalyzerFacade**: 分析器门面，连接 IDE 和分析引擎
- **ResolverForProject**: 管理所有模块的解析器
- **ResolverForModule**: 单个模块的解析器，包含 PackageFragmentProvider 和 ComponentProvider
- **LazyTopDownAnalyzer**: 执行实际的语义分析
- **BindingContext**: 存储 PSI 元素到描述符的映射
- **Descriptors**: 各种描述符（ClassDescriptor, FunctionDescriptor 等）

---

## 语义分析流程

### 完整分析流程

```
1. 文件加载
   ↓
2. PSI 解析 (词法+语法分析)
   ↓
3. 包声明分析
   ↓
4. 导入语句解析
   ↓
5. 顶层声明收集
   ↓
6. 类型解析
   ↓
7. 继承关系分析
   ↓
8. 成员解析
   ↓
9. 函数体分析
   ↓
10. 类型推导
   ↓
11. 符号引用解析
   ↓
12. BindingContext 构建
```

### 关键数据结构

#### BindingContext

存储分析结果的核心数据结构：

```kotlin
BindingContext {
    LEXICAL_SCOPE: CjElement -> LexicalScope
    TYPE: CjTypeReference -> CjType
    REFERENCE_TARGET: CjReferenceExpression -> DeclarationDescriptor
    FUNCTION: CjFunction -> FunctionDescriptor
    CLASS: CjClass -> ClassDescriptor
    VARIABLE: CjVariable -> VariableDescriptor
    // ... 更多映射
}
```

#### Descriptors

描述符层次结构：

```
DeclarationDescriptor (根接口)
  ├── ClassDescriptor (类/接口/枚举描述符)
  ├── FunctionDescriptor (函数描述符)
  ├── PropertyDescriptor (属性描述符)
  ├── VariableDescriptor (变量描述符)
  ├── ConstructorDescriptor (构造函数描述符)
  └── ModuleDescriptor (模块描述符)
```

---

## 测试框架架构

### 现有测试基类

```kotlin
CangJieAnalysisTestBase : CangJieTestBase() {
    // PSI 工厂
    protected lateinit var factory: CjPsiFactory

    // 创建测试文件
    protected fun createFile(content: String, fileName: String): CjFile

    // 执行分析并在上下文中进行断言
    protected fun <R> analyzeForTest(file: CjFile, action: AnalysisContext.() -> R): R

    // 分析上下文
    data class AnalysisContext(
        val bindingContext: BindingContext,
        val file: CjFile
    )
}
```

### 测试工具方法

```kotlin
// 查找 PSI 元素
fun findElement<T : CjElement>(file: CjFile): T? = PsiTreeUtil.findChildOfType(file, T::class.java)

// 断言类型
fun assertType(element: CjElement, expectedType: String)

// 断言引用目标
fun assertResolves(reference: CjReferenceExpression, targetName: String)

// 断言作用域包含符号
fun assertScopeContains(scope: LexicalScope, symbolName: String)
```

---

## 测试覆盖范围

### 1. 包声明和导入系统

#### 1.1 包声明测试

- [x] 简单包声明 (`package test`)
- [x] 嵌套包声明 (`package com.example.test`)
- [ ] 无包声明（默认包）
- [ ] 包声明作用域验证

#### 1.2 单项导入测试

- [x] 单个类型导入 (`import a.b.C`)
- [ ] 函数导入 (`import a.b.foo`)
- [ ] 属性导入 (`import a.b.bar`)
- [ ] 导入解析验证

#### 1.3 通配符导入测试

- [x] 包通配符导入 (`import a.b.*`)
- [ ] 子包导入验证
- [ ] 通配符导入优先级

#### 1.4 别名导入测试

- [x] 类型别名 (`import a.b.C as MyC`)
- [ ] 函数别名 (`import a.b.foo as myFoo`)
- [ ] 别名冲突检测

#### 1.5 同包多项导入测试

- [x] 花括号多项导入 (`import a.b.{C, D, E}`)
- [ ] 混合导入（类型+函数）
- [ ] 多项导入与别名

#### 1.6 跨包导入测试

- [x] 不同包多项导入 (`import {a.b.C, x.y.Z}`)
- [ ] 跨包导入优先级
- [ ] 跨包导入冲突

---

### 2. 类型系统测试

#### 2.1 基本类型测试

```kotlin
// 测试用例
func `test basic types`() {
    val file = createFile("""
        package test

        var i: Int64 = 0
        var f: Float64 = 0.0
        var b: Bool = true
        var s: String = ""
        var r: Rune = r'c'
    """)

    analyzeForTest(file) {
        // 验证每个变量的类型推导正确
    }
}
```

#### 2.2 复合类型测试

- [ ] 数组类型 (`Array<Int64>`)
- [ ] 元组类型 (`(Int64, String)`)
- [ ] 函数类型 (`(Int64) -> String`)
- [ ] Option 类型 (`Option<Int64>`)
- [ ] Range 类型 (`Range<Int64>`)

#### 2.3 用户定义类型测试

- [ ] Class 类型
- [ ] Interface 类型
- [ ] Struct 类型
- [ ] Enum 类型

---

### 3. 类声明和继承测试

#### 3.1 类基本声明测试

```kotlin
func `test simple class declaration`() {
    val file = createFile("""
        package test

        class Foo {
            public var bar: Int64 = 0
        }
    """)

    analyzeForTest(file) {
        val classDecl = findElement<CjClass>(file)
        assertNotNull(classDecl)

        val classDescriptor = bindingContext[BindingContext.CLASS, classDecl]
        assertNotNull(classDescriptor)
        assertEquals("Foo", classDescriptor.name.asString())
    }
}
```

#### 3.2 类继承测试

- [ ] 单继承 (`class B <: A`)
- [ ] 接口实现 (`class C <: I`)
- [ ] 多接口实现 (`class D <: I1 & I2`)
- [ ] 继承 + 接口 (`class E <: A & I`)

#### 3.3 Open/Sealed 修饰符测试

- [ ] Open 类继承
- [ ] Sealed 类限制
- [ ] 抽象类继承
- [ ] 修饰符冲突检测

#### 3.4 继承关系验证测试

```kotlin
func `test inheritance hierarchy`() {
    val file = createFile("""
        package test

        open class Base {}
        class Child <: Base {}

        interface I {}
        class Impl <: I {}
    """)

    analyzeForTest(file) {
        val childClass = findElement<CjClass>("Child")
        val childDescriptor = bindingContext[BindingContext.CLASS, childClass]

        // 验证父类关系
        val superClass = childDescriptor.getSuperClassDescriptor()
        assertEquals("Base", superClass.name.asString())

        // 验证接口实现
        val implClass = findElement<CjClass>("Impl")
        val implDescriptor = bindingContext[BindingContext.CLASS, implClass]
        assertTrue(implDescriptor.implementsInterface("I"))
    }
}
```

---

### 4. 接口声明和实现测试

#### 4.1 接口基本声明测试

```kotlin
func `test interface declaration`() {
    val file = createFile("""
        package test

        interface Flyable {
            func fly(): Unit
        }
    """)

    analyzeForTest(file) {
        val interfaceDecl = findElement<CjInterface>(file)
        val interfaceDescriptor = bindingContext[BindingContext.CLASS, interfaceDecl]

        assertNotNull(interfaceDescriptor)
        assertTrue(interfaceDescriptor.isInterface)
        assertEquals("Flyable", interfaceDescriptor.name.asString())
    }
}
```

#### 4.2 接口实现验证测试

- [ ] 接口成员实现完整性
- [ ] 默认实现继承
- [ ] 多接口实现冲突
- [ ] 静态成员实现

#### 4.3 接口继承测试

- [ ] 接口继承接口
- [ ] 多接口继承
- [ ] 接口层次结构验证

---

### 5. 泛型系统测试

#### 5.1 泛型类测试

```kotlin
func `test generic class`() {
    val file = createFile("""
        package test

        class Box<T> {
            public var value: T

            public init(value: T) {
                this.value = value
            }
        }

        func test() {
            var intBox = Box<Int64>(10)
            var strBox = Box<String>("hello")
        }
    """)

    analyzeForTest(file) {
        // 验证泛型类声明
        val boxClass = findElement<CjClass>("Box")
        val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass]
        assertTrue(boxDescriptor.declaredTypeParameters.size == 1)

        // 验证泛型实例化
        val intBox = findVariable("intBox")
        val intBoxType = bindingContext[BindingContext.TYPE, intBox.typeReference]
        assertEquals("Box<Int64>", intBoxType.toString())
    }
}
```

#### 5.2 泛型函数测试

- [ ] 泛型函数声明
- [ ] 泛型函数调用
- [ ] 类型参数推导
- [ ] 泛型约束验证

#### 5.3 泛型约束测试

```kotlin
func `test generic constraints`() {
    val file = createFile("""
        package test

        interface Comparable<T> {
            func compareTo(other: T): Int64
        }

        func max<T>(a: T, b: T): T where T <: Comparable<T> {
            if (a.compareTo(b) > 0) a else b
        }
    """)

    analyzeForTest(file) {
        val maxFunc = findElement<CjFunction>("max")
        val funcDescriptor = bindingContext[BindingContext.FUNCTION, maxFunc]

        // 验证泛型约束
        val typeParam = funcDescriptor.typeParameters[0]
        assertTrue(typeParam.upperBounds.any { it.toString().contains("Comparable") })
    }
}
```

#### 5.4 型变测试

- [ ] 协变 (`out T`)
- [ ] 逆变 (`in T`)
- [ ] 不变（默认）
- [ ] 型变规则验证

---

### 6. 函数声明和调用测试

#### 6.1 函数基本声明测试

```kotlin
func `test function declaration`() {
    val file = createFile("""
        package test

        func add(a: Int64, b: Int64): Int64 {
            return a + b
        }
    """)

    analyzeForTest(file) {
        val function = findElement<CjFunction>(file)
        val functionDescriptor = bindingContext[BindingContext.FUNCTION, function]

        assertNotNull(functionDescriptor)
        assertEquals("add", functionDescriptor.name.asString())
        assertEquals(2, functionDescriptor.valueParameters.size)
    }
}
```

#### 6.2 函数重载测试

- [ ] 参数个数重载
- [ ] 参数类型重载
- [ ] 重载决策验证

#### 6.3 Lambda 表达式测试

- [ ] Lambda 类型推导
- [ ] 闭包变量捕获
- [ ] Lambda 作为参数
- [ ] 尾随 Lambda

#### 6.4 高阶函数测试

- [ ] 函数作为参数
- [ ] 函数作为返回值
- [ ] 函数类型兼容性

---

### 7. 变量和属性测试

#### 7.1 变量声明测试

```kotlin
func `test variable declaration`() {
    val file = createFile("""
        package test

        func test() {
            var x: Int64 = 10
            let y = "hello"
        }
    """)

    analyzeForTest(file) {
        val xVar = findVariable("x")
        val yVar = findVariable("y")

        // 验证 x 的类型显式声明
        val xType = bindingContext[BindingContext.TYPE, xVar.typeReference]
        assertEquals("Int64", xType.toString())

        // 验证 y 的类型推导
        val yDescriptor = bindingContext[BindingContext.VARIABLE, yVar]
        assertEquals("String", yDescriptor.type.toString())
    }
}
```

#### 7.2 属性测试

- [ ] Getter/Setter 声明
- [ ] 属性访问
- [ ] Backing field
- [ ] 计算属性

#### 7.3 作用域测试

- [ ] 局部变量作用域
- [ ] 成员变量作用域
- [ ] 变量遮蔽
- [ ] 闭包作用域

---

### 8. 表达式和类型推导测试

#### 8.1 算术表达式测试

```kotlin
func `test arithmetic expressions`() {
    val file = createFile("""
        package test

        func test() {
            var a = 1 + 2
            var b = 3.0 * 4.0
            var c = 10 / 2
        }
    """)

    analyzeForTest(file) {
        val aVar = findVariable("a")
        val aType = inferType(aVar.initializer)
        assertEquals("Int64", aType.toString())

        val bVar = findVariable("b")
        val bType = inferType(bVar.initializer)
        assertEquals("Float64", bType.toString())
    }
}
```

#### 8.2 逻辑表达式测试

- [ ] 布尔运算 (`&&`, `||`, `!`)
- [ ] 比较运算 (`==`, `!=`, `<`, `>`)
- [ ] 类型推导验证

#### 8.3 调用表达式测试

- [ ] 函数调用
- [ ] 方法调用
- [ ] 构造函数调用
- [ ] 参数类型匹配

#### 8.4 限定表达式测试

```kotlin
func `test qualified expressions`() {
    val file = createFile("""
        package test

        class Foo {
            public var bar: Int64 = 0
        }

        func test() {
            var foo = Foo()
            var x = foo.bar
        }
    """)

    analyzeForTest(file) {
        val qualifiedExpr = findElement<CjDotQualifiedExpression>(file)
        val receiverExpr = qualifiedExpr.receiverExpression
        val selectorExpr = qualifiedExpr.selectorExpression

        // 验证 foo 解析到变量
        val receiverTarget = bindingContext[BindingContext.REFERENCE_TARGET, receiverExpr]
        assertNotNull(receiverTarget)

        // 验证 bar 解析到属性
        val selectorTarget = bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr]
        assertTrue(selectorTarget is PropertyDescriptor)
    }
}
```

---

### 9. 控制流测试

#### 9.1 条件表达式测试

```kotlin
func `test if expression`() {
    val file = createFile("""
        package test

        func test() {
            var x = if (true) 1 else 2
        }
    """)

    analyzeForTest(file) {
        val xVar = findVariable("x")
        val xType = inferType(xVar.initializer)
        // if 表达式的类型是两个分支的公共类型
        assertEquals("Int64", xType.toString())
    }
}
```

#### 9.2 循环表达式测试

- [ ] While 循环
- [ ] For-in 循环
- [ ] Do-while 循环
- [ ] Break/Continue

#### 9.3 Match 表达式测试

- [ ] Match 模式匹配
- [ ] 穷举性检查
- [ ] 类型细化

---

### 10. 错误处理测试

#### 10.1 异常声明测试

```kotlin
func `test exception hierarchy`() {
    val file = createFile("""
        package test

        class MyException <: Exception {
            public init(msg: String) {
                super(msg)
            }
        }
    """)

    analyzeForTest(file) {
        val exceptionClass = findElement<CjClass>("MyException")
        val classDescriptor = bindingContext[BindingContext.CLASS, exceptionClass]

        // 验证继承 Exception
        val superClass = classDescriptor.getSuperClassDescriptor()
        assertEquals("Exception", superClass.name.asString())
    }
}
```

#### 10.2 Throw/Try/Catch 测试

- [ ] Throw 语句
- [ ] Try-catch 块
- [ ] Finally 块
- [ ] 异常类型检查

---

### 11. 模式匹配测试

#### 11.1 枚举模式测试

```kotlin
func `test enum pattern matching`() {
    val file = createFile("""
        package test

        enum Option<T> {
            Some(T) | None
        }

        func unwrap<T>(opt: Option<T>): T {
            match (opt) {
                case Some(value) => value
                case None => throw Exception("unwrap None")
            }
        }
    """)

    analyzeForTest(file) {
        val matchExpr = findElement<CjMatchExpression>(file)

        // 验证模式穷举性
        // 验证每个分支的类型
    }
}
```

#### 11.2 元组模式测试

- [ ] 元组解构
- [ ] 嵌套模式
- [ ] 通配符模式

---

### 12. 符号解析测试

#### 12.1 简单引用解析测试

```kotlin
func `test simple reference resolution`() {
    val file = createFile("""
        package test

        var x = 10

        func test() {
            var y = x
        }
    """)

    analyzeForTest(file) {
        val yVar = findVariable("y")
        val xRef = yVar.initializer as CjReferenceExpression

        // 验证 x 引用解析到顶层变量
        val target = bindingContext[BindingContext.REFERENCE_TARGET, xRef]
        assertNotNull(target)
        assertTrue(target is VariableDescriptor)
        assertEquals("x", target.name.asString())
    }
}
```

#### 12.2 限定引用解析测试

- [ ] 成员访问 (`obj.member`)
- [ ] 包限定 (`std.core.String`)
- [ ] 嵌套类访问 (`Outer.Inner`)

#### 12.3 重载解析测试

- [ ] 函数重载解析
- [ ] 构造函数重载
- [ ] 操作符重载解析

---

### 13. 作用域测试

#### 13.1 词法作用域测试

```kotlin
func `test lexical scope`() {
    val file = createFile("""
        package test

        var global = 1

        func outer() {
            var outer = 2

            func inner() {
                var inner = 3
                var x = global + outer + inner
            }
        }
    """)

    analyzeForTest(file) {
        // 验证每个作用域包含正确的符号
        val innerFunc = findElement<CjFunction>("inner")
        val scope = bindingContext[BindingContext.LEXICAL_SCOPE, innerFunc]

        assertScopeContains(scope, "inner")
        assertScopeContains(scope, "outer")
        assertScopeContains(scope, "global")
    }
}
```

#### 13.2 类作用域测试

- [ ] 成员可见性
- [ ] This 绑定
- [ ] Super 绑定

---

### 14. 特殊语言特性测试

#### 14.1 扩展测试

```kotlin
func `test extension`() {
    val file = createFile("""
        package test

        extend Int64 <: ToString {
            public func toString(): String {
                // 实现
            }
        }
    """)

    analyzeForTest(file) {
        // 验证扩展声明
        // 验证接口实现
    }
}
```

#### 14.2 操作符重载测试

- [ ] 算术操作符
- [ ] 比较操作符
- [ ] 索引操作符
- [ ] 调用操作符

#### 14.3 属性委托测试

- [ ] Lazy 委托
- [ ] Observable 委托
- [ ] 自定义委托

---

## 测试用例设计原则

### 1. 独立性

每个测试用例应该独立，不依赖其他测试的状态。

### 2. 完整性

每个测试应该包含：
- **Given**: 测试前置条件和输入
- **When**: 触发的操作
- **Then**: 预期结果验证

### 3. 可读性

测试代码应该清晰表达测试意图：
```kotlin
fun `test that class inheritance creates correct hierarchy`() {
    // Given
    val file = createFile(/* ... */)

    // When
    analyzeForTest(file) {
        // Then
        val childDescriptor = bindingContext[...]
        assertEquals(expected, actual)
    }
}
```

### 4. 边界条件

每个功能应该测试：
- **正常情况**: 标准用例
- **边界情况**: 空输入、单元素等
- **错误情况**: 非法输入、类型错误等

---

## 实施建议

### Phase 1: 基础覆盖（1-2周）

1. 实现包声明和导入测试（已有基础）
2. 实现基本类型和类声明测试
3. 实现简单的符号解析测试

### Phase 2: 核心功能（2-3周）

1. 实现继承和接口测试
2. 实现泛型系统测试
3. 实现函数和表达式测试

### Phase 3: 高级特性（2-3周）

1. 实现模式匹配测试
2. 实现作用域和闭包测试
3. 实现扩展和操作符重载测试

### Phase 4: 完善和优化（1-2周）

1. 补充边界条件测试
2. 优化测试性能
3. 完善测试文档

---

## 总结

本测试计划基于：

1. **现有代码分析**:
   - `CangJieAnalysisTestBase` 测试框架
   - `QualifiedExpressionResolverFacadeTest` 示例
   - 语义分析组件架构

2. **仓颉语言文档**:
   - 类型系统
   - 类和接口
   - 泛型
   - 继承机制
   - 导入系统

3. **测试最佳实践**:
   - 独立性
   - 可读性
   - 完整性覆盖

该计划提供了全面的测试覆盖，从基础的包声明到复杂的泛型约束，确保语义分析系统的正确性和健壮性。
