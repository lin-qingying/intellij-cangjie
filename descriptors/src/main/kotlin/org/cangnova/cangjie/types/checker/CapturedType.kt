/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*

import org.cangnova.cangjie.types.error.ErrorScopeKind
import org.cangnova.cangjie.types.model.CaptureStatus
import org.cangnova.cangjie.types.model.CapturedTypeMarker
import org.cangnova.cangjie.types.model.CapturedTypeConstructorMarker

/**
 * 捕获类型构造器接口
 *
 * 仓颉语言中的捕获类型用于类型推导过程中表示待确定的具体类型。
 * 仓颉语言的所有类型参数都是不变的（invariant）。
 */
interface CapturedTypeConstructor : CapturedTypeConstructorMarker, TypeConstructor {
    val argument: TypeArgument
}

/**
 * 捕获类型构造器实现
 *
 * 仓颉语言中的捕获类型构造器用于类型推导过程，
 * 表示一个待推导的类型参数及其约束条件（父类型）。
 *
 * @param argument 类型参数
 * @param supertypesComputation 父类型计算（延迟计算以支持递归类型）
 * @param original 原始构造器（用于类型精化时保持相等性）
 * @param typeParameter 关联的类型参数描述符
 */
class CapturedTypeConstructorImpl(
    override val argument: TypeArgument,
    private var supertypesComputation: (() -> List<UnwrappedType>)? = null,
    private val original: CapturedTypeConstructorImpl? = null,
    val typeParameter: TypeParameterDescriptor? = null
) : CapturedTypeConstructor {

    constructor(
        projection: TypeArgument,
        supertypes: List<UnwrappedType>,
        original: CapturedTypeConstructorImpl? = null
    ) : this(projection, { supertypes }, original)

    private val _supertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supertypesComputation?.invoke()
    }

    /**
     * 延迟初始化父类型
     */
    fun initializeSupertypes(supertypes: List<UnwrappedType>) {
        assert(this.supertypesComputation == null) {
            "Already initialized! oldValue = ${this.supertypesComputation}, newValue = $supertypes"
        }
        this.supertypesComputation = { supertypes }
    }

    override val supertypes: Collection<CangJieType>
        get() = _supertypes ?: emptyList()

    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()

    override val isFinal: Boolean
        get() = false

    override val isDenotable: Boolean
        get() = false

    override val declarationDescriptor: ClassifierDescriptor?
        get() = null

    override val builtIns: CangJieBuiltIns
        get() = argument.type.builtIns

    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        CapturedTypeConstructorImpl(
            argument.refine(cangjieTypeRefiner),
            supertypesComputation?.let {
                { supertypes.map { it.unwrap().refine(cangjieTypeRefiner) } }
            },
            original ?: this,
            typeParameter = typeParameter
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CapturedTypeConstructorImpl
        return (original ?: this) === (other.original ?: other)
    }

    override fun hashCode(): Int = original?.hashCode() ?: super.hashCode()
    override fun toString() = "CapturedType($argument)"
}

/**
 * 捕获类型
 *
 * 仓颉语言中的捕获类型用于类型推导过程，表示一个待确定的类型参数实例。
 * 仓颉语言的所有类型参数都是不变的（invariant）。
 *
 * @param captureStatus 捕获状态（FROM_EXPRESSION 或 FOR_SUBTYPING）
 * @param constructor 捕获类型构造器
 * @param lowerType 下界类型（在仓颉的不变类型系统中通常为 null）
 * @param attributes 类型属性
 * @param isOption 是否为可选类型
 */
class CapturedType(
    val captureStatus: CaptureStatus,
    override val constructor: CapturedTypeConstructorImpl,
    val lowerType: UnwrappedType?,
    override val attributes: TypeAttributes = TypeAttributes.Empty,
    override val isOption: Boolean = false
) : SimpleType(), CapturedTypeMarker {

    internal constructor(
        captureStatus: CaptureStatus,
        lowerType: UnwrappedType?,
        projection: TypeArgument,
        typeParameter: TypeParameterDescriptor
    ) : this(captureStatus, CapturedTypeConstructorImpl(projection, typeParameter = typeParameter), lowerType)

    override val arguments: List<TypeArgument> get() = listOf()

    override val memberScope: MemberScope
        get() = ErrorUtils.createErrorScope(ErrorScopeKind.CAPTURED_TYPE_SCOPE, throwExceptions = true)

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        CapturedType(captureStatus, constructor, lowerType, newAttributes, isOption)

    override fun makeOptionAsSpecified(isOption: Boolean): SimpleType =
        CapturedType(captureStatus, constructor, lowerType, attributes, isOption)

    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        CapturedType(
            captureStatus,
            constructor.refine(cangjieTypeRefiner),
            lowerType?.let { cangjieTypeRefiner.refineType(it).unwrap() },
            attributes,
            isOption
        )
}

fun CangJieType.isCaptured(): Boolean = constructor is CapturedTypeConstructor

internal fun captureFromArguments(type: SimpleType, status: CaptureStatus): SimpleType? = null

fun TypeSubstitution.wrapWithCapturingSubstitution(needApproximation: Boolean = false): TypeSubstitution =
    if (this is IndexedParametersSubstitution)
        IndexedParametersSubstitution(
            this.parameters,
            this.arguments,
            approximateContravariantCapturedTypes = false
        )
    else
        this
