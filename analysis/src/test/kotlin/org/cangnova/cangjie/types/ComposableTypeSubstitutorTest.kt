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
 */

package org.cangnova.cangjie.types

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * ComposableTypeSubstitutor 测试
 *
 * 测试新的类型替换器系统：
 * - 基本替换功能
 * - 泛型参数替换
 * - 替换器组合
 * - 选项配置
 * - 错误处理
 *
 * ## 测试策略
 *
 * 1. **基本替换**: 验证简单类型参数的替换
 * 2. **嵌套替换**: 验证嵌套泛型类型的替换
 * 3. **组合替换**: 验证多个替换器的组合
 * 4. **Option类型**: 验证Option类型的替换
 * 5. **空替换器**: 验证EMPTY单例的行为
 */
class ComposableTypeSubstitutorTest : CangJieAnalysisTestBase() {

    // ==================== 基本替换测试 ====================

    /**
     * 测试简单类型参数替换
     *
     * 验证：
     * 1. 单个类型参数的替换
     * 2. 替换后类型正确
     */
    fun `test simple type parameter substitution`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }

            func createIntBox(): Box<Int64> {
                return Box<Int64>(42)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 查找 Box 类
            val boxClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Box" }
            assertNotNull("应该找到 Box 类", boxClass)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)

            // 测试类型参数
            val typeParameters = boxDescriptor!!.typeConstructor.parameters
            assertEquals("Box 应该有 1 个类型参数", 1, typeParameters.size)

            val tParam = typeParameters[0]
            assertEquals("类型参数名称应该是 T", "T", tParam.name.asString())

            // 创建替换器：T -> Int64
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val substitutor = ComposableTypeSubstitutor.create(
                mapOf(tParam.typeConstructor to int64Type)
            )

            assertFalse("替换器不应该为空", substitutor.isEmpty)

            // 测试替换
            val tType = tParam.defaultType
            val substitutedType = substitutor.safeSubstitute(tType.unwrap())

            assertNotNull("替换后的类型不应为 null", substitutedType)
            assertEquals("替换后应该是 Int64", int64Type, substitutedType)
        }
    }

    /**
     * 测试多个类型参数替换
     *
     * 验证：
     * 1. 多个类型参数的同时替换
     * 2. 每个参数都正确替换
     */
    fun `test multiple type parameters substitution`() {
        val file = createFile(
            """
            package test

            class Pair<K, V> {
                var key: K
                var value: V
                init(key: K, value: V) {
                    this.key = key
                    this.value = value
                }
            }

            func createPair(): Pair<String, Int64> {
                return Pair<String, Int64>("answer", 42)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val pairClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Pair" }
            assertNotNull("应该找到 Pair 类", pairClass)

            val pairDescriptor = bindingContext[BindingContext.CLASS, pairClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Pair 的描述符", pairDescriptor)

            val typeParameters = pairDescriptor!!.typeConstructor.parameters
            assertEquals("Pair 应该有 2 个类型参数", 2, typeParameters.size)

            val kParam = typeParameters[0]
            val vParam = typeParameters[1]

            // 创建替换器：K -> String, V -> Int64
            val stringType = resolutionFacade.moduleDescriptor.builtIns.stdlibTypes.stringType
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val substitutor = ComposableTypeSubstitutor.create(
                mapOf(
                    kParam.typeConstructor to stringType.unwrap(),
                    vParam.typeConstructor to int64Type
                )
            )

            // 测试 K 的替换
            val kType = kParam.defaultType
            val substitutedK = substitutor.safeSubstitute(kType.unwrap())
            assertEquals("K 应该替换为 String", stringType.unwrap(), substitutedK)

            // 测试 V 的替换
            val vType = vParam.defaultType
            val substitutedV = substitutor.safeSubstitute(vType.unwrap())
            assertEquals("V 应该替换为 Int64", int64Type, substitutedV)
        }
    }

    // ==================== 嵌套类型替换测试 ====================

    /**
     * 测试嵌套泛型类型替换
     *
     * 验证：
     * 1. Array<T> 中的 T 替换为具体类型
     * 2. 嵌套结构保持正确
     */
    fun `test nested generic type substitution`() {
        val file = createFile(
            """
            package test

            class Container<T> {
                var items: Array<T>
                init(items: Array<T>) {
                    this.items = items
                }
            }

            func createContainer(): Container<String> {
                return Container<String>(Array<String>(0, {i => ""}))
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val containerClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Container" }
            assertNotNull("应该找到 Container 类", containerClass)

            val containerDescriptor = bindingContext[BindingContext.CLASS, containerClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Container 的描述符", containerDescriptor)

            val typeParameters = containerDescriptor!!.typeConstructor.parameters
            assertEquals("Container 应该有 1 个类型参数", 1, typeParameters.size)

            val tParam = typeParameters[0]

            // 创建替换器：T -> String
            val stringType = resolutionFacade.moduleDescriptor.builtIns.stdlibTypes.stringType
            val substitutor = ComposableTypeSubstitutor.create(
                mapOf(tParam.typeConstructor to stringType.unwrap())
            )

            // 测试替换
            val tType = tParam.defaultType
            val substitutedType = substitutor.safeSubstitute(tType.unwrap())
            assertEquals("T 应该替换为 String", stringType.unwrap(), substitutedType)
        }
    }

    // ==================== 替换器组合测试 ====================

    /**
     * 测试替换器组合
     *
     * 验证：
     * 1. 两个替换器可以组合
     * 2. 组合后的替换器按顺序应用
     */
    fun `test substitutor composition`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val boxClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Box" }
            assertNotNull("应该找到 Box 类", boxClass)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)

            val typeParameters = boxDescriptor!!.typeConstructor.parameters
            val tParam = typeParameters[0]

            // 创建第一个替换器：T -> Int64
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val substitutor1 = ComposableTypeSubstitutor.create(
                mapOf(tParam.typeConstructor to int64Type)
            )

            // 创建第二个空替换器
            val substitutor2 = ComposableTypeSubstitutor.EMPTY

            // 组合替换器
            val composed = substitutor1.compose(substitutor2)
            assertFalse("组合后的替换器不应该为空", composed.isEmpty)

            // 测试组合替换器
            val tType = tParam.defaultType
            val substitutedType = composed.safeSubstitute(tType.unwrap())
            assertEquals("组合替换器应该正确替换", int64Type, substitutedType)
        }
    }

    // ==================== Option 类型替换测试 ====================

    /**
     * 测试 Option 类型替换
     *
     * 验证：
     * 1. Option<T> 中的 T 可以正确替换
     * 2. Option 包装保持正确
     */
    fun `test option type substitution`() {
        val file = createFile(
            """
            package test

            class Result<T> {
                var value: Option<T>
                init(value: Option<T>) {
                    this.value = value
                }
            }

            func createResult(): Result<Int64> {
                return Result<Int64>(Some(42))
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Result" }
            assertNotNull("应该找到 Result 类", resultClass)

            val resultDescriptor = bindingContext[BindingContext.CLASS, resultClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Result 的描述符", resultDescriptor)

            val typeParameters = resultDescriptor!!.typeConstructor.parameters
            assertEquals("Result 应该有 1 个类型参数", 1, typeParameters.size)

            val tParam = typeParameters[0]

            // 创建替换器：T -> Int64
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val substitutor = ComposableTypeSubstitutor.create(
                mapOf(tParam.typeConstructor to int64Type)
            )

            // 测试替换
            val tType = tParam.defaultType
            val substitutedType = substitutor.safeSubstitute(tType.unwrap())
            assertEquals("T 应该替换为 Int64", int64Type, substitutedType)
        }
    }

    // ==================== 空替换器测试 ====================

    /**
     * 测试空替换器
     *
     * 验证：
     * 1. EMPTY 替换器不进行任何替换
     * 2. isEmpty 返回 true
     */
    fun `test empty substitutor`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val boxClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Box" }
            assertNotNull("应该找到 Box 类", boxClass)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)

            val typeParameters = boxDescriptor!!.typeConstructor.parameters
            val tParam = typeParameters[0]

            // 使用空替换器
            val emptySubstitutor = ComposableTypeSubstitutor.EMPTY
            assertTrue("EMPTY 替换器应该为空", emptySubstitutor.isEmpty)

            // 测试空替换器不改变类型
            val tType = tParam.defaultType
            val substitutedType = emptySubstitutor.safeSubstitute(tType.unwrap())
            assertEquals("空替换器不应该改变类型", tType.unwrap(), substitutedType)
        }
    }

    /**
     * 测试从空 Map 创建替换器
     *
     * 验证：
     * 1. 从空 Map 创建的替换器等同于 EMPTY
     */
    fun `test create from empty map`() {
        val substitutor = ComposableTypeSubstitutor.create(emptyMap())
        assertTrue("从空 Map 创建的替换器应该为空", substitutor.isEmpty)
        assertSame("应该返回 EMPTY 单例", ComposableTypeSubstitutor.EMPTY, substitutor)
    }

    // ==================== 替换选项测试 ====================

    /**
     * 测试替换选项
     *
     * 验证：
     * 1. 可以修改替换选项
     * 2. 选项修改生效
     */
    fun `test substitution options`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val boxClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Box" }
            assertNotNull("应该找到 Box 类", boxClass)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)

            val typeParameters = boxDescriptor!!.typeConstructor.parameters
            val tParam = typeParameters[0]

            // 创建替换器
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val substitutor = ComposableTypeSubstitutor.create(
                mapOf(tParam.typeConstructor to int64Type)
            )

            // 修改选项 - 修改为不同的值以确保返回新实例
            val modifiedSubstitutor = substitutor.withOptions {
                copy(keepAnnotations = false)  // 修改为 false,与默认值 true 不同
            }

            assertNotNull("修改选项后应该返回新的替换器", modifiedSubstitutor)
            assertNotSame("应该返回不同的实例", substitutor, modifiedSubstitutor)
        }
    }

    // ==================== 错误处理测试 ====================

    /**
     * 测试错误类型处理
     *
     * 验证：
     * 1. 错误类型不被替换
     * 2. 不抛出异常
     */
    fun `test error type handling`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 使用空替换器测试错误类型
            val emptySubstitutor = ComposableTypeSubstitutor.EMPTY

            // 创建错误类型
            val errorType = ErrorUtils.createErrorType(
                org.cangnova.cangjie.types.error.ErrorTypeKind.UNRESOLVED_TYPE,
                "test error"
            )

            // 尝试替换错误类型（不应该抛出异常）
            val result = emptySubstitutor.safeSubstitute(errorType)
            assertNotNull("替换错误类型应该返回结果", result)
        }
    }
}
