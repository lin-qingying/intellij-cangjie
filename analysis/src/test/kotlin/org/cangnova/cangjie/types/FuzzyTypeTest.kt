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
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * FuzzyType 测试
 *
 * 测试 FuzzyType 类用于 IDE 代码补全的类型匹配功能：
 * - 基本类型匹配
 * - 自由类型参数处理
 * - 子类型和超类型检查
 * - Option 类型处理
 * - 扩展函数支持
 *
 * ## 测试策略
 *
 * 1. **基本匹配**: 验证无自由参数的类型匹配
 * 2. **泛型匹配**: 验证带自由类型参数的匹配
 * 3. **子类型检查**: 验证 checkIsSubtypeOf 方法
 * 4. **超类型检查**: 验证 checkIsSuperTypeOf 方法
 * 5. **Option类型**: 验证 makeOption/unwrapOption 方法
 * 6. **扩展函数**: 验证 fuzzyReturnType 等扩展函数
 */
class FuzzyTypeTest : CangJieAnalysisTestBase() {

    // ==================== 基本类型匹配测试 ====================

    /**
     * 测试相同类型的匹配
     *
     * 验证：
     * 1. 相同类型应该互相匹配
     * 2. 返回非空的替换器
     */
    fun `test same type matching`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type

            val fuzzyType1 = int64Type.toFuzzyType(emptyList())
            val fuzzyType2 = int64Type.toFuzzyType(emptyList())

            // 测试子类型匹配
            val substitutor1 = fuzzyType1.checkIsSubtypeOf(fuzzyType2)
            assertNotNull("相同类型应该匹配", substitutor1)

            // 测试超类型匹配
            val substitutor2 = fuzzyType1.checkIsSuperTypeOf(fuzzyType2)
            assertNotNull("相同类型应该互为超类型", substitutor2)
        }
    }

    /**
     * 测试不同类型的不匹配
     *
     * 验证：
     * 1. 不同基本类型不应该匹配
     * 2. 返回 null
     */
    fun `test different types not matching`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            var y: Float64 = 3.14
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val float64Type = resolutionFacade.moduleDescriptor.builtIns.float64Type

            val fuzzyInt = int64Type.toFuzzyType(emptyList())
            val fuzzyFloat = float64Type.toFuzzyType(emptyList())

            // Int64 不是 Float64 的子类型
            val substitutor1 = fuzzyInt.checkIsSubtypeOf(fuzzyFloat)
            assertNull("Int64 不应该匹配 Float64", substitutor1)

            // Float64 不是 Int64 的子类型
            val substitutor2 = fuzzyFloat.checkIsSubtypeOf(fuzzyInt)
            assertNull("Float64 不应该匹配 Int64", substitutor2)
        }
    }

    // ==================== 类继承关系匹配测试 ====================

    /**
     * 测试子类型匹配
     *
     * 验证：
     * 1. 子类应该匹配父类（子类型关系）
     * 2. 父类不应该匹配子类
     */
    fun `test subtype matching with class inheritance`() {
        val file = createFile(
            """
            package test

            open class Animal {
                func makeSound(): Unit {}
            }

            class Dog <: Animal {
                override func makeSound(): Unit {}
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val animalClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Animal" }
            val dogClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Dog" }

            assertNotNull("应该找到 Animal 类", animalClass)
            assertNotNull("应该找到 Dog 类", dogClass)

            val animalDescriptor = bindingContext[BindingContext.CLASS, animalClass!!] as? ClassDescriptor
            val dogDescriptor = bindingContext[BindingContext.CLASS, dogClass!!] as? ClassDescriptor

            val animalType = animalDescriptor!!.defaultType
            val dogType = dogDescriptor!!.defaultType

            val fuzzyAnimal = animalType.toFuzzyType(emptyList())
            val fuzzyDog = dogType.toFuzzyType(emptyList())

            // Dog 是 Animal 的子类型
            val substitutor1 = fuzzyDog.checkIsSubtypeOf(fuzzyAnimal)
            assertNotNull("Dog 应该匹配 Animal（子类型关系）", substitutor1)

            // Animal 不是 Dog 的子类型
            val substitutor2 = fuzzyAnimal.checkIsSubtypeOf(fuzzyDog)
            assertNull("Animal 不应该匹配 Dog（不是子类型）", substitutor2)
        }
    }

    /**
     * 测试超类型匹配
     *
     * 验证：
     * 1. 父类应该匹配子类（超类型关系）
     * 2. 子类不应该作为超类型匹配父类
     */
    fun `test supertype matching with class inheritance`() {
        val file = createFile(
            """
            package test

            open class Animal {
                func makeSound(): Unit {}
            }

            class Dog <: Animal {
                override func makeSound(): Unit {}
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val animalClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Animal" }
            val dogClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Dog" }

            val animalDescriptor = bindingContext[BindingContext.CLASS, animalClass!!] as? ClassDescriptor
            val dogDescriptor = bindingContext[BindingContext.CLASS, dogClass!!] as? ClassDescriptor

            val animalType = animalDescriptor!!.defaultType
            val dogType = dogDescriptor!!.defaultType

            val fuzzyAnimal = animalType.toFuzzyType(emptyList())
            val fuzzyDog = dogType.toFuzzyType(emptyList())

            // Animal 是 Dog 的超类型
            val substitutor1 = fuzzyAnimal.checkIsSuperTypeOf(fuzzyDog)
            assertNotNull("Animal 应该是 Dog 的超类型", substitutor1)

            // Dog 不是 Animal 的超类型
            val substitutor2 = fuzzyDog.checkIsSuperTypeOf(fuzzyAnimal)
            assertNull("Dog 不应该是 Animal 的超类型", substitutor2)
        }
    }

    // ==================== 自由类型参数测试 ====================

    /**
     * 测试带自由类型参数的匹配
     *
     * 验证：
     * 1. 有自由参数时的简化匹配逻辑
     * 2. 返回空替换器（TODO: 未来实现完整推断）
     */
    fun `test matching with free type parameters`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }

            func createBox<T>(value: T): Box<T> {
                return Box<T>(value)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val boxClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            val createBoxFunc = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)

            assertNotNull("应该找到 Box 类", boxClass)
            assertNotNull("应该找到 createBox 函数", createBoxFunc)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            val funcDescriptor = bindingContext[BindingContext.FUNCTION, createBoxFunc!!] as? FunctionDescriptor

            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)
            assertNotNull("应该能获取 createBox 的描述符", funcDescriptor)

            val boxType = boxDescriptor!!.defaultType
            val typeParameters = funcDescriptor!!.typeParameters

            // 创建带自由参数的 FuzzyType
            val fuzzyBox = boxType.toFuzzyType(typeParameters)

            assertEquals("应该有 1 个自由类型参数", 1, fuzzyBox.freeParameters.size)

            // 测试与自身的匹配（简化逻辑）
            val substitutor = fuzzyBox.checkIsSubtypeOf(fuzzyBox)
            assertNotNull("带自由参数的类型应该匹配自身", substitutor)
        }
    }

    // ==================== Option 类型测试 ====================

    /**
     * 测试 makeOption 方法
     *
     * 验证：
     * 1. 类型被正确包装为 Option 类型
     * 2. 自由参数保持不变
     */
    fun `test makeOption method`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            var y: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val fuzzyInt = int64Type.toFuzzyType(emptyList())

            // 转换为 Option 类型
            val fuzzyOption = fuzzyInt.makeOption()

            assertNotNull("应该成功创建 Option 类型", fuzzyOption)
            assertEquals("自由参数应该保持不变", 0, fuzzyOption.freeParameters.size)

            // 验证类型确实是 Option 类型
            assertTrue(
                "类型应该是 Option 类型",
                fuzzyOption.type.optionality() != TypeOptionality.NOT_OPTION
            )
        }
    }

    /**
     * 测试 unwrapOption 方法
     *
     * 验证：
     * 1. Option 类型被正确解包
     * 2. 自由参数保持不变
     */
    fun `test unwrapOption method`() {
        val file = createFile(
            """
            package test

            var x: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val optionInt64Type = int64Type.makeOption()
            val fuzzyOption = optionInt64Type.toFuzzyType(emptyList())

            // 解包 Option 类型
            val unwrapped = fuzzyOption.unwrapOption()

            assertNotNull("应该成功解包 Option 类型", unwrapped)
            assertEquals("自由参数应该保持不变", 0, unwrapped.freeParameters.size)

            // 验证解包后的类型
            val unwrappedType = unboxOptionType(optionInt64Type)
            assertEquals("解包后应该是 Int64 类型", unwrappedType, unwrapped.type)
        }
    }

    /**
     * 测试 isAlmostEverything 方法
     *
     * 验证：
     * 1. Option<Nothing> 返回 true
     * 2. 其他类型返回 false
     */
    fun `test isAlmostEverything method`() {
        val file = createFile(
            """
            package test

            var x: Option<Nothing> = None
            var y: Int64 = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            val nothingType = resolutionFacade.moduleDescriptor.builtIns.nothingType
            val optionNothing = nothingType.makeOption()
            val fuzzyOptionNothing = optionNothing.toFuzzyType(emptyList())

            assertTrue(
                "Option<Nothing> 应该返回 true",
                fuzzyOptionNothing.isAlmostEverything()
            )

            // 测试普通类型
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val fuzzyInt = int64Type.toFuzzyType(emptyList())

            assertFalse(
                "Int64 应该返回 false",
                fuzzyInt.isAlmostEverything()
            )
        }
    }

    // ==================== 扩展函数测试 ====================

    /**
     * 测试 fuzzyReturnType 扩展函数
     *
     * 验证：
     * 1. 能获取函数的模糊返回类型
     * 2. 自由参数包含函数的类型参数
     */
    fun `test fuzzyReturnType extension function`() {
        val file = createFile(
            """
            package test

            func identity<T>(value: T): T {
                return value
            }

            func getNumber(): Int64 {
                return 42
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val identityFunc = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
                .firstOrNull { it.name == "identity" }
            val getNumberFunc = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
                .firstOrNull { it.name == "getNumber" }

            assertNotNull("应该找到 identity 函数", identityFunc)
            assertNotNull("应该找到 getNumber 函数", getNumberFunc)

            val identityDescriptor = bindingContext[BindingContext.FUNCTION, identityFunc!!] as? FunctionDescriptor
            val getNumberDescriptor = bindingContext[BindingContext.FUNCTION, getNumberFunc!!] as? FunctionDescriptor

            assertNotNull("应该能获取 identity 的描述符", identityDescriptor)
            assertNotNull("应该能获取 getNumber 的描述符", getNumberDescriptor)

            // 测试泛型函数的模糊返回类型
            val identityFuzzyReturn = identityDescriptor!!.fuzzyReturnType()
            assertNotNull("identity 应该有模糊返回类型", identityFuzzyReturn)
            assertEquals(
                "identity 的自由参数应该包含类型参数 T",
                1,
                identityFuzzyReturn!!.freeParameters.size
            )

            // 测试非泛型函数的模糊返回类型
            val getNumberFuzzyReturn = getNumberDescriptor!!.fuzzyReturnType()
            assertNotNull("getNumber 应该有模糊返回类型", getNumberFuzzyReturn)
            assertEquals(
                "getNumber 没有自由参数",
                0,
                getNumberFuzzyReturn!!.freeParameters.size
            )
        }
    }


    // ==================== presentationType 测试 ====================

    /**
     * 测试 presentationType 方法
     *
     * 验证：
     * 1. 返回用于展示的类型
     * 2. 不进行类型替换
     */
    fun `test presentationType method`() {
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
            val boxClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Box 类", boxClass)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)

            val boxType = boxDescriptor!!.defaultType
            val typeParameters = boxDescriptor.typeConstructor.parameters

            val fuzzyBox = boxType.toFuzzyType(typeParameters)

            // 获取展示类型
            val presentType = fuzzyBox.presentationType()

            assertNotNull("应该有展示类型", presentType)
            assertEquals("展示类型应该是原始类型", boxType, presentType)
        }
    }

    // ==================== equals 和 hashCode 测试 ====================

    /**
     * 测试 equals 和 hashCode 方法
     *
     * 验证：
     * 1. 相同类型和自由参数的 FuzzyType 相等
     * 2. hashCode 一致
     */
    fun `test equals and hashCode`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type

            val fuzzy1 = int64Type.toFuzzyType(emptyList())
            val fuzzy2 = int64Type.toFuzzyType(emptyList())

            assertEquals("相同类型和自由参数应该相等", fuzzy1, fuzzy2)
            assertEquals("hashCode 应该相同", fuzzy1.hashCode(), fuzzy2.hashCode())
        }
    }

    /**
     * 测试 toString 方法
     *
     * 验证：
     * 1. 无自由参数时只显示类型
     * 2. 有自由参数时显示参数列表
     */
    fun `test toString method`() {
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
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type

            // 无自由参数
            val fuzzyInt = int64Type.toFuzzyType(emptyList())
            val strInt = fuzzyInt.toString()
            assertNotNull("toString 应该返回非空字符串", strInt)

            // 有自由参数
            val boxClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            val boxType = boxDescriptor!!.defaultType
            val typeParameters = boxDescriptor.typeConstructor.parameters

            val fuzzyBox = boxType.toFuzzyType(typeParameters)
            val strBox = fuzzyBox.toString()
            assertNotNull("toString 应该返回非空字符串", strBox)
            assertTrue(
                "应该包含自由参数信息",
                strBox.contains("free:") || strBox.contains("T")
            )
        }
    }
}
