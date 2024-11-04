package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.types.CangJieType

/**
 * This represents the receiver of hasNext and next() in for-loops
 * Cannot be an expression receiver because there is no expression for the iterator() call
 */
class TransientReceiver private constructor(
    type:  CangJieType,
    original:  ReceiverValue?
) :
     AbstractReceiverValue(type, original) {
    constructor(type:  CangJieType) : this(type, null)

    override fun toString(): String {
        return "{Transient} : " + getType()
    }

    override fun replaceType(newType:  CangJieType):  ReceiverValue {
        return TransientReceiver(newType, getOriginal())
    }
}
