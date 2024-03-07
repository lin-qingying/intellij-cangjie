package com.huawei.cangjie.lexer

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.psi.tree.IElementType

open class CjToken : IElementType {


    var tokenId: Int


    companion object {
        private const val INVALID_ID = -1
    }

    constructor(debugName: String) : this(debugName, INVALID_ID)

    constructor(debugName: String, tokenid: Int) : super(debugName, CangJieLanguage) {
        tokenId = tokenid
    }


}


