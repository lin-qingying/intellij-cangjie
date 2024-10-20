package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.diagnostics.DiagnosticFactory
import com.huawei.cangjie.diagnostics.DiagnosticFactoryForDeprecation
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.ide.quickfix.overrideImplement.ImplementMembersHandler
import com.huawei.cangjie.lexer.CjTokens.ABSTRACT_KEYWORD
import com.huawei.cangjie.lexer.CjTokens.OVERRIDE_KEYWORD
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
        EXPLICIT_DELEGATION_CALL_REQUIRED.registerFactory(InsertDelegationCallQuickfix.InsertThisDelegationCallFactory)
        EXPLICIT_DELEGATION_CALL_REQUIRED.registerFactory(InsertDelegationCallQuickfix.InsertSuperDelegationCallFactory)



        UNRESOLVED_REFERENCE.registerFactory(ImportFix)
        UNRESOLVED_REFERENCE.registerFactory(ImportConstructorReferenceFix)


        NO_ELSE_IN_MATCH.registerFactory(AddMatchElseBranchFix, AddMatchRemainingBranchesFix)
        NO_ELSE_IN_MATCH_WARNING.registerFactory(AddMatchElseBranchFix, AddMatchRemainingBranchesFix)

//        添加 ABSTRACT_KEYWORD
        val addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD)
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)
//        添加 OVERRIDE_KEYWORD
        val addOverrideModifierFactory = AddModifierFix.createFactory(OVERRIDE_KEYWORD)
        VIRTUAL_MEMBER_HIDDEN.registerFactory(addOverrideModifierFactory)

//生成成员函数
        val implementMembersHandler = ImplementMembersHandler()
//        val implementMembersAsParametersHandler = ImplementAsConstructorParameter()
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerActions(implementMembersHandler/*, implementMembersAsParametersHandler*/)
        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.registerActions(implementMembersHandler/*, implementMembersAsParametersHandler*/)


        NOTHING_TO_OVERRIDE.registerFactory(
            RemoveModifierFixBase.createRemoveModifierFromListOwnerPsiBasedFactory(OVERRIDE_KEYWORD),
            ChangeMemberFunctionSignatureFix,
            AddFunctionToSupertypeFix,
//            AddPropertyToSupertypeFix
        )




        REDUNDANT_OPTIONAL.registerFactory(RemoveOptionalFix.removeForRedundant)
        NESTING_DOLL_OPTINOTYPE.registerFactory(RemoveOptionalFix.removeForRedundantDoll)

    }
}
