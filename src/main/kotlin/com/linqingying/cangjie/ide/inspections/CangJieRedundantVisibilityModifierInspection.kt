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

package com.linqingying.cangjie.ide.inspections

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.config.AnalysisFlags
import com.linqingying.cangjie.config.ExplicitApiMode
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import com.linqingying.cangjie.ide.inspections.CangJieRedundantVisibilityModifierInspection.Holder.getRedundantVisibility
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.ide.quickfix.RemoveModifierFixBase
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens.*
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.resolve.caches.descriptor
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.isEffectivelyPublicApi
import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor


class CangJieRedundantVisibilityModifierInspection : AbstractCangJieInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {


        return declarationVisitor(fun(declaration: CjDeclaration) {
            val visibilityModifier = declaration.visibilityModifier() ?: return
            val redundantVisibility = getRedundantVisibility(declaration, skipGetters = true) ?: return
            holder.registerProblem(
                visibilityModifier,
                CangJieBundle.message("redundant.visibility.modifier"),
                IntentionWrapper(
                    RemoveModifierFixBase(
                        declaration,
                        redundantVisibility,
                        isRedundant = true
                    ).asIntention()
                )
            )
        })
    }


    object Holder {
        fun getRedundantVisibility(declaration: CjDeclaration, skipGetters: Boolean = false): CjModifierKeywordToken? {
            if (skipGetters && declaration is CjPropertyAccessor && declaration.isGetter) {
                // There is a quick fix for REDUNDANT_MODIFIER_IN_GETTER
                return null
            }

            val visibilityModifier = declaration.visibilityModifier() ?: return null

            if (isVisibilityNeededForExplicitApiMode(declaration)) return null
            val parent = declaration.getStrictParentOfType<CjTypeStatement>()

            val implicitVisibility = declaration.implicitVisibility()
            val redundantVisibility = when {
                visibilityModifier.node.elementType == implicitVisibility -> implicitVisibility
                declaration.hasModifier(INTERNAL_KEYWORD) && declaration.isInsideLocalOrPrivate() -> INTERNAL_KEYWORD

                parent is CjInterface && declaration.hasModifier(PUBLIC_KEYWORD) -> PUBLIC_KEYWORD
                else -> null
            } ?: return null

            if (redundantVisibility == INTERNAL_KEYWORD
                && declaration is CjProperty
                && declaration.hasModifier(OVERRIDE_KEYWORD)
                && declaration.isVar
                && declaration.setterVisibility().let { it != null && it != DescriptorVisibilities.INTERNAL }
            ) return null

            return redundantVisibility
        }

        private fun CjDeclaration.isInsideLocalOrPrivate(): Boolean =
            containingTypeStatement?.let { it.isLocal || it.isPrivate() } == true

        private fun isVisibilityNeededForExplicitApiMode(declaration: CjDeclaration): Boolean {
            val isExplicitApiMode =
                declaration.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) != ExplicitApiMode.DISABLED
            if (!isExplicitApiMode) return false
            return (declaration.resolveToDescriptorIfAny() as? DeclarationDescriptorWithVisibility)?.isEffectivelyPublicApi == true
        }

        private fun CjProperty.setterVisibility(): DescriptorVisibility? {
            val descriptor = descriptor as? PropertyDescriptor ?: return null
            if (setter?.visibilityModifier() != null) {
                val visibility = descriptor.setter?.visibility
                if (visibility != null) return visibility
            }
            return (descriptor as? CallableMemberDescriptor)
                ?.overriddenDescriptors
                ?.firstNotNullOfOrNull { (it as? PropertyDescriptor)?.setter }
                ?.visibility
        }
    }
}
