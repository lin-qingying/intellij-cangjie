package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.create.Crate
import com.huawei.cangjie.stdext.withPrevious
import com.intellij.psi.PsiElement



enum class CjCodeStatus {

    CFG_DISABLED,


    CFG_UNKNOWN,


    ATTR_PROC_MACRO_CALL,


    CODE
}


