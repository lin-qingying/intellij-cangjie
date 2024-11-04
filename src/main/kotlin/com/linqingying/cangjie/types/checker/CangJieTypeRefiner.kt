package com.linqingying.cangjie.types.checker

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.model.CangJieTypeMarker


sealed class TypeRefinementSupport(val isEnabled: Boolean) {
    object Disabled : TypeRefinementSupport(isEnabled = false)
    object EnabledUninitialized : TypeRefinementSupport(isEnabled = true)
    class Enabled(val typeRefiner: CangJieTypeRefiner) : TypeRefinementSupport(isEnabled = true)
}

@TypeRefinement
val REFINER_CAPABILITY = ModuleCapability<Ref<TypeRefinementSupport>>("CangJieTypeRefiner")
class Ref<T : Any>(var value: T)

@TypeRefinement
fun CangJieTypeRefiner.refineTypes(types: Iterable<CangJieType>): List<CangJieType> = types.map { refineType(it) }

@DefaultImplementation(impl = AbstractTypeRefiner.Default::class)
abstract class CangJieTypeRefiner : AbstractTypeRefiner(){
    @TypeRefinement
    abstract override fun refineType(type: CangJieTypeMarker): CangJieType

    @TypeRefinement
    abstract fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<CangJieType>

    @TypeRefinement
    abstract fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor?
    @TypeRefinement
    abstract fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean
    @TypeRefinement
    abstract fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean
    @TypeRefinement
    abstract fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S

    object Default : CangJieTypeRefiner() {
        @TypeRefinement
        override fun refineType(type: CangJieTypeMarker): CangJieType = type as CangJieType

        @TypeRefinement
        override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor? {
            return null

        }
//
        @TypeRefinement
        override fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<CangJieType> {
            return classDescriptor.typeConstructor.supertypes
        }
//
//        @TypeRefinement
//        override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassDescriptor? {
//            return null
//        }
//
//        @TypeRefinement
//        override fun findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
//            return null
//        }
//
        @TypeRefinement
        override fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean {
            return false
        }

        @TypeRefinement
        override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
            return false
        }

        @TypeRefinement
        override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S {
            return compute()
        }
    }
}
