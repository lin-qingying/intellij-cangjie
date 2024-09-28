
package com.huawei.cangjie.resolve.controlFlow.pseudocode

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.config.LanguageVersionSettingsImpl
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.descriptors.impl.LocalVariableDescriptor
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.psi.*


import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContextUtils.variableDescriptorForDeclaration
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.util.hasThisOrNoDispatchReceiver
import com.huawei.cangjie.resolve.controlFlow.ControlFlowProcessor
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special.VariableDeclarationInstruction
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice
import com.huawei.cangjie.utils.slicedMap.WritableSlice
import com.intellij.openapi.project.Project

object PseudocodeUtil {
    fun generatePseudocode(declaration: CjDeclaration, bindingContext: BindingContext): Pseudocode {
        return generatePseudocode(declaration, bindingContext, LanguageVersionSettingsImpl.DEFAULT)
    }

    fun generatePseudocode(
        declaration: CjDeclaration,
        bindingContext: BindingContext,
        languageVersionSettings: LanguageVersionSettings
    ): Pseudocode {
        val mockTrace: BindingTrace = object : BindingTrace  {
            override val bindingContext: BindingContext
                get() = bindingContext



            override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
            }

            override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {

            }

            override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
                return bindingContext.get(slice, key)
            }

            override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
                return bindingContext.getKeys(slice)
            }

            override fun getType(expression: CjExpression): CangJieType? {
                return bindingContext.getType(expression)
            }

            override fun recordType(expression: CjExpression, type: CangJieType?) {
            }

            override fun report(diagnostic: Diagnostic) {
            }

            override fun wantsDiagnostics(): Boolean {
                return false
            }


        }
        return ControlFlowProcessor(mockTrace, languageVersionSettings).generatePseudocode(declaration)
    }

    fun extractVariableDescriptorFromReference(
        instruction: Instruction,
        bindingContext: BindingContext
    ): VariableDescriptor? {
        if (instruction is AccessValueInstruction) {
            val element: CjElement = (instruction as AccessValueInstruction).element
            if (element is CjDeclaration) return null
            val descriptor: VariableDescriptor? = extractVariableDescriptorIfAny(instruction, bindingContext)
//            if (descriptor is PropertyImportedFromObject) {
//                return (descriptor as PropertyImportedFromObject).getCallableFromObject()
//            }
            return descriptor
        }
        return null
    }


    fun extractVariableDescriptorIfAny(
        instruction: Instruction,
        bindingContext: BindingContext
    ): VariableDescriptor? {
        if (instruction is VariableDeclarationInstruction) {
            val declaration: CjDeclaration =
                (instruction as VariableDeclarationInstruction).variableDeclarationElement
            return variableDescriptorForDeclaration(
                bindingContext.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                )
            )
        } else if (instruction is AccessValueInstruction) {
            val target: AccessTarget = (instruction as AccessValueInstruction).target
            if (target is AccessTarget.Declaration) {
                return (target as AccessTarget.Declaration).descriptor
            } else if (target is AccessTarget.Call) {
                return variableDescriptorForDeclaration(
                    (target as AccessTarget.Call).resolvedCall.getResultingDescriptor()
                )
            }
        }
        return null
    }

    // When deal with constructed object (not this) treat it like it's fully initialized
    // Otherwise (this or access with empty receiver) access instruction should be handled as usual
    fun isThisOrNoDispatchReceiver(
        instruction: AccessValueInstruction,
        bindingContext: BindingContext
    ): Boolean {
        if (instruction.receiverValues.isEmpty()) {
            return true
        }
        val accessTarget: AccessTarget = instruction.target
        if (accessTarget is AccessTarget.BlackBox) return false
        assert(accessTarget is AccessTarget.Call) { "AccessTarget.Declaration has no receivers and it's not BlackBox, so it should be Call" }

        val accessResolvedCall: ResolvedCall<*> = (accessTarget as AccessTarget.Call).resolvedCall
        return accessResolvedCall. hasThisOrNoDispatchReceiver(  bindingContext)
    }
}


val Instruction.sideEffectFree: Boolean
    get() = owner.isSideEffectFree(this)

fun Instruction.calcSideEffectFree(): Boolean {
    if (this !is InstructionWithValue) return false
    if (!inputValues.all { it.createdAt?.sideEffectFree == true }) return false

    return when (this) {
        is ReadValueInstruction -> target.let {
            when (it) {
                is AccessTarget.Call -> when (it.resolvedCall.resultingDescriptor) {
                    is LocalVariableDescriptor, is ValueParameterDescriptor, is ReceiverParameterDescriptor -> true
                    else -> false
                }

                else -> when (element) {
                    is CjNamedFunction -> element.name == null
                    is CjConstantExpression, is CjLambdaExpression, is CjStringTemplateExpression -> true
                    else -> false
                }
            }
        }

        is MagicInstruction -> kind.sideEffectFree

        else -> false
    }
}
