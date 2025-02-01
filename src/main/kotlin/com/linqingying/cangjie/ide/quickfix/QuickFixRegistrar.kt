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

package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.diagnostics.DiagnosticFactory
import com.linqingying.cangjie.diagnostics.DiagnosticFactoryForDeprecation
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.ide.quickfix.overrideImplement.ImplementMembersHandler
import com.intellij.codeInsight.intention.IntentionAction
import com.linqingying.cangjie.lexer.CjTokens.*


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
        NO_ELSE_IN_MATCH_BY_PATTERN.registerFactory(AddMatchElseBranchFix, AddMatchPatternRemainingBranchesFix)



//        添加 ABSTRACT_KEYWORD
        val addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD)
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.registerFactory(addAbstractModifierFactory)
//        添加 OVERRIDE_KEYWORD
        val addOverrideModifierFactory = AddModifierFix.createFactory(OVERRIDE_KEYWORD)
        val addRedefModifierFactory = AddModifierFix.createFactory(REDEF_KEYWORD)

        VIRTUAL_MEMBER_HIDDEN.registerFactory(addOverrideModifierFactory,addRedefModifierFactory)

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
        REDEF_NOTHING_TO_OVERRIDE.registerFactory(
            RemoveModifierFixBase.createRemoveModifierFromListOwnerPsiBasedFactory(REDEF_KEYWORD),
            ChangeMemberFunctionSignatureFix,
            AddFunctionToSupertypeFix,
//            AddPropertyToSupertypeFix
        )

        REDEF_INSTANCE_ERROR.registerFactory(
            RemoveModifierFixBase.createRemoveModifierFromListOwnerPsiBasedFactory(REDEF_KEYWORD),
        )
        OVERRIDE_STATIC_ERROR.registerFactory(
            RemoveModifierFixBase.createRemoveModifierFromListOwnerPsiBasedFactory(OVERRIDE_KEYWORD),
        )

        REDUNDANT_OPTIONAL.registerFactory(RemoveOptionalFix.removeForRedundant)
        NESTING_DOLL_OPTINOTYPE.registerFactory(RemoveOptionalFix.removeForRedundantDoll)

        TYPE_MISMATCH_MULTIPLE_SUPERTYPES.registerFactory(SpecifyTypeExplicitlyFix.SpecifyTypeExplicitlyFixFactory)
        IMPLICIT_INTERSECTION_TYPE.registerFactory(SpecifyTypeExplicitlyFix.SpecifyTypeExplicitlyFixFactory)

    }
}
