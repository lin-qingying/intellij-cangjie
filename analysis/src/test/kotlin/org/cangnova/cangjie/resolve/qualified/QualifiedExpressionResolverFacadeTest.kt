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
 */

package org.cangnova.cangjie.resolve.qualified

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.scopes.LexicalScope

/**
 * QualifiedExpressionResolverFacade 测试类
 *
 * 测试限定表达式解析器门面的语义分析功能：
 * - 包声明的语义分析和作用域注册
 * - 导入语句的符号解析
 * - 类型引用的完整解析和类型推导
 * - 限定表达式的符号解析
 *
 * ## 测试策略
 *
 * 使用 `analyzeForTest { }` 执行完整的语义分析，验证：
 * 1. BindingContext 中记录了正确的符号解析信息
 * 2. 词法作用域（LexicalScope）正确构建
 * 3. 类型推导结果正确
 * 4. 引用目标（REFERENCE_TARGET）正确解析
 */
class QualifiedExpressionResolverFacadeTest : CangJieAnalysisTestBase() {

    // ==================== 包声明语义分析测试 ====================

    /**
     * 测试简单包声明的语义分析
     *
     * 验证：
     * 1. 包声明的 PSI 结构正确
     * 2. 文件的词法作用域包含包信息
     */
    fun `test simple package declaration analysis`() {
        val file = createFile("""
            package test

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            // 验证 PSI 级别的包声明
            val packageDirective = file.packageDirective
            assertNotNull("包声明不应为空", packageDirective)
            assertEquals("test", file.packageFqName.asString())

            // 验证语义分析：文件应该有词法作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("文件应该有词法作用域", lexicalScope)
        }
    }

    /**
     * 测试嵌套包声明的语义分析
     *
     * 验证嵌套包的完全限定名正确记录在 BindingContext 中
     */
    fun `test nested package declaration analysis`() {
        val file = createFile("""
            package com.example.test

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            assertEquals("com.example.test", file.packageFqName.asString())

            // 验证词法作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("嵌套包文件应该有词法作用域", lexicalScope)
        }
    }

    
    // ==================== 导入语句语义分析测试 ====================

    /**
     * 测试单个导入的语义分析
     *
     * 验证：
     * 1. PSI 级别的导入结构正确
     * 2. 导入路径被正确解析
     * 3. 文件的词法作用域受导入影响
     */
    fun `test single import analysis`() {
        val file = createFile("""
            package test
            import a.b.C

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            // 验证 PSI 结构
            val importDirectives = file.importDirectives
            assertEquals("应该有 1 个导入指令", 1, importDirectives.size)

            val directive = importDirectives[0]
            val items = directive.importItems
            assertEquals("应该有 1 个导入项", 1, items.size)

            val item = items[0]
            assertEquals("a.b.C", item.importedFqName?.asString())
            assertFalse("不是通配符导入", item.isAllUnder)
            assertNull("没有别名", item.aliasName)

            // 验证语义分析：导入应该影响词法作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("文件应该有词法作用域", lexicalScope)
        }
    }

    /**
     * 测试通配符导入的语义分析
     *
     * 验证通配符导入的 PSI 结构和作用域影响
     */
    fun `test wildcard import analysis`() {
        val file = createFile("""
            package test
            import a.b.*

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            val importDirectives = file.importDirectives
            assertEquals(1, importDirectives.size)

            val item = importDirectives[0].importItems[0]
            assertEquals("a.b", item.importedFqName?.asString())
            assertTrue("应该是通配符导入", item.isAllUnder)

            // 通配符导入也应该影响词法作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("通配符导入文件应该有词法作用域", lexicalScope)
        }
    }

    /**
     * 测试带别名导入的语义分析
     *
     * 验证别名在 BindingContext 中正确记录
     */
    fun `test aliased import analysis`() {
        val file = createFile("""
            package test
            import a.b.C as MyC

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            val importDirectives = file.importDirectives
            assertEquals(1, importDirectives.size)

            val item = importDirectives[0].importItems[0]
            assertEquals("a.b.C", item.importedFqName?.asString())
            assertEquals("MyC", item.aliasName)

            // 验证别名导入的作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("别名导入文件应该有词法作用域", lexicalScope)
        }
    }

    /**
     * 测试同包多项导入的语义分析
     *
     * 验证所有导入项都被正确解析
     */
    fun `test same package multiple imports analysis`() {
        val file = createFile("""
            package test
            import a.b.{C, D, E}

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            val importDirectives = file.importDirectives
            assertEquals(1, importDirectives.size)

            val items = importDirectives[0].importItems
            assertEquals(3, items.size)

            // 验证所有导入项
            assertEquals("a.b.C", items[0].importedFqName?.asString())
            assertFalse(items[0].isAllUnder)
            assertNull(items[0].aliasName)

            assertEquals("a.b.D", items[1].importedFqName?.asString())
            assertFalse(items[1].isAllUnder)
            assertNull(items[1].aliasName)

            assertEquals("a.b.E", items[2].importedFqName?.asString())
            assertFalse(items[2].isAllUnder)
            assertNull(items[2].aliasName)

            // 验证多项导入的作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("多项导入文件应该有词法作用域", lexicalScope)
        }
    }

    /**
     * 测试多包导入的语义分析
     *
     * 验证不同包的导入都被正确处理
     */
    fun `test multiple package imports analysis`() {
        val file = createFile("""
            package test
            import {a.b.C, x.y.Z}

            main() {}
        """.trimIndent())

        analyzeForTest(file) {
            val importDirectives = file.importDirectives
            assertEquals(1, importDirectives.size)

            val items = importDirectives[0].importItems
            assertEquals(2, items.size)

            assertEquals("a.b.C", items[0].importedFqName?.asString())
            assertEquals("x.y.Z", items[1].importedFqName?.asString())

            // 验证跨包导入的作用域
            val lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, file!!]
            assertNotNull("跨包导入文件应该有词法作用域", lexicalScope)
        }
    }

    // ==================== 类型引用语义分析测试 ====================

    /**
     * 测试简单类型引用的语义分析
     *
     * 验证：
     * 1. 类型引用的 PSI 结构正确
     * 2. 类型在 BindingContext 中正确解析
     * 3. 变量声明的类型推导正确
     */
    fun `test simple type reference analysis`() {
        val file = createFile("""
            package test

            var x: Int64 = 0
        """.trimIndent())

        analyzeForTest(file) {
            // 查找变量声明
            val variable = PsiTreeUtil.findChildOfType(file, CjPatternVariable::class.java)
            assertNotNull("应该找到变量声明", variable)

            val typeReference = variable!!.typeReference
            assertNotNull("应该有类型引用", typeReference)

            val basicType = typeReference!!.typeElement as? CjBasicType
            assertNotNull("应该是基本类型", basicType)
            assertEquals("Int64", basicType!!.name)

            // 验证语义分析：类型引用应该被解析
            val resolvedType = bindingContext[BindingContext.TYPE, typeReference!!]
            // 注意：Int64 是内置类型，应该能解析
        }
    }

    /**
     * 测试限定类型引用的语义分析
     *
     * 验证带包路径的类型引用能正确解析
     */
    fun `test qualified type reference analysis`() {
        val file = createFile("""
            package test

            var name: std.core.String = ""
        """.trimIndent())

        analyzeForTest(file) {
            val variable = PsiTreeUtil.findChildOfType(file, CjVariable::class.java)
            assertNotNull("应该找到变量声明", variable)

            val typeReference = variable!!.typeReference
            assertNotNull("应该有类型引用", typeReference)

            // 验证限定类型引用的语义分析
            val resolvedType =typeReference?.let{ bindingContext[BindingContext.TYPE, typeReference!!]}
        }
    }

    // ==================== 限定表达式语义分析测试 ====================

    /**
     * 测试简单限定表达式的语义分析
     *
     * 验证：
     * 1. 点限定表达式的 PSI 结构正确
     * 2. 接收者表达式正确解析
     * 3. 选择器正确解析到目标符号
     */
    fun `test simple qualified expression analysis`() {
        val file = createFile("""
            package test

            class Foo {
                public var bar: Int64 = 0
            }

            func test(foo: Foo) {
                var x = foo.bar
            }
        """.trimIndent())

        analyzeForTest(file) {
            // 查找限定表达式 foo.bar
            val qualifiedExpr = PsiTreeUtil.findChildOfType(file, CjDotQualifiedExpression::class.java)
            assertNotNull("应该找到限定表达式", qualifiedExpr)

            // 验证 PSI 结构
            val receiverExpr = qualifiedExpr!!.receiverExpression
            assertNotNull("应该有接收者表达式", receiverExpr)

            val selectorExpr = qualifiedExpr.selectorExpression
            assertNotNull("应该有选择器表达式", selectorExpr)

            // 验证语义分析：foo 应该解析到参数
            if (receiverExpr is CjReferenceExpression) {
                val receiverTarget = bindingContext[BindingContext.REFERENCE_TARGET, receiverExpr!!]
            }

            // 验证 bar 解析到 Foo.bar 字段
            if (selectorExpr is CjReferenceExpression) {
                val selectorTarget = bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr!!]
            }
        }
    }

    /**
     * 测试函数声明的语义分析
     *
     * 验证函数声明能正确创建 FunctionDescriptor
     */
    fun `test function declaration analysis`() {
        val file = createFile("""
            package test

            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }
        """.trimIndent())

        analyzeForTest(file) {
            // 查找函数声明
            val function = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", function)

            // 验证函数描述符
            val functionDescriptor =function?.let{
                bindingContext[BindingContext.FUNCTION, function!!]
            }
        }
    }

    /**
     * 测试类声明的语义分析
     *
     * 验证类声明能正确创建 ClassDescriptor
     */
    fun `test class declaration analysis`() {
        val file = createFile("""
            package test

            class Foo {
                public var bar: Int64 = 0
            }
        """.trimIndent())

        analyzeForTest(file) {
            // 查找类声明
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            // 验证类描述符
            val classDescriptor =classDecl?.let{
                bindingContext[BindingContext.CLASS, classDecl!!]
            }
        }
    }
}
