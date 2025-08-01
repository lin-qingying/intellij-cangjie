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


package cn.cangnova.cangjie.resolve.controlFlow.pseudocode

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.config.LanguageVersionSettingsImpl
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import cn.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.psi.*


import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.BindingContextUtils.variableDescriptorForDeclaration
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall
import cn.cangnova.cangjie.resolve.calls.util.hasThisOrNoDispatchReceiver
import cn.cangnova.cangjie.resolve.controlFlow.ControlFlowProcessor
import cn.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import cn.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.VariableDeclarationInstruction
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.slicedMap.ReadOnlySlice
import cn.cangnova.cangjie.utils.slicedMap.WritableSlice

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
            val element: CjElement = instruction.element
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
                instruction.variableDeclarationElement
            return variableDescriptorForDeclaration(
                bindingContext.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                )
            )
        } else if (instruction is AccessValueInstruction) {
            val target: AccessTarget = instruction.target
            if (target is AccessTarget.Declaration) {
                return target.descriptor
            } else if (target is AccessTarget.Call) {
                return variableDescriptorForDeclaration(
                    target.resolvedCall.getResultingDescriptor()
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
