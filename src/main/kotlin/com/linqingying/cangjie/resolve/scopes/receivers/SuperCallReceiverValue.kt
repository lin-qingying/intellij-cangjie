package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.types.CangJieType


interface SuperCallReceiverValue : ReceiverValue {
    // This type is an actual receiver type used for invoke super-descriptor while ReceiverValue.type is some specific super-type of it
    val thisType: CangJieType
}
