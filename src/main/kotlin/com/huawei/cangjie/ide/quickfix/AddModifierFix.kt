package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.inspections.CangJieUniversalQuickFix
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.containingClass
import com.huawei.cangjie.psi.psiUtil.visibilityModifierType
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

open class AddModifierFix(
    element: CjModifierListOwner,
    @SafeFieldForPreview
    protected val modifier: CjModifierKeywordToken
) : CangJieCrossLanguageQuickFixAction<CjModifierListOwner>(element), CangJieUniversalQuickFix {
    interface Factory<T : AddModifierFix> {
        fun createFactory(modifier: CjModifierKeywordToken): QuickFixesPsiBasedFactory<PsiElement> {
            return createFactory(modifier, CjModifierListOwner::class.java)
        }

        fun <T : CjModifierListOwner> createFactory(
            modifier: CjModifierKeywordToken,
            modifierOwnerClass: Class<T>
        ): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { e ->
                val modifierListOwner =
                    PsiTreeUtil.getParentOfType(e, modifierOwnerClass, false)
                        ?: return@quickFixesPsiBasedFactory emptyList()
                listOfNotNull(createIfApplicable(modifierListOwner, modifier))
            }
        }

        fun createIfApplicable(modifierListOwner: CjModifierListOwner, modifier: CjModifierKeywordToken): T? {
            when (modifier) {
                CjTokens.ABSTRACT_KEYWORD, CjTokens.OPEN_KEYWORD -> {

//                    if (modifierListOwner is CjEnumEntry) return null
//                    if (modifierListOwner is CjDeclaration && modifierListOwner !is CjClass) {
//                        val parentClassOrObject = modifierListOwner.containingClassOrObject ?: return null

//                        if (parentClassOrObject is CjEnumEntry) return null
//                    }
                    if (modifier == CjTokens.ABSTRACT_KEYWORD
                        && modifierListOwner is CjClass

                    ) return null
                }

            }
            return createModifierFix(modifierListOwner, modifier)
        }

        fun createModifierFix(element: CjModifierListOwner, modifier: CjModifierKeywordToken): T
    }

    companion object : Factory<AddModifierFix> {
        val addAbstractModifier = AddModifierFix.createFactory(CjTokens.ABSTRACT_KEYWORD)
        val addAbstractToContainingClass =
            AddModifierFix.createFactory(CjTokens.ABSTRACT_KEYWORD, CjTypeStatement::class.java)
        val addOpenToContainingClass = AddModifierFix.createFactory(CjTokens.OPEN_KEYWORD, CjTypeStatement::class.java)

        val addOverrideModifier = createFactory(CjTokens.OVERRIDE_KEYWORD)


        val modifiersWithWarning: Set<CjModifierKeywordToken> = setOf(CjTokens.ABSTRACT_KEYWORD)
        private val modalityModifiers = modifiersWithWarning + CjTokens.OPEN_KEYWORD

        override fun createModifierFix(element: CjModifierListOwner, modifier: CjModifierKeywordToken): AddModifierFix =
            AddModifierFix(element, modifier)
    }

    protected fun invokeOnElement(element: CjModifierListOwner?) {
        if (element == null) return

        element.addModifier(modifier)

        when (modifier) {
            CjTokens.ABSTRACT_KEYWORD -> {
                if (element is CjProperty || element is CjNamedFunction) {
                    element.containingClass()?.let { cclass ->
                        if (!cclass.hasModifier(CjTokens.ABSTRACT_KEYWORD) && !cclass.hasModifier(CjTokens.SEALED_KEYWORD)) {
                            cclass.addModifier(CjTokens.ABSTRACT_KEYWORD)
                        }
                    }
                }
            }

            CjTokens.OVERRIDE_KEYWORD -> {
                val visibility = element.visibilityModifierType()?.takeIf { it != CjTokens.PUBLIC_KEYWORD }
                visibility?.let { element.removeModifier(it) }
            }
//
//            CjTokens.NOINLINE_KEYWORD ->
//                element.removeModifier(CjTokens.CROSSINLINE_KEYWORD)
        }
    }

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        val originalElement = element
        invokeOnElement(originalElement)
    }

    override fun getFamilyName() = CangJieBundle.message("fix.add.modifier.family")


    override fun getText(): String {
        val element = element ?: return ""
        if (modifier in modalityModifiers || modifier in CjTokens.VISIBILITY_MODIFIERS || modifier == CjTokens.CONST_KEYWORD) {
            return CangJieBundle.message(
                "fix.add.modifier.text",
                RemoveModifierFixBase.getElementName(element),
                modifier.value
            )
        }
        return CangJieBundle.message("fix.add.modifier.text.generic", modifier.value)
    }
}
