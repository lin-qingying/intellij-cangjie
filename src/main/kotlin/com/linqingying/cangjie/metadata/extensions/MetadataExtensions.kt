package com.linqingying.cangjie.metadata.extensions

import com.linqingying.cangjie.metadata.*
import com.linqingying.cangjie.metadata.extensions.*
import com.linqingying.cangjie.metadata.internal.*

import com.linqingying.cangjie.metadata.node.*
import java.util.*

interface MetadataExtensions {
    fun readClassExtensions(cmClass: CmClass, proto: ProtoBuf.Class, c: ReadContext)

    fun readPackageExtensions(cmPackage: CmPackage, proto: ProtoBuf.Package, c: ReadContext)

    fun readModuleFragmentExtensions(
        cmModuleFragment: CmModuleFragment,
        proto: ProtoBuf.PackageFragment,
        c: ReadContext
    )

    fun readFunctionExtensions(cmFunction: CmFunction, proto: ProtoBuf.Function, c: ReadContext)
    fun readVariableExtensions(cmVariable: CmVariable, proto: ProtoBuf.Variable, c: ReadContext)

    fun readPropertyExtensions(cmProperty: CmProperty, proto: ProtoBuf.Property, c: ReadContext)

    fun readConstructorExtensions(cmConstructor: CmConstructor, proto: ProtoBuf.Constructor, c: ReadContext)

    fun readTypeParameterExtensions(
        cmTypeParameter: CmTypeParameter,
        proto: ProtoBuf.TypeParameter,
        c: ReadContext
    )

    fun readTypeExtensions(cmType: CmType, proto: ProtoBuf.Type, c: ReadContext)

    fun readTypeAliasExtensions(cmTypeAlias: CmTypeAlias, proto: ProtoBuf.TypeAlias, c: ReadContext)

    fun readValueParameterExtensions(
        cmValueParameter: CmValueParameter,
        proto: ProtoBuf.ValueParameter,
        c: ReadContext
    )

    fun writeClassExtensions(cmClass: CmClass, proto: ProtoBuf.Class.Builder, c: WriteContext)

    fun writePackageExtensions(cmPackage: CmPackage, proto: ProtoBuf.Package.Builder, c: WriteContext)

    fun writeModuleFragmentExtensions(
        cmModuleFragment: CmModuleFragment, proto: ProtoBuf.PackageFragment.Builder, c: WriteContext
    )

    fun writeFunctionExtensions(cmFunction: CmFunction, proto: ProtoBuf.Function.Builder, c: WriteContext)

    fun writePropertyExtensions(cmProperty: CmProperty, proto: ProtoBuf.Property.Builder, c: WriteContext)
    fun writeVariableExtensions(cmProperty: CmVariable, proto: ProtoBuf.Variable.Builder, c: WriteContext)

    fun writeConstructorExtensions(
        cmConstructor: CmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext
    )

    fun writeTypeParameterExtensions(
        cmTypeParameter: CmTypeParameter, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext
    )

    fun writeTypeExtensions(type: CmType, proto: ProtoBuf.Type.Builder, c: WriteContext)

    fun writeTypeAliasExtensions(typeAlias: CmTypeAlias, proto: ProtoBuf.TypeAlias.Builder, c: WriteContext)

    fun writeValueParameterExtensions(
        valueParameter: CmValueParameter, proto: ProtoBuf.ValueParameter.Builder, c: WriteContext
    )

    fun createClassExtension(): CmClassExtension

    fun createPackageExtension(): CmPackageExtension

    fun createModuleFragmentExtensions(): CmModuleFragmentExtension

    fun createFunctionExtension(): CmFunctionExtension

    fun createPropertyExtension(): CmPropertyExtension

    fun createConstructorExtension(): CmConstructorExtension

    fun createTypeParameterExtension(): CmTypeParameterExtension

    fun createTypeExtension(): CmTypeExtension

    fun createTypeAliasExtension(): CmTypeAliasExtension?

    fun createValueParameterExtension(): CmValueParameterExtension?


    companion object {
        internal val INSTANCES: List<MetadataExtensions> by lazy {
            ServiceLoader.load(MetadataExtensions::class.java, MetadataExtensions::class.java.classLoader).toList()
                .also {
                    if (it.isEmpty()) error(
                        "No MetadataExtensions instances found in the classpath. Please ensure that the META-INF/services/ " +
                                "is not stripped from your application and that the Java virtual machine is not running " +
                                "under a security manager"
                    )
                }
        }
    }
}
