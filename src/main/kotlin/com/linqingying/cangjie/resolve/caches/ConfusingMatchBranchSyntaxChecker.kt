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
