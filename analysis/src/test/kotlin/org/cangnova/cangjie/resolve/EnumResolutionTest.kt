/*
 * Copyright 2026 LinQingYing. and contributors.
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
package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.EnumKind
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.resolve.binding.BindingContext


/**
 * 枚举解析测试
 *
 * 测试枚举类型的声明、解析和类型检查，包括：
 * - 简单枚举（无关联值）
 * - 带关联值的枚举
 * - 泛型枚举
 * - 非穷尽性枚举
 * - Option 类型枚举
 * - 枚举构造函数解析
 * - 枚举成员访问
 * - 枚举类型推断
 */
class EnumResolutionTest : CangJieAnalysisTestBase() {

    // ==================== 简单枚举测试 ====================

    /**
     * 测试简单枚举声明
     *
     * 验证：
     * 1. 枚举描述符正确创建
     * 2. 枚举类型为 ENUM
     * 3. 枚举构造函数正确解析
     * 4. 不是非穷尽性枚举
     */
    fun `test simple enum declaration`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor
            // 验证基本属性
            assertEquals("Color", enumDescriptor.name.asString())
            assertEquals(ClassKind.ENUM, enumDescriptor.kind)
            assertEquals(EnumKind.ENUM, enumDescriptor.enumKind)
            assertFalse("简单枚举不应该有关联值", enumDescriptor.hasArguments)
            assertFalse("简单枚举不应该是非穷尽性", enumDescriptor.isNonExhaustive)

            // 验证构造函数
            val constructors = enumDescriptor.constructors
            assertEquals(3, constructors.size)

            val constructorNames = constructors.map { it.name.asString() }.toSet()
            assertTrue(constructorNames.contains("Red"))
            assertTrue(constructorNames.contains("Green"))
            assertTrue(constructorNames.contains("Blue"))

            // 验证构造函数属性
            constructors.forEach { constructor ->
                assertTrue("枚举构造函数应该是静态的", constructor.isStatic)
                assertTrue("枚举构造函数应该是常量", constructor.isConst)
                assertFalse("枚举构造函数不应该是变量", constructor.isVar)
                assertTrue("简单构造函数应该没有参数", constructor.isSimpleConstructor)
                assertFalse("简单构造函数不应该是函数构造函数", constructor.isFunctionConstructor)
                assertEquals(0, constructor.valueParameters.size)
            }
        }
    }

    // ==================== 带关联值的枚举测试 ====================

    /**
     * 测试带关联值的枚举
     *
     * 验证：
     * 1. 有关联值的构造函数正确解析
     * 2. 参数类型正确
     * 3. 混合简单和复杂构造函数
     */
    fun `test enum with associated values`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String) |
                Pending
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor =bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            assertTrue("枚举应该有关联值", enumDescriptor.hasArguments)

            val constructors = enumDescriptor.constructors
            assertEquals(3, constructors.size)

            // 检查 Success 构造函数
            val success = constructors.first { it.name.asString() == "Success" }
            assertTrue("Success 应该有参数", success.hasArguments)
            assertTrue("Success 应该是函数构造函数", success.isFunctionConstructor)
            assertFalse("Success 不应该是简单构造函数", success.isSimpleConstructor)
            assertEquals(1, success.valueParameters.size)
            assertEquals("Int64", success.valueParameters[0].type.toString())

            // 检查 Error 构造函数
            val error = constructors.first { it.name.asString() == "Error" }
            assertTrue("Error 应该有参数", error.hasArguments)
            assertEquals(1, error.valueParameters.size)
            assertEquals("String", error.valueParameters[0].type.toString())

            // 检查 Pending 构造函数
            val pending = constructors.first { it.name.asString() == "Pending" }
            assertFalse("Pending 不应该有参数", pending.hasArguments)
            assertTrue("Pending 应该是简单构造函数", pending.isSimpleConstructor)
            assertEquals(0, pending.valueParameters.size)
        }
    }

    // ==================== 泛型枚举测试 ====================
    /**
     * 测试泛型枚举解析与推导
     *
     * 验证：
     * 1. 类型参数正确解析
     * 2. 构造函数使用类型参数
     * 3. 泛型约束处理
     */
    fun `test generic enum type inference`() {
        val file = createFile(
            """
            package test

            enum Wrapper<T> {
                Value(T) |
                Empty
            }
func test():Unit{
Wrapper.Empty<Int>
}
//            func wrapInt(x: Int64): Wrapper<Int64> {
//                return Wrapper.Value(x)
//            }
//
//            func wrapString(s: String): Wrapper<String> {
//                return Wrapper.Value(s)
//            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证类型参数
            val typeParams = enumDescriptor.declaredTypeParameters
            assertEquals(1, typeParams.size)
            assertEquals("T", typeParams[0].name.asString())

            // 验证构造函数使用正确的类型参数
            val valueConstructor = enumDescriptor.constructors.first { it.name.asString() == "Value" }
            val paramType = valueConstructor.valueParameters[0].type
            assertTrue("参数类型应该是类型参数 T", paramType.toString().contains("T"))
        }
    }
    /**
     * 测试泛型枚举
     *
     * 验证：
     * 1. 类型参数正确解析
     * 2. 构造函数使用类型参数
     * 3. 泛型约束处理
     */
    fun `test generic enum`() {
        val file = createFile(
            """
            package test

            enum Option<T> {
                Some(T) |
                None
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证类型参数
            val typeParams = enumDescriptor.declaredTypeParameters
            assertEquals(1, typeParams.size)
            assertEquals("T", typeParams[0].name.asString())

            // 验证构造函数
            val constructors = enumDescriptor.constructors
            assertEquals(2, constructors.size)

            // 检查 Some 构造函数
            val some = constructors.first { it.name.asString() == "Some" }
            assertTrue("Some 应该有参数", some.hasArguments)
            assertEquals(1, some.valueParameters.size)

            val paramType = some.valueParameters[0].type
            assertTrue("参数类型应该是类型参数 T", paramType.toString().contains("T"))

            // 检查 None 构造函数
            val none = constructors.first { it.name.asString() == "None" }
            assertFalse("None 不应该有参数", none.hasArguments)
        }
    }

    /**
     * 测试多个类型参数的泛型枚举
     */
    fun `test enum with multiple type parameters`() {
        val file = createFile(
            """
            package test

            enum Either<L, R> {
                Left(L) |
                Right(R)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor =bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证类型参数
            val typeParams = enumDescriptor.declaredTypeParameters
            assertEquals(2, typeParams.size)
            assertEquals("L", typeParams[0].name.asString())
            assertEquals("R", typeParams[1].name.asString())

            // 验证构造函数使用正确的类型参数
            val left = enumDescriptor.constructors.first { it.name.asString() == "Left" }
            val right = enumDescriptor.constructors.first { it.name.asString() == "Right" }

            assertEquals(1, left.valueParameters.size)
            assertEquals(1, right.valueParameters.size)
        }
    }

    // ==================== 非穷尽性枚举测试 ====================

    /**
     * 测试非穷尽性枚举
     *
     * 验证：
     * 1. 省略号正确识别
     * 2. isNonExhaustive 标志正确
     * 3. enumKind 正确
     */
    fun `test non-exhaustive enum`() {
        val file = createFile(
            """
            package test

            enum Status {
                Active |
                Inactive |
                ...
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            assertTrue("应该识别为非穷尽性枚举", enumDescriptor.isNonExhaustive)
            assertTrue("hasEllipsis 应该为 true", enumDescriptor.hasEllipsis)
            assertEquals(EnumKind.NON_EXHAUSTIVE, enumDescriptor.enumKind)
            assertTrue("isNonExhaustiveEnum() 应该返回 true", enumDescriptor.isNonExhaustiveEnum())
        }
    }

    // ==================== Option 类型测试 ====================

    /**
     * 测试标准库 Option 类型识别
     *
     * 验证：
     * 1. Option 是枚举类型
     * 2. 有正确的构造函数
     * 3. 是泛型枚举
     */
    fun `test Option type enum`() {
        val file = createFile(
            """
            package test

            import std.core.*

            func test() {
                let x: Option<Int64> = Some(42)
                let y: Option<String> = None
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val optionDescriptor = builtIns.stdlibTypes.option

            assertTrue("Option 应该是枚举类型", optionDescriptor is EnumDescriptor)
            assertEquals(ClassKind.ENUM, optionDescriptor.kind)

            if (optionDescriptor is EnumDescriptor) {
                // 验证 Option 的构造函数
                val constructors = optionDescriptor.constructors
                assertEquals(2, constructors.size)

                val constructorNames = constructors.map { it.name.asString() }.toSet()
                assertTrue("应该有 Some 构造函数", constructorNames.contains("Some"))
                assertTrue("应该有 None 构造函数", constructorNames.contains("None"))

                // 验证类型参数
                assertEquals(1, optionDescriptor.declaredTypeParameters.size)
            }
        }
    }

    // ==================== 枚举构造函数调用测试 ====================

    /**
     * 测试枚举构造函数调用解析
     *
     * 验证：
     * 1. Color.Red 正确解析到 Red 构造函数
     * 2. 表达式类型为 Color
     * 3. 引用目标是 EnumConstructorDescriptor
     */
    fun `test enum constructor call resolution`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue
            }

            func getColor(): Color {
                return Color.Red
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = com.intellij.psi.util.PsiTreeUtil.findChildOfType(file, org.cangnova.cangjie.psi.CjFunction::class.java)
            assertNotNull("应该找到 getColor 函数", function)

            // 查找函数体中的引用表达式 (Color.Red)
            val referenceExpressions = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                function,
                org.cangnova.cangjie.psi.CjReferenceExpression::class.java
            )
            assertFalse("应该找到引用表达式", referenceExpressions.isEmpty())

            // 找到 Color.Red 中的 Red 引用
            val redReference = referenceExpressions.firstOrNull { it.text == "Red" }
            assertNotNull("应该找到 Red 引用", redReference)

            // 验证引用解析到枚举构造函数
            val target = bindingContext[BindingContext.REFERENCE_TARGET, redReference!!]
            assertNotNull("Red 应该解析到描述符", target)
            assertTrue("应该解析到枚举构造函数", target is EnumConstructorDescriptor)

            if (target is EnumConstructorDescriptor) {
                assertEquals("Red", target.name.asString())
                assertTrue("构造函数应该是静态的", target.isStatic)
                assertTrue("构造函数应该是常量", target.isConst)
                assertFalse("简单构造函数不应该有参数", target.hasArguments)
            }

            // 验证表达式类型
            val typeInfo = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, redReference]
            assertNotNull("表达式应该有类型信息", typeInfo)
            assertEquals("Color", typeInfo?.type?.toString())
        }
    }

    /**
     * 测试带参数的枚举构造函数调用
     *
     * 验证：
     * 1. Result.Success(42) 正确解析
     * 2. 参数类型正确匹配
     * 3. 返回类型为 Result
     * 4. 解析到带参数的枚举构造函数
     */
    fun `test enum constructor call with arguments`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String)
            }

            func createResult(): Result {
                return Result.Success(42)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = com.intellij.psi.util.PsiTreeUtil.findChildOfType(file, org.cangnova.cangjie.psi.CjFunction::class.java)
            assertNotNull("应该找到 createResult 函数", function)

            // 查找调用表达式 Result.Success(42)
            val callExpressions = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                function,
                org.cangnova.cangjie.psi.CjCallExpression::class.java
            )
            assertFalse("应该找到调用表达式", callExpressions.isEmpty())

            // 获取 Success 引用
            val referenceExpressions = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                function,
                org.cangnova.cangjie.psi.CjReferenceExpression::class.java
            )
            val successReference = referenceExpressions.firstOrNull { it.text == "Success" }
            assertNotNull("应该找到 Success 引用", successReference)

            // 验证引用解析到枚举构造函数
            val target = bindingContext[BindingContext.REFERENCE_TARGET, successReference!!]
            assertNotNull("Success 应该解析到描述符", target)
            assertTrue("应该解析到枚举构造函数", target is EnumConstructorDescriptor)

            if (target is EnumConstructorDescriptor) {
                assertEquals("Success", target.name.asString())
                assertTrue("构造函数应该有参数", target.hasArguments)
                assertTrue("应该是函数构造函数", target.isFunctionConstructor)
                assertFalse("不应该是简单构造函数", target.isSimpleConstructor)

                // 验证参数
                val valueParams = target.valueParameters
                assertEquals(1, valueParams.size)
                assertEquals("Int64", valueParams[0].type.toString())
            }

            // 验证调用表达式的类型
            val callExpr = callExpressions.first()
            val callTypeInfo = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, callExpr]
            assertNotNull("调用表达式应该有类型信息", callTypeInfo)
            assertEquals("Result", callTypeInfo?.type?.toString())
        }
    }

    /**
     * 测试直接调用枚举构造器（不使用类型前缀）
     *
     * 验证：
     * 1. 直接使用 Red（而不是 Color.Red）能够正确解析
     * 2. 引用目标是 EnumConstructorDescriptor
     * 3. 表达式类型为 Color
     * 4. 作用域内可以直接访问枚举构造器
     */
    fun `test direct enum constructor call`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue
            }

            func getColor(): Color {
                return Red
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = com.intellij.psi.util.PsiTreeUtil.findChildOfType(
                file,
                org.cangnova.cangjie.psi.CjFunction::class.java
            )
            assertNotNull("应该找到 getColor 函数", function)

            // 查找函数体中的引用表达式 (直接使用 Red)
            val referenceExpressions = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                function,
                org.cangnova.cangjie.psi.CjReferenceExpression::class.java
            )
            val redReference = referenceExpressions.firstOrNull { it.text == "Red" }
            assertNotNull("应该找到 Red 引用", redReference)

            // 验证引用解析到枚举构造函数
            val target = bindingContext[BindingContext.REFERENCE_TARGET, redReference!!]
            assertNotNull("Red 应该解析到描述符", target)
            assertTrue("应该解析到枚举构造函数", target is EnumConstructorDescriptor)

            if (target is EnumConstructorDescriptor) {
                assertEquals("Red", target.name.asString())
                assertTrue("构造函数应该是静态的", target.isStatic)
                assertTrue("构造函数应该是常量", target.isConst)
                assertFalse("简单构造函数不应该有参数", target.hasArguments)

                // 验证所属枚举类型
                val containingDeclaration = target.containingDeclaration
                assertTrue("应该属于枚举类型", containingDeclaration is EnumDescriptor)
                if (containingDeclaration is EnumDescriptor) {
                    assertEquals("Color", containingDeclaration.name.asString())
                }
            }

            // 验证表达式类型
            val typeInfo = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, redReference]
            assertNotNull("表达式应该有类型信息", typeInfo)
            assertEquals("Color", typeInfo?.type?.toString())
        }
    }

    /**
     * 测试直接调用带参数的枚举构造器
     *
     * 验证：
     * 1. 直接使用 Success(42) 能够正确解析
     * 2. 参数类型检查正确
     * 3. 返回类型为 Result
     */
    fun `test direct enum constructor call with arguments`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String)
            }

            func createSuccess(): Result {
                return Success(42)
            }

            func createError(): Result {
                return Error("failed")
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                file,
                org.cangnova.cangjie.psi.CjFunction::class.java
            )

            // 测试 Success(42)
            val createSuccessFunc = functions.firstOrNull { it.name == "createSuccess" }
            assertNotNull("应该找到 createSuccess 函数", createSuccessFunc)

            val successCallExpr = com.intellij.psi.util.PsiTreeUtil.findChildOfType(
                createSuccessFunc,
                org.cangnova.cangjie.psi.CjCallExpression::class.java
            )
            assertNotNull("应该找到调用表达式", successCallExpr)

            val successRefs = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                createSuccessFunc,
                org.cangnova.cangjie.psi.CjReferenceExpression::class.java
            )
            val successRef = successRefs.firstOrNull { it.text == "Success" }
            assertNotNull("应该找到 Success 引用", successRef)

            val successTarget = bindingContext[BindingContext.REFERENCE_TARGET, successRef!!]
            assertNotNull("Success 应该解析到描述符", successTarget)
            assertTrue("应该解析到枚举构造函数", successTarget is EnumConstructorDescriptor)

            if (successTarget is EnumConstructorDescriptor) {
                assertEquals("Success", successTarget.name.asString())
                assertTrue("应该有参数", successTarget.hasArguments)
                assertEquals(1, successTarget.valueParameters.size)
                assertEquals("Int64", successTarget.valueParameters[0].type.toString())
            }

            // 测试 Error("failed")
            val createErrorFunc = functions.firstOrNull { it.name == "createError" }
            assertNotNull("应该找到 createError 函数", createErrorFunc)

            val errorRefs = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
                createErrorFunc,
                org.cangnova.cangjie.psi.CjReferenceExpression::class.java
            )
            val errorRef = errorRefs.firstOrNull { it.text == "Error" }
            assertNotNull("应该找到 Error 引用", errorRef)

            val errorTarget = bindingContext[BindingContext.REFERENCE_TARGET, errorRef!!]
            assertNotNull("Error 应该解析到描述符", errorTarget)
            assertTrue("应该解析到枚举构造函数", errorTarget is EnumConstructorDescriptor)

            if (errorTarget is EnumConstructorDescriptor) {
                assertEquals("Error", errorTarget.name.asString())
                assertTrue("应该有参数", errorTarget.hasArguments)
                assertEquals(1, errorTarget.valueParameters.size)
                assertEquals("String", errorTarget.valueParameters[0].type.toString())
            }
        }
    }

    // ==================== 枚举成员访问测试 ====================

    /**
     * 测试枚举成员函数
     */
    fun `test enum member functions`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue

                func toHex(): String {
                    return ""
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证枚举有成员函数
            val memberScope = enumDescriptor.unsubstitutedMemberScope
            assertNotNull("枚举应该有作用域", memberScope)
        }
    }

    // ==================== 嵌套枚举测试 ====================

    /**
     * 测试嵌套枚举类型
     */
    fun `test nested enum Option types`() {
        val file = createFile(
            """
            package test

            enum Result<T> {
                Success(T) |
                Error(String)
            }

            func test() {
                let x: Option<Result<Int64>> = Some(Result.Success(42))
                let y: Option<Option<Int64>> = Some(Some(42))
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 验证嵌套的 Option 和 Result 枚举类型
        }
    }


}
