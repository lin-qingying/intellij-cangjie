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

import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Label
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.contracts.description.EventOccurrencesRange
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.constants.CompileTimeConstant
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue


abstract class ControlFlowBuilderAdapter : ControlFlowBuilder {

    protected abstract val delegateBuilder: ControlFlowBuilder


    override fun merge(expression: CjExpression, inputValues: List<PseudoValue>): MergeInstruction =
        delegateBuilder.merge(expression, inputValues)

    override fun readVariable(
        expression: CjExpression,
        resolvedCall: ResolvedCall<*>,
        receiverValues: Map<PseudoValue, ReceiverValue>
    ): ReadValueInstruction =
        delegateBuilder.readVariable(expression, resolvedCall, receiverValues)

    override fun call(
        valueElement: CjElement,
        resolvedCall: ResolvedCall<*>,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        arguments: Map<PseudoValue, ValueParameterDescriptor>
    ): CallInstruction =
        delegateBuilder.call(valueElement, resolvedCall, receiverValues, arguments)


    override fun throwException(throwExpression: CjThrowExpression, thrownValue: PseudoValue) {
        delegateBuilder.throwException(throwExpression, thrownValue)
    }


    override fun loadUnit(expression: CjExpression) {
        delegateBuilder.loadUnit(expression)
    }

    override fun loadConstant(expression: CjExpression, constant: CompileTimeConstant<*>?): InstructionWithValue =
        delegateBuilder.loadConstant(expression, constant)


    override fun createLambda(expression: CjFunction): InstructionWithValue = delegateBuilder.createLambda(expression)

    override val returnSubroutine: CjElement
        get() = delegateBuilder.returnSubroutine

    override fun returnValue(returnExpression: CjExpression, returnValue: PseudoValue, subroutine: CjElement) {
        delegateBuilder.returnValue(returnExpression, returnValue, subroutine)
    }


    override fun read(element: CjElement, target: AccessTarget, receiverValues: Map<PseudoValue, ReceiverValue>) =
        delegateBuilder.read(element, target, receiverValues)

    override fun write(
        assignment: CjElement,
        lValue: CjElement,
        rValue: PseudoValue,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>
    ) {
        delegateBuilder.write(assignment, lValue, rValue, target, receiverValues)
    }

    override fun declarePattern(parameter: CjCasePatternElement) {
        delegateBuilder.declarePattern(parameter)

    }

    override fun declareParameter(parameter: CjParameterBase) {
        delegateBuilder.declareParameter(parameter)
    }

    override fun returnNoValue(returnExpression: CjReturnExpression, subroutine: CjElement) {
        delegateBuilder.returnNoValue(returnExpression, subroutine)
    }

    override fun predefinedOperation(
        expression: CjExpression,
        operation: ControlFlowBuilder.PredefinedOperation,
        inputValues: List<PseudoValue>
    ): OperationInstruction = delegateBuilder.predefinedOperation(expression, operation, inputValues)

    override fun createUnboundLabel(): Label = delegateBuilder.createUnboundLabel()

    override fun createUnboundLabel(name: String): Label = delegateBuilder.createUnboundLabel(name)

    override fun bindLabel(label: Label) {
        delegateBuilder.bindLabel(label)
    }

    override fun jump(label: Label, element: CjElement) {
        delegateBuilder.jump(label, element)
    }

    override fun jumpOnFalse(label: Label, element: CjElement, conditionValue: PseudoValue?) {
        delegateBuilder.jumpOnFalse(label, element, conditionValue)
    }

    override fun jumpOnTrue(label: Label, element: CjElement, conditionValue: PseudoValue?) {
        delegateBuilder.jumpOnTrue(label, element, conditionValue)
    }

    override fun magic(
        instructionElement: CjElement,
        valueElement: CjElement?,
        inputValues: List<PseudoValue>,
        kind: MagicKind
    ): MagicInstruction = delegateBuilder.magic(instructionElement, valueElement, inputValues, kind)

    override fun nondeterministicJump(label: Label, element: CjElement, inputValue: PseudoValue?) {
        delegateBuilder.nondeterministicJump(label, element, inputValue)
    }

    override fun nondeterministicJump(label: List<Label>, element: CjElement) {
        delegateBuilder.nondeterministicJump(label, element)
    }

    //
    override fun jumpToError(element: CjElement) {
        delegateBuilder.jumpToError(element)
    }

    override fun exitTryFinally() {
        delegateBuilder.exitTryFinally()
    }

    override fun enterSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange?) {
        delegateBuilder.enterSubroutine(subroutine, eventOccurrencesRange)
    }

    override fun exitSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange?): Pseudocode =
        delegateBuilder.exitSubroutine(subroutine, eventOccurrencesRange)

    override val currentSubroutine: CjElement
        get() = delegateBuilder.currentSubroutine

    override fun getSubroutineExitPoint(labelElement: CjElement): Label? =
        delegateBuilder.getSubroutineExitPoint(labelElement)

    override fun getLoopConditionEntryPoint(loop: CjLoopExpression): Label? =
        delegateBuilder.getLoopConditionEntryPoint(loop)

    override fun loadStringTemplate(
        expression: CjStringTemplateExpression,
        inputValues: List<PseudoValue>
    ): InstructionWithValue =
        delegateBuilder.loadStringTemplate(expression, inputValues)

    override fun getLoopExitPoint(loop: CjLoopExpression): Label? = delegateBuilder.getLoopExitPoint(loop)

    override fun enterLoop(expression: CjLoopExpression): LoopInfo = delegateBuilder.enterLoop(expression)

    override fun enterLoopBody(expression: CjLoopExpression) {
        delegateBuilder.enterLoopBody(expression)
    }

    override fun exitLoopBody(expression: CjLoopExpression) {
        delegateBuilder.exitLoopBody(expression)
    }

    override val currentLoop: CjLoopExpression?
        get() = delegateBuilder.currentLoop

    override fun enterTryFinally(trigger: GenerationTrigger) {
        delegateBuilder.enterTryFinally(trigger)
    }

    override fun declareVariable(property: CjVariableDeclaration) {
        delegateBuilder.declareVariable(property)
    }

    override fun declareFunction(subroutine: CjElement, pseudocode: Pseudocode) {
        delegateBuilder.declareFunction(subroutine, pseudocode)
    }

    override fun declareInlinedFunction(
        subroutine: CjElement,
        pseudocode: Pseudocode,
        eventOccurrencesRange: EventOccurrencesRange
    ) {
        delegateBuilder.declareInlinedFunction(subroutine, pseudocode, eventOccurrencesRange)
    }

    override fun declareEntryOrObject(entryOrObject: CjTypeStatement) {
        delegateBuilder.declareEntryOrObject(entryOrObject)
    }

    override fun repeatPseudocode(startLabel: Label, finishLabel: Label) {
        delegateBuilder.repeatPseudocode(startLabel, finishLabel)
    }

    override fun mark(element: CjElement) {
        delegateBuilder.mark(element)
    }

    override fun getBoundValue(element: CjElement?): PseudoValue? = delegateBuilder.getBoundValue(element)

    override fun bindValue(value: PseudoValue, element: CjElement) {
        delegateBuilder.bindValue(value, element)
    }

    override fun newValue(element: CjElement?): PseudoValue = delegateBuilder.newValue(element)

    override fun enterBlockScope(block: CjElement) {
        delegateBuilder.enterBlockScope(block)
    }

    override fun exitBlockScope(block: CjElement) {
        delegateBuilder.exitBlockScope(block)
    }
}
