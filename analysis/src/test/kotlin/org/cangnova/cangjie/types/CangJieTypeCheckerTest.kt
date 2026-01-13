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
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

/**
 * CangJieTypeChecker 测试
 *
 * 测试仓颉类型检查系统：
 * - 子类型关系检查
 * - Option 类型兼容性
 * - 类继承关系
 * - 接口实现关系
 * - 泛型类型兼容性
 *
 * ## 测试策略
 *
 * 1. **基本类型**: 验证内置类型的子类型关系
 * 2. **类继承**: 验证类继承链的子类型关系
 * 3. **接口实现**: 验证接口实现的子类型关系
 * 4. **Option类型**: 验证Option类型的兼容性规则
 * 5. **泛型类型**: 验证泛型类型的子类型关系
 */
class CangJieTypeCheckerTest : CangJieAnalysisTestBase() {

    // ==================== 基本类型测试 ====================

    /**
     * 测试相同类型的子类型关系
     *
     * 验证：
     * 1. 相同类型是自身的子类型
     */
    fun `test same type is subtype of itself`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Int64 应该是自身的子类型",
                typeChecker.isSubtypeOf(int64Type, int64Type)
            )
        }
    }

    /**
     * 测试不同基本类型的非子类型关系
     *
     * 验证：
     * 1. Int64 不是 Float64 的子类型
     * 2. Bool 不是 Int64 的子类型
     */
    fun `test different basic types are not subtypes`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            var y: Float64 = 3.14
            var z: Bool = true
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val float64Type = resolutionFacade.moduleDescriptor.builtIns.float64Type
            val boolType = resolutionFacade.moduleDescriptor.builtIns.boolType
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertFalse(
                "Int64 不应该是 Float64 的子类型",
                typeChecker.isSubtypeOf(int64Type, float64Type)
            )

            assertFalse(
                "Bool 不应该是 Int64 的子类型",
                typeChecker.isSubtypeOf(boolType, int64Type)
            )
        }
    }

    // ==================== 类继承关系测试 ====================

    /**
     * 测试类继承的子类型关系
     *
     * 验证：
     * 1. 子类是父类的子类型
     * 2. 父类不是子类的子类型
     */
    fun `test class inheritance subtype relationship`() {
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

            assertNotNull("应该能获取 Animal 的描述符", animalDescriptor)
            assertNotNull("应该能获取 Dog 的描述符", dogDescriptor)

            val animalType = animalDescriptor!!.defaultType
            val dogType = dogDescriptor!!.defaultType
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Dog 应该是 Animal 的子类型",
                typeChecker.isSubtypeOf(dogType, animalType)
            )

            assertFalse(
                "Animal 不应该是 Dog 的子类型",
                typeChecker.isSubtypeOf(animalType, dogType)
            )
        }
    }

    /**
     * 测试多级继承的子类型关系
     *
     * 验证：
     * 1. 孙类是祖父类的子类型（传递性）
     */
    fun `test multi-level inheritance subtype relationship`() {
        val file = createFile(
            """
            package test

            open class Animal {
                func makeSound(): Unit {}
            }

            open class Mammal <: Animal {
                func feedMilk(): Unit {}
            }

            class Dog <: Mammal {
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

            assertNotNull("应该能获取 Animal 的描述符", animalDescriptor)
            assertNotNull("应该能获取 Dog 的描述符", dogDescriptor)

            val animalType = animalDescriptor!!.defaultType
            val dogType = dogDescriptor!!.defaultType
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Dog 应该是 Animal 的子类型（通过传递性）",
                typeChecker.isSubtypeOf(dogType, animalType)
            )
        }
    }

    // ==================== 接口实现关系测试 ====================

    /**
     * 测试接口实现的子类型关系
     *
     * 验证：
     * 1. 实现类是接口的子类型
     * 2. 接口不是实现类的子类型
     */
    fun `test interface implementation subtype relationship`() {
        val file = createFile(
            """
            package test

            interface Drawable {
                func draw(): Unit
            }

            class Circle <: Drawable {
                public func draw(): Unit {}
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val drawableInterface = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            val circleClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)

            assertNotNull("应该找到 Drawable 接口", drawableInterface)
            assertNotNull("应该找到 Circle 类", circleClass)

            val drawableDescriptor = bindingContext[BindingContext.CLASS, drawableInterface!!] as? ClassDescriptor
            val circleDescriptor = bindingContext[BindingContext.CLASS, circleClass!!] as? ClassDescriptor

            assertNotNull("应该能获取 Drawable 的描述符", drawableDescriptor)
            assertNotNull("应该能获取 Circle 的描述符", circleDescriptor)

            val drawableType = drawableDescriptor!!.defaultType
            val circleType = circleDescriptor!!.defaultType
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Circle 应该是 Drawable 的子类型",
                typeChecker.isSubtypeOf(circleType, drawableType)
            )

            assertFalse(
                "Drawable 不应该是 Circle 的子类型",
                typeChecker.isSubtypeOf(drawableType, circleType)
            )
        }
    }

    /**
     * 测试多接口实现的子类型关系
     *
     * 验证：
     * 1. 实现类是所有实现接口的子类型
     */
    fun `test multiple interface implementation subtype relationship`() {
        val file = createFile(
            """
            package test

            interface Drawable {
                func draw(): Unit
            }

            interface Clickable {
                func onClick(): Unit
            }

            class Button <: Drawable, Clickable {
                public func draw(): Unit {}
                public func onClick(): Unit {}
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaces = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
            val buttonClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)

            val drawableInterface = interfaces.firstOrNull { it.name == "Drawable" }
            val clickableInterface = interfaces.firstOrNull { it.name == "Clickable" }

            assertNotNull("应该找到 Drawable 接口", drawableInterface)
            assertNotNull("应该找到 Clickable 接口", clickableInterface)
            assertNotNull("应该找到 Button 类", buttonClass)

            val drawableDescriptor = bindingContext[BindingContext.CLASS, drawableInterface!!] as? ClassDescriptor
            val clickableDescriptor = bindingContext[BindingContext.CLASS, clickableInterface!!] as? ClassDescriptor
            val buttonDescriptor = bindingContext[BindingContext.CLASS, buttonClass!!] as? ClassDescriptor

            val drawableType = drawableDescriptor!!.defaultType
            val clickableType = clickableDescriptor!!.defaultType
            val buttonType = buttonDescriptor!!.defaultType
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Button 应该是 Drawable 的子类型",
                typeChecker.isSubtypeOf(buttonType, drawableType)
            )

            assertTrue(
                "Button 应该是 Clickable 的子类型",
                typeChecker.isSubtypeOf(buttonType, clickableType)
            )
        }
    }

    // ==================== Option 类型测试 ====================

    /**
     * 测试 Option 类型的子类型关系
     *
     * 验证：
     * 1. T 是 Option<T> 的子类型（自动装箱）
     * 2. Option<T> 不是 T 的子类型
     */
    fun `test option type subtype relationship`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            var y: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val optionInt64Type = int64Type.makeOption()
            val typeChecker = CangJieTypeChecker.DEFAULT

            // 注意：在仓颉语言中，T 可以自动转换为 Option<T>（通过 Some）
            // 但这不是严格的子类型关系，而是隐式转换
            // 这里测试类型系统的行为

            assertFalse(
                "Option<Int64> 不应该是 Int64 的子类型",
                typeChecker.isSubtypeOf(optionInt64Type, int64Type)
            )
        }
    }

    /**
     * 测试 Option 类型的嵌套
     *
     * 验证：
     * 1. Option<Option<T>> 和 Option<T> 是不同类型
     */
    fun `test nested option type`() {
        val file = createFile(
            """
            package test

            var x: Option<Int64> = Some(42)
            var y: Option<Option<Int64>> = Some(Some(42))
            """.trimIndent()
        )

        analyzeForTest(file) {
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val optionInt64Type = int64Type.makeOption()
            val optionOptionInt64Type = optionInt64Type.makeOption()
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertFalse(
                "Option<Option<Int64>> 不应该是 Option<Int64> 的子类型",
                typeChecker.isSubtypeOf(optionOptionInt64Type, optionInt64Type)
            )

            assertFalse(
                "Option<Int64> 不应该是 Option<Option<Int64>> 的子类型",
                typeChecker.isSubtypeOf(optionInt64Type, optionOptionInt64Type)
            )
        }
    }

    // ==================== 泛型类型测试 ====================

    /**
     * 测试泛型类型的子类型关系
     *
     * 验证：
     * 1. 泛型类型参数相同时才是子类型
     * 2. 仓颉语言不支持型变（协变/逆变）
     */
    fun `test generic type subtype relationship`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }

            var intBox: Box<Int64> = Box<Int64>(42)
            var floatBox: Box<Float64> = Box<Float64>(3.14)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val boxClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Box 类", boxClass)

            val boxDescriptor = bindingContext[BindingContext.CLASS, boxClass!!] as? ClassDescriptor
            assertNotNull("应该能获取 Box 的描述符", boxDescriptor)

            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val float64Type = resolutionFacade.moduleDescriptor.builtIns.float64Type

            val boxInt64Type = CangJieTypeFactory.simpleType(
                boxDescriptor!!.defaultType,
                arguments = listOf(TypeArgumentImpl(int64Type))
            )

            val boxFloat64Type = CangJieTypeFactory.simpleType(
                boxDescriptor.defaultType,
                arguments = listOf(TypeArgumentImpl(float64Type))
            )

            val typeChecker = CangJieTypeChecker.DEFAULT

            // 仓颉语言不支持型变，Box<Int64> 和 Box<Float64> 是不同类型
            assertFalse(
                "Box<Int64> 不应该是 Box<Float64> 的子类型",
                typeChecker.isSubtypeOf(boxInt64Type, boxFloat64Type)
            )

            assertTrue(
                "Box<Int64> 应该是自身的子类型",
                typeChecker.isSubtypeOf(boxInt64Type, boxInt64Type)
            )
        }
    }

    // ==================== Nothing 和 Unit 类型测试 ====================

    /**
     * 测试 Nothing 类型
     *
     * 验证：
     * 1. Nothing 是所有类型的子类型（底类型）
     */
    fun `test nothing type is subtype of all types`() {
        val file = createFile(
            """
            package test

            func alwaysThrow(): Nothing {
                throw Exception("error")
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val nothingType = resolutionFacade.moduleDescriptor.builtIns.nothingType
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val stringType = resolutionFacade.moduleDescriptor.builtIns.stringType
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Nothing 应该是 Int64 的子类型",
                typeChecker.isSubtypeOf(nothingType, int64Type)
            )

            assertTrue(
                "Nothing 应该是 String 的子类型",
                typeChecker.isSubtypeOf(nothingType, stringType)
            )
        }
    }

    /**
     * 测试 Unit 类型
     *
     * 验证：
     * 1. Unit 只是自身的子类型
     * 2. Unit 不是其他类型的子类型
     */
    fun `test unit type subtype relationship`() {
        val file = createFile(
            """
            package test

            func doNothing(): Unit {
                // do nothing
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val unitType = resolutionFacade.moduleDescriptor.builtIns.unitType
            val int64Type = resolutionFacade.moduleDescriptor.builtIns.int64Type
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "Unit 应该是自身的子类型",
                typeChecker.isSubtypeOf(unitType, unitType)
            )

            assertFalse(
                "Unit 不应该是 Int64 的子类型",
                typeChecker.isSubtypeOf(unitType, int64Type)
            )

            assertFalse(
                "Int64 不应该是 Unit 的子类型",
                typeChecker.isSubtypeOf(int64Type, unitType)
            )
        }
    }

    // ==================== 类型等价测试 ====================

    /**
     * 测试类型等价性
     *
     * 验证：
     * 1. 相同类型是等价的
     * 2. 不同类型不等价
     */
    fun `test type equivalence`() {
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
            val typeChecker = CangJieTypeChecker.DEFAULT

            assertTrue(
                "相同类型应该互为子类型（等价）",
                typeChecker.equalTypes(int64Type, int64Type)
            )

            assertFalse(
                "不同类型不应该等价",
                typeChecker.equalTypes(int64Type, float64Type)
            )
        }
    }
}
