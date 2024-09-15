package com.huawei.cangjie.resolve.controlFlow

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import com.huawei.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.LoadUnitValueInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MergeInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special.MarkInstruction
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.TypeUtils.noExpectedType

interface ControlFlowInformationProvider {
    fun checkForLocalClassOrObjectMode()

    fun checkDeclaration()

    fun checkFunction(expectedReturnType: CangJieType?)

    interface Factory {
        fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
//            diagnosticSuppressor: PlatformDiagnosticSuppressor,
//            enumWhenTracker: EnumWhenTracker
        ): ControlFlowInformationProvider
    }
}

class ControlFlowInformationProviderImpl private constructor(
    private val subroutine: CjElement,
    private val trace: BindingTrace,
    private val pseudocode: Pseudocode,
    private val languageVersionSettings: LanguageVersionSettings,
//    private val diagnosticSuppressor: PlatformDiagnosticSuppressor,
//    private val enumWhenTracker: EnumWhenTracker?
) : ControlFlowInformationProvider {
    //    private val pseudocodeVariablesData by lazy {
//        PseudocodeVariablesData(pseudocode, trace.bindingContext)
//    }


    object Factory : ControlFlowInformationProvider.Factory {
        override fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
//        diagnosticSuppressor: PlatformDiagnosticSuppressor,
//        enumWhenTracker: EnumWhenTracker
        ): ControlFlowInformationProvider =
            ControlFlowInformationProviderImpl(
                declaration,
                trace,
                languageVersionSettings,/* diagnosticSuppressor, enumWhenTracker*/
            )
    }

    constructor(
        declaration: CjElement,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
//        diagnosticSuppressor: PlatformDiagnosticSuppressor,
//        enumWhenTracker: EnumWhenTracker? = null
    ) : this(
        declaration,
        trace,
        ControlFlowProcessor(trace, languageVersionSettings).generatePseudocode(declaration),
        languageVersionSettings,
//        diagnosticSuppressor,
//        enumWhenTracker
    )

    private data class ReturnedExpressionsInfo(
        val returnedExpressions: Collection<CjElement>,
        val hasReturnsInInlinedLambda: Boolean
    )

    private fun collectReturnExpressions(): ReturnedExpressionsInfo {
        val instructions = pseudocode.instructions.toHashSet()
        val exitInstruction = pseudocode.exitInstruction
//
        val returnedExpressions = arrayListOf<CjElement>()
        var hasReturnsInInlinedLambda = false
//
        for (previousInstruction in exitInstruction.previousInstructions) {
            previousInstruction.accept(object : InstructionVisitor() {
                override fun visitReturnValue(instruction: ReturnValueInstruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add(instruction.element)
                    }

                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.element)
                    }

                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                // Note that there's no need to overload `visitThrowException`, because
                // it can never be a predecessor of EXIT (throwing always leads to ERROR)

                private fun redirectToPrevInstructions(instruction: Instruction) {
                    for (redirectInstruction in instruction.previousInstructions) {
                        redirectInstruction.accept(this)
                    }
                }

                override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitMarkInstruction(instruction: MarkInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitInstruction(instruction: Instruction) {
                    if (instruction is CjElementInstruction) {
                        // Caveats:
                        // - for empty block-bodies, read(Unit) is emitted and will be processed here
                        // - for Unit-coerced blocks, last expression will be processed here
                        returnedExpressions.add(instruction.element)
                    } else {
                        throw IllegalStateException("$instruction precedes the exit point")
                    }
                }
            })
        }

        return ReturnedExpressionsInfo(returnedExpressions, hasReturnsInInlinedLambda)

    }

    private fun checkDefiniteReturn(expectedReturnType: CangJieType, unreachableCode: UnreachableCode) {
        val function = subroutine as? CjDeclarationWithBody
            ?: throw AssertionError("checkDefiniteReturn is called for ${subroutine.text} which is not CjDeclarationWithBody")

        if (!function.hasBody()) return

        val (returnedExpressions, hasReturnsInInlinedLambdas) = collectReturnExpressions()

        val blockBody = function.hasBlockBody()

        var noReturnError = false
        for (returnedExpression in returnedExpressions) {
            returnedExpression.accept(object : CjVisitorVoid() {
                override fun visitReturnExpression(expression: CjReturnExpression) {
                    noReturnError = false

                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression))
                    }
                }

                override fun visitExpression(expression: CjExpression) {


                }

                override fun visitBlockExpression(expression: CjBlockExpression) {
                    visitCjElement(expression)
                }

                override fun visitCjElement(element: CjElement) {
                    if (!(element is CjExpression || element is CjMatchCondition)) return

                    if (blockBody && !noExpectedType(expectedReturnType)
                        && !CangJieBuiltIns.isUnit(expectedReturnType)
                        && !unreachableCode.elements.contains(element)
                    ) {
                        noReturnError = true
                    }

                }
            })
        }

        if (noReturnError) {
            if (hasReturnsInInlinedLambdas) {
                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION.on(function))
            } else {
                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function))
            }
        }
    }

    override fun checkForLocalClassOrObjectMode() {
//        recordInitializedVariables()
    }

    //    private fun recordInitializedVariables() {
//        val pseudocode = pseudocodeVariablesData.pseudocode
//        val initializers = pseudocodeVariablesData.variableInitializers
//        recordInitializedVariables(pseudocode, initializers)
//        for (instruction in pseudocode.localDeclarations) {
//            recordInitializedVariables(instruction.body, initializers)
//        }
//    }

    fun getLocalFunctions(): Set<Pair<CjFunction, FunctionDescriptor?>> {
        return pseudocode.localDeclarations.mapNotNull {
            if (it.element is CjFunction) {
                Pair(
                    it.element as CjFunction,
                    trace.bindingContext.get(DECLARATION_TO_DESCRIPTOR, it.element) as? FunctionDescriptor
                )
            } else {
                null
            }
        }.toSet()


    }

    private fun checkLocalFunctions() {
        for (localDeclarationInstruction in pseudocode.localDeclarations) {
            val element = localDeclarationInstruction.element
            if (element is CjDeclarationWithBody) {

                val functionDescriptor =
                    trace.bindingContext.get(DECLARATION_TO_DESCRIPTOR, element) as? CallableDescriptor

                val expectedType = functionDescriptor?.returnType

                val providerForLocalDeclaration = ControlFlowInformationProviderImpl(
                    element,
                    trace,
                    localDeclarationInstruction.body,
                    languageVersionSettings/*, diagnosticSuppressor, enumWhenTracker*/
                )

                providerForLocalDeclaration.checkFunction(expectedType)
            }
        }
    }

    private fun checkMainFunction() {

    }

    override fun checkDeclaration() {
//        recordInitializedVariables()

        checkLocalFunctions()
        checkMainFunction()
        markUninitializedVariables()

//        if (trace.wantsDiagnostics()) {
//            markUnusedVariables()
//        }
//
//        checkForSuspendLambdaAndMarkParameters(pseudocode)
//
//        markStatements()
//        markAnnotationArguments()
//
//        markUnusedExpressions()
//
//        if (trace.wantsDiagnostics()) {
//            checkIfExpressions()
//        }
//
//        checkWhenExpressions()
//
//        checkConstructorConsistency()

    }
    ////////////////////////////////////////////////////////////////////////////////
    //  Uninitialized variables analysis

    private fun markUninitializedVariables() {
//        val varWithUninitializedErrorGenerated = hashSetOf<VariableDescriptor>()
//        val varWithValReassignErrorGenerated = hashSetOf<VariableDescriptor>()
//        val processClassOrObject = subroutine is CjClassOrObject || subroutine is CjSecondaryConstructor
//
//        val initializers = pseudocodeVariablesData.variableInitializers
//        val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true)
//        val blockScopeVariableInfo = pseudocodeVariablesData.blockScopeVariableInfo
//
//        val reportedDiagnosticMap = hashMapOf<Instruction, DiagnosticFactory<*>>()
//
//        pseudocode.traverse(TraversalOrder.FORWARD, initializers) { instruction: Instruction,
//                                                                    enterData: VariableInitReadOnlyControlFlowInfo,
//                                                                    exitData: VariableInitReadOnlyControlFlowInfo ->
//
//            val ctxt =
//                VariableInitContext(instruction, reportedDiagnosticMap, enterData, exitData, blockScopeVariableInfo)
//            if (ctxt.variableDescriptor == null) return@traverse
//            if (instruction is ReadValueInstruction) {
//                val element = instruction.element
//                if (PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, trace.bindingContext)
//                    && declaredVariables.contains(ctxt.variableDescriptor)
//                ) {
//                    checkIsInitialized(ctxt, element, varWithUninitializedErrorGenerated)
//                }
//                return@traverse
//            }
//            if (instruction !is WriteValueInstruction) return@traverse
//            val element = instruction.lValue as? CjExpression ?: return@traverse
//            var error = checkValReassignment(
//                ctxt, element, instruction,
//                varWithValReassignErrorGenerated
//            )
//            if (!error && processClassOrObject) {
//                error = checkAssignmentBeforeDeclaration(ctxt, element)
//            }
//            if (!error && processClassOrObject) {
//                checkInitializationForCustomSetter(ctxt, element)
//            }
//        }
    }

    private fun reportUnreachableCode(unreachableCode: UnreachableCode) {
        for (element in unreachableCode.elements) {
            trace.report(
                UNREACHABLE_CODE.on(
                    element,
                    unreachableCode.reachableElements,
                    unreachableCode.unreachableElements
                )
            )
        }
    }

    private fun collectUnreachableCode(): UnreachableCode {
        val reachableElements = hashSetOf<CjElement>()
        val unreachableElements = hashSetOf<CjElement>()
        for (instruction in pseudocode.instructionsIncludingDeadCode) {
            if (instruction !is CjElementInstruction
                || instruction is LoadUnitValueInstruction
                || instruction is MergeInstruction
                || instruction is MagicInstruction && instruction.synthetic
            )
                continue

            val element = instruction.element

            if (instruction is JumpInstruction) {
                val isJumpElement = element is CjBreakExpression
                        || element is CjContinueExpression
                        || element is CjReturnExpression
                        || element is CjThrowExpression
                if (!isJumpElement) continue
            }

            if (instruction.dead) {
                unreachableElements.add(element)
            } else {
                reachableElements.add(element)
            }
        }
        return UnreachableCodeImpl(reachableElements, unreachableElements)
    }

    override fun checkFunction(expectedReturnType: CangJieType?) {
        val unreachableCode = collectUnreachableCode()
        reportUnreachableCode(unreachableCode)


        if (subroutine is CjFunctionLiteral) return

        checkDefiniteReturn(expectedReturnType ?: NO_EXPECTED_TYPE, unreachableCode)

        markAndCheckTailCalls()
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Tail calls

    private fun markAndCheckTailCalls() {
        val subroutineDescriptor = trace.get(DECLARATION_TO_DESCRIPTOR, subroutine) as? FunctionDescriptor ?: return

//        markAndCheckRecursiveTailCalls(subroutineDescriptor)
    }

}
