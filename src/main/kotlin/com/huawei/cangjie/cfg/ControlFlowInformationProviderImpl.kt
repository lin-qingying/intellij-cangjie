package com.huawei.cangjie.cfg

import com.huawei.cangjie.cfg.pseudocode.Pseudocode
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.types.CangJieType

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

//    constructor(
//        declaration: CjElement,
//        trace: BindingTrace,
//        languageVersionSettings: LanguageVersionSettings,
////        diagnosticSuppressor: PlatformDiagnosticSuppressor,
////        enumWhenTracker: EnumWhenTracker? = null
//    ) : this(
//        declaration,
//        trace,
//        ControlFlowProcessor(trace, languageVersionSettings).generatePseudocode(declaration),
//        languageVersionSettings,
//        diagnosticSuppressor,
//        enumWhenTracker
//    )

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
    override fun checkDeclaration() {
        markUninitializedVariables()

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
            trace.report(Errors.UNREACHABLE_CODE.on(element, unreachableCode.reachableElements, unreachableCode.unreachableElements))
        }
    }
    private fun collectUnreachableCode(): UnreachableCode {
        val reachableElements = hashSetOf<CjElement>()
        val unreachableElements = hashSetOf<CjElement>()
//        for (instruction in pseudocode.instructionsIncludingDeadCode) {
//            if (instruction !is CjElementInstruction
//                || instruction is LoadUnitValueInstruction
//                || instruction is MergeInstruction
//                || instruction is MagicInstruction && instruction.synthetic
//            )
//                continue
//
//            val element = instruction.element
//
//            if (instruction is JumpInstruction) {
//                val isJumpElement = element is CjBreakExpression
//                        || element is CjContinueExpression
//                        || element is CjReturnExpression
//                        || element is CjThrowExpression
//                if (!isJumpElement) continue
//            }
//
//            if (instruction.dead) {
//                unreachableElements.add(element)
//            } else {
//                reachableElements.add(element)
//            }
//        }
        return UnreachableCodeImpl(reachableElements, unreachableElements)
    }
    override fun checkFunction(expectedReturnType: CangJieType?) {
        val unreachableCode = collectUnreachableCode()
        reportUnreachableCode(unreachableCode)
    }
}
