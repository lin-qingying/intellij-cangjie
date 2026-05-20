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

package org.cangnova.cangjie.lsp4ij

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.features.semanticTokens.DefaultSemanticTokensColorsProvider
import org.cangnova.cangjie.highlighter.CangJieHighlightingColors

class CangJieSemanticTokensColorsProvider : DefaultSemanticTokensColorsProvider() {
    /**
     * 获取语法元素的颜色属性
     * 
     * LSP服务器返回的语义标记类型映射:
     * - "comment" -> 注释 (块注释、行注释、文档注释)
     * - "keyword" -> 关键字 (let, var, const, func, class 等)
     * - "string" -> 字符串字面量
     * - "number" -> 数字字面量
     * - "operator" -> 运算符 (+, -, *, / 等)
     * - "type" -> 类型别名定义
     * - "class" -> 以下情况都会返回 class:
     *   - 类型引用 (使用其他类型时)
     *   - 类定义
     *   - 接口定义
     *   - 结构体定义
     *   - 枚举定义
     * - "enum" -> 枚举定义
     * - "interface" -> 接口定义
     * - "struct" -> 结构体定义
     * - "member"/"function" -> 函数/方法声明和调用
     * - "parameter" -> 函数参数
     * - "typeParameter" -> 类型参数
     * - "property" -> 属性定义
     * - "variable" -> 变量定义和使用
     */
    override fun getTextAttributesKey(
        tokenType: String,
        tokenModifiers: List<String>,
        file: PsiFile
    ): TextAttributesKey? = when (tokenType) {
        "comment" -> CangJieHighlightingColors.BLOCK_COMMENT
        "keyword" -> CangJieHighlightingColors.KEYWORD
        "string" -> CangJieHighlightingColors.STRING
        "number" -> CangJieHighlightingColors.NUMBER
        "operator" -> CangJieHighlightingColors.OPERATOR_SIGN
        "type" -> CangJieHighlightingColors.TYPE_ALIAS
        "class" -> CangJieHighlightingColors.CLASS
        "enum" -> CangJieHighlightingColors.ENUM
        "interface" -> CangJieHighlightingColors.INTERFACE
        "struct" -> CangJieHighlightingColors.STRUCT
        "typeParameter" -> CangJieHighlightingColors.TYPE_PARAMETER
        "parameter" -> CangJieHighlightingColors.PARAMETER
        "property" -> if ("declaration" in tokenModifiers) {
            CangJieHighlightingColors.PROPERTY
        } else {
            CangJieHighlightingColors.INSTANCE_PROPERTY
        }

        "variable" -> if ("readonly" in tokenModifiers) {
            CangJieHighlightingColors.LOCAL_VARIABLE
        } else {
            CangJieHighlightingColors.MUTABLE_VARIABLE
        }

        "enumMember" -> CangJieHighlightingColors.ENUM_CONSTRUCTOR
        "function", "method" -> if ("declaration" in tokenModifiers) {
            CangJieHighlightingColors.FUNCTION_DECLARATION
        } else {
            CangJieHighlightingColors.FUNCTION_CALL
        }

        "macro" -> if ("declaration" in tokenModifiers) {
            CangJieHighlightingColors.MACRO_DECLARATION
        } else {
            CangJieHighlightingColors.MACRO_CALL
        }

        else -> super.getTextAttributesKey(tokenType, tokenModifiers, file)
    }


}
