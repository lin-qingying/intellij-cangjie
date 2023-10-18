package com.huawei.cangjie1.lexer

import com.huawei.cangjie1.lang.CangJieLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

open class CjToken : IElementType {

     var tokenId = 0


    companion object {
        private const val INVALID_ID = -1
    }

    constructor(debugName: String) : this(debugName, INVALID_ID)

    constructor(debugName: String, tokenid: Int) : super(debugName, CangJieLanguage) {
        tokenId = tokenid
    }


}


