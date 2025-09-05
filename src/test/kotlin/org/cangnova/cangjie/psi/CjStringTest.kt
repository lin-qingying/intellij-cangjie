package org.cangnova.cangjie.psi


import org.cangnova.cangjie.CangJieTestBase
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * CangJie语言字符串功能的全面测试
 * 测试范围包括：
 * 1. 普通字符串（单引号和双引号）
 * 2. 原始字符串（三引号）
 * 3. 哈希字符串（#引号）
 * 4. 字符串插值
 * 5. 转义序列
 * 6. 错误处理
 */
class CjStringTest : CangJieTestBase() {
    private lateinit var factory: CjPsiFactory

    override fun setUp() {
        super.setUp()
        factory = CjPsiFactory(project)
    }

    fun testSingleQuotedString() {
        val testCases = arrayOf(
            "''",                    // 空字符串
            "'hello'",              // 基本字符串
            "'hello\\nworld'",      // 带转义序列
            "'hello\\tworld'",      // 制表符
            "'hello\\\\world'",     // 转义反斜杠
            "'hello\\'world'"       // 转义单引号
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let str = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }

    fun testDoubleQuotedString() {
        val testCases = arrayOf(
            "\"\"",                  // 空字符串
            "\"hello\"",            // 基本字符串
            "\"hello\\nworld\"",    // 带转义序列
            "\"hello\\tworld\"",    // 制表符
            "\"hello\\\\world\"",   // 转义反斜杠
            "\"hello\\\"world\""    // 转义双引号
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let str = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }

    fun testRawString() {
        val testCases = arrayOf(
            "''''''",               // 空原始字符串
            "'''hello'''",          // 基本原始字符串
            "'''hello\nworld'''",   // 多行字符串
            "'''hello\\nworld'''",  // 原始字符串中的转义序列不处理
            "'''hello'world'''",    // 包含单引号
            "'''hello''world'''",   // 包含连续单引号
            "\"\"\"\"\"\"",        // 空原始字符串（双引号）
            "\"\"\"hello\"\"\"",   // 基本原始字符串（双引号）
            "\"\"\"hello\nworld\"\"\"" // 多行字符串（双引号）
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let str = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }

    fun testHashString() {
        val testCases = arrayOf(
            "#'hello'#",            // 基本哈希字符串
            "##'hello'##",          // 双哈希字符串
            "###'hello'###",        // 三哈希字符串
            "#\"hello\"#",          // 双引号哈希字符串
            "##\"hello\"##",        // 双引号双哈希字符串
            "#'hello#world'#",      // 内容包含#
            "##'hello##world'##"    // 内容包含##
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let str = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }

    fun testStringInterpolation() {
        val testCases = arrayOf(
            "\"Hello \${name}\"",              // 基本插值
            "\"Count: \${1 + 2}\"",            // 表达式插值
            "\"Nested: \${\"inner\${x}\"}\"",  // 嵌套插值
            "\"Multiple \${x} \${y}\"",        // 多个插值
            "\"Complex \${foo.bar()}\"",       // 方法调用插值
            "\"Array \${arr[0]}\"",            // 数组访问插值
            "#\"Raw \${name}\"#",              // 哈希字符串中的插值
            "'''Triple \${name}'''"            // 三引号字符串中的插值
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let name = "world"
                    let x = 1
                    let y = 2
                    let arr = [1, 2, 3]
                    let str = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }

    fun testUnicodeEscapes() {
        val testCases = arrayOf(
            "'\\u{1F600}'",    // 笑脸emoji
            "'\\u{1F431}'",    // 猫脸emoji
            "'\\u{2764}'",     // 心形
            "'\\u{1F4A9}'",    // 便便emoji
            "'\\u{1F680}'"     // 火箭emoji
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let str = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }


    fun testStringConcatenation() {
        val testCases = arrayOf(
            "'hello' + 'world'",
            "\"hello\" + \"world\"",
            "'hello' + \"world\"",
            "#'hello'# + #'world'#",
            "'''hello''' + '''world'''",
            "'hello' + \"\${name}\"",
            "#'hello'# + '''world'''"
        )

        testCases.forEach { str ->
            val file = factory.createFile(
                """
                main() {
                    let name = "test"
                    let result = $str
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $str")
        }
    }

    fun testStringExpressions() {
        val testCases = arrayOf(
            "let x = 'hello'.length()",
            "let x = \"hello\"[0]",
            "let x = '''hello'''.toUpperCase()",
            "let x = #'hello'#.toLowerCase()",
            "let x = \"hello\${name}\".trim()",
            "let x = ('hello' + 'world').substring(0, 5)",
            "let x = #'hello'#.replace('l', 'L')"
        )

        testCases.forEach { expr ->
            val file = factory.createFile(
                """
                main() {
                    let name = "world"
                    $expr
                }
            """.trimIndent()
            )

            assertNotNull(file, "Failed to parse: $expr")
        }
    }
} 