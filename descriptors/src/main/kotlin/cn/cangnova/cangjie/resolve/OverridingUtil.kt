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
package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.DescriptorVisibilities.compare
import cn.cangnova.cangjie.descriptors.DescriptorVisibilities.isPrivate
import cn.cangnova.cangjie.descriptors.DescriptorVisibilities.isVisibleIgnoringReceiver
import cn.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.PropertyDescriptorImpl
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.name.Name.equals
import cn.cangnova.cangjie.resolve.DescriptorEquivalenceForOverrides.areEquivalent
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.AbstractTypeChecker.equalTypes
import cn.cangnova.cangjie.types.AbstractTypeChecker.isSubtypeOf
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.checker.CangJieTypePreparator
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.utils.SmartSet
import java.util.*
import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1
import kotlin.jvm.functions.Function2

class OverridingUtil private constructor(
    private val equalityAxioms: CangJieTypeChecker.TypeConstructorEquality,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
    private val cangjieTypePreparator: CangJieTypePreparator,
    private val customSubtype: Function2<CangJieType?, CangJieType?, Boolean?>?
) {
    //    /**
    //     * 将sub 根据是否是扩展转换到super
    //     * @param superDescriptor
    //     * @param subDescriptor
    //     * @return
    //     */
    //    @NotNull
    //    static public CallableDescriptor conversion(
    //            @NotNull CallableDescriptor superDescriptor,
    //            @NotNull CallableDescriptor subDescriptor
    //
    //    ) {
    //        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
    //        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();
    //
    //        if(subDescriptor.getExtensionReceiverParameter() != null &&  superTypeParameters.isEmpty()){
    //            return subDescriptor;
    //        }
    //
    //        return subDescriptor;
    //    }
    /**
     * 提取并绑定成员的覆盖信息
     * 该方法用于处理当前类成员描述符与超类成员描述符之间的关系，根据可覆盖性将合适的超类成员绑定到当前类成员
     *
     * @param fromCurrent          当前类的成员描述符，用于检查与超类成员的覆盖关系
     * @param descriptorsFromSuper 超类的成员描述符集合，作为潜在被覆盖的成员
     * @param current              当前类的描述符，用于获取类上下文信息
     * @param strategy             覆盖策略接口，用于处理覆盖冲突等情况
     * @return 返回绑定后的成员描述符集合，包括了当前类成员覆盖的超类成员
     */
    private fun extractAndBindOverridesForMember(
        fromCurrent: CallableMemberDescriptor,
        descriptorsFromSuper: MutableCollection<out CallableMemberDescriptor>,
        current: ClassDescriptor,
        strategy: OverridingStrategy
    ): MutableCollection<CallableMemberDescriptor?> {
        // 初始化用于存储绑定后的成员描述符的集合
        val bound: MutableCollection<CallableMemberDescriptor?> =
            ArrayList<CallableMemberDescriptor?>(descriptorsFromSuper.size)
        // 初始化用于存储被当前类成员覆盖的超类成员描述符的集合
        val overridden: MutableCollection<CallableMemberDescriptor> = SmartSet.create()

        // 遍历超类成员描述符集合，检查每个成员是否被当前类成员覆盖
        for (fromSupertype in descriptorsFromSuper) {
            // 检查超类成员是否可被当前类成员覆盖，以及覆盖的可见性
            val resultInfo = isOverridableBy(fromSupertype, fromCurrent, current)

            val result = resultInfo.result
            val isVisibleForOverride: Boolean = isVisibleForOverride(fromCurrent, fromSupertype, false)

            // 根据覆盖检查结果，决定如何处理当前超类成员
            when (result) {
                OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                    // 如果超类成员可被覆盖且可见，则将其添加到被覆盖成员集合中，并将其绑定到当前类成员
                    if (isVisibleForOverride) {
                        overridden.add(fromSupertype)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.STATIC_CONFLICT -> {
                    // 如果存在静态覆盖冲突且可见，则调用覆盖策略处理冲突，并将其绑定到当前类成员
                    if (isVisibleForOverride) {
                        strategy.staticConflict(fromSupertype, fromCurrent, resultInfo.debugMessage)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.CONFLICT -> {
                    // 如果存在覆盖冲突且可见，则调用覆盖策略处理冲突，并将其绑定到当前类成员
                    if (isVisibleForOverride) {
                        strategy.overrideConflict(fromSupertype, fromCurrent)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.INCOMPATIBLE -> {}
            }
        }

        // 设置当前类成员所覆盖的超类成员描述符集合
        strategy.setOverriddenDescriptors(fromCurrent, overridden)

        // 返回绑定后的成员描述符集合
        return bound
    }

    fun <T : CallableMemberDescriptor?> generateOverridesInFunctionGroup(
        name: Name,  //DO NOT DELETE THIS PARAMETER: needed to make sure all descriptors have the same name
        membersFromSupertypes: MutableCollection<T?>,
        membersFromCurrent: MutableCollection<T?>,
        current: ClassDescriptor,
        strategy: OverridingStrategy
    ) {
        val notOverridden: MutableCollection<CallableMemberDescriptor?> =
            LinkedHashSet<CallableMemberDescriptor?>(membersFromSupertypes)

        //        排除来自扩展的方法
        for (member in notOverridden) {
            if (member is FunctionDescriptor) {
                run {
                    if (member.isExtend) {
                        membersFromCurrent.add(member as T)
                    }
                }
            }
        }


        for (fromCurrent in membersFromCurrent) {
            val bound =
                extractAndBindOverridesForMember(fromCurrent!!, membersFromSupertypes, current, strategy)
            notOverridden.removeAll(bound)
        }

        createAndBindFakeOverrides(current, notOverridden, strategy)
    }

    private fun createTypeCheckerState(
        firstParameters: MutableList<TypeParameterDescriptor?>,
        secondParameters: MutableList<TypeParameterDescriptor?>
    ): TypeCheckerState {
        assert(firstParameters.size == secondParameters.size) { "Should be the same number of type parameters: " + firstParameters + " vs " + secondParameters }

        if (firstParameters.isEmpty()) {
            return OverridingUtilTypeSystemContext(
                null, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
            ).newTypeCheckerState(true, true)
        }

        val matchingTypeConstructors: MutableMap<TypeConstructor?, TypeConstructor?> =
            HashMap<TypeConstructor?, TypeConstructor?>()
        for (i in firstParameters.indices) {
            matchingTypeConstructors.put(
                firstParameters.get(i)!!.typeConstructor,
                secondParameters.get(i)!!.typeConstructor
            )
        }

        return OverridingUtilTypeSystemContext(
            matchingTypeConstructors, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
        ).newTypeCheckerState(true, true)
    }

    /**
     * 检查一个描述符是否可以在不考虑外部条件的情况下覆盖另一个描述符
     * 此方法主要用于检查继承结构中方法的覆盖是否存在问题
     * 它通过比较超类和子类中的方法参数类型和返回类型来确定是否可以进行覆盖
     *
     * @param superDescriptor 超类的方法描述符
     * @param subDescriptor   子类的方法描述符
     * @param checkReturnType 是否检查返回类型
     * @return 返回一个OverrideCompatibilityInfo对象，包含覆盖是否成功或失败的原因
     */
    fun isOverridableByWithoutExternalConditions(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        checkReturnType: Boolean
    ): OverrideCompatibilityInfo {
        // 检查基本的覆盖问题，如抽象方法是否可以覆盖具体方法等
        val basicOverridability: OverrideCompatibilityInfo? =
            getBasicOverridabilityProblem(superDescriptor, subDescriptor)
        if (null != basicOverridability) return basicOverridability

        // 获取超类和子类方法的参数类型列表
        val superValueParameters: MutableList<CangJieType?> = compiledValueParameters(superDescriptor)
        val subValueParameters: MutableList<CangJieType?> = compiledValueParameters(subDescriptor)

        // 获取超类方法的类型参数列表
        val superTypeParameters: MutableList<TypeParameterDescriptor?> = superDescriptor.typeParameters

        // 当成员为扩展时，有且只有一个类型参数，并且为扩展类型中的泛型时，需特殊处理
        val subTypeParameters: MutableList<TypeParameterDescriptor?> = subDescriptor.typeParametersNotExtend

        // 如果类型参数数量不匹配，则进一步检查参数类型是否兼容
        if (superTypeParameters.size != subTypeParameters.size) {
            for (i in superValueParameters.indices) {
                // TODO: compare erasure
                if (!CangJieTypeChecker.DEFAULT.equalTypes(
                        superValueParameters.get(i)!!,
                        subValueParameters.get(i)!!
                    )
                ) {
                    return OverrideCompatibilityInfo.Companion.incompatible("Type parameter number mismatch")
                }
            }
            return OverrideCompatibilityInfo.Companion.conflict("Type parameter number mismatch")
        }

        // 创建类型检查状态对象，用于后续的类型参数和值参数的比较
        val typeCheckerState = createTypeCheckerState(superTypeParameters, subTypeParameters)

        // 检查类型参数是否等价
        for (i in superTypeParameters.indices) {
            if (!Companion.areTypeParametersEquivalent(
                    superTypeParameters.get(i)!!,
                    subTypeParameters.get(i)!!,
                    typeCheckerState
                )
            ) {
                return OverrideCompatibilityInfo.Companion.incompatible("Type parameter bounds mismatch")
            }
        }

        // 检查值参数类型是否等价
        for (i in superValueParameters.indices) {
            if (!Companion.areTypesEquivalent(
                    superValueParameters.get(i)!!,
                    subValueParameters.get(i)!!,
                    typeCheckerState
                )
            ) {
                return OverrideCompatibilityInfo.Companion.incompatible("Value parameter type mismatch")
            }
        }


        // 检查返回类型是否兼容，如果需要检查返回类型的话
        if (checkReturnType) {
            val superReturnType = superDescriptor.returnType
            val subReturnType = subDescriptor.returnType

            if (null != superReturnType && null != subReturnType) {
                val bothErrors = subReturnType.isError && superReturnType.isError
                if (!bothErrors &&
                    !isSubtypeOf(
                        typeCheckerState,
                        subReturnType.unwrap(),
                        superReturnType.unwrap()
                    )
                ) {
                    return OverrideCompatibilityInfo.Companion.conflict("Return type mismatch")
                }
            }
        }


        // 如果所有检查都通过，则表示可以成功覆盖
        return OverrideCompatibilityInfo.Companion.success()
    }

    /**
     * 判断一个方法是否可以被重写。
     *
     * @param superDescriptor    父类的方法描述符。
     * @param subDescriptor      子类的方法描述符。
     * @param subClassDescriptor 子类的类描述符，可以为 null。
     * @param checkReturnType    是否检查返回类型。
     * @return 返回一个 [OverrideCompatibilityInfo] 对象，表示是否可以重写以及相关信息。
     *
     *
     * 方法执行流程如下：
     * 1. 首先调用 `isOverridableByWithoutExternalConditions` 方法进行基础的可重写性检查，
     * 判断是否可以在不考虑外部条件的情况下重写方法。
     * 2. 遍历所有外部的可重写性条件 [ExternalOverridabilityCondition]，根据其契约（`Contract`）：
     * - 如果契约是 `SUCCESS_ONLY`，则只在基本检查成功时执行。
     * - 如果契约是 `CONFLICTS_ONLY`，则跳过本次检查。
     * 3. 如果某个外部条件返回 `OVERRIDABLE`，则标记检查成功。
     * - 如果返回 `INCOMPATIBLE`，立即返回一个表示不兼容的结果。
     * - 如果返回 `UNKNOWN`，继续下一个条件检查。
     * 4. 如果基础检查失败且没有其他条件声明成功，直接返回基础检查的结果。
     * 5. 再次遍历所有外部条件，但这次仅运行契约为 `CONFLICTS_ONLY` 的条件，
     * 用于检测潜在的冲突：
     * - 如果条件返回 `INCOMPATIBLE`，返回不兼容结果。
     * - 如果条件返回 `OVERRIDABLE`，抛出异常，因为这违反了契约约定。
     * - 如果条件返回 `UNKNOWN`，继续下一个条件检查。
     * 6. 如果没有冲突且至少一个检查成功，则返回成功的结果。
     * @throws IllegalStateException 如果某个契约为 `CONFLICTS_ONLY` 的条件返回成功，违反契约。
     */
    fun isOverridableBy(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?,
        checkReturnType: Boolean
    ): OverrideCompatibilityInfo {
        val basicResult = isOverridableByWithoutExternalConditions(superDescriptor, subDescriptor, checkReturnType)
        var wasSuccess = OverrideCompatibilityInfo.Result.OVERRIDABLE == basicResult.result

        for (externalCondition in EXTERNAL_CONDITIONS) {
            // Do not run CONFLICTS_ONLY while there was no success
            if (ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY == externalCondition.contract) continue
            if (wasSuccess && ExternalOverridabilityCondition.Contract.SUCCESS_ONLY == externalCondition.contract) continue

            val result =
                externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor)

            when (result) {
                ExternalOverridabilityCondition.Result.OVERRIDABLE -> wasSuccess = true
                ExternalOverridabilityCondition.Result.INCOMPATIBLE -> return OverrideCompatibilityInfo.Companion.incompatible(
                    "External condition"
                )

                ExternalOverridabilityCondition.Result.UNKNOWN -> {}
            }
        }

        if (!wasSuccess) {
            return basicResult
        }

        // Search for conflicts from external conditions
        for (externalCondition in EXTERNAL_CONDITIONS) {
            // Run all conditions that was not run before (i.e. CONFLICTS_ONLY)
            if (ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY != externalCondition.contract) continue

            val result =
                externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor)
            when (result) {
                ExternalOverridabilityCondition.Result.INCOMPATIBLE -> return OverrideCompatibilityInfo.Companion.incompatible(
                    "External condition"
                )

                ExternalOverridabilityCondition.Result.OVERRIDABLE -> throw IllegalStateException(
                    "Contract violation in " + externalCondition.javaClass.getName() + " condition. It's not supposed to end with success"
                )

                ExternalOverridabilityCondition.Result.UNKNOWN -> {}
            }
        }

        return OverrideCompatibilityInfo.Companion.success()
    }

    fun isOverridableBy(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?
    ): OverrideCompatibilityInfo {
        return isOverridableBy(superDescriptor, subDescriptor, subClassDescriptor, false)
    }

    class OverrideCompatibilityInfo(val result: Result, val debugMessage: String) {
        override fun toString(): String {
            return result.toString() + ": " + debugMessage
        }

        /**
         * 枚举类型Result用于表示不同结果状态
         * 这些结果状态可能在某些方法或逻辑判断中被使用，以决定程序的行为
         */
        enum class Result {
            /**
             * 可以被覆盖的结果状态
             * 这通常意味着当前结果可以被后续的某个操作或值覆盖
             */
            OVERRIDABLE,

            /**
             * 不兼容的结果状态
             * 这表示当前结果与预期或某些条件不兼容，可能需要特殊处理
             */
            INCOMPATIBLE,

            /**
             * 冲突的结果状态
             * 这通常意味着当前操作或值与已存在的状态存在冲突，需要解决
             */
            CONFLICT,

            /**
             * 静态冲突的结果状态
             */
            STATIC_CONFLICT
        }

        companion object {
            private val SUCCESS = OverrideCompatibilityInfo(Result.OVERRIDABLE, "SUCCESS")
            fun success(): OverrideCompatibilityInfo {
                return SUCCESS
            }

            fun incompatible(debugMessage: String): OverrideCompatibilityInfo {
                return OverrideCompatibilityInfo(Result.INCOMPATIBLE, debugMessage)
            }

            fun staticConflict(debugMessage: String): OverrideCompatibilityInfo {
                return OverrideCompatibilityInfo(Result.STATIC_CONFLICT, debugMessage)
            }

            fun conflict(debugMessage: String): OverrideCompatibilityInfo {
                return OverrideCompatibilityInfo(Result.CONFLICT, debugMessage)
            }
        }
    }

    companion object {
        @JvmField
        val DEFAULT: OverridingUtil
        private val EXTERNAL_CONDITIONS: MutableList<ExternalOverridabilityCondition> =
            ServiceLoader.load<ExternalOverridabilityCondition?>(
                ExternalOverridabilityCondition::class.java,
                ExternalOverridabilityCondition::class.java.getClassLoader()
            ).toList<ExternalOverridabilityCondition?>()
        private val DEFAULT_TYPE_CONSTRUCTOR_EQUALITY =
            CangJieTypeChecker.TypeConstructorEquality { obj: TypeConstructor, obj: TypeConstructor -> obj.equals(obj) }

        init {
            DEFAULT = OverridingUtil(
                DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, CangJieTypeRefiner.Default, CangJieTypePreparator.Default,
                null
            )
        }

        fun isVisibleForOverride(
            overriding: MemberDescriptor,
            fromSuper: MemberDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            return !isPrivate(fromSuper.visibility) &&
                    isVisibleIgnoringReceiver(fromSuper, overriding, useSpecialRulesForPrivateSealedConstructors)
        }

        fun create(
            cangjieTypeRefiner: CangJieTypeRefiner,
            equalityAxioms: CangJieTypeChecker.TypeConstructorEquality
        ): OverridingUtil {
            return OverridingUtil(equalityAxioms, cangjieTypeRefiner, CangJieTypePreparator.Default, null)
        }

        private fun allHasSameContainingDeclaration(notOverridden: MutableCollection<CallableMemberDescriptor?>): Boolean {
            if (2 > notOverridden.size) return true

            val containingDeclaration = notOverridden.iterator().next()!!.containingDeclaration
            return notOverridden.all<CallableMemberDescriptor?> { descriptor: CallableMemberDescriptor? -> descriptor!!.containingDeclaration === containingDeclaration }
        }

        fun createWithTypeRefiner(cangjieTypeRefiner: CangJieTypeRefiner): OverridingUtil {
            return OverridingUtil(
                DEFAULT_TYPE_CONSTRUCTOR_EQUALITY,
                cangjieTypeRefiner,
                CangJieTypePreparator.Default,
                null
            )
        }

        /**
         * 创建并绑定未被覆盖的可调用成员的假覆盖。
         * 此方法用于处理未被覆盖的成员描述符的假覆盖创建，允许它们在当前类描述符中正确匹配。
         *
         * @param current       当前类描述符
         * @param notOverridden 未被覆盖的可调用成员描述符集合
         * @param strategy      覆盖策略
         */
        private fun createAndBindFakeOverrides(
            current: ClassDescriptor,
            notOverridden: MutableCollection<CallableMemberDescriptor?>,
            strategy: OverridingStrategy
        ) {
            // 优化：如果所有未被覆盖的描述符具有相同的包含声明，
            // 则可以直接为它们创建假覆盖，因为它们在其包含声明中应该能够正确匹配
            if (allHasSameContainingDeclaration(notOverridden)) {
                for (descriptor in notOverridden) {
                    Companion.createAndBindFakeOverride(
                        mutableSetOf<CallableMemberDescriptor?>(descriptor),
                        current,
                        strategy
                    )
                }
                return
            }

            // 使用队列处理未被覆盖的成员描述符
            val fromSuperQueue: Queue<CallableMemberDescriptor?> = LinkedList<CallableMemberDescriptor?>(notOverridden)
            while (!fromSuperQueue.isEmpty()) {
                // 查找具有最大可见性的未被覆盖成员
                val notOverriddenFromSuper = findMemberWithMaxVisibility(fromSuperQueue)
                // 提取可以在两个方向上覆盖的成员
                val overridables: MutableCollection<CallableMemberDescriptor> =
                    extractMembersOverridableInBothWays(notOverriddenFromSuper, fromSuperQueue, strategy)
                // 创建并绑定假覆盖
                createAndBindFakeOverride(overridables, current, strategy)
            }
        }


        private fun extractMembersOverridableInBothWays(
            overrider: CallableMemberDescriptor,
            extractFrom: Queue<CallableMemberDescriptor?>,
            strategy: OverridingStrategy
        ): MutableCollection<CallableMemberDescriptor> {
            return Companion.extractMembersOverridableInBothWays<CallableMemberDescriptor?>(
                overrider, extractFrom,  // ID
                { descriptor: CallableMemberDescriptor? -> descriptor },
                { descriptor: CallableMemberDescriptor? ->
                    strategy.inheritanceConflict(overrider, descriptor!!)
                    Unit
                })
        }

        private fun determineModalityForFakeOverride(
            descriptors: MutableCollection<CallableMemberDescriptor>,
            current: ClassDescriptor
        ): Modality {
            // Optimization: avoid creating hash sets in frequent cases when modality can be computed trivially
            var hasOpen = false
            var hasAbstract = false
            for (descriptor in descriptors) {
                when (descriptor.modality) {
                    Modality.FINAL -> return Modality.FINAL
                    Modality.SEALED -> throw IllegalStateException("Member cannot have SEALED modality: " + descriptor)
                    Modality.OPEN -> hasOpen = true
                    Modality.ABSTRACT -> hasAbstract = true
                }
            }

            // Fake overrides of abstract members in non-abstract expected classes should not be abstract, because otherwise it would be
            // impossible to inherit a non-expected class from that expected class in common code.
            // We're making their modality that of the containing class, because this is the least confusing behavior for the users.
            // However, it may cause problems if we reuse resolution results of common code when compiling platform code
            val transformAbstractToClassModality =
                (Modality.ABSTRACT != current.modality && Modality.SEALED != current.modality)

            if (hasOpen && !hasAbstract) {
                return Modality.OPEN
            }
            if (!hasOpen && hasAbstract) {
                return if (transformAbstractToClassModality) current.modality else Modality.ABSTRACT
            }

            val allOverriddenDeclarations: MutableSet<CallableMemberDescriptor?> = HashSet<CallableMemberDescriptor?>()
            for (descriptor in descriptors) {
                allOverriddenDeclarations.addAll(getOverriddenDeclarations(descriptor))
            }
            return Companion.getMinimalModality(
                filterOutOverridden<CallableMemberDescriptor?>(allOverriddenDeclarations),
                transformAbstractToClassModality,
                current.modality
            )
        }

        private fun getMinimalModality(
            descriptors: MutableCollection<CallableMemberDescriptor>,
            transformAbstractToClassModality: Boolean,
            classModality: Modality
        ): Modality {
            var result = Modality.ABSTRACT
            for (descriptor in descriptors) {
                val effectiveModality =
                    if (transformAbstractToClassModality && Modality.ABSTRACT == descriptor.modality)
                        classModality
                    else
                        descriptor.modality
                if (0 > effectiveModality.compareTo(result)) {
                    result = effectiveModality
                }
            }
            return result
        }

        fun filterVisibleFakeOverrides(
            current: ClassDescriptor,
            toFilter: MutableCollection<CallableMemberDescriptor>
        ): MutableCollection<CallableMemberDescriptor?> {
            return toFilter.filter<CallableMemberDescriptor?> { descriptor: CallableMemberDescriptor? ->
                !isPrivate(
                    descriptor!!.visibility
                ) &&
                        DescriptorVisibilities.isVisibleIgnoringReceiver(descriptor, current, false)
            }
        }


        private fun createAndBindFakeOverride(
            overridables: MutableCollection<CallableMemberDescriptor>,
            current: ClassDescriptor,
            strategy: OverridingStrategy
        ) {
            val visibleOverridables: MutableCollection<CallableMemberDescriptor?> =
                filterVisibleFakeOverrides(current, overridables)
            val allInvisible = visibleOverridables.isEmpty()
            val effectiveOverridden: MutableCollection<CallableMemberDescriptor> =
                if (allInvisible) overridables else visibleOverridables

            val modality: Modality = determineModalityForFakeOverride(effectiveOverridden, current)
            //        DescriptorVisibility visibility = allInvisible ? DescriptorVisibilities.INVISIBLE_FAKE : DescriptorVisibilities.INHERITED;
            val visibility =
                if (allInvisible) DescriptorVisibilities.INVISIBLE_FAKE else DescriptorVisibilities.INHERITED

            // FIXME doesn't work as expected for flexible types: should create a refined signature.
            // Current algorithm produces bad results in presence of annotated Java signatures such as:
            //      J: foo(s: String!): String -- @NotNull String foo(String s);
            //      K: foo(s: String): String?
            //  --> 'foo(s: String!): String' as an inherited signature with most specific return type.
            // This is bad because it can be overridden by 'foo(s: String?): String', which is not override-equivalent with K::foo above.
            // Should be 'foo(s: String): String'.
            val mostSpecific: CallableMemberDescriptor =
                Companion.selectMostSpecificMember<CallableMemberDescriptor>(
                    effectiveOverridden
                ) { descriptor: CallableMemberDescriptor -> descriptor }
            val fakeOverride =
                mostSpecific.copy(current, modality, visibility, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false)
            strategy.setOverriddenDescriptors(fakeOverride, effectiveOverridden)
            assert(
                !fakeOverride.overriddenDescriptors.isEmpty()
            ) { "Overridden descriptors should be set for " + CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            strategy.addFakeOverride(fakeOverride)
        }

        /**
         * @return overridden real descriptors (not fake overrides). Note that most usages of this method should be followed by calling
         * [.filterOutOverridden], because some of the declarations can override the other.
         */
        fun getOverriddenDeclarations(descriptor: CallableMemberDescriptor): MutableSet<CallableMemberDescriptor?> {
            val result: MutableSet<CallableMemberDescriptor?> = LinkedHashSet<CallableMemberDescriptor?>()
            collectOverriddenDeclarations(descriptor, result)
            return result
        }

        private fun collectOverriddenDeclarations(
            descriptor: CallableMemberDescriptor,
            result: MutableSet<CallableMemberDescriptor?>
        ) {
            if (descriptor.kind.isReal) {
                result.add(descriptor)
            } else {
                check(!descriptor.overriddenDescriptors.isEmpty()) { "No overridden descriptors found for (fake override) " + descriptor }
                for (overridden in descriptor.overriddenDescriptors) {
                    collectOverriddenDeclarations(overridden, result)
                }
            }
        }

        /**
         * @return whether f overrides g
         */
        fun <D : CallableDescriptor?> overrides(
            f: D,
            g: D,
            allowDeclarationCopies: Boolean,
            distinguishExpectsAndNonExpects: Boolean
        ): Boolean {
            // In a multi-module project different "copies" of the same class may be present in different libraries,
            // that's why we use structural equivalence for members (DescriptorEquivalenceForOverrides).

            // This first check cover the case of duplicate classes in different modules:
            // when B is defined in modules m1 and m2, and C (indirectly) inherits from both versions,
            // we'll be getting sets of members that do not override each other, but are structurally equivalent.
            // As other code relies on no equal descriptors passed here, we guard against f == g, but this may not be necessary
            // Note that this is needed for the usage of this function in the IDE code

            if (f != g && areEquivalent(
                    f!!.original,
                    g!!.original,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
                )
            ) {
                return true
            }

            val originalG: CallableDescriptor? = g!!.original
            for (overriddenFunction in DescriptorUtils.getAllOverriddenDescriptors<D>(f!!)) {
                if (areEquivalent(
                        originalG,
                        overriddenFunction,
                        allowDeclarationCopies,
                        distinguishExpectsAndNonExpects
                    )
                ) {
                    return true
                }
            }
            return false
        }

        fun <D> filterOverrides(
            candidateSet: MutableSet<D?>,
            allowDescriptorCopies: Boolean,
            cancellationCallback: Function0<*>?,
            transformFirst: Function2<in D?, in D?, Pair<CallableDescriptor, CallableDescriptor>>
        ): MutableSet<D?> {
            if (1 >= candidateSet.size) return candidateSet

            val result: MutableSet<D?> = LinkedHashSet<D?>()
            outerLoop@ for (meD in candidateSet) {
                if (null != cancellationCallback) {
                    cancellationCallback.invoke()
                }
                val iterator = result.iterator()
                while (iterator.hasNext()) {
                    val otherD = iterator.next()
                    val meAndOther = transformFirst.invoke(meD, otherD)
                    val me = meAndOther.component1()
                    val other = meAndOther.component2()
                    if (overrides<CallableDescriptor?>(me, other, allowDescriptorCopies, true)) {
                        iterator.remove()
                    } else if (overrides<CallableDescriptor?>(other, me, allowDescriptorCopies, true)) {
                        continue@outerLoop
                    }
                }
                result.add(meD)
            }

            assert(!result.isEmpty()) { "All candidates filtered out from " + candidateSet }

            return result
        }

        private fun isVisibilityMoreSpecific(
            a: DeclarationDescriptorWithVisibility,
            b: DeclarationDescriptorWithVisibility
        ): Boolean {
            val result = compare(a.visibility, b.visibility)
            return null == result || 0 <= result
        }

        private fun isReturnTypeMoreSpecific(
            a: CallableDescriptor,
            aReturnType: CangJieType,
            b: CallableDescriptor,
            bReturnType: CangJieType,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            return isSubtypeOf(typeCheckerState, aReturnType.unwrap(), bReturnType.unwrap())
        }

        fun isMoreSpecific(a: CallableDescriptor, b: CallableDescriptor): Boolean {
            val aReturnType: CangJieType = a.returnType!!
            val bReturnType: CangJieType = b.returnType!!

            checkNotNull(aReturnType) { "Return type of " + a + " is null" }
            checkNotNull(bReturnType) { "Return type of " + b + " is null" }

            if (!isVisibilityMoreSpecific(a, b)) return false


            val checkerState: TypeCheckerState =
                DEFAULT.createTypeCheckerState(a.typeParameters, b.typeParameters)

            if (a is FunctionDescriptor) {
                assert(b is FunctionDescriptor) { "b is " + b.javaClass }

                return isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checkerState)
            }
            if (a is VariableDescriptor) {
                assert(b is VariableDescriptor) { "b is " + b.javaClass }

                val pb = b as VariableDescriptor

                //            if (!isAccessorMoreSpecific(pa.getSetter(), pb.getSetter())) return false;
                if (a.isVar && pb.isVar) {
                    // TODO(dsavvinov): using DEFAULT here looks suspicious
                    return equalTypes(checkerState, aReturnType.unwrap(), bReturnType.unwrap())
                } else {
                    // both vals or var vs val: val can't be more specific then var
                    return !(!a.isVar && pb.isVar) && isReturnTypeMoreSpecific(
                        a,
                        aReturnType,
                        b,
                        bReturnType,
                        checkerState
                    )
                }
            }
            throw IllegalArgumentException("Unexpected callable: " + a.javaClass)
        }

        private fun isMoreSpecificThenAllOf(
            candidate: CallableDescriptor,
            descriptors: MutableCollection<CallableDescriptor>
        ): Boolean {
            // NB subtyping relation in CangJie is not transitive in presence of flexible types:
            //  String? <: String! <: String, but not String? <: String
            for (descriptor in descriptors) {
                if (!isMoreSpecific(candidate, descriptor)) {
                    return false
                }
            }
            return true
        }

        //    private static bool isAccessorMoreSpecific(@Nullable PropertyAccessorDescriptor a, @Nullable PropertyAccessorDescriptor b) {
        //        if (a == null || b == null) return true;
        //        return isVisibilityMoreSpecific(a, b);
        //    }
        fun <H> selectMostSpecificMember(
            overridables: MutableCollection<H?>,
            descriptorByHandle: Function1<H?, CallableDescriptor>
        ): H {
            assert(!overridables.isEmpty()) { "Should have at least one overridable descriptor" }

            if (1 == overridables.size) {
                return overridables.first<H?>()
            }

            val candidates: MutableCollection<H?> = ArrayList<H?>(2)
            val callableMemberDescriptors: MutableList<CallableDescriptor> =
                overridables.map<H?, CallableDescriptor?>(descriptorByHandle)

            var transitivelyMostSpecific = overridables.first<H?>()
            val transitivelyMostSpecificDescriptor = descriptorByHandle.invoke(transitivelyMostSpecific)

            for (overridable in overridables) {
                val descriptor = descriptorByHandle.invoke(overridable)
                if (isMoreSpecificThenAllOf(descriptor, callableMemberDescriptors)) {
                    candidates.add(overridable)
                }
                if (isMoreSpecific(descriptor, transitivelyMostSpecificDescriptor)
                    && !isMoreSpecific(transitivelyMostSpecificDescriptor, descriptor)
                ) {
                    transitivelyMostSpecific = overridable
                }
            }

            if (candidates.isEmpty()) {
                return transitivelyMostSpecific
            } else if (1 == candidates.size) {
                return candidates.first<H?>()
            }

            var firstNonFlexible: H? = null
            for (candidate in candidates) {
                if (!descriptorByHandle.invoke(candidate).returnType!!.isFlexible()) {
                    firstNonFlexible = candidate
                    break
                }
            }
            if (null != firstNonFlexible) {
                return firstNonFlexible
            }

            return candidates.first<H?>()
        }

        /**
         * @param <H> is something that handles CallableDescriptor inside
         * @return
        </H> */
        fun <H> extractMembersOverridableInBothWays(
            overrider: H,
            extractFrom: MutableCollection<H?>,
            descriptorByHandle: Function1<H?, CallableDescriptor>,
            onConflict: Function1<H?, Unit?>
        ): MutableCollection<H?> {
            val overridable: MutableCollection<H?> = ArrayList<H?>()
            overridable.add(overrider)
            val overriderDescriptor = descriptorByHandle.invoke(overrider)
            val iterator = extractFrom.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val candidateDescriptor = descriptorByHandle.invoke(candidate)
                if (overrider === candidate) {
                    iterator.remove()
                    continue
                }

                val finalResult: OverrideCompatibilityInfo.Result? =
                    getBothWaysOverridability(overriderDescriptor, candidateDescriptor)

                if (OverrideCompatibilityInfo.Result.OVERRIDABLE == finalResult) {
                    overridable.add(candidate)
                    iterator.remove()
                } else if (OverrideCompatibilityInfo.Result.CONFLICT == finalResult) {
                    onConflict.invoke(candidate)
                    iterator.remove()
                }
            }
            return overridable
        }

        private fun compiledValueParameters(callableDescriptor: CallableDescriptor): MutableList<CangJieType?> {
//        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getExtensionReceiverParameter();
            val parameters: MutableList<CangJieType?> = ArrayList<CangJieType?>()
            //        if (receiverParameter != null) {
//            parameters.add(receiverParameter.getType());
//        }
            for (valueParameterDescriptor in callableDescriptor.valueParameters) {
                parameters.add(valueParameterDescriptor.type)
            }
            return parameters
        }

        /**
         * 检查两个描述符之间的基本覆盖（重写）兼容性问题
         * 此方法主要用于识别在 attempting to override a member in a superclass or interface 时是否存在基本的不兼容问题
         * 它不考虑更复杂的覆盖规则或条件，只提供初步的兼容性检查
         *
         * @param superDescriptor 超类或接口的描述符
         * @param subDescriptor   子类的描述符
         * @return 如果存在基本的覆盖兼容性问题，则返回问题的信息；否则返回 null
         */
        fun getBasicOverridabilityProblem(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo? {
            //        检查静态覆盖
            if (superDescriptor.isStatic != subDescriptor.isStatic) {
                if (subDescriptor.isStatic) {
                    return OverrideCompatibilityInfo.Companion.staticConflict(
                        CangJieDiagnosisBundle.message(
                            "CONFLICTING_STATIC_BY_STATIC_TO_NON_STATIC",
                            subDescriptor.name
                        )
                    )
                }
                return OverrideCompatibilityInfo.Companion.staticConflict(
                    CangJieDiagnosisBundle.message(
                        "CONFLICTING_STATIC_BY_NON_STATIC_TO_STATIC",
                        subDescriptor.name
                    )
                )
            }
            // 检查是否涉及枚举类描述符，因为枚举成员不能覆盖非枚举成员
            if (subDescriptor is EnumClassCallableDescriptor || superDescriptor is EnumClassCallableDescriptor) {
                return OverrideCompatibilityInfo.Companion.incompatible("Enum member cannot override non-enum member")
            }

            // 检查成员种类是否匹配，即函数描述符只能被函数描述符覆盖，变量描述符只能被变量描述符覆盖
            if (superDescriptor is FunctionDescriptor && subDescriptor !is FunctionDescriptor ||
                superDescriptor is VariableDescriptor && subDescriptor !is VariableDescriptor
            ) {
                return OverrideCompatibilityInfo.Companion.incompatible("Member kind mismatch")
            }

            // 确保超类描述符是可检查覆盖性的类型
            require(!(superDescriptor !is FunctionDescriptor && superDescriptor !is VariableDescriptor)) { "This type of CallableDescriptor cannot be checked for overridability: " + superDescriptor }

            // 检查成员名称是否相同，名称不相同不能覆盖
            // TODO: check outside of this method
            if (!superDescriptor.name.equals(subDescriptor.name)) {
                return OverrideCompatibilityInfo.Companion.incompatible("Name mismatch")
            }

            // 进一步检查接收者和参数数量的兼容性
            return checkReceiverAndParameterCount(superDescriptor, subDescriptor)
        }

        /**
         * 检查接收器和参数数量的兼容性
         *
         *
         * 此方法用于检查超类和子类的 CallableDescriptor 对象的接收器和参数数量是否兼容。
         * 如果接收器存在性不匹配或参数数量不匹配，则返回一个 OverrideCompatibilityInfo 对象，表明存在兼容性问题。
         *
         * @param superDescriptor 超类的 CallableDescriptor 对象
         * @param subDescriptor   子类的 CallableDescriptor 对象
         * @return 如果存在兼容性问题，返回相应的 OverrideCompatibilityInfo 对象；如果没有问题，返回 null
         */
        private fun checkReceiverAndParameterCount(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo? {
            //        TODO 如果使用扩展接收器，那么这里就要注释掉
            // 检查接收器是否存在，如果存在性不匹配，则返回一个 OverrideCompatibilityInfo 对象
//        if ((superDescriptor.getExtensionReceiverParameter() == null)!= (subDescriptor.getExtensionReceiverParameter() == null)) {
//            return OverrideCompatibilityInfo.incompatible("Receiver presence mismatch");
//        }

            // 检查参数数量是否匹配，如果不匹配，则返回一个 OverrideCompatibilityInfo 对象

            if (superDescriptor.valueParameters.size() !== subDescriptor.valueParameters.size()) {
                return OverrideCompatibilityInfo.Companion.incompatible("Value parameter number mismatch")
            }

            // 如果没有发现兼容性问题，则返回 null
            return null
        }


        /**
         * 检查两种类型是否等价
         * 此方法用于比较在超类和子类中使用的两种类型是否等价，以支持类型检查和推断
         * 它首先检查两种类型是否都为错误类型，如果是，则认为它们等价
         * 如果两种类型都不是错误类型，则委托给AbstractTypeChecker进行类型等价性比较
         *
         * @param typeInSuper      超类中的类型
         * @param typeInSub        子类中的类型
         * @param typeCheckerState 类型检查器的状态，用于跟踪类型检查过程中的信息
         * @return 如果两种类型等价则返回true，否则返回false
         */
        private fun areTypesEquivalent(
            typeInSuper: CangJieType,
            typeInSub: CangJieType,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            // 检查两种类型是否都为错误类型，如果是，则认为它们等价
            val bothErrors = typeInSuper.isError && typeInSub.isError
            if (bothErrors) return true
            // 如果两种类型都不是错误类型，则委托给AbstractTypeChecker进行类型等价性比较
            return equalTypes(typeCheckerState, typeInSuper.unwrap(), typeInSub.unwrap())
        }

        // See JLS 8, 8.4.4 Generic Methods
        private fun areTypeParametersEquivalent(
            superTypeParameter: TypeParameterDescriptor,
            subTypeParameter: TypeParameterDescriptor,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            val superBounds: MutableList<CangJieType> = superTypeParameter.upperBounds
            val subBounds: MutableList<CangJieType?> = ArrayList<CangJieType?>(subTypeParameter.upperBounds)
            if (superBounds.size != subBounds.size) return false

            outer@ for (superBound in superBounds) {
                val it: MutableListIterator<CangJieType> = subBounds.listIterator()
                while (it.hasNext()) {
                    val subBound = it.next()
                    if (areTypesEquivalent(superBound, subBound, typeCheckerState)) {
                        it.remove()
                        continue@outer
                    }
                }
                return false
            }

            return true
        }

        fun getBothWaysOverridability(
            overriderDescriptor: CallableDescriptor,
            candidateDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo.Result? {
            val result1: OverrideCompatibilityInfo.Result =
                DEFAULT.isOverridableBy(
                    candidateDescriptor,
                    overriderDescriptor,
                    null
                ).result
            val result2: OverrideCompatibilityInfo.Result =
                DEFAULT.isOverridableBy(
                    overriderDescriptor,
                    candidateDescriptor,
                    null
                ).result

            return if (OverrideCompatibilityInfo.Result.OVERRIDABLE == result1 && OverrideCompatibilityInfo.Result.OVERRIDABLE == result2) OverrideCompatibilityInfo.Result.OVERRIDABLE else
                (if (OverrideCompatibilityInfo.Result.CONFLICT == result1 || OverrideCompatibilityInfo.Result.CONFLICT == result2) OverrideCompatibilityInfo.Result.CONFLICT else OverrideCompatibilityInfo.Result.INCOMPATIBLE)
        }

        /**
         * Given a set of descriptors, returns a set containing all the given descriptors except those which _are overridden_ by at least
         * one other descriptor from the original set.
         */
        fun <D : CallableDescriptor?> filterOutOverridden(candidateSet: MutableSet<D?>): MutableSet<D?> {
//        boolean allowDescriptorCopies = !candidateSet.isEmpty() &&
//                DescriptorUtilsKt
//                        .isTypeRefinementEnabled(DescriptorUtilsKt.getModule(candidateSet.iterator().next()));
//
//        return filterOverrides(candidateSet, allowDescriptorCopies, null, new Function2<D, D, Pair<CallableDescriptor, CallableDescriptor>>() {
//            @Override
//            public Pair<CallableDescriptor, CallableDescriptor> invoke(D a, D b) {
//                return new Pair<CallableDescriptor, CallableDescriptor>(a, b);
//            }
//        });
            return candidateSet
        }

        fun resolveUnknownVisibilityForMember(
            memberDescriptor: CallableMemberDescriptor,
            cannotInferVisibility: Function1<CallableMemberDescriptor?, Unit?>?
        ) {
            for (descriptor in memberDescriptor.overriddenDescriptors) {
                if (descriptor.visibility === DescriptorVisibilities.INHERITED) {
                    resolveUnknownVisibilityForMember(descriptor, cannotInferVisibility)
                }
            }

            if (memberDescriptor.visibility !== DescriptorVisibilities.INHERITED) {
                return
            }

            val maxVisibility: DescriptorVisibility? = computeVisibilityToInherit(memberDescriptor)
            val visibilityToInherit: DescriptorVisibility?
            if (null == maxVisibility) {
                if (null != cannotInferVisibility) {
                    cannotInferVisibility.invoke(memberDescriptor)
                }
                visibilityToInherit = DescriptorVisibilities.PUBLIC
            } else {
                visibilityToInherit = maxVisibility
            }

            if (memberDescriptor is PropertyDescriptorImpl) {
                memberDescriptor.visibility = visibilityToInherit
                for (accessor in (memberDescriptor as PropertyDescriptor).accessors) {
                    // If we couldn't infer visibility for property, the diagnostic is already reported, no need to report it again on accessors
                    resolveUnknownVisibilityForMember(
                        accessor,
                        if (null == maxVisibility) null else cannotInferVisibility
                    )
                }
            } /*else   if (memberDescriptor instanceof VariableDescriptorImpl) {
            ((VariableDescriptorImpl) memberDescriptor).setVisibility(visibilityToInherit);

        } */ else if (memberDescriptor is FunctionDescriptorImpl) {
                memberDescriptor.visibility = visibilityToInherit
            } else {
                assert(memberDescriptor is PropertyAccessorDescriptorImpl)
                val propertyAccessorDescriptor = memberDescriptor as PropertyAccessorDescriptorImpl
                propertyAccessorDescriptor.visibility = visibilityToInherit
                if (visibilityToInherit !== propertyAccessorDescriptor.correspondingProperty.visibility) {
                    propertyAccessorDescriptor.isDefault = false
                }
            }
        }

        fun findMaxVisibility(descriptors: MutableCollection<out CallableMemberDescriptor>): DescriptorVisibility? {
            if (descriptors.isEmpty()) {
                return DescriptorVisibilities.DEFAULT_VISIBILITY
            }
            var maxVisibility: DescriptorVisibility? = null
            for (descriptor in descriptors) {
                val visibility = descriptor.visibility
                assert(visibility !== DescriptorVisibilities.INHERITED) { "Visibility should have been computed for " + descriptor }
                if (null == maxVisibility) {
                    maxVisibility = visibility
                    continue
                }
                val compareResult = compare(visibility, maxVisibility)
                if (null == compareResult) {
                    maxVisibility = null
                } else if (0 < compareResult) {
                    maxVisibility = visibility
                }
            }
            if (null == maxVisibility) {
                return null
            }
            for (descriptor in descriptors) {
                val compareResult = compare(maxVisibility, descriptor.visibility)
                if (null == compareResult || 0 > compareResult) {
                    return null
                }
            }
            return maxVisibility
        }

        private fun computeVisibilityToInherit(memberDescriptor: CallableMemberDescriptor): DescriptorVisibility? {
            val overriddenDescriptors: MutableCollection<out CallableMemberDescriptor> =
                memberDescriptor.overriddenDescriptors
            val maxVisibility: DescriptorVisibility? = findMaxVisibility(overriddenDescriptors)
            if (null == maxVisibility) {
                return null
            }
            if (CallableMemberDescriptor.Kind.FAKE_OVERRIDE == memberDescriptor.kind) {
                for (overridden in overriddenDescriptors) {
                    // An implementation (a non-abstract overridden member) of a fake override should have the maximum possible visibility
                    if (Modality.ABSTRACT != overridden.modality && !overridden.visibility.equals(maxVisibility)) {
                        return null
                    }
                }
                return maxVisibility
            }
            return maxVisibility.normalize()
        }
    }
}
