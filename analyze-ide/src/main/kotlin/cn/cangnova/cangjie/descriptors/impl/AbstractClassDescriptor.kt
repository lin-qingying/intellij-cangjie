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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.descriptorUtil.getCangJieTypeRefiner
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import cn.cangnova.cangjie.resolve.scopes.*
import cn.cangnova.cangjie.storage.NotNullLazyValue
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.CangJieTypeFactory.computeExpandedType
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.util.TypeUtils
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import java.util.*

/**
 * 抽象类描述符基类
 *
 * 提供类描述符的基本实现，包括类型计算、作用域管理和成员访问等功能
 */
abstract class AbstractClassDescriptor(
    private val storageManager: StorageManager,
    override val name: Name
) : ModuleAwareClassDescriptor() {

    protected val _defaultType: NotNullLazyValue<SimpleType> = storageManager.createLazyValue {
        TypeUtils.makeUnsubstitutedType(
            this, unsubstitutedMemberScope,
            { cangjieTypeRefiner ->
                val descriptor = cangjieTypeRefiner.refineDescriptor(this@AbstractClassDescriptor)
                // If we've refined descriptor
                if (descriptor == null) return@makeUnsubstitutedType _defaultType.invoke()

                if (descriptor is TypeAliasDescriptor) {
                    return@makeUnsubstitutedType descriptor.computeExpandedType(
                        TypeUtils.getDefaultTypeProjections(descriptor.typeConstructor.parameters)
                    )
                }

                if (descriptor is ModuleAwareClassDescriptor) {
                    val refinedConstructor = descriptor.typeConstructor.refine(cangjieTypeRefiner)
                    return@makeUnsubstitutedType TypeUtils.makeUnsubstitutedType(
                        refinedConstructor,
                        descriptor.getUnsubstitutedMemberScope(cangjieTypeRefiner),
                        this
                    )
                }

                descriptor.defaultType
            }
        )
    }

    private val unsubstitutedInnerClassesScope: NotNullLazyValue<MemberScope> = storageManager.createLazyValue {
        InnerClassesScopeWrapper(unsubstitutedMemberScope)
    }

    private val thisAsReceiverParameter: NotNullLazyValue<ReceiverParameterDescriptor> =
        storageManager.createLazyValue {
            LazyClassReceiverParameterDescriptor(this@AbstractClassDescriptor)
        }

    private var extendClassDescriptor: Set<LazyExtendClassDescriptor>? = null

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor {
        return thisAsReceiverParameter.invoke()
    }


    override val contextReceivers: Collection<ReceiverParameterDescriptor>
        get() = emptyList()
    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.PUBLIC

    override fun getUnsubstitutedMemberScope(): MemberScope {
        return getUnsubstitutedMemberScope(DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())
    }


    /**
     * 获取扩展类描述符集合
     *
     * @return 扩展类描述符集合
     */
    fun getExtendClassDescriptors(): Set<LazyExtendClassDescriptor> {
        if (extendClassDescriptor == null) {
            getExtendClass()
        }
        return extendClassDescriptor!!
    }

    /**
     * 获取扩展类型（权宜之计）
     *
     * @return 扩展类描述符集合
     */
    fun getExtendClass(): Set<LazyExtendClassDescriptor> {
        extendClassDescriptor =
            HashSet(getCurrentEditorScope().getExtendClasss(name, NoLookupLocation.FROM_PACKAGE))
        return extendClassDescriptor!!
    }

    /**
     * 获取相对于活动编辑的 LexicalScope（权宜之计）
     *
     * @return 词法作用域，如果无法获取则返回null
     */
    fun getCurrentEditorScope(): LexicalScope? {
        val fileEditorManager = FileEditorManager.getInstance(storageManager.project)
        val editor = fileEditorManager.selectedTextEditor
        val file = editor?.let { PsiDocumentManager.getInstance(storageManager.project).getPsiFile(it.document) }
        if (file is CjFile) {
            return ScopeUtilsKt.getScope()
        }
        return null
    }

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
        return getMemberScope(typeArguments, DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())
    }

    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        return getMemberScope(typeSubstitution, DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())
    }


    override val original: ClassifierDescriptor
        get() = this

    override fun getUnsubstitutedInnerClassesScope(): MemberScope {
        return unsubstitutedInnerClassesScope.invoke()
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor {
        if (substitutor.isEmpty()) {
            return this
        }
        return LazySubstitutingClassDescriptor(this, substitutor)
    }

    override val defaultType: SimpleType
        get() = _defaultType.invoke()


    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? {
        return null
    }

    override fun isDefinitelyNotSamInterface(): Boolean {
        return false
    }
} 