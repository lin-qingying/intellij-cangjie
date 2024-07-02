package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.types.CangJieType


interface SuperCallReceiverValue : ReceiverValue {
    // This type is an actual receiver type used for invoke super-descriptor while ReceiverValue.type is some specific super-type of it
    val thisType: CangJieType
}
