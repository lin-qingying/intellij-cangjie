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

package org.cangnova.cangjie.resolve.controlFlow.pseudocode

import org.cangnova.cangjie.resolve.controlFlow.ControlFlowBuilder
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowBuilderAdapter
import org.cangnova.cangjie.resolve.controlFlow.GenerationTrigger
import org.cangnova.cangjie.resolve.controlFlow.LoopInfo
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.contracts.description.EventOccurrencesRange
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.constants.CompileTimeConstant
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.Stack
import java.util.*

abstract class BlockInfo

abstract class BreakableBlockInfo(open val element: CjElement, val entryPoint: Label, val exitPoint: Label) :
    BlockInfo() {
    val referablePoints: MutableSet<Label> = hashSetOf()

    init {
        markReferablePoints(entryPoint, exitPoint)
    }

    protected fun markReferablePoints(vararg labels: Label) {
        Collections.addAll(referablePoints, *labels)
    }
}

interface Label {
    val pseudocode: Pseudocode

    val name: String

    val targetInstructionIndex: Int

    fun resolveToInstruction(): Instruction
}


class SubroutineInfo(subroutine: CjElement, entryPoint: Label, exitPoint: Label) :
    BreakableBlockInfo(subroutine, entryPoint, exitPoint)

class ControlFlowInstructionsGenerator : ControlFlowBuilderAdapter() {
    companion object {
        val LOG = Logger.getInstance(ControlFlowInstructionsGenerator::class.java)
    }

    private var builder: ControlFlowBuilder? = null
    private val builders = Stack<ControlFlowInstructionsGeneratorWorker>()
    private val blockScopes = Stack<BlockScope>()
    private val elementToLoopInfo = HashMap<CjLoopExpression, LoopInfo>()
    private val loopInfo = Stack<LoopInfo>()

    override val delegateBuilder: ControlFlowBuilder
        get() = builder ?: throw AssertionError("Builder stack is empty in ControlFlowInstructionsGenerator!")

    private fun pushBuilder(scopingElement: CjElement, subroutine: CjElement, shouldInline: Boolean) {
        val worker = ControlFlowInstructionsGeneratorWorker(scopingElement, subroutine, shouldInline)
        builders.push(worker)
//        LOG.info(worker.pseudocode.toString())
//        println(worker.pseudocode.toString())
        builder = worker
    }

    private fun popBuilder(): ControlFlowInstructionsGeneratorWorker {
        val worker = builders.pop()
        builder = if (!builders.isEmpty()) {
            builders.peek()
        } else {
            null
        }
        return worker
    }

    override fun exitSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange?): Pseudocode {
        super.exitSubroutine(subroutine, eventOccurrencesRange)
        delegateBuilder.exitBlockScope(subroutine)
        val worker = popBuilder()
        if (!builders.empty()) {
            val builder = builders.peek()
            if (eventOccurrencesRange == null) {
                builder.declareFunction(subroutine, worker.pseudocode)
            } else {
                builder.declareInlinedFunction(subroutine, worker.pseudocode, eventOccurrencesRange)
            }
        }
        return worker.pseudocode
    }

    override fun enterSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange?) {
        val builder = builder
        val shouldInlnie = eventOccurrencesRange != null
        if (builder != null && subroutine is CjFunctionLiteral) {
            pushBuilder(subroutine, builder.returnSubroutine, shouldInlnie)
        } else {
            pushBuilder(subroutine, subroutine, shouldInlnie)
        }
        delegateBuilder.enterBlockScope(subroutine)
        delegateBuilder.enterSubroutine(subroutine)
    }

    private inner class ControlFlowInstructionsGeneratorWorker(
        scopingElement: CjElement,
        override val returnSubroutine: CjElement,
        shouldInline: Boolean
    ) : ControlFlowBuilder {
        val pseudocode: PseudocodeImpl = PseudocodeImpl(scopingElement, shouldInline)
        private val allBlocks = Stack<BlockInfo>()
        private val elementToSubroutineInfo = HashMap<CjElement, SubroutineInfo>()
        private val error: Label = pseudocode.createLabel("error", null)
        private val sink: Label = pseudocode.createLabel("sink", null)
        private var labelCount = 0
        private val valueFactory = object : PseudoValueFactoryImpl() {
            override fun newValue(element: CjElement?, instruction: InstructionWithValue?): PseudoValue {
                val value = super.newValue(element, instruction)
                if (element != null) {
                    bindValue(value, element)
                }
                return value
            }
        }
        private val currentScope: BlockScope
            get() = blockScopes.peek()

        override fun bindLabel(label: Label) {
            pseudocode.bindLabel(label as PseudocodeLabel)
        }

        private fun handleJumpInsideTryFinally(jumpTarget: Label) {
            val finallyBlocks = ArrayList<TryFinallyBlockInfo>()

            for (blockInfo in allBlocks.asReversed()) {
                when (blockInfo) {
                    is BreakableBlockInfo -> if (blockInfo.referablePoints.contains(jumpTarget) || jumpTarget === error) {
                        for (finallyBlockInfo in finallyBlocks) {
                            finallyBlockInfo.generateFinallyBlock()
                        }
                        return
                    }

                    is TryFinallyBlockInfo -> finallyBlocks.add(blockInfo)
                }
            }
        }

        override fun jump(label: Label, element: CjElement) {
            handleJumpInsideTryFinally(label)
            add(UnconditionalJumpInstruction(element, label, currentScope))
        }

        override fun jumpOnFalse(label: Label, element: CjElement, conditionValue: PseudoValue?) {
            handleJumpInsideTryFinally(label)
            add(ConditionalJumpInstruction(element, false, currentScope, label, conditionValue))
        }

        override fun jumpOnTrue(label: Label, element: CjElement, conditionValue: PseudoValue?) {
            handleJumpInsideTryFinally(label)
            add(ConditionalJumpInstruction(element, true, currentScope, label, conditionValue))
        }

        override fun nondeterministicJump(label: Label, element: CjElement, inputValue: PseudoValue?) {
            handleJumpInsideTryFinally(label)
            add(NondeterministicJumpInstruction(element, listOf(label), currentScope, inputValue))
        }

        override fun nondeterministicJump(label: List<Label>, element: CjElement) {
            add(NondeterministicJumpInstruction(element, label, currentScope, null))
        }

        private fun add(instruction: Instruction) {
            pseudocode.addInstruction(instruction)
        }

        override fun enterSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange?) {
            val blockInfo = SubroutineInfo(
                subroutine,
                /* entry point */ createUnboundLabel(),
                /* exit point  */ createUnboundLabel()
            )
            elementToSubroutineInfo.put(subroutine, blockInfo)
            allBlocks.push(blockInfo)
            bindLabel(blockInfo.entryPoint)
            add(SubroutineEnterInstruction(subroutine, currentScope))
        }

        override fun exitSubroutine(subroutine: CjElement, eventOccurrencesRange: EventOccurrencesRange?): Pseudocode {
            getSubroutineExitPoint(subroutine)?.let { bindLabel(it) }
            pseudocode.addExitInstruction(SubroutineExitInstruction(subroutine, currentScope, false))
            bindLabel(error)
            pseudocode.addErrorInstruction(SubroutineExitInstruction(subroutine, currentScope, true))
            bindLabel(sink)
            pseudocode.addSinkInstruction(SubroutineSinkInstruction(subroutine, currentScope, "<SINK>"))
            elementToSubroutineInfo.remove(subroutine)
            allBlocks.pop()
            return pseudocode
        }

        override val currentSubroutine: CjElement
            get() = pseudocode.correspondingElement


        override fun enterBlockScope(block: CjElement) {
            val current = if (blockScopes.isEmpty()) null else currentScope
            val scope = BlockScope(current, block)
            blockScopes.push(scope)
        }

        override fun exitBlockScope(block: CjElement) {
            val currentScope = currentScope
            assert(currentScope.block === block) {
                "Exit from not the current block scope.\n" +
                        "Current scope is for a block: " + currentScope.block.text + ".\n" +
                        "Exit from the scope for: " + block.text
            }
            blockScopes.pop()
        }

        override fun getSubroutineExitPoint(labelElement: CjElement): Label? {
            return elementToSubroutineInfo[labelElement]?.exitPoint
        }

        override fun getLoopConditionEntryPoint(loop: CjLoopExpression): Label? {
            return elementToLoopInfo[loop]?.conditionEntryPoint
        }

        override fun getLoopExitPoint(loop: CjLoopExpression): Label? {
            return elementToLoopInfo[loop]?.exitPoint
        }

        override fun declareParameter(parameter: CjParameterBase) {
            add(VariableDeclarationInstruction(parameter, currentScope))

        }

        override fun declarePattern(parameter: CjCasePattern) {

        }

        override fun declareVariable(property: CjVariableDeclaration) {
            add(VariableDeclarationInstruction(property, currentScope))

        }

        override fun declareFunction(subroutine: CjElement, pseudocode: Pseudocode) {
            add(LocalFunctionDeclarationInstruction(subroutine, pseudocode, currentScope))

        }

        override fun declareInlinedFunction(
            subroutine: CjElement,
            pseudocode: Pseudocode,
            eventOccurrencesRange: EventOccurrencesRange
        ) {
            add(InlinedLocalFunctionDeclarationInstruction(subroutine, pseudocode, currentScope, eventOccurrencesRange))

        }

        override fun declareEntryOrObject(entryOrObject: CjTypeStatement) {
            add(VariableDeclarationInstruction(entryOrObject, currentScope))

        }

        override fun createUnboundLabel(): Label {
            return pseudocode.createLabel("L" + labelCount++, null)

        }

        override fun createUnboundLabel(name: String): Label {
            val a = pseudocode.createLabel("L" + labelCount++, name)

            return a

        }

        override fun jumpToError(element: CjElement) {
            handleJumpInsideTryFinally(error)
            add(UnconditionalJumpInstruction(element, error, currentScope))
        }

        override fun returnValue(returnExpression: CjExpression, returnValue: PseudoValue, subroutine: CjElement) {
            val exitPoint = getSubroutineExitPoint(subroutine) ?: return
            handleJumpInsideTryFinally(exitPoint)
            add(ReturnValueInstruction(returnExpression, currentScope, exitPoint, returnValue, subroutine))
        }

        override fun returnNoValue(returnExpression: CjReturnExpression, subroutine: CjElement) {
            val exitPoint = getSubroutineExitPoint(subroutine) ?: return
            handleJumpInsideTryFinally(exitPoint)
            add(ReturnNoValueInstruction(returnExpression, currentScope, exitPoint, subroutine))
        }

        override fun throwException(throwExpression: CjThrowExpression, thrownValue: PseudoValue) {
            handleJumpInsideTryFinally(error)
            add(ThrowExceptionInstruction(throwExpression, currentScope, error, thrownValue))
        }

        override fun enterLoop(expression: CjLoopExpression): LoopInfo {
            if (expression is CjDoWhileExpression) {
                (pseudocode.rootPseudocode as PseudocodeImpl).containsDoWhile = true
            }

            val info = LoopInfo(
                expression,
                createUnboundLabel("loop entry point"),
                createUnboundLabel("loop exit point"),
                createUnboundLabel("body entry point"),
                createUnboundLabel("body exit point"),
                createUnboundLabel("condition entry point")
            )
            bindLabel(info.entryPoint)
            elementToLoopInfo.put(expression, info)
            return info
        }

        override fun enterLoopBody(expression: CjLoopExpression) {
            val info = elementToLoopInfo[expression]!!
            bindLabel(info.bodyEntryPoint)
            loopInfo.push(info)
            allBlocks.push(info)
        }

        override fun exitLoopBody(expression: CjLoopExpression) {
            val info = loopInfo.pop()
            elementToLoopInfo.remove(expression)
            allBlocks.pop()
            bindLabel(info.bodyExitPoint)
        }

        override val currentLoop: CjLoopExpression?
            get() = if (loopInfo.empty()) null else loopInfo.peek().element

        override fun enterTryFinally(trigger: GenerationTrigger) {
            allBlocks.push(TryFinallyBlockInfo(trigger))

        }

        override fun exitTryFinally() {
            val pop = allBlocks.pop()
            assert(pop is TryFinallyBlockInfo)
        }

        override fun repeatPseudocode(startLabel: Label, finishLabel: Label) {
            labelCount = pseudocode.repeatPart(startLabel, finishLabel, labelCount)

        }

        override fun mark(element: CjElement) {
            add(MarkInstruction(element, currentScope))

        }

        override fun getBoundValue(element: CjElement?): PseudoValue? {
            return pseudocode.getElementValue(element)
        }

        override fun bindValue(value: PseudoValue, element: CjElement) {
            pseudocode.bindElementToValue(element, value)
        }

        override fun newValue(element: CjElement?): PseudoValue {
            return valueFactory.newValue(element, null)
        }

        override fun loadUnit(expression: CjExpression) {
            add(LoadUnitValueInstruction(expression, currentScope))

        }

        private fun read(
            expression: CjExpression,
            resolvedCall: ResolvedCall<*>? = null,
            receiverValues: Map<PseudoValue, ReceiverValue> = emptyMap()
        ) = read(
            expression,
            if (resolvedCall != null) AccessTarget.Call(resolvedCall) else AccessTarget.BlackBox,
            receiverValues
        )

        override fun loadConstant(expression: CjExpression, constant: CompileTimeConstant<*>?): InstructionWithValue {
            return read(expression)
        }

        override fun createLambda(expression: CjFunction): InstructionWithValue {
            return read(if (expression is CjFunctionLiteral) expression.getParent() as CjLambdaExpression else expression)

        }

        override fun loadStringTemplate(
            expression: CjStringTemplateExpression,
            inputValues: List<PseudoValue>
        ): InstructionWithValue {
            return if (inputValues.isEmpty()) read(expression)
            else magic(expression, expression, inputValues, MagicKind.STRING_TEMPLATE)
        }

        override fun magic(
            instructionElement: CjElement,
            valueElement: CjElement?,
            inputValues: List<PseudoValue>,
            kind: MagicKind
        ): MagicInstruction {
            val instruction = MagicInstruction(
                instructionElement, valueElement, currentScope, inputValues, kind, valueFactory
            )
            add(instruction)
            return instruction
        }

        override fun merge(expression: CjExpression, inputValues: List<PseudoValue>): MergeInstruction {
            val instruction = MergeInstruction(expression, currentScope, inputValues, valueFactory)
            add(instruction)
            return instruction
        }

        override fun readVariable(
            expression: CjExpression,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>
        ): ReadValueInstruction {
            return read(expression, resolvedCall, receiverValues)
        }

        override fun call(
            valueElement: CjElement,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>,
            arguments: Map<PseudoValue, ValueParameterDescriptor>
        ): CallInstruction {
            val instruction = CallInstruction(
                valueElement,
                currentScope,
                resolvedCall,
                receiverValues,
                arguments,
                valueFactory
            )
            add(instruction)
            return instruction
        }

        override fun predefinedOperation(
            expression: CjExpression,
            operation: ControlFlowBuilder.PredefinedOperation,
            inputValues: List<PseudoValue>
        ): OperationInstruction {
            return magic(expression, expression, inputValues, getMagicKind(operation))
        }

        private fun getMagicKind(operation: ControlFlowBuilder.PredefinedOperation) = when (operation) {
            ControlFlowBuilder.PredefinedOperation.AND -> MagicKind.AND
            ControlFlowBuilder.PredefinedOperation.OR -> MagicKind.OR
            ControlFlowBuilder.PredefinedOperation.NOT_NULL_ASSERTION -> MagicKind.NOT_NULL_ASSERTION
        }

        override fun read(
            element: CjElement,
            target: AccessTarget,
            receiverValues: Map<PseudoValue, ReceiverValue>
        ): ReadValueInstruction {
            return ReadValueInstruction(element, currentScope, target, receiverValues, valueFactory).apply {
                add(this)
            }
        }

        override fun write(
            assignment: CjElement,
            lValue: CjElement,
            rValue: PseudoValue,
            target: AccessTarget,
            receiverValues: Map<PseudoValue, ReceiverValue>
        ) {
            add(WriteValueInstruction(assignment, currentScope, target, receiverValues, lValue, rValue))

        }
    }

    private class TryFinallyBlockInfo(private val finallyBlock: GenerationTrigger) : BlockInfo() {

        fun generateFinallyBlock() {
            finallyBlock.generate()
        }
    }

}
