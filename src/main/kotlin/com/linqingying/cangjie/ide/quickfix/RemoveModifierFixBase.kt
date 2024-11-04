package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.codeInsight.daemon.impl.actions.IntentionActionWithFixAllOption
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nls

open class RemoveModifierFixBase(
    element: CjModifierListOwner,
    @FileModifier.SafeFieldForPreview
    private val modifier: CjModifierKeywordToken,
    private val isRedundant: Boolean
) : CangJieCrossLanguageQuickFixAction<CjModifierListOwner>(element), IntentionActionWithFixAllOption {

    @Nls
    private val text = run {
        val modifierText = modifier.value
        when {
            isRedundant ->
                CangJieBundle.message("remove.redundant.0.modifier", modifierText)
            modifier === CjTokens.ABSTRACT_KEYWORD || modifier === CjTokens.OPEN_KEYWORD ->
                CangJieBundle.message("make.0.not.1", getElementName(element), modifierText)
            else ->
                CangJieBundle.message("remove.0.modifier", modifierText, modifier)
        }
    }

    override fun getFamilyName() = CangJieBundle.message("remove.modifier")

    override fun getText() = text

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile) = element?.hasModifier(modifier) == true

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        invoke()
    }

    operator fun invoke() {
        element?.removeModifier(modifier)
    }

    companion object {
        val removeRedundantModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFactory(isRedundant = true)
        val removeNonRedundantModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFactory(isRedundant = false)
        val removeAbstractModifier: QuickFixesPsiBasedFactory<PsiElement> =
            createRemoveModifierFromListOwnerPsiBasedFactory(CjTokens.ABSTRACT_KEYWORD)
        val removeOpenModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFromListOwnerPsiBasedFactory(CjTokens.OPEN_KEYWORD)

        val removePrivateModifier: QuickFixesPsiBasedFactory<PsiElement> = createRemoveModifierFromListOwnerPsiBasedFactory(CjTokens.PRIVATE_KEYWORD)

        fun createRemoveModifierFromListOwnerPsiBasedFactory(
            modifier: CjModifierKeywordToken,
            isRedundant: Boolean = false
        ): QuickFixesPsiBasedFactory<PsiElement> =
            createRemoveModifierFromListOwnerFactoryByModifierListOwner(
                modifier,
                isRedundant
            ).coMap { PsiTreeUtil.getParentOfType(it, CjModifierListOwner::class.java, false) }


        private fun createRemoveModifierFromListOwnerFactoryByModifierListOwner(
            modifier: CjModifierKeywordToken,
            isRedundant: Boolean = false
        ) = quickFixesPsiBasedFactory<CjModifierListOwner> {
            listOf(RemoveModifierFixBase(it, modifier, isRedundant))
        }

        private fun createRemoveModifierFactory(isRedundant: Boolean = false): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val elementType = psiElement.node.elementType as? CjModifierKeywordToken ?: return@quickFixesPsiBasedFactory emptyList()
                val modifierListOwner = psiElement.getStrictParentOfType<CjModifierListOwner>()
                    ?: return@quickFixesPsiBasedFactory emptyList()
                listOf(RemoveModifierFixBase(modifierListOwner, elementType, isRedundant))
            }
        }


        fun createRemoveProjectionFactory(isRedundant: Boolean): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                val projection = psiElement as CjTypeProjection
                val elementType = projection.projectionToken?.node?.elementType as? CjModifierKeywordToken
                    ?: return@quickFixesPsiBasedFactory listOf()
                listOf(RemoveModifierFixBase(projection, elementType, isRedundant))
            }
        }

        fun createRemoveVarianceFactory(): QuickFixesPsiBasedFactory<PsiElement> {
            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
                require(psiElement is CjTypeParameter)
                val modifier = when (psiElement.variance) {
//                    Variance.IN_VARIANCE -> CjTokens.IN_KEYWORD
//                    Variance.OUT_VARIANCE -> CjTokens.OUT_KEYWORD
                    else -> return@quickFixesPsiBasedFactory emptyList()
                }
                listOf(RemoveModifierFixBase(psiElement, modifier, isRedundant = false))
            }
        }

//        fun createRemoveSuspendFactory(): QuickFixesPsiBasedFactory<PsiElement> {
//            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
//                val modifierList = psiElement.parent as CjDeclarationModifierList
//                val type = modifierList.parent as CjTypeReference
//                if (!type.hasModifier(CjTokens.SUSPEND_KEYWORD)) return@quickFixesPsiBasedFactory emptyList()
//                listOf(RemoveModifierFixBase(type, CjTokens.SUSPEND_KEYWORD, isRedundant = false))
//            }
//        }
//
//        fun createRemoveLateinitFactory(): QuickFixesPsiBasedFactory<PsiElement> {
//            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
//                val property = psiElement as? CjProperty ?: return@quickFixesPsiBasedFactory emptyList()
//                if (!property.hasModifier(CjTokens.LATEINIT_KEYWORD)) return@quickFixesPsiBasedFactory emptyList()
//                listOf(RemoveModifierFixBase(property, CjTokens.LATEINIT_KEYWORD, isRedundant = false))
//            }
//        }

//        fun createRemoveFunFromInterfaceFactory(): QuickFixesPsiBasedFactory<PsiElement> {
//            return quickFixesPsiBasedFactory { psiElement: PsiElement ->
//                val modifierList = psiElement.parent as? CjDeclarationModifierList
//                    ?: return@quickFixesPsiBasedFactory emptyList()
//                val funInterface = (modifierList.parent as? CjClass)?.takeIf {
//                    it.isInterface() && it.hasModifier(CjTokens.FUN_KEYWORD)
//                } ?: return@quickFixesPsiBasedFactory emptyList()
//                listOf(RemoveModifierFixBase(funInterface, CjTokens.FUNC_KEYWORD, isRedundant = false))
//            }
//        }

        fun getElementName(modifierListOwner: CjModifierListOwner): String {
            var name: String? = null
            if (modifierListOwner is PsiNameIdentifierOwner) {
                val nameIdentifier = modifierListOwner.nameIdentifier
                if (nameIdentifier != null) {
                    name = nameIdentifier.text
                }
            } else if (modifierListOwner is CjPropertyAccessor) {
                name = modifierListOwner.namePlaceholder.text
            }
            if (name == null) {
                name = modifierListOwner.text
            }
            return "'$name'"
        }
    }
}
