package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.resolve.scopes.TupleClassScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.AbstractClassTypeConstructor
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner

class TupleClassDescriptor(
    private val storageManager: StorageManager,
    override val containingDeclaration: DeclarationDescriptor,

    val arity: Int
) : AbstractClassDescriptor(storageManager, numberedClassName(arity)) {
    private val typeConstructor = TupleTypeConstructor()
    private val memberScope = TupleClassScope(storageManager, this)

    companion object {
        fun numberedClassName(arity: Int) = Name.identifier("Tuple$arity")

        fun create(
            storageManager: StorageManager,
            containingDeclaration: DeclarationDescriptor,

            arity: Int
        ): TupleClassDescriptor {
            return TupleClassDescriptor(storageManager, containingDeclaration, arity)
        }
    }

//    private val typeConstructor = TupleTypeConstructor()
//    private val memberScope = FunctionClassScope(storageManager, this)

    private val parameters: List<TypeParameterDescriptor>

    init {
        val result = ArrayList<TypeParameterDescriptor>()

        fun typeParameter(variance: Variance, name: String) {
            result.add(
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    this@TupleClassDescriptor,
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
            typeParameter(Variance.INVARIANT, "T$i")
        }


        parameters = result.toList()
    }


    private inner class TupleTypeConstructor : AbstractClassTypeConstructor(storageManager) {
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

        override fun getParameters() = this@TupleClassDescriptor.parameters

        override fun getDeclarationDescriptor() = this@TupleClassDescriptor
        override fun isDenotable() = true

        override fun toString() = declarationDescriptor.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = SupertypeLoopChecker.EMPTY
    }

    override fun getStaticScope() = MemberScope.Empty

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope


    override val source: SourceElement = SourceElement.NO_SOURCE


    override fun getConstructors() = emptyList<ClassConstructorDescriptor>()
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> =emptySet()


    override fun getKind() = ClassKind.TUPLE
    override fun getModality() = Modality.FINAL
    override fun getUnsubstitutedPrimaryConstructor() = null


    override fun getDeclaredTypeParameters() = parameters

    override fun toString(): String {
        return "Tuple$arity"
    }

    override fun isFun() = false
    override fun isValue() = false
    override fun isExpect() = false

    override fun getSealedSubclasses() = emptyList<ClassDescriptor>()
}
