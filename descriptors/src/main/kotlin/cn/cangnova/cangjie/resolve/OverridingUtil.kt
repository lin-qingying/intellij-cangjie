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
import cn.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.PropertyDescriptorImpl
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.checker.CangJieTypePreparator
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.utils.SmartSet
import java.util.*

/**
 * 覆盖工具类
 *
 * 处理方法覆盖、可见性检查和假覆盖创建等功能
 */
class OverridingUtil private constructor(
    private val equalityAxioms: CangJieTypeChecker.TypeConstructorEquality,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
    private val cangjieTypePreparator: CangJieTypePreparator,
    private val customSubtype: ((CangJieType, CangJieType) -> Boolean)?
) {

    companion object {
        /**
         * 默认实例
         */
        val DEFAULT: OverridingUtil by lazy {
            OverridingUtil(
                { obj1, obj2 -> obj1 == obj2 },
                CangJieTypeRefiner.Default,
                CangJieTypePreparator.Default,
                null
            )
        }

        /**
         * 外部覆盖条件
         */
        private val EXTERNAL_CONDITIONS: List<ExternalOverridabilityCondition> by lazy {
            ServiceLoader.load(
                ExternalOverridabilityCondition::class.java,
                ExternalOverridabilityCondition::class.java.classLoader
            ).toList()
        }

        /**
         * 检查成员是否对覆盖可见
         */
        fun isVisibleForOverride(
            overriding: MemberDescriptor,
            fromSuper: MemberDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean = false
        ): Boolean = !DescriptorVisibilities.isPrivate(fromSuper.visibility) &&
                DescriptorVisibilities.isVisibleIgnoringReceiver(
                    fromSuper,
                    overriding,
                    useSpecialRulesForPrivateSealedConstructors
                )

        /**
         * 创建覆盖工具实例
         */
        fun create(
            cangjieTypeRefiner: CangJieTypeRefiner,
            equalityAxioms: CangJieTypeChecker.TypeConstructorEquality
        ): OverridingUtil = OverridingUtil(
            equalityAxioms,
            cangjieTypeRefiner,
            CangJieTypePreparator.Default,
            null
        )

        /**
         * 使用类型精炼器创建实例
         */
        fun createWithTypeRefiner(cangjieTypeRefiner: CangJieTypeRefiner): OverridingUtil =
            OverridingUtil(
                { obj1, obj2 -> obj1 == obj2 },
                cangjieTypeRefiner,
                CangJieTypePreparator.Default,
                null
            )

        /**
         * 检查所有描述符是否有相同的包含声明
         */
        private fun allHasSameContainingDeclaration(notOverridden: Collection<CallableMemberDescriptor>): Boolean {
            if (notOverridden.size < 2) return true

            val containingDeclaration = notOverridden.first().containingDeclaration
            return notOverridden.all { it.containingDeclaration == containingDeclaration }
        }

        /**
         * 过滤可见的假覆盖
         */
        fun filterVisibleFakeOverrides(
            current: ClassDescriptor,
            toFilter: Collection<CallableMemberDescriptor>
        ): Collection<CallableMemberDescriptor> = toFilter.filter { descriptor ->
            !DescriptorVisibilities.isPrivate(descriptor.visibility) &&
                    DescriptorVisibilities.isVisibleIgnoringReceiver(descriptor, current, false)
        }

        /**
         * 获取覆盖的声明
         */
        fun getOverriddenDeclarations(descriptor: CallableMemberDescriptor): Set<CallableMemberDescriptor> =
            mutableSetOf<CallableMemberDescriptor>().apply {
                collectOverriddenDeclarations(descriptor, this)
            }

        /**
         * 递归收集覆盖的声明
         */
        private fun collectOverriddenDeclarations(
            descriptor: CallableMemberDescriptor,
            result: MutableSet<CallableMemberDescriptor>
        ) {
            when {
                descriptor.kind.isReal -> result.add(descriptor)
                descriptor.overriddenDescriptors.isEmpty() ->
                    error("No overridden descriptors found for (fake override) $descriptor")

                else -> descriptor.overriddenDescriptors.forEach {
                    collectOverriddenDeclarations(it, result)
                }
            }
        }

        /**
         * 检查函数是否覆盖另一个函数
         */
        fun <D : CallableDescriptor> overrides(
            f: D,
            g: D,
            allowDeclarationCopies: Boolean = false,
            distinguishExpectsAndNonExpects: Boolean = true
        ): Boolean {
            // 结构等价性检查
            if (f != g && DescriptorEquivalenceForOverrides.areEquivalent(
                    f.original,
                    g.original,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
                )
            ) {
                return true
            }

            val originalG = g.original
            return DescriptorUtils.getAllOverriddenDescriptors(f).any { overriddenFunction ->
                DescriptorEquivalenceForOverrides.areEquivalent(
                    originalG,
                    overriddenFunction,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
                )
            }
        }

        /**
         * 过滤覆盖关系
         */
        fun <D> filterOverrides(
            candidateSet: Set<D>,
            allowDescriptorCopies: Boolean = false,
            cancellationCallback: (() -> Unit)? = null,
            transformFirst: (D, D) -> Pair<CallableDescriptor, CallableDescriptor>
        ): Set<D> {
            if (candidateSet.size <= 1) return candidateSet

            val result = linkedSetOf<D>()

            candidateSet@ for (meD in candidateSet) {
                cancellationCallback?.invoke()

                val iterator = result.iterator()
                while (iterator.hasNext()) {
                    val otherD = iterator.next()
                    val (me, other) = transformFirst(meD, otherD)

                    when {
                        overrides(me, other, allowDescriptorCopies, true) -> iterator.remove()
                        overrides(other, me, allowDescriptorCopies, true) -> continue@candidateSet
                    }
                }
                result.add(meD)
            }

            check(result.isNotEmpty()) { "All candidates filtered out from $candidateSet" }
            return result
        }

        /**
         * 过滤掉被覆盖的描述符
         */
        fun <D : CallableDescriptor> filterOutOverridden(candidateSet: Set<D>): Set<D> = candidateSet

        /**
         * 查找最大可见性
         */
        fun findMaxVisibility(descriptors: Collection<CallableMemberDescriptor>): DescriptorVisibility? {
            if (descriptors.isEmpty()) return DescriptorVisibilities.DEFAULT_VISIBILITY

            var maxVisibility: DescriptorVisibility? = null

            for (descriptor in descriptors) {
                val visibility = descriptor.visibility
                require(visibility != DescriptorVisibilities.INHERITED) {
                    "Visibility should have been computed for $descriptor"
                }

                when {
                    maxVisibility == null -> maxVisibility = visibility
                    else -> {
                        val compareResult = DescriptorVisibilities.compare(visibility, maxVisibility)
                        when {
                            compareResult == null -> maxVisibility = null
                            compareResult > 0 -> maxVisibility = visibility
                        }
                    }
                }
            }

            maxVisibility?.let { max ->
                for (descriptor in descriptors) {
                    val compareResult = DescriptorVisibilities.compare(max, descriptor.visibility)
                    if (compareResult == null || compareResult < 0) {
                        return null
                    }
                }
            }

            return maxVisibility
        }

        /**
         * 解析未知可见性
         */
        fun resolveUnknownVisibilityForMember(
            memberDescriptor: CallableMemberDescriptor,
            cannotInferVisibility: ((CallableMemberDescriptor) -> Unit)? = null
        ) {
            // 递归解析覆盖描述符的可见性
            memberDescriptor.overriddenDescriptors.forEach { descriptor ->
                if (descriptor.visibility == DescriptorVisibilities.INHERITED) {
                    resolveUnknownVisibilityForMember(descriptor, cannotInferVisibility)
                }
            }

            if (memberDescriptor.visibility != DescriptorVisibilities.INHERITED) return

            val maxVisibility = computeVisibilityToInherit(memberDescriptor)
            val visibilityToInherit = maxVisibility ?: run {
                cannotInferVisibility?.invoke(memberDescriptor)
                DescriptorVisibilities.PUBLIC
            }

            when (memberDescriptor) {
                is PropertyDescriptorImpl -> {
                    memberDescriptor.visibility = visibilityToInherit
                    (memberDescriptor as PropertyDescriptor).accessors.forEach { accessor ->
                        resolveUnknownVisibilityForMember(
                            accessor,
                            if (maxVisibility == null) null else cannotInferVisibility
                        )
                    }
                }

                is FunctionDescriptorImpl -> memberDescriptor.visibility = visibilityToInherit
                is PropertyAccessorDescriptorImpl -> {
                    memberDescriptor.visibility = visibilityToInherit
                    if (visibilityToInherit != memberDescriptor.correspondingProperty.visibility) {
                        memberDescriptor.isDefault = false
                    }
                }
            }
        }

        /**
         * 计算要继承的可见性
         */
        private fun computeVisibilityToInherit(memberDescriptor: CallableMemberDescriptor): DescriptorVisibility? {
            val overriddenDescriptors = memberDescriptor.overriddenDescriptors
            val maxVisibility = findMaxVisibility(overriddenDescriptors) ?: return null

            if (memberDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                for (overridden in overriddenDescriptors) {
                    if (overridden.modality != Modality.ABSTRACT && overridden.visibility != maxVisibility) {
                        return null
                    }
                }
                return maxVisibility
            }
            return maxVisibility.normalize()
        }

        /**
         * 检查是否更具体
         */
        fun isMoreSpecific(a: CallableDescriptor, b: CallableDescriptor): Boolean {
            val aReturnType = requireNotNull(a.returnType) { "Return type of $a is null" }
            val bReturnType = requireNotNull(b.returnType) { "Return type of $b is null" }

            if (!isVisibilityMoreSpecific(a, b)) return false

            val checkerState = DEFAULT.createTypeCheckerState(a.typeParameters, b.typeParameters)

            return when {
                a is FunctionDescriptor -> {
                    require(b is FunctionDescriptor) { "b is ${b::class}" }
                    isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checkerState)
                }

                a is VariableDescriptor -> {
                    require(b is VariableDescriptor) { "b is ${b::class}" }
                    when {
                        a.isVar && b.isVar ->
                            AbstractTypeChecker.equalTypes(checkerState, aReturnType.unwrap(), bReturnType.unwrap())

                        else ->
                            !(!a.isVar && b.isVar) && isReturnTypeMoreSpecific(
                                a,
                                aReturnType,
                                b,
                                bReturnType,
                                checkerState
                            )
                    }
                }

                else -> error("Unexpected callable: ${a::class}")
            }
        }

        /**
         * 检查可见性是否更具体
         */
        private fun isVisibilityMoreSpecific(
            a: DeclarationDescriptorWithVisibility,
            b: DeclarationDescriptorWithVisibility
        ): Boolean {
            val result = DescriptorVisibilities.compare(a.visibility, b.visibility)
            return result == null || result >= 0
        }

        /**
         * 检查返回类型是否更具体
         */
        private fun isReturnTypeMoreSpecific(
            a: CallableDescriptor,
            aReturnType: CangJieType,
            b: CallableDescriptor,
            bReturnType: CangJieType,
            typeCheckerState: TypeCheckerState
        ): Boolean = AbstractTypeChecker.isSubtypeOf(typeCheckerState, aReturnType.unwrap(), bReturnType.unwrap())

        /**
         * 检查是否比所有都更具体
         */
        private fun isMoreSpecificThenAllOf(
            candidate: CallableDescriptor,
            descriptors: Collection<CallableDescriptor>
        ): Boolean = descriptors.all { isMoreSpecific(candidate, it) }

        /**
         * 选择最具体的成员
         */
        fun <H> selectMostSpecificMember(
            overridables: Collection<H>,
            descriptorByHandle: (H) -> CallableDescriptor
        ): H {
            require(overridables.isNotEmpty()) { "Should have at least one overridable descriptor" }

            if (overridables.size == 1) return overridables.first()

            val candidates = mutableListOf<H>()
            val callableMemberDescriptors = overridables.map(descriptorByHandle)

            var transitivelyMostSpecific = overridables.first()
            var transitivelyMostSpecificDescriptor = descriptorByHandle(transitivelyMostSpecific)

            for (overridable in overridables) {
                val descriptor = descriptorByHandle(overridable)
                if (isMoreSpecificThenAllOf(descriptor, callableMemberDescriptors)) {
                    candidates.add(overridable)
                }
                if (isMoreSpecific(descriptor, transitivelyMostSpecificDescriptor) &&
                    !isMoreSpecific(transitivelyMostSpecificDescriptor, descriptor)
                ) {
                    transitivelyMostSpecific = overridable
                    transitivelyMostSpecificDescriptor = descriptor
                }
            }

            return when {
                candidates.isEmpty() -> transitivelyMostSpecific
                candidates.size == 1 -> candidates.first()
                else -> {
                    // 寻找第一个非flexible类型的候选
                    candidates.find { candidate ->
                        descriptorByHandle(candidate).returnType?.isFlexible() == false
                    } ?: candidates.first()
                }
            }
        }

        /**
         * 获取双向覆盖能力
         */
        fun getBothWaysOverridability(
            overriderDescriptor: CallableDescriptor,
            candidateDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo.Result {
            val result1 = DEFAULT.isOverridableBy(candidateDescriptor, overriderDescriptor, null).result
            val result2 = DEFAULT.isOverridableBy(overriderDescriptor, candidateDescriptor, null).result

            return when {
                result1 == OverrideCompatibilityInfo.Result.OVERRIDABLE &&
                        result2 == OverrideCompatibilityInfo.Result.OVERRIDABLE ->
                    OverrideCompatibilityInfo.Result.OVERRIDABLE

                result1 == OverrideCompatibilityInfo.Result.CONFLICT ||
                        result2 == OverrideCompatibilityInfo.Result.CONFLICT ->
                    OverrideCompatibilityInfo.Result.CONFLICT

                else -> OverrideCompatibilityInfo.Result.INCOMPATIBLE
            }
        }

        /**
         * 获取基本覆盖问题
         */
        fun getBasicOverridabilityProblem(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo? {
            // 检查静态覆盖
//            if (superDescriptor.isStatic() != subDescriptor.isStatic()) {
//                return if (subDescriptor.isStatic()) {
//                    OverrideCompatibilityInfo.staticConflict(
//                        CangJieDiagnosisBundle.message("CONFLICTING_STATIC_BY_STATIC_TO_NON_STATIC", subDescriptor.name)
//                    )
//                } else {
//                    OverrideCompatibilityInfo.staticConflict(
//                        CangJieDiagnosisBundle.message("CONFLICTING_STATIC_BY_NON_STATIC_TO_STATIC", subDescriptor.name)
//                    )
//                }
//            }

            // 检查枚举类描述符
//            if (subDescriptor is EnumClassCallableDescriptor || superDescriptor is EnumClassCallableDescriptor) {
//                return OverrideCompatibilityInfo.incompatible("Enum member cannot override non-enum member")
//            }

            // 检查成员种类匹配
            if ((superDescriptor is FunctionDescriptor && subDescriptor !is FunctionDescriptor) ||
                (superDescriptor is VariableDescriptor && subDescriptor !is VariableDescriptor)
            ) {
                return OverrideCompatibilityInfo.incompatible("Member kind mismatch")
            }

            // 确保是可检查的类型
            if (superDescriptor !is FunctionDescriptor && superDescriptor !is VariableDescriptor) {
                error("This type of CallableDescriptor cannot be checked for overridability: $superDescriptor")
            }

            // 检查名称匹配
            if (superDescriptor.name != subDescriptor.name) {
                return OverrideCompatibilityInfo.incompatible("Name mismatch")
            }

            return checkReceiverAndParameterCount(superDescriptor, subDescriptor)
        }

        /**
         * 检查接收者和参数数量
         */
        private fun checkReceiverAndParameterCount(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo? {
            // 检查参数数量匹配
            if (superDescriptor.valueParameters.size != subDescriptor.valueParameters.size) {
                return OverrideCompatibilityInfo.incompatible("Value parameter number mismatch")
            }
            return null
        }

        /**
         * 提取双向可覆盖的成员
         */
        fun <H> extractMembersOverridableInBothWays(
            overrider: H,
            extractFrom: MutableCollection<H>,
            descriptorByHandle: (H) -> CallableDescriptor,
            onConflict: (H) -> Unit
        ): Collection<H> {
            val overridable = mutableListOf<H>()
            overridable.add(overrider)
            val overriderDescriptor = descriptorByHandle(overrider)

            val iterator = extractFrom.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (overrider == candidate) {
                    iterator.remove()
                    continue
                }

                val candidateDescriptor = descriptorByHandle(candidate)
                val finalResult = getBothWaysOverridability(overriderDescriptor, candidateDescriptor)

                when (finalResult) {
                    OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                        overridable.add(candidate)
                        iterator.remove()
                    }

                    OverrideCompatibilityInfo.Result.CONFLICT -> {
                        onConflict(candidate)
                        iterator.remove()
                    }

                    else -> {
                        // INCOMPATIBLE - do nothing
                    }
                }
            }
            return overridable
        }

        /**
         * 编译值参数
         */
        private fun compiledValueParameters(callableDescriptor: CallableDescriptor): List<CangJieType> =
            callableDescriptor.valueParameters.map { it.type }

        /**
         * 检查类型是否等价
         */
        private fun areTypesEquivalent(
            typeInSuper: CangJieType,
            typeInSub: CangJieType,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            val bothErrors = typeInSuper.isError && typeInSub.isError
            return bothErrors || AbstractTypeChecker.equalTypes(
                typeCheckerState,
                typeInSuper.unwrap(),
                typeInSub.unwrap()
            )
        }

        /**
         * 检查类型参数是否等价
         */
        private fun areTypeParametersEquivalent(
            superTypeParameter: TypeParameterDescriptor,
            subTypeParameter: TypeParameterDescriptor,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            val superBounds = superTypeParameter.upperBounds
            val subBounds = subTypeParameter.upperBounds.toMutableList()

            if (superBounds.size != subBounds.size) return false

            for (superBound in superBounds) {
                val iterator = subBounds.listIterator()
                var found = false
                while (iterator.hasNext()) {
                    val subBound = iterator.next()
                    if (areTypesEquivalent(superBound, subBound, typeCheckerState)) {
                        iterator.remove()
                        found = true
                        break
                    }
                }
                if (!found) return false
            }
            return true
        }

        /**
         * 创建并绑定假覆盖
         */
        private fun createAndBindFakeOverrides(
            current: ClassDescriptor,
            notOverridden: Collection<CallableMemberDescriptor>,
            strategy: OverridingStrategy
        ) {
            if (allHasSameContainingDeclaration(notOverridden)) {
                notOverridden.forEach { descriptor ->
                    createAndBindFakeOverride(setOf(descriptor), current, strategy)
                }
                return
            }

            val fromSuperQueue = LinkedList(notOverridden)
            while (fromSuperQueue.isNotEmpty()) {
                val notOverriddenFromSuper = findMemberWithMaxVisibility(fromSuperQueue)
                val overridables = extractMembersOverridableInBothWays(
                    notOverriddenFromSuper,
                    fromSuperQueue
                ) { descriptor ->
                    strategy.inheritanceConflict(notOverriddenFromSuper, descriptor)
                }
                createAndBindFakeOverride(overridables, current, strategy)
            }
        }

        /**
         * 创建并绑定单个假覆盖
         */
        private fun createAndBindFakeOverride(
            overridables: Collection<CallableMemberDescriptor>,
            current: ClassDescriptor,
            strategy: OverridingStrategy
        ) {
            val visibleOverridables = filterVisibleFakeOverrides(current, overridables)
            val allInvisible = visibleOverridables.isEmpty()
            val effectiveOverridden = if (allInvisible) overridables else visibleOverridables

            val modality = determineModalityForFakeOverride(effectiveOverridden, current)
            val visibility =
                if (allInvisible) DescriptorVisibilities.INVISIBLE_FAKE else DescriptorVisibilities.INHERITED

            val mostSpecific = selectMostSpecificMember(effectiveOverridden) { it }
            val fakeOverride = mostSpecific.copy(
                current,
                modality,
                visibility,
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                false
            )

            strategy.setOverriddenDescriptors(fakeOverride, effectiveOverridden)
            require(fakeOverride.overriddenDescriptors.isNotEmpty()) {
                "Overridden descriptors should be set for ${CallableMemberDescriptor.Kind.FAKE_OVERRIDE}"
            }
            strategy.addFakeOverride(fakeOverride)
        }

        /**
         * 确定假覆盖的模态性
         */
        private fun determineModalityForFakeOverride(
            descriptors: Collection<CallableMemberDescriptor>,
            current: ClassDescriptor
        ): Modality {
            // 优化：在常见情况下避免创建哈希集合
            var hasOpen = false
            var hasAbstract = false

            for (descriptor in descriptors) {
                when (descriptor.modality) {
                    Modality.FINAL -> return Modality.FINAL
                    Modality.SEALED -> error("Member cannot have SEALED modality: $descriptor")
                    Modality.OPEN -> hasOpen = true
                    Modality.ABSTRACT -> hasAbstract = true
                }
            }

            val transformAbstractToClassModality =
                    current.modality != Modality.ABSTRACT && current.modality != Modality.SEALED

            return when {
                hasOpen && !hasAbstract -> Modality.OPEN
                !hasOpen && hasAbstract -> if (transformAbstractToClassModality) current.modality else Modality.ABSTRACT
                else -> {
                    val allOverriddenDeclarations = descriptors.flatMap { getOverriddenDeclarations(it) }.toSet()
                    getMinimalModality(
                        filterOutOverridden(allOverriddenDeclarations),
                        transformAbstractToClassModality,
                        current.modality
                    )
                }
            }
        }

        /**
         * 获取最小模态性
         */
        private fun getMinimalModality(
            descriptors: Collection<CallableMemberDescriptor>,
            transformAbstractToClassModality: Boolean,
            classModality: Modality
        ): Modality {
            var result = Modality.ABSTRACT
            for (descriptor in descriptors) {
                val effectiveModality =
                    if (transformAbstractToClassModality && descriptor.modality == Modality.ABSTRACT) {
                        classModality
                    } else {
                        descriptor.modality
                    }
                if (effectiveModality < result) {
                    result = effectiveModality
                }
            }
            return result
        }

        /**
         * 提取双向可覆盖的成员（带处理器）
         */
        private fun extractMembersOverridableInBothWays(
            overrider: CallableMemberDescriptor,
            extractFrom: Queue<CallableMemberDescriptor>,
            onConflict: (CallableMemberDescriptor) -> Unit
        ): Collection<CallableMemberDescriptor> = extractMembersOverridableInBothWays(
            overrider,
            extractFrom,
            { it },
            onConflict
        )
    }

    /**
     * 生成函数组中的覆盖
     */
    fun <T : CallableMemberDescriptor> generateOverridesInFunctionGroup(
        name: Name, // 确保所有描述符有相同名称
        membersFromSupertypes: Collection<T>,
        membersFromCurrent: Collection<T>,
        current: ClassDescriptor,
        strategy: OverridingStrategy
    ) {
        val notOverridden = LinkedHashSet(membersFromSupertypes)



        for (fromCurrent in membersFromCurrent) {
            val bound = extractAndBindOverridesForMember(fromCurrent, membersFromSupertypes, current, strategy)
            notOverridden.removeAll(bound.toSet())
        }

        createAndBindFakeOverrides(current, notOverridden, strategy)
    }

    /**
     * 创建类型检查器状态
     */
    private fun createTypeCheckerState(
        firstParameters: List<TypeParameterDescriptor>,
        secondParameters: List<TypeParameterDescriptor>
    ): TypeCheckerState {
        require(firstParameters.size == secondParameters.size) {
            "Should be the same number of type parameters: $firstParameters vs $secondParameters"
        }

        if (firstParameters.isEmpty()) {
            return OverridingUtilTypeSystemContext(
                null, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
            ).newTypeCheckerState(true, true)
        }

        val matchingTypeConstructors = firstParameters.indices.associate { i ->
            firstParameters[i].typeConstructor to secondParameters[i].typeConstructor
        }

        return OverridingUtilTypeSystemContext(
            matchingTypeConstructors, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
        ).newTypeCheckerState(true, true)
    }

    /**
     * 检查是否可以在不考虑外部条件的情况下覆盖
     */
    fun isOverridableByWithoutExternalConditions(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        checkReturnType: Boolean = false
    ): OverrideCompatibilityInfo {
        // 基本覆盖检查
        getBasicOverridabilityProblem(superDescriptor, subDescriptor)?.let { return it }

        val superValueParameters = compiledValueParameters(superDescriptor)
        val subValueParameters = compiledValueParameters(subDescriptor)
        val superTypeParameters = superDescriptor.typeParameters
        val subTypeParameters = subDescriptor.typeParametersNotExtend

        // 类型参数数量检查
        if (superTypeParameters.size != subTypeParameters.size) {
            for (i in superValueParameters.indices) {
                if (!CangJieTypeChecker.DEFAULT.equalTypes(superValueParameters[i], subValueParameters[i])) {
                    return OverrideCompatibilityInfo.incompatible("Type parameter number mismatch")
                }
            }
            return OverrideCompatibilityInfo.conflict("Type parameter number mismatch")
        }

        val typeCheckerState = createTypeCheckerState(superTypeParameters, subTypeParameters)

        // 检查类型参数等价性
        for (i in superTypeParameters.indices) {
            if (!areTypeParametersEquivalent(
                    superTypeParameters[i],
                    subTypeParameters[i],
                    typeCheckerState
                )
            ) {
                return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch")
            }
        }

        // 检查值参数类型等价性
        for (i in superValueParameters.indices) {
            if (!areTypesEquivalent(
                    superValueParameters[i],
                    subValueParameters[i],
                    typeCheckerState
                )
            ) {
                return OverrideCompatibilityInfo.incompatible("Value parameter type mismatch")
            }
        }

        // 检查返回类型
        if (checkReturnType) {
            val superReturnType = superDescriptor.returnType
            val subReturnType = subDescriptor.returnType

            if (superReturnType != null && subReturnType != null) {
                val bothErrors = subReturnType.isError && superReturnType.isError
                if (!bothErrors && !AbstractTypeChecker.isSubtypeOf(
                        typeCheckerState,
                        subReturnType.unwrap(),
                        superReturnType.unwrap()
                    )
                ) {
                    return OverrideCompatibilityInfo.conflict("Return type mismatch")
                }
            }
        }

        return OverrideCompatibilityInfo.success()
    }

    /**
     * 检查是否可以覆盖（包含外部条件）
     */
    fun isOverridableBy(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?,
        checkReturnType: Boolean = false
    ): OverrideCompatibilityInfo {
        val basicResult = isOverridableByWithoutExternalConditions(superDescriptor, subDescriptor, checkReturnType)
        var wasSuccess = basicResult.result == OverrideCompatibilityInfo.Result.OVERRIDABLE

        // 第一轮：运行非CONFLICTS_ONLY条件
        for (externalCondition in EXTERNAL_CONDITIONS) {
            if (externalCondition.contract == ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) continue
            if (wasSuccess && externalCondition.contract == ExternalOverridabilityCondition.Contract.SUCCESS_ONLY) continue

            when (externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor)) {
                ExternalOverridabilityCondition.Result.OVERRIDABLE -> wasSuccess = true
                ExternalOverridabilityCondition.Result.INCOMPATIBLE ->
                    return OverrideCompatibilityInfo.incompatible("External condition")

                ExternalOverridabilityCondition.Result.UNKNOWN -> {
                    // 继续下一个条件
                }
            }
        }

        if (!wasSuccess) return basicResult

        // 第二轮：运行CONFLICTS_ONLY条件
        for (externalCondition in EXTERNAL_CONDITIONS) {
            if (externalCondition.contract != ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) continue

            when (externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor)) {
                ExternalOverridabilityCondition.Result.INCOMPATIBLE ->
                    return OverrideCompatibilityInfo.incompatible("External condition")

                ExternalOverridabilityCondition.Result.OVERRIDABLE ->
                    error("Contract violation in ${externalCondition::class.java.name} condition. It's not supposed to end with success")

                ExternalOverridabilityCondition.Result.UNKNOWN -> {
                    // 继续下一个条件
                }
            }
        }

        return OverrideCompatibilityInfo.success()
    }

    /**
     * 检查是否可以覆盖（默认不检查返回类型）
     */
    fun isOverridableBy(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?
    ): OverrideCompatibilityInfo = isOverridableBy(superDescriptor, subDescriptor, subClassDescriptor, false)

    /**
     * 提取并绑定成员的覆盖信息
     */
    private fun extractAndBindOverridesForMember(
        fromCurrent: CallableMemberDescriptor,
        descriptorsFromSuper: Collection<CallableMemberDescriptor>,
        current: ClassDescriptor,
        strategy: OverridingStrategy
    ): Collection<CallableMemberDescriptor> {
        val bound = mutableListOf<CallableMemberDescriptor>()
        val overridden = SmartSet.create<CallableMemberDescriptor>()

        for (fromSupertype in descriptorsFromSuper) {
            val resultInfo = isOverridableBy(fromSupertype, fromCurrent, current)
            val result = resultInfo.result
            val isVisibleForOverride = isVisibleForOverride(fromCurrent, fromSupertype, false)

            when (result) {
                OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                    if (isVisibleForOverride) {
                        overridden.add(fromSupertype)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.STATIC_CONFLICT -> {
                    if (isVisibleForOverride) {
                        strategy.staticConflict(fromSupertype, fromCurrent, resultInfo.debugMessage)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.CONFLICT -> {
                    if (isVisibleForOverride) {
                        strategy.overrideConflict(fromSupertype, fromCurrent)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.INCOMPATIBLE -> {
                    // 不进行任何操作
                }
            }
        }

        strategy.setOverriddenDescriptors(fromCurrent, overridden)
        return bound
    }

    /**
     * 覆盖兼容性信息
     */
    data class OverrideCompatibilityInfo(
        val result: Result,
        val debugMessage: String
    ) {
        companion object {
            private val SUCCESS = OverrideCompatibilityInfo(Result.OVERRIDABLE, "SUCCESS")

            fun success(): OverrideCompatibilityInfo = SUCCESS
            fun incompatible(debugMessage: String): OverrideCompatibilityInfo =
                OverrideCompatibilityInfo(Result.INCOMPATIBLE, debugMessage)

            fun staticConflict(debugMessage: String): OverrideCompatibilityInfo =
                OverrideCompatibilityInfo(Result.STATIC_CONFLICT, debugMessage)

            fun conflict(debugMessage: String): OverrideCompatibilityInfo =
                OverrideCompatibilityInfo(Result.CONFLICT, debugMessage)
        }

        override fun toString(): String = "$result: $debugMessage"

        /**
         * 覆盖结果枚举
         */
        enum class Result {
            /** 可以被覆盖 */
            OVERRIDABLE,

            /** 不兼容 */
            INCOMPATIBLE,

            /** 冲突 */
            CONFLICT,

            /** 静态冲突 */
            STATIC_CONFLICT
        }
    }
}