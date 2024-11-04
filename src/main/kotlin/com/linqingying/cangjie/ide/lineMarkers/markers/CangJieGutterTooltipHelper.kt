package com.linqingying.cangjie.ide.lineMarkers.markers

import com.linqingying.cangjie.highlighter.unwrapped
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.containingClass
import com.linqingying.cangjie.utils.module
import com.intellij.codeInsight.daemon.impl.GutterTooltipBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil


object CangJieGutterTooltipHelper : GutterTooltipBuilder() {
    override fun getLinkProtocol(): String {
        return "kotlinClass"
    }

    override fun shouldSkipAsFirstElement(element: PsiElement): Boolean {
        return element.unwrapped is CjCallableDeclaration
    }

    override fun getLinkReferenceText(element: PsiElement): String? {
        val moduleName = element.module?.name?.let { "$it:" } ?: ""
        val qualifiedName = when (val el = element.unwrapped) {

            is CjTypeStatement -> el.fqName?.asString()
            else -> PsiTreeUtil.getStubOrPsiParentOfType(el, CjClass::class.java)?.fqName?.asString()
        } ?: return null
        return moduleName + qualifiedName
    }

    override fun getContainingElement(element: PsiElement): PsiElement? {
        val unwrapped = element.unwrapped


        var member: CjDeclaration?
        if (unwrapped is CjParameter) {
            member = PsiTreeUtil.getStubOrPsiParentOfType(unwrapped, CjClass::class.java)
        }
        else {
            member = PsiTreeUtil.getStubOrPsiParentOfType(unwrapped, CjDeclaration::class.java)
        }
        if (member == null && unwrapped is CjDeclaration) {
            member = unwrapped.containingClass()
        }
        return member ?: unwrapped?.containingFile
    }

    override fun getLocationString(element: PsiElement): String? {
        val classOrObject = element.unwrapped as? CjTypeStatement ?: return null
        val moduleName = element.module?.name ?: return null
//        val moduleNameRequired = classOrObject.hasActualModifier() || classOrObject.isExpectDeclaration()
//        return if (moduleNameRequired) " [$moduleName]" else null
        return " [$moduleName]"
    }

    override fun getPresentableName(element: PsiElement): String? {
        return (element as? PsiNamedElement)?.name
    }




}
