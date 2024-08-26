package com.huawei.cangjie.name

import com.huawei.cangjie.psi.psiUtil.quoteIfNeeded

fun FqName.parentOrNull(): FqName? = if (this.isRoot) null else parent()

fun FqName.quoteIfNeeded(): FqName {
    return FqName(pathSegments().joinToString(".") { it.asString().quoteIfNeeded() })
}
