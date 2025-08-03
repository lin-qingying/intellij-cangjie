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
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.annotations.composeAnnotations
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.CjFunction
import cn.cangnova.cangjie.resolve.DescriptorFactory
import cn.cangnova.cangjie.resolve.scopes.receivers.ExtensionReceiver
import cn.cangnova.cangjie.resolve.scopes.receivers.ImplicitContextReceiver
import cn.cangnova.cangjie.resolve.source.getPsi
import cn.cangnova.cangjie.types.*
import com.intellij.util.SmartList
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


    protected var userDataMap: Map<out CallableDescriptor.UserDataKey<*>, Any>? = null
    override lateinit var typeParameters: List<TypeParameterDescriptor>
    private var unsubstitutedValueParameters: List<ValueParameterDescriptor> = ArrayList()
    private var unsubstitutedReturnType: CangJieType? = null
    override var contextReceiverParameters: List<ReceiverParameterDescriptor> = emptyList()
    override val valueParameters: List<ValueParameterDescriptor>
        get() = unsubstitutedValueParameters

    //    扩展接收器
    override var extensionReceiverParameter: ReceiverParameterDescriptor? = null
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
            substitutor: TypeSubstitutor
        ): List<ValueParameterDescriptor>? {
            return getSubstitutedValueParameters(
                substitutedDescriptor, unsubstitutedValueParameters, substitutor, false, false, null
            )
        }

        @Nullable
        fun getSubstitutedValueParameters(
            substitutedDescriptor: FunctionDescriptor,
            unsubstitutedValueParameters: List<ValueParameterDescriptor>,
            substitutor: TypeSubstitutor,
            dropOriginal: Boolean,
            preserveSourceElement: Boolean,
            wereChanges: BooleanArray?
        ): List<ValueParameterDescriptor>? {
            val result = ArrayList<ValueParameterDescriptor>(unsubstitutedValueParameters.size)
            for (unsubstitutedValueParameter in unsubstitutedValueParameters) {
                // TODO : Lazy?
                val substitutedType = substitutor.substitute(unsubstitutedValueParameter.type, Variance.INVARIANT)
                val varargElementType = unsubstitutedValueParameter.varargElementType
                val substituteVarargElementType =
                    varargElementType?.let { substitutor.substitute(it, Variance.INVARIANT) }
                if (substitutedType == null) return null
                if (substitutedType != unsubstitutedValueParameter.type || varargElementType != substituteVarargElementType) {
                    wereChanges?.set(0, true)
                }

                val destructuringVariablesAction = getDestructuringVariablesAction(unsubstitutedValueParameter)

                result.add(
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        substitutedDescriptor,
                        if (dropOriginal) null else unsubstitutedValueParameter,
                        unsubstitutedValueParameter.index,
                        unsubstitutedValueParameter.annotations,
                        unsubstitutedValueParameter.name,
                        unsubstitutedValueParameter.isNamed,
                        substitutedType,
                        unsubstitutedValueParameter.declaresDefaultValue(),
                        if (preserveSourceElement) unsubstitutedValueParameter.source else SourceElement.NO_SOURCE,
                        destructuringVariablesAction
                    )
                )
            }
            return result
        }

        private fun getDestructuringVariablesAction(unsubstitutedValueParameter: ValueParameterDescriptor): (() -> List<VariableDescriptor>)? {
            return if (unsubstitutedValueParameter is ValueParameterDescriptorImpl.WithDestructuringDeclaration) {
                val destructuringVariables = unsubstitutedValueParameter.destructuringVariables
                { destructuringVariables }
            } else null
        }
    }

    fun setExpect(isExpect: Boolean) {
        this.isExpect = isExpect
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

    override val isExtend: Boolean

        get() = false

open fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: List<ReceiverParameterDescriptor>,
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
        this.extensionReceiverParameter = extensionReceiverParameter
        this.dispatchReceiverParameter = dispatchReceiverParameter
        this.contextReceiverParameters = contextReceiverParameters

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


    @Nullable


    fun setExtensionReceiverParameter(extensionReceiverParameter: ReceiverParameterDescriptor) {
        this.extensionReceiverParameter = extensionReceiverParameter
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


    private fun setHiddenToOvercomeSignatureClash(hiddenToOvercomeSignatureClash: Boolean) {
        isHiddenToOvercomeSignatureClash = hiddenToOvercomeSignatureClash
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


    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        if (substitutor.isEmpty) {
            return this
        }

        return newCopyBuilder(substitutor)
            .setOriginal(original)
            .setPreserveSourceElement()
            .setJustForTypeSubstitution(true)
            .build()
    }

    @Nullable
    private fun getExtensionReceiverParameterType(): CangJieType? {
        return extensionReceiverParameter?.type
    }


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        return newCopyBuilder(TypeSubstitutor.EMPTY)
    }

    @NotNull
    protected fun newCopyBuilder(substitutor: TypeSubstitutor): CopyConfiguration {
        return CopyConfiguration(
            substitutor.substitution,
            containingDeclaration, modality, visibility, kind, valueParameters, contextReceiverParameters,
            extensionReceiverParameter, returnType!!, null
        )
    }

    /**
     * 替换函数描述符中的各种参数和类型。
     *
     * @param configuration 替换配置对象，包含新的扩展接收者参数、分发接收者参数、值参数描述符等。
     * @return 替换后的函数描述符，如果替换过程中出现错误则返回 null。
     */
    @Nullable
    protected fun doSubstitute(configuration: CopyConfiguration): FunctionDescriptor? {
        val wereChanges = BooleanArray(1)

        // 合并原始注解和附加注解
        val resultAnnotations =
            configuration.additionalAnnotations?.let { composeAnnotations(annotations, it) }
                ?: annotations

        // 创建一个新的函数描述符副本
        val substitutedDescriptor = createSubstitutedCopy(
            configuration.newOwner, configuration.original, configuration.kind, configuration.name, resultAnnotations,
            getSourceToUseForCopy(configuration.preserveSourceElement, configuration.original)
        )

        // 获取未替换的类型参数
        val unsubstitutedTypeParameters =
            configuration.newTypeParameters ?: typeParameters

        wereChanges[0] = wereChanges[0] or unsubstitutedTypeParameters.isNotEmpty()

        // 替换类型参数
        val substitutedTypeParameters = ArrayList<TypeParameterDescriptor>(unsubstitutedTypeParameters.size)
        val substitutor = DescriptorSubstitutor.substituteTypeParameters(
            unsubstitutedTypeParameters,
            configuration.substitution,
            substitutedDescriptor,
            substitutedTypeParameters,
            wereChanges
        )
        if (substitutor == null) return null

        // 替换上下文接收者参数
        val substitutedContextReceiverParameters = mutableListOf<ReceiverParameterDescriptor>()

        if (configuration.newContextReceiverParameters.isNotEmpty()) {
            var index = 0
            for (newContextReceiverParameter in configuration.newContextReceiverParameters) {
                val substitutedContextReceiverType =
                    substitutor.substitute(newContextReceiverParameter.type, Variance.INVARIANT)
                if (substitutedContextReceiverType == null) {
                    return null
                }
                val substitutedContextReceiverParameter =
                    DescriptorFactory.createContextReceiverParameterForCallable(
                        substitutedDescriptor, substitutedContextReceiverType,
                        (newContextReceiverParameter.value as ImplicitContextReceiver).customLabelName,
                        newContextReceiverParameter.annotations,
                        index
                    )
                index++
//                substitutedContextReceiverParameters方法是根据substitutedContextReceiverType是否返回null的，所以这里已经判断过substitutedContextReceiverType，所以这里一定不为null，使用?let是为了好看
                substitutedContextReceiverParameter?.let { substitutedContextReceiverParameters.add(it) }

                wereChanges[0] = wereChanges[0] or (substitutedContextReceiverType != newContextReceiverParameter.type)
            }
        }

        // 替换扩展接收者参数
        var substitutedReceiverParameter: ReceiverParameterDescriptor? = null
        configuration.newExtensionReceiverParameter?.let { newExtensionReceiverParameter ->
            val substitutedExtensionReceiverType =
                substitutor.substitute(newExtensionReceiverParameter.type, Variance.INVARIANT)
            if (substitutedExtensionReceiverType == null) {
                return null
            }
            substitutedReceiverParameter = ReceiverParameterDescriptorImpl(
                substitutedDescriptor,
                ExtensionReceiver(
                    substitutedDescriptor, substitutedExtensionReceiverType, newExtensionReceiverParameter.value
                ),
                newExtensionReceiverParameter.annotations
            )

            wereChanges[0] = wereChanges[0] or (substitutedExtensionReceiverType != newExtensionReceiverParameter.type)
        }

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
        val substitutedReturnType = substitutor.substitute(configuration.newReturnType, Variance.INVARIANT)
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
            substitutedReceiverParameter, substitutedExpectedThis, substitutedContextReceiverParameters,
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
            if (configuration.substitution.isEmpty()) {
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


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
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
        var substitution: TypeSubstitution,
        var newOwner: DeclarationDescriptor,
        var newModality: Modality,
        var newVisibility: DescriptorVisibility,
        var kind: CallableMemberDescriptor.Kind,
        var newValueParameterDescriptors: List<ValueParameterDescriptor>,
        var newContextReceiverParameters: List<ReceiverParameterDescriptor>,
        var newExtensionReceiverParameter: ReceiverParameterDescriptor?,
        var newReturnType: CangJieType,
        var name: Name?
    ) : FunctionDescriptor.CopyBuilder<FunctionDescriptor> {

        val userDataMap: MutableMap<CallableDescriptor.UserDataKey<*>, Any> = LinkedHashMap()
        var original: FunctionDescriptor? = null
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


        override fun setContextReceiverParameters(contextReceiverParameters: List<ReceiverParameterDescriptor>): FunctionDescriptor.CopyBuilder<FunctionDescriptor> {
            newContextReceiverParameters = contextReceiverParameters
            return this
        }

        override fun setExtensionReceiverParameter(extensionReceiverParameter: ReceiverParameterDescriptor?): CopyConfiguration {
            newExtensionReceiverParameter = extensionReceiverParameter
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
            return original
        }

        override fun setOriginal(original: CallableMemberDescriptor?): CopyConfiguration {
            this.original = original as? FunctionDescriptor
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
            return substitution
        }

        override fun setSubstitution(substitution: TypeSubstitution): CopyConfiguration {
            this.substitution = substitution
            return this
        }


        fun setJustForTypeSubstitution(value: Boolean): CopyConfiguration {
            justForTypeSubstitution = value
            return this
        }
    }
}