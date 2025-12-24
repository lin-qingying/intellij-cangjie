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

package org.cangnova.cangjie.resolve.controlFlow

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.contracts.description.EventOccurrencesRange
import org.cangnova.cangjie.contracts.description.canBeRevisited
import org.cangnova.cangjie.contracts.description.isDefinitelyVisited
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.CompileTimeConstantUtils
import org.cangnova.cangjie.resolve.calls.model.ArgumentMatch
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.AccessTarget
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.InstructionWithValue
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicKind
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.expressions.match.MatchChecker
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartFMap
import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.diagnostics.infos.errors.BREAK_OR_CONTINUE_IN_WHEN
import org.cangnova.cangjie.diagnostics.infos.errors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
import org.cangnova.cangjie.diagnostics.infos.warnings.ELSE_MISPLACED_IN_MATCH
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import java.util.*

class ControlFlowProcessor(
    private val trace: BindingTrace,
    private val languageVersionSettings: LanguageVersionSettings
) {
    private val builder: ControlFlowBuilder = ControlFlowInstructionsGenerator()
    private fun generateImplicitReturnValue(bodyExpression: CjExpression, subroutine: CjElement) {
        val subroutineDescriptor =
            trace[BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine] as CallableDescriptor? ?: return

        val returnType = subroutineDescriptor.returnType
        if (returnType != null && CangJieBuiltIns.isUnit(returnType) && subroutineDescriptor is AnonymousFunctionDescriptor) return

        val returnValue = builder.getBoundValue(bodyExpression) ?: return

        builder.returnValue(bodyExpression, returnValue, subroutine)
    }

    private fun generate(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange? = null): Pseudocode {
        builder.enterSubroutine(subroutine, eventOccurrencesRange)
        val cfpVisitor = CFPVisitor(builder)
        if (subroutine is CjDeclarationWithBody && subroutine !is CjSecondaryConstructor) {
            val valueParameters = subroutine.valueParameters
            for (valueParameter in valueParameters) {
                cfpVisitor.generateInstructions(valueParameter)
            }
            val bodyExpression = subroutine.bodyExpression
            if (bodyExpression != null) {
                cfpVisitor.generateInstructions(bodyExpression)
                if (!subroutine.hasBlockBody()) {
                    generateImplicitReturnValue(bodyExpression, subroutine)
                }
            }
        } else {
            cfpVisitor.generateInstructions(subroutine)
        }
        return builder.exitSubroutine(subroutine, eventOccurrencesRange)
    }

    fun generatePseudocode(subroutine: CjElement): Pseudocode {
        val pseudocode = generate(subroutine)
        (pseudocode as PseudocodeImpl).postProcess()
        return pseudocode
    }

    private class CatchFinallyLabels(
        val onException: Label?,
        val toFinally: Label?,
        val tryExpression: CjTryExpression?
    )

    private fun processLocalDeclaration(subroutine: CjDeclaration) {
        val afterDeclaration = builder.createUnboundLabel("after local declaration")

        builder.nondeterministicJump(afterDeclaration, subroutine, null)
        generate(subroutine)
        builder.bindLabel(afterDeclaration)
    }

    private inner class CFPVisitor(private val builder: ControlFlowBuilder) : CjVisitorUnit() {

        private val catchFinallyStack = Stack<CatchFinallyLabels>()

        // Some language constructs (e.g. inlined lambdas) should be partially processed before call
        // (to provide argument for call itself), and partially - after (in case of inlined lambdas,
        // their body should be generated after call). To do so, we store deferred generators, which
        // will be called after call instruction is emitted.
        // Stack is necessary to store generators across nested calls
        private val deferredGeneratorsStack = Stack<MutableList<DeferredGenerator>>()

        private val conditionVisitor = object : CjVisitorUnit() {

            private fun getSubjectExpression(condition: CjCasePatternElement): CjExpression? =
                condition.getStrictParentOfType<CjMatchExpression>()?.subjectExpression

            //
//            override fun visitMatchConditionInRange(condition: CjMatchConditionInRange) {
//                if (!generateCall(condition.operationReference)) {
//                    val rangeExpression = condition.rangeExpression
//                    generateInstructions(rangeExpression)
//                    createNonSyntheticValue(condition, MagicKind.UNRESOLVED_CALL, rangeExpression)
//                }
//            }
//
//            override fun visitMatchConditionIsPattern(condition: CjMatchConditionIsPattern) {
//                mark(condition)
//                createNonSyntheticValue(condition, MagicKind.IS, getSubjectExpression(condition))
//            }
//
            override fun visitMatchConditionWithExpression(element: CjMatchConditionWithExpression) {
                mark(element)

                val expression = element.expression
                generateInstructions(expression)

                val subjectExpression = getSubjectExpression(element)
                if (subjectExpression != null) {
                    // todo: this can be replaced by equals() invocation (match corresponding resolved call is recorded)
                    createNonSyntheticValue(element, MagicKind.EQUALS_IN_MATCH_CONDITION, subjectExpression, expression)
                } else {
                    copyValue(expression, element)
                }
            }

            override fun visitPatternByConstant(element: CjConstantPattern) {
                mark(element)
//
                val expression = element.expression
                generateInstructions(expression)

                val subjectExpression = getSubjectExpression(element)
                if (subjectExpression != null) {
                    // todo: this can be replaced by equals() invocation (match corresponding resolved call is recorded)
                    createNonSyntheticValue(element, MagicKind.EQUALS_IN_MATCH_CONDITION, subjectExpression, expression)
                } else {
                    copyValue(expression, element)
                }
            }

            override fun visitPatternByBinding(element: CjBindingPattern) {

            }

            override fun visitPatternByWildcard(element: CjWildcardPattern) {

            }

            override fun visitPatternByTuple(element: CjTuplePattern) {

            }

            override fun visitPatternByEnum(element: CjEnumPattern) {

            }

            override fun visitPatternByType(element: CjTypePattern) {
                mark(element)
                createNonSyntheticValue(element, MagicKind.IS, getSubjectExpression(element))
            }

            override fun visitCjElement(element: CjElement) {
                throw UnsupportedOperationException("[ControlFlowProcessor] $element")
            }
        }

        private fun mark(element: CjElement) {
            builder.mark(element)
        }

        fun generateInstructions(element: CjElement?) {
            if (element == null) return
            element.accept(this)
            checkNothingType(element)
        }

        private fun checkNothingType(element: CjElement) {
            if (element !is CjExpression) return

            val expression = CjPsiUtil.deparenthesize(element) ?: return

            if (expression is CjStatementExpression || expression is CjTryExpression
                || expression is CjIfExpression || expression is CjMatchExpression
            ) {
                return
            }

            val type = trace.bindingContext.getType(expression)
            if (type != null && CangJieBuiltIns.isNothing(type)) {
                builder.jumpToError(expression)
            }
        }

        private fun createSyntheticValue(
            instructionElement: CjElement,
            kind: MagicKind,
            vararg from: CjElement
        ): PseudoValue =
            builder.magic(instructionElement, null, elementsToValues(from.asList()), kind).outputValue

        private fun createNonSyntheticValue(to: CjElement, from: List<CjElement?>, kind: MagicKind): PseudoValue =
            builder.magic(to, to, elementsToValues(from), kind).outputValue

        private fun createNonSyntheticValue(to: CjElement, kind: MagicKind, vararg from: CjElement?): PseudoValue =
            createNonSyntheticValue(to, from.asList(), kind)

        private fun mergeValues(from: List<CjExpression>, to: CjExpression) {
            builder.merge(to, elementsToValues(from))
        }

        private fun copyValue(from: CjElement?, to: CjElement) {
            getBoundOrUnreachableValue(from)?.let { builder.bindValue(it, to) }
        }

        private fun getBoundOrUnreachableValue(element: CjElement?): PseudoValue? {
            if (element == null) return null

            val value = builder.getBoundValue(element)
            return if (value != null || element is CjDeclaration) value else builder.newValue(element)
        }

        private fun elementsToValues(from: List<CjElement?>): List<PseudoValue> =
            from.mapNotNull { element -> getBoundOrUnreachableValue(element) }

        private fun generateInitializer(declaration: CjDeclaration, initValue: PseudoValue) {
            builder.write(declaration, declaration, initValue, getDeclarationAccessTarget(declaration), emptyMap())
        }

        private fun getResolvedCallAccessTarget(element: CjElement?): AccessTarget =
            element.getResolvedCall(trace.bindingContext)?.let { AccessTarget.Call(it) }
                ?: AccessTarget.BlackBox

        private fun getDeclarationAccessTarget(element: CjElement): AccessTarget {
            val descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
            return if (descriptor is VariableDescriptor)
                AccessTarget.Declaration(descriptor)
            else
                AccessTarget.BlackBox
        }

        override fun visitParenthesizedExpression(expression: CjParenthesizedExpression) {
            mark(expression)
            val innerExpression = expression.expression
            if (innerExpression != null) {
                generateInstructions(innerExpression)
                copyValue(innerExpression, expression)
            }
        }

//        override fun visitAnnotatedExpression(expression: CjAnnotatedExpression) {
//            val baseExpression = expression.baseExpression
//            if (baseExpression != null) {
//                generateInstructions(baseExpression)
//                copyValue(baseExpression, expression)
//            }
//        }

        override fun visitThisExpression(expression: CjThisExpression) {
            val resolvedCall = expression.getResolvedCall(trace.bindingContext)
            if (resolvedCall == null) {
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL)
                return
            }

            val resultingDescriptor = resolvedCall.resultingDescriptor
            if (resultingDescriptor is ReceiverParameterDescriptor) {
                builder.readVariable(expression, resolvedCall, getReceiverValues(resolvedCall))
            }

            copyValue(expression, expression.instanceReference)
        }

        override fun visitConstantExpression(expression: CjConstantExpression) {
            val constant = ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)
            builder.loadConstant(expression, constant)
        }

        override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
            val resolvedCall = expression.getResolvedCall(trace.bindingContext)
            if (resolvedCall is VariableAsFunctionResolvedCall) {
                generateCall(resolvedCall.variableCall)
            } else {
                if (resolvedCall == null) {
                    val qualifierExpression = expression
//                        when (languageVersionSettings.supportsFeature(ProhibitQualifiedAccessToUninitializedEnumEntry)) {
//                            true -> expression.getTopmostParentQualifiedExpressionForSelector() ?: expression
//                            false -> expression
//                        }
                    val qualifier = trace.bindingContext[BindingContext.QUALIFIER, qualifierExpression]
                    if (qualifier != null && generateQualifier(expression, qualifier)) return
                }
                if (!generateCall(expression) && expression.parent !is CjCallExpression) {
                    createNonSyntheticValue(
                        expression,
                        MagicKind.UNRESOLVED_CALL,
                        generateAndGetReceiverIfAny(expression)
                    )
                }
            }
        }

//        override fun visitLabeledExpression(expression: CjLabeledExpression) {
//            mark(expression)
//            val baseExpression = expression.baseExpression
//            if (baseExpression != null) {
//                generateInstructions(baseExpression)
//                copyValue(baseExpression, expression)
//            }
//
//            val labelNameExpression = expression.getTargetLabel()
//            if (labelNameExpression != null) {
//                val deparenthesizedBaseExpression = CjPsiUtil.deparenthesize(expression)
//                if (deparenthesizedBaseExpression !is CjLambdaExpression &&
//                    deparenthesizedBaseExpression !is CjLoopExpression &&
//                    deparenthesizedBaseExpression !is CjNamedFunction
//                ) {
//                    trace.report(REDUNDANT_LABEL_WARNING.on(labelNameExpression))
//                }
//            }
//        }

        override fun visitBinaryExpression(expression: CjBinaryExpression) {
            val operationReference = expression.operationReference
            val operationType = operationReference.referencedNameElementType

            val left = expression.left
            val right = expression.right
            if (operationType === ANDAND || operationType === OROR) {
                generateBooleanOperation(expression)
            } else if (operationType === EQ) {
                visitAssignment(left, getDeferredValue(right), expression)
            } else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                val resolvedCall = expression.getResolvedCall(trace.bindingContext)
                if (resolvedCall != null) {
                    val rhsValue = generateCall(resolvedCall).outputValue
                    val assignMethodName =
                        OperatorConventions.getNameForOperationSymbol(expression.operationToken as CjToken)
                    if (resolvedCall.resultingDescriptor.name != assignMethodName) {
                        /* At this point assignment of the form a += b actually means a = a + b
                         * So we first generate call of "+" operation and then use its output pseudo-value
                         * as a right-hand side when generating assignment call
                         */
                        visitAssignment(left, getValueAsFunction(rhsValue), expression)
                    }
                } else {
                    generateBothArgumentsAndMark(expression)
                }
            } else if (operationType === COALESCING) {
                generateInstructions(left)
                mark(expression)
                val afterElvis = builder.createUnboundLabel("after elvis operator")
                builder.jumpOnTrue(afterElvis, expression, builder.getBoundValue(left))
                generateInstructions(right)
                builder.bindLabel(afterElvis)
                mergeValues(listOfNotNull(left, right), expression)
//                if (right != null && languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNonExhaustiveIfInRhsOfElvis)) {
//                    right.recordUsedAsExpression(trace, true)
//                }
            } else {
                if (!generateCall(expression)) {
                    generateBothArgumentsAndMark(expression)
                }
            }
        }

        private fun generateBooleanOperation(expression: CjBinaryExpression) {
            val operationType = expression.operationReference.referencedNameElementType
            val left = expression.left
            val right = expression.right

            val resultLabel = builder.createUnboundLabel("result of boolean operation")
            generateInstructions(left)
            if (operationType === ANDAND) {
                builder.jumpOnFalse(resultLabel, expression, builder.getBoundValue(left))
            } else {
                builder.jumpOnTrue(resultLabel, expression, builder.getBoundValue(left))
            }
            generateInstructions(right)
            builder.bindLabel(resultLabel)
            val operation: ControlFlowBuilder.PredefinedOperation =
                if (operationType === ANDAND) ControlFlowBuilder.PredefinedOperation.AND else ControlFlowBuilder.PredefinedOperation.OR
            builder.predefinedOperation(expression, operation, elementsToValues(listOfNotNull(left, right)))
        }

        private fun getValueAsFunction(value: PseudoValue?): () -> PseudoValue? = { value }

        private fun getDeferredValue(expression: CjExpression?): () -> PseudoValue? = {
            generateInstructions(expression)
            getBoundOrUnreachableValue(expression)
        }

        private fun generateBothArgumentsAndMark(expression: CjBinaryExpression) {
            val left = CjPsiUtil.deparenthesize(expression.left)
            if (left != null) {
                generateInstructions(left)
            }
            val right = expression.right
            if (right != null) {
                generateInstructions(right)
            }
            mark(expression)
            createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, left, right)
        }

        private fun visitAssignment(
            lhs: CjExpression?,
            rhsDeferredValue: () -> PseudoValue?,
            parentExpression: CjExpression
        ) {
            val left = CjPsiUtil.deparenthesize(lhs)
            if (left == null) {
                val arguments = rhsDeferredValue()?.let { listOf(it) } ?: emptyList()
                builder.magic(parentExpression, parentExpression, arguments, MagicKind.UNSUPPORTED_ELEMENT)
                return
            }

            if (left is CjArrayAccessExpression) {
                generateArrayAssignment(left, rhsDeferredValue, parentExpression)
                return
            }

            var receiverValues: Map<PseudoValue, ReceiverValue> = SmartFMap.emptyMap()
            var accessTarget: AccessTarget = AccessTarget.BlackBox
            if (left is CjSimpleNameExpression || left is CjQualifiedExpression) {
                accessTarget = getResolvedCallAccessTarget(left.getQualifiedElementSelector())
                if (accessTarget is AccessTarget.Call) {
                    receiverValues = getReceiverValues(accessTarget.resolvedCall)
                }
            } else if (left is CjProperty || left is CjVariable<*>) {
                accessTarget = getDeclarationAccessTarget(left)
            }

            if (accessTarget === AccessTarget.BlackBox && left !is CjProperty && left !is CjVariable<*>) {
                generateInstructions(left)
                createSyntheticValue(left, MagicKind.VALUE_CONSUMER, left)
            }

            val rightValue = rhsDeferredValue.invoke()
            val rValue = rightValue ?: createSyntheticValue(parentExpression, MagicKind.UNRECOGNIZED_WRITE_RHS)
            builder.write(parentExpression, left, rValue, accessTarget, receiverValues)
        }

        private fun generateArrayAssignment(
            lhs: CjArrayAccessExpression,
            rhsDeferredValue: () -> PseudoValue?,
            parentExpression: CjExpression
        ) {
            val setResolvedCall = trace.get(BindingContext.INDEXED_LVALUE_SET, lhs)

            if (setResolvedCall == null) {
                generateArrayAccess(lhs, null)

                val arguments = listOfNotNull(getBoundOrUnreachableValue(lhs), rhsDeferredValue.invoke())
                builder.magic(parentExpression, parentExpression, arguments, MagicKind.UNRESOLVED_CALL)

                return
            }

            // In case of simple ('=') array assignment mark instruction is not generated yet, so we put it before generating "set" call
            if ((parentExpression as CjOperationExpression).operationReference.referencedNameElementType === EQ) {
                mark(lhs)
            }

            generateInstructions(lhs.arrayExpression)

            val receiverValues = getReceiverValues(setResolvedCall)
            val argumentValues = getArraySetterArguments(rhsDeferredValue, setResolvedCall)

            builder.call(parentExpression, setResolvedCall, receiverValues, argumentValues)
        }

        /* We assume that assignment right-hand side corresponds to the last argument of the call
        *  So receiver instructions/pseudo-values are generated for all arguments except the last one which is replaced
        *  by pre-generated pseudo-value
        *  For example, assignment a[1, 2] += 3 means a.set(1, 2, a.get(1) + 3), so in order to generate "set" call
        *  we first generate instructions for 1 and 2 whereas 3 is replaced by pseudo-value corresponding to "a.get(1) + 3"
        */
        private fun getArraySetterArguments(
            rhsDeferredValue: () -> PseudoValue?,
            setResolvedCall: ResolvedCall<FunctionDescriptor>
        ): SmartFMap<PseudoValue, ValueParameterDescriptor> {
            val valueArguments = setResolvedCall.resultingDescriptor.valueParameters.flatMapTo(
                ArrayList()
            ) { descriptor -> setResolvedCall.valueArguments[descriptor]?.arguments ?: emptyList() }

            val rhsArgument = valueArguments.lastOrNull()
            var argumentValues = SmartFMap.emptyMap<PseudoValue, ValueParameterDescriptor>()
            for (valueArgument in valueArguments) {
                val argumentMapping = setResolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: continue
                val parameterDescriptor = argumentMapping.valueParameter
                if (valueArgument !== rhsArgument) {
                    argumentValues = generateValueArgument(valueArgument, parameterDescriptor, argumentValues)
                } else {
                    val rhsValue = rhsDeferredValue.invoke()
                    if (rhsValue != null) {
                        argumentValues = argumentValues.plus(rhsValue, parameterDescriptor)
                    }
                }
            }
            return argumentValues
        }

        private fun generateArrayAccess(
            arrayAccessExpression: CjArrayAccessExpression,
            resolvedCall: ResolvedCall<*>?
        ) {
            if (builder.getBoundValue(arrayAccessExpression) != null) return
            mark(arrayAccessExpression)
            if (!checkAndGenerateCall(resolvedCall)) {
                generateArrayAccessWithoutCall(arrayAccessExpression)
            }
        }

        private fun generateArrayAccessWithoutCall(arrayAccessExpression: CjArrayAccessExpression) {
            createNonSyntheticValue(
                arrayAccessExpression,
                generateArrayAccessArguments(arrayAccessExpression),
                MagicKind.UNRESOLVED_CALL
            )
        }

        private fun generateArrayAccessArguments(arrayAccessExpression: CjArrayAccessExpression): List<CjExpression> {
            val inputExpressions = ArrayList<CjExpression>()

            val arrayExpression = arrayAccessExpression.arrayExpression
            if (arrayExpression != null) {
                inputExpressions.add(arrayExpression)
            }
            generateInstructions(arrayExpression)

            for (index in arrayAccessExpression.indexExpressions) {
                generateInstructions(index)
                inputExpressions.add(index)
            }

            return inputExpressions
        }

        override fun visitUnaryExpression(expression: CjUnaryExpression) {
            val operationSign = expression.operationReference
            val operationType = operationSign.referencedNameElementType
            val baseExpression = expression.baseExpression ?: return


            val incrementOrDecrement = isIncrementOrDecrement(operationType)
            val resolvedCall = expression.getResolvedCall(trace.bindingContext)

            val rhsValue: PseudoValue? = if (resolvedCall != null) {
                generateCall(resolvedCall).outputValue
            } else {
                generateInstructions(baseExpression)
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, baseExpression)
            }

            if (incrementOrDecrement && resolvedCall != null) {
                visitAssignment(baseExpression, getValueAsFunction(rhsValue), expression)
                if (expression is CjPostfixExpression) {
                    copyValue(baseExpression, expression)
                }
            }
        }

        private fun isIncrementOrDecrement(operationType: IElementType): Boolean =
            operationType === PLUSPLUS || operationType === MINUSMINUS

        override fun visitIfExpression(expression: CjIfExpression) {
            mark(expression)
            val branches = ArrayList<CjExpression>(2)
            val condition = expression.condition
            generateInstructions(condition)
            val elseLabel = builder.createUnboundLabel("else branch")
            builder.jumpOnFalse(elseLabel, expression, builder.getBoundValue(condition))
            val thenBranch = expression.then
            if (thenBranch != null) {
                branches.add(thenBranch)
                generateInstructions(thenBranch)
            } else {
                builder.loadUnit(expression)
            }
            val resultLabel = builder.createUnboundLabel("'if' expression result")
            builder.jump(resultLabel, expression)
            builder.bindLabel(elseLabel)
            val elseBranch = expression.`else`
            if (elseBranch != null) {
                branches.add(elseBranch)
                generateInstructions(elseBranch)
            } else {
                builder.loadUnit(expression)
            }
            builder.bindLabel(resultLabel)
            mergeValues(branches, expression)
        }

        private inner class FinallyBlockGenerator(private val finallyBlock: CjFinallySection?) {
            private var startFinally: Label? = null
            private var finishFinally: Label? = null

            fun generate() {
                val finalExpression = finallyBlock?.finalExpression ?: return
                catchFinallyStack.push(CatchFinallyLabels(null, null, null))
                startFinally?.let {
                    assert(finishFinally != null) { "startFinally label is set to $startFinally but finishFinally label is not set" }
                    builder.repeatPseudocode(it, finishFinally!!)
                    catchFinallyStack.pop()
                    return
                }
                builder.createUnboundLabel("start finally").let {
                    startFinally = it
                    builder.bindLabel(it)
                }
                generateInstructions(finalExpression)
                builder.createUnboundLabel("finish finally").let {
                    finishFinally = it
                    builder.bindLabel(it)
                }
                catchFinallyStack.pop()
            }
        }

        override fun visitTryExpression(expression: CjTryExpression) {
            mark(expression)

            val finallyBlock = expression.finallyBlock
            val finallyBlockGenerator = FinallyBlockGenerator(finallyBlock)
            val hasFinally = finallyBlock != null
            if (hasFinally) {
                builder.enterTryFinally(object : GenerationTrigger {
                    private var working = false

                    override fun generate() {
                        // This checks are needed for the case of having e.g. return inside finally: 'try {return} finally{return}'
                        if (working) return
                        working = true
                        finallyBlockGenerator.generate()
                        working = false
                    }
                })
            }

            val onExceptionToFinallyBlock = generateTryAndCatches(expression)

            if (hasFinally) {
                assert(onExceptionToFinallyBlock != null) { "No finally label generated: " + expression.text }

                builder.exitTryFinally()

                val skipFinallyToErrorBlock = builder.createUnboundLabel("skipFinallyToErrorBlock")
                builder.jump(skipFinallyToErrorBlock, expression)
                builder.bindLabel(onExceptionToFinallyBlock!!)
                finallyBlockGenerator.generate()
                builder.jumpToError(expression)
                builder.bindLabel(skipFinallyToErrorBlock)

                finallyBlockGenerator.generate()
            }

            val branches = ArrayList<CjExpression>()
            branches.add(expression.tryBlock)
            for (catchClause in expression.catchClauses) {
                catchClause.catchBody?.let { branches.add(it) }
            }
            mergeValues(branches, expression)
        }

        // Returns label for 'finally' block
        private fun generateTryAndCatches(expression: CjTryExpression): Label? {
            val catchClauses = expression.catchClauses
            val hasCatches = catchClauses.isNotEmpty()

            var onException: Label? = null
            if (hasCatches) {
                onException = builder.createUnboundLabel("onException")
                builder.nondeterministicJump(onException, expression, null)
            }

            var onExceptionToFinallyBlock: Label? = null
            if (expression.finallyBlock != null) {
                onExceptionToFinallyBlock = builder.createUnboundLabel("onExceptionToFinallyBlock")
                builder.nondeterministicJump(onExceptionToFinallyBlock, expression, null)
            }

            val tryBlock = expression.tryBlock
            catchFinallyStack.push(CatchFinallyLabels(onException, onExceptionToFinallyBlock, expression))
            generateInstructions(tryBlock)
            generateJumpsToCatchAndFinally()
            catchFinallyStack.pop()

            if (hasCatches && onException != null) {
                val afterCatches = builder.createUnboundLabel("afterCatches")
                builder.jump(afterCatches, expression)

                builder.bindLabel(onException)
                val catchLabels = LinkedList<Label>()
                val catchClausesSize = catchClauses.size
                for (i in 0 until catchClausesSize - 1) {
                    catchLabels.add(builder.createUnboundLabel("catch $i"))
                }
                if (!catchLabels.isEmpty()) {
                    builder.nondeterministicJump(catchLabels, expression)
                }
                var isFirst = true
                for (catchClause in catchClauses) {
                    builder.enterBlockScope(catchClause)
                    if (!isFirst) {
                        builder.bindLabel(catchLabels.remove())
                    } else {
                        isFirst = false
                    }
                    val catchParameter = catchClause.catchParameter
                    if (catchParameter != null) {
                        builder.declareParameter(catchParameter)
                        generateInitializer(
                            catchParameter,
                            createSyntheticValue(catchParameter, MagicKind.FAKE_INITIALIZER)
                        )
                    }
                    generateInstructions(catchClause.catchBody)
                    builder.jump(afterCatches, expression)
                    builder.exitBlockScope(catchClause)
                }

                builder.bindLabel(afterCatches)
            }

            return onExceptionToFinallyBlock
        }

        override fun visitWhileExpression(expression: CjWhileExpression) {
            val loopInfo = builder.enterLoop(expression)

            builder.bindLabel(loopInfo.conditionEntryPoint)
            val condition = expression.condition
            generateInstructions(condition)
            mark(expression)
            if (!CompileTimeConstantUtils.canBeReducedToBooleanConstant(condition, trace.bindingContext, true)) {
                builder.jumpOnFalse(loopInfo.exitPoint, expression, builder.getBoundValue(condition))
            } else {
                assert(condition != null) { "Invalid while condition: " + expression.text }
                createSyntheticValue(condition!!, MagicKind.VALUE_CONSUMER, condition)
            }

            builder.enterLoopBody(expression)
            generateInstructions(expression.body)
            builder.jump(loopInfo.entryPoint, expression)
            builder.exitLoopBody(expression)
            builder.bindLabel(loopInfo.exitPoint)
            builder.loadUnit(expression)
        }

        override fun visitDoWhileExpression(expression: CjDoWhileExpression) {
            builder.enterBlockScope(expression)
            mark(expression)
            val loopInfo = builder.enterLoop(expression)

            builder.enterLoopBody(expression)
            generateInstructions(expression.body)
            builder.exitLoopBody(expression)
            builder.bindLabel(loopInfo.conditionEntryPoint)
            val condition = expression.condition
            generateInstructions(condition)
            builder.exitBlockScope(expression)
            if (!CompileTimeConstantUtils.canBeReducedToBooleanConstant(condition, trace.bindingContext, true)) {
                builder.jumpOnTrue(loopInfo.entryPoint, expression, builder.getBoundValue(expression.condition))
            } else {
                assert(condition != null) { "Invalid do / while condition: " + expression.text }
                createSyntheticValue(condition!!, MagicKind.VALUE_CONSUMER, condition)
                builder.jump(loopInfo.entryPoint, expression)
            }
            builder.bindLabel(loopInfo.exitPoint)
            builder.loadUnit(expression)
        }

        override fun visitForExpression(expression: CjForExpression) {
            builder.enterBlockScope(expression)

            val loopRange = expression.loopRange
            generateInstructions(loopRange)
            generateLoopConventionCall(loopRange, BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL)
            declareLoopParameter(expression)

            // TODO : primitive cases
            val loopInfo = builder.enterLoop(expression)

            builder.bindLabel(loopInfo.conditionEntryPoint)
            generateLoopConventionCall(loopRange, BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL)
            builder.nondeterministicJump(loopInfo.exitPoint, expression, null)
            generateLoopConventionCall(loopRange, BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL)

            writeLoopParameterAssignment(expression)

            mark(expression)
            builder.enterLoopBody(expression)
            generateInstructions(expression.body)
            builder.jump(loopInfo.entryPoint, expression)

            builder.exitLoopBody(expression)
            builder.bindLabel(loopInfo.exitPoint)
            builder.loadUnit(expression)
            builder.exitBlockScope(expression)
        }

        private fun generateLoopConventionCall(
            loopRange: CjExpression?,
            callSlice: ReadOnlySlice<CjExpression, ResolvedCall<FunctionDescriptor>>
        ) {
            if (loopRange == null) return
            val resolvedCall = trace.bindingContext[callSlice, loopRange] ?: return
            generateCall(resolvedCall)
        }

        private fun declareLoopParameter(expression: CjForExpression) {
            val loopParameter = expression.loopParameter
            if (loopParameter != null) {
                builder.declareParameter(loopParameter)
            }
        }

        private fun writeLoopParameterAssignment(expression: CjForExpression) {
            val loopParameter = expression.loopParameter
            val loopRange = expression.loopRange

            val value = builder.magic(
                loopRange ?: expression,
                null,
                ContainerUtil.createMaybeSingletonList(builder.getBoundValue(loopRange)),
                MagicKind.LOOP_RANGE_ITERATION
            ).outputValue

            if (loopParameter != null) {
                generateInitializer(loopParameter, value)
                }
            }


        override fun visitBreakExpression(expression: CjBreakExpression) {
            val loop = getCorrespondingLoop(expression)
            if (loop != null) {
                if (jumpCrossesTryCatchBoundary(expression, loop)) {
                    generateJumpsToCatchAndFinally()
                }
                if (jumpDoesNotCrossFunctionBoundary(expression, loop)) {
                    builder.getLoopExitPoint(loop)?.let { builder.jump(it, expression) }
                }
            }
        }

        override fun visitContinueExpression(expression: CjContinueExpression) {
            val loop = getCorrespondingLoop(expression)
            if (loop != null) {
                if (jumpCrossesTryCatchBoundary(expression, loop)) {
                    generateJumpsToCatchAndFinally()
                }
                if (jumpDoesNotCrossFunctionBoundary(expression, loop)) {
                    builder.getLoopConditionEntryPoint(loop)?.let { builder.jump(it, expression) }
                }
            }
        }

        private fun getNearestLoopExpression(expression: CjExpression) =
            expression.getStrictParentOfType<CjLoopExpression>()

        private fun getCorrespondingLoopWithoutLabel(expression: CjExpression): CjLoopExpression? {
            val parentLoop = getNearestLoopExpression(expression) ?: return null
            val parentBody = parentLoop.body
            return if (parentBody != null && parentBody.textRange.contains(expression.textRange)) {
                parentLoop
            } else {
                getNearestLoopExpression(parentLoop)
            }
        }

        private fun getCorrespondingLoop(expression: CjExpressionWithLabel): CjLoopExpression? {

            val loop: CjLoopExpression? = getCorrespondingLoopWithoutLabel(expression)
            if (loop == null) {
                trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression))
            } else {
                val matchExpression = PsiTreeUtil.getParentOfType(
                    expression, CjMatchExpression::class.java, true,
                    CjLoopExpression::class.java
                )
                if (matchExpression != null) {
                    trace.report(BREAK_OR_CONTINUE_IN_WHEN.on(expression))
                }

            }

            loop?.body?.let {
                if (!it.textRange.contains(expression.textRange)) {
                    trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression))
                    return null
                }
            }
            return loop
        }

        private fun returnCrossesTryCatchBoundary(returnExpression: CjReturnExpression): Boolean {
            val targetLabel = returnExpression.getTargetLabel() ?: return true
            val labeledElement = trace.get(BindingContext.LABEL_TARGET, targetLabel) ?: return true
            return jumpCrossesTryCatchBoundary(returnExpression, labeledElement)
        }

        private fun jumpCrossesTryCatchBoundary(
            jumpExpression: CjExpressionWithLabel,
            jumpTarget: PsiElement
        ): Boolean {
            var current = jumpExpression.parent
            while (true) {
                when (current) {
                    jumpTarget -> return false
                    is CjTryExpression -> return true
                    else -> current = current?.parent
                }
            }
        }

        private fun jumpDoesNotCrossFunctionBoundary(
            jumpExpression: CjExpressionWithLabel,
            jumpTarget: CjLoopExpression
        ): Boolean {
            return true
//            val bindingContext = trace.bindingContext
//            val skipInlineFunctions =  languageVersionSettings.supportsFeature(BreakContinueInInlineLambdas)
//            val labelExprEnclosingFunc =
//                getEnclosingFunctionDescriptor(bindingContext, jumpExpression, skipInlineFunctions)
//            val labelTargetEnclosingFunc =
//                getEnclosingFunctionDescriptor(bindingContext, jumpTarget, skipInlineFunctions)
//            return if (labelExprEnclosingFunc !== labelTargetEnclosingFunc) {
//                // Check to report only once
//                if (builder.getLoopExitPoint(jumpTarget) != null ||
//                    // Local class secondary constructors are handled differently
//                    // They are the only local class element NOT included in owner pseudocode
//                    // See generateInitializersForClassOrObject && generateDeclarationForLocalClassOrObjectIfNeeded
//                    labelExprEnclosingFunc is ConstructorDescriptor && !labelExprEnclosingFunc.isPrimary
//                ) {
//                    val dependsOnInlineLambdas = !skipInlineFunctions &&
//                            getEnclosingFunctionDescriptor(
//                                bindingContext,
//                                jumpExpression,
//                                true
//                            ) == getEnclosingFunctionDescriptor(bindingContext, jumpTarget, true)
//                    if (dependsOnInlineLambdas) {
//                        trace.report(
//                            UNSUPPORTED_FEATURE.on(
//                                jumpExpression,
//                                BreakContinueInInlineLambdas to languageVersionSettings
//                            )
//                        )
//                    } else {
//                        trace.report(BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY.on(jumpExpression))
//                    }
//                }
//                false
//            } else {
//                true
//            }
        }

        override fun visitReturnExpression(expression: CjReturnExpression) {
            if (returnCrossesTryCatchBoundary(expression)) {
                generateJumpsToCatchAndFinally()
            }
            val returnedExpression = expression.returnedExpression
            if (returnedExpression != null) {
                generateInstructions(returnedExpression)
            }
            val labelElement = expression.getTargetLabel()
            val subroutine: CjElement?
            val labelName = expression.getLabelName()
            subroutine = if (labelElement != null && labelName != null) {
                trace.get(BindingContext.LABEL_TARGET, labelElement)?.let { labeledElement ->
                    val labeledCjElement = labeledElement as CjElement
//                    checkReturnLabelTarget(expression, labeledCjElement)
                    labeledCjElement
                }
            } else {
                builder.returnSubroutine
                // TODO : a context check
            }

            if (subroutine is CjFunction || subroutine is CjPropertyAccessor) {
                val returnValue = if (returnedExpression != null) builder.getBoundValue(returnedExpression) else null
                if (returnValue == null) {
                    builder.returnNoValue(expression, subroutine)
                } else {
                    builder.returnValue(expression, returnValue, subroutine)
                }
            } else {
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, returnedExpression)
            }
        }


        override fun visitParameter(parameter: CjParameter) {
            builder.declareParameter(parameter)
            val defaultValue = parameter.defaultValue
            if (defaultValue != null) {
                val skipDefaultValue =
                    builder.createUnboundLabel("after default value for parameter ${parameter.name ?: "<anonymous>"}")
                builder.nondeterministicJump(skipDefaultValue, defaultValue, null)
                generateInstructions(defaultValue)
                builder.bindLabel(skipDefaultValue)
            }
            generateInitializer(parameter, computePseudoValueForParameter(parameter))
        }

        private fun computePseudoValueForParameter(parameter: CjParameter): PseudoValue {
            val syntheticValue = createSyntheticValue(parameter, MagicKind.FAKE_INITIALIZER)
            val defaultValue = builder.getBoundValue(parameter.defaultValue) ?: return syntheticValue
            return builder.merge(parameter, arrayListOf(defaultValue, syntheticValue)).outputValue
        }

        override fun visitBlockExpression(expression: CjBlockExpression) {
            val declareBlockScope = !isBlockInDoWhile(expression)
            if (declareBlockScope) {
                builder.enterBlockScope(expression)
            }
            mark(expression)
            val statements = expression.statements
            for (statement in statements) {
                val afterClassLabel =
                    (statement as? CjTypeStatement)?.let { builder.createUnboundLabel("after local class") }
                if (afterClassLabel != null) {
                    builder.nondeterministicJump(afterClassLabel, statement, null)
                }
                generateInstructions(statement)
                if (afterClassLabel != null) {
                    builder.bindLabel(afterClassLabel)
                }
            }
            if (statements.isEmpty()) {
                builder.loadUnit(expression)
            } else {
                copyValue(statements.lastOrNull(), expression)
            }
            if (declareBlockScope) {
                builder.exitBlockScope(expression)
            }
        }

        private fun isBlockInDoWhile(expression: CjBlockExpression): Boolean {
            val parent = expression.parent
            return parent.parent is CjDoWhileExpression
        }

        private fun visitFunction(function: CjFunction, eventOccurrencesRange: EventOccurrencesRange? = null) {
            if (eventOccurrencesRange == null) {
                processLocalDeclaration(function)
            } else {
                visitInlinedFunction(function, eventOccurrencesRange)
            }

            val isAnonymousFunction = function is CjFunctionLiteral || function.name == null
            if (isAnonymousFunction || function.isLocal && function.parent !is CjBlockExpression) {
                builder.createLambda(function)
            }
        }

        private fun visitInlinedFunction(
            lambdaFunctionLiteral: CjFunction,
            eventOccurrencesRange: EventOccurrencesRange
        ) {
            // Defer emitting of inlined declaration
            deferredGeneratorsStack.peek().add { builder ->
                val beforeDeclaration = builder.createUnboundLabel("before inlined declaration")
                val afterDeclaration = builder.createUnboundLabel("after inlined declaration")

                builder.bindLabel(beforeDeclaration)

                if (!eventOccurrencesRange.isDefinitelyVisited()) {
                    builder.nondeterministicJump(afterDeclaration, lambdaFunctionLiteral, null)
                }

                generate(lambdaFunctionLiteral, eventOccurrencesRange)

                if (eventOccurrencesRange.canBeRevisited()) {
                    builder.nondeterministicJump(beforeDeclaration, lambdaFunctionLiteral, null)
                }

                builder.bindLabel(afterDeclaration)
            }
        }

        override fun visitNamedFunction(function: CjNamedFunction) {
            visitFunction(function)
        }

        override fun visitLambdaExpression(lambdaExpression: CjLambdaExpression) {
            mark(lambdaExpression)
            val functionLiteral = lambdaExpression.functionLiteral

            // NB. Behaviour here is implicitly controlled by the LanguageFeature 'UseCallsInPlaceEffect'
            // If this feature is turned off, then slice LAMBDA_INVOCATIONS is never written and invocationKind
            // in all subsequent calls always 'null', resulting in falling back to old behaviour
            visitFunction(functionLiteral, trace[BindingContext.LAMBDA_INVOCATIONS, lambdaExpression])
            copyValue(functionLiteral, lambdaExpression)
        }

        override fun visitQualifiedExpression(expression: CjQualifiedExpression) {
            mark(expression)
            val selectorExpression = expression.selectorExpression
            val receiverExpression = expression.receiverExpression
            val safe = expression is CjSafeQualifiedExpression

            // todo: replace with selectorExpresion != null after parser is fixed
            if (selectorExpression is CjCallExpression || selectorExpression is CjSimpleNameExpression) {
                if (!safe) {
                    generateInstructions(selectorExpression)
                } else {
                    val resultLabel = builder.createUnboundLabel("result of call")
                    builder.jumpOnFalse(resultLabel, expression, null)
                    generateInstructions(selectorExpression)
                    builder.bindLabel(resultLabel)
                }
                copyValue(selectorExpression, expression)
            } else {
                generateInstructions(receiverExpression)
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, receiverExpression)
            }
        }

        override fun visitCallExpression(expression: CjCallExpression) {
            if (!generateCall(expression)) {
                val inputExpressions = ArrayList<CjExpression>()
                for (argument in expression.valueArguments) {
                    val argumentExpression = argument.getArgumentExpression()
                    if (argumentExpression != null) {
                        generateInstructions(argumentExpression)
                        inputExpressions.add(argumentExpression)
                    }
                }
                val calleeExpression = expression.calleeExpression
                generateInstructions(calleeExpression)
                if (calleeExpression != null) {
                    inputExpressions.add(calleeExpression)
                    generateAndGetReceiverIfAny(expression)?.let { inputExpressions.add(it) }
                }

                mark(expression)
                createNonSyntheticValue(expression, inputExpressions, MagicKind.UNRESOLVED_CALL)
            }
        }

        private fun generateAndGetReceiverIfAny(expression: CjExpression): CjExpression? {
            val parent = expression.parent as? CjQualifiedExpression ?: return null

            if (parent.selectorExpression !== expression) return null

            val receiverExpression = parent.receiverExpression
            generateInstructions(receiverExpression)

            return receiverExpression
        }


        override fun visitVariable(variable: CjVariable<*>) {
            builder.declareVariable(variable)
            val initializer = variable.initializer
            if (initializer != null) {
                visitAssignment(variable, getDeferredValue(initializer), variable)
            }


        }

        override fun visitProperty(property: CjProperty) {
            builder.declareVariable(property)

        }


        override fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS) {
            mark(expression)

            val operationType = expression.operationReference.referencedNameElementType
            val left = expression.left
            if (operationType === AS_KEYWORD) {
                generateInstructions(left)
                if (getBoundOrUnreachableValue(left) != null) {
                    createNonSyntheticValue(expression, MagicKind.CAST, left)
                }
            } else {
                visitCjElement(expression)
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, left)
            }
        }

        private fun generateJumpsToCatchAndFinally() {
            if (catchFinallyStack.isNotEmpty()) {
                with(catchFinallyStack.peek()) {
                    if (tryExpression != null) {
                        onException?.let {
                            builder.nondeterministicJump(it, tryExpression, null)
                        }
                        toFinally?.let {
                            builder.nondeterministicJump(it, tryExpression, null)
                        }
                    }
                }
            }
        }

        override fun visitThrowExpression(expression: CjThrowExpression) {
            mark(expression)

            generateJumpsToCatchAndFinally()

            val thrownExpression = expression.thrownExpression ?: return
            generateInstructions(thrownExpression)

            val thrownValue = builder.getBoundValue(thrownExpression) ?: return
            builder.throwException(expression, thrownValue)
        }

        override fun visitArrayAccessExpression(expression: CjArrayAccessExpression) {
            generateArrayAccess(expression, trace[BindingContext.INDEXED_LVALUE_GET, expression])
        }

        override fun visitIsExpression(expression: CjIsExpression) {
            mark(expression)
            val left = expression.leftHandSide
            generateInstructions(left)
            createNonSyntheticValue(expression, MagicKind.IS, left)
        }

        override fun visitMatchExpression(expression: CjMatchExpression) {
            mark(expression)

            val subjectExpression = expression.subjectExpression
            if (subjectExpression != null) {
                generateInstructions(subjectExpression)
            }

            val branches = ArrayList<CjExpression>()

            val doneLabel = builder.createUnboundLabel("after 'match' expression")

            var nextLabel: Label? = null
            val iterator = expression.entries.iterator()
            while (iterator.hasNext()) {
                val matchEntry = iterator.next()
                mark(matchEntry)

                val isElse = matchEntry.isElse
                if (isElse) {
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_MATCH.on(matchEntry))
                    }
                }
                val bodyLabel = builder.createUnboundLabel("'when' entry body")

                val conditions = matchEntry.conditions
                for (i in conditions.indices) {
                    val condition = conditions[i]
                    condition.accept(conditionVisitor)
                    if (i + 1 < conditions.size) {
                        builder.nondeterministicJump(bodyLabel, expression, builder.getBoundValue(condition))
                    }
                }

                if (!isElse) {
                    nextLabel = builder.createUnboundLabel("next 'match' entry")
                    val lastCondition = conditions.lastOrNull()
                    builder.nondeterministicJump(nextLabel, expression, builder.getBoundValue(lastCondition))
                }

                builder.bindLabel(bodyLabel)
                val matchEntryExpression = matchEntry.expression
                if (matchEntryExpression != null) {
                    generateInstructions(matchEntryExpression)
                    branches.add(matchEntryExpression)
                }
                builder.jump(doneLabel, expression)

                if (!isElse && nextLabel != null) {
                    builder.bindLabel(nextLabel)
                    // For the last entry of exhaustive match,
                    // attempt to jump further should lead to error, not to "done"
                    if (!iterator.hasNext() && MatchChecker.isMatchExhaustive(expression, trace)) {
                        builder.magic(expression, null, emptyList(), MagicKind.EXHAUSTIVE_MATCH_ELSE)
                    }
                }
            }
            builder.bindLabel(doneLabel)

            mergeValues(branches, expression)
            MatchChecker.checkDuplicatedLabels(
                expression,
                trace,
                languageVersionSettings
            )
        }


        override fun visitStringTemplateExpression(expression: CjStringTemplateExpression) {
            mark(expression)

            val inputExpressions = ArrayList<CjExpression>()
            for (entry in expression.entries) {
                if (entry is CjStringTemplateEntryWithExpression) {
                    val entryExpression = entry.expression
                    generateInstructions(entryExpression)
                    if (entryExpression != null) {
                        inputExpressions.add(entryExpression)
                    }
                }
            }
            builder.loadStringTemplate(expression, elementsToValues(inputExpressions))
        }

        override fun visitTypeProjection(typeProjection: CjTypeProjection) {
            // TODO : Support Type Arguments. Companion object may be initialized at this point");
        }

        override fun visitAnonymousInitializer(initializer: CjAnonymousInitializer) {
            generateInstructions(initializer.body)
        }

        private fun generateHeaderDelegationSpecifiers(classOrObject: CjTypeStatement) {
            for (specifier in classOrObject.superTypeListEntries) {
                generateInstructions(specifier)
            }
        }

        private fun generateInitializersForClassOrObject(classOrObject: CjDeclarationContainer) {
            for (declaration in classOrObject.declarations) {
                if (declaration is CjProperty || declaration is CjVariable<*> || declaration is CjAnonymousInitializer) {
                    generateInstructions(declaration)
                }
            }
        }

        override fun visitTypeStatement(typeStatement: CjTypeStatement) {
            if (typeStatement.hasPrimaryConstructor()) {
                processParameters(typeStatement.primaryConstructorParameters)

                // delegation specifiers of primary constructor, anonymous class and property initializers
                generateHeaderDelegationSpecifiers(typeStatement)
                generateInitializersForClassOrObject(typeStatement)
            }

            generateDeclarationForLocalClassOrObjectIfNeeded(typeStatement)
        }

        override fun visitClass(cclass: CjClass) {
            if (cclass.hasPrimaryConstructor()) {
                processParameters(cclass.primaryConstructorParameters)

                // delegation specifiers of primary constructor, anonymous class and property initializers
                generateHeaderDelegationSpecifiers(cclass)
                generateInitializersForClassOrObject(cclass)
            }

            generateDeclarationForLocalClassOrObjectIfNeeded(cclass)

//            if (cclass.isEnum()) {
//                cclass.declarations.forEach {
//                    when (it) {
//                        is CjEnumConstructor -> {
//                            processEntryOrObject(it)
//                        }
//
//                    }
//                }
//            }
        }


        private fun generateDeclarationForLocalClassOrObjectIfNeeded(classOrObject: CjTypeStatement) {
            if (classOrObject.isLocal) {
                for (declaration in classOrObject.declarations) {
                    if (declaration is CjSecondaryConstructor ||
                        declaration is CjProperty ||
                        declaration is CjAnonymousInitializer
                    ) {
                        continue
                    }
                    generateInstructions(declaration)
                }
            }
        }

        private fun processParameters(parameters: List<CjParameter>) {
            for (parameter in parameters) {
                generateInstructions(parameter)
            }
        }

        override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
            val classOrObject =
                PsiTreeUtil.getParentOfType(constructor, CjTypeStatement::class.java)
                    ?: error("Guaranteed by parsing contract")

            processParameters(constructor.valueParameters)
            constructor.delegationCall?.let { generateCallOrMarkUnresolved(it) }

            if (constructor.delegationCall?.isCallToThis != true) {
                generateInitializersForClassOrObject(classOrObject)
            }

            generateInstructions(constructor.bodyExpression)
        }

        override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
            generateCallOrMarkUnresolved(call)
        }


        private fun generateCallOrMarkUnresolved(call: CjCallElement) {
            if (!generateCall(call)) {
                val arguments = call.valueArguments.mapNotNull(ValueArgument::getArgumentExpression)

                for (argument in arguments) {
                    generateInstructions(argument)
                }
                createNonSyntheticValue(call, arguments, MagicKind.UNRESOLVED_CALL)
            }
        }


        override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry) {
            // Do not generate UNSUPPORTED_ELEMENT here
        }

        override fun visitSuperTypeList(list: CjSuperTypeList) {
            list.acceptChildren(this)
        }

        override fun visitCjFile(file: CjFile) {
            for (declaration in file.declarations) {
                if (declaration is CjProperty || declaration is CjVariable<*>) {
                    generateInstructions(declaration)
                }
            }
        }



        override fun visitCjElement(element: CjElement) {
            createNonSyntheticValue(element, MagicKind.UNSUPPORTED_ELEMENT)
        }

        private fun generateQualifier(expression: CjExpression, qualifier: QualifierReceiver): Boolean {
            qualifier.descriptor
//            if (qualifierDescriptor is ClassDescriptor) {
//                getFakeDescriptorForObject(qualifierDescriptor)?.let {
//                    mark(expression)
//                    builder.read(expression, AccessTarget.Declaration(it), emptyMap())
//                    return true
//                }
//            }
            return false
        }

        private fun generateCall(callElement: CjElement): Boolean {
            val resolvedCall = callElement.getResolvedCall(trace.bindingContext)
            resolvedCall?.call?.callElement ?: return false

            return checkAndGenerateCall(resolvedCall)
        }

        private fun checkAndGenerateCall(resolvedCall: ResolvedCall<*>?): Boolean {
            if (resolvedCall == null) return false
            generateCall(resolvedCall)
            return true
        }

        private fun generateCall(resolvedCall: ResolvedCall<*>): InstructionWithValue {
            val callElement = resolvedCall.call.callElement

            val receivers = getReceiverValues(resolvedCall)

            deferredGeneratorsStack.push(mutableListOf())

            var parameterValues = SmartFMap.emptyMap<PseudoValue, ValueParameterDescriptor>()
            for (argument in resolvedCall.call.valueArguments) {
                val argumentMapping = resolvedCall.getArgumentMapping(argument)
                val argumentExpression = argument.getArgumentExpression()
                if (argumentMapping is ArgumentMatch) {
                    parameterValues = generateValueArgument(argument, argumentMapping.valueParameter, parameterValues)
                } else if (argumentExpression != null) {
                    generateInstructions(argumentExpression)
                    createSyntheticValue(argumentExpression, MagicKind.VALUE_CONSUMER, argumentExpression)
                }
            }

            val callInstruction = if (resolvedCall.resultingDescriptor is VariableDescriptor) {
                // If a callee of the call is just a variable (without 'invoke'), 'read variable' is generated.

                val callExpression =
                    callElement as? CjExpression
                        ?: error("Variable-based call without callee expression: " + callElement.text)
                assert(parameterValues.isEmpty()) { "Variable-based call with non-empty argument list: " + callElement.text }
                builder.readVariable(callExpression, resolvedCall, receivers)
            } else {
                mark(resolvedCall.call.callElement)
                builder.call(callElement, resolvedCall, receivers, parameterValues)
            }
            deferredGeneratorsStack.pop().forEach { it.invoke(builder) }
            return callInstruction
        }

        private fun getReceiverValues(resolvedCall: ResolvedCall<*>): Map<PseudoValue, ReceiverValue> {
            var varCallResult: PseudoValue? = null
            var explicitReceiver: ReceiverValue? = null
            if (resolvedCall is VariableAsFunctionResolvedCall) {
                varCallResult = generateCall(resolvedCall.variableCall).outputValue

                //noinspection EnumSwitchStatementWhichMissesCases
                when (resolvedCall.explicitReceiverKind) {
                    ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver = resolvedCall.dispatchReceiver
                    ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> explicitReceiver =
                        resolvedCall.extensionReceiver

                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> {
                    }
                }
            }

            var receiverValues = SmartFMap.emptyMap<PseudoValue, ReceiverValue>()
            if (explicitReceiver != null && varCallResult != null) {
                receiverValues = receiverValues.plus(varCallResult, explicitReceiver)
            }
            val callElement = resolvedCall.call.callElement
            receiverValues = getReceiverValues(callElement, resolvedCall.dispatchReceiver, receiverValues)
            receiverValues = getReceiverValues(callElement, resolvedCall.extensionReceiver, receiverValues)
            return receiverValues
        }

        private fun getReceiverValues(
            callElement: CjElement,
            receiver: ReceiverValue?,
            receiverValuesArg: SmartFMap<PseudoValue, ReceiverValue>
        ): SmartFMap<PseudoValue, ReceiverValue> {
            var receiverValues = receiverValuesArg
            if (receiver == null || receiverValues.containsValue(receiver)) return receiverValues

            when (receiver) {
                is ImplicitReceiver -> {
//                    if (callElement is CjCallExpression) {
//                        val declaration = receiver.declarationDescriptor
//                        if (declaration is ClassDescriptor) {
//                            val fakeDescriptor = getFakeDescriptorForObject(declaration)
//                            val calleeExpression = callElement.calleeExpression
//                            if (fakeDescriptor != null && calleeExpression != null) {
//                                builder.read(calleeExpression, AccessTarget.Declaration(fakeDescriptor), emptyMap())
//                            }
//                        }
//                    }
                    receiverValues =
                        receiverValues.plus(createSyntheticValue(callElement, MagicKind.IMPLICIT_RECEIVER), receiver)
                }

                is ExpressionReceiver -> {
                    val expression = receiver.expression
                    if (builder.getBoundValue(expression) == null) {
                        generateInstructions(expression)
                    }

                    val receiverPseudoValue = getBoundOrUnreachableValue(expression)
                    if (receiverPseudoValue != null) {
                        receiverValues = receiverValues.plus(receiverPseudoValue, receiver)
                    }
                }

                is TransientReceiver -> {
                    // Do nothing
                }

                else -> {
                    throw IllegalArgumentException("Unknown receiver kind: $receiver")
                }
            }

            return receiverValues
        }

        private fun generateValueArgument(
            valueArgument: ValueArgument,
            parameterDescriptor: ValueParameterDescriptor,
            parameterValuesArg: SmartFMap<PseudoValue, ValueParameterDescriptor>
        ): SmartFMap<PseudoValue, ValueParameterDescriptor> {
            var parameterValues = parameterValuesArg
            val expression = valueArgument.getArgumentExpression()
            if (expression != null) {
                if (!valueArgument.isExternal()) {
                    generateInstructions(expression)
                }

                val argValue = getBoundOrUnreachableValue(expression)
                if (argValue != null) {
                    parameterValues = parameterValues.plus(argValue, parameterDescriptor)
                }
            }
            return parameterValues
        }
    }

}
typealias DeferredGenerator = (ControlFlowBuilder) -> Unit
