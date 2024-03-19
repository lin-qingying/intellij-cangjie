package com.huawei.cangjie.descriptors

abstract class DescriptorVisibility protected constructor(){

    abstract val delegate: Visibility

    val name: String
        get() = delegate.name

    val isPublicAPI: Boolean
        get() = delegate.isPublicAPI
}