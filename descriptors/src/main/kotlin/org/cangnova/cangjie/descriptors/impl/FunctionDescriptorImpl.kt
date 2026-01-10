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

/**
 * 抽象函数描述符基类。
 *
 * 该类为具体函数描述符（如局部函数、成员函数、匿名函数等）提供通用实现，包括：
 * - 参数、返回类型与接收者的存储与访问；
 * - 类型参数与覆盖关系的管理；
 * - 延迟计算覆盖关系的支持；
 * - 用户数据映射（用于在描述符上附加自定义信息）。
 *
 * 子类应在 initialize 方法中完成具体的参数与类型初始化，并可根据需要覆盖默认行为。
 */
package org.cangnova.cangjie.descriptors.impl

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.composeAnnotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.concurrent.atomic.AtomicReference

abstract class FunctionDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    original: FunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    override val kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, source), FunctionDescriptor {

    private val _original: FunctionDescriptor = original ?: this
    override val original: FunctionDescriptor
        get() = if (_original == this) this else _original.original


    protected var userDataMap: Map<CallableDescriptor.UserDataKey<*>, Any>? = null
    override lateinit var typeParameters: List<TypeParameterDescriptor>
    private var unsubstitutedValueParameters: List<ValueParameterDescriptor> = ArrayList()
    private var unsubstitutedReturnType: CangJieType? = null
    override val valueParameters: List<ValueParameterDescriptor>
        get() = unsubstitutedValueParameters


    override var dispatchReceiverParameter: ReceiverParameterDescriptor? = null
    override var modality: Modality = Modality.FINAL
    override var visibility: DescriptorVisibility = DescriptorVisibilities.INTERNAL

    private var isExpect: Boolean = false
    override var isHiddenToOvercomeSignatureClash: Boolean = false
    override var isHiddenForResolutionEverywhereBesideSupercalls: Boolean = false
    private var hasStableParameterNames: Boolean = true
    private var hasSynthesizedParameterNames: Boolean = false

    private var overriddenFunctions: MutableCollection<FunctionDescriptor>? = null
    private var lazyOverriddenFunctionsTask: AtomicReference<() -> Collection<FunctionDescriptor>> =
        AtomicReference()

    @Nullable
    override var initialSignatureDescriptor: FunctionDescriptor? = null

    companion object {
        @Nullable
        fun getSubstitutedValueParameters(
            substitutedDescriptor: FunctionDescriptor,
            unsubstitutedValueParameters: List<ValueParameterDescriptor>,
            substitutor: DefaultTypeSubstitutor
        ): List<ValueParameterDescriptor>? {
            return getSubstitutedValueParameters(
                substitutedDescriptor, unsubstitutedValueParameters, substitutor, false, false, null
            )
        }

        @Nullable
        fun getSubstitutedValueParameters(
            substitutedDescriptor: FunctionDescriptor,
            unsubstitutedValueParameters: List<ValueParameterDescriptor>,
            substitutor: DefaultTypeSubstitutor,
            dropOriginal: Boolean,
            preserveSourceElement: Boolean,
            wereChanges: BooleanArray?
        ): List<ValueParameterDescriptor>? {
            val result = ArrayList<ValueParameterDescriptor>(unsubstitutedValueParameters.size)
            for (unsubstitutedValueParameter in unsubstitutedValueParameters) {
                // TODO : Lazy?
                val substitutedType = substitutor.substitute(unsubstitutedValueParameter.type )
                val varargElementType = unsubstitutedValueParameter.varargElementType
                val substituteVarargElementType =
                    varargElementType?.let { substitutor.substitute(it ) }
                if (substitutedType == null) return null
                if (substitutedType != unsubstitutedValueParameter.type || varargElementType != substituteVarargElementType) {
                    wereChanges?.set(0, true)
                }


                result.add(
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        substitutedDescriptor,
                        if (dropOriginal) null else unsubstitutedValueParameter,
                        unsubstitutedValueParameter.index,
                        unsubstitutedValueParameter.annotations,
                        unsubstitutedValueParameter.name,
                        unsubstitutedValueParameter.isNamed,
                        substitutedType,
                        unsubstitutedValueParameter.declaresDefaultValue,
                        if (preserveSourceElement) unsubstitutedValueParameter.source else SourceElement.NO_SOURCE,
                    )
                )
            }
            return result
        }


    }


    fun setHasStableParameterNames(hasStableParameterNames: Boolean) {
        this.hasStableParameterNames = hasStableParameterNames
    }

    override fun hasStableParameterNames(): Boolean {
        return hasStableParameterNames
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? {
        return userDataMap?.get(key) as? V
    }


    fun setIsStatic(isStatic: Boolean) {
        this.isStatic = isStatic
    }

    fun setIsConst(isConst: Boolean) {
        this.isConst = isConst
    }

    fun setIsOperator(isOperator: Boolean) {
        this.isOperator = isOperator
    }


    open fun initialize(
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility
    ): FunctionDescriptorImpl {
        this.typeParameters = typeParameters.toList()
        this.unsubstitutedValueParameters = unsubstitutedValueParameters.toList()

        this.unsubstitutedReturnType = unsubstitutedReturnType
        this.modality = modality ?: Modality.FINAL
        this.visibility = visibility
        this.dispatchReceiverParameter = dispatchReceiverParameter

        for (i in typeParameters.indices) {
            val typeParameterDescriptor = typeParameters[i]
            if (typeParameterDescriptor.index != i) {
                throw IllegalStateException("${typeParameterDescriptor} index is ${typeParameterDescriptor.index} but position is $i")
            }
        }

        for (i in unsubstitutedValueParameters.indices) {
            // TODO fill me
            val firstValueParameterOffset = 0 // receiverParameter.exists() ? 1 : 0;
            val valueParameterDescriptor = unsubstitutedValueParameters[i]
            if (valueParameterDescriptor.index != i + firstValueParameterOffset) {
                throw IllegalStateException("${valueParameterDescriptor}index is ${valueParameterDescriptor.index} but position is $i")
            }
        }

        return this
    }


    override val overriddenDescriptors: Collection<FunctionDescriptor>
        get() {
            performOverriddenLazyCalculationIfNeeded()
            return overriddenFunctions ?: emptyList()
        }

    /**
     * 重写规则  this 表示的该对象 (已经重写的对象，open修饰): 注意 这里的open
     *
     * @param overriddenDescriptors 被重写方法，也就是抽象方法
     */
    @Suppress("UNCHECKED_CAST")
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor>) {

        overriddenFunctions = ArrayList()

        for (function in overriddenDescriptors as Collection<FunctionDescriptor>) {
//
            overriddenFunctions?.add(function)
            if (function.isHiddenForResolutionEverywhereBesideSupercalls) {
                isHiddenForResolutionEverywhereBesideSupercalls = true
                break
            }

        }
    }

    private fun performOverriddenLazyCalculationIfNeeded() {
        val overriddenTask = lazyOverriddenFunctionsTask.get()
        if (overriddenTask != null) {
            overriddenFunctions = overriddenTask.invoke().toMutableList()
            // Here it's important that this assignment is strictly after previous one
            // `lazyOverriddenFunctionsTask` is volatile, so when someone will see that it's null,
            // he can read consistent collection from `overriddenFunctions`,
            // because it's assignment happens-before of "lazyOverriddenFunctionsTask = null"
            lazyOverriddenFunctionsTask.set(null)
        }
    }


    override var isConst: Boolean = false
        get() {
            val element = source.getPsi()
            if (element is CjFunction) {
                field = element.isConst
            }
            return field
        }
    override var isStatic: Boolean = false
        get() {
            val element = source.getPsi()
            if (element is CjFunction) {
                field = element.isStatic
            }
            return field
        }
    override var isUnsafe: Boolean = false
        get() {
            val element = source.getPsi()
            if (element is CjFunction) {
                field = element.isUnsafe
            }
            return field
        }


    override var isOperator: Boolean = false
        get() {
            if (field) return true

            for (descriptor in original.overriddenDescriptors) {
                if (descriptor.isOperator) return true
            }
            return false

        }


    override fun hasSynthesizedParameterNames(): Boolean {
        return hasSynthesizedParameterNames
    }


    override fun validate() {
        typeParameters
    }

    override val returnType: CangJieType?
        get() = unsubstitutedReturnType


    fun setReturnType(unsubstitutedReturnType: CangJieType) {

        this.unsubstitutedReturnType = unsubstitutedReturnType
    }


    override fun substitute(substitutor: DefaultTypeSubstitutor): FunctionDescriptor? {
        if (substitutor.isEmpty) {
            return this
        }

        return newCopyBuilder(substitutor)
            .setOriginal(original)
            .setPreserveSourceElement()
            .setJustForTypeSubstitution(true)
            .build()
    }



    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        return newCopyBuilder(DefaultTypeSubstitutor.EMPTY)
    }

    @NotNull
    protected fun newCopyBuilder(substitutor: DefaultTypeSubstitutor): CopyConfiguration {
        return CopyConfiguration(
            substitutor.substitution,
            containingDeclaration, modality, visibility, kind, valueParameters, returnType!!, null
        )
    }

    /**
     * 替换函数描述符中的各种参数和类型。
     *
     * @param configuration 替换配置对象，包含新的扩展接收者参数、分发接收者参数、值参数描述符等。
     * @return 替换后的函数描述符，如果替换过程中出现错误则返回 null。
     */
    @Nullable
    protected open fun doSubstitute(configuration: CopyConfiguration): FunctionDescriptor? {
        val wereChanges = BooleanArray(1)

        // 合并原始注解和附加注解
        val resultAnnotations =
            configuration.additionalAnnotations?.let { composeAnnotations(annotations, it) }
                ?: annotations

        // 创建一个新的函数描述符副本
        val substitutedDescriptor = createSubstitutedCopy(
            configuration.newOwner,
            configuration.getOriginal(),
            configuration.kind,
            configuration.name,
            resultAnnotations,
            getSourceToUseForCopy(configuration.preserveSourceElement, configuration.getOriginal())
        )

        // 获取未替换的类型参数
        val unsubstitutedTypeParameters =
            configuration.newTypeParameters ?: typeParameters

        wereChanges[0] = wereChanges[0] or unsubstitutedTypeParameters.isNotEmpty()

        // 替换类型参数
        val substitutedTypeParameters = ArrayList<TypeParameterDescriptor>(unsubstitutedTypeParameters.size)
        val substitutor = DescriptorSubstitutor.substituteTypeParameters(
            unsubstitutedTypeParameters,
            configuration.getSubstitution(),
            substitutedDescriptor,
            substitutedTypeParameters,
            wereChanges
        )
        if (substitutor == null) return null



        // 替换分发接收者参数
        var substitutedExpectedThis: ReceiverParameterDescriptor? = null
        configuration.dispatchReceiverParameter?.let { dispatchReceiverParameter ->
            // 当生成假覆盖成员时，其分发接收者参数类型应为基类类型，这是正确的。
            // 例如：
            // class Base { fun foo() }
            // class Derived <: Base
            // let x: Base
            // if (x is Derived) {
            //    // `x` 不应被标记为智能转换
            //    // 但如果假覆盖的 `foo` 有 `Derived` 作为其分发接收者参数类型，则会标记为智能转换
            //    x.foo()
            // }
            substitutedExpectedThis = dispatchReceiverParameter.substitute(substitutor)
            if (substitutedExpectedThis == null) {
                return null
            }

            wereChanges[0] = wereChanges[0] or (substitutedExpectedThis != dispatchReceiverParameter)
        }

        // 替换值参数
        val substitutedValueParameters = getSubstitutedValueParameters(
            substitutedDescriptor,
            configuration.newValueParameterDescriptors,
            substitutor,
            configuration.dropOriginalInContainingParts,
            configuration.preserveSourceElement,
            wereChanges
        )
        if (substitutedValueParameters == null) {
            return null
        }

        // 替换返回类型
        val substitutedReturnType = substitutor.substitute(configuration.newReturnType, )
        if (substitutedReturnType == null) {
            return null
        }

        wereChanges[0] = wereChanges[0] or (substitutedReturnType != configuration.newReturnType)

        // 如果没有变化且仅用于类型替换，则返回当前描述符
        if (!wereChanges[0] && configuration.justForTypeSubstitution) {
            return this
        }

        // 初始化替换后的描述符
        substitutedDescriptor.initialize(
              substitutedExpectedThis,
            substitutedTypeParameters,
            substitutedValueParameters,
            substitutedReturnType,
            configuration.newModality,
            configuration.newVisibility
        )
        substitutedDescriptor.isOperator = isOperator
        substitutedDescriptor.isExpect = isExpect
        substitutedDescriptor.hasStableParameterNames = hasStableParameterNames
        substitutedDescriptor.isHiddenToOvercomeSignatureClash = configuration.isHiddenToOvercomeSignatureClash
        substitutedDescriptor.isHiddenForResolutionEverywhereBesideSupercalls =
            configuration.isHiddenForResolutionEverywhereBesideSupercalls
        substitutedDescriptor.isStatic = isStatic
        substitutedDescriptor.isConst = isConst
        substitutedDescriptor.hasSynthesizedParameterNames =
            configuration.newHasSynthesizedParameterNames ?: hasSynthesizedParameterNames

        // 处理用户数据映射
        if (configuration.userDataMap.isNotEmpty() || userDataMap != null) {
            val newMap = configuration.userDataMap.toMutableMap()

            userDataMap?.forEach { (key, value) ->
                if (!newMap.containsKey(key)) {
                    newMap[key] = value
                }
            }

            substitutedDescriptor.userDataMap = when {
                newMap.size == 1 -> {
                    val entry = newMap.entries.first()
                    mapOf(entry.key to entry.value)
                }

                else -> newMap
            }
        }

        // 处理签名更改或初始签名描述符
        if (configuration.signatureChange || initialSignatureDescriptor != null) {
            val initialSignature = initialSignatureDescriptor ?: this
            val initialSignatureSubstituted = initialSignature.substitute(substitutor) as FunctionDescriptor
            substitutedDescriptor.initialSignatureDescriptor = initialSignatureSubstituted
        }

        // 处理覆盖描述符
        if (configuration.copyOverrides && original.overriddenDescriptors.isNotEmpty()) {
            if (configuration.getSubstitution().isEmpty()) {
                val overriddenFunctionsTask = lazyOverriddenFunctionsTask.get()
                if (overriddenFunctionsTask != null) {
                    substitutedDescriptor.lazyOverriddenFunctionsTask.set(overriddenFunctionsTask)
                } else {
                    substitutedDescriptor.setOverriddenDescriptors(overriddenDescriptors)
                }
            } else {
                substitutedDescriptor.lazyOverriddenFunctionsTask.set {
                    val result = SmartList<FunctionDescriptor>()
                    for (overriddenFunction in overriddenDescriptors) {
                        result.add(overriddenFunction.substitute(substitutor) as FunctionDescriptor)
                    }
                    result
                }
            }
        }

        return substitutedDescriptor
    }

    @NotNull
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): FunctionDescriptor {
        return newCopyBuilder()
            .setOwner(newOwner)
            .setModality(modality)
            .setVisibility(visibility)
            .setKind(kind)
            .setCopyOverrides(copyOverrides)
            .build() ?: throw IllegalStateException("Copy builder returned null")
    }

    fun setHasSynthesizedParameterNames(hasSynthesizedParameterNames: Boolean) {
        this.hasSynthesizedParameterNames = hasSynthesizedParameterNames
    }

    @NotNull
    protected abstract fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl

    @NotNull
    private fun getSourceToUseForCopy(preserveSource: Boolean, original: FunctionDescriptor?): SourceElement {
        return if (preserveSource) {
            (original ?: this.original).source
        } else {
            SourceElement.NO_SOURCE
        }
    }


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
        return visitor.visitFunctionDescriptor(this, data)
    }

    // Don't use on published descriptors
    fun <V> putInUserDataMap(key: CallableDescriptor.UserDataKey<V>, value: Any) {
        if (userDataMap == null) {
            userDataMap = LinkedHashMap()
        }
        (userDataMap as MutableMap)[key] = value
    }

    inner class CopyConfiguration(
        private var _substitution: TypeSubstitution,
        var newOwner: DeclarationDescriptor,
        var newModality: Modality,
        var newVisibility: DescriptorVisibility,
        var kind: CallableMemberDescriptor.Kind,
        var newValueParameterDescriptors: List<ValueParameterDescriptor>,
        var newReturnType: CangJieType,
        var name: Name?
    ) : FunctionDescriptor.CopyBuilder<FunctionDescriptor> {

        val userDataMap: MutableMap<CallableDescriptor.UserDataKey<*>, Any> = LinkedHashMap()
        private var _original: FunctionDescriptor? = null
        var dispatchReceiverParameter: ReceiverParameterDescriptor? =
            this@FunctionDescriptorImpl.dispatchReceiverParameter
        var copyOverrides: Boolean = true
        var signatureChange: Boolean = false
        var preserveSourceElement: Boolean = false
        var dropOriginalInContainingParts: Boolean = false
        var justForTypeSubstitution: Boolean = false
        var isHiddenToOvercomeSignatureClash: Boolean = this@FunctionDescriptorImpl.isHiddenToOvercomeSignatureClash
        var additionalAnnotations: Annotations? = null
        var isHiddenForResolutionEverywhereBesideSupercalls: Boolean =
            this@FunctionDescriptorImpl.isHiddenForResolutionEverywhereBesideSupercalls
        var newTypeParameters: List<TypeParameterDescriptor>? = null
        var newHasSynthesizedParameterNames: Boolean? = null

        override fun setOwner(owner: DeclarationDescriptor): CopyConfiguration {
            newOwner = owner
            return this
        }

        override fun setModality(modality: Modality): CopyConfiguration {
            newModality = modality
            return this
        }

        override fun setVisibility(visibility: DescriptorVisibility): CopyConfiguration {
            newVisibility = visibility
            return this
        }

        override fun setKind(kind: CallableMemberDescriptor.Kind): CopyConfiguration {
            this.kind = kind
            return this
        }

        override fun setCopyOverrides(copyOverrides: Boolean): CopyConfiguration {
            this.copyOverrides = copyOverrides
            return this
        }

        override fun setName(name: Name): CopyConfiguration {
            this.name = name
            return this
        }


        override fun setValueParameters(parameters: List<ValueParameterDescriptor>): CopyConfiguration {
            newValueParameterDescriptors = parameters.toList()
            return this
        }

        override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CopyConfiguration {
            newTypeParameters = parameters
            return this
        }

        override fun setReturnType(type: CangJieType): CopyConfiguration {
            newReturnType = type
            return this
        }






        override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyConfiguration {
            this.dispatchReceiverParameter = dispatchReceiverParameter
            return this
        }

        override fun setPreserveSourceElement(): CopyConfiguration {
            preserveSourceElement = true
            return this
        }

        override fun setSignatureChange(): CopyConfiguration {
            signatureChange = true
            return this
        }

        fun setHasSynthesizedParameterNames(value: Boolean): CopyConfiguration {
            newHasSynthesizedParameterNames = value
            return this
        }

        override fun setDropOriginalInContainingParts(): CopyConfiguration {
            dropOriginalInContainingParts = true
            return this
        }

        override fun setHiddenToOvercomeSignatureClash(): CopyConfiguration {
            isHiddenToOvercomeSignatureClash = true
            return this
        }

        override fun setHiddenForResolutionEverywhereBesideSupercalls(): CopyConfiguration {
            isHiddenForResolutionEverywhereBesideSupercalls = true
            return this
        }

        override fun setAdditionalAnnotations(additionalAnnotations: Annotations): CopyConfiguration {
            this.additionalAnnotations = additionalAnnotations
            return this
        }

        override fun build(): FunctionDescriptor? {
            return doSubstitute(this)
        }

        fun getOriginal(): FunctionDescriptor? {
            return _original
        }

        override fun setOriginal(original: CallableMemberDescriptor?): CopyConfiguration {
            this._original = original as? FunctionDescriptor
            return this
        }

        override fun <V> putUserData(
            userDataKey: CallableDescriptor.UserDataKey<V>,
            value: V
        ): FunctionDescriptor.CopyBuilder<FunctionDescriptor> {
            userDataMap[userDataKey] = value as Any
            return this
        }

        fun getSubstitution(): TypeSubstitution {
            return _substitution
        }

        override fun setSubstitution(substitution: TypeSubstitution): CopyConfiguration {
            this._substitution = substitution
            return this
        }


        fun setJustForTypeSubstitution(value: Boolean): CopyConfiguration {
            justForTypeSubstitution = value
            return this
        }
    }
}