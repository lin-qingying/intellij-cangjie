package com.huawei.cangjie1.doc.lexer

import com.huawei.cangjie1.lexer.CjToken
import org.jetbrains.annotations.NonNls


class CDocToken : CjToken {
    @Deprecated("")
    constructor(debugName: @NonNls String) : super(debugName)
    constructor(debugName: @NonNls String, tokenId: Int) : super(debugName, tokenId)
}

