/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.getCangJieTypeRefiner
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.CangJieTypeFactory.computeExpandedType
import org.cangnova.cangjie.types.TypeUtils.makeUnsubstitutedType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 抽象类描述符基类
 *
 * 提供类描述符的基本实现，包括类型计算、作用域管理和成员访问等功能
 */
abstract class AbstractClassDescriptor(
    private val storageManager: StorageManager,
    override val name: Name
) : ModuleAwareClassDescriptor(), ClassDescriptor {

    
    protected val _defaultType: NotNullLazyValue<SimpleType> = storageManager.createLazyValue {
        makeUnsubstitutedType(
            this, unsubstitutedMemberScope,

            object : (CangJieTypeRefiner) -> SimpleType? {
                override fun invoke(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType {
                    val descriptor = cangjieTypeRefiner.refineDescriptor(this@AbstractClassDescriptor)
                    // If we've refined descriptor
                    if (descriptor == null) return _defaultType.invoke()

                    if (descriptor is TypeAliasDescriptor) {
                        return descriptor.computeExpandedType(
                            TypeUtils.getDefaultTypeProjections(descriptor.typeConstructor.parameters)
                        )
                    }

                    if (descriptor is ModuleAwareClassDescriptor) {
                        val refinedConstructor = descriptor.typeConstructor.refine(cangjieTypeRefiner)
                        return makeUnsubstitutedType(
                            refinedConstructor,
                            descriptor.getUnsubstitutedMemberScope(cangjieTypeRefiner),
                            this

                        )
                    }

                    return descriptor.defaultType
                }

            }


        )
    }

//    private val _unsubstitutedInnerClassesScope: NotNullLazyValue<MemberScope> = storageManager.createLazyValue {
//        InnerClassesScopeWrapper(unsubstitutedMemberScope)
//    }

    private val _thisAsReceiverParameter: NotNullLazyValue<ReceiverParameterDescriptor> =
        storageManager.createLazyValue {
            LazyClassReceiverParameterDescriptor(this@AbstractClassDescriptor)
        }

    override val thisAsReceiverParameter: ReceiverParameterDescriptor
        get() = _thisAsReceiverParameter.invoke()


    override val contextReceivers: List<ReceiverParameterDescriptor>
        get() = emptyList()
    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.PUBLIC


    
    override val unsubstitutedMemberScope: MemberScope
        get() = getUnsubstitutedMemberScope(DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())


    override fun getMemberScope(
        typeArguments: List<TypeProjection>,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        assert(typeArguments.size == typeConstructor.parameters.size) {
            "Illegal number of type arguments: expected ${typeConstructor.parameters.size} but was ${typeArguments.size} for $typeConstructor ${typeConstructor.parameters}"
        }
        if (typeArguments.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner)

        val substitutor = TypeConstructorSubstitution.create(typeConstructor, typeArguments).buildSubstitutor()
        return SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor)
    }

    override fun getMemberScope(
        typeSubstitution: TypeSubstitution,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        if (typeSubstitution.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner)

        val substitutor = TypeSubstitutor.create(typeSubstitution)
        return SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor)
    }


    
    override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope {
        return getMemberScope(typeArguments.toList(), DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())

    }


    
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        return getMemberScope(typeSubstitution, DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())
    }


    override val original: ClassDescriptor
        get() = this


//    override val unsubstitutedInnerClassesScope: MemberScope
//        get() = _unsubstitutedInnerClassesScope.invoke()

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor {
        if (substitutor.isEmpty) {
            return this
        }
        return LazySubstitutingClassDescriptor(this, substitutor)
    }

    override val defaultType: SimpleType
        get() = _defaultType.invoke()


    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitClassDescriptor(this, data)

    }

    override val defaultFunctionTypeForSamInterface: SimpleType?
        get() = null


    override val isDefinitelyNotSamInterface: Boolean
        get() = false
} 