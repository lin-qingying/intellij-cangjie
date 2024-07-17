package com.huawei.cangjie.psi.dummpholder

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.psi.impl.source.HolderFactory
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.CharTable

class CangJieDummyHolderFactory: HolderFactory {
    override fun createHolder(manager: PsiManager, contentElement: TreeElement?, context: PsiElement?): DummyHolder {
        return CangJieDummyHolder(manager, contentElement, context)
    }

    override fun createHolder(manager: PsiManager, table: CharTable?, validity: Boolean): DummyHolder {
        return CangJieDummyHolder(manager, table, validity)
    }

    override fun createHolder(manager: PsiManager, context: PsiElement?): DummyHolder {
        return CangJieDummyHolder(manager, context)
    }

    override fun createHolder(manager: PsiManager, language: Language?, context: PsiElement?): DummyHolder {
        return if (language ===CangJieLanguage) CangJieDummyHolder(manager, context) else DummyHolder(
            manager,
            language,
            context
        )

    }

    override fun createHolder(
        manager: PsiManager,
        contentElement: TreeElement?,
        context: PsiElement?,
        table: CharTable?
    ): DummyHolder {
        return CangJieDummyHolder(manager, contentElement, context, table)
    }

    override fun createHolder(manager: PsiManager, context: PsiElement?, table: CharTable?): DummyHolder {
        return CangJieDummyHolder(manager, context, table)
    }

    override fun createHolder(manager: PsiManager, table: CharTable?, language: Language?): DummyHolder {

        return CangJieDummyHolder(manager,table)
    }
}
