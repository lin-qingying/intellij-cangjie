package com.huawei.cangjie.serialization.deserialization

interface NameResolver {
    fun getString(index: Int): String

    /**
     * @return the fully qualified name of some class in the format: `org/foo/bar/Test.Inner`
     */
    fun getQualifiedClassName(index: Int): String

    fun isLocalClassName(index: Int): Boolean
}
