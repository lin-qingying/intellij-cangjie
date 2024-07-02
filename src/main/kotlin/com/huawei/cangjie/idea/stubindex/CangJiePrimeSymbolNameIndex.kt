package com.huawei.cangjie.idea.stubindex

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJiePrimeSymbolNameIndex internal constructor() : StringStubIndexExtension<NavigatablePsiElement>() {
    companion object Helper : CangJieStringStubIndexHelper<NavigatablePsiElement>(NavigatablePsiElement::class.java) {
        override val indexKey: StubIndexKey<String, NavigatablePsiElement> = StubIndexKey.createIndexKey("cangjie.primeIndexKey")
    }

    override fun getKey(): StubIndexKey<String, NavigatablePsiElement> = indexKey

    override fun getVersion(): Int {
        return super.getVersion() + 0
    }
}