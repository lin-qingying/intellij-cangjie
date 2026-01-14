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

package org.cangnova.cangjie.resolve

import com.intellij.util.SmartList
import org.cangnova.cangjie.builtins.PlatformToCangJieClassMapper
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.composeAnnotations
import org.cangnova.cangjie.descriptors.impl.AbstractVariableDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.NESTING_DOLL_OPTINOTYPE
import org.cangnova.cangjie.diagnostics.infos.warnings.REDUNDANT_OPTIONAL
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.cangnova.cangjie.psi.debugtext.getDebugText
import org.cangnova.cangjie.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.cangnova.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.cangnova.cangjie.resolve.PossiblyBareType.Companion.bare
import org.cangnova.cangjie.resolve.PossiblyBareType.Companion.type
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator.commonSuperType
import org.cangnova.cangjie.resolve.qualified.ExpressionQualifierPart
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolverFacade
import org.cangnova.cangjie.resolve.qualified.TypeQualifierResolutionResult

import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.source.CangJieSourceElement
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.ErrorUtils.invalidType
import org.cangnova.cangjie.types.TypeUtils.addTypeParameterToStub
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext
import org.cangnova.cangjie.types.checker.TrailingCommaChecker
import org.cangnova.cangjie.types.checker.TypeIntersector
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.TypeAttributeTranslators
import kotlin.math.min

/**
 * 类型解析器
 *
 * 负责将 PSI 类型引用转换为语义类型（CangJieType），是仓颉语言类型系统的核心组件。
 *
 * 主要职责：
 * - 解析用户类型（UserType）为类、接口、枚举、结构体等
 * - 解析基本类型（Int、String、Bool 等）
 * - 解析函数类型（`(Int, String) -> Bool`）
 * - 解析元组类型（`(Int, String)`）
 * - 解析可选类型（`Int?`）
 * - 解析数组类型（`Array<T>`）
 * - 解析类型别名及其展开
 * - 处理类型参数和泛型约束
 * - 检查类型上界边界
 * - 处理类型投影和型变
 * - 解析注解和类型属性
 *
 * @param annotationResolver 注解解析器，用于解析类型上的注解
 * @param moduleDescriptor 模块描述符，提供内置类型访问
 * @param identifierChecker 标识符检查器，验证标识符合法性
 * @param languageVersionSettings 语言版本设置
 * @param qualifiedExpressionResolver 限定表达式解析器，处理 `A.B.C` 形式的类型引用
 * @param typeAttributeTranslators 类型属性翻译器，将注解转换为类型属性
 * @param upperBoundChecker 上界检查器，验证类型参数的上界约束
 * @param platformToCangJieClassMapper 平台类映射器，将 Java 类型映射到仓颉类型
 */
class TypeResolver(
    private val annotationResolver: AnnotationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val identifierChecker: IdentifierChecker,

    private val languageVersionSettings: LanguageVersionSettings,
    private val qualifiedExpressionResolver: QualifiedExpressionResolverFacade,
    private val typeAttributeTranslators: TypeAttributeTranslators,
    private val upperBoundChecker: UpperBoundChecker,
    private val platformToCangJieClassMapper: PlatformToCangJieClassMapper,

    ) {
    /**
     * 解析缩写类型
     *
     * 解析类型别名引用为缩写类型（不展开类型别名）。
     * 例如：`typealias StringList = List<String>` 中的 `StringList` 解析为缩写类型。
     *
     * 如果类型别名展开为动态类型，会报告错误并返回错误类型。
     *
     * @param scope 解析作用域
     * @param typeReference 类型引用 PSI 元素
     * @param trace 绑定追踪器
     * @return 解析得到的简单类型（SimpleType）
     * @throws IllegalStateException 如果解析结果既不是 DynamicType 也不是 SimpleType
     */
    fun resolveAbbreviatedType(scope: LexicalScope, typeReference: CjTypeReference, trace: BindingTrace): SimpleType {
        val resolvedType = resolveType(
            TypeResolutionContext(scope, trace, true, false, typeReference.suppressDiagnosticsInDebugMode(), true),
            typeReference
        ).unwrap()
        return when (resolvedType) {
            is DynamicType -> {
                trace.report(TYPEALIAS_SHOULD_EXPAND_TO_CLASS.on(typeReference, resolvedType))
                ErrorUtils.createErrorType(ErrorTypeKind.PROHIBITED_DYNAMIC_TYPE)
            }

            is SimpleType -> resolvedType
            else -> error("Unexpected type: $resolvedType")
        }
    }

    /**
     * 解析类分类器
     *
     * 从用户类型引用中解析类分类器（ClassifierDescriptor）。
     * 这是一个便捷方法，只返回分类器而不返回完整的类型。
     *
     * @param scope 解析作用域
     * @param userType 用户类型 PSI 元素
     * @param trace 绑定追踪器
     * @param isDebuggerContext 是否在调试器上下文中（调试器上下文会有不同的解析规则）
     * @return 解析得到的分类器描述符，如果解析失败则返回 null
     */
    fun resolveClass(
        scope: LexicalScope, userType: CjUserType, trace: BindingTrace, isDebuggerContext: Boolean
    ): ClassifierDescriptor? = resolveDescriptorForType(scope, userType, trace, isDebuggerContext).classifierDescriptor

    /**
     * 解析类型别名的展开类型
     *
     * 将类型别名完全展开为其底层类型。
     * 例如：`typealias StringList = List<String>` 展开为 `List<String>`
     *
     * 使用非报告的类型别名展开器（NON_REPORTING），不会生成诊断信息。
     *
     * @param typeAliasDescriptor 类型别名描述符
     * @return 展开后的简单类型
     */
    fun resolveExpandedTypeForTypeAlias(typeAliasDescriptor: TypeAliasDescriptor): SimpleType {
        val typeAliasExpansion = TypeAliasExpansion.createWithFormalArguments(typeAliasDescriptor)
        val expandedType =
            TypeAliasExpander.NON_REPORTING.expandWithoutAbbreviation(typeAliasExpansion, TypeAttributes.Empty)
        return expandedType
    }


    /**
     * 检查函数类型上的非括号注解
     *
     * 检查函数类型的注解是否正确使用括号。
     * 对于函数类型（例如 `(Int) -> String`），注解必须使用括号或方括号分组，
     * 否则可能与函数类型的参数列表产生歧义。
     *
     * 例如：
     * - `@Anno(Int) -> String` - 错误：需要括号
     * - `@Anno() (Int) -> String` - 正确：注解有括号
     * - `[@Anno] (Int) -> String` - 正确：注解用方括号分组
     *
     * @param typeElement 函数类型 PSI 元素
     * @param annotationEntries 注解条目列表
     * @param trace 绑定追踪器，用于报告错误
     */
    private fun checkNonParenthesizedAnnotationsOnFunctionalType(
        typeElement: CjFunctionType,
        annotationEntries: List<CjAnnotation>,
        trace: BindingTrace
    ) {
        val lastAnnotationEntry = annotationEntries.lastOrNull()
        val isAnnotationsGroupedUsingBrackets =
            lastAnnotationEntry?.getNextSiblingIgnoringWhitespaceAndComments()?.node?.elementType == CjTokens.RBRACKET
        val hasAnnotationParentheses = lastAnnotationEntry?.valueArgumentList != null
        val isFunctionalTypeStartingWithParentheses = typeElement.firstChild is CjParameterList
        val hasSuspendModifierBeforeParentheses =
            typeElement.getPrevSiblingIgnoringWhitespaceAndComments()
                .run { this is CjDeclarationModifierList /*&& hasSuspendModifier() */ }

        if (lastAnnotationEntry != null &&
            isFunctionalTypeStartingWithParentheses &&
            !hasAnnotationParentheses &&
            !isAnnotationsGroupedUsingBrackets &&
            !hasSuspendModifierBeforeParentheses
        ) {
            trace.report(NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES.on(lastAnnotationEntry))
        }
    }

    /**
     * 解析类型注解
     *
     * 解析类型引用上的注解。
     * 目前返回空注解集合（EMPTY），可能在未来版本中实现完整的类型注解解析。
     *
     * @param trace 绑定追踪器
     * @param scope 解析作用域
     * @param modifierListsOwner 修饰符列表拥有者（通常是类型引用）
     * @return 解析得到的注解集合，当前总是返回 Annotations.EMPTY
     */
    fun resolveTypeAnnotations(
        trace: BindingTrace,
        scope: LexicalScope,
        modifierListsOwner: CjElementImplStub<*>
    ): Annotations {

        val result = Annotations.EMPTY

        return result
    }

    /**
     * 强制解析类型内容（轻量版）
     *
     * 这是 ForceResolveUtil.forceResolveAllContents 的轻量版本。
     * 递归地解析类型参数中的所有类型，但不会解析类型构造器本身。
     *
     * 这样做是为了避免循环依赖问题。例如：
     * ```
     * class A: List<A.B> {
     *   class B
     * }
     * ```
     * 在解析 B 类时，需要解析 A 的超类型，但不应该触发 B 类的解析，
     * 否则会形成循环。成员作用域能够延迟获取 B 类的描述符而不触发解析。
     *
     * @param type 要强制解析的类型
     */
    private fun forceResolveTypeContents(type: CangJieType) {


        for (projection in type.arguments) {

            forceResolveTypeContents(projection.type)

        }

    }

    /**
     * 解析类型的描述符
     *
     * 从用户类型引用中解析分类器描述符。
     * 该方法会先解析限定符的类型参数，然后委托给 qualifiedExpressionResolver 进行实际解析。
     *
     * 处理限定类型引用（例如 `A.B<Int>.C<String>`）时：
     * 1. 首先强制解析限定符部分的类型参数
     * 2. 然后解析整个限定表达式
     *
     * @param scope 解析作用域
     * @param userType 用户类型 PSI 元素
     * @param trace 绑定追踪器
     * @param isDebuggerContext 是否在调试器上下文中
     * @return 类型限定符解析结果，包含分类器描述符和限定符部分列表
     */
    fun resolveDescriptorForType(
        scope: LexicalScope,
        userType: CjUserType,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        if (userType.qualifier != null) { // 必须解析限定符类型参数中的所有类型引用
            for (typeArgument in userType.qualifier!!.typeArguments) {
                typeArgument.typeReference?.let {
                    // 在限定表达中，类型参数只能在不正确的代码中有界限
                    forceResolveTypeContents(resolveType(scope, it, trace, false))
                }
            }
        }

        return qualifiedExpressionResolver.resolveDescriptorForType(userType, scope, trace, isDebuggerContext).apply {

        }
    }


    /**
     * 解析类型参数列表
     *
     * 解析类型参数列表中的所有类型参数（type arguments）。
     * 对每个类型参数执行以下操作：
     * 1. 检查修饰符是否合法（通过 ModifierCheckerCore）
     * 2. 解析类型引用为具体类型
     * 3. 创建类型参数（TypeArgument）
     *
     * 仓颉语言中所有类型参数都是不变的（invariant）。
     *
     * @param c 类型解析上下文
     * @param constructor 类型构造器，用于参数数量检查
     * @param argumentElements 类型参数 PSI 元素列表
     * @return 解析得到的类型参数列表
     */
    fun resolveTypeProjections(
        c: TypeResolutionContext,
        constructor: TypeConstructor,
        argumentElements: List<CjTypeProjection>
    ): List<TypeArgument> {
        return argumentElements.mapIndexed { i, argumentElement ->

            ModifierCheckerCore.check(argumentElement, c.trace, null, languageVersionSettings)

            val type = resolveType(c.noBareTypes(), argumentElement.typeReference!!)


            TypeArgumentImpl(type)


        }

    }

    fun String.toName(): Name {
        return Name.identifier(this)
    }

    /**
     * 解析类型元素
     *
     * 这是类型解析的核心方法，负责将各种类型的 PSI 元素转换为语义类型。
     * 使用访问者模式遍历类型元素并生成对应的类型。
     *
     * 支持的类型元素：
     * - **ThisType** (`This`): 当前类类型，用于成员作用域
     * - **VArrayType** (`Array<T, N>`): 固定大小的数组类型
     * - **BasicType** (`Int`, `String`, etc.): 基本类型
     * - **UserType** (`MyClass<T>`): 用户定义的类型
     * - **ParenthesizedType** (`(T)`): 括号类型
     * - **FunctionType** (`(A, B) -> C`): 函数类型
     * - **TupleType** (`(A, B, C)`): 元组类型
     * - **OptionType** (`T?`): 可选类型
     *
     * @param c 类型解析上下文
     * @param annotations 类型上的注解
     * @param outerModifierList 外部修饰符列表（用于嵌套类型）
     * @param typeElement 要解析的类型元素 PSI
     * @return 可能为 Bare 类型的解析结果
     */
    private fun resolveTypeElement(
        c: TypeResolutionContext,
        annotations: Annotations,
        outerModifierList: CjModifierList?,
        typeElement: CjTypeElement?,

        ): PossiblyBareType {


        var result: PossiblyBareType? = null


        typeElement?.accept(object : CjVisitorUnit() {
            fun checkThisTypeForClass(): CangJieType {
                val parent: LexicalScope = c.scope.parent as? LexicalScope ?: return invalidType

                if (parent.kind == LexicalScopeKind.CLASS_MEMBER_SCOPE) {

                    return ThisType((parent.ownerDescriptor as ClassDescriptor).defaultType)


                }
                c.trace.report(
                    INVALID_THIS_TYPE.on(
                        typeElement as CjThisType,
                    )
                )
                return invalidType
            }

            override fun visitThisType(type: CjThisType) {
                checkThisTypeForClass().let {
                    result = type(it)
                }
            }

            fun resolveVArrayType(type: CjVArrayType): CangJieType {
                val argumentType = resolveTypeElement(
                    c,
                    annotations,
                    outerModifierList,
                    type.typeElement
                ).actualType

                val size = type.literal?.text?.toInt() ?: -1

                return createVArrayType(moduleDescriptor.builtIns, argumentType, size)

            }

            override fun visitVArrayType(type: CjVArrayType) {
                result = type(resolveVArrayType(type))
            }


            override fun visitBasicType(type: CjBasicType) {
                val typeName = type.name
                val basicScope = moduleDescriptor.builtIns.BASIC_SCOPE

                // 通过 BASIC_SCOPE 查找基本类型的分类器
                val classifier = basicScope.getContributedClassifier(
                    Name.identifier(typeName),
                    NoLookupLocation.FROM_BUILTINS
                )

                if (classifier == null || classifier !is ClassDescriptor) {
                    // 未找到基本类型，返回错误类型
                    result = type(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, typeName))
                    return
                }

                // 获取基本类型，并应用 annotations 和 type attributes
                val resultingType = CangJieTypeFactory.simpleType(
                    typeAttributeTranslators.toAttributes(
                        annotations,
                        classifier.typeConstructor,
                        c.scope.ownerDescriptor
                    ),
                    classifier,
                    emptyList() // 基本类型没有类型参数
                )

                result = type(resultingType)
            }

            override fun visitUserType(type: CjUserType) {
                val qualifierResolutionResult = resolveDescriptorForType(c.scope, type, c.trace, c.isDebuggerContext)
                val classifier = qualifierResolutionResult.classifierDescriptor


                if (classifier == null) {
                    val arguments = resolveTypeProjections(
                        c,
                        ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, typeElement.text).constructor,
                        qualifierResolutionResult.allProjections
                    )
                    val unresolvedType = ErrorUtils.createErrorTypeWithArguments(
                        ErrorTypeKind.UNRESOLVED_TYPE,
                        arguments,
                        type.getDebugText()
                    )
                    result = type(unresolvedType)
                    return
                }


                val referenceExpression = type.referenceExpression ?: return

                c.trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifier)

                result = resolveTypeForClassifier(c, classifier, qualifierResolutionResult, type, annotations)
            }

            override fun visitParenthesizedType(parenthesizedType: CjParenthesizedType) {
                result = resolveTypeElement(c, Annotations.EMPTY, null, parenthesizedType.getType())
            }

            private fun resolveParametersOfFunctionType(parameters: List<CjParameter>): List<VariableDescriptor> {

                class ParameterOfFunctionTypeDescriptor(
                    containingDeclaration: DeclarationDescriptor,
                    annotations: Annotations,
                    name: Name,
                    type: CangJieType,
                    source: SourceElement
                ) : AbstractVariableDescriptor(containingDeclaration, annotations, name, type, source) {

                    override var visibility: DescriptorVisibility = DescriptorVisibilities.LOCAL
                    override fun substitute(substitutor: ComposableTypeSubstitutor): CallableDescriptor {
                        throw UnsupportedOperationException("Should not be called for descriptor of type ${this::class.java}")
                    }

                    override val isVar: Boolean = false


                    override val overriddenDescriptors: Collection<CallableDescriptor>
                        get() = emptyList()

                    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
                        return visitor.visitVariableDescriptorBase(this, data)
                    }

                    override fun getCompileTimeInitializer() = null

                    override fun cleanCompileTimeInitializerCache() {}


                }

                parameters.forEach {
                    identifierChecker.checkDeclaration(it, c.trace)
                    checkParameterInFunctionType(it)
                }
                return parameters.map { parameter ->
                    val parameterType = resolveType(c.noBareTypes(), parameter.typeReference!!)
                    val descriptor = ParameterOfFunctionTypeDescriptor(
                        c.scope.ownerDescriptor,
                        annotationResolver.resolveAnnotationsWithoutArguments(c.scope, parameter.annotations, c.trace),
                        parameter.nameAsSafeName,
                        parameterType,
                        parameter.toSourceElement()
                    )
                    c.trace.record(BindingContext.VALUE_PARAMETER, parameter, descriptor)
                    descriptor
                }
            }

            private fun checkParameterInFunctionType(param: CjParameter) {
                if (param.hasDefaultValue()) {
                    c.trace.report(
                        UNSUPPORTED.on(
                            param.defaultValue ?: return,
                            "default value of parameter in function type"
                        )
                    )
                }

                if (param.name != null) {
                    for (annotationEntry in param.annotationEntries) {
                        c.trace.report(
                            UNSUPPORTED.on(
                                annotationEntry,
                                "annotation on parameter in function type"
                            )
                        )
                    }
                }

                val modifierList = param.modifierList
                if (modifierList != null) {
                    CjTokens.MODIFIER_KEYWORDS_ARRAY
                        .mapNotNull { modifierList.getModifier(it) }
                        .forEach {
                            c.trace.report(UNSUPPORTED.on(it, "modifier on parameter in function type"))
                        }
                }

                param.letOrVarKeyword?.let {
                    c.trace.report(UNSUPPORTED.on(it, "let or var on parameter in function type"))
                }
            }

            private fun checkParametersOfFunctionType(parameterDescriptors: List<VariableDescriptor>) {
                val parametersByName = parameterDescriptors.filter { !it.name.isSpecial }.groupBy { it.name }
                for (parametersGroup in parametersByName.values) {
                    if (parametersGroup.size < 2) continue
                    for (parameter in parametersGroup) {
                        val cjParameter = (parameter.source.getPsi() as? CjParameter) ?: continue
                        c.trace.report(DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE.on(cjParameter))
                    }
                }
            }


            private fun resoleTypeFoTupleType(types: List<CjTypeReference>): List<CangJieType> {
                return types.map { resolveType(c.noBareTypes(), it) }
            }

            override fun visitTupleType(type: CjTupleType) {

                val parameterTypes: List<CangJieType> = resoleTypeFoTupleType(type.typeArgumentsAsTypes)
                result = type(
                    createTupleType(
                        moduleDescriptor.builtIns, annotations,
                        parameterTypes

                    )
                )


            }


            override fun visitFunctionType(type: CjFunctionType) {
                val receiverTypeRef = type.receiverTypeReference
                val receiverType = if (receiverTypeRef == null) null else resolveType(c.noBareTypes(), receiverTypeRef)


                val parameterDescriptors = resolveParametersOfFunctionType(type.parameters)
                checkParametersOfFunctionType(parameterDescriptors)

                val returnTypeRef = type.returnTypeReference
                val returnType = if (returnTypeRef != null) resolveType(c.noBareTypes(), returnTypeRef)
                else moduleDescriptor.builtIns.unitType

                val parameterList = type.parameterList
                if (parameterList?.stub == null) {
                    TrailingCommaChecker.check(parameterList?.trailingComma, c.trace, languageVersionSettings)
                }

                result = type(
                    createFunctionType(
                        moduleDescriptor.builtIns, annotations, receiverType,
                        parameterDescriptors.map { it.type },
                        parameterDescriptors.map { it.name },
                        returnType,

                        )
                )
            }

            override fun visitOptionType(optionType: CjOptionType) {
                val innerType = optionType.getInnerType()

                val baseType = createTypeFromInner(optionType, optionType.getModifierList(), innerType)

                // 仓颉语言：移除 DefinitelyNonOptionType 检查
                // 在仓颉中，Option 是确切的类型，不需要"明确非 Option"的概念

                if (baseType.isOptional() || innerType is CjOptionType/* || innerType is CjDynamicType*/) {
                    c.trace.report(REDUNDANT_OPTIONAL.on(optionType))

                    c.trace.report(NESTING_DOLL_OPTINOTYPE.on(optionType))
                }

                result = type(
                    OptionTypeUtils.createOptionType(
                        addTypeParameterToStub(
                            resolveOptionType(),
                            baseType.actualType
                        ) as SimpleType,
                        moduleDescriptor.builtIns
                    )
                )
            }

            fun resolveOptionType(): CangJieType {
                return moduleDescriptor.builtIns.stdlibTypes.option.defaultType
            }


            private fun createTypeFromInner(
                typeElement: CjTypeElement,
                innerModifierList: CjModifierList?,
                innerType: CjTypeElement?
            ): PossiblyBareType {
                if (innerModifierList != null && outerModifierList != null) {
                    c.trace.report(MODIFIER_LIST_NOT_ALLOWED.on(innerModifierList))
                }

                val innerAnnotations = composeAnnotations(
                    annotations,
                    resolveTypeAnnotations(c.trace, c.scope, typeElement as CjElementImplStub<*>)
                )

                return resolveTypeElement(c, innerAnnotations, outerModifierList ?: innerModifierList, innerType)
            }

            override fun visitCjElement(element: CjElement) {
                c.trace.report(UNSUPPORTED.on(element, "Self-types are not supported yet"))
            }
        })

        return result ?: type(
            ErrorUtils.createErrorType(
                ErrorTypeKind.NO_TYPE_SPECIFIED,
                typeElement?.getDebugText() ?: "unknown element"
            )
        )

    }


    private fun resolveTypeProjectionsWithErrorConstructor(
        c: TypeResolutionContext,
        argumentElements: List<CjTypeProjection>,
        message: String = "Error type for resolving type projections"
    ) = resolveTypeProjections(
        c,
        ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR, message),
        argumentElements
    )

    private fun createErrorTypeForTypeConstructor(
        c: TypeResolutionContext,
        arguments: List<CjTypeProjection>,
        typeConstructor: TypeConstructor
    ): PossiblyBareType =
        type(
            ErrorUtils.createErrorTypeWithArguments(
                ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR,
                resolveTypeProjectionsWithErrorConstructor(c, arguments),
                typeConstructor.declarationDescriptor?.name?.asString() ?: typeConstructor.toString()
            )
        )

    // Returns true in case when at least one argument for this class could be specified
    // It could be always equal to 'typeConstructor.parameters.isNotEmpty()' unless local classes could captured type parameters
    // from enclosing functions. In such cases you can not specify any argument:
    // fun <E> foo(x: Any?) {
    //    class C
    //    if (x is C) { // 'C' should not be treated as bare type here
    //       ...
    //    }
    // }
    //
    // It's needed to determine whether this particular type could be bare
    private fun isPossibleToSpecifyTypeArgumentsFor(classifierDescriptor: ClassifierDescriptorWithTypeParameters): Boolean {
        // First parameter relates to the innermost declaration
        // If it's declared in function there
        val firstTypeParameter = classifierDescriptor.typeConstructor.parameters.firstOrNull() ?: return false
        return firstTypeParameter.original.containingDeclaration is ClassifierDescriptorWithTypeParameters
    }

    /**
     * 判断类型别名是否可以用作 Bare 类型
     *
     * 类型别名可以用作 Bare 类型（在 is/as 表达式之后，例如 `x is List`），
     * 当且仅当对应展开类型的所有类型参数满足以下条件：
     * - 是星投影（star projections）
     * - 或者是该类型别名的类型参数且为不变投影（invariant argument）
     * - 且每个类型参数最多只被使用一次
     *
     * 示例：
     * ```
     * typealias HashMap<K, V> = java.util.HashMap<K, V>    // 可以用作 Bare 类型
     * typealias MyList<T, X> = List<X>                     // 可以用作 Bare 类型
     * typealias StarMap<T> = Map<T, *>                     // 可以用作 Bare 类型
     * typealias MyMap<T> = Map<T, T>                       // 不能用作 Bare 类型：类型参数 'T' 被使用了两次
     * typealias StringMap<T> = Map<String, T>              // 不能用作 Bare 类型：类型参数 'String' 不是类型参数
     * ```
     *
     * @param descriptor 类型别名描述符
     * @return 如果可以用作 Bare 类型则返回 true，否则返回 false
     */
    private fun canBeUsedAsBareType(descriptor: TypeAliasDescriptor): Boolean {
        val expandedType = descriptor.expandedType
        if (expandedType.isError) return false

        val classDescriptor = descriptor.classDescriptor ?: return false
        if (!isPossibleToSpecifyTypeArgumentsFor(classDescriptor)) return false

        val usedTypeParameters = linkedSetOf<TypeParameterDescriptor>()
        for (argument in expandedType.arguments) {


//            if (argument.projectionKind != INVARIANT) return false

            val argumentTypeDescriptor =
                argument.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
            if (argumentTypeDescriptor.containingDeclaration != descriptor) return false
            if (usedTypeParameters.contains(argumentTypeDescriptor)) return false

            usedTypeParameters.add(argumentTypeDescriptor)
        }

        return true
    }

    private fun ClassifierDescriptor?.classifierDescriptorsFromInnerToOuter(): List<ClassifierDescriptorWithTypeParameters> =
        generateSequence(
            { this as? ClassifierDescriptorWithTypeParameters },
            { it.containingDeclaration as? ClassifierDescriptorWithTypeParameters }
        ).toList()

    /**
     * @return 未解决的 CjTypeProjection 参数和与外部类相关的已解决参数
     * @return 如果发生错误则返回 null
     *
     * 如果第二个组件为 null，则其余参数应使用相关参数的默认类型进行补充
     */

    private fun collectArgumentsForClassifierTypeConstructor(
        c: TypeResolutionContext,
        classifierDescriptor: ClassifierDescriptorWithTypeParameters,
        qualifierParts: List<ExpressionQualifierPart>
    ): Pair<List<CjTypeProjection>, List<TypeArgument>?>? {
        val classifierDescriptorChain = classifierDescriptor.classifierDescriptorsFromInnerToOuter()
        val reversedQualifierParts = qualifierParts.asReversed()

        val wasStatic = false
        val result = SmartList<CjTypeProjection>()

        val classifierChainLastIndex = min(classifierDescriptorChain.size, reversedQualifierParts.size) - 1

        for (index in 0..classifierChainLastIndex) {
            val qualifierPart = reversedQualifierParts[index]
            val currentArguments = qualifierPart.typeArguments?.arguments.orEmpty()
            val declaredTypeParameters = classifierDescriptorChain[index].declaredTypeParameters
            val currentParameters = if (wasStatic) emptyList() else declaredTypeParameters

            if (wasStatic && currentArguments.isNotEmpty() && declaredTypeParameters.isNotEmpty()) {
                c.trace.report(
                    TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED.on(
                        qualifierPart.typeArguments ?: return null
                    )
                )
                return null
            }

            if (currentArguments.size != currentParameters.size) {
                c.trace.report(
                    WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                        qualifierPart.typeArguments ?: qualifierPart.expression,
                        currentParameters.size, classifierDescriptorChain[index]
                    )
                )
                return null
            }

            result.addAll(currentArguments)

//            wasStatic = wasStatic || !classifierDescriptorChain[index].isInner
        }

        val nonClassQualifierParts =
            reversedQualifierParts.subList(
                min(classifierChainLastIndex + 1, reversedQualifierParts.size),
                reversedQualifierParts.size
            )

        for ((_, _, typeArguments) in nonClassQualifierParts) {
            if (typeArguments != null) {
                c.trace.report(TYPE_ARGUMENTS_NOT_ALLOWED.on(typeArguments, "here"))
                return null
            }
        }

//        val parameters = classifierDescriptor.typeConstructor.parameters
//        if (result.size < parameters.size) {
//            val nextParameterOwner =
//                parameters[result.size].original.containingDeclaration as? ClassDescriptor
//                // If next parameter is captured from the enclosing function, default arguments must be used
//                // (see appendDefaultArgumentsForLocalClassifier)
//                    ?: return Pair(result, null)
////
//            val restArguments = c.scope.findImplicitOuterClassArguments(nextParameterOwner)
//            val restParameters = parameters.subList(result.size, parameters.size)
//
//            val typeArgumentsCanBeSpecifiedCount =
//                classifierDescriptor.classifierDescriptorsFromInnerToOuter().sumOf { it.declaredTypeParameters.size }
//
//            if (restArguments == null && typeArgumentsCanBeSpecifiedCount > result.size) {
//                c.trace.report(
//                    OUTER_CLASS_ARGUMENTS_REQUIRED.on(qualifierParts.first().expression, nextParameterOwner)
//                )
//                return null
//            } else if (restArguments == null) {
//                assert(typeArgumentsCanBeSpecifiedCount == result.size) {
//                    "Number of type arguments that can be specified ($typeArgumentsCanBeSpecifiedCount) " +
//                            "should be equal to actual arguments number ${result.size}, (classifier: $classifierDescriptor)"
//                }
//                return Pair(result, null)
//            } else {
//                assert(restParameters.size == restArguments.size) {
//                    "Number of type of restParameters should be equal to ${restParameters.size}, " +
//                            "but ${restArguments.size} were found for $classifierDescriptor/$nextParameterOwner"
//                }
//
//                return Pair(result, restArguments)
//            }
//        }

        return Pair(result, null)
    }


    private class TracingTypeAliasExpansionReportStrategy(
        val trace: BindingTrace,
        val type: CjElement?,
        val typeArgumentsOrTypeName: CjElement?,
        val typeAliasDescriptor: TypeAliasDescriptor,
        typeParameters: List<TypeParameterDescriptor>,
        typeArguments: List<CjTypeProjection>,
        val upperBoundChecker: UpperBoundChecker
    ) : TypeAliasExpansionReportStrategy {

        private val mappedArguments = typeParameters.zip(typeArguments).toMap()

        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {
            if (typeArgumentsOrTypeName != null) {
                trace.report(
                    WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                        typeArgumentsOrTypeName,
                        numberOfParameters,
                        typeAliasDescriptor
                    )
                )
            }
        }

        override fun conflictingProjection(
            typeAlias: TypeAliasDescriptor,
            typeParameter: TypeParameterDescriptor?,
            substitutedArgument: CangJieType
        ) {
            val argumentElement = typeParameter?.let { mappedArguments[it] }
            if (argumentElement != null) {
                trace.report(CONFLICTING_PROJECTION.on(argumentElement, typeParameter))
            } else if (type != null) {
                trace.report(CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION.on(type, typeAliasDescriptor.underlyingType))
            }
        }

        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {
            if (type != null) {
                trace.report(RECURSIVE_TYPEALIAS_EXPANSION.on(type, typeAlias))
            }
        }

        override fun boundsViolationInSubstitution(
            substitutor: ComposableTypeSubstitutor,
            unsubstitutedArgument: CangJieType,
            argument: CangJieType,
            typeParameter: TypeParameterDescriptor
        ) {
            val descriptorForUnsubstitutedArgument = unsubstitutedArgument.constructor.declarationDescriptor
            val argumentElement = mappedArguments[descriptorForUnsubstitutedArgument]
            val argumentTypeReferenceElement = argumentElement?.typeReference
            upperBoundChecker.checkBounds(
                argumentTypeReferenceElement,
                argument,
                typeParameter,
                substitutor,
                trace,
                type
            )
        }

        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {
            val annotationEntry = (annotation.source as? CangJieSourceElement)?.psi as? CjAnnotation ?: return
            trace.report(REPEATED_ANNOTATION.on(annotationEntry))
        }
    }

    /**
     * 解析类型别名的类型
     *
     * 将类型别名引用解析为完整的类型（可能为 Bare 类型或展开类型）。
     * 该方法负责处理类型别名的完整解析流程，包括：
     * 1. 检查类型别名描述符是否有效
     * 2. 处理 Bare 类型情况（用于 is/as 表达式）
     * 3. 收集和解析类型参数
     * 4. 验证类型参数数量
     * 5. 创建类型别名展开或缩写类型
     *
     * 解析流程：
     * - 如果是错误类型，返回错误类型
     * - 如果允许 Bare 类型且没有类型参数，尝试返回 Bare 类型
     * - 收集限定符部分的类型参数
     * - 解析类型投影
     * - 验证参数数量
     * - 根据上下文返回缩写类型或展开类型
     *
     * 示例：
     * ```
     * typealias StringList = List<String>
     *
     * val x: StringList         // 展开为 List<String>
     * if (y is StringList)      // Bare 类型用法
     * ```
     *
     * @param c 类型解析上下文
     * @param annotations 类型上的注解
     * @param descriptor 类型别名描述符
     * @param type PSI 元素（用于错误报告）
     * @param qualifierResolutionResult 限定符解析结果，包含类型参数信息
     * @return 可能为 Bare 类型的解析结果
     */
    private fun resolveTypeForTypeAlias(
        c: TypeResolutionContext,
        annotations: Annotations,
        descriptor: TypeAliasDescriptor,
        type: CjElement,
        qualifierResolutionResult: TypeQualifierResolutionResult
    ): PossiblyBareType {
        val typeConstructor = descriptor.typeConstructor
        val projectionFromAllQualifierParts = qualifierResolutionResult.allProjections

        if (ErrorUtils.isError(descriptor)) {
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }


        val parameters = typeConstructor.parameters

        if (c.allowBareTypes && projectionFromAllQualifierParts.isEmpty() && isPossibleToSpecifyTypeArgumentsFor(
                descriptor
            )
        ) {
            val classDescriptor = descriptor.classDescriptor
            if (classDescriptor != null && canBeUsedAsBareType(descriptor)) {
                return bare(
                    descriptor.classDescriptor!!.typeConstructor,
                    TypeUtils.isOptionType(descriptor.expandedType)
                )
            }
        }

        val typeAliasQualifierPart =
            qualifierResolutionResult.qualifierParts.lastOrNull()
                ?: return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)

        val (argumentElementsFromUserType, argumentsForOuterClass) =
            collectArgumentsForClassifierTypeConstructor(c, descriptor, qualifierResolutionResult.qualifierParts)
                ?: return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)

        val argumentsFromUserType = resolveTypeProjections(c, typeConstructor, argumentElementsFromUserType)

        val arguments = buildFinalArgumentList(argumentsFromUserType, argumentsForOuterClass, parameters)

        val reportStrategy = TracingTypeAliasExpansionReportStrategy(
            c.trace,
            type, typeAliasQualifierPart.typeArguments ?: typeAliasQualifierPart.expression,
            descriptor, descriptor.declaredTypeParameters,
            argumentElementsFromUserType, // TODO arguments from inner scope
            upperBoundChecker
        )

        if (parameters.size != arguments.size) {
            reportStrategy.wrongNumberOfTypeArguments(descriptor, parameters.size)
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }

        val attributes =
            typeAttributeTranslators.toAttributes(annotations, descriptor.typeConstructor, c.scope.ownerDescriptor)

        return if (c.abbreviated) {
            val abbreviatedType = CangJieTypeFactory.simpleType(
                attributes,
                descriptor.typeConstructor,
                arguments,

            )
            type(abbreviatedType)
        } else {
            val typeAliasExpansion = TypeAliasExpansion.create(null, descriptor, arguments)
            val expandedType = TypeAliasExpander(reportStrategy, c.checkBounds).expand(typeAliasExpansion, attributes)
            type(expandedType)
        }
    }

    fun resolveTypeForClassifier(
        c: TypeResolutionContext,
        descriptor: ClassifierDescriptor,
        qualifierResolutionResult: TypeQualifierResolutionResult,
        element: CjElement,
        annotations: Annotations
    ): PossiblyBareType {
        val qualifierParts = qualifierResolutionResult.qualifierParts

        if (element is CjUserType && element.stub == null) {
            TrailingCommaChecker.check(element.typeArgumentList?.trailingComma, c.trace, languageVersionSettings)
        }

        return when (descriptor) {
            is TypeParameterDescriptor -> {
                assert(qualifierParts.size == 1) {
                    "Type parameter can be resolved only by it's short name, but '${element.text}' is contradiction " +
                            "with ${qualifierParts.size} qualifier parts"
                }

                val qualifierPart = qualifierParts.single()
                type(
                    resolveTypeForTypeParameter(
                        c,
                        annotations,
                        descriptor,
                        qualifierPart.expression,
                        qualifierPart.typeArguments
                    )
                )
            }

            is EnumDescriptor -> resolveTypeForEnum(c, annotations, descriptor, element, qualifierResolutionResult)

            is ClassDescriptor -> resolveTypeForClass(c, annotations, descriptor, element, qualifierResolutionResult)
            is TypeAliasDescriptor -> resolveTypeForTypeAlias(
                c,
                annotations,
                descriptor,
                element,
                qualifierResolutionResult
            )

            else -> error("Unexpected classifier type: ${descriptor::class.java}")
        }
    }

    private fun shouldCheckBounds(c: TypeResolutionContext, inType: CangJieType): Boolean {
        if (!c.checkBounds) return false
        if (inType.containsTypeAliasParameters()) return false
        if (c.abbreviated && inType.containsTypeAliases()) return false

        return true
    }

    /**
     * 接受一个名称原子表达式
     * @return 返回一个类型，该类型带有类型参数，并检查边界
     */
    fun resolveTypeForClass(
        expression: CjSimpleNameExpression,
        scope: LexicalScope,
        trace: BindingTrace,
        classDescriptor: DeclarationDescriptor,
    ): CangJieType? {
        if (classDescriptor !is ClassAndEnumDescriptor) return null
        if (expression !is CjNameReferenceExpression) return null
        val c = TypeResolutionContext(scope, trace, true, false, false)
        val typeConstructor = classDescriptor.typeConstructor
        val parameters = typeConstructor.parameters


        val typeArguments = expression.typeArguments


//        val (collectedArgumentAsTypeProjections, argumentsForOuterClass) =
//            collectArgumentsForClassifierTypeConstructor(c, classDescriptor, emptyList())
//                ?:  return null

        val argumentsFromUserType = resolveTypeProjections(c, typeConstructor, typeArguments)

        if (argumentsFromUserType.isNotEmpty() && argumentsFromUserType.size != parameters.size) {
            c.trace.report(
                WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                    expression.typeArgumentList ?: expression,
                    argumentsFromUserType.size, classDescriptor
                )
            )
            return null
        }
        val arguments = buildFinalArgumentList(argumentsFromUserType, null, parameters)

        val resultingType =
            CangJieTypeFactory.simpleType(
                typeAttributeTranslators.toAttributes(
                    Annotations.EMPTY,
                    classDescriptor.typeConstructor,
                    c.scope.ownerDescriptor
                ),
                classDescriptor,
                arguments
            )

        if (shouldCheckBounds(c, resultingType)) {
            val substitutor = ComposableTypeSubstitutor.create(resultingType)
            for (i in parameters.indices) {
                val parameter = parameters[i]
                val argument = arguments[i].type

                val typeReference = typeArguments.getOrNull(i)?.typeReference

                if (typeReference != null) {
                    upperBoundChecker.checkBounds(typeReference, argument, parameter, substitutor, c.trace)
                }
            }
        }


        return resultingType


    }

    /**
     * 为类或枚举类型解析类型
     *
     * 统一处理类和枚举的类型解析逻辑，因为它们具有相同的特性：
     * - 类型构造器和类型参数
     * - 类型参数绑定和边界检查
     * - 支持泛型实例化
     *
     * @param c 类型解析上下文
     * @param annotations 注解
     * @param classDescriptor 类或枚举描述符
     * @param element PSI 元素
     * @param qualifierResolutionResult 限定符解析结果
     * @return 可能为 Bare 类型的类型
     */
    private fun resolveTypeForClassOrEnum(
        c: TypeResolutionContext, annotations: Annotations,
        classDescriptor: ClassAndEnumDescriptor, element: CjElement,
        qualifierResolutionResult: TypeQualifierResolutionResult
    ): PossiblyBareType {
        val typeConstructor = classDescriptor.typeConstructor

        val projectionFromAllQualifierParts = qualifierResolutionResult.allProjections
        val parameters = typeConstructor.parameters
        if (c.allowBareTypes && projectionFromAllQualifierParts.isEmpty() && isPossibleToSpecifyTypeArgumentsFor(
                classDescriptor
            )
        ) {
            // See docs for PossiblyBareType
            return bare(typeConstructor, false)
        }

        if (ErrorUtils.isError(classDescriptor)) {
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }

        val (collectedArgumentAsTypeProjections, argumentsForOuterClass) =
            collectArgumentsForClassifierTypeConstructor(c, classDescriptor, qualifierResolutionResult.qualifierParts)
                ?: return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)

        assert(collectedArgumentAsTypeProjections.size <= parameters.size) {
            "Collected arguments count should be not greater then parameters count," +
                    " but ${collectedArgumentAsTypeProjections.size} instead of ${parameters.size} found in ${element.text}"
        }

        val argumentsFromUserType = resolveTypeProjections(c, typeConstructor, collectedArgumentAsTypeProjections)
        val arguments = buildFinalArgumentList(argumentsFromUserType, argumentsForOuterClass, parameters)

        assert(arguments.size == parameters.size) {
            "Collected arguments count should be equal to parameters count," +
                    " but ${collectedArgumentAsTypeProjections.size} instead of ${parameters.size} found in ${element.text}"
        }

        val resultingType =
            CangJieTypeFactory.simpleType(
                typeAttributeTranslators.toAttributes(
                    annotations,
                    classDescriptor.typeConstructor,
                    c.scope.ownerDescriptor
                ),
                classDescriptor,
                arguments
            )

        if (shouldCheckBounds(c, resultingType)) {
            val substitutor = ComposableTypeSubstitutor.create(resultingType)
            for (i in parameters.indices) {
                val parameter = parameters[i]
                val argument = arguments[i].type

                val typeReference = collectedArgumentAsTypeProjections.getOrNull(i)?.typeReference

                if (typeReference != null) {
                    upperBoundChecker.checkBounds(typeReference, argument, parameter, substitutor, c.trace)
                }
            }
        }

        return type(resultingType)
    }

    /**
     * 为枚举类型解析类型
     *
     * @param c 类型解析上下文
     * @param annotations 注解
     * @param classDescriptor 枚举描述符
     * @param element PSI 元素
     * @param qualifierResolutionResult 限定符解析结果
     * @return 可能为 Bare 类型的类型
     */
    fun resolveTypeForEnum(
        c: TypeResolutionContext, annotations: Annotations,
        classDescriptor: EnumDescriptor, element: CjElement,
        qualifierResolutionResult: TypeQualifierResolutionResult
    ): PossiblyBareType = resolveTypeForClassOrEnum(c, annotations, classDescriptor, element, qualifierResolutionResult)

    /**
     * 为类类型解析类型
     *
     * @param c 类型解析上下文
     * @param annotations 注解
     * @param classDescriptor 类描述符
     * @param element PSI 元素
     * @param qualifierResolutionResult 限定符解析结果
     * @return 可能为 Bare 类型的类型
     */
    fun resolveTypeForClass(
        c: TypeResolutionContext, annotations: Annotations,
        classDescriptor: ClassDescriptor, element: CjElement,
        qualifierResolutionResult: TypeQualifierResolutionResult
    ): PossiblyBareType = resolveTypeForClassOrEnum(c, annotations, classDescriptor, element, qualifierResolutionResult)

    private fun buildFinalArgumentList(
        argumentsFromUserType: List<TypeArgument>,
        argumentsForOuterClass: List<TypeArgument>?,
        parameters: List<TypeParameterDescriptor>
    ): List<TypeArgument> {
        return argumentsFromUserType +
                (argumentsForOuterClass ?: appendDefaultArgumentsForLocalClassifier(
                    argumentsFromUserType.size,
                    parameters
                ))
    }

    /**
     * For cases like:
     * func <E> foo() {
     *  class Local<F>
     *  let x: Local<Int> <-- resolveName this type
     * }
     *
     * type constructor for `Local` captures type parameter E from containing outer function
     */
    private fun appendDefaultArgumentsForLocalClassifier(
        fromIndex: Int,
        constructorParameters: List<TypeParameterDescriptor>
    ) = constructorParameters.subList(fromIndex, constructorParameters.size).map {
        TypeArgumentImpl(it.original.defaultType)
    }

    private fun getScopeForTypeParameter(
        c: TypeResolutionContext,
        typeParameterDescriptor: TypeParameterDescriptor
    ): MemberScope {
        return when {
            c.checkBounds -> TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor).memberScope
            else -> LazyScopeAdapter {
                TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor).memberScope
            }
        }
    }

    private fun resolveTypeForTypeParameter(
        c: TypeResolutionContext, annotations: Annotations,
        typeParameter: TypeParameterDescriptor,
        referenceExpression: CjSimpleNameExpression,
        typeArgumentList: CjTypeArgumentList?
    ): CangJieType {
        val scopeForTypeParameter = getScopeForTypeParameter(c, typeParameter)

        if (typeArgumentList != null) {
            resolveTypeProjections(
                c,
                ErrorUtils.createErrorType(ErrorTypeKind.ERROR_TYPE_PARAMETER).constructor,
                typeArgumentList.arguments
            )
            c.trace.report(TYPE_ARGUMENTS_NOT_ALLOWED.on(typeArgumentList, "for type parameters"))
        }

        val containing = typeParameter.containingDeclaration
        if (containing is ClassDescriptor) {
            DescriptorResolver.checkHasOuterClassInstance(c.scope, c.trace, referenceExpression, containing)
        }

        return if (scopeForTypeParameter is ErrorScope && scopeForTypeParameter !is ThrowingScope)
            ErrorUtils.createErrorType(ErrorTypeKind.ERROR_TYPE_PARAMETER)
        else
            CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
                typeAttributeTranslators.toAttributes(annotations, typeParameter.typeConstructor, containing),
                typeParameter.typeConstructor,
                listOf(),
                scopeForTypeParameter
            )
    }

    /**
     * 解析可能为 Bare 类型的类型引用
     *
     * 解析类型引用，结果可能是 Bare 类型或完整类型。
     * Bare 类型用于 is/as 表达式中的类型模式，例如 `x is List`（省略类型参数）。
     *
     * 处理流程：
     * 1. 尝试从缓存中获取已解析的类型（如果启用缓存）
     * 2. 解析类型注解
     * 3. 解析类型元素（委托给 resolveTypeElement）
     * 4. 如果是完整类型（非 Bare），强制解析类型参数并记录到绑定上下文
     * 5. 记录作用域信息
     *
     * @param c 类型解析上下文
     * @param typeReference 类型引用 PSI 元素
     * @return 可能为 Bare 类型的解析结果
     */
    fun resolvePossiblyBareType(
        c: TypeResolutionContext,
        typeReference: CjTypeReference,


        ): PossiblyBareType {
        if (c.useCache) {
            val cachedType = c.trace.bindingContext[BindingContext.TYPE, typeReference]
            if (cachedType != null) return type(cachedType)
        }


        val resolvedTypeSlice = if (c.abbreviated) BindingContext.ABBREVIATED_TYPE else BindingContext.TYPE

        val annotations = resolveTypeAnnotations(c.trace, c.scope, typeReference)
        val type =
            resolveTypeElement(c, annotations, typeReference.modifierList, typeReference.typeElement)
        c.trace.recordScope(c.scope, typeReference)

        if (!type.isBare()) {
            for (argument in type.actualType.arguments) {
                forceResolveTypeContents(argument.type)
            }
            c.trace.record(resolvedTypeSlice, typeReference, type.actualType)
        }
        return type
    }

    /**
     * 解析类型引用（内部实现）
     *
     * 解析类型引用为完整的语义类型。
     * 该方法不允许 Bare 类型，如果需要 Bare 类型支持，使用 resolvePossiblyBareType。
     *
     * @param c 类型解析上下文
     * @param typeReference 类型引用 PSI 元素
     * @return 解析得到的完整类型
     * @throws AssertionError 如果上下文允许 Bare 类型
     */
    private fun resolveType(
        c: TypeResolutionContext,
        typeReference: CjTypeReference,
    ): CangJieType {
        assert(!c.allowBareTypes) { "Use resolvePossiblyBareType() when bare types are allowed" }

        return resolvePossiblyBareType(c, typeReference).actualType
    }


    /**
     * 解析多个类型引用的公共超类型
     *
     * 解析多个类型引用并计算它们的公共超类型。
     * 这用于多重异常捕获等场景，例如：
     * ```
     * try {
     *     ...
     * } catch (e: IOException | NetworkException) {
     *     ...
     * }
     * ```
     *
     * @param scope 解析作用域
     * @param typeReference 类型引用 PSI 元素列表
     * @param trace 绑定追踪器
     * @param checkBounds 是否检查类型参数上界
     * @return 所有类型的公共超类型
     */
    fun resolveType(
        scope: LexicalScope,
        typeReference: List<CjTypeReference>,
        trace: BindingTrace,
        checkBounds: Boolean,
    ): CangJieType {
        val types = typeReference.map {
            resolveType(scope, it, trace, checkBounds)
        }

        val resultType = SimpleClassicTypeSystemContext.commonSuperType(
            types
        )
        return resultType as CangJieType
    }


    /**
     * 解析类型引用（公共接口）
     *
     * 解析单个类型引用为完整的语义类型。
     * 这是主要的公共接口，被编译器的各个阶段广泛使用。
     *
     * 功能：
     * - 解析类型引用为 CangJieType
     * - 可选的类型边界检查
     * - 可选的类型缓存
     * - 不允许 Bare 类型
     *
     * @param scope 解析作用域
     * @param typeReference 类型引用 PSI 元素
     * @param trace 绑定追踪器
     * @param checkBounds 是否检查类型参数的上界约束
     * @param useCache 是否使用类型缓存（默认 true）
     * @return 解析得到的完整类型
     */
    @JvmOverloads
    fun resolveType(
        scope: LexicalScope,
        typeReference: CjTypeReference,
        trace: BindingTrace,
        checkBounds: Boolean,
        useCache: Boolean = true
    ): CangJieType {
        // bare types are not allowed
        return resolveType(
            TypeResolutionContext(
                scope,
                trace,
                checkBounds,
                false,
                typeReference.suppressDiagnosticsInDebugMode(),
                false,
                useCache
            ),
            typeReference,

            )
    }
}
