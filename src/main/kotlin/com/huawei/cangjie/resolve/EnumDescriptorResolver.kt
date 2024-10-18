package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.huawei.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import com.huawei.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjEnum
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo

import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.resolve.scopes.LocalRedeclarationChecker
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.storage.StorageManager

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
