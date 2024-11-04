package com.linqingying.cangjie.name

import com.linqingying.cangjie.psi.psiUtil.quoteIfNeeded

fun FqName.parentOrNull(): FqName? = if (this.isRoot) null else parent()

fun FqName.quoteIfNeeded(): FqName {
    return FqName(pathSegments().joinToString(".") { it.asString().quoteIfNeeded() })
}
fun FqName.isOneSegmentFQN(): Boolean = !isRoot && parent().isRoot
fun FqName.isChildOf(packageName: FqName): Boolean = parentOrNull() == packageName
