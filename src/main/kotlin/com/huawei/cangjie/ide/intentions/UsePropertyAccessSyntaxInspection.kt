package com.huawei.cangjie.ide.intentions

import com.huawei.cangjie.NotPropertiesService
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjCallableReferenceExpression
import com.huawei.cangjie.psi.CjExpression
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class NotPropertiesServiceImpl(private val project: Project) : NotPropertiesService {
    override fun getNotProperties(element: PsiElement): Set<FqNameUnsafe> {
        return emptySet()
    }

}

private val commonGetterLikePrefixes: Set<Regex> = setOf(
    "^getOr[A-Z]".toRegex(),
    "^getAnd[A-Z]".toRegex(),
    "^getIf[A-Z]".toRegex(),
)

private inline fun <T> CjExpression.callOrReferenceOrNull(
    call: (CjCallExpression) -> T,
    reference: (CjCallableReferenceExpression) -> T
): T? =
    when (this) {
        is CjCallExpression -> call(this)
        is CjCallableReferenceExpression -> reference(this)
        else -> null
    }
