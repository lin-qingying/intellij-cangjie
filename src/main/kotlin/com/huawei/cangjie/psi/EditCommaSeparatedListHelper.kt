package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.siblings
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace



object EditCommaSeparatedListHelper {
    @JvmOverloads
    fun <TItem : CjElement> addItem(list: CjElement, allItems: List<TItem>, item: TItem, prefix: CjToken = CjTokens.LPAR): TItem {
        return addItemBefore(list, allItems, item, null, prefix)
    }

    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun <TItem : CjElement> addItemAfter(
        list: CjElement,
        allItems: List<TItem>,
        item: TItem,
        anchor: TItem?,
        prefix: CjToken = CjTokens.LPAR
    ): TItem {
        assert(anchor == null || anchor.parent == list)
        if (allItems.isEmpty()) {
            return if (list.firstChild?.node?.elementType == prefix) {
                list.addAfter(item, list.firstChild) as TItem
            } else {
                list.add(item) as TItem
            }
        } else {
            var comma = CjPsiFactory(list.project).createComma()
            return if (anchor != null) {
                comma = list.addAfter(comma, anchor)
                list.addAfter(item, comma) as TItem
            } else {
                comma = list.addBefore(comma, allItems.first())
                list.addBefore(item, comma) as TItem
            }
        }
    }

    @JvmOverloads
    fun <TItem : CjElement> addItemBefore(
        list: CjElement,
        allItems: List<TItem>,
        item: TItem,
        anchor: TItem?,
        prefix: CjToken = CjTokens.LPAR
    ): TItem {
        val anchorAfter: TItem?
        anchorAfter = if (allItems.isEmpty()) {
            assert(anchor == null)
            null
        } else {
            if (anchor != null) {
                val index = allItems.indexOf(anchor)
                assert(index >= 0)
                if (index > 0) allItems[index - 1] else null
            } else {
                allItems[allItems.size - 1]
            }
        }
        return addItemAfter(list, allItems, item, anchorAfter, prefix)
    }

    fun <TItem : CjElement> removeItem(item: TItem) {
        var comma = item.siblings(withItself = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        if (comma?.node?.elementType != CjTokens.COMMA) {
            comma = item.siblings(forward = false, withItself = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
            if (comma?.node?.elementType != CjTokens.COMMA) {
                comma = null
            }
        }

        item.delete()
        comma?.delete()
    }
}
