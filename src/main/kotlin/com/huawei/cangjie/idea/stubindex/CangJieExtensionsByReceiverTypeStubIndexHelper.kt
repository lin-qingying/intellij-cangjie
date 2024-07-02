package com.huawei.cangjie.idea.stubindex

import com.huawei.cangjie.psi.CjCallableDeclaration


abstract class CangJieExtensionsByReceiverTypeStubIndexHelper : CangJieStringStubIndexHelper<CjCallableDeclaration>(
    CjCallableDeclaration::class.java
) {
    fun buildKey(receiverTypeName: String, callableName: String): String = receiverTypeName + SEPARATOR + callableName

    fun receiverTypeNameFromKey(key: String): String = key.substringBefore(SEPARATOR, "")

    fun callableNameFromKey(key: String): String = key.substringAfter(SEPARATOR, "")

    private companion object {
        private const val SEPARATOR = '\n'
    }
}