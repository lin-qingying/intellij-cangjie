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

package org.cangnova.cangjie.resolve.calls.components
import com.intellij.util.SmartList
import org.cangnova.cangjie.builtins.UnsignedTypes
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.*
import org.cangnova.cangjie.resolve.calls.inference.components.*
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.*
import org.cangnova.cangjie.resolve.calls.util.getReceiverValueWithSmartCast
import org.cangnova.cangjie.resolve.isInsideInterface
import org.cangnova.cangjie.resolve.isStatic
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.receivers.ClassifierQualifier
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeConstructorMarker
import org.cangnova.cangjie.utils.compactIfPossible
import java.util.concurrent.ConcurrentHashMap

/**
 * 存储显式类类型参数的映射
 *
 * Key: resolvedCall.atom.psiCangJieCall 的 identity hash
 * Value: TypeConstructor 到具体类型的映射
 *
 * 使用 TypeConstructor 而不是 TypeParameterDescriptor 作为内部 Map 的 key，
 * 因为 TypeConstructor 有基于 FqName 的正确 equals() 实现，
 * 而 TypeParameterDescriptor 默认使用身份比较（identity equals）。
 */
private val explicitClassTypeArgumentsCache =
    ConcurrentHashMap<Int, Map<TypeConstructor, CangJieType>>()

/**
 * 创建新类型变量替换器的解析部分
 *
 * 此类负责在类型推导过程中创建新的类型变量,并建立初始约束。
 * 主要用于处理泛型方法调用时的类型参数推导。
 */
internal object CreateFreshVariablesSubstitutor : ResolutionPart() {

    /**
     * 检查类型参数是否应该是弹性类型
     *
     * @param flexibleCheck 用于检查类型是否弹性的函数,默认检查 isFlexible()
     * @return 如果类型参数的任一上界是弹性类型,则返回 true
     */
    fun TypeParameterDescriptor.shouldBeFlexible(flexibleCheck: (CangJieType) -> Boolean = { it.isFlexible() }): Boolean {
        return upperBounds.any {
            flexibleCheck(it) || ((it.constructor.declarationDescriptor as? TypeParameterDescriptor)?.run { shouldBeFlexible() }
                ?: false)
        }
    }

    /**
     * 创建到新类型变量的替换器,并添加初始约束
     *
     * 此方法执行以下操作:
     * 1. 为每个类型参数创建对应的新类型变量
     * 2. 创建从原类型参数到新类型变量的替换器
     * 3. 在约束系统中注册所有新类型变量
     * 4. 为每个类型变量添加其上界约束
     * 5. 特殊处理类型别名构造器的情况
     *
     * @param candidateDescriptor 候选的可调用描述符(函数/构造器等)
     * @param cangjieCall 仓颉调用表达式
     * @param csBuilder 约束系统构建器,用于注册变量和添加约束
     * @param typeParameters 需要处理的类型参数列表,默认为候选描述符的类型参数
     * @return 返回替换器和新类型变量列表的配对
     */
    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        candidateDescriptor: CallableDescriptor,
        cangjieCall: CangJieCall,
        csBuilder: ConstraintSystemOperation,
        typeParameters: List<TypeParameterDescriptor> = candidateDescriptor.typeParameters
    ): Pair<ComposableTypeSubstitutor, List<TypeVariableFromCallableDescriptor>> {

        // 为每个类型参数创建对应的新类型变量
        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        // 创建从原类型参数到新类型变量的替换器
        val toFreshVariables = ComposableTypeSubstitutor.create(
            SubstitutorFunction.fromFreshVariables(freshTypeVariables),
            SubstitutionOptions.INFERENCE
        )

        // 在约束系统中注册所有新创建的类型变量
        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        /**
         * 为类型变量添加子类型约束
         *
         * @param upperBound 上界类型
         * @param position 约束位置信息,用于错误报告
         */
        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
            upperBound: CangJieType,
            position: DeclaredUpperBoundConstraintPositionImpl
        ) {
            csBuilder.addSubtypeConstraint(
                defaultType,
                toFreshVariables.safeSubstitute(upperBound.unwrap()),
                position
            )
        }

        // 为每个类型参数添加其声明的上界约束
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, cangjieCall)

            // 为类型参数的每个上界添加约束
            for (upperBound in typeParameter.upperBounds) {
                freshVariable.addSubtypeConstraint(upperBound, position)
            }
        }

        // 特殊处理类型别名构造器的情况
        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
            // 获取底层类型的类型参数
            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters

            for (index in typeParameters.indices) {
                val typeParameter = typeParameters[index]
                val freshVariable = freshTypeVariables[index]

                // 找到当前类型参数在底层类型中对应的位置
                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, cangjieType: CangJieType ->
                    if (cangjieType == typeParameter.defaultType) i else null
                }

                // 为对应的原始类型参数添加上界约束
                for (originalIndex in typeMapping) {
                    // 可能为 null,因为在内部类的情况下,类型参数可能已经在外部类中被捕获
                    // 参见测试 innerClassTypeAliasConstructor.cj
                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter, cangjieCall)

                    for (upperBound in originalTypeParameter.upperBounds) {
                        freshVariable.addSubtypeConstraint(upperBound, position)
                    }
                }
            }
        }
        return toFreshVariables to freshTypeVariables
    }

    /**
     * 获取保留类型变量弹性的类型
     *
     * 根据类型变量的原始类型参数是否应该是弹性的,来决定是否创建弹性类型。
     * 弹性类型包含一个下界(非可选)和一个上界(可选)。
     *
     * @param type 原始类型
     * @param typeVariable 类型变量
     * @return 可能是弹性类型的类型
     */
    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: CangJieType,
        typeVariable: TypeVariableFromCallableDescriptor
    ): CangJieType {
        // 创建弹性类型:下界为非可选,上界为可选
        fun createFlexibleType() =
            CangJieTypeFactory.flexibleType(
                type.makeNonOption().lowerIfFlexible(),
                type.makeOption().upperIfFlexible()
            )

        return when {
            // 如果类型参数应该是带增强的弹性类型
            typeVariable.originalTypeParameter.shouldBeFlexible { it is FlexibleTypeWithEnhancement } ->
                createFlexibleType().wrapEnhancement(type)

            // 如果类型参数应该是普通弹性类型
            typeVariable.originalTypeParameter.shouldBeFlexible() -> createFlexibleType()

            // 否则保持原类型
            else -> type
        }
    }

    /**
     * 从新类型变量替换器创建已知参数替换器
     *
     * 此方法处理已知类型参数(例如从外部上下文传入的类型参数)的替换。
     * 它会检查每个类型变量对应的原始类型参数,如果在已知替换器中有替换,
     * 则创建一个映射关系。
     *
     * @param freshVariables 新创建的类型变量列表
     * @param knownTypeParametersSubstitutor 已知类型参数的替换器
     * @return 组合后的替换器
     */
    private fun createKnownParametersFromFreshVariablesSubstitutor(
        freshVariables: List<TypeVariableFromCallableDescriptor>,
        knownTypeParametersSubstitutor: ComposableTypeSubstitutor,
    ): ComposableTypeSubstitutor {
        // 如果已知替换器为空,直接返回空替换器
        if (knownTypeParametersSubstitutor.isEmpty)
            return ComposableTypeSubstitutor.EMPTY

        // 构建从类型变量到已知类型参数的映射
        val knownTypeParameterByTypeVariable = mutableMapOf<TypeConstructor, UnwrappedType>().let { map ->
            for (typeVariable in freshVariables) {
                val typeParameterType = typeVariable.originalTypeParameter.defaultType
                val substitutedKnownTypeParameter = knownTypeParametersSubstitutor.safeSubstitute(typeParameterType.unwrap())

                // 如果替换后的类型与原类型不同,说明这是一个已知的类型参数
                if (substitutedKnownTypeParameter !== typeParameterType.unwrap())
                    map[typeVariable.defaultType.constructor] = substitutedKnownTypeParameter
            }
            map
        }

        // 组合已知参数替换器和类型变量映射
        return knownTypeParametersSubstitutor.compose(
            ComposableTypeSubstitutor.create(knownTypeParameterByTypeVariable)
        )
    }

    /**
     * 获取需要进行类型推导的类型参数列表
     *
     * 此方法会检查是否有显式指定的类型参数(如 Option<Int>.None),
     * 并将显式类型参数信息存储到 resolvedCall 中,以便后续添加约束。
     *
     * ## 仓颉语言语义
     *
     * 在仓颉语言中,类不能作为值使用,只能作为类型限定符。
     * 例如 `Option<Int>.None`:
     * - `Option<Int>` 是 ClassifierQualifier(类型限定符)
     * - `None` 是枚举构造器
     * - Int 是显式指定的类型参数,应该添加 EQUALITY 约束
     *
     * ## 关键实现细节
     *
     * **TypeParameterDescriptor 实例一致性**:
     *
     * 必须使用 `candidateDescriptor.containingDeclaration` 的类型参数,
     * 而不是 `ClassifierQualifier.descriptor` 的类型参数。原因是:
     * - 方法的参数类型(如 `b: T`)引用的是 containingDeclaration 的 TypeParameterDescriptor
     * - 类型替换器使用 TypeConstructor 的身份标识(identity)进行查找
     * - 如果使用不同的 TypeParameterDescriptor 实例,替换会失败
     *
     * ## 解决方案
     *
     * 1. 检查显式接收器是否是 ClassifierQualifier
     * 2. 使用 `candidateDescriptor.containingDeclaration.declaredTypeParameters` 作为类类型参数
     * 3. 通过 TypeConstructor 等价性(equals)将 ClassifierQualifier 的显式类型参数映射到正确的实例
     *    - TypeConstructor 有基于 FqName 的 equals() 实现，可正确匹配不同实例的等价类型参数
     *    - 使用 TypeConstructor 而非 TypeParameterDescriptor 作为 Map 的 key
     * 4. 返回所有类型参数(类的 + 方法的)
     * 5. 在 process() 方法中检查并添加 EQUALITY 约束
     *
     * @return 所有需要处理的类型参数列表
     */
    fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
        // 检查显式接收器是否是 ClassifierQualifier（类型限定符）
        val explicitReceiver = resolvedCall.atom.explicitReceiver?.receiver
        if (explicitReceiver is ClassifierQualifier) {
            val qualifierDescriptor = explicitReceiver.descriptor

            // 关键修复：使用候选描述符的 containingDeclaration 的类型参数
            // 这确保了我们使用的 TypeParameterDescriptor 实例与方法参数类型中引用的是同一实例
            //
            // 问题背景：
            // - ClassifierQualifier.descriptor 和 candidateDescriptor.containingDeclaration
            //   可能是同一个类的不同实例（例如一个来自缓存，一个来自新的反序列化）
            // - 这导致 declaredTypeParameters 返回不同的 TypeParameterDescriptor 实例
            // - 类型替换器使用 TypeConstructor 的身份标识（identity）进行查找
            // - 如果实例不同，替换失败，导致 TYPE_MISMATCH 错误显示未替换的类型参数
            val containingDeclaration = candidateDescriptor.containingDeclaration
            val classTypeParameters = if (containingDeclaration is ClassAndEnumDescriptor) {
                containingDeclaration.declaredTypeParameters
            } else {
                qualifierDescriptor.declaredTypeParameters
            }

            // 使用 ClassifierQualifier.getTypeArgumentsForConstraints() 获取显式类型参数
            // 这个方法会过滤掉类型变量，只返回具体类型
            val qualifierTypeParamMap = explicitReceiver.getTypeArgumentsForConstraints()

            // 转换映射：使用 TypeConstructor 作为 key
            // TypeConstructor 有基于 FqName 的 equals() 实现，可以正确匹配不同实例的等价类型参数
            val explicitTypeParamMap = if (qualifierTypeParamMap.isNotEmpty() && containingDeclaration is ClassAndEnumDescriptor) {
                // 遍历 ClassifierQualifier 的类型参数映射，使用 TypeConstructor 等价性查找对应的类型参数
                qualifierTypeParamMap.mapNotNull { (qualifierParam, type) ->
                    // 使用 TypeConstructor.equals() 找到对应的 containingDeclaration 的类型参数
                    classTypeParameters.find { containingParam ->
                        containingParam.typeConstructor == qualifierParam.typeConstructor
                    }?.let { it.typeConstructor to type }
                }.toMap()
            } else {
                // 直接使用 qualifierTypeParamMap，但转换为 TypeConstructor 作为 key
                qualifierTypeParamMap.map { (param, type) -> param.typeConstructor to type }.toMap()
            }

            // 将显式类型参数信息存储到缓存中
            if (explicitTypeParamMap.isNotEmpty()) {
                // 使用 identity hash code 作为 key,确保每个调用点有唯一标识
                val cacheKey = System.identityHashCode(resolvedCall.atom.psiCangJieCall)
                explicitClassTypeArgumentsCache[cacheKey] = explicitTypeParamMap
            }

            // 返回所有类型参数(类的 + 方法的)
            // 重要：必须使用 candidateDescriptor.typeParameters 而不是 .original.typeParameters
            // 因为参数类型（如 b: D）引用的是 candidateDescriptor 的类型参数实例，
            // 而不是 original 的。如果使用不同的实例，替换器的映射将无法匹配，
            // 导致类型参数未被替换。
            return classTypeParameters + candidateDescriptor.typeParameters
        }

        // 默认情况:只返回方法自身的类型参数
        // 同样使用 candidateDescriptor.typeParameters 以保持一致性
        return candidateDescriptor.typeParameters
    }

    /**
     * 处理解析候选
     *
     * 此方法是类型推导的核心处理逻辑,执行以下步骤:
     * 1. 创建新的类型变量和替换器
     * 2. 处理已知类型参数
     * 3. 为显式指定的类型参数添加等式约束
     * 4. 为类类型参数添加特殊约束
     *
     * @param workIndex 工作索引(未使用)
     */
    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()

        // 获取所有需要处理的类型参数(包括类和方法的)
        val typeParameters = getTypeParameters()

        // 计算类类型参数的数量，用于后续区分类类型参数和方法类型参数
        val classTypeParametersCount = run {
            val explicitReceiver = resolvedCall.atom.explicitReceiver?.receiver
            if (explicitReceiver is ClassifierQualifier) {
                val containingDeclaration = candidateDescriptor.containingDeclaration
                if (containingDeclaration is ClassAndEnumDescriptor) {
                    containingDeclaration.declaredTypeParameters.size
                } else {
                    explicitReceiver.descriptor.declaredTypeParameters.size
                }
            } else {
                0
            }
        }

        // 如果没有类型参数,创建空的替换器和变量列表
        // 否则创建新类型变量替换器并添加初始约束
        val (toFreshVariables, freshTypeVariables) =
            if (typeParameters.isEmpty())
                ComposableTypeSubstitutor.EMPTY to emptyList()
            else
                createToFreshVariableSubstitutorAndAddInitialConstraints(
                    candidateDescriptor,
                    resolvedCall.atom,
                    csBuilder,
                    typeParameters
                )

        // 如果存在已知类型参数替换器,创建对应的替换器
        val knownTypeParametersSubstitutor = knownTypeParametersResultingSubstitutor?.let {
            createKnownParametersFromFreshVariablesSubstitutor(freshTypeVariables, it)
        } ?: ComposableTypeSubstitutor.EMPTY

        // 将替换器和变量存储到 resolvedCall 中,供后续使用
        resolvedCall.freshVariablesSubstitutor = toFreshVariables
        resolvedCall.freshVariables = freshTypeVariables
        resolvedCall.knownParametersSubstitutor = knownTypeParametersSubstitutor

        // 如果没有类型参数,直接返回
        if (typeParameters.isEmpty()) {
            return
        }

        // 如果函数声明有问题(约束系统中存在矛盾),直接返回
        if (csBuilder.hasContradiction) return

        // 从缓存中获取显式类类型参数信息(如果有)
        val cacheKey = System.identityHashCode(resolvedCall.atom.psiCangJieCall)
        val explicitClassTypeArguments = explicitClassTypeArgumentsCache[cacheKey]

        // 优化:如果没有显式类型参数、没有已知类型参数、也没有显式类类型参数,直接返回
        if (resolvedCall.typeArgumentMappingByOriginal == TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
            && knownTypeParametersResultingSubstitutor == null
            && explicitClassTypeArguments.isNullOrEmpty()) {
            return
        }

        // 为每个类型参数添加相应的约束
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]

            // 情况1: 检查是否是显式指定的类类型参数
            // 例如: a<Int64>.method() 中的 Int64
            // 使用 TypeConstructor 作为 key 进行查找，因为它有正确的 equals() 实现
            val explicitClassTypeArg: CangJieType? = explicitClassTypeArguments?.get(typeParameter.typeConstructor)
            if (explicitClassTypeArg != null) {
                // 为显式类类型参数添加 EQUALITY 约束
                val typeArgument = object : SimpleTypeArgument {
                    override val type: UnwrappedType = explicitClassTypeArg.unwrap()
                }

                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(explicitClassTypeArg, freshVariable),
                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
                )
                continue
            }

            // 情况2: 检查是否是已知的类型参数
            // 例如从外部上下文传入的类型参数
            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.safeSubstitute(typeParameter.defaultType.unwrap())
            if (knownTypeArgument != null) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument, freshVariable),
                    KnownTypeParameterConstraintPositionImpl(knownTypeArgument)
                )
                continue
            }

            // 情况3: 检查是否是显式的方法类型参数
            // 例如: method<Int64>() 中的 Int64
            //
            // 重要：typeArgumentMappingByOriginal 是用 original 类型参数作为 key 的，
            // 但我们的 typeParameters 列表使用的是 candidateDescriptor.typeParameters（非 original）。
            // 对于方法类型参数，需要使用 original 版本进行查找。
            val lookupTypeParameter = if (index >= classTypeParametersCount) {
                // 方法类型参数：使用 original 版本进行查找
                val methodTypeParamIndex = index - classTypeParametersCount
                candidateDescriptor.original.typeParameters.getOrNull(methodTypeParamIndex) ?: typeParameter
            } else {
                // 类类型参数：直接使用（类类型参数不在 typeArgumentMappingByOriginal 中）
                typeParameter
            }
            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(lookupTypeParameter)

            if (typeArgument is SimpleTypeArgument) {
                // 为显式类型参数添加等式约束
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
                )
            } else {
                // 类型参数占位符,表示需要推导的类型参数
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }
    }
}


