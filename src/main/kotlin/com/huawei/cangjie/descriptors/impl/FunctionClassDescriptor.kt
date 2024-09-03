package com.huawei.cangjie.descriptors.impl


import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.builtins.functions.FunctionClassKind
import com.huawei.cangjie.builtins.functions.FunctionTypeKind
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.FunctionClassScope
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.AbstractClassTypeConstructor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.types.checker.CangJieTypeRefiner


class FunctionClassDescriptor(
    private val storageManager: StorageManager,
    override val containingDeclaration: DeclarationDescriptor,
    val functionTypeKind: FunctionTypeKind,
    val arity: Int
) : AbstractClassDescriptor(storageManager, functionTypeKind.numberedClassName(arity)) {

    private val typeConstructor = FunctionTypeConstructor()
    private val memberScope = FunctionClassScope(storageManager, this)

    private val parameters: List<TypeParameterDescriptor>

    init {
        val result = ArrayList<TypeParameterDescriptor>()

        fun typeParameter(variance: Variance, name: String) {
            result.add(
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    this@FunctionClassDescriptor,
                    Annotations.EMPTY,
//                    false,
                    variance,
                    Name.identifier(name),
                    result.size,
                    storageManager
                )
            )
        }

        (1..arity).map { i ->
            typeParameter(Variance.INVARIANT, "P$i")
        }

        typeParameter(Variance.INVARIANT, "R")

        parameters = result.toList()
    }


    override fun getStaticScope() = MemberScope.Empty

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope

    override fun getConstructors() = emptyList<ClassConstructorDescriptor>()
    override fun getKind() = ClassKind.INTERFACE
    override fun getModality() = Modality.ABSTRACT
    override fun getUnsubstitutedPrimaryConstructor() = null


    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    override fun isFun() = false
    override fun isValue() = false
    override fun isExpect() = false

    override val annotations: Annotations get() = Annotations.EMPTY
    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getSealedSubclasses() = emptyList<ClassDescriptor>()


    override fun getDeclaredTypeParameters() = parameters

    private inner class FunctionTypeConstructor : AbstractClassTypeConstructor(storageManager) {
        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {

            return emptyList()
        }

        override fun computeSupertypes(): Collection<CangJieType> {
//                 val supertypes = when (functionTypeKind) {
//                FunctionTypeKind.Function -> // Function$N <: Function
//                    listOf(functionClassId)
//
//
//                else -> shouldNotBeCalled()
//            }
//
//            val moduleDescriptor = containingDeclaration.containingDeclaration
//            return supertypes.map { id ->
//                val descriptor =
//                    moduleDescriptor.findClassAcrossModuleDependencies(id) ?: error("Built-in class $id not found")
//
//                // Substitute all type parameters of the super class with our last type parameters
//                val arguments = parameters.takeLast(descriptor.typeConstructor.parameters.size).map {
//                    TypeProjectionImpl(it.defaultType)
//                }
//
//                CangJieTypeFactory.simpleNotNullType(TypeAttributes.Empty, descriptor, arguments)
//            }.toList()
            return listOf(builtIns.anyType)
        }

        override fun getParameters() = this@FunctionClassDescriptor.parameters

        override fun getDeclarationDescriptor() = this@FunctionClassDescriptor
        override fun isDenotable() = true

        override fun toString() = declarationDescriptor.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = SupertypeLoopChecker.EMPTY
    }

    override fun toString() = name.asString()

    companion object {
        fun create(
            storageManager: StorageManager,
            containingDeclaration: DeclarationDescriptor,
            functionTypeKind: FunctionTypeKind,
            arity: Int
        ): FunctionClassDescriptor {
            return FunctionClassDescriptor(
                storageManager, containingDeclaration, functionTypeKind, arity
            )
        }

//        fun create(builtIns: CangJieBuiltIns, parameterCount: Int): FunctionClassDescriptor {
//            return create(
//                builtIns.storageManager,
//                builtIns.builtInsModule,
//                FunctionTypeKind.Function,
//                parameterCount
//            )
//        }

        private val functionClassId = ClassId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Function"))
//        private val kFunctionClassId = ClassId(CANGJIE_REFLECT_FQ_NAME, Name.identifier("KFunction"))
    }


    val functionKind: FunctionClassKind = FunctionClassKind.getFunctionClassKind(functionTypeKind)
}
