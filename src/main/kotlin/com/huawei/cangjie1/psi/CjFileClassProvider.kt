package com.huawei.cangjie1.psi

import com.intellij.psi.PsiClass

interface CjFileClassProvider {
    fun getFileClasses(file: CjFile): Array<PsiClass>
}


class  CjFileClassProviderImpl : CjFileClassProvider {
    override fun getFileClasses(file: CjFile): Array<PsiClass> {

 TODO("CjFileClassProviderImpl.getFileClasses is not implemented1   ")
        return PsiClass.EMPTY_ARRAY
    }
}
