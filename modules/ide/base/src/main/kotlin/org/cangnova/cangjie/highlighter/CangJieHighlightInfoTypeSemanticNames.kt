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

package org.cangnova.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.editor.colors.TextAttributesKey


object CangJieHighlightInfoTypeSemanticNames {
    val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: HighlightInfoType =
         CangJieHighlightInfoTypeSemanticNames.createSymbolTypeInfo(
            CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
        )
    // 关键字相关
    val KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.KEYWORD)
    val LET_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LET_KEYWORD)
    val MUT_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUT_KEYWORD)
    val PROP_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PROP_KEYWORD)
    val VAR_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.VAR_KEYWORD)
    val CONST_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CONST_KEYWORD)
    val QUOTE_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.QUOTE_KEYWORD)
    val STATIC_KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STATIC_KEYWORD)

    // 注释相关
    val LINE_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LINE_COMMENT)
    val BLOCK_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BLOCK_COMMENT)
    val DOC_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DOC_COMMENT)
    val CDOC_TAG: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CDOC_TAG)
    val CDOC_LINK: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CDOC_LINK)

    // 字面量相关
    val NUMBER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.NUMBER)
    val STRING: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRING)
    val STRING_ESCAPE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRING_ESCAPE)
    val INVALID_STRING_ESCAPE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INVALID_STRING_ESCAPE)

    // 运算符和标点符号
    val OPERATOR_SIGN: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.OPERATOR_SIGN)
    val PARENTHESIS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PARENTHESIS)
    val BRACES: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BRACES)
    val BRACKETS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BRACKETS)
    val COMMA: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.COMMA)
    val SEMICOLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SEMICOLON)
    val COLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.COLON)
    val DOT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DOT)
    val SAFE_ACCESS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SAFE_ACCESS)
    val QUEST: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.QUEST)
    val ARROW: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ARROW)

    // 类和类型相关
    val TYPE_DEFINED: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_DEFINED)
    val TYPE_REFERENCE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_REFERENCE)
    val CLASS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CLASS)
    val TYPE_PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_PARAMETER)
    val ABSTRACT_CLASS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ABSTRACT_CLASS)
    val INTERFACE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INTERFACE)
    val STRUCT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRUCT)
    val ENUM: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ENUM)
    val ENUM_CONSTRUCTOR: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ENUM_CONSTRUCTOR)
    val TYPE_ALIAS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_ALIAS)

    // 变量和属性相关
    val MUTABLE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUTABLE_VARIABLE)
    val MUTABLE_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUTABLE_PROPERTY)
    val LOCAL_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LOCAL_VARIABLE)
    val PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PROPERTY)
    val PACKAGE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_VARIABLE)
    val PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PARAMETER)
    val INSTANCE_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INSTANCE_PROPERTY)
    val INSTANCE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INSTANCE_VARIABLE)
    val WRAPPED_INTO_REF: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.WRAPPED_INTO_REF)
    val BACKING_FIELD_VARIABLE: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.BACKING_FIELD_VARIABLE)
    val EXTENSION_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.EXTENSION_PROPERTY)

    // 函数相关
    val FUNCTION_DECLARATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_DECLARATION)
    val FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_CALL)
    val PACKAGE_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_FUNCTION_CALL)
    val EXTENSION_FUNCTION_CALL: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.EXTENSION_FUNCTION_CALL)
    val CONSTRUCTOR_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CONSTRUCTOR_CALL)
    val VARIABLE_AS_FUNCTION_CALL: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL)
    val VARIABLE_AS_FUNCTION_LIKE_CALL: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL)
    val FUNCTION_LITERAL_BRACES_AND_ARROW: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW)

    // 注解相关
    val ANNOTATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ANNOTATION)
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: HighlightInfoType =
        createSymbolTypeInfo(CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES)

    // 宏相关
    val MACRO_DECLARATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MACRO_DECLARATION)
    val MACRO_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MACRO_CALL)

    // 智能转换和调试相关
    val SMART_CAST_VALUE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CAST_VALUE)
    val SMART_CONSTANT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CONSTANT)
    val SMART_CAST_RECEIVER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CAST_RECEIVER)
    val DEBUG_INFO: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DEBUG_INFO)

    // 其他
    val BAD_CHARACTER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BAD_CHARACTER)
    val LABEL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LABEL)
    val RESOLVED_TO_ERROR: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.RESOLVED_TO_ERROR)
    val NAMED_ARGUMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.NAMED_ARGUMENT)
    val LT_COLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LT_COLON)

    private fun createSymbolTypeInfo(attributesKey: TextAttributesKey): HighlightInfoType {
        return HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, attributesKey, false)
    }
}
