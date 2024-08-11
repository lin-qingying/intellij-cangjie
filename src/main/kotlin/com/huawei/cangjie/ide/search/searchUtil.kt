package com.huawei.cangjie.ide.search

import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.utils.getNonStrictParentOfType
import com.intellij.psi.PsiReference


fun PsiReference.isImportUsage(): Boolean =
    element.getNonStrictParentOfType<CjImportDirective>() != null

