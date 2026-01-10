/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.siblings
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
        prefix: CjToken = CjTokens.LPAR,
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
        prefix: CjToken = CjTokens.LPAR,
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
