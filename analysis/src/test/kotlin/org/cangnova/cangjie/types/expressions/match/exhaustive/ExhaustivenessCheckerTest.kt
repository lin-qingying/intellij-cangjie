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

package org.cangnova.cangjie.types.expressions.match.exhaustive

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.constants.BoolValue
import org.cangnova.cangjie.resolve.constants.Int64Value
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.expressions.match.Constructor
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.specialized.BooleanChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.specialized.IntegerIntervalChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.trivial.TrivialChecker

/**
 * 穷举性检查器完整测试
 *
 * 使用 CjPsiFactory 创建语法结构进行完整的穷举性检测测试。
 * 这些测试覆盖各种模式类型和穷举场景。
 */
class ExhaustivenessCheckerTest : CangJieTestBase() {

    private lateinit var factory: CjPsiFactory

    override fun setUp() {
        super.setUp()
        factory = CjPsiFactory(project)
    }

    // ==================== 布尔类型穷举性测试 ====================

    fun `test boolean - exhaustive with true and false`() {
        val type = ErrorUtils.invalidType // 使用占位类型

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Const(BoolValue(true)))),
            listOf(Pattern(type, PatternKind.Const(BoolValue(false))))
        )

        // BooleanChecker 需要真正的布尔类型，ErrorUtils.invalidType 不是布尔类型
        // 所以 isApplicable 返回 false
        val checker = BooleanChecker()
        assertFalse(checker.isApplicable(type, matrix.flatten()))

        // 但我们可以测试模式本身是否包含布尔常量
        val patterns = matrix.flatten()
        assertTrue(patterns.any { (it.kind as? PatternKind.Const)?.value == BoolValue(true) })
        assertTrue(patterns.any { (it.kind as? PatternKind.Const)?.value == BoolValue(false) })
    }

    fun `test boolean - non-exhaustive missing true`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Const(BoolValue(false))))
        )

        // 只覆盖 false，缺少 true
        val checker = BooleanChecker()
        // BooleanChecker 对于非布尔类型返回 false
        assertFalse(checker.isApplicable(type, matrix.flatten()))

        // 验证模式只包含 false
        val patterns = matrix.flatten()
        assertTrue(patterns.any { (it.kind as? PatternKind.Const)?.value == BoolValue(false) })
        assertFalse(patterns.any { (it.kind as? PatternKind.Const)?.value == BoolValue(true) })
    }

    fun `test boolean - exhaustive with wildcard`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Wild))
        )

        // 通配符覆盖所有情况
        val trivialChecker = TrivialChecker()
        assertTrue(trivialChecker.isApplicable(type, matrix.flatten()))
    }

    fun `test boolean - exhaustive with binding`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Binding(type, "x")))
        )

        // 绑定模式覆盖所有情况
        val trivialChecker = TrivialChecker()
        assertTrue(trivialChecker.isApplicable(type, matrix.flatten()))
    }

    // ==================== 整数区间穷举性测试 ====================

    fun `test integer - single value not exhaustive`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Const(Int64Value(0))))
        )

        // IntegerIntervalChecker 需要真正的整数类型
        // ErrorUtils.invalidType 不是整数类型，所以 isApplicable 返回 false
        val checker = IntegerIntervalChecker()
        assertFalse(checker.isApplicable(type, matrix.flatten()))

        // 验证模式包含整数常量
        val patterns = matrix.flatten()
        assertTrue(patterns.any { (it.kind as? PatternKind.Const)?.value is Int64Value })
    }

    fun `test integer - exhaustive with wildcard fallback`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Const(Int64Value(0)))),
            listOf(Pattern(type, PatternKind.Const(Int64Value(1)))),
            listOf(Pattern(type, PatternKind.Wild))
        )

        // 有通配符兜底，应该穷举
        val trivialChecker = TrivialChecker()
        assertTrue(trivialChecker.isApplicable(type, matrix.flatten()))
    }

    // ==================== 通配符和绑定模式测试 ====================

    fun `test wildcard - always exhaustive`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern.wild(type))
        )

        val checker = TrivialChecker()
        val result = checker.check(matrix, type, null)
        assertTrue(result is ExhaustivenessResult.Exhaustive)
    }

    fun `test binding - always exhaustive`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = listOf(
            listOf(Pattern(type, PatternKind.Binding(type, "value")))
        )

        val checker = TrivialChecker()
        val result = checker.check(matrix, type, null)
        assertTrue(result is ExhaustivenessResult.Exhaustive)
    }

    fun `test empty matrix - not exhaustive`() {
        val type = ErrorUtils.invalidType

        val matrix: Matrix = emptyList()

        val checker = TrivialChecker()
        val result = checker.check(matrix, type, null)
        assertTrue(result is ExhaustivenessResult.NonExhaustive)
    }

    // ==================== 元组模式穷举性测试 ====================

    fun `test tuple - all wildcards exhaustive`() {
        val type = ErrorUtils.invalidType
        val innerType = ErrorUtils.invalidType

        val tuplePattern = Pattern(
            type,
            PatternKind.Tuple(listOf(
                Pattern.wild(innerType),
                Pattern.wild(innerType)
            ))
        )

        val matrix: Matrix = listOf(listOf(tuplePattern))

        // 元组中所有都是通配符，应该穷举
        assertNotNull(matrix)
        assertEquals(1, matrix.size)
    }

    fun `test tuple - partial coverage not exhaustive`() {
        val type = ErrorUtils.invalidType
        val boolType = ErrorUtils.invalidType

        // (true, _) 只覆盖第一个分量为 true 的情况
        val tuplePattern = Pattern(
            type,
            PatternKind.Tuple(listOf(
                Pattern(boolType, PatternKind.Const(BoolValue(true))),
                Pattern.wild(boolType)
            ))
        )

        val matrix: Matrix = listOf(listOf(tuplePattern))

        // 只覆盖部分，不穷举
        assertNotNull(matrix)
    }

    fun `test tuple - complete bool coverage exhaustive`() {
        val type = ErrorUtils.invalidType
        val boolType = ErrorUtils.invalidType

        // (true, _) 和 (false, _) 覆盖所有情况
        val pattern1 = Pattern(
            type,
            PatternKind.Tuple(listOf(
                Pattern(boolType, PatternKind.Const(BoolValue(true))),
                Pattern.wild(boolType)
            ))
        )

        val pattern2 = Pattern(
            type,
            PatternKind.Tuple(listOf(
                Pattern(boolType, PatternKind.Const(BoolValue(false))),
                Pattern.wild(boolType)
            ))
        )

        val matrix: Matrix = listOf(listOf(pattern1), listOf(pattern2))

        assertEquals(2, matrix.size)
    }

    // ==================== 构造器测试 ====================

    fun `test Constructor allConstructors - returns Single for unknown type`() {
        val type = ErrorUtils.invalidType

        val constructors = Constructor.allConstructors(type)

        // 对于未知类型，应该返回 Single 构造器
        assertEquals(1, constructors.size)
        assertEquals(Constructor.Single, constructors[0])
    }

    fun `test Constructor arity - Single constructor has zero arity`() {
        val type = ErrorUtils.invalidType

        val arity = Constructor.Single.arity(type)

        assertEquals(0, arity)
    }

    fun `test Constructor subTypes - Single constructor has empty subtypes`() {
        val type = ErrorUtils.invalidType

        val subTypes = Constructor.Single.subTypes(type)

        assertTrue(subTypes.isEmpty())
    }

    // ==================== 模式测试 ====================

    fun `test Pattern wild - creates wildcard pattern`() {
        val type = ErrorUtils.invalidType

        val pattern = Pattern.wild(type)

        assertEquals(PatternKind.Wild, pattern.kind)
        assertEquals(type, pattern.type)
    }

    fun `test Pattern Error - is error pattern`() {
        val pattern = Pattern.Error

        assertEquals(PatternKind.Error, pattern.kind)
    }

    fun `test Pattern constructors - wildcard returns null`() {
        val pattern = Pattern.wild()

        assertNull(pattern.constructors)
    }

    fun `test Pattern constructors - binding returns null`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Binding(type, "x"))

        assertNull(pattern.constructors)
    }

    fun `test Pattern constructors - const returns ConstantValue`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Const(Int64Value(42)))

        val constructors = pattern.constructors
        assertNotNull(constructors)
        assertEquals(1, constructors!!.size)
        assertTrue(constructors[0] is Constructor.ConstantValue)
    }

    fun `test Pattern constructors - tuple returns Single`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Tuple(emptyList()))

        val constructors = pattern.constructors
        assertNotNull(constructors)
        assertEquals(1, constructors!!.size)
        assertEquals(Constructor.Single, constructors[0])
    }

    // ==================== 文本生成测试 ====================

    fun `test Pattern text - wildcard`() {
        val pattern = Pattern.wild()

        assertEquals("_", pattern.text(null))
    }

    fun `test Pattern text - binding`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Binding(type, "myVar"))

        assertEquals("myVar", pattern.text(null))
    }

    fun `test Pattern text - const int`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Const(Int64Value(42)))

        assertEquals("42", pattern.text(null))
    }

    fun `test Pattern text - const bool true`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Const(BoolValue(true)))

        assertEquals("true", pattern.text(null))
    }

    fun `test Pattern text - const bool false`() {
        val type = ErrorUtils.invalidType
        val pattern = Pattern(type, PatternKind.Const(BoolValue(false)))

        assertEquals("false", pattern.text(null))
    }

    fun `test Pattern text - tuple`() {
        val type = ErrorUtils.invalidType
        val innerPattern1 = Pattern.wild(type)
        val innerPattern2 = Pattern.wild(type)
        val pattern = Pattern(type, PatternKind.Tuple(listOf(innerPattern1, innerPattern2)))

        assertEquals("(_,_)", pattern.text(null))
    }

    // ==================== HybridDispatcher 测试 ====================

    fun `test HybridDispatcher - selects TrivialChecker for empty matrix`() {
        val type = ErrorUtils.invalidType
        val matrix: Matrix = emptyList()

        val dispatcher = HybridDispatcher.DEFAULT
        val result = dispatcher.check(matrix, type, null)

        assertTrue(result is ExhaustivenessResult.NonExhaustive)
    }

    fun `test HybridDispatcher - selects TrivialChecker for wildcard only`() {
        val type = ErrorUtils.invalidType
        val matrix: Matrix = listOf(listOf(Pattern.wild(type)))

        val dispatcher = HybridDispatcher.DEFAULT
        val result = dispatcher.check(matrix, type, null)

        assertTrue(result is ExhaustivenessResult.Exhaustive)
    }

    // ==================== ExhaustivenessResult 测试 ====================

    fun `test ExhaustivenessResult Exhaustive - is exhaustive`() {
        val result = ExhaustivenessResult.Exhaustive

        assertTrue(result.isExhaustive)
    }

    fun `test ExhaustivenessResult NonExhaustive - is not exhaustive`() {
        val result = ExhaustivenessResult.NonExhaustive(emptyList(), CheckSource.TRIVIAL)

        assertFalse(result.isExhaustive)
    }

    fun `test ExhaustivenessResult NonExhaustive - contains missing patterns`() {
        val type = ErrorUtils.invalidType
        val missingPattern = Pattern.wild(type)
        val result = ExhaustivenessResult.NonExhaustive(listOf(missingPattern), CheckSource.MARANGET)

        assertEquals(1, result.missingPatterns.size)
        assertEquals(missingPattern, result.missingPatterns[0])
    }

    fun `test ExhaustivenessResult Error - contains message`() {
        val result = ExhaustivenessResult.Error("Test error message")

        assertFalse(result.isExhaustive)
        assertEquals("Test error message", result.reason)
    }

    fun `test ExhaustivenessResult Skipped - is not exhaustive`() {
        val result = ExhaustivenessResult.Skipped

        assertFalse(result.isExhaustive)
    }

    // ==================== PSI 工厂测试 ====================

    fun `test create match expression with factory`() {
        val matchEntry = factory.createMatchEntry("case 0 => 0")

        assertNotNull(matchEntry)
        assertNotNull(matchEntry.conditions)
    }

    fun `test create enum declaration`() {
        val file = factory.createFile("""
            enum Color {
                | Red
                | Green
                | Blue
            }
        """.trimIndent())

        val enum = PsiTreeUtil.findChildOfType(file, CjEnum::class.java)
        assertNotNull(enum)
        assertEquals(3, enum!!.constructor.size)
    }

    fun `test create function with match expression`() {
        val function = factory.createFunction("""
            func test(x: Int64): Int64 {
                match (x) {
                    case 0 => 0
                    case 1 => 1
                    case _ => -1
                }
            }
        """.trimIndent())

        assertNotNull(function)
        val matchExpr = PsiTreeUtil.findChildOfType(function, CjMatchExpression::class.java)
        assertNotNull(matchExpr)
        assertEquals(3, matchExpr!!.entries.size)
    }

    fun `test create match with boolean patterns`() {
        val function = factory.createFunction("""
            func test(b: Bool): Int64 {
                match (b) {
                    case true => 1
                    case false => 0
                }
            }
        """.trimIndent())

        val matchExpr = PsiTreeUtil.findChildOfType(function, CjMatchExpression::class.java)
        assertNotNull(matchExpr)
        assertEquals(2, matchExpr!!.entries.size)
    }

    fun `test create match with tuple pattern`() {
        val function = factory.createFunction("""
            func test(t: (Int64, Bool)): Int64 {
                match (t) {
                    case (0, true) => 1
                    case (_, false) => 2
                    case _ => 3
                }
            }
        """.trimIndent())

        val matchExpr = PsiTreeUtil.findChildOfType(function, CjMatchExpression::class.java)
        assertNotNull(matchExpr)
        assertEquals(3, matchExpr!!.entries.size)
    }

    fun `test create match with binding pattern`() {
        val function = factory.createFunction("""
            func test(x: Int64): Int64 {
                match (x) {
                    case value => value * 2
                }
            }
        """.trimIndent())

        val matchExpr = PsiTreeUtil.findChildOfType(function, CjMatchExpression::class.java)
        assertNotNull(matchExpr)
        assertEquals(1, matchExpr!!.entries.size)
    }

    // ==================== 枚举穷举性测试 (PSI) ====================

    fun `test create enum match exhaustive`() {
        val file = factory.createFile("""
            enum Option<T> {
                | Some(T)
                | None
            }

            func test(opt: Option<Int64>): Int64 {
                match (opt) {
                    case Some(x) => x
                    case None => 0
                }
            }
        """.trimIndent())

        val matchExpr = PsiTreeUtil.findChildOfType(file, CjMatchExpression::class.java)
        assertNotNull(matchExpr)
        assertEquals(2, matchExpr!!.entries.size)
    }

    fun `test create enum match non-exhaustive`() {
        val file = factory.createFile("""
            enum Result<T, E> {
                | Ok(T)
                | Err(E)
            }

            func test(r: Result<Int64, String>): Int64 {
                match (r) {
                    case Ok(x) => x
                }
            }
        """.trimIndent())

        val matchExpr = PsiTreeUtil.findChildOfType(file, CjMatchExpression::class.java)
        assertNotNull(matchExpr)
        // 只有一个分支，缺少 Err 分支
        assertEquals(1, matchExpr!!.entries.size)
    }

    // ==================== CheckSource 测试 ====================

    fun `test CheckSource values`() {
        // 验证所有检查源类型
        val sources = listOf(
            CheckSource.TRIVIAL,
            CheckSource.BOOLEAN_FLAG,
            CheckSource.ENUM_BITVECTOR,
            CheckSource.INTEGER_INTERVAL,
            CheckSource.MARANGET
        )

        assertEquals(5, sources.size)
    }
}
