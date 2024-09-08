package com.huawei.cangjie.analyzer

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.parentOrNull
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.psi.psiUtil.getParentOfTypes2
import com.huawei.cangjie.resolve.QualifiedExpressionResolver

fun FqName.withRootPrefixIfNeeded(targetElement: CjElement? = null): FqName {
    if (canAddRootPrefix() && targetElement?.canAddRootPrefix() != false) {
        return FqName(QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT + asString())
    }

    return this
}
fun FqName.canAddRootPrefix(): Boolean {
    return !asString().startsWith(QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT)
            && parentOrNull()?.isRoot == false
}
fun CjElement.canAddRootPrefix(): Boolean {
    return getParentOfTypes2<CjImportDirective, CjPackageDirective>() == null
}
