package com.huawei.cangjie.lang.doc.psi.ext

import com.huawei.cangjie.lang.doc.psi.CjDocElementTypes
import com.intellij.psi.tree.IElementType

val IElementType.isDocCommentLeafToken: Boolean
    get() = this == CjDocElementTypes.DOC_GAP || this == CjDocElementTypes.DOC_DATA
