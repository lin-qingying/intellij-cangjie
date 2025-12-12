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

package org.cangnova.cangjie.resolve.check

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticSink

import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionExpressionDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.caches.DeclarationChecker
import org.cangnova.cangjie.resolve.caches.DeclarationCheckerContext
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.infos.errors.UNDERSCORE_IS_RESERVED
import org.cangnova.cangjie.diagnostics.infos.errors.UNSUPPORTED_FEATURE


object UnderscoreChecker : DeclarationChecker {
    /**
     * 检查标识符中的下划线使用是否合法
     *
     * 下划线命名规则：
     * 1. **多个下划线**（如 `__`, `___`）始终是错误的，在任何上下文中都保留
     * 2. **单个下划线** `_` 只在特定上下文中允许：
     *    - Lambda/匿名函数参数中可以使用（需要语言特性支持）
     *    - 其他位置（类名、函数名、变量名等）不允许
     *
     * @param identifier 要检查的标识符 PSI 元素
     * @param diagnosticHolder 诊断信息收集器，用于报告错误
     * @param languageVersionSettings 语言版本设置，用于检查特性是否启用
     * @param allowSingleUnderscore 是否允许单个下划线（仅在 Lambda 参数等特殊上下文中为 true）
     *
     * ## 检查逻辑：
     * 1. 如果标识符为空，跳过检查
     * 2. 判断是否为"有效的单下划线"：必须同时满足
     *    - `allowSingleUnderscore = true`（在允许的上下文中）
     *    - 标识符文本恰好是 `"_"`
     * 3. 如果不是有效的单下划线，且所有字符都是下划线
     *    → 报告 **UNDERSCORE_IS_RESERVED** 错误
     * 4. 如果是有效的单下划线，但语言版本不支持 SingleUnderscoreForParameterName 特性
     *    → 报告 **UNSUPPORTED_FEATURE** 错误
     *
     * ## 示例：
     * ```
     * // ✅ 允许（Lambda 中，且特性已启用）
     * list.forEach { _ -> println("处理") }
     *
     * // ❌ 错误：多个下划线始终保留
     * val __ = 10  // Error: UNDERSCORE_IS_RESERVED
     *
     * // ❌ 错误：在非 Lambda 上下文中使用单下划线
     * val _ = 10  // Error: UNDERSCORE_IS_RESERVED
     *
     * // ❌ 错误：Lambda 中使用，但语言版本不支持
     * list.forEach { _ -> ... }  // Error: UNSUPPORTED_FEATURE
     * ```
     */
    @JvmOverloads
    fun checkIdentifier(
        identifier: PsiElement?,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings,
        allowSingleUnderscore: Boolean = false
    ) {
        // 1. 空检查：标识符为 null 或空字符串则跳过
        if (identifier == null || identifier.text.isEmpty()) return

        // 2. 判断是否为有效的单下划线
        //    条件：允许单下划线（Lambda 参数等）且标识符恰好是 "_"
        val isValidSingleUnderscore = allowSingleUnderscore && identifier.text == "_"

        // 3. 检查规则 1：禁止多个下划线或非法的单下划线
        //    如果不是有效的单下划线，且所有字符都是下划线（_, __, ___, ...）
        //    则报告错误：下划线是保留的
        if (!isValidSingleUnderscore && identifier.text.all { it == '_' }) {
            diagnosticHolder.report(UNDERSCORE_IS_RESERVED.on(identifier))
        }
        // 4. 检查规则 2：单下划线需要语言特性支持
        //    如果是有效的单下划线，但当前语言版本不支持该特性
        //    则报告错误：不支持的特性
        else if (isValidSingleUnderscore && !languageVersionSettings.supportsFeature(LanguageFeature.SingleUnderscoreForParameterName)) {
            diagnosticHolder.report(
                UNSUPPORTED_FEATURE.on(
                    identifier,
                    LanguageFeature.SingleUnderscoreForParameterName to languageVersionSettings
                )
            )
        }
    }

    @JvmOverloads
    fun checkNamed(
        declaration: CjNamedDeclaration,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings,
        allowSingleUnderscore: Boolean = false
    ) {
        checkIdentifier(declaration.nameIdentifier, diagnosticHolder, languageVersionSettings, allowSingleUnderscore)
    }

    override fun check(
        declaration: CjDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration is CjProperty && descriptor !is VariableDescriptor) return
        if (declaration is CjCallableDeclaration) {
            for (parameter in declaration.valueParameters) {
                checkNamed(
                    parameter, context.trace, context.languageVersionSettings,
                    allowSingleUnderscore = descriptor is FunctionExpressionDescriptor
                )
            }
        }
        if (declaration is CjTypeParameterListOwner) {
            for (typeParameter in declaration.typeParameters) {
                checkNamed(typeParameter, context.trace, context.languageVersionSettings)
            }
        }
        if (declaration !is CjNamedDeclaration) return
        checkNamed(declaration, context.trace, context.languageVersionSettings)
    }
}
