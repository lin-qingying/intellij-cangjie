package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.descriptors.DiagnosticFactory
import com.huawei.cangjie.descriptors.DiagnosticFactoryForDeprecation
import com.huawei.cangjie.descriptors.Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import com.huawei.cangjie.ide.quickfix.overrideImplement.ImplementMembersHandler
import com.huawei.cangjie.lexer.CjTokens.ABSTRACT_KEYWORD
import com.intellij.codeInsight.intention.IntentionAction


class QuickFixRegistrar : QuickFixContributor {

    override fun registerQuickFixes(quickFixes: QuickFixes) {
        fun DiagnosticFactory<*>.registerFactory(vararg factory: QuickFixFactory) {
            quickFixes.register(this, *factory)
        }

        fun DiagnosticFactoryForDeprecation<*, *, *>.registerFactory(vararg factory: QuickFixFactory) {
            quickFixes.register(this.errorFactory, *factory)
            quickFixes.register(this.warningFactory, *factory)
        }

        fun DiagnosticFactory<*>.registerActions(vararg action: IntentionAction) {
            quickFixes.register(this, *action)
        }

//        添加 ABSTRACT_KEYWORD
        val addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD)
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)


//生成成员函数
        val implementMembersHandler = ImplementMembersHandler()
//        val implementMembersAsParametersHandler = ImplementAsConstructorParameter()
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerActions(implementMembersHandler/*, implementMembersAsParametersHandler*/)

    }
}
