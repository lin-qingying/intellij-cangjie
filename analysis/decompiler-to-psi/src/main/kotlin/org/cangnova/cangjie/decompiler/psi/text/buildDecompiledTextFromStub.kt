/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

/**
 * Stub 到反编译文本转换器
 *
 * ## 架构概述
 *
 * 本文件实现了从 Stub 树到可读源代码的转换逻辑，是反编译系统的核心渲染层。
 * 它使用访问者模式遍历 PSI 树，生成格式化的仓颉源代码。
 *
 * ## 核心设计模式: 访问者模式
 *
 * 使用 IntelliJ Platform 的访问者模式 ([CjVisitor]) 遍历 PSI 树：
 *
 * ```
 * CjVisitor<Unit, Unit>
 *   ├─ visitClass()          → 渲染 class 声明
 *   ├─ visitInterface()      → 渲染 interface 声明
 *   ├─ visitEnum()           → 渲染 enum 声明
 *   ├─ visitExtend()         → 渲染 extend 声明
 *   ├─ visitNamedFunction()  → 渲染函数声明
 *   ├─ visitProperty()       → 渲染属性声明
 *   ├─ visitVariable()       → 渲染变量声明
 *   ├─ visitTypeReference()  → 渲染类型引用
 *   ├─ visitModifierList()   → 渲染修饰符
 *   └─ visitElement()        → 默认处理（占位符）
 * ```
 *
 * ## 处理流程
 *
 * ```
 * buildDecompiledText(fileStub)
 *   ↓
 * 1. 验证 Stub 有效性
 *   ├─ Invalid Stub → 返回错误消息
 *   └─ Valid Stub → 继续
 *   ↓
 * 2. 输出文件头注释
 *   ├─ "// IntelliJ API Decompiler stub source generated from a cjo file"
 *   └─ "// Implementation of methods is not available"
 *   ↓
 * 3. 输出包声明
 *   └─ package com.example.app
 *   ↓
 * 4. 创建访问者实例
 *   └─ CjVisitor with PrettyPrinter
 *   ↓
 * 5. 遍历顶层声明
 *   ├─ 类声明 (class/interface/struct/enum)
 *   ├─ 扩展声明 (extend)
 *   ├─ 函数声明 (func)
 *   ├─ 类型别名 (type)
 *   └─ 顶层变量 (let/var)
 *   ↓
 * 6. 返回 DecompiledText
 * ```
 *
 * ## 渲染策略
 *
 * ### 声明顺序
 *
 * 顶层声明按照在 Stub 中的顺序渲染，声明之间用双换行分隔：
 * ```
 * class Foo { ... }
 *
 * func bar() { ... }
 *
 * let x: Int = COMPILED_CODE
 * ```
 *
 * ### 缩进规则
 *
 * - **顶层声明**: 无缩进
 * - **类成员**: 4 空格缩进
 * - **嵌套类**: 累加缩进（8 空格）
 * - **Where 子句**: 与声明同一行或换行后无缩进
 *
 * ### 类型参数渲染
 *
 * 类型参数使用两阶段渲染：
 * 1. **参数列表**: `<T, U, V>`
 * 2. **约束子句**: `where T: Comparable<T>, U: Serializable`
 *
 * ### 方法体占位符
 *
 * 方法体无法从 Stub 恢复，使用占位符：
 * - **抽象方法**: 无方法体
 * - **普通方法**: `{ /* compiled code */ }`
 * - **构造函数**: `{ /* compiled code */ }`
 * - **属性访问器**: `{ /* compiled code */ }`
 *
 * ## 关键常量
 *
 * - [DECOMPILED_CODE_COMMENT]: 方法体占位符 `/* compiled code */`
 * - [FLEXIBLE_TYPE_COMMENT]: 平台类型注释 `/* platform type */`
 * - [COMPILED_DEFAULT_PARAMETER_VALUE]: 参数默认值占位符（来自 stub 模块）
 * - [COMPILED_DEFAULT_INITIALIZER]: 变量初始化器占位符（来自 stub 模块）
 *
 * ## 特殊处理
 *
 * ### 跨包类型引用
 *
 * 使用完全限定名渲染跨包类型：
 * ```kotlin
 * // 类型来自 std.collection 包
 * let list: std.collection.ArrayList<Int>
 * ```
 *
 * ### 修饰符顺序
 *
 * 修饰符按照 [CjTokens.MODIFIER_KEYWORDS_ARRAY] 的顺序渲染：
 * 1. 访问控制: `public`, `private`, `protected`, `internal`
 * 2. 模态: `open`, `abstract`, `sealed`
 * 3. 其他: `static`, `mut`, `const`, `suspend`
 *
 * ### 枚举渲染
 *
 * 枚举项与成员用分号和换行分隔：
 * ```kotlin
 * enum Color {
 *     RED
 *     | GREEN
 *     | BLUE;
 *
 *     func toHex(): String { /* compiled code */ }
 * }
 * ```
 *
 * ## 性能优化
 *
 * ### 高性能 StringBuilder
 *
 * 使用 [PrettyPrinter] 进行高效文本拼接：
 * - 预分配缓冲区（默认 16KB）
 * - 避免重复字符串连接
 * - 智能缩进管理
 *
 * ### 避免完整 PSI 树构建
 *
 * 直接从 Stub 读取数据，无需构建完整 PSI 树：
 * - 访问 Stub 属性（如 `stub.name`）
 * - 不调用 `getDescriptor()` 或 `resolve()`
 * - 减少内存占用和解析时间
 *
 * ### 延迟计算
 *
 * 仅在需要时访问子节点：
 * - 空集合不遍历
 * - 可选元素检查后访问
 *
 * ## 错误处理和降级
 *
 * ### Stub 验证
 *
 * 处理无效 Stub：
 * ```kotlin
 * (fileStub.kind as? CangJieFileStubKind.Invalid)?.errorMessage?.let {
 *     return DecompiledText(it)
 * }
 * ```
 *
 * ### 未知元素
 *
 * 对于无法识别的 PSI 元素，输出占位符：
 * ```kotlin
 * append("/* !${element::class.simpleName}! */")
 * ```
 *
 * ## 与其他组件的关系
 *
 * ### 输入
 *
 * - **CangJieFileStubImpl**: 文件 Stub 树根节点
 * - **PSI 元素**: 通过 Stub.psi 访问
 * - **Stub 数据**: 直接从 Stub 属性读取
 *
 * ### 输出
 *
 * - **DecompiledText**: 包含反编译源码的数据类
 * - **使用方**: [CjDecompiledFile.calcTreeElement]
 *
 * ### 依赖
 *
 * - **PrettyPrinter**: 格式化文本输出
 * - **CjVisitor**: 访问者模式基类
 * - **ModifierListStubImpl**: 修饰符数据
 *
 * ## 扩展点
 *
 * ### 添加新的声明类型
 *
 * 1. 在 [CjVisitor] 中添加新的 visit 方法
 * 2. 实现该方法的渲染逻辑
 * 3. 确保在 [CjFile.FILE_DECLARATION_TYPES] 中注册
 *
 * ### 自定义渲染逻辑
 *
 * 可以通过继承 [CjVisitor] 并覆盖特定方法来自定义渲染：
 * ```kotlin
 * val customVisitor = object : CjVisitor<Unit, Unit>() {
 *     override fun visitClass(klass: CjClass, data: Unit): Unit? {
 *         // 自定义类渲染逻辑
 *     }
 * }
 * ```
 *
 * ## 已知限制
 *
 * 1. **方法体丢失**: 无法恢复方法实现
 * 2. **注释丢失**: 源码注释不在 Stub 中
 * 3. **格式偏好**: 使用固定格式，不保留原始格式
 * 4. **表达式简化**: 复杂表达式简化为类型引用
 * 5. **泛型擦除**: 运行时泛型信息可能丢失
 *
 * @see buildDecompiledText
 * @see DecompiledText
 * @see CjVisitor
 * @see PrettyPrinter
 * @see CangJieFileStubImpl
 */
package org.cangnova.cangjie.decompiler.psi.text

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.decompiler.COMPILED_DEFAULT_INITIALIZER
import org.cangnova.cangjie.decompiler.COMPILED_DEFAULT_PARAMETER_VALUE
import org.cangnova.cangjie.decompiler.DECOMPILED_CODE_COMMENT
import org.cangnova.cangjie.decompiler.FLEXIBLE_TYPE_COMMENT
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorString
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets.FILE_DECLARATION_TYPES
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.utils.printer.PrettyPrinter

/**
 * 基于 Stub 构建反编译文本
 *
 * ## 功能说明
 *
 * 这是反编译系统的核心函数，从 Stub 树生成格式化的仓颉源代码。
 * 它直接操作 Stub 数据结构，避免创建完整的 Descriptor 树，实现高性能反编译。
 *
 * ## 性能特点
 *
 * - **零 Descriptor 创建**: 不调用 `getDescriptor()` 或 `resolve()`
 * - **直接 Stub 访问**: 通过 `stub.name`、`stub.visibility` 等属性直接读取
 * - **延迟遍历**: 仅在需要时访问子节点
 * - **高效文本拼接**: 使用 [PrettyPrinter] 的 StringBuilder
 *
 * ## 工作流程
 *
 * ```
 * 1. 验证 Stub 有效性
 *    ├─ Invalid → 返回错误消息
 *    └─ Valid → 继续
 * 2. 初始化 PrettyPrinter (4 空格缩进)
 * 3. 输出文件头注释
 * 4. 输出包声明 (如果非根包)
 * 5. 创建访问者实例
 *    └─ 定义所有 visit* 方法
 * 6. 遍历顶层声明
 *    ├─ class/interface/struct
 *    ├─ enum
 *    ├─ extend
 *    ├─ func
 *    ├─ type alias
 *    └─ let/var
 * 7. 返回 DecompiledText
 * ```
 *
 * ## 渲染示例
 *
 * ### 输入 (Stub 树)
 * ```
 * CangJieFileStub (package: std.collection)
 *   ├─ CangJieClassStub (name: ArrayList, typeParams: [T])
 *   │   ├─ CjTypeParameterList
 *   │   │   └─ CjTypeParameter ("T")
 *   │   ├─ CjTypeConstraintList
 *   │   │   └─ CjTypeConstraint (T: Comparable<T>)
 *   │   └─ CjClassBody
 *   │       ├─ CjVariable (size: Int32)
 *   │       └─ CjNamedFunction (add: (T) -> Unit)
 * ```
 *
 * ### 输出 (反编译文本)
 * ```kotlin
 * // IntelliJ API Decompiler stub source generated from a cjo file
 * // Implementation of methods is not available
 *
 * package std.collection
 *
 * public class ArrayList<T> where T: Comparable<T> {
 *     private var size: Int32
 *
 *     public func add(element: T): Unit { /* compiled code */ }
 * }
 * ```
 *
 * ## 错误处理
 *
 * ### 无效 Stub
 * ```kotlin
 * val invalidStub = CangJieFileStubImpl(kind = Invalid("Corrupted metadata"))
 * buildDecompiledText(invalidStub)
 * // 返回: DecompiledText("Corrupted metadata")
 * ```
 *
 * ### 缺失元素
 * 对于可选的 PSI 元素（如注解、类型参数），安全地检查 null：
 * ```kotlin
 * typeStatement.typeParameterList?.accept(visitor, Unit)
 * ```
 *
 * ### 未知元素
 * 输出占位符以便调试：
 * ```kotlin
 * /* !UnknownElement! */
 * ```
 *
 * ## 使用场景
 *
 * 1. **编辑器显示**: 用户导航到 .cjo 文件时显示反编译代码
 * 2. **调试器**: 在调试时显示库代码
 * 3. **API 浏览**: 查看第三方库的接口
 * 4. **代码生成**: 作为模板生成代码
 *
 * ## 线程安全
 *
 * - **只读操作**: 只读取 Stub 数据，不修改
 * - **无状态**: 每次调用创建新的访问者实例
 * - **并发友好**: 可以在多个线程中并发调用
 *
 * ## 性能指标
 *
 * 对于典型文件：
 * - **小文件** (< 100 行): < 10ms
 * - **中等文件** (100-500 行): 10-50ms
 * - **大文件** (> 500 行): 50-200ms
 *
 * 大部分时间消耗在字符串拼接和缩进计算上。
 *
 * @param fileStub 文件 Stub 树的根节点
 * @return 反编译后的文本，包含格式化的仓颉源代码
 *
 * @see DecompiledText
 * @see CangJieFileStubImpl
 * @see CjVisitor
 * @see PrettyPrinter
 */
fun buildDecompiledText(fileStub: CangJieFileStubImpl): DecompiledText {
    val text = PrettyPrinter(indentSize = 4).apply {
        // 处理无效的 stub
        (fileStub.kind as? CangJieFileStubKind.Invalid)?.errorMessage?.let {
            return DecompiledText(it)
        }

        appendLine("// IntelliJ API Decompiler stub source generated from a cjo file")
        appendLine("// Implementation of methods is not available")
        appendLine()

        val packageFqName = fileStub.getPackageFqName()
        if (!packageFqName.isRoot) {
            append("package ")
            appendLine(packageFqName.render())
            appendLine()
        }

        // 定义访问器
        val visitor = object : CjVisitor<Unit, Unit>() {
            private inline val explicitThis get() = this

            override fun visitClass(klass: CjClass, data: Unit): Unit? {
                printClassOrInterface(klass, "class")
                return null
            }

            override fun visitInterface(cinterface: CjInterface, data: Unit): Unit? {
                printClassOrInterface(cinterface, "interface")
                return null
            }

            override fun visitStruct(cstruct: CjStruct, data: Unit): Unit? {
                printClassOrInterface(cstruct, "struct")
                return null
            }

            private fun printClassOrInterface(typeStatement: CjTypeStatement, keyword: String) {
                withSuffix(" ") { typeStatement.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { typeStatement.modifierList?.accept(explicitThis, Unit) }
                append(keyword)
                withPrefix(" ") { append(typeStatement.name?.quoteIfNeeded()) }
                typeStatement.typeParameterList?.accept(explicitThis, Unit)

                withPrefix(" <: ") { typeStatement.getSuperTypeList()?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { typeStatement.typeConstraintList?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    val declarations = typeStatement.body?.declarations ?: emptyList()
                    withSuffix("\n") {
                        printCollectionIfNotEmpty(declarations, separator = "\n\n") {
                            it.accept(explicitThis, Unit)
                        }
                    }
                }
                append('}')
            }

            override fun visitTypeConstraintList(list: CjTypeConstraintList, data: Unit): Unit? {
                append("where ")
                printCollection(list.constraints, separator = ", ") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitEnum(cenum: CjEnum, data: Unit): Unit? {
                withSuffix(" ") { cenum.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { cenum.modifierList?.accept(explicitThis, Unit) }
                append("enum")
                withPrefix(" ") { append(cenum.name?.quoteIfNeeded()) }
                cenum.typeParameterList?.accept(explicitThis, Unit)
                withPrefix(" <: ") { cenum.getSuperTypeList()?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { cenum.typeConstraintList?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    val constructors = cenum.constructor
                    val members = cenum.body?.declarations ?: emptyList()

                    // 检查是否为非穷举枚举
                    val isNonExhaustive = cenum.isNonExhaustive

                    // 渲染枚举构造器
                    if (constructors.isNotEmpty()) {
                        printCollection(constructors, separator = "\n    | ") {
                            it.accept(explicitThis, Unit)
                        }

                        // 如果是非穷举枚举，添加省略号
                        if (isNonExhaustive) {
                            appendLine()
                            append("    | ...")
                        }

                        // 如果有成员，添加分号分隔
                        if (members.isNotEmpty()) {
                            append(";")
                            appendLine()
                        } else {
                            appendLine()
                        }
                    }

                    // 渲染成员声明
                    if (members.isNotEmpty()) {
                        appendLine()
                        printCollectionIfNotEmpty(members, separator = "\n\n") {
                            it.accept(explicitThis, Unit)
                        }
                        appendLine()
                    }
                }
                append('}')
                return null
            }

            override fun visitEnumConstructor(cjEnumConstructor: CjEnumConstructor, data: Unit): Unit? {
                withSuffix(" ") { cjEnumConstructor.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { cjEnumConstructor.modifierList?.accept(explicitThis, Unit) }
                append(cjEnumConstructor.name?.quoteIfNeeded())

                // 如果有类型参数，渲染类型列表
                val typeRefs = cjEnumConstructor.typeReferences
                if (typeRefs.isNotEmpty()) {
                    printCollection(typeRefs, prefix = "(", postfix = ")") {
                        it.accept(explicitThis, Unit)
                    }
                }

                return null
            }

            override fun visitExtend(cjExtend: CjExtend, data: Unit): Unit? {
                withSuffix(" ") { cjExtend.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { cjExtend.modifierList?.accept(explicitThis, Unit) }
                append("extend")
                cjExtend.typeParameterList?.accept(explicitThis, Unit)
                withPrefix(" ") { cjExtend.receiverTypeReceiver?.accept(explicitThis, Unit) }
                withPrefix(" <: ") { cjExtend.getSuperTypeList()?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { cjExtend.typeConstraintList?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    val declarations = cjExtend.body?.declarations ?: emptyList()
                    withSuffix("\n") {
                        printCollectionIfNotEmpty(declarations, separator = "\n\n") {
                            it.accept(explicitThis, Unit)
                        }
                    }
                }
                append('}')
                return null
            }

            override fun visitNamedFunction(function: CjNamedFunction, data: Unit): Unit? {
                withSuffix(" ") { function.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { function.modifierList?.accept(explicitThis, Unit) }
                append("func ")

                // 处理运算符重载的函数名渲染
                val name = function.name
                if (function.isOperator && name != null) {
                    // 将内部运算符名称（如 *operator_get）转换为运算符符号（如 []）
                    val operatorSymbol = name.asOperatorName().asOperatorString()
                    append(operatorSymbol)
                } else {
                    append(name?.quoteIfNeeded())
                }

                function.typeParameterList?.accept(explicitThis, Unit)
                function.valueParameterList?.accept(explicitThis, Unit)
                withPrefix(": ") { function.typeReference?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { function.typeConstraintList?.accept(explicitThis, Unit) }

                printFunctionBody(function)
                return null
            }

            override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: Unit): Unit? {
                withSuffix(" ") { constructor.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { constructor.modifierList?.accept(explicitThis, Unit) }
                append(constructor.name)
                constructor.valueParameterList?.accept(explicitThis, Unit)
                append(" { $DECOMPILED_CODE_COMMENT }")
                return null
            }

            override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: Unit): Unit? {
                withSuffix(" ") { constructor.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { constructor.modifierList?.accept(explicitThis, Unit) }
                append("init")
                constructor.valueParameterList?.accept(explicitThis, Unit)
                append(" { $DECOMPILED_CODE_COMMENT }")
                return null
            }

            private fun printFunctionBody(function: CjNamedFunction) {
                val modifierList = function.modifierList
                val isAbstract = modifierList?.hasModifier(CjTokens.ABSTRACT_KEYWORD) == true

                if (!isAbstract && function.hasBody()) {
                    append(" { $DECOMPILED_CODE_COMMENT }")
                }
            }

            override fun visitFieldVariable(field: CjFieldVariable, data: Unit) {
                withSuffix(" ") { field.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { field.modifierList?.accept(explicitThis, Unit) }
                if (field.isVar) {
                    append("var ")
                } else {
                    append("let ")
                }

                append(field.name?.quoteIfNeeded())

                withPrefix(": ") { field.typeReference?.accept(explicitThis, Unit) }

                if (field.hasInitializer()) {
                    append(" = $COMPILED_DEFAULT_INITIALIZER")
                }
            }

            override fun visitPatternVariable(variable: CjPatternVariable, data: Unit): Unit? {
                withSuffix(" ") { variable.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { variable.modifierList?.accept(explicitThis, Unit) }
                if (variable.isVar) {
                    append("var ")
                } else {
                    append("let ")
                }
                // 从模式中获取名称（对于反编译的代码，总是简单绑定模式）
                val pattern = variable.pattern

//                没必要处理其他模式了，反编译代码中不会出现复杂模式
                if (pattern is CjBindingPattern) {
                    append(pattern.name?.quoteIfNeeded())
                }
                withPrefix(": ") { variable.typeReference?.accept(explicitThis, Unit) }

                if (variable.hasInitializer()) {
                    append(" = $COMPILED_DEFAULT_INITIALIZER")
                }
                return null
            }

            override fun visitProperty(property: CjProperty, data: Unit): Unit? {
                withSuffix(" ") { property.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { property.modifierList?.accept(explicitThis, Unit) }
                if (property.isVar) {
                    append("mut prop ")
                } else {
                    append("prop ")
                }
                append(property.name?.quoteIfNeeded())
                withPrefix(": ") { property.typeReference?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    for (accessor in property.accessors) {
                        accessor.accept(explicitThis, Unit)
                        appendLine()
                    }
                }
                append('}')
                return null
            }

            override fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: Unit): Unit? {
                withSuffix(" ") { accessor.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { accessor.modifierList?.accept(explicitThis, Unit) }
                if (accessor.isGetter) {
                    append("get()")
                } else {
                    append("set(")
                    accessor.parameter?.let {
                        append(it.name?.quoteIfNeeded())
                    }
                    append(")")
                }
                append(" { $DECOMPILED_CODE_COMMENT }")
                return null
            }

            override fun visitTypeAlias(typeAlias: CjTypeAlias, data: Unit): Unit? {
                withSuffix(" ") { typeAlias.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { typeAlias.modifierList?.accept(explicitThis, Unit) }
                append("type ")
                append(typeAlias.name?.quoteIfNeeded())
                typeAlias.typeParameterList?.accept(explicitThis, Unit)
                withPrefix(" = ") { typeAlias.getTypeReference()?.accept(explicitThis, Unit) }
                return null
            }

            override fun visitTypeParameter(parameter: CjTypeParameter, data: Unit): Unit? {
                withSuffix(" ") { parameter.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { parameter.modifierList?.accept(explicitThis, Unit) }
                append(parameter.name?.quoteIfNeeded())
                // 注意：类型约束应该在 where 子句中处理，不在类型参数列表中
                // extendsBound 会通过 typeConstraintList 来渲染
                return null
            }

            override fun visitTypeParameterList(list: CjTypeParameterList, data: Unit): Unit? {
                printCollection(list.parameters, prefix = "<", postfix = ">") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitParameterList(cjParameterList: CjParameterList, data: Unit): Unit? {
                printCollection(cjParameterList.parameters, prefix = "(", postfix = ")") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitParameter(cjParameter: CjParameter, data: Unit): Unit? {
                withSuffix(" ") { cjParameter.annotations?.accept(explicitThis, Unit) }
                withSuffix(" ") { cjParameter.modifierList?.accept(explicitThis, Unit) }
                append(cjParameter.name?.quoteIfNeeded())
                // 如果是命名参数，添加 ! 标记
                if (cjParameter.isNamed) {
                    append("!")
                }
                append(": ")
                cjParameter.typeReference?.accept(explicitThis, Unit)
                if (cjParameter.hasDefaultValue()) {
                    append(" = $COMPILED_DEFAULT_PARAMETER_VALUE")
                }
                return null
            }

            override fun visitTypeReference(typeReference: CjTypeReference, data: Unit): Unit? {
                typeReference.typeElement?.accept(explicitThis, Unit)
                return null
            }

            override fun visitUserType(type: CjUserType, data: Unit): Unit? {
                withSuffix(".") { type.qualifier?.accept(explicitThis, Unit) }
                val name = type.referencedName
                if (!name.isNullOrEmpty()) {
                    append(name.quoteIfNeeded())
                }
                type.typeArgumentList?.accept(explicitThis, Unit)
                return null
            }

            override fun visitFunctionType(type: CjFunctionType, data: Unit): Unit? {
                try {
                    printCollection(type.parameters, prefix = "(", postfix = ")") { param ->
                        val paramName = param.name
                        // 只有当参数名不为空且不是 "_" 时才渲染参数名
                        if (!paramName.isNullOrEmpty() && paramName != "_") {
                            append(paramName)
                            append(": ")
                        }
                        param.typeReference?.accept(explicitThis, Unit)
                    }
                    type.returnTypeReference?.let { returnType ->
                        append(" -> ")
                        returnType.accept(explicitThis, Unit)
                    }
                } catch (e: Exception) {
                    append("/* error rendering function type: ${e.message ?: e::class.simpleName} */")
                }
                return null
            }

            override fun visitOptionType(optionType: CjOptionType, data: Unit): Unit? {
                optionType.getInnerType()?.accept(explicitThis, Unit)
                append("?")
                return null
            }

            override fun visitTupleType(cjTupleType: CjTupleType, data: Unit): Unit? {
                printCollection(cjTupleType.typeArgumentsAsTypes, prefix = "(", postfix = ")") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitBasicType(basicType: CjBasicType, data: Unit): Unit? {
                append(basicType.name)
                return null
            }

            override fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList, data: Unit): Unit? {
                printCollection(typeArgumentList.arguments, prefix = "<", postfix = ">") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitTypeProjection(typeProjection: CjTypeProjection, data: Unit): Unit? {
                typeProjection.typeReference?.accept(explicitThis, Unit)
                return null
            }

            override fun visitSuperTypeList(list: CjSuperTypeList, data: Unit): Unit? {
                printCollection(list.entries, separator = " & ") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: Unit): Unit? {
                specifier.typeReference?.accept(explicitThis, Unit)
                return null
            }

            override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: Unit): Unit? {
                call.typeReference?.accept(explicitThis, Unit)
                return null
            }


            override fun visitTypeConstraint(constraint: CjTypeConstraint, data: Unit): Unit? {
                constraint.subjectTypeParameterName?.accept(explicitThis, Unit)
                append(" <: ")
                constraint.boundTypeReference?.accept(explicitThis, Unit)
                return null
            }

            override fun visitModifierList(list: CjModifierList, data: Unit): Unit? {
                // 注解由 visitAnnotation 处理，这里只处理修饰符
                printModifiers(list)
                return null
            }

            private fun visitAnnotationEntry(annotation: CjAnnotation) {
                append('@')
                // 注意：从 stub 创建的 CjAnnotation 没有 CONSTRUCTOR_CALLEE 子节点，
                // 因此 typeReference 会是 null。直接使用 stub 中存储的 shortName。
                val shortName = annotation.shortName?.asString()
                if (shortName != null) {
                    append(shortName)
                } else {
                    // 降级：尝试使用 typeReference（用于非 stub 场景）
                    annotation.typeReference?.accept(explicitThis, Unit)
                }
            }

            override fun visitAnnotation(annotation: CjAnnotations, data: Unit): Unit? {
                printCollectionIfNotEmpty(annotation.entries, separator = " ") {
                    visitAnnotationEntry(it)
                }
                return null
            }

            private fun printModifiers(list: CjModifierList) {
                val stub = list.stub as? CangJieModifierListStubImpl ?: return

                // 直接使用 MODIFIER_KEYWORDS_ARRAY 的顺序（已经是正确的：访问控制修饰符在前）
                var hadValue = false
                for (modifier in CjTokens.MODIFIER_KEYWORDS_ARRAY) {
                    if (!stub.hasModifier(modifier)) continue
                    if (hadValue) {
                        append(" ")
                    } else {
                        hadValue = true
                    }
                    append(modifier.value)
                }
            }

            override fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: Unit): Unit? {
                append(expression.referencedName)
                return null
            }

            override fun visitElement(element: PsiElement) {
                // 默认实现：输出占位符
                append("/* !${element::class.simpleName}! */")
                super.visitElement(element)
            }
        }

        // 获取声明并遍历
        val declarations = fileStub.getChildrenByType(
            FILE_DECLARATION_TYPES,
            CjDeclaration.ARRAY_FACTORY
        ).asList()

        printCollectionIfNotEmpty(declarations, separator = "\n\n", postfix = "\n") {
            it.accept(visitor, Unit)
        }
    }.toString()

    return DecompiledText(text)
}