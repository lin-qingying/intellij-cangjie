package com.huawei.cangjie.psi.dummpholder

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.util.CharTable

class CangJieDummyHolder : DummyHolder {

    val a = 1

    constructor(
        manager: PsiManager,
        contentElement: TreeElement?,
        context: PsiElement?
    ) : super(manager, contentElement, context, null, null, language(context, CangJieLanguage))


    constructor(
        manager: PsiManager,
        table: CharTable?,
        validity: Boolean
    ) : super(manager, null, null, table, validity, CangJieLanguage)

    constructor(manager: PsiManager, context: PsiElement?) : super(
        manager,
        null,
        context,
        null,
        null,
        language(context, CangJieLanguage)
    )

    constructor(manager: PsiManager, contentElement: TreeElement?, context: PsiElement?, table: CharTable?) : super(
        manager,
        contentElement,
        context,
        table,
        null,
        language(context, CangJieLanguage)
    )

    constructor(manager: PsiManager, context: PsiElement?, table: CharTable?) : super(
        manager,
        null,
        context,
        table,
        null,
        language(context, CangJieLanguage)
    )

    constructor(manager: PsiManager, table: CharTable?) : super(manager, null, null, table, null, CangJieLanguage)


    override fun getFileElementType(): IFileElementType? {


        return super.getFileElementType()
    }
}



