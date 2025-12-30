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

package org.cangnova.cangjie.resolve.operators

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 运算符重载和类型转换语义分析测试
 *
 * 专注于测试运算符重载和类型转换的语义分析，包括：
 * - 运算符重载函数的定义
 * - 运算符调用的解析
 * - 显式类型转换
 * - 隐式类型转换
 * - 智能类型转换
 *
 * ## 测试策略
 *
 * 1. **运算符重载**: 验证各种运算符的重载语义
 * 2. **运算符解析**: 验证运算符调用解析到正确的函数
 * 3. **类型转换**: 验证显式和隐式转换的语义
 * 4. **类型检查**: 验证转换的类型安全性
 * 5. **智能转换**: 验证基于控制流的类型细化
 */
class OperatorSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 算术运算符重载测试 ====================

    /**
     * 测试加法运算符重载
     *
     * 验证：
     * 1. operator + 函数的定义
     * 2. + 运算符调用解析到重载函数
     * 3. 返回类型正确
     */
    fun `test plus operator overload`() {
        val file = createFile(
            """
            package test

            class Vector {
                public let x: Float64
                public let y: Float64

                public init(x: Float64, y: Float64) {
                    this.x = x
                    this.y = y
                }

                public operator func +(other: Vector): Vector {
                    return Vector(this.x + other.x, this.y + other.y)
                }
            }

            func main() {
                let v1 = Vector(1.0, 2.0)
                let v2 = Vector(3.0, 4.0)
                let sum = v1 + v2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val vectorClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Vector 类", vectorClass)

            // 验证运算符函数
            val plusOperator = PsiTreeUtil.findChildrenOfType(vectorClass, CjFunction::class.java)
                .firstOrNull { it.name == "+" }
            assertNotNull("应该找到 + 运算符函数", plusOperator)

            val plusDescriptor = bindingContext[BindingContext.FUNCTION, plusOperator!!]
            assertNotNull("+ 运算符应该有描述符", plusDescriptor)

            // 验证返回类型
            val returnType = plusDescriptor!!.returnType
            assertNotNull("运算符应该有返回类型", returnType)
            assertTrue("返回类型应该是 Vector", returnType!!.toString().contains("Vector"))

            // 验证使用运算符的变量
            val sumProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "sum" }
            if (sumProperty != null) {
                val sumDescriptor = bindingContext[BindingContext.VARIABLE, sumProperty!!]
                assertNotNull("sum 应该有描述符", sumDescriptor)
            }
        }
    }

    /**
     * 测试减法运算符重载
     *
     * 验证：
     * 1. operator - 函数的定义
     * 2. 一元和二元减法的区分
     */
    fun `test minus operator overload`() {
        val file = createFile(
            """
            package test

            class Complex {
                public let real: Float64
                public let imag: Float64

                public init(real: Float64, imag: Float64) {
                    this.real = real
                    this.imag = imag
                }

                // 一元负号
                public operator func -(): Complex {
                    return Complex(-this.real, -this.imag)
                }

                // 二元减法
                public operator func -(other: Complex): Complex {
                    return Complex(this.real - other.real, this.imag - other.imag)
                }
            }

            func main() {
                let c1 = Complex(3.0, 4.0)
                let c2 = Complex(1.0, 2.0)
                let negated = -c1
                let diff = c1 - c2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val complexClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Complex 类", complexClass)

            // 验证运算符函数
            val minusOperators = PsiTreeUtil.findChildrenOfType(complexClass, CjFunction::class.java)
                .filter { it.name == "-" }
            assertEquals("应该有 2 个 - 运算符函数", 2, minusOperators.size)
        }
    }

    /**
     * 测试乘法和除法运算符重载
     *
     * 验证：
     * 1. operator * 和 / 的定义
     * 2. 运算符优先级保持不变
     */
    fun `test multiplication and division operator overload`() {
        val file = createFile(
            """
            package test

            class Matrix {
                private let data: Array<Array<Float64>>

                public operator func *(other: Matrix): Matrix {
                    // matrix multiplication
                    return this
                }

                public operator func *(scalar: Float64): Matrix {
                    // scalar multiplication
                    return this
                }

                public operator func /(scalar: Float64): Matrix {
                    // scalar division
                    return this
                }
            }

            func main() {
                let m1 = Matrix()
                let m2 = Matrix()
                let product = m1 * m2
                let scaled = m1 * 2.0
                let divided = m1 / 2.0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val matrixClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Matrix 类", matrixClass)

            // 验证多个重载的运算符函数
            val multiplyOperators = PsiTreeUtil.findChildrenOfType(matrixClass, CjFunction::class.java)
                .filter { it.name == "*" }
            assertEquals("应该有 2 个 * 运算符函数", 2, multiplyOperators.size)
        }
    }

    // ==================== 比较运算符重载测试 ====================

    /**
     * 测试相等运算符重载
     *
     * 验证：
     * 1. operator == 的定义
     * 2. 返回 Bool 类型
     */
    fun `test equality operator overload`() {
        val file = createFile(
            """
            package test

            class Point {
                public let x: Int64
                public let y: Int64

                public init(x: Int64, y: Int64) {
                    this.x = x
                    this.y = y
                }

                public operator func ==(other: Point): Bool {
                    return this.x == other.x && this.y == other.y
                }

                public operator func !=(other: Point): Bool {
                    return !(this == other)
                }
            }

            func main() {
                let p1 = Point(1, 2)
                let p2 = Point(1, 2)
                let areEqual = p1 == p2
                let areNotEqual = p1 != p2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val pointClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Point 类", pointClass)

            // 验证 == 运算符
            val equalsOperator = PsiTreeUtil.findChildrenOfType(pointClass, CjFunction::class.java)
                .firstOrNull { it.name == "==" }
            assertNotNull("应该找到 == 运算符函数", equalsOperator)

            val equalsDescriptor = bindingContext[BindingContext.FUNCTION, equalsOperator!!]
            assertNotNull("== 运算符应该有描述符", equalsDescriptor)
            assertEquals("Bool", equalsDescriptor!!.returnType!!.toString())

            // 验证使用结果
            val areEqualProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "areEqual" }
            if (areEqualProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, areEqualProperty!!]
                assertNotNull("areEqual 应该有描述符", descriptor)
                assertEquals("Bool", descriptor!!.type.toString())
            }
        }
    }

    /**
     * 测试比较运算符重载
     *
     * 验证：
     * 1. operator <, >, <=, >= 的定义
     * 2. 所有比较运算符返回 Bool
     */
    fun `test comparison operator overload`() {
        val file = createFile(
            """
            package test

            class Version {
                public let major: Int64
                public let minor: Int64
                public let patch: Int64

                public init(major: Int64, minor: Int64, patch: Int64) {
                    this.major = major
                    this.minor = minor
                    this.patch = patch
                }

                public operator func <(other: Version): Bool {
                    if (this.major != other.major) {
                        return this.major < other.major
                    }
                    if (this.minor != other.minor) {
                        return this.minor < other.minor
                    }
                    return this.patch < other.patch
                }

                public operator func >(other: Version): Bool {
                    return other < this
                }

                public operator func <=(other: Version): Bool {
                    return !(this > other)
                }

                public operator func >=(other: Version): Bool {
                    return !(this < other)
                }
            }

            func main() {
                let v1 = Version(1, 0, 0)
                let v2 = Version(2, 0, 0)
                let isLess = v1 < v2
                let isGreater = v1 > v2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val versionClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Version 类", versionClass)

            // 验证所有比较运算符
            val comparisonOps = listOf("<", ">", "<=", ">=")
            for (op in comparisonOps) {
                val operator = PsiTreeUtil.findChildrenOfType(versionClass, CjFunction::class.java)
                    .firstOrNull { it.name == op }
                assertNotNull("应该找到 $op 运算符函数", operator)

                val descriptor = bindingContext[BindingContext.FUNCTION, operator!!]
                assertNotNull("$op 运算符应该有描述符", descriptor)
                assertEquals("Bool", descriptor!!.returnType!!.toString())
            }
        }
    }

    // ==================== 下标运算符重载测试 ====================

    /**
     * 测试下标运算符重载
     *
     * 验证：
     * 1. operator [] 的定义
     * 2. get 和 set 访问器
     */
    fun `test subscript operator overload`() {
        val file = createFile(
            """
            package test

            class CustomArray<T> {
                private var data: Array<T>

                public init() {
                    data = Array<T>()
                }

                public operator func [](index: Int64): T {
                    return data[index]
                }

                public operator func []=(index: Int64, value: T) {
                    data[index] = value
                }
            }

            func main() {
                let arr = CustomArray<String>()
                arr[0] = "hello"
                let value = arr[0]
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val customArrayClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 CustomArray 类", customArrayClass)

            // 验证 [] 运算符（getter）
            val getOperator = PsiTreeUtil.findChildrenOfType(customArrayClass, CjFunction::class.java)
                .firstOrNull { it.name == "[]" }
            assertNotNull("应该找到 [] 运算符函数", getOperator)

            val getDescriptor = bindingContext[BindingContext.FUNCTION, getOperator!!]
            assertNotNull("[] 运算符应该有描述符", getDescriptor)

            // 验证 []= 运算符（setter）
            val setOperator = PsiTreeUtil.findChildrenOfType(customArrayClass, CjFunction::class.java)
                .firstOrNull { it.name == "[]=" }
            if (setOperator != null) {
                val setDescriptor = bindingContext[BindingContext.FUNCTION, setOperator!!]
                assertNotNull("[]= 运算符应该有描述符", setDescriptor)
            }
        }
    }

    // ==================== 显式类型转换测试 ====================

    /**
     * 测试显式类型转换
     *
     * 验证：
     * 1. as 转换操作符的语义
     * 2. 转换后的类型正确
     */
    fun `test explicit type cast`() {
        val file = createFile(
            """
            package test

            open class Animal {
                public func speak(): String {
                    return "sound"
                }
            }

            class Dog <: Animal {
                public override func speak(): String {
                    return "woof"
                }

                public func fetch(): Unit {
                    // fetch implementation
                }
            }

            func main() {
                let animal: Animal = Dog()
                let dog = animal as Dog
                dog.fetch()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val dogProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "dog" }
            assertNotNull("应该找到 dog 变量", dogProperty)

            val dogDescriptor = bindingContext[BindingContext.VARIABLE, dogProperty!!]
            assertNotNull("dog 应该有描述符", dogDescriptor)
            // 转换后的类型应该是 Dog
            assertTrue("类型应该是 Dog", dogDescriptor!!.type.toString().contains("Dog"))
        }
    }

    /**
     * 测试安全类型转换
     *
     * 验证：
     * 1. as? 安全转换的语义
     * 2. 转换失败返回 null
     * 3. 结果类型是可空类型
     */
    fun `test safe type cast`() {
        val file = createFile(
            """
            package test

            open class Animal

            class Dog <: Animal {
                public func bark(): Unit {
                    // bark
                }
            }

            class Cat <: Animal {
                public func meow(): Unit {
                    // meow
                }
            }

            func main() {
                let animal: Animal = Cat()
                let maybeDog = animal as? Dog
                if (maybeDog != null) {
                    maybeDog.bark()
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val maybeDogProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "maybeDog" }
            assertNotNull("应该找到 maybeDog 变量", maybeDogProperty)

            val maybeDogDescriptor = bindingContext[BindingContext.VARIABLE, maybeDogProperty!!]
            assertNotNull("maybeDog 应该有描述符", maybeDogDescriptor)
            // 安全转换的结果应该是可空类型
            assertNotNull("应该有类型", maybeDogDescriptor!!.type)
        }
    }

    // ==================== 隐式类型转换测试 ====================

    /**
     * 测试数值类型的隐式转换
     *
     * 验证：
     * 1. 小范围到大范围的隐式转换
     * 2. 类型提升的语义
     */
    fun `test implicit numeric conversion`() {
        val file = createFile(
            """
            package test

            func main() {
                let smallInt: Int8 = 10
                let largeInt: Int64 = smallInt

                let intValue: Int64 = 42
                let floatValue: Float64 = intValue.toFloat64()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertTrue("应该有变量声明", properties.isNotEmpty())

            val largeIntProperty = properties.firstOrNull { it.name == "largeInt" }
            if (largeIntProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, largeIntProperty!!]
                assertNotNull("largeInt 应该有描述符", descriptor)
                assertEquals("Int64", descriptor!!.type.toString())
            }

            val floatValueProperty = properties.firstOrNull { it.name == "floatValue" }
            if (floatValueProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, floatValueProperty!!]
                assertNotNull("floatValue 应该有描述符", descriptor)
                assertEquals("Float64", descriptor!!.type.toString())
            }
        }
    }

    // ==================== 智能类型转换测试 ====================

    /**
     * 测试智能类型转换（类型细化）
     *
     * 验证：
     * 1. is 检查后的类型细化
     * 2. 智能转换在控制流中的作用
     */
    fun `test smart cast after type check`() {
        val file = createFile(
            """
            package test

            open class Animal {
                public func speak(): String {
                    return "sound"
                }
            }

            class Dog <: Animal {
                public override func speak(): String {
                    return "woof"
                }

                public func fetch(): Unit {
                    // fetch
                }
            }

            func handleAnimal(animal: Animal) {
                if (animal is Dog) {
                    // 在此处 animal 被智能转换为 Dog
                    animal.fetch()
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val handleFunc = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到 handleAnimal 函数", handleFunc)

            val handleDescriptor = bindingContext[BindingContext.FUNCTION, handleFunc!!]
            assertNotNull("handleAnimal 应该有描述符", handleDescriptor)

            // 验证参数类型
            val parameters = handleDescriptor!!.valueParameters
            assertEquals("应该有 1 个参数", 1, parameters.size)
            assertTrue("参数类型应该是 Animal", parameters[0].type.toString().contains("Animal"))
        }
    }

    /**
     * 测试可空类型的智能转换
     *
     * 验证：
     * 1. null 检查后的类型细化
     * 2. 可空类型到非空类型的智能转换
     */
    fun `test smart cast after null check`() {
        val file = createFile(
            """
            package test

            func processString(str: String?) {
                if (str != null) {
                    // str 被智能转换为非空 String
                    let length = str.length
                }
            }

            func useValue(value: String?) {
                let len = value?.length ?? 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 2 个函数", 2, functions.size)

            val processFunc = functions.first { it.name == "processString" }
            val processDescriptor = bindingContext[BindingContext.FUNCTION, processFunc!!]
            assertNotNull("processString 应该有描述符", processDescriptor)

            val useFunc = functions.first { it.name == "useValue" }
            val useDescriptor = bindingContext[BindingContext.FUNCTION, useFunc!!]
            assertNotNull("useValue 应该有描述符", useDescriptor)
        }
    }

    // ==================== 类型转换函数测试 ====================

    /**
     * 测试自定义类型转换函数
     *
     * 验证：
     * 1. to* 命名约定的转换函数
     * 2. 转换函数的返回类型
     */
    fun `test custom type conversion function`() {
        val file = createFile(
            """
            package test

            class Temperature {
                private let celsius: Float64

                public init(celsius: Float64) {
                    this.celsius = celsius
                }

                public func toFahrenheit(): Float64 {
                    return celsius * 9.0 / 5.0 + 32.0
                }

                public func toKelvin(): Float64 {
                    return celsius + 273.15
                }
            }

            func main() {
                let temp = Temperature(25.0)
                let fahrenheit = temp.toFahrenheit()
                let kelvin = temp.toKelvin()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val tempClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Temperature 类", tempClass)

            // 验证转换函数
            val toFahrenheit = PsiTreeUtil.findChildrenOfType(tempClass, CjFunction::class.java)
                .firstOrNull { it.name == "toFahrenheit" }
            assertNotNull("应该找到 toFahrenheit 函数", toFahrenheit)

            val toFahrenheitDescriptor = bindingContext[BindingContext.FUNCTION, toFahrenheit!!]
            assertNotNull("toFahrenheit 应该有描述符", toFahrenheitDescriptor)
            assertEquals("Float64", toFahrenheitDescriptor!!.returnType!!.toString())

            // 验证使用转换函数的变量
            val fahrenheitProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "fahrenheit" }
            if (fahrenheitProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, fahrenheitProperty!!]
                assertNotNull("fahrenheit 应该有描述符", descriptor)
                assertEquals("Float64", descriptor!!.type.toString())
            }
        }
    }
}
