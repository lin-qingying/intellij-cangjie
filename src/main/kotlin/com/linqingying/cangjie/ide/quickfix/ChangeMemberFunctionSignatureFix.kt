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

package com.linqingying.cangjie.ide.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.ide.IdeDescriptorRenderers
import com.linqingying.cangjie.ide.ShortenReferences
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.typeRefHelpers.setReceiverTypeReference
import com.linqingying.cangjie.renderer.ClassifierNamePolicy
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.findMemberWithMaxVisibility
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.setSingleOverridden
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.util.supertypes
import com.linqingying.cangjie.utils.executeWriteCommand
import java.util.*

class ChangeMemberFunctionSignatureFix private constructor(
    element: CjNamedFunction,
    private val signatures: List<Signature>
) : CangJieQuickFixAction<CjNamedFunction>(element) {

    init {
        assert(signatures.isNotEmpty())
    }

    private class Signature(function: FunctionDescriptor) {
        val sourceCode = SIGNATURE_SOURCE_RENDERER.render(function)
        val preview = SIGNATURE_PREVIEW_RENDERER.render(function)

        companion object {
            private val SIGNATURE_SOURCE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
                defaultParameterValueRenderer = null
            }

            private val SIGNATURE_PREVIEW_RENDERER = DescriptorRenderer.withOptions {
                typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
                withDefinedIn = false
                modifiers = emptySet()
                classifierNamePolicy = ClassifierNamePolicy.SHORT
                unitReturnType = false
                defaultParameterValueRenderer = null
            }
        }
    }

    companion object : CangJieSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = diagnostic.psiElement as? CjNamedFunction ?: return null
            val signatures = computePossibleSignatures(function)
            if (signatures.isEmpty()) return null
            return ChangeMemberFunctionSignatureFix(function, signatures)
        }

        /**
         * Computes all the signatures a 'functionElement' could be changed to in order to remove NOTHING_TO_OVERRIDE error.
         */
        private fun computePossibleSignatures(functionElement: CjNamedFunction): List<Signature> {
            if (functionElement.valueParameterList == null) {
                // we won't be able to modify its signature
                return emptyList()
            }

            val functionDescriptor =
                functionElement.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return emptyList()
            val superFunctions = getPossibleSuperFunctionsDescriptors(functionDescriptor)

            return superFunctions
                .asSequence()
                .filter { it.kind.isReal }
                .map { signatureToMatch(functionDescriptor, it) }
                .distinctBy { it.sourceCode }
                .sortedBy { it.preview }
                .toList()
        }

        /**
         * Changes function's signature to match superFunction's signature. Returns new descriptor.
         */
        private fun signatureToMatch(function: FunctionDescriptor, superFunction: FunctionDescriptor): Signature {
            val superParameters = superFunction.valueParameters
            val parameters = function.valueParameters
            val newParameters = superParameters.toMutableList()

            // Parameters in superFunction, which are matched in new function signature:
            val matched = BitSet(superParameters.size)
            // Parameters in this function, which are used in new function signature:
            val used = BitSet(superParameters.size)

            matchParameters(ParameterChooser.MatchNames, superParameters, parameters, newParameters, matched, used)
            matchParameters(ParameterChooser.MatchTypes, superParameters, parameters, newParameters, matched, used)

            val newFunction = replaceFunctionParameters(
                superFunction.copy(
                    function.containingDeclaration,
                    Modality.OPEN,
                    findMemberWithMaxVisibility(listOf(superFunction, function)).visibility,
                    CallableMemberDescriptor.Kind.DELEGATION,
                    /* copyOverrides = */ true
                ),
                newParameters
            )
            newFunction.setSingleOverridden(superFunction)

            return Signature(newFunction)
        }

        /**
         * Match function's parameters with super function's parameters using parameterChooser.
         * Doesn't have to preserve ordering, parameter names or types.
         * @param superParameters - super function's parameters
         * *
         * @param parameters - function's parameters
         * *
         * @param newParameters - new parameters (may be modified by this function)
         * *
         * @param matched - true iff this parameter in super function is matched by some parameter in function (may be modified by this function)
         * *
         * @param used - true iff this parameter in function is used to match some parameter in super function (may be modified by this function)
         */
        private fun matchParameters(
            parameterChooser: ParameterChooser,
            superParameters: List<ValueParameterDescriptor>,
            parameters: List<ValueParameterDescriptor>,
            newParameters: MutableList<ValueParameterDescriptor>,
            matched: BitSet,
            used: BitSet
        ) {
            for (superParameter in superParameters) {
                if (!matched[superParameter.index]) {
                    for (parameter in parameters) {
                        val choice = parameterChooser.choose(parameter, superParameter)
                        if (choice != null && !used[parameter.index]) {
                            used[parameter.index] = true
                            matched[superParameter.index] = true
                            newParameters[superParameter.index] = choice
                            break
                        }
                    }
                }
            }
        }

        /**
         * Returns all open functions in superclasses which have the same name as 'functionDescriptor' (but possibly
         * different parameters/return type).
         */
        private fun getPossibleSuperFunctionsDescriptors(functionDescriptor: FunctionDescriptor): List<FunctionDescriptor> {
            val containingClass = functionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            val name = functionDescriptor.name
            return containingClass.defaultType.supertypes()
                .flatMap { supertype -> supertype.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE) }
                .filter { it.kind.isReal && it.isOverridable }
        }

        /**
         * Returns function's copy with new parameter list.
         * Note that parameters may belong to other methods or have incorrect "index" property -- it will be fixed by this function.
         */
        private fun replaceFunctionParameters(
            function: FunctionDescriptor,
            newParameters: List<ValueParameterDescriptor>
        ): FunctionDescriptor {
            val descriptor = SimpleFunctionDescriptorImpl.create(
                function.containingDeclaration,
                function.annotations,
                function.name,
                function.kind,
                SourceElement.NO_SOURCE
            )

            val parameters = newParameters.asSequence().withIndex().map { (index, parameter) ->
                ValueParameterDescriptorImpl(
                    descriptor,
                    null,
                    index,
                    parameter.annotations,
                    parameter.name,
                    parameter.isNamed,
                    parameter.returnType!!,
                    parameter.declaresDefaultValue(),
                    /*   parameter.isCrossinline, parameter.isNoinline, parameter.varargElementType, */
                    SourceElement.NO_SOURCE
                )
            }.toList()

            return descriptor.apply {
                initialize(
                    function.extensionReceiverParameter?.copy(this),
                    function.dispatchReceiverParameter,
                    function.contextReceiverParameters.map { it.copy(this) },
                    function.typeParameters,
                    parameters,
                    function.returnType,
                    function.modality,
                    function.visibility
                )
                isOperator = function.isOperator
//                isInfix = function.isInfix
//                isExternal = function.isExternal
//                isInline = function.isInline
//                isTailrec = function.isTailrec
//                isSuspend = function.isSuspend
            }
        }
    }

    override fun getText(): String {
        val single = signatures.singleOrNull()
        return when {
            single != null -> CangJieBundle.message("fix.change.signature.function.text", single.preview)
            else -> CangJieBundle.message("fix.change.signature.function.text.generic")
        }
    }

    override fun getFamilyName() = CangJieBundle.message("fix.change.signature.function.family")

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            MyAction(project, editor, element, signatures).execute()
        }
    }

    /** Helper interface for matchParameters(..) method.  */
    private interface ParameterChooser {
        /**
         * Checks if 'parameter' may be used to match 'superParameter'.
         * If so, returns (possibly modified) descriptor to be used as the new parameter.
         * If not, returns null.
         */
        fun choose(
            parameter: ValueParameterDescriptor,
            superParameter: ValueParameterDescriptor
        ): ValueParameterDescriptor?

        object MatchNames : ParameterChooser {
            override fun choose(
                parameter: ValueParameterDescriptor,
                superParameter: ValueParameterDescriptor
            ): ValueParameterDescriptor? {
                return superParameter.takeIf { parameter.name == superParameter.name }
            }
        }

        object MatchTypes : ParameterChooser {
            override fun choose(
                parameter: ValueParameterDescriptor,
                superParameter: ValueParameterDescriptor
            ): ValueParameterDescriptor? {
                // TODO: support for generic functions
                return if (CangJieTypeChecker.DEFAULT.equalTypes(parameter.type, superParameter.type)) {
                    superParameter.copy(parameter.containingDeclaration, parameter.name, parameter.index)
                } else {
                    null
                }
            }
        }

    }

    private class MyAction(
        private val project: Project,
        private val editor: Editor?,
        private val function: CjNamedFunction,
        private val signatures: List<Signature>
    ) {
        fun execute() {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            if (!function.isValid || signatures.isEmpty()) return

            if (signatures.size == 1 || editor == null || !editor.component.isShowing) {
                changeSignature(signatures.first())
            } else {
                chooseSignatureAndChange()
            }
        }

        private val signaturePopup: BaseListPopupStep<Signature>
            get() {
                return object : BaseListPopupStep<Signature>(
                    CangJieBundle.message("fix.change.signature.function.popup.title"),
                    signatures
                ) {
                    override fun isAutoSelectionEnabled() = false

                    override fun onChosen(selectedValue: Signature, finalChoice: Boolean): PopupStep<*>? {
                        if (finalChoice) {
                            changeSignature(selectedValue)
                        }
                        return FINAL_CHOICE
                    }

                    override fun getIconFor(aValue: Signature) = AllIcons.Nodes.Function
                    override fun getTextFor(aValue: Signature) = aValue.preview
                }
            }

        private fun changeSignature(signature: Signature) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            project.executeWriteCommand(CangJieBundle.message("fix.change.signature.function.family")) {
                val patternFunction = CjPsiFactory(project).createFunction(signature.sourceCode)

//                if (patternFunction.hasModifier(CjTokens.SUSPEND_KEYWORD)) {
//                    function.addModifier(CjTokens.SUSPEND_KEYWORD)
//                }

                val newTypeRef = function.setTypeReference(patternFunction.typeReference)
                if (newTypeRef != null) {
                    ShortenReferences.DEFAULT.process(newTypeRef)
                }

//                patternFunction.valueParameters.forEach { param ->
//                    param.annotationEntries.forEach { a ->
//                        a.typeReference?.run {
//                            val fqName = FqName(this.text)
//                            if (fqName in (NULLABLE_ANNOTATIONS + NOT_NULL_ANNOTATIONS)) a.delete()
//                        }
//                    }
//                }

                val newParameterList = patternFunction.valueParameterList?.let {
                    function.valueParameterList?.replace(it)
                } as CjParameterList
                ShortenReferences.DEFAULT.process(newParameterList)

                val patternFunctionReceiver = patternFunction.receiverTypeReference
                if (patternFunctionReceiver == null) {
                    if (function.receiverTypeReference != null) {
                        function.setReceiverTypeReference(null)
                    }
                } else {
                    function.setReceiverTypeReference(patternFunction.receiverTypeReference)?.let {
                        ShortenReferences.DEFAULT.process(it)
                    }
                }

                val patternTypeParameterList = patternFunction.typeParameterList
                if (patternTypeParameterList != null) {
                    ShortenReferences.DEFAULT.process(
                        (if (function.typeParameterList != null) function.typeParameterList?.replace(
                            patternTypeParameterList
                        )
                        else function.addAfter(patternTypeParameterList, function.funKeyword)) as CjTypeParameterList
                    )
                } else function.typeParameterList?.delete()
            }
        }

        private fun chooseSignatureAndChange() {
            JBPopupFactory.getInstance().createListPopup(signaturePopup).showInBestPositionFor(editor!!)
        }
    }
}
