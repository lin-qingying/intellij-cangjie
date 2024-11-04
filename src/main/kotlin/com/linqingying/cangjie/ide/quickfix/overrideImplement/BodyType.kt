package com.linqingying.cangjie.ide.quickfix.overrideImplement

sealed class BodyType(val requiresReturn: Boolean = true) {
    object NoBody : BodyType()
    object EmptyOrTemplate : BodyType(requiresReturn = false)
    object FromTemplate : BodyType(requiresReturn = false)
    object Super : BodyType()
    object QualifiedSuper : BodyType()

    class Delegate(val receiverName: String) : BodyType()

    fun effectiveBodyType(canBeEmpty: Boolean): BodyType {
        return if (!canBeEmpty && this == EmptyOrTemplate) FromTemplate else this
    }
}
