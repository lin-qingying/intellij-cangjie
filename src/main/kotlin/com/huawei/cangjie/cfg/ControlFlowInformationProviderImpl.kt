package com.huawei.cangjie.cfg

import com.huawei.cangjie.cfg.pseudocode.Pseudocode
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjElement
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
//        val processClassOrObject = subroutine is KtClassOrObject || subroutine is KtSecondaryConstructor
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
//            val element = instruction.lValue as? KtExpression ?: return@traverse
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

    override fun checkFunction(expectedReturnType: CangJieType?) {
//        TODO("Not yet implemented")
    }
}
