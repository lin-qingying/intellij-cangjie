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

package org.cangnova.cangjie.resolve.lazy.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.extend.AbstractExtendDescriptor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.LazyEntity
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScopeImpl
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.LocalRedeclarationChecker
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.*

/**
 * 延迟扩展描述符
 *
 * 采用延迟计算策略的扩展描述符实现，仅在需要时才解析扩展的各种信息。
 *
 * 扩展声明示例：
 * ```cangjie
 * extend<T> Array<T> <: Printable where T <: ToString {
 *     func print() { ... }
 * }
 * ```
 *
 * 扩展 ID 的生成由父类 [AbstractExtendDescriptor] 实现，遵循编译器的 name mangling 策略。
 *
 * @param c 延迟类上下文，提供解析所需的各种服务
 * @param containingDeclaration 包含该扩展的声明描述符（通常是包描述符）
 * @param cjExtend 扩展的 PSI 元素
 */
class LazyExtendDescriptor(
    private val c: LazyClassContext,
    override val containingDeclaration: DeclarationDescriptor,
    private val cjExtend: CjExtend
) : AbstractExtendDescriptor(c.storageManager), LazyEntity, HasResolutionScopes {

    /**
     * 声明提供者，用于延迟解析扩展成员
     */
    private val declarationProvider: ClassMemberDeclarationProvider by lazy {
        c.declarationProviderFactory.getClassMemberDeclarationProvider(
            org.cangnova.cangjie.descriptors.data.CjClassInfoUtil.createClassLikeInfo(cjExtend)
        )
    }

    /**
     * 被扩展的类型
     */
    private val _extendType = c.storageManager.createLazyValue {
        val typeRef = cjExtend.receiverTypeReceiver
            ?: error("Extend declaration must have receiver type: ${cjExtend.text}")

        val scope = c.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)
        c.typeResolver.resolveType(
            scope,
            typeRef,
            c.trace,
            checkBounds = true
        )
    }

    override val extendType: CangJieType
        get() = _extendType()

    /**
     * 扩展声明的类型参数列表
     */
    private val _declaredTypeParameters = c.storageManager.createLazyValue {
        val typeParameterList = cjExtend.typeParameterList
            ?: return@createLazyValue emptyList<TypeParameterDescriptor>()

        typeParameterList.parameters.mapIndexed { index, parameter ->
            LazyTypeParameterDescriptor(c, this, parameter, org.cangnova.cangjie.descriptors.annotations.Annotations.EMPTY, index)
        }
    }

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = _declaredTypeParameters()

    /**
     * 扩展实现的接口列表（superTypes）
     */
    private val _superTypes = c.storageManager.createLazyValue {
        val entries = cjExtend.superTypeListEntries
        if (entries.isEmpty()) {
            return@createLazyValue emptyList<CangJieType>()
        }

        val scope = c.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)
        entries.mapNotNull { entry ->
            val typeRef = entry.typeReference ?: return@mapNotNull null
            c.typeResolver.resolveType(
                scope,
                typeRef,
                c.trace,
                checkBounds = true
            )
        }
    }

    override val superTypes: Collection<CangJieType>
        get() = _superTypes()

    /**
     * 未进行类型替换的成员作用域
     */
    private val _unsubstitutedMemberScope = c.storageManager.createLazyValue {
        LazyExtendMemberScope(c, declarationProvider, this, c.trace)
    }

    override val unsubstitutedMemberScope: MemberScope
        get() = _unsubstitutedMemberScope()

    /**
     * 声明的可调用成员（不含 fake overrides）
     *
     * 仅返回在扩展体内直接声明的函数和属性，
     * 用于重写检查。
     */
    override val declaredCallableMembers: Collection<CallableMemberDescriptor>
        get() = unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.DECLARATION }

    /**
     * 类头解析作用域
     *
     * 用于解析扩展的类型参数约束。包含扩展声明的类型参数。
     */
    override val scopeForClassHeaderResolution: LexicalScope
        get() = _scopeForClassHeaderResolution()

    private val _scopeForClassHeaderResolution = c.storageManager.createLazyValue {
        val outerScope = c.declarationScopeProvider.getResolutionScopeForDeclaration(
            declarationProvider.ownerInfo!!.scopeAnchor
        )

        LexicalScopeImpl(
            outerScope,
            this,
            false,
            null,
            LexicalScopeKind.CLASS_HEADER,
            LocalRedeclarationChecker.DO_NOTHING
        ) {
            declaredTypeParameters.forEach { addClassifierDescriptor(it) }
        }
    }

    /**
     * 源码位置信息
     */
    override val source: SourceElement
        get() = cjExtend.toSourceElement()

    /**
     * 强制解析所有延迟计算的内容
     */
    override fun forceResolveAllContents() {
        org.cangnova.cangjie.resolve.lazy.ForceResolveUtil.forceResolveAllContents(this)
    }

    override fun toString(): String {
        return "LazyExtendDescriptor($extendId)"
    }
}
