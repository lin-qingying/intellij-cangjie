/*
 * Copyright 2025 LinQingYing. and contributors.
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

import java.util.ArrayDeque
import org.cangnova.cangjie.builtins.ReflectionTypes
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.context.TemporaryTraceAndCache
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import org.cangnova.cangjie.resolve.calls.util.createValueParametersForInvokeInFunctionType
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.DataFlowAnalyzer
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import jakarta.inject.Inject
import org.cangnova.cangjie.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolver
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo


sealed class DoubleColonLHS(val type: CangJieType) {

    class Expression(val typeInfo: CangJieTypeInfo) : DoubleColonLHS(typeInfo.type!!) {
        val dataFlowInfo: DataFlowInfo = typeInfo.dataFlowInfo
    }

    class Type(type: CangJieType, val possiblyBareType: PossiblyBareType) : DoubleColonLHS(type)
}

private fun CangJieTypeRefiner.refineBareType(type: PossiblyBareType): PossiblyBareType {
    if (type.isBare()) return type
    val newType = type.actualType.let { refineType(it) }
    return PossiblyBareType.type(newType)
}

class DoubleColonExpressionResolver(
    val callResolver: CallResolver,
    val qualifiedExpressionResolver: QualifiedExpressionResolver,
    val dataFlowAnalyzer: DataFlowAnalyzer,
    val reflectionTypes: ReflectionTypes,
    val typeResolver: TypeResolver,
    val languageVersionSettings: LanguageVersionSettings,
//    val additionalCheckers: Iterable<ClassLiteralChecker>,
    val dataFlowValueFactory: DataFlowValueFactory,
//    val bigAritySupport: FunctionWithBigAritySupport,
//    val genericArrayClassLiteralSupport: GenericArrayClassLiteralSupport,
    val cangjieTypeRefiner: CangJieTypeRefiner
) {
    private lateinit var expressionTypingServices: ExpressionTypingServices

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    private data class ReservedDoubleColonLHSResolutionResult(
        val isReservedExpressionSyntax: Boolean,
        val lhs: DoubleColonLHS?,
        val traceAndCache: TemporaryTraceAndCache?
    )

    private class LHSResolutionResult<out T : DoubleColonLHS>(
        val lhs: T?,
        val expression: CjExpression,
        val traceAndCache: TemporaryTraceAndCache
    ) {
        fun commit(): T? {
//            if (lhs != null) {
//                traceAndCache.trace.record(BindingContext.DOUBLE_COLON_LHS, expression, lhs)
//            }
            traceAndCache.commit()
            return lhs
        }
    }

    internal fun bindFunctionReference(
        expression: CjCallableReference,
        type: CangJieType,
        context: ResolutionContext<*>,
        referencedFunction: FunctionDescriptor
    ) {
        val functionDescriptor = AnonymousFunctionDescriptor(
            context.scope.ownerDescriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.DECLARATION,
            expression.toSourceElement(),

            )

        functionDescriptor.initialize(
            null, null, emptyList(), emptyList(),
            createValueParametersForInvokeInFunctionType(functionDescriptor, type.arguments.dropLast(1)),
            type.arguments.last().type,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )

        context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor)
//
//        if (functionDescriptor.valueParameters.size >= BuiltInFunctionArity.BIG_ARITY &&
//            bigAritySupport.shouldCheckLanguageVersionSettings &&
//            !languageVersionSettings.supportsFeature(LanguageFeature.FunctionTypesWithBigArity)
//        ) {
//            context.trace.report(
//                UNSUPPORTED_FEATURE.on(expression, LanguageFeature.FunctionTypesWithBigArity to languageVersionSettings)
//            )
//        }
    }

    private fun resolveExpressionOnLHS(
        expression: CjExpression,
        c: ExpressionTypingContext
    ): DoubleColonLHS.Expression? {
        val typeInfo = expressionTypingServices.getTypeInfo(expression, c)

        // TODO: do not lose data flow info maybe
        if (typeInfo.type == null) return null

        // Be careful not to call a utility function to get a resolved call by an expression which may accidentally
        // deparenthesize that expression, as this is undesirable here
        val call = c.trace.bindingContext[BindingContext.CALL, expression.getQualifiedElementSelector() ?: return null]
        val resolvedCall = call?.getResolvedCall(c.trace.bindingContext)

//        if (resolvedCall != null) {
//            val resultingDescriptor = resolvedCall.resultingDescriptor
//            if (resultingDescriptor is FakeCallableDescriptorForObject) {
//                val classDescriptor = resultingDescriptor.classDescriptor
//
//                if (DescriptorUtils.isObject(classDescriptor) ||
//                    (!languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences) &&
//                            DescriptorUtils.isEnumEntry(classDescriptor))) {
//                    return DoubleColonLHS.Expression(typeInfo )
//                }
//            }
//
//            // Check if this is resolved to a function (with the error "arguments expected"), such as in "Runnable::class"
//            if (expression.canBeConsideredProperType() && resultingDescriptor !is VariableDescriptor) return null
//        }

        return DoubleColonLHS.Expression(typeInfo)
    }

    /**
     * Returns null if the LHS is definitely not an expression. Returns a non-null result if a resolution was attempted and led to
     * either a successful result or not.
     */
    private fun <T : DoubleColonLHS> tryResolveLHS(
        doubleColonExpression: CjCallableReference,
        context: ExpressionTypingContext,

        resolve: (CjExpression, ExpressionTypingContext) -> T?
    ): LHSResolutionResult<T>? {
        val expression = doubleColonExpression.receiverExpression ?: return null


        val traceAndCache = TemporaryTraceAndCache.create(context, "resolve '::' LHS", doubleColonExpression)
        val c = context
            .replaceTraceAndCache(traceAndCache)
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        val lhs = resolve(expression, c)
        return LHSResolutionResult(lhs, expression, traceAndCache)
    }

    private fun resolveTypeOnLHS(
        expression: CjExpression, doubleColonExpression: CjCallableReference, c: ExpressionTypingContext
    ): DoubleColonLHS.Type? {
        val qualifierResolutionResult =
            qualifiedExpressionResolver.resolveDescriptorForDoubleColonLHS(
                expression,
                c.scope,
                c.trace,
                c.isDebuggerContext
            )

        val typeResolutionContext = TypeResolutionContext(
            c.scope, c.trace, /* checkBounds = */ true, /* allowBareTypes = */ true,
            /* isDebuggerContext = */ expression.suppressDiagnosticsInDebugMode() /* TODO: test this */
        )

        val classifier = qualifierResolutionResult.classifierDescriptor
        if (classifier == null) {
            typeResolver.resolveTypeProjections(
                typeResolutionContext,
                ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, expression.text).constructor,
                qualifierResolutionResult.allProjections
            )
            return null
        }

        val possiblyBareType = typeResolver.resolveTypeForClassifier(
            typeResolutionContext, classifier, qualifierResolutionResult, expression, Annotations.EMPTY
        )

        val type = if (possiblyBareType.isBare()) {
            val descriptor = possiblyBareType.bareTypeConstructor.declarationDescriptor as? ClassDescriptor
                ?: error("Only classes can produce bare types: $possiblyBareType")


            val arguments = descriptor.typeConstructor.parameters.map(TypeUtils::makeProjection)
            CangJieTypeFactory.simpleType(
                TypeAttributes.Empty, descriptor.typeConstructor, arguments,
                possiblyBareType.isOptional()
            )
        } else {
            val actualType = possiblyBareType.actualType
            /*            if (doubleColonExpression.hasQuestionMarks) actualType.makeOption() else */actualType
        }

        return DoubleColonLHS.Type(type, possiblyBareType)
    }

    internal fun resolveDoubleColonLHS(
        doubleColonExpression: CjCallableReference,
        c: ExpressionTypingContext
    ): DoubleColonLHS? {
        val resultForExpr = tryResolveLHS(doubleColonExpression, c, this::resolveExpressionOnLHS)
        if (resultForExpr != null) {
            val lhs = resultForExpr.lhs
            // If expression result is an object, we remember this and skip it here, because there are valid situations where
            // another type (representing another classifier) should win
            if (lhs != null) {
                return resultForExpr.commit()
            }
        }

        val (isReservedExpressionSyntax, doubleColonLHS, traceAndCacheFromReservedDoubleColonLHS) =
            resolveReservedExpressionSyntaxOnDoubleColonLHS(doubleColonExpression, c)

        if (isReservedExpressionSyntax) return doubleColonLHS

        val resultForType = tryResolveLHS(doubleColonExpression, c) { expression, context ->
            resolveTypeOnLHS(expression, doubleColonExpression, context)?.let {
                // If lhs is not expression then ExpressionTypingVisitor don't refine it's type and we should do this manually

                DoubleColonLHS.Type(
                    cangjieTypeRefiner.refineType(it.type),
                    cangjieTypeRefiner.refineBareType(it.possiblyBareType)
                )
            }
        }
        if (resultForType != null) {
            val lhs = resultForType.lhs
            if (resultForExpr != null && lhs != null && lhs.type == resultForExpr.lhs?.type) {
                // If we skipped an object expression result before and the type result is the same, this means that
                // there were no other classifier except that object that could win. We prefer to treat the LHS as an expression here,
                // to have a bound callable reference / class literal
                return resultForExpr.commit()
            }
            if (lhs != null) {
                return resultForType.commit()
            }
        }

        if (resultForExpr != null) return resultForExpr.commit()
        if (resultForType != null) return resultForType.commit()

        /*
         * If the LHS could be resolved neither as an expression nor as a type,
         * but it was resolved as expression with reserved syntax like `foo?::bar?::bar`,
         * then we commit the trace of that resolution result.
         */
        traceAndCacheFromReservedDoubleColonLHS?.commit()

        return null
    }

    private fun resolveReservedExpressionOnLHS(
        expression: CjExpression,
        c: ExpressionTypingContext
    ): DoubleColonLHS.Expression? {
//        val doubleColonExpression = expression.parent as? CjCallableReference ?: return null // should assert here?

        if (expression is CjCallExpression && expression.typeArguments.isNotEmpty()) {
            val callee = expression.calleeExpression ?: return null
            val calleeAsDoubleColonLHS = resolveExpressionOnLHS(callee, c) ?: return null

            for (typeArgument in expression.typeArguments) {
                val typeReference = typeArgument.typeReference ?: continue
                typeResolver.resolveType(c.scope, typeReference, c.trace, true)
            }

            return calleeAsDoubleColonLHS
        } /*else if (doubleColonExpression.hasQuestionMarks) {
            return resolveExpressionOnLHS(expression, c)
        }*/ else {
            return null
        }
    }

    private fun resolveReservedExpressionSyntaxOnDoubleColonLHS(
        doubleColonExpression: CjCallableReference,
        c: ExpressionTypingContext
    ):
            ReservedDoubleColonLHSResolutionResult {
        val resultForReservedExpr = tryResolveLHS(
            doubleColonExpression, c,

            this::resolveReservedExpressionOnLHS
        )
        if (resultForReservedExpr != null) {
            val lhs = resultForReservedExpr.lhs
            if (lhs != null) {
//                c.trace.report(RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS.on(resultForReservedExpr.expression))
                return ReservedDoubleColonLHSResolutionResult(
                    true,
                    resultForReservedExpr.commit(),
                    resultForReservedExpr.traceAndCache
                )
            }
        }

        val resultForReservedCallChain = tryResolveLHS(
            doubleColonExpression, c,

            this::resolveReservedCallChainOnLHS
        )
        if (resultForReservedCallChain != null) {
            val lhs = resultForReservedCallChain.lhs
            if (lhs != null) {
//                c.trace.report(RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS.on(resultForReservedCallChain.expression))
                // DO NOT commit trace from resultForReservedCallChain here
                return ReservedDoubleColonLHSResolutionResult(true, null, resultForReservedExpr?.traceAndCache)
            }
        }

        return ReservedDoubleColonLHSResolutionResult(
            false, null, resultForReservedExpr?.traceAndCache ?: resultForReservedCallChain?.traceAndCache
        )
    }

    private fun CjExpression.getQualifierChainParts(): List<CjExpression>? {
        if (this !is CjQualifiedExpression) return listOf(this)

        val result = ArrayDeque<CjExpression>()
        var finger: CjQualifiedExpression = this
        while (true) {
            if (finger.operationSign != CjTokens.DOT) return null

            finger.selectorExpression?.let { result.push(it) }

            val receiver = finger.receiverExpression
            if (receiver is CjQualifiedExpression) {
                finger = receiver
            } else {
                result.push(receiver)
                return result.toList()
            }
        }
    }

    private fun CjExpression?.getQualifiedNameStringPart(): String? =
        when (this) {
            is CjNameReferenceExpression ->
                text

            is CjCallExpression ->
                if (valueArguments.isEmpty() && typeArguments.isNotEmpty())
                    (calleeExpression as? CjNameReferenceExpression)?.text
                else
                    null

            else ->
                null
        }

    private fun CjQualifiedExpression.buildNewExpressionForReservedGenericPropertyCallChainResolution(): CjExpression? {
        val parts = this.getQualifierChainParts()?.map { it.getQualifiedNameStringPart() ?: return null } ?: return null
        val qualifiedExpressionText = parts.joinToString(separator = ".")
        return CjPsiFactory(project, markGenerated = false).createExpression(qualifiedExpressionText)
    }

    private fun resolveReservedCallChainOnLHS(
        expression: CjExpression,
        c: ExpressionTypingContext
    ): DoubleColonLHS.Expression? {
        if (expression !is CjQualifiedExpression) return null

        val newExpression = expression.buildNewExpressionForReservedGenericPropertyCallChainResolution() ?: return null

        val temporaryTraceAndCache =
            TemporaryTraceAndCache.create(c, "resolve reserved generic property call chain in '::' LHS", newExpression)
        val contextForCallChainResolution =
            c.replaceTraceAndCache(temporaryTraceAndCache)
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)

        return resolveExpressionOnLHS(expression, contextForCallChainResolution)
    }

}
