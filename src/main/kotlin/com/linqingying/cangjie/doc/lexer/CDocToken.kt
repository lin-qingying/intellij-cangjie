package com.linqingying.cangjie.doc.lexer

import com.linqingying.cangjie.lexer.CjToken
import org.jetbrains.annotations.NonNls


class CDocToken : CjToken {
    @Deprecated("")
    constructor(debugName: @NonNls String) : super(debugName)
    constructor(debugName: @NonNls String, tokenId: Int) : super(debugName, tokenId)
}

