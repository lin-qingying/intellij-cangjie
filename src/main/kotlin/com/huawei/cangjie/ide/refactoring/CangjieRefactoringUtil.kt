package com.huawei.cangjie.ide.refactoring

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.psiUtil.isIdentifier
import com.huawei.cangjie.psi.psiUtil.quoteIfNeeded

fun FqName.hasIdentifiersOnly(): Boolean = pathSegments().all { it.asString().quoteIfNeeded().isIdentifier() }
