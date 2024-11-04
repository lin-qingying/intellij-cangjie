package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.lazy.LazyClassContext
import com.linqingying.cangjie.resolve.lazy.data.CjEnmuEntryInfo

import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.LexicalScopeKind
import com.linqingying.cangjie.resolve.scopes.LexicalWritableScope
import com.linqingying.cangjie.resolve.scopes.LocalRedeclarationChecker
import com.linqingying.cangjie.resolve.source.toSourceElement
import com.linqingying.cangjie.storage.StorageManager

class EnumDescriptorResolver(
    private val typeResolver: TypeResolver,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager

) {
    fun resolbeEnumEntryConstructorDescriptor(
        scope: LexicalScope,
        classDescriptor: ClassDescriptor,
        entry: CjEnumEntry,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        inferenceSession: InferenceSession?,
    ): ClassConstructorDescriptorImpl {

        val parameterScope = LexicalWritableScope(
            scope,
            classDescriptor,
            false,
            LocalRedeclarationChecker.DO_NOTHING,
            LexicalScopeKind.CONSTRUCTOR_HEADER
        )
        val types = entry.typeReferences.map {
            typeResolver.resolveType(parameterScope, it, trace, true)
        }


        return EnumEntryConstructorDescriptor(classDescriptor, null, entry.toSourceElement()) {
            types
        }

    }

    fun resolveEnumEntryDescriptor(
        c: LazyClassContext,
        thisDescriptor: DeclarationDescriptor,
        name: Name,
        info: CjEnmuEntryInfo,
        external: Boolean
    ): EnumEntryDescriptor {
//        TODO 在这里校验还是在 LazyEnumEntryDescriptor的构造函数中校验？
        return when (thisDescriptor) {
            is PackageFragmentDescriptor -> {
                val descriptor = (info.correspondingClass.parent?.parent as? CjEnum)?.nameAsSafeName?.let {
                    thisDescriptor.getMemberScope().getContributedClassifier(
                        it,
                        NoLookupLocation.FROM_IDE
                    )
                }

                EnumEntryDescriptor(c, descriptor ?: thisDescriptor, name, info)

            }

            else -> {
                EnumEntryDescriptor(c, thisDescriptor, name, info)

            }
        }
    }

}
