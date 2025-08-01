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

import com.intellij.util.SmartList
import cn.cangnova.cangjie.builtins.createFunctionType
import cn.cangnova.cangjie.builtins.createTupleType
import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.annotations.composeAnnotations
import cn.cangnova.cangjie.descriptors.impl.AbstractVariableDescriptor
import cn.cangnova.cangjie.diagnostics.Errors.*
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.debugtext.getDebugText
import cn.cangnova.cangjie.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import cn.cangnova.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import cn.cangnova.cangjie.resolve.PossiblyBareType.bare
import cn.cangnova.cangjie.resolve.PossiblyBareType.type
import cn.cangnova.cangjie.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import cn.cangnova.cangjie.resolve.scopes.*
import cn.cangnova.cangjie.resolve.source.CangJieSourceElement
import cn.cangnova.cangjie.resolve.source.getPsi
import cn.cangnova.cangjie.resolve.source.toSourceElement
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.ErrorUtils.invalidType
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext
import cn.cangnova.cangjie.types.checker.TrailingCommaChecker
import cn.cangnova.cangjie.types.error.ErrorScope
import cn.cangnova.cangjie.types.error.ErrorTypeKind
import cn.cangnova.cangjie.types.error.ThrowingScope
import cn.cangnova.cangjie.types.expressions.TypeAttributeTranslators
import cn.cangnova.cangjie.types.util.*
import cn.cangnova.cangjie.types.util.TypeUtils.addTypeParameterToStub
import kotlin.math.min

class TypeResolver(
    private val annotationResolver: AnnotationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val identifierChecker: IdentifierChecker,

    private val languageVersionSettings: LanguageVersionSettings,
    private val qualifiedExpressionResolver: QualifiedExpressionResolver,
    private val typeAttributeTranslators: TypeAttributeTranslators,
    private val upperBoundChecker: UpperBoundChecker,
//    private val platformToCangJieClassMapper: PlatformToCangJieClassMapper,

) {
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

    fun resolveClass(
        scope: LexicalScope, userType: CjUserType, trace: BindingTrace, isDebuggerContext: Boolean
    ): ClassifierDescriptor? = resolveDescriptorForType(scope, userType, trace, isDebuggerContext).classifierDescriptor

    fun resolveExpandedTypeForTypeAlias(typeAliasDescriptor: TypeAliasDescriptor): SimpleType {
        val typeAliasExpansion = TypeAliasExpansion.createWithFormalArguments(typeAliasDescriptor)
        val expandedType =
            TypeAliasExpander.NON_REPORTING.expandWithoutAbbreviation(typeAliasExpansion, TypeAttributes.Empty)
        return expandedType
    }

    internal fun CjElementImplStub<*>.getAllModifierLists(): Array<out CjDeclarationModifierList> =
        getStubOrPsiChildren(CjStubElementTypes.MODIFIER_LIST, CjStubElementTypes.MODIFIER_LIST.arrayFactory)

    private fun checkNonParenthesizedAnnotationsOnFunctionalType(
        typeElement: CjFunctionType,
        annotationEntries: List<CjAnnotationEntry>,
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

    fun resolveTypeAnnotations(
        trace: BindingTrace,
        scope: LexicalScope,
        modifierListsOwner: CjElementImplStub<*>
    ): Annotations {
//        val modifierLists = modifierListsOwner.getAllModifierLists()

        val result = Annotations.EMPTY
//        var isSplitModifierList = false

//        if (!isNonParenthesizedAnnotationsOnFunctionalTypesEnabled) {
//            val targetType = when (modifierListsOwner) {

//                is CjTypeReference -> modifierListsOwner.typeElement
//                else -> null
//            }
//            val annotationEntries = when (modifierListsOwner) {

//                is CjTypeReference -> modifierListsOwner.annotationEntries
//                else -> null
//            }
//
//            // `targetType.stub == null` means that we don't apply this check for files that are built with stubs (that aren't opened in IDE and not in compile time)
//            if (targetType is CjFunctionType && targetType.stub == null && annotationEntries != null) {
//                checkNonParenthesizedAnnotationsOnFunctionalType(targetType, annotationEntries, trace)
//            }
//        }

//        for (modifierList in modifierLists) {
//            if (isSplitModifierList) {
//                trace.report(MODIFIER_LIST_NOT_ALLOWED.on(modifierList))
//            }
//
//            val annotations =
//                annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList.annotationEntries, trace)
//            result = composeAnnotations(result, annotations)
//
//            isSplitModifierList = true
//        }

        return result
    }

    /**
     *  This function is light version of ForceResolveUtil.forceResolveAllContents
     *  We can't use ForceResolveUtil.forceResolveAllContents here because it runs ForceResolveUtil.forceResolveAllContents(getConstructor()),
     *  which is unsafe for some cyclic cases. For Example:
     *  class A: List<A.B> {
     *    class B
     *  }
     *  Here when we resolve class B, we should resolve supertype for A and we shouldn't start resolve for class B,
     *  otherwise it would be a cycle.
     *  Now there is no cycle here because member scope for A is very clever and can get lazy descriptor for class B without resolving it.
     *
     *  todo: find another way after release
     */
    private fun forceResolveTypeContents(type: CangJieType) {
        type.annotations // force read type annotations
//        if (type.isFlexible()) {
//            forceResolveTypeContents(type.asFlexibleType().lowerBound)
//            forceResolveTypeContents(type.asFlexibleType().upperBound)
//        } else {
        type.constructor // force read type constructor
        for (projection in type.arguments) {

            forceResolveTypeContents(projection.type)

        }
//        }
    }

    fun resolveDescriptorForType(
        scope: LexicalScope, userType: CjUserType, trace: BindingTrace, isDebuggerContext: Boolean
    ): QualifiedExpressionResolver.TypeQualifierResolutionResult {
        if (userType.qualifier != null) { // 必须解析限定符类型参数中的所有类型引用
            for (typeArgument in userType.qualifier!!.typeArguments) {
                typeArgument.typeReference?.let {
                    // 在限定表达中，类型参数只能在不正确的代码中有界限
                    forceResolveTypeContents(resolveType(scope, it, trace, false))
                }
            }
        }

        return qualifiedExpressionResolver.resolveDescriptorForType(userType, scope, trace, isDebuggerContext).apply {
//            if (classifierDescriptor != null) {
//                PlatformClassesMappedToCangJieChecker.reportPlatformClassMappedToCangJie(
//                    platformToCangJieClassMapper, trace, userType, classifierDescriptor
//                )
//            }
        }
    }


    fun resolveTypeProjections(
        c: TypeResolutionContext,
        constructor: TypeConstructor,
        argumentElements: List<CjTypeProjection>
    ): List<TypeProjection> {
        return argumentElements.mapIndexed { i, argumentElement ->

            ModifierCheckerCore.check(argumentElement, c.trace, null, languageVersionSettings)

            val type = resolveType(c.noBareTypes(), argumentElement.typeReference!!)


            TypeProjectionImpl(type)


        }

    }

    fun String.toName(): Name {
        return Name.identifier(this)
    }

    private fun resolveTypeElement(
        c: TypeResolutionContext,
        annotations: Annotations,
        outerModifierList: CjModifierList?,
        typeElement: CjTypeElement?,
//        该参数仅解决出现递归调用
        isgetExtend: Boolean = true
    ): PossiblyBareType {

        fun resolveOptionType(): CangJieType {
            val classifier = c.scope.findFirstClassifierWithDeprecationStatus(
                OPTION,
                NoLookupLocation.FROM_BUILTINS
            ) ?: throw IllegalStateException("Option type not found")
            return classifier.descriptor.defaultType
        }


        var result: PossiblyBareType? = null


        typeElement?.accept(object : CjVisitorVoid() {
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

//                val extendSuper = if (isgetExtend) {
//                    c.scope.getExtendClasss(type.name.toName(), NoLookupLocation.FROM_BUILTINS)
//                } else {
//                    emptyList()
//                }.toSet()
                result = type(createBasicType(moduleDescriptor.builtIns, type.text /*extendSuper*/))
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

            override fun visitParenthesizedType(cjParenthesizedType: CjParenthesizedType) {
                result = resolveTypeElement(c, Annotations.EMPTY, null, cjParenthesizedType.getType())
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
                    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
                        throw UnsupportedOperationException("Should not be called for descriptor of type ${this::class.java}")
                    }

                    override val isVar: Boolean = false
                    override fun getOverriddenDescriptors(): List<CallableDescriptor> {
                        return emptyList()
                    }


                    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
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
                        annotationResolver.resolveAnnotationsWithoutArguments(c.scope, parameter.modifierList, c.trace),
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
                            param.defaultValue!!,
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

                val contextReceiverList = type.contextReceiverList
                val contextReceiversTypes = if (contextReceiverList != null) {
                    checkContextReceiversAreEnabled(c.trace, languageVersionSettings, contextReceiverList)
                    val types = contextReceiverList.typeReferences().map { typeRef ->
                        resolveType(c.noBareTypes(), typeRef)
                    }
                    checkSubtypingBetweenContextReceivers(c.trace, contextReceiverList, types)
                    types
                } else emptyList()

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
                        moduleDescriptor.builtIns, annotations, receiverType, contextReceiversTypes,
                        parameterDescriptors.map { it.type },
                        parameterDescriptors.map { it.name },
                        returnType,

                        )
                )
            }

            override fun visitOptionType(optionType: CjOptionType) {
                val innerType = optionType.getInnerType()

                val baseType = createTypeFromInner(optionType, optionType.getModifierList(), innerType)

                if (!baseType.isBare && baseType.actualType is DefinitelyNotNullType) {
                    c.trace.report(NULLABLE_ON_DEFINITELY_NOT_OPTIONAL.on(optionType))
                }

                if (baseType.isOptional || innerType is CjOptionType/* || innerType is CjDynamicType*/) {
                    c.trace.report(REDUNDANT_OPTIONAL.on(optionType))

                    c.trace.report(NESTING_DOLL_OPTINOTYPE.on(optionType))
                }

                result = type(CangJieTypeFactory.optionType(addTypeParameterToStub(resolveOptionType(), baseType.actualType) as SimpleType))
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
     * Type alias can be used as bare type (after is/as, e.g., 'x is List')
     * iff all type arguments of the corresponding expanded type are either star projections
     * or type parameters of the given type alias in invariant projection,
     * and each of the type parameters is mentioned no more than once.
     *
     * E.g.:
     * ```
     * typealias HashMap<K, V> = java.util.HashMap<K, V>    // can be used as bare type
     * typealias MyList<T, X> = List<X>                     // can be used as bare type
     * typealias StarMap<T> = Map<T, *>                     // can be used as bare type
     * typealias MyMap<T> = Map<T, T>                       // CAN NOT be used as bare type: type parameter 'T' is used twice
     * typealias StringMap<T> = Map<String, T>              // CAN NOT be used as bare type: type argument 'String' is not a type parameter
     * ```
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
//    private fun LexicalScope.findImplicitOuterClassArguments(
//        outerClass: ClassDescriptor
//    ): List<TypeProjection>? {
//        val enclosingClass = findFirstFromMeAndParent { scope ->
//            if (scope is LexicalScope && scope.kind == LexicalScopeKind.CLASS_MEMBER_SCOPE)
//                scope.ownerDescriptor as ClassDescriptor
//            else
//                null
//        } ?: return null
//
//        return findImplicitOuterClassArguments(enclosingClass, outerClass)
//    }

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
        qualifierParts: List<QualifiedExpressionResolver.ExpressionQualifierPart>
    ): Pair<List<CjTypeProjection>, List<TypeProjection>?>? {
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
                c.trace.report(TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED.on(qualifierPart.typeArguments!!))
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
            substitutor: TypeSubstitutor,
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
            val annotationEntry = (annotation.source as? CangJieSourceElement)?.psi as? CjAnnotationEntry ?: return
            trace.report(REPEATED_ANNOTATION.on(annotationEntry))
        }
    }

    private fun resolveTypeForTypeAlias(
        c: TypeResolutionContext,
        annotations: Annotations,
        descriptor: TypeAliasDescriptor,
        type: CjElement,
        qualifierResolutionResult: QualifiedExpressionResolver.TypeQualifierResolutionResult
    ): PossiblyBareType {
        val typeConstructor = descriptor.typeConstructor
        val projectionFromAllQualifierParts = qualifierResolutionResult.allProjections

        if (ErrorUtils.isError(descriptor)) {
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }
        if (!languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)) {
            c.trace.report(UNSUPPORTED_FEATURE.on(type, LanguageFeature.TypeAliases to languageVersionSettings))
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
                    TypeUtils.isNullableType(descriptor.expandedType)
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
                false
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
        qualifierResolutionResult: QualifiedExpressionResolver.TypeQualifierResolutionResult,
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
        if (classDescriptor !is ClassDescriptor) return null
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
            CangJieTypeFactory.simpleNotNullType(
                typeAttributeTranslators.toAttributes(
                    Annotations.EMPTY,
                    classDescriptor.typeConstructor,
                    c.scope.ownerDescriptor
                ),
                classDescriptor,
                arguments
            )

        if (shouldCheckBounds(c, resultingType)) {
            val substitutor = TypeSubstitutor.create(resultingType)
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

    fun resolveTypeForClass(
        c: TypeResolutionContext, annotations: Annotations,
        classDescriptor: ClassDescriptor, element: CjElement,
        qualifierResolutionResult: QualifiedExpressionResolver.TypeQualifierResolutionResult
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
            CangJieTypeFactory.simpleNotNullType(
                typeAttributeTranslators.toAttributes(
                    annotations,
                    classDescriptor.typeConstructor,
                    c.scope.ownerDescriptor
                ),
                classDescriptor,
                arguments
            )

        // We create flexible types by convention here
        // This is not intended to be used in normal users' environments, only for tests and debugger etc
//        typeTransformerForTests.transformType(resultingType)?.let { return type(it) }

        if (shouldCheckBounds(c, resultingType)) {
            val substitutor = TypeSubstitutor.create(resultingType)
            for (i in parameters.indices) {
                val parameter = parameters[i]
                val argument = arguments[i].type

                val typeReference = collectedArgumentAsTypeProjections.getOrNull(i)?.typeReference

                if (typeReference != null) {
                    upperBoundChecker.checkBounds(typeReference, argument, parameter, substitutor, c.trace)
                }
            }
        }

//        if (resultingType.isArrayOfNothing()) {
//            c.trace.report(UNSUPPORTED.on(element, "Array<Nothing> is illegal"))
//        }

        return type(resultingType)
    }

    private fun buildFinalArgumentList(
        argumentsFromUserType: List<TypeProjection>,
        argumentsForOuterClass: List<TypeProjection>?,
        parameters: List<TypeParameterDescriptor>
    ): List<TypeProjection> {
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
     *  let x: Local<Int> <-- resolve this type
     * }
     *
     * type constructor for `Local` captures type parameter E from containing outer function
     */
    private fun appendDefaultArgumentsForLocalClassifier(
        fromIndex: Int,
        constructorParameters: List<TypeParameterDescriptor>
    ) = constructorParameters.subList(fromIndex, constructorParameters.size).map {
        TypeProjectionImpl(it.original.defaultType)
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
                false,
                scopeForTypeParameter
            )
    }

    fun resolvePossiblyBareType(
        c: TypeResolutionContext,
        typeReference: CjTypeReference,
        isgetExtend: Boolean = true

    ): PossiblyBareType {
        if (c.useCache) {
            val cachedType = c.trace.bindingContext.get(BindingContext.TYPE, typeReference)
            if (cachedType != null) return type(cachedType)
        }


        val resolvedTypeSlice = if (c.abbreviated) BindingContext.ABBREVIATED_TYPE else BindingContext.TYPE

        val annotations = resolveTypeAnnotations(c.trace, c.scope, typeReference)
        val type =
            resolveTypeElement(c, annotations, typeReference.modifierList, typeReference.typeElement, isgetExtend)
        c.trace.recordScope(c.scope, typeReference)

        if (!type.isBare) {
            for (argument in type.actualType.arguments) {
                forceResolveTypeContents(argument.type)
            }
            c.trace.record(resolvedTypeSlice, typeReference, type.actualType)
        }
        return type
    }

    private fun resolveType(
        c: TypeResolutionContext,
        typeReference: CjTypeReference,
        isgetExtend: Boolean = true
    ): CangJieType {
        assert(!c.allowBareTypes) { "Use resolvePossiblyBareType() when bare types are allowed" }

        return resolvePossiblyBareType(c, typeReference, isgetExtend).actualType
    }

    fun resolveEnumType(
        scope: LexicalScope,
        typeReference: CjTypeReference,
        trace: BindingTrace,
        checkBounds: Boolean,

        ): CangJieType {
        // bare types are not allowed
        return resolveType(
            TypeResolutionContext(
                scope,
                trace,
                checkBounds,
                false,
                typeReference.suppressDiagnosticsInDebugMode(),
                false
            ),
            typeReference,
            true
        )
    }

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


    @JvmOverloads
    fun resolveType(
        scope: LexicalScope,
        typeReference: CjTypeReference,
        trace: BindingTrace,
        checkBounds: Boolean,
        isgetExtend: Boolean = true,
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
            isgetExtend
        )
    }
}
