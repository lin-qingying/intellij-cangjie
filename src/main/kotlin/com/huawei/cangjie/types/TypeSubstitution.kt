package com.huawei.cangjie.types

abstract class TypeSubstitution{
    companion object {
        @JvmField
        val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: CangJieType): Nothing? = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    abstract operator fun get(key: CangJieType): TypeProjection?
    fun buildSubstitutor(): TypeSubstitutor = TypeSubstitutor.create(this)

    open fun isEmpty(): Boolean = false

    open fun approximateCapturedTypes(): Boolean = false
    open fun approximateContravariantCapturedTypes(): Boolean = false
}