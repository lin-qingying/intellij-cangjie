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

package com.linqingying.cangjie.types.expressions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.IndexNotReadyException
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.PsiDiagnosticUtils.Companion.atLocation
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.calls.components.InferenceSession.Companion.default
import com.linqingying.cangjie.resolve.calls.context.CallPosition
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.LexicalScopeKind
import com.linqingying.cangjie.resolve.scopes.LexicalWritableScope
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.storage.ReenteringLazyValueComputationException
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.DeferredType
import com.linqingying.cangjie.types.ErrorUtils.createErrorType
import com.linqingying.cangjie.types.TypeRefinement
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.match.PatternMatchingTypingVisitor
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo

import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.utils.CangJieExceptionWithAttachments
import com.linqingying.cangjie.utils.CangJieFrontEndException
import com.linqingying.cangjie.utils.PerformanceCounter
import com.linqingying.cangjie.utils.PerformanceCounter.Companion.create
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

    abstract class ExpressionTypingVisitorDispatcher private constructor(
    private val components: ExpressionTypingComponents,
    private val annotationChecker: AnnotationChecker
) : CjVisitor<CangJieTypeInfo, ExpressionTypingContext>(), ExpressionTypingInternals {
    @Suppress("LeakingThis")
    protected val basic: BasicExpressionTypingVisitor = BasicExpressionTypingVisitor(
        this
    )

    @Suppress("LeakingThis")

    protected val functions: FunctionsTypingVisitor = FunctionsTypingVisitor(
        this
    )

    @Suppress("LeakingThis")

    protected val tuples: TuplesTypingVisitor = TuplesTypingVisitor(this)

    @Suppress("LeakingThis")

    protected val controlStructures: ControlStructureTypingVisitor = ControlStructureTypingVisitor(
        this
    )

    @Suppress("LeakingThis")

    protected val patterns: PatternMatchingTypingVisitor = PatternMatchingTypingVisitor(
        this
    )

    override fun defineLocalVariablesFromPattern(
        writableScope: LexicalWritableScope,
        casePattern: CjCasePattern,
        receiver: ReceiverValue,
        initializer: CjExpression?,
        context: ExpressionTypingContext
    ) {
        patterns.defineLocalVariablesFromPattern(writableScope, casePattern, receiver, initializer, context)
    }

    override fun checkLetExpression(pattern: CjLetExpression, context: ExpressionTypingContext) {
        patterns.visitLetExpression(pattern, context)
    }

    protected fun createStatementVisitor(context: ExpressionTypingContext): ExpressionTypingVisitorForStatements {
        return ExpressionTypingVisitorForStatements(
            this,
            ExpressionTypingUtils.newWritableScopeImpl(
                context,
                LexicalScopeKind.CODE_BLOCK,
                components.overloadChecker
            ),
            basic,
            controlStructures, patterns, functions
        )
    }

    override fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,
        dataFlowInfo: DataFlowInfo,
        expectedReturnType: CangJieType?,
        trace: BindingTrace,
        localContext: ExpressionTypingContext?
    ): CangJieTypeInfo {
        val context = ExpressionTypingContext.newContext(
            trace,
            scope, dataFlowInfo, expectedReturnType ?: NO_EXPECTED_TYPE,
            components.languageVersionSettings, components.dataFlowValueFactory,
            localContext?.inferenceSession ?: default
        )
        return getTypeInfo(
            expression, context

        )
    }

    override fun safeGetTypeInfo(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        val typeInfo = getTypeInfo(expression, context)
        if (typeInfo.type != null) {
            return typeInfo
        }
        return typeInfo
            .replaceType(createErrorType(ErrorTypeKind.NO_RECORDED_TYPE, expression.text))
            .replaceDataFlowInfo(context.dataFlowInfo)
    }

    override fun getTypeInfoByEnum(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        var context = context

        val result: CangJieTypeInfo = getTypeInfo(expression, context, ForGetEnum(components, annotationChecker))
        //        annotationChecker.checkExpression(expression, context.trace);
        return result
    }


    override fun getTypeInfoByCaseEnum(
        expression: CjExpression, argument: List<ValueArgument>, context: ExpressionTypingContext,
        isReportError: Boolean
    ): CangJieTypeInfo {
        var context = context


        val result: CangJieTypeInfo = getTypeInfo(
            expression, context, ForCaseEnum(
                components,
                annotationChecker, argument, isReportError
            )
        )
        //        annotationChecker.checkExpression(expression, context.trace);
        return result
    }

    override fun getTypeInfo(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        var context = context
//        if (context.expectedType === EXPRESSION_TYPE) {
//            context = context.replaceExpectedType(components.builtIns.anyType)
//        }
        val result: CangJieTypeInfo = getTypeInfo(expression, context, this)
        annotationChecker.checkExpression(expression, context.trace)
        return result
    }

    protected abstract fun getStatementVisitor(context: ExpressionTypingContext): ExpressionTypingVisitorForStatements

    override fun visitVariable(variable: CjVariable, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitVariable(variable, data)
    }

    @OptIn(TypeRefinement::class)
    private fun getTypeInfoByEnum(
        expression: CjExpression,
        context: ExpressionTypingContext,
        visitor: CjVisitor<CangJieTypeInfo, ExpressionTypingContext>
    ): CangJieTypeInfo {
        ProgressManager.checkCanceled()
        return typeInfoPerfCounter.time {
            try {
                val recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.bindingContext)
                if (recordedTypeInfo != null) {
                    return@time recordedTypeInfo
                }

                context.trace.record(BindingContext.DATA_FLOW_INFO_BEFORE, expression, context.dataFlowInfo)

                var result: CangJieTypeInfo
                try {
                    result = expression.accept(visitor, context)

                    if (context.trace.get<CjExpression, Boolean>(
                            BindingContext.PROCESSED,
                            expression
                        ) === java.lang.Boolean.TRUE
                    ) {
                        val type = context.trace.bindingContext.getType(expression)
                        return@time result.replaceType(type)
                    }
                    //
                    if (result.type is DeferredType) {
                        result = result.replaceType((result.type as DeferredType).delegate)
                    }


                    val refinedType = if (result.type != null)
                        components.cangjieTypeChecker.cangjieTypeRefiner.refineType(result.type!!)
                    else
                        null

                    if (refinedType !== result.type) {
                        result = result.replaceType(refinedType)
                    }

                    context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, result)
                } catch (e: ReenteringLazyValueComputationException) {
//                    context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.onError(expression));
                    result = noTypeInfo(context)
                }
                if (context.isSaveTypeInfo) {
                    context.trace.record(BindingContext.PROCESSED, expression)
                }
                context.trace.recordScope(context.scope, expression)
                //                if (context.isSaveTypeInfo) {
                context.replaceDataFlowInfo(result.dataFlowInfo).recordDataFlowInfo(expression)

                //                }
//                try {
//                    // Here we have to resolve some types, so the following exception is possible
//                    // Example: val a = ::a, fun foo() = ::foo
//                    recordTypeInfo(expression, result);
//                }
//                catch (ReenteringLazyValueComputationException e) {
//                    context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.onError(expression));
//                    return TypeInfoFactoryKt.noTypeInfo(context);
//                }
                return@time result
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: CangJieFrontEndException) {
                throw e
            } catch (e: IndexNotReadyException) {
                throw e
            } catch (e: Throwable) {
                context.trace.report(Errors.EXCEPTION_FROM_ANALYZER.on(expression, e))
                logOrThrowException(expression, e)
                return@time createTypeInfo(
                    createErrorType(ErrorTypeKind.TYPE_FOR_COMPILER_EXCEPTION, e.javaClass.simpleName),
                    context
                )
            }
        }
    }

    @OptIn(TypeRefinement::class)
    private fun getTypeInfo(
        expression: CjExpression,
        context: ExpressionTypingContext,
        visitor: CjVisitor<CangJieTypeInfo, ExpressionTypingContext>
    ): CangJieTypeInfo {
        ProgressManager.checkCanceled()
        return typeInfoPerfCounter.time {
            try {
                val recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.bindingContext)
                if (recordedTypeInfo != null) {
                    return@time recordedTypeInfo
                }

                context.trace.record(BindingContext.DATA_FLOW_INFO_BEFORE, expression, context.dataFlowInfo)

                var result: CangJieTypeInfo
                try {
                    result = expression.accept(visitor, context)

                    if (context.trace.get<CjExpression, Boolean>(
                            BindingContext.PROCESSED,
                            expression
                        ) === java.lang.Boolean.TRUE
                    ) {
                        val type = context.trace.bindingContext.getType(expression)
                        return@time result.replaceType(type)
                    }
                    //
                    if (result.type is DeferredType) {
                        result = result.replaceType((result.type as DeferredType).delegate)
                    }


                    val refinedType = if (result.type != null)
                        components.cangjieTypeChecker.cangjieTypeRefiner.refineType(result.type!!)
                    else
                        null

                    if (refinedType !== result.type) {
                        result = result.replaceType(refinedType)
                    }

                    context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, result)
                } catch (e: ReenteringLazyValueComputationException) {
                    context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.onError(expression))
                    result = noTypeInfo(context)
                }
                if (context.isSaveTypeInfo) {
                    context.trace.record(BindingContext.PROCESSED, expression)
                }
                context.trace.recordScope(context.scope, expression)
                //                if (context.isSaveTypeInfo) {
                context.replaceDataFlowInfo(result.dataFlowInfo).recordDataFlowInfo(expression)

                //                }
//                try {
//                    // Here we have to resolve some types, so the following exception is possible
//                    // Example: val a = ::a, fun foo() = ::foo
//                    recordTypeInfo(expression, result);
//                }
//                catch (ReenteringLazyValueComputationException e) {
//                    context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.onError(expression));
//                    return TypeInfoFactoryKt.noTypeInfo(context);
//                }
                return@time result
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: CangJieFrontEndException) {
                throw e
            } catch (e: IndexNotReadyException) {
                throw e
            } catch (e: Throwable) {
                context.trace.report(Errors.EXCEPTION_FROM_ANALYZER.on(expression, e))
                logOrThrowException(expression, e)
                return@time createTypeInfo(
                    createErrorType(ErrorTypeKind.TYPE_FOR_COMPILER_EXCEPTION, e.javaClass.simpleName),
                    context
                )
            }
        }
    }

    override fun getTypeInfo(
        expression: CjExpression,
        context: ExpressionTypingContext,
        isStatement: Boolean
    ): CangJieTypeInfo {
        var newContext = context
        if (expression.suppressDiagnosticsInDebugMode()) {
            newContext = ExpressionTypingContext.newContext(context, true)
        }
        if (!isStatement) return getTypeInfo(expression, newContext)
        return getTypeInfo(expression, newContext, getStatementVisitor(newContext))
    }

    override fun checkInExpression(
        callElement: CjElement,
        operationSign: CjSimpleNameExpression,
        leftArgument: ValueArgument,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.checkInExpression(callElement, operationSign, leftArgument, right, context)
    }

    override fun checkStatementType(expression: CjExpression, context: ExpressionTypingContext) {
    }

    override fun getComponents(): ExpressionTypingComponents {
        return components
    }

    override fun visitSimpleNameExpression(
        expression: CjSimpleNameExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitSimpleNameExpression(expression, data)
    }


    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitParenthesizedExpression(expression, data)
    }

    override fun visitTupleExpression(expression: CjTupleExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return tuples.visitTupleExpression(expression, data)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun visitConstructorDelegationCall(
        cjConstructorDelegationCall: CjConstructorDelegationCall,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return super.visitConstructorDelegationCall(cjConstructorDelegationCall, data)!!
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun visitLambdaExpression(
        expression: CjLambdaExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo? {
        // Erasing call position to unknown is necessary to prevent wrong call positions when type checking lambda's body
        return functions.visitLambdaExpression(expression, data.replaceCallPosition(CallPosition.Unknown))
    }

    override fun visitSpawnExpression(expression: CjSpawnExpression, data: ExpressionTypingContext): CangJieTypeInfo {
//       if(data.config.getProcessingMode() == ProcessingMode.PARENT) {
//           return super.visitSpawnExpression(expression, data);
//       }else {
        return components.spawnExpressionResolver.resolveSpawnExpression(expression, data)


        //       }
    }

    override fun visitUnsafeExpression(expression: CjUnsafeExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return components.unsafeExpressionResolver.resolveUnsafeExpression(expression, data)
    }

    override fun visitNamedFunction(function: CjNamedFunction, data: ExpressionTypingContext): CangJieTypeInfo {
        return functions.visitNamedFunction(function, data)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun visitThrowExpression(expression: CjThrowExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitThrowExpression(expression, data)
    }

    override fun visitReturnExpression(expression: CjReturnExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitReturnExpression(expression, data)
    }

    override fun visitQuoteExpression(element: CjQuoteExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitQuoteExpression(element, data)
    }

    override fun visitContinueExpression(
        expression: CjContinueExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return controlStructures.visitContinueExpression(expression, data)
    }

    override fun visitIfExpression(expression: CjIfExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitIfExpression(expression, data)
    }

    override fun visitTryExpression(expression: CjTryExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitTryExpression(expression, data)
    }

    override fun visitForExpression(expression: CjForExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitForExpression(expression, data)
    }

    override fun visitWhileExpression(expression: CjWhileExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitWhileExpression(expression, data)
    }

    override fun visitDoWhileExpression(
        expression: CjDoWhileExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return controlStructures.visitDoWhileExpression(expression, data)
    }

    override fun visitBreakExpression(expression: CjBreakExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return controlStructures.visitBreakExpression(expression, data)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun visitIsExpression(expression: CjIsExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return patterns.visitIsExpression(expression, data)
    }

    override fun visitMatchExpression(expression: CjMatchExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return patterns.visitMatchExpression(expression, data)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    override fun visitConstantExpression(
        expression: CjConstantExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitConstantExpression(expression, data)
    }

    override fun visitBinaryWithTypeRHSExpression(
        expression: CjBinaryExpressionWithTypeRHS,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitBinaryWithTypeRHSExpression(expression, data)
    }

    override fun visitThisExpression(expression: CjThisExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitThisExpression(expression, data)
    }

    override fun visitSuperExpression(expression: CjSuperExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitSuperExpression(expression, data)
    }

    override fun visitBlockExpression(expression: CjBlockExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitBlockExpression(expression, data)
    }

    override fun visitQualifiedExpression(
        expression: CjQualifiedExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitQualifiedExpression(expression, data)
    }

    override fun visitCallExpression(expression: CjCallExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitCallExpression(expression, data)
    }


    override fun visitUnaryExpression(expression: CjUnaryExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitUnaryExpression(expression, data)
    }

    override fun visitBinaryExpression(expression: CjBinaryExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitBinaryExpression(expression, data)
    }

    override fun visitRangeExpression(expression: CjRangeExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitRangeExpression(expression, data)
    }

    override fun visitArrayAccessExpression(
        expression: CjArrayAccessExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitArrayAccessExpression(expression, data)
    }


    override fun visitDeclaration(dcl: CjDeclaration, data: ExpressionTypingContext): CangJieTypeInfo {
        return basic.visitDeclaration(dcl, data)
    }

    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        return basic.visitStringTemplateExpression(expression, data)
    }

    override fun visitSynchronizedExpression(
        expression: CjSynchronizedExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
//        return components.syncExpressionResolver.resolveSynchronizedExpression(expression, data)
        return basic.visitSynchronizedExpression(expression, data)
    }

    override fun visitCjElement(element: CjElement, data: ExpressionTypingContext): CangJieTypeInfo {
        return element.accept(basic, data)
    }


    class ForBlock(
        components: ExpressionTypingComponents,
        annotationChecker: AnnotationChecker,
        writableScope: LexicalWritableScope
    ) : ExpressionTypingVisitorDispatcher(components, annotationChecker) {
        private val visitorForBlock =
            ExpressionTypingVisitorForStatements(
                this, writableScope,
                basic,
                controlStructures, patterns, functions
            )

        override fun getStatementVisitor(context: ExpressionTypingContext): ExpressionTypingVisitorForStatements {
            return visitorForBlock
        }
    }

    class ForGetEnum(components: ExpressionTypingComponents, annotationChecker: AnnotationChecker) :
        ExpressionTypingVisitorDispatcher(components, annotationChecker) {
        override fun visitSimpleNameExpression(
            expression: CjSimpleNameExpression,
            data: ExpressionTypingContext
        ): CangJieTypeInfo {
            return basic.visitSimpleNameExpressionByEnum(expression, data)
        }

        override fun visitDotQualifiedExpression(
            expression: CjDotQualifiedExpression,
            data: ExpressionTypingContext
        ): CangJieTypeInfo {
            return basic.visitQualifiedExpressionByEnum(expression, data)
        }

        override fun getStatementVisitor(context: ExpressionTypingContext): ExpressionTypingVisitorForStatements {
            return createStatementVisitor(context)
        }


    }

    class ForCaseEnum(
        components: ExpressionTypingComponents, annotationChecker: AnnotationChecker, var argument: List<ValueArgument>,
        isReportError: Boolean
    ) : ExpressionTypingVisitorDispatcher(components, annotationChecker) {
        var isReportError: Boolean = true

        init {
            this.isReportError = isReportError
        }

        override fun visitSimpleNameExpression(
            expression: CjSimpleNameExpression,
            data: ExpressionTypingContext
        ): CangJieTypeInfo {
            return basic.visitSimpleNameExpressionByCaseEnum(expression, argument, data, isReportError)
        }

        override fun visitDotQualifiedExpression(
            expression: CjDotQualifiedExpression,
            data: ExpressionTypingContext
        ): CangJieTypeInfo {
            return basic.visitQualifiedExpressionByCaseEnum(expression, argument, data)
        }

        override fun getStatementVisitor(context: ExpressionTypingContext): ExpressionTypingVisitorForStatements {
            return createStatementVisitor(context)
        }
    }


    class ForDeclarations(components: ExpressionTypingComponents, annotationChecker: AnnotationChecker) :
        ExpressionTypingVisitorDispatcher(components, annotationChecker) {
        override fun getStatementVisitor(context: ExpressionTypingContext): ExpressionTypingVisitorForStatements {
            return createStatementVisitor(context)
        }
    }

    companion object {
        val typeInfoPerfCounter: PerformanceCounter = create("Type info", true)
        private val LOG = Logger.getInstance(ExpressionTypingVisitor::class.java)
        private fun logOrThrowException(expression: CjExpression, e: Throwable) {
            try {
                val location = try {
                    " in " + atLocation(expression)
                } catch (ex: Exception) {
                    ""
                }
                // This trows AssertionError in CLI and reports the error in the IDE
                LOG.error(
                    CangJieExceptionWithAttachments("Exception while analyzing expression$location", e)
                        .withPsiAttachment("expression.cj", expression)
                )
            } catch (errorFromLogger: AssertionError) {
                // If we ended up here, we are in CLI, and the initial exception needs to be rethrown,
                // simply throwing AssertionError causes its being wrapped over and over again
                throw CangJieFrontEndException(errorFromLogger.message!!, e)
            }
        }
    }
}
