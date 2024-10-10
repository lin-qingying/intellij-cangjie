package com.huawei.cangjie.ide.inspections

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.config.AnalysisFlags
import com.huawei.cangjie.config.ExplicitApiMode
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import com.huawei.cangjie.ide.inspections.CangJieRedundantVisibilityModifierInspection.Holder.getRedundantVisibility
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.ide.quickfix.RemoveModifierFixBase
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.resolve.caches.descriptor
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.isEffectivelyPublicApi
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
