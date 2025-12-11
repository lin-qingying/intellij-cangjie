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

import org.cangnova.cangjie.resolve.controlFlow.pseudocode.BreakableBlockInfo
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


interface GenerationTrigger {
    fun generate()
}

class LoopInfo(
    override val element: CjLoopExpression,
    entryPoint: Label,
    exitPoint: Label,
    val bodyEntryPoint: Label,
    val bodyExitPoint: Label,
    val conditionEntryPoint: Label
) : BreakableBlockInfo(element, entryPoint, exitPoint) {

    init {
        markReferablePoints(bodyEntryPoint, bodyExitPoint, conditionEntryPoint)
    }
}

interface ControlFlowBuilder {
    // Subroutines
    fun enterSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange? = null)

    fun exitSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange? = null): Pseudocode

    val currentSubroutine: CjElement
    val returnSubroutine: CjElement

    // Scopes
    fun enterBlockScope(block: CjElement)

    fun exitBlockScope(block: CjElement)

    fun getSubroutineExitPoint(labelElement: CjElement): Label?
    fun getLoopConditionEntryPoint(loop: CjLoopExpression): Label?
    fun getLoopExitPoint(loop: CjLoopExpression): Label?

    // Declarations
    fun declareParameter(parameter: CjParameterBase)
    fun declarePattern(parameter: CjCasePattern)
    fun declareVariable(property: CjVariableDeclaration)
    fun declareFunction(subroutine: CjElement, pseudocode: Pseudocode)

    fun declareInlinedFunction(
        subroutine: CjElement,
        pseudocode: Pseudocode,
        eventOccurrencesRange: EventOccurrencesRange
    )

    fun declareEntryOrObject(entryOrObject: CjTypeStatement)

    // Labels
    fun createUnboundLabel(): Label

    fun createUnboundLabel(name: String): Label

    fun bindLabel(label: Label)

    // Jumps
    fun jump(label: Label, element: CjElement)

    fun jumpOnFalse(label: Label, element: CjElement, conditionValue: PseudoValue?)
    fun jumpOnTrue(label: Label, element: CjElement, conditionValue: PseudoValue?)
    fun nondeterministicJump(label: Label, element: CjElement, inputValue: PseudoValue?)  // Maybe, jump to label
    fun nondeterministicJump(label: List<Label>, element: CjElement)
    fun jumpToError(element: CjElement)

    fun returnValue(returnExpression: CjExpression, returnValue: PseudoValue, subroutine: CjElement)
    fun returnNoValue(returnExpression: CjReturnExpression, subroutine: CjElement)

    fun throwException(throwExpression: CjThrowExpression, thrownValue: PseudoValue)

    // Loops
    fun enterLoop(expression: CjLoopExpression): LoopInfo

    fun enterLoopBody(expression: CjLoopExpression)
    fun exitLoopBody(expression: CjLoopExpression)
    val currentLoop: CjLoopExpression?

    // Try-Finally
    fun enterTryFinally(trigger: GenerationTrigger)

    fun exitTryFinally()

    fun repeatPseudocode(startLabel: Label, finishLabel: Label)

    // Reading values
    fun mark(element: CjElement)

    fun getBoundValue(element: CjElement?): PseudoValue?
    fun bindValue(value: PseudoValue, element: CjElement)
    fun newValue(element: CjElement?): PseudoValue

    fun loadUnit(expression: CjExpression)

    fun loadConstant(expression: CjExpression, constant: CompileTimeConstant<*>?): InstructionWithValue

    fun createLambda(expression: CjFunction): InstructionWithValue
    fun loadStringTemplate(expression: CjStringTemplateExpression, inputValues: List<PseudoValue>): InstructionWithValue

    fun magic(
        instructionElement: CjElement,
        valueElement: CjElement?,
        inputValues: List<PseudoValue>,
        kind: MagicKind
    ): MagicInstruction

    fun merge(
        expression: CjExpression,
        inputValues: List<PseudoValue>
    ): MergeInstruction

    fun readVariable(
        expression: CjExpression,
        resolvedCall: ResolvedCall<*>,
        receiverValues: Map<PseudoValue, ReceiverValue>
    ): ReadValueInstruction

    fun call(
        valueElement: CjElement,
        resolvedCall: ResolvedCall<*>,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        arguments: Map<PseudoValue, ValueParameterDescriptor>
    ): CallInstruction

    enum class PredefinedOperation {
        AND,
        OR,
        NOT_NULL_ASSERTION
    }

    fun predefinedOperation(
        expression: CjExpression,
        operation: PredefinedOperation,
        inputValues: List<PseudoValue>
    ): OperationInstruction

    fun read(
        element: CjElement,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>
    ): ReadValueInstruction

    fun write(
        assignment: CjElement,
        lValue: CjElement,
        rValue: PseudoValue,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>
    )
}
