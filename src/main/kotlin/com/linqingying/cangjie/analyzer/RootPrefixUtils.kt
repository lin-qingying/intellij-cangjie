package com.linqingying.cangjie.analyzer

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.parentOrNull
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.psiUtil.getParentOfTypes2
import com.linqingying.cangjie.resolve.QualifiedExpressionResolver

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
