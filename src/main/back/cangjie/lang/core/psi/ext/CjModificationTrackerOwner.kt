package com.huawei.cangjie.lang.core.psi.ext

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass
/**。
 *出于某种原因持有修改跟踪器的PSI元素。
 *这主要用于使缓存的类型推断结果无效。
 */
interface CjModificationTrackerOwner : CjElement {
    val modificationTracker: ModificationTracker


    fun incModificationCount(element: PsiElement): Boolean
}
fun PsiElement.findModificationTrackerOwner(strict: Boolean): CjModificationTrackerOwner? {
    return findContextOfTypeWithoutIndexAccess(
        strict,
        CjItemElement::class,
    ) as? CjModificationTrackerOwner
}
@Suppress("UNCHECKED_CAST")
private fun <T : PsiElement> PsiElement.findContextOfTypeWithoutIndexAccess(strict: Boolean, vararg classes: KClass<out T>): T? {
    var element = if (strict) contextWithoutIndexAccess else this

    while (element != null && !classes.any { it.isInstance(element) }) {
        element = element.contextWithoutIndexAccess
    }

    return element as T?
}

// 由于在PSI事件处理过程中访问索引速度较慢，所以必须在没有索引访问的情况下处理上下文
private val PsiElement.contextWithoutIndexAccess: PsiElement?
    get() =
//    if (this is CjExpandedElement) {
//        CjExpandedElement.getContextImpl(this, isIndexAccessForbidden = true)
//    } else {
        stubParent
//    }
