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

package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.lexer.CjTokens.*
import com.linqingying.cangjie.psi.*
import com.intellij.psi.tree.TokenSet


object ConfusingMatchBranchSyntaxChecker {
    private val prohibitedTokens = TokenSet.create(
        IN_KEYWORD,
        LT, LTEQ, GT, GTEQ,
        ANDAND, OROR
    )

    fun check(whenExpression: CjMatchExpression, languageVersionSettings: LanguageVersionSettings, trace: BindingTrace) {
        if (whenExpression.subjectExpression == null   ) return
        for (entry in whenExpression.entries) {
            for (condition in entry.conditions) {
                checkCondition(condition, languageVersionSettings, trace)
            }
        }
    }

    private fun checkCondition(condition: CjCasePattern, languageVersionSettings: LanguageVersionSettings, trace: BindingTrace) {
        when (condition) {
            is CjMatchConditionWithExpression -> checkConditionExpression(condition.expression, languageVersionSettings, trace)

        }
    }

    private fun checkConditionExpression(rawExpression: CjExpression?, languageVersionSettings: LanguageVersionSettings, trace: BindingTrace) {
        if (rawExpression == null) return
        if (rawExpression is CjParenthesizedExpression) return
        val shouldReport = when (val expression = CjPsiUtil.safeDeparenthesize(rawExpression)) {
            is CjIsExpression -> true
            is CjBinaryExpression -> expression.operationToken in prohibitedTokens
            else -> false
        }

    }
}
