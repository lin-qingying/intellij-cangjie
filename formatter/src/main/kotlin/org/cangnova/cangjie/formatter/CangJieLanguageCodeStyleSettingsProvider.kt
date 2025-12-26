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

package org.cangnova.cangjie.formatter


import org.cangnova.cangjie.lang.CangJieLanguage
// Removed CangJieFormatterBundle import
import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.application.options.codeStyle.properties.CodeStylePropertyAccessor
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.codeStyle.*
import org.jetbrains.annotations.Nls
import kotlin.reflect.KProperty

class CangJieLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = CangJieLanguage
    override fun getConfigurableDisplayName(): String = CangJieFormatterBundle.message("codestyle.name.cangjie")
    override fun createConfigurable(
        settings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable =
        object : CodeStyleAbstractConfigurable(settings, modelSettings, CangJieLanguage.displayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
                CangJieCodeStylePanel(currentSettings, settings)

            override fun getHelpTopic(): String = "reference.settingsdialog.codestyle.cangjie"
        }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()


    @Deprecated("Deprecated in Java")
    override fun getDefaultCommonSettings(): CommonCodeStyleSettings = CangJieCommonCodeStyleSettings().apply {
        initIndentOptions()
    }

    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
        CangJieCodeStyleSettings(settings)

//    override fun getAccessor(codeStyleObject: Any, field: Field): CodeStyleFieldAccessor<*, *>? {
//        if (codeStyleObject is CangJieCodeStyleSettings && CangJiePackageEntryTable::class.java.isAssignableFrom(field.type)) {
//            return CangJiePackageEntryTableAccessor(codeStyleObject, field)
//        }
//
//        return super.getAccessor(codeStyleObject, field)
//    }

    override fun getAdditionalAccessors(codeStyleObject: Any): List<CodeStylePropertyAccessor<*>> {
        if (codeStyleObject is CangJieCodeStyleSettings) {
            return listOf(CangJieCodeStylePropertyAccessor(codeStyleObject))
        }

        return super.getAdditionalAccessors(codeStyleObject)
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        fun showCustomOption(
            field: KProperty<*>,
            @Nls @NlsContexts.Label title: String,
            @Nls @NlsContexts.Label groupName: String? = null,
            vararg options: Any
        ) {
            @Suppress("HardCodedStringLiteral")
            consumer.showCustomOption(CangJieCodeStyleSettings::class.java, field.name, title, groupName, *options)
        }

        val codeStyleSettingsCustomizableOptions = CodeStyleSettingsCustomizableOptions.getInstance()
        when (settingsType) {
            SettingsType.SPACING_SETTINGS -> {
                consumer.showStandardOptions(
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                    "SPACE_AROUND_LOGICAL_OPERATORS",
                    "SPACE_AROUND_EQUALITY_OPERATORS",
                    "SPACE_AROUND_RELATIONAL_OPERATORS",
                    "SPACE_AROUND_ADDITIVE_OPERATORS",
                    "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
//                    "SPACE_AROUND_UNARY_OPERATOR",
                    "SPACE_AFTER_COMMA",
                    "SPACE_BEFORE_COMMA",
                    "SPACE_BEFORE_IF_PARENTHESES",
                    "SPACE_BEFORE_WHILE_PARENTHESES",
                    "SPACE_BEFORE_FOR_PARENTHESES",
                    "SPACE_BEFORE_CATCH_PARENTHESES"
                )
                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_AROUND_UNARY_OPERATOR,
                    CangJieFormatterBundle.message("formatter.title.unary.operator"),
                    codeStyleSettingsCustomizableOptions.SPACES_AROUND_OPERATORS
                )
                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_AROUND_RANGE,
                    CangJieFormatterBundle.message("formatter.title.range.operator"),
                    codeStyleSettingsCustomizableOptions.SPACES_AROUND_OPERATORS
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_BEFORE_TYPE_COLON,
                    CangJieFormatterBundle.message("formatter.title.before.colon.after.declaration.name"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_AFTER_TYPE_COLON,
                    CangJieFormatterBundle.message("formatter.title.after.colon.before.declaration.type"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_BEFORE_EXTEND_COLON,
                    CangJieFormatterBundle.message("formatter.title.before.colon.in.new.type.definition"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_AFTER_EXTEND_COLON,
                    CangJieFormatterBundle.message("formatter.title.after.colon.in.new.type.definition"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD,
                    CangJieFormatterBundle.message("formatter.title.in.simple.one.line.methods"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_AROUND_FUNCTION_TYPE_ARROW,
                    CangJieFormatterBundle.message("formatter.title.around.arrow.in.function.types"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_AROUND_MATCH_ARROW,
                    CangJieFormatterBundle.message("formatter.title.around.arrow.in"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_BEFORE_LAMBDA_ARROW,
                    CangJieFormatterBundle.message("formatter.title.before.lambda.arrow"),
                    codeStyleSettingsCustomizableOptions.SPACES_OTHER
                )

                showCustomOption(
                    CangJieCodeStyleSettings::SPACE_BEFORE_MATCH_PARENTHESES,
                    CangJieFormatterBundle.message("formatter.title.match.parentheses"),
                    codeStyleSettingsCustomizableOptions.SPACES_BEFORE_PARENTHESES
                )
            }

            SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
                consumer.showStandardOptions(
                    // "ALIGN_MULTILINE_CHAINED_METHODS",
                    "RIGHT_MARGIN",
                    "WRAP_ON_TYPING",
                    "KEEP_FIRST_COLUMN_COMMENT",
                    "KEEP_LINE_BREAKS",
                    "ALIGN_MULTILINE_EXTENDS_LIST",
                    "ALIGN_MULTILINE_PARAMETERS",
                    "ALIGN_MULTILINE_PARAMETERS_IN_CALLS",
                    "ALIGN_MULTILINE_METHOD_BRACKETS",
                    "ALIGN_MULTILINE_BINARY_OPERATION",
                    "ELSE_ON_NEW_LINE",
                    "WHILE_ON_NEW_LINE",
                    "CATCH_ON_NEW_LINE",
                    "FINALLY_ON_NEW_LINE",
                    "CALL_PARAMETERS_WRAP",
                    "METHOD_PARAMETERS_WRAP",
                    "EXTENDS_LIST_WRAP",
                    "METHOD_ANNOTATION_WRAP",
                    "CLASS_ANNOTATION_WRAP",
                    "PARAMETER_ANNOTATION_WRAP",
                    "VARIABLE_ANNOTATION_WRAP",
                    "FIELD_ANNOTATION_WRAP",
                    "METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE",
                    "METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE",
                    "CALL_PARAMETERS_LPAREN_ON_NEXT_LINE",
                    "CALL_PARAMETERS_RPAREN_ON_NEXT_LINE",
                    "ENUM_CONSTANTS_WRAP",
                    "METHOD_CALL_CHAIN_WRAP",
                    "WRAP_FIRST_METHOD_IN_CALL_CHAIN",
                    "ASSIGNMENT_WRAP",
                )

                consumer.renameStandardOption(
                    codeStyleSettingsCustomizableOptions.WRAPPING_SWITCH_STATEMENT,
                    CangJieFormatterBundle.message("formatter.title.match.statements")
                )

                consumer.renameStandardOption(
                    "FIELD_ANNOTATION_WRAP",
                    CangJieFormatterBundle.message("formatter.title.property.annotations")
                )
                consumer.renameStandardOption(
                    "METHOD_PARAMETERS_WRAP",
                    CangJieFormatterBundle.message("formatter.title.function.declaration.parameters")
                )

                consumer.renameStandardOption(
                    "CALL_PARAMETERS_WRAP",
                    CangJieFormatterBundle.message("formatter.title.function.call.arguments")
                )
                consumer.renameStandardOption(
                    "METHOD_CALL_CHAIN_WRAP",
                    CangJieFormatterBundle.message("formatter.title.chained.function.calls")
                )
                consumer.renameStandardOption(
                    "METHOD_ANNOTATION_WRAP",
                    CangJieFormatterBundle.message("formatter.title.function.annotations")
                )
                consumer.renameStandardOption(
                    codeStyleSettingsCustomizableOptions.WRAPPING_METHOD_PARENTHESES,
                    CangJieFormatterBundle.message("formatter.title.function.parentheses")
                )

                showCustomOption(
                    CangJieCodeStyleSettings::ALIGN_IN_COLUMNS_CASE_BRANCH,
                    CangJieFormatterBundle.message("formatter.title.align.match.branches.in.columns"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_SWITCH_STATEMENT
                )

                showCustomOption(
                    CangJieCodeStyleSettings::LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY,
                    CangJieFormatterBundle.message("formatter.title.line.break.after.multiline.match.entry"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_SWITCH_STATEMENT
                )

                showCustomOption(
                    CangJieCodeStyleSettings::LBRACE_ON_NEXT_LINE,
                    CangJieFormatterBundle.message("formatter.title.put.left.brace.on.new.line"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_BRACES
                )

                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS,
                    CangJieFormatterBundle.message("formatter.title.use.continuation.indent"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_METHOD_PARAMETERS
                )

                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_ARGUMENT_LISTS,
                    CangJieFormatterBundle.message("formatter.title.use.continuation.indent"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_METHOD_ARGUMENTS_WRAPPING
                )

                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS,
                    CangJieFormatterBundle.message("formatter.title.use.continuation.indent"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_CALL_CHAIN
                )

                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS,
                    CangJieFormatterBundle.message("formatter.title.use.continuation.indent"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_EXTENDS_LIST
                )

                showCustomOption(
                    CangJieCodeStyleSettings::WRAP_EXPRESSION_BODY_FUNCTIONS,
                    CangJieFormatterBundle.message("formatter.title.expression.body.functions"),
                    options = arrayOf(
                        codeStyleSettingsCustomizableOptions.WRAP_OPTIONS_FOR_SINGLETON,
                        CodeStyleSettingsCustomizable.WRAP_VALUES_FOR_SINGLETON
                    )
                )

                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES,
                    CangJieFormatterBundle.message("formatter.title.use.continuation.indent"),
                    CangJieFormatterBundle.message("formatter.title.expression.body.functions")
                )

                showCustomOption(
                    CangJieCodeStyleSettings::WRAP_ELVIS_EXPRESSIONS,
                    CangJieFormatterBundle.message("formatter.title.elvis.expressions"),
                    options = arrayOf(
                        codeStyleSettingsCustomizableOptions.WRAP_OPTIONS_FOR_SINGLETON,
                        CodeStyleSettingsCustomizable.WRAP_VALUES_FOR_SINGLETON
                    )
                )
                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_ELVIS,
                    title = CangJieFormatterBundle.message("formatter.title.use.continuation.indent"),
                    groupName = CangJieFormatterBundle.message("formatter.title.elvis.expressions")
                )

                showCustomOption(
                    CangJieCodeStyleSettings::IF_RPAREN_ON_NEW_LINE,
                    ApplicationBundle.message("wrapping.rpar.on.new.line"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_IF_STATEMENT
                )

                showCustomOption(
                    CangJieCodeStyleSettings::CONTINUATION_INDENT_IN_IF_CONDITIONS,
                    CangJieFormatterBundle.message("formatter.title.use.continuation.indent.in.conditions"),
                    codeStyleSettingsCustomizableOptions.WRAPPING_IF_STATEMENT
                )
            }

            SettingsType.BLANK_LINES_SETTINGS -> {
                consumer.showStandardOptions(
                    "KEEP_BLANK_LINES_IN_CODE",
                    "KEEP_BLANK_LINES_IN_DECLARATIONS",
                    "KEEP_BLANK_LINES_BEFORE_RBRACE",
                    "BLANK_LINES_AFTER_CLASS_HEADER"
                )

                showCustomOption(
                    CangJieCodeStyleSettings::BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES,
                    CangJieFormatterBundle.message("formatter.title.around.match.branches.with"),
                    codeStyleSettingsCustomizableOptions.BLANK_LINES
                )

                showCustomOption(
                    CangJieCodeStyleSettings::BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE,
                    CangJieFormatterBundle.message("formatter.title.before.declaration.with.comment.or.annotation"),
                    codeStyleSettingsCustomizableOptions.BLANK_LINES
                )
            }

            SettingsType.COMMENTER_SETTINGS -> {
                consumer.showAllStandardOptions()
            }

            else -> consumer.showStandardOptions()
        }
    }


    override fun getCodeSample(settingsType: SettingsType): String = when (settingsType) {
        SettingsType.WRAPPING_AND_BRACES_SETTINGS ->
            """
              public class ThisIsASampleClass <: Comparable<Any> & Appendable{
              let test = 12

    func foo1(i1: Int, i2: Int, i3: Int): Int {
        match (i1) {
            case 0 => 1
            case _ => 0
        }
        if (i2 > 0 && i3 < 0) {
            return 2
        }
        return 0
    }
    private func foo2(): Int {
        // todo: something
        try {
            return foo1(12, 13, 14)
        } catch (e: Exception) {
            return 0
        } finally {
            if (true) {
                return 1
            } else {
                return 2
            }
        }
    }

    func longMethod(
        param1: Int,
        param2: String
    ) {
        let foo = 1
    }

    func multilineMethod(
        foo: String,
        bar: ?String,
        x: ?Int
    ) {
        foo.toAsciiUpper().trimAscii().size
        let barLen = bar?.size ?? x ?? -1
        if (foo.size > 0 && barLen > 0) {
            println("> 0")
        }
    }
}

let bar = 1

enum Enumeration {
    A | B
}

        
            """.trimIndent()

        SettingsType.BLANK_LINES_SETTINGS ->
            """
                class Foo {
                   private var field1: Int = 1
                   private let field2: String? = None
                   
                   

                   init() {
                       field1 = 2;
                   }

                   func foo1() {
                       match(field1) {   
                           case 1 => println("1")
                           case 2 => println("2")
                           case 3 => 
                           
                                println("3" +
                                     "4")
                       }

                       match(field2) {
                           case 1 =>   println("1")
                           case 2 =>    println("2")
                           
                       }
                   }


                   class InnerClass {
                   }
               }



               class AnotherClass {
               }

               interface TestInterface {
               }
               func run(f: () -> Unit) {
                   f()
               }
               
               class Bar {
                
                   
                   let a = 42
                   
                   let b = 43
                   func c() {
                   
                       a + b
                   }
                   func d() = Unit
                   // smth
                   func e() {
                       d()
                   }
               
               }
               """.trimIndent()

        else -> """
            func b(a: Int): Int {
                return 1
            }
            open class Some {
               private let f: (a: Int) -> Int = { a => a * 1 }


                prop aprop: String {
                    get() {
                        "this is a String"
                    }
                }

                func foo(): Int {
                  a += 1
                    let test: Int = 12
                    let test2 = 1 && 2
                    let test3 = 1 == 1
                    let test4 = 1 > 2
                    let test5 = 1 + 1
                    let test6 = 1 * 1
                    let test7 = 1..1
                    let test8 = -a
                    match (i1) {
                        case 0 => 1
                        case _ => 0
                    }
                    for (i in 10..42) {
                        println(match (i) {
                            case a => i
                        })
                    }
                    if (true) {
                    }
                    while (true) {
                        break
                    }
                    try {
                        match (test) {
                            case 12 => println("foo")

                            case _ => println("bar")
                        }
                    } catch (e: Exception) {
                    } finally {
                    }
                    return test
                }

                private func foo2<T>(): Int where T <: List<T> {
                    return 0
                }

                func multilineMethod(
                        foo: String,
                        bar: String
                ) {
                    foo.size
                }
            }

            class AnotherClass<T> where T <: String {}
        """.trimIndent()
    }
}
