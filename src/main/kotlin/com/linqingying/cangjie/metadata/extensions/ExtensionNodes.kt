package com.linqingying.cangjie.metadata.extensions

import com.linqingying.cangjie.metadata.node.*
import kotlin.reflect.KClass


/**
 * A type of the extension expected by the code that uses the extensions API.
 *
 * Each declaration that can have platform-specific extensions in the metadata has a method `getExtension`, e.g.:
 * `fun CmFunction.getExtension(type: CmExtensionType): CmFunctionExtension`.
 *
 * These functions are used by -jvm and -klib counterparts to retrieve platform-specific metadata
 * and should not be used in any other way.
 */
class CmExtensionType(private val klass: KClass<out CmExtension>) {
    override fun equals(other: Any?): Boolean =
        other is CmExtensionType && klass == other.klass

    override fun hashCode(): Int =
        klass.hashCode()

    override fun toString(): String =
        klass.java.name
}

/**
 * Base interface for all extensions to hold the extension type.
 */
interface CmExtension {

    /**
     * Type of this extension.
     */
    val type: CmExtensionType
}

interface CmClassExtension : CmExtension

fun CmClass.getExtension(type: CmExtensionType): CmClassExtension = extensions.singleOfType(type)

interface CmPackageExtension : CmExtension

fun CmPackage.getExtension(type: CmExtensionType): CmPackageExtension = extensions.singleOfType(type)

interface CmModuleFragmentExtension : CmExtension

fun CmModuleFragment.getExtension(type: CmExtensionType): CmModuleFragmentExtension =
    extensions.singleOfType(type)

interface CmFunctionExtension : CmExtension

fun CmFunction.getExtension(type: CmExtensionType): CmFunctionExtension = extensions.singleOfType(type)

interface CmPropertyExtension : CmExtension

fun CmProperty.getExtension(type: CmExtensionType): CmPropertyExtension = extensions.singleOfType(type)

interface CmConstructorExtension : CmExtension

fun CmConstructor.getExtension(type: CmExtensionType): CmConstructorExtension = extensions.singleOfType(type)

interface CmTypeParameterExtension : CmExtension

fun CmTypeParameter.getExtension(type: CmExtensionType): CmTypeParameterExtension = extensions.singleOfType(type)

interface CmTypeExtension : CmExtension {
    /**
     * Has to be implemented for [CmType.equals] to work.
     * Remember to implement `hashCode`, too (although it is not used in [CmType.hashCode]).
     */
    override fun equals(other: Any?): Boolean
}

fun CmType.getExtension(type: CmExtensionType): CmTypeExtension = extensions.singleOfType(type)

interface CmTypeAliasExtension : CmExtension

fun CmTypeAlias.getExtension(type: CmExtensionType): CmTypeAliasExtension = extensions.singleOfType(type)

interface CmValueParameterExtension : CmExtension

fun CmValueParameter.getExtension(type: CmExtensionType): CmValueParameterExtension =
    extensions.singleOfType(type)

private fun <N : CmExtension> Collection<N>.singleOfType(type: CmExtensionType): N {
    var result: N? = null
    for (node in this) {
        if (node.type != type) continue
        if (result != null) {
            throw IllegalStateException("Multiple extensions handle the same extension type: $type")
        }
        result = node
    }
    if (result == null) {
        throw IllegalStateException("No extensions handle the extension type: $type")
    }
    return result
}
