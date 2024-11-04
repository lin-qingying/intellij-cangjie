package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.TypeRefinement
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.storage.getValue
import com.linqingying.cangjie.resolve.descriptorUtil.module
class ScopesHolderForClass<T : MemberScope> private constructor(
    private val classDescriptor: ClassDescriptor,
    storageManager: StorageManager,
    private val scopeFactory: (CangJieTypeRefiner) -> T,
    private val cangjieTypeRefinerForOwnerModule: CangJieTypeRefiner
) {
    private val scopeForOwnerModule by storageManager.createLazyValue {
        scopeFactory(cangjieTypeRefinerForOwnerModule)
    }

    @OptIn(TypeRefinement::class)
    fun getScope(cangjieTypeRefiner: CangJieTypeRefiner): T {
        /*
         * That check doesn't break anything, because scopeForOwnerModule _will_ anyway refine supertypes from module of
         *   class descriptor.
         *
         * Without that fastpass there is problem wit recursion types such that:
         *
         *   interface A<T : A<T>>
         *
         *   interface B : B<T>
         *
         * In this case (without check) we start compute default type of class descriptor B, go to isRefinementNeededForTypeConstructor,
         *   ask for supertypes of B, see A<B>, ask default type of class B and fail with recursion problem
         */
        if (!cangjieTypeRefiner.isRefinementNeededForModule(classDescriptor.module)) return scopeForOwnerModule

        if (!cangjieTypeRefiner.isRefinementNeededForTypeConstructor(classDescriptor.typeConstructor)) return scopeForOwnerModule
        return cangjieTypeRefiner.getOrPutScopeForClass(classDescriptor) { scopeFactory(cangjieTypeRefiner) }
    }

    companion object {
        @JvmStatic
        fun <T : MemberScope> create(
            classDescriptor: ClassDescriptor,
            storageManager: StorageManager,
            cangjieTypeRefinerForOwnerModule: CangJieTypeRefiner,
            scopeFactory: (CangJieTypeRefiner) -> T
        ): ScopesHolderForClass<T> {
            return ScopesHolderForClass(classDescriptor, storageManager, scopeFactory, cangjieTypeRefinerForOwnerModule)
        }
    }
}
