/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.highlighter

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object CangJieHighlightingColors {
    /*********************************关键字*********************************************************/
    // 基本关键字的高亮，如class、fun、val等
    val KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    // 内置注解的高亮，如@Override、@Deprecated等
//    val BUILTIN_ANNOTATION: TextAttributesKey =
//        TextAttributesKey.createTextAttributesKey("CANGJIE_BUILTIN_ANNOTATION", KEYWORD)

    // let关键字的高亮
    val LET_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_LET", KEYWORD)

    // mut关键字的高亮
    val MUT_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_MUT", KEYWORD)

    // prop关键字的高亮
    val PROP_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_PROP", KEYWORD)

    // var关键字的高亮
    val VAR_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_VAR", KEYWORD)

    // const关键字的高亮
    val CONST_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_CONST", KEYWORD)

    // quote关键字的高亮
    val QUOTE_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_QUOTE", KEYWORD)

    // static关键字的高亮
    val STATIC_KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STATIC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    /*********************************注释*********************************************************/
    // 单行注释的高亮，以//开头
    val LINE_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)

    // 块注释的高亮，以/*开头，以*/结尾
    val BLOCK_COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT,
    )

    // 文档注释的高亮，以/**开头
    val DOC_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)

    // 文档注释中标签的高亮，如@param、@return等
    val CDOC_TAG: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)

    // 文档注释中链接的高亮
    val CDOC_LINK: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)

    /*********************************字面量*********************************************************/
    // 数字字面量的高亮，包括整数、浮点数等
    val NUMBER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

    // 字符串字面量的高亮
    val STRING: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRING", DefaultLanguageHighlighterColors.STRING)

    // 字符串中转义字符的高亮，如\n、\t等
    val STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
    )

    // 字符串中无效转义字符的高亮
    val INVALID_STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INVALID_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE,
    )

    /*********************************运算符和标点符号*************************************************/
    // 运算符的高亮，如+、-、*、/等
    val OPERATOR_SIGN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_OPERATION_SIGN",
        DefaultLanguageHighlighterColors.OPERATION_SIGN,
    )

    // 小括号的高亮
    val PARENTHESIS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES)

    // 大括号的高亮
    val BRACES: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACES", DefaultLanguageHighlighterColors.BRACES)

    // 中括号的高亮
    val BRACKETS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)

    // 逗号的高亮
    val COMMA: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_COMMA", DefaultLanguageHighlighterColors.COMMA)

    // 分号的高亮
    val SEMICOLON: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)

    // 冒号的高亮
    val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_COLON")

    // 点号的高亮
    val DOT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOT", DefaultLanguageHighlighterColors.DOT)

    // 安全调用操作符的高亮（?.）
    val SAFE_ACCESS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT)

    // 问号的高亮
    val QUEST: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_QUEST")

    // 双感叹号的高亮（!!）
//    val EXCLEXCL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_EXCLEXCL")

    // 箭头的高亮（->）
    val ARROW: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_ARROW", PARENTHESIS)

    /*********************************类和类型*******************************************************/
    // 类型定义
    val TYPE_DEFINED: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_DEFINED", DefaultLanguageHighlighterColors.CLASS_NAME)

    // 类型引用
    val TYPE_REFERENCE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_REFERENCE", DefaultLanguageHighlighterColors.CLASS_NAME)

    // 类名的高亮
    val CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CLASS", TYPE_DEFINED)

    // 类型参数的高亮，如泛型参数T、E等
    val TYPE_PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)

    // 抽象类名的高亮
    val ABSTRACT_CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ABSTRACT_CLASS", TYPE_DEFINED)

    // 接口名的高亮
    val INTERFACE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_INTERFACE", TYPE_DEFINED)

    //结构体名的高亮
    val STRUCT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRUCT", TYPE_DEFINED)

    // 枚举类名的高亮
    val ENUM: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM", TYPE_DEFINED)

    // 枚举常量的高亮
    val ENUM_ENTRY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM_ENTRY", DefaultLanguageHighlighterColors.STATIC_FIELD)

    // 类型别名的高亮（typealias）
    val TYPE_ALIAS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_ALIAS", TYPE_DEFINED)

    /*********************************变量和属性****************************************************/
    // 可变变量的高亮
    val MUTABLE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_VARIABLE")

    // 可变属性
    val MUTABLE_PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_PROPERTY", MUTABLE_VARIABLE)

    // 局部变量的高亮
    val LOCAL_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_LOCAL_VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE,
    )

    //属性
    val PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PROPERTY", LOCAL_VARIABLE)


    // 包级别变量的高亮
    val PACKAGE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_VARIABLE",
        DefaultLanguageHighlighterColors.STATIC_FIELD,
    )


    // 参数的高亮
    val PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)

    // 实例属性的高亮
    val INSTANCE_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )

    // 实例变量的高亮
    val INSTANCE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_VARIABLE",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )

    // 包装为引用的变量高亮
    val WRAPPED_INTO_REF: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_WRAPPED_INTO_REF",
        DefaultLanguageHighlighterColors.CLASS_NAME,
    )


    // 后备字段的高亮
    val BACKING_FIELD_VARIABLE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BACKING_FIELD_VARIABLE")

    // 扩展属性的高亮
    val EXTENSION_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_PROPERTY",
        DefaultLanguageHighlighterColors.STATIC_FIELD,
    )

    // 合成的扩展属性的高亮
//    val SYNTHETIC_EXTENSION_PROPERTY: TextAttributesKey =
//        TextAttributesKey.createTextAttributesKey("CANGJIE_SYNTHETIC_EXTENSION_PROPERTY", EXTENSION_PROPERTY)

    // 动态属性调用的高亮
//    val DYNAMIC_PROPERTY_CALL: TextAttributesKey =
//        TextAttributesKey.createTextAttributesKey("CANGJIE_DYNAMIC_PROPERTY_CALL")


    /*********************************函数*********************************************************/
    // 函数声明的高亮，用于定义函数时的函数名
    val FUNCTION_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_DECLARATION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
    )

    // 普通函数调用的高亮，用于调用类的成员函数或局部函数时
    val FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.FUNCTION_CALL,
    )

    // 包级函数调用的高亮，用于调用定义在文件顶层的函数时
    val PACKAGE_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD,
    )

    // 扩展函数调用的高亮，用于调用给某个类型添加的扩展函数时
    val EXTENSION_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD,
    )

    // 构造函数调用的高亮，用于创建类的实例时
    val CONSTRUCTOR_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL)

    // 动态函数调用的高亮，用于动态调用或反射调用函数时
//    val DYNAMIC_FUNCTION_CALL: TextAttributesKey =
//        TextAttributesKey.createTextAttributesKey("CANGJIE_DYNAMIC_FUNCTION_CALL")
    // 挂起函数调用的高亮，用于调用协程中的suspend函数时
//    val SUSPEND_FUNCTION_CALL: TextAttributesKey =
//        TextAttributesKey.createTextAttributesKey("CANGJIE_SUSPEND_FUNCTION_CALL", FUNCTION_CALL)
    // 变量作为函数调用的高亮，用于调用函数类型的变量时
    val VARIABLE_AS_FUNCTION_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION")

    // 变量作为类函数调用的高亮，用于调用实现了invoke操作符的变量时
    val VARIABLE_AS_FUNCTION_LIKE_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION_LIKE")

    // Lambda表达式的括号和箭头高亮
    val FUNCTION_LITERAL_BRACES_AND_ARROW: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_FUNCTION_LITERAL_BRACES_AND_ARROW")

    // Lambda表达式默认参数的高亮
//    val FUNCTION_LITERAL_DEFAULT_PARAMETER: TextAttributesKey =
//        TextAttributesKey.createTextAttributesKey("CANGJIE_CLOSURE_DEFAULT_PARAMETER", PARAMETER)

    /*********************************注解*********************************************************/
    // 注解的高亮
    val ANNOTATION: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA)

    // 注解属性名的高亮
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES",
        DefaultLanguageHighlighterColors.METADATA,
    )

    /*********************************宏*********************************************************/
// 宏声明
    val MACRO_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MACRO_DECLARATION")

    //    宏调用
    val MACRO_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MACRO_CALL")

    /*********************************智能转换和调试*************************************************/
    // 智能转换值的高亮
    val SMART_CAST_VALUE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_VALUE")

    // 智能常量的高亮
    val SMART_CONSTANT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CONSTANT")

    // 智能转换接收者的高亮
    val SMART_CAST_RECEIVER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_RECEIVER")

    // 调试信息的高亮
    val DEBUG_INFO: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DEBUG_INFO")

    /*********************************其他*********************************************************/
    // 错误字符的高亮
    val BAD_CHARACTER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    // 标签的高亮
    val LABEL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LABEL", DefaultLanguageHighlighterColors.LABEL)

    // 解析为错误的引用高亮
    val RESOLVED_TO_ERROR: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_RESOLVED_TO_ERROR")

    // 命名参数的高亮
    val NAMED_ARGUMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_NAMED_ARGUMENT")

    // 小于号和冒号组合的高亮
    val LT_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_LT_COLON")
}


