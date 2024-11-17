/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.resolve.lazy.declarations.impl

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.linqingying.cangjie.builtins.StandardNames.FqNames.cpointerUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.unitUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.int8UFqName

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.DeclarationDescriptorNonRootImpl
import com.linqingying.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.linqingying.cangjie.descriptors.impl.basic.BuiltInTypeDescriptor
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.utils.Printer
import kotlin.reflect.full.memberProperties


class ReexportPackageFragment(
    module: ModuleDescriptor,
    fqName: FqName,
    private val memberScope: MemberScope
) : PackageFragmentDescriptorImpl(module, fqName) {
    override fun getMemberScope(): MemberScope {
        return memberScope
    }

}

abstract class PackageFragmentDescriptorImpl(
    module: ModuleDescriptor,
    final override val fqName: FqName
) : DeclarationDescriptorNonRootImpl(module, Annotations.EMPTY, fqName.shortNameOrSpecial(), SourceElement.NO_SOURCE),
    PackageFragmentDescriptor {
    // Not inlined in order to not capture ref on 'module'
    private val debugString: String = "package $fqName of $module"

//    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
//
//    override fun getContainingDeclaration(): ModuleDescriptor {
//        return super.getContainingDeclaration() as ModuleDescriptor
//    }
    override val containingDeclaration: ModuleDescriptor
        get() = super.containingDeclaration  as ModuleDescriptor
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        return visitor.visitPackageFragmentDescriptor(this, data!!)

    }


    override val source: SourceElement = SourceElement.NO_SOURCE
    override fun toString(): String = debugString
}

fun craeteBasicTypePackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,

    ): PackageFragmentDescriptorBasicImpl {
    return PackageFragmentDescriptorBasicImpl(storageManager, module, BUILT_INS_PACKAGE_FQ_NAME)
}



abstract class AbstractPackageFragmentDescriptorBuiltlnImpl(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName)

//
class PackageFragmentDescriptorBasicImpl(
    val storageManager: StorageManager,
    val module: ModuleDescriptor,
    fqName: FqName
) : AbstractPackageFragmentDescriptorBuiltlnImpl(module, fqName) {

    val basicMemberScope = BasicMemberScope()
    private fun createBasicTypeDescriptor(name: FqNameUnsafe): BasicTypeDescriptor =
        BasicTypeDescriptor.create(basicMemberScope, storageManager, name.shortName())

    private fun createBuiltInTypeDescriptor(
        name: FqNameUnsafe,
        parameters: List<TypeParameterDescriptor> = emptyList()
    ): BuiltInTypeDescriptor =
        BuiltInTypeDescriptor.create(basicMemberScope, storageManager, name.shortName(), parameters)


    //Unit
    val UNIT_DESCRIPTOR = createBasicTypeDescriptor(unitUFqName)

    //    Int
    val INT8_DESCRIPTOR = createBasicTypeDescriptor(int8UFqName)
    val INT16_DESCRIPTOR = createBasicTypeDescriptor( StandardNames.FqNames.int16UFqName)
    val INT32_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.int32UFqName)
    val INT64_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.int64UFqName)
    val INTNATIVE_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.int_nativeUFqName)

    //    UInt
    val UINT8_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint8UFqName)
    val UINT16_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint16UFqName)
    val UINT32_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint32UFqName)
    val UINT64_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint64UFqName)
    val UINTNATIVE_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint_nativeUFqName)

    //    Char
    val RUNE_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.runeUFqName)

    //    Bool
    val BOOL_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.boolUFqName)

    //Nothing
    val NOTHING_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.nothingUFqName)

    //    Float
    val FLOAT16_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.float16UFqName)
    val FLOAT32_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.float32UFqName)
    val FLOAT64_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.float64UFqName)

    val CSTRING = createBuiltInTypeDescriptor(StandardNames.FqNames.cstringUFqName, emptyList())
    val CPOINTER = createBuiltInTypeDescriptor(
        cpointerUFqName
    ).apply {
        addParameter(
            TypeParameterDescriptorImpl.createForFurtherModification(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("T"), 0, SourceElement.NO_SOURCE,
                null, SupertypeLoopChecker.EMPTY, storageManager


            ).apply {


            }
        )
    }


    val DESCRIPTOR_MAP = mutableMapOf<Name, BasicTypeDescriptor>(

    )

    init {
//        反射赋值
        val fields = this::class.memberProperties
        fields.forEach { field ->

            val descriptor = field.call(this)
            if (descriptor is BasicTypeDescriptor) {
                DESCRIPTOR_MAP[descriptor.name] = descriptor

            }


        }

    }


    inner class BasicMemberScope : MemberScope {

        fun getBuiltIns():CangJieBuiltIns{
            return module.builtIns
        }
        override fun getContributedVariables(
            name: Name,
            location: LookupLocation
        ): Collection<@JvmWildcard VariableDescriptor> = emptyList()

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
            emptyList()

        override fun getFunctionNames(): Set<Name> = emptySet()

        override fun getVariableNames(): Set<Name> = emptySet()
        override fun getPropertyNames(): Set<Name> = emptySet()

        override fun getClassifierNames(): Set<Name> = DESCRIPTOR_MAP.map {
            it.key
        }.toSet()


        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {

            return   emptyList()
        }

        override fun printScopeStructure(p: Printer) {
            p.println("Basic member scope")
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): BasicTypeDescriptor? {

            return DESCRIPTOR_MAP[name]
        }

        override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
            return emptyList()
        }

        override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
            return emptyList()
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<BasicTypeDescriptor> = emptyList()

    }

    override fun getMemberScope(): MemberScope {
        return basicMemberScope
    }
}
