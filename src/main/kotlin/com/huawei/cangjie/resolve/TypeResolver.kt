package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.descriptors.Errors.UNSUPPORTED
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.debugtext.getDebugText
import com.huawei.cangjie.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import com.huawei.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.resolve.PossiblyBareType.type
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.util.createBasicType

class TypeResolver(
    private val annotationResolver: AnnotationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val languageVersionSettings: LanguageVersionSettings,

    ) {

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
            trace.report(Errors.NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES.on(lastAnnotationEntry))
        }
    }

    fun resolveTypeAnnotations(
        trace: BindingTrace,
        scope: LexicalScope,
        modifierListsOwner: CjElementImplStub<*>
    ): Annotations {
//        val modifierLists = modifierListsOwner.getAllModifierLists()

        var result = Annotations.EMPTY
        var isSplitModifierList = false

//        if (!isNonParenthesizedAnnotationsOnFunctionalTypesEnabled) {
//            val targetType = when (modifierListsOwner) {
////                is CjNullableType -> modifierListsOwner.innerType
//                is CjTypeReference -> modifierListsOwner.typeElement
//                else -> null
//            }
//            val annotationEntries = when (modifierListsOwner) {
////                is CjNullableType -> modifierListsOwner.modifierList?.annotationEntries
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
            if (!projection.isStarProjection) {
                forceResolveTypeContents(projection.type)
            }
        }
//        }
    }

    private fun resolveTypeElement(
        c: TypeResolutionContext,
        annotations: Annotations,
        outerModifierList: CjModifierList?,
        typeElement: CjTypeElement?
    ): PossiblyBareType {
        var result: PossiblyBareType? = null


        typeElement?.accept(object : CjVisitorVoid() {
            override fun visitBasicType(type: CjBasicType) {

                result = type(createBasicType(moduleDescriptor.builtIns, type.text))
            }

            override fun visitUserType(type: CjUserType) {
//                super.visitUserType(type)
            }

            override fun visitFunctionType(type: CjFunctionType) {
//                return super.visitFunctionType(type)
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

    fun resolvePossiblyBareType(c: TypeResolutionContext, typeReference: CjTypeReference): PossiblyBareType {
        val cachedType = c.trace.bindingContext.get(BindingContext.TYPE, typeReference)
        if (cachedType != null) return type(cachedType)

        val resolvedTypeSlice = if (c.abbreviated) BindingContext.ABBREVIATED_TYPE else BindingContext.TYPE

        val annotations = resolveTypeAnnotations(c.trace, c.scope, typeReference)
        val type = resolveTypeElement(c, annotations, typeReference.modifierList, typeReference.typeElement)
        c.trace.recordScope(c.scope, typeReference)

        if (!type.isBare) {
            for (argument in type.actualType.arguments) {
                forceResolveTypeContents(argument.type)
            }
            c.trace.record(resolvedTypeSlice, typeReference, type.actualType)
        }
        return type
    }

    private fun resolveType(c: TypeResolutionContext, typeReference: CjTypeReference): CangJieType {
        assert(!c.allowBareTypes) { "Use resolvePossiblyBareType() when bare types are allowed" }
//        TODO()
        return resolvePossiblyBareType(c, typeReference).actualType
    }

    fun resolveType(
        scope: LexicalScope,
        typeReference: CjTypeReference,
        trace: BindingTrace,
        checkBounds: Boolean
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
            typeReference
        )
    }
}
