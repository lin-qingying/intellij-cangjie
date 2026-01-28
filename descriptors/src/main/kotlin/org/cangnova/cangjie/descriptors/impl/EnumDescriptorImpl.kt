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
package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.composeAnnotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.InstanceMemberScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.StaticMemberScope

import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


/**
 * 具体的枚举描述符实现
 *
 * 继承自抽象基类，提供完整的枚举描述符实现。
 * 这是原来的 EnumDescriptorImpl 的重构版本。
 *
 * 示例：
 * ```kotlin
 * // 简单枚举
 * val colorEnum = EnumDescriptorImpl(
 *     name = "Color",
 *     containingDeclaration = packageDescriptor,
 *     constructors = listOf(redConstructor, greenConstructor, blueConstructor),
 *     members = listOf(getNameFunction),
 *     hasArguments = false,
 *     isNonExhaustive = false
 * )
 *
 * // 泛型枚举
 * val optionEnum = EnumDescriptorImpl(
 *     name = "Option",
 *     containingDeclaration = packageDescriptor,
 *     constructors = listOf(someConstructor, noneConstructor),
 *     members = listOf(mapFunction, flatMapFunction),
 *     hasArguments = true,
 *     isNonExhaustive = false,
 *     declaredTypeParameters = listOf(typeParameterT)
 * )
 * ```
 */
class EnumDescriptorImpl(
    storageManager: StorageManager,
    name: Name,
    override val containingDeclaration: DeclarationDescriptor,
    override val constructors: Collection<EnumConstructorDescriptor>,
    private val enumMembers: Collection<EnumMember>,
    override val hasArguments: Boolean,
    override val isNonExhaustive: Boolean,
    override val source: SourceElement,
    supertypes: List<CangJieType>,
    override val enumKind: EnumKind = EnumKind.ENUM,
    override val modality: Modality = Modality.FINAL,
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    override val declaredTypeParameters: List<TypeParameterDescriptor> = emptyList(),
    override val unsubstitutedMemberScope: MemberScope
) : AbstractEnumDescriptor(
    storageManager,
    name = name,

    ) {


    public override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
        return unsubstitutedMemberScope
    }

    override val typeConstructor: TypeConstructor =
        EnumTypeConstructorImpl(this, mutableListOf(), supertypes, storageManager)


    /**
     * 静态作用域
     */
    override val staticScope: MemberScope
        get() = StaticMemberScope(unsubstitutedMemberScope)


    /**
     * 实例作用域
     */
    override val instanceScope: MemberScope
        get() = InstanceMemberScope(unsubstitutedMemberScope)


    /**
     * 创建替换后的成员作用域
     *
     * @param substitutor 类型替换器
     * @return 替换后的成员作用域
     */
    override fun createSubstitutedMemberScope(substitutor: ComposableTypeSubstitutor): MemberScope {
        /*
        TODO
        对于具体实现，可以在这里添加更复杂的类型替换逻辑
         例如，替换构造函数和成员函数的类型
         */

        return unsubstitutedMemberScope
    }



}


/**
 * 抽象枚举构造函数描述符基类
 *
 * 提供枚举构造函数描述符的通用实现，子类可以根据具体需求进行定制。
 * 支持：
 * - 简单构造函数（无关联值）
 * - 函数构造函数（有关联值）
 * - 参数管理
 * - 类型管理
 * - 泛型支持
 * - 类型替换
 *
 * 该抽象类定义了枚举构造函数的核心行为和状态，子类可以：
 * - 自定义参数和类型管理
 * - 定制类型替换逻辑
 * - 扩展构造函数特定行为
 * - 实现特殊的构造函数类型
 */
abstract class AbstractEnumConstructorDescriptor(
    override val name: Name,
    override val containingDeclaration: EnumDescriptor,

    override val source: SourceElement,

    ) :   DeclarationDescriptorImpl(Annotations.EMPTY, name), EnumConstructorDescriptor {
    protected var userDataMap: Map<CallableDescriptor.UserDataKey<*>, Any>? = null

    /**
     * 调用成员类型（默认为声明）
     */
    override val kind: CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.DECLARATION
    override var visibility: DescriptorVisibility = DescriptorVisibilities.INTERNAL

    override var modality: Modality = Modality.FINAL

    /**
     * 原始描述符（自身）
     */
    override val original: ConstructorDescriptor = this
    private var unsubstitutedReturnType: CangJieType? = null
    private var unsubstitutedValueParameters: List<ValueParameterDescriptor> = ArrayList()
    override val valueParameters: List<ValueParameterDescriptor>
        get() = unsubstitutedValueParameters
    override val returnType: CangJieType
        get() = unsubstitutedReturnType!!
    override val type: CangJieType
        get() = unsubstitutedReturnType!!

    /**
     * 调度接收器参数（枚举构造函数不支持）
     */
    override var dispatchReceiverParameter: ReceiverParameterDescriptor? = null

    /**
     * 重写的描述符（枚举构造函数不支持重写）
     */
    override val overriddenDescriptors: Collection<ConstructorDescriptor>
        get() = emptyList()


    /**
     * 类型参数(继承自枚举声明)
     *
     * 枚举构造器继承所属枚举的类型参数。
     * 例如：对于 `enum Option<T> { Some(T), None }`，
     * 构造器 Some 和 None 都继承类型参数 T。
     */
    override var typeParameters: List<TypeParameterDescriptor> = emptyList()


    /**
     * 是否有稳定的参数名称
     * 子类可重写
     */
    override fun hasStableParameterNames(): Boolean {
        return false
    }

    /**
     * 是否有合成的参数名称
     * 子类可重写
     */
    override fun hasSynthesizedParameterNames(): Boolean {
        return true
    }

    /**
     * 设置重写的描述符
     * 枚举构造函数不支持重写，默认实现为空
     *
     * @param overriddenDescriptors 重写的描述符列表
     */
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor>) {
        // 枚举构造函数不支持重写，所以这里不做任何操作
    }

    /**
     * 复制构造函数描述符
     * 子类可重写以提供自定义的复制逻辑
     *
     * @param newOwner 新的拥有者
     * @param modality 模态性
     * @param visibility 可见性
     * @param kind 调用成员类型
     * @param copyOverrides 是否复制重写信息
     * @return 复制的调用成员描述符
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        return newCopyBuilder()
            .setOwner(newOwner)
            .setModality(modality)
            .setVisibility(visibility)
            .setKind(kind)
            .setCopyOverrides(copyOverrides)
            .build() ?: throw IllegalStateException("Copy builder returned null")
    }


    /**
     * 创建新的复制构建器
     * 提供流式API用于复制构造函数描述符
     *
     * @return 复制构建器
     */
    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out ConstructorDescriptor> {
        return newCopyBuilder(ComposableTypeSubstitutor.EMPTY)
    }

    /**
     * 获取用户数据
     * 默认实现返回null，子类可重写
     *
     * @param key 用户数据键
     * @return 用户数据值
     */
    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? {
        return null
    }

    inner class CopyConfiguration(
        private var _substitution: ComposableTypeSubstitutor,
        var newOwner: DeclarationDescriptor,
        var newModality: Modality,
        var newVisibility: DescriptorVisibility,
        var kind: CallableMemberDescriptor.Kind,
        var newValueParameterDescriptors: List<ValueParameterDescriptor>,
        var newReturnType: CangJieType,
        var name: Name?
    ) : EnumConstructorDescriptor.CopyBuilder<EnumConstructorDescriptor> {

        val userDataMap: MutableMap<CallableDescriptor.UserDataKey<*>, Any> = LinkedHashMap()
        private var _original: EnumConstructorDescriptor? = null
        var dispatchReceiverParameter: ReceiverParameterDescriptor? =
            this@AbstractEnumConstructorDescriptor.dispatchReceiverParameter
        var copyOverrides: Boolean = true
        var signatureChange: Boolean = false
        var preserveSourceElement: Boolean = false
        var dropOriginalInContainingParts: Boolean = false
        var justForTypeSubstitution: Boolean = false

        var additionalAnnotations: Annotations? = null
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

        override fun setHiddenToOvercomeSignatureClash(): FunctionDescriptor.CopyBuilder<EnumConstructorDescriptor> {
            return this
        }

        override fun setHiddenForResolutionEverywhereBesideSupercalls(): FunctionDescriptor.CopyBuilder<EnumConstructorDescriptor> {
            return this
        }


        override fun setAdditionalAnnotations(additionalAnnotations: Annotations): CopyConfiguration {
            this.additionalAnnotations = additionalAnnotations
            return this
        }

        override fun build(): EnumConstructorDescriptor? {
            return doSubstitute(this)
        }

        fun getOriginal(): EnumConstructorDescriptor? {
            return _original
        }

        override fun setOriginal(original: CallableMemberDescriptor?): CopyConfiguration {
            this._original = original as? EnumConstructorDescriptor
            return this
        }

        override fun <V> putUserData(
            userDataKey: CallableDescriptor.UserDataKey<V>,
            value: V
        ): EnumConstructorDescriptor.CopyBuilder<EnumConstructorDescriptor> {
            userDataMap[userDataKey] = value as Any
            return this
        }

        fun getSubstitution(): ComposableTypeSubstitutor {
            return _substitution
        }

        override fun setSubstitution(substitution: ComposableTypeSubstitutor): CopyConfiguration {
            this._substitution = substitution
            return this
        }


        fun setJustForTypeSubstitution(value: Boolean): CopyConfiguration {
            justForTypeSubstitution = value
            return this
        }
    }

    protected fun newCopyBuilder(substitutor: ComposableTypeSubstitutor): CopyConfiguration {
        return CopyConfiguration(
            substitutor,
            containingDeclaration, modality, visibility, kind, valueParameters,
            returnType!!, null
        )
    }

    @NotNull
    protected abstract fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: EnumConstructorDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name,
        annotations: Annotations,
        source: SourceElement
    ): AbstractEnumConstructorDescriptor

    @NotNull
    private fun getSourceToUseForCopy(preserveSource: Boolean, original: EnumConstructorDescriptor?): SourceElement {
        return if (preserveSource) {
            (original ?: this.original).source
        } else {
            SourceElement.NO_SOURCE
        }
    }

    open fun initialize(
        dispatchReceiverParameter: ReceiverParameterDescriptor?= null,
        typeParameters: List<TypeParameterDescriptor> = emptyList(),

        unsubstitutedValueParameters: List<ValueParameterDescriptor> = emptyList(),
        unsubstitutedReturnType: CangJieType? = null,
        modality: Modality  = Modality.FINAL,
        visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    ): AbstractEnumConstructorDescriptor {
//        TODO 编译器的逻辑是：当调用枚举构造器时，类型参数从枚举限定符（如 A<String>）传递到构造器，而不是要求构造器单独提供类型参数。
//        this.typeParameters = typeParameters.toList()

        this.unsubstitutedValueParameters = unsubstitutedValueParameters.toList()

        this.unsubstitutedReturnType = unsubstitutedReturnType
        this.modality = modality
        this.visibility = visibility
        this.dispatchReceiverParameter = dispatchReceiverParameter


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

    /**
     * 替换函数描述符中的各种参数和类型。
     *
     * @param configuration 替换配置对象，包含新的扩展接收者参数、分发接收者参数、值参数描述符等。
     * @return 替换后的函数描述符，如果替换过程中出现错误则返回 null。
     */
    @Nullable
    protected open fun doSubstitute(configuration: CopyConfiguration): EnumConstructorDescriptor? {
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
            configuration.name ?: Name.ERROR_NAME,
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
        val substitutedReturnType = substitutor.substitute(configuration.newReturnType )
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





        return substitutedDescriptor
    }


    /**
     * 类型替换
     * 支持枚举构造函数的类型参数替换
     * 子类可重写以提供自定义的类型替换逻辑
     *
     * @param substitutor 类型替换器
     * @return 替换后的可调用描述符
     */
    override fun substitute(substitutor: ComposableTypeSubstitutor): ConstructorDescriptor? {
        if (substitutor.isEmpty) {
            return this
        }

        return newCopyBuilder(substitutor)
            .setOriginal(original)
            .setPreserveSourceElement()
            .setJustForTypeSubstitution(true)
            .build()
    }


    /**
     * 接受访问者
     *
     * @param visitor 声明描述符访问者
     * @param data 数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
        return visitor.visitEnumConstructorDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
        visitor.visitEnumConstructorDescriptor(this, null)
    }


    companion object {
        @Nullable
        fun getSubstitutedValueParameters(
            substitutedDescriptor: EnumConstructorDescriptor,
            unsubstitutedValueParameters: List<ValueParameterDescriptor>,
            substitutor: ComposableTypeSubstitutor
        ): List<ValueParameterDescriptor>? {
            return getSubstitutedValueParameters(
                substitutedDescriptor, unsubstitutedValueParameters, substitutor, false, false, null
            )
        }

        @Nullable
        fun getSubstitutedValueParameters(
            substitutedDescriptor: EnumConstructorDescriptor,
            unsubstitutedValueParameters: List<ValueParameterDescriptor>,
            substitutor: ComposableTypeSubstitutor,
            dropOriginal: Boolean,
            preserveSourceElement: Boolean,
            wereChanges: BooleanArray?
        ): List<ValueParameterDescriptor>? {
            val result = ArrayList<ValueParameterDescriptor>(unsubstitutedValueParameters.size)
            for (unsubstitutedValueParameter in unsubstitutedValueParameters) {
                // TODO : Lazy?
                val substitutedType = substitutor.substitute(unsubstitutedValueParameter.type)
                val varargElementType = unsubstitutedValueParameter.varargElementType
                val substituteVarargElementType =
                    varargElementType?.let { substitutor.substitute(it) }
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
    override val isPrimary: Boolean
        get() = false
    override val isEnd: Boolean
        get() = false
    override val constructedClass: EnumDescriptor
        get() = containingDeclaration
    override val initialSignatureDescriptor: FunctionDescriptor?
        get() = null
    override val isHiddenToOvercomeSignatureClash: Boolean
        get() = false
    override val isOperator: Boolean
        get() = false
    override val isHiddenForResolutionEverywhereBesideSupercalls: Boolean
        get() = false

}


/**
 * 具体的枚举构造函数描述符实现
 *
 * 继承自抽象基类，提供完整的枚举构造函数描述符实现。
 * 这是原来的 EnumConstructorDescriptorImpl 的重构版本。
 *
 * 示例：
 * ```kotlin
 * // 简单构造函数
 * val simpleConstructor = EnumConstructorDescriptorImpl(
 *     name = "Red",
 *     containingEnum = colorEnum,
 *     hasArguments = false
 * )
 *
 * // 函数构造函数（带关联值）
 * val functionConstructor = EnumConstructorDescriptorImpl(
 *     name = "Success",
 *     containingEnum = resultEnum,
 *     hasArguments = true,
 *     argumentTypes = listOf(genericTypeParameter)
 * )
 *
 * // Option 枚举的构造函数
 * val someConstructor = EnumConstructorDescriptorImpl(
 *     name = "Some",
 *     containingEnum = optionEnum,
 *     hasArguments = true,
 *     argumentTypes = listOf(typeParameterT)
 * )
 * ```
 */
open class EnumConstructorDescriptorImpl(
    name: Name,
    containingDeclaration: EnumDescriptor,
    original: EnumConstructorDescriptor?,
    annotations: Annotations,
    source: SourceElement
    ) : AbstractEnumConstructorDescriptor(
    name = name,
    containingDeclaration = containingDeclaration,
    source = source,
    ) {
    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: EnumConstructorDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name,
        annotations: Annotations,
        source: SourceElement
    ): EnumConstructorDescriptorImpl {
        check(!(kind != CallableMemberDescriptor.Kind.DECLARATION && kind != CallableMemberDescriptor.Kind.SYNTHESIZED)) {
            """
                Attempt at creating a constructor that is not a declaration: 
                copy from: $this
                newOwner: $newOwner
                kind: $kind
                """.trimIndent()
        }

        return EnumConstructorDescriptorImpl(
            newName,
            newOwner as EnumDescriptor,
            this,
            annotations,
            source,

            )
    }




}