package com.linqingying.cangjie.resolve.controlFlow

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.BreakableBlockInfo
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import com.linqingying.cangjie.contracts.description.EventOccurrencesRange
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.constants.CompileTimeConstant
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue


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
    fun declareParameter(parameter: CjParameter)

    fun declareVariable(property: CjVariableDeclaration)
    fun declareFunction(subroutine: CjElement, pseudocode: Pseudocode)

    fun declareInlinedFunction(subroutine: CjElement, pseudocode: Pseudocode, eventOccurrencesRange: EventOccurrencesRange)

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

    fun read(element: CjElement, target: AccessTarget, receiverValues: Map<PseudoValue, ReceiverValue>): ReadValueInstruction

    fun write(
        assignment: CjElement,
        lValue: CjElement,
        rValue: PseudoValue,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>
    )
}
