package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.types.CangJieType
import java.lang.UnsupportedOperationException

/**
 * Describes any "this" receiver inside a class
 */
interface ThisClassReceiver : ReceiverValue {
    val classDescriptor: ClassDescriptor
}
/**
 * Same but implicit only
 */
open class ImplicitClassReceiver(
    final override val classDescriptor: ClassDescriptor,
    original: ImplicitClassReceiver? = null
) : ThisClassReceiver, ImplicitReceiver {

    private val original = original ?: this

    override fun getType() = classDescriptor.defaultType

    override val declarationDescriptor = classDescriptor

    override fun equals(other: Any?) = classDescriptor == (other as? ImplicitClassReceiver)?.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()

    override fun toString() = "Class{$type}"

    override fun replaceType(newType: CangJieType) =
        throw UnsupportedOperationException("Replace type should not be called for this receiver")

    override fun getOriginal() = original
}