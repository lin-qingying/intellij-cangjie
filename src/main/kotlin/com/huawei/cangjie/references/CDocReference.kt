package com.huawei.cangjie.references

import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.name.Name
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


abstract class CDocReference(element: CDocName) : CjMultiReference<CDocName>(element) {
    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun getCanonicalText(): String = element.getNameText()

    override val resolvesByNames: Collection<Name> get() = listOf(Name.identifier(element.getNameText()))
}
