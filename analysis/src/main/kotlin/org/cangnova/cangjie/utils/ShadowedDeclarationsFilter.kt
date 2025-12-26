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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.createExpressionByPattern
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.scopes.ExplicitImportsScope
import org.cangnova.cangjie.resolve.scopes.addImportingScope
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.Receiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTraceFilter.Companion.NO_DIAGNOSTICS
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.binding.getDataFlowInfoBefore
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.types.TypeUtils
import kotlin.reflect.KProperty

class ShadowedDeclarationsFilter(
    private val bindingContext: BindingContext,
    private val resolutionFacade: ResolutionFacade,
    private val context: PsiElement,
    private val explicitReceiverValue: ReceiverValue?
) {
    companion object {
        fun create(
            bindingContext: BindingContext,
            resolutionFacade: ResolutionFacade,
            context: PsiElement,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>
        ): ShadowedDeclarationsFilter? {
            val receiverExpression = when (callTypeAndReceiver) {
                is CallTypeAndReceiver.DEFAULT -> null
                is CallTypeAndReceiver.DOT -> callTypeAndReceiver.receiver
                is CallTypeAndReceiver.SAFE -> callTypeAndReceiver.receiver
//                is CallTypeAndReceiver.SUPER_MEMBERS -> callTypeAndReceiver.receiver
//                is CallTypeAndReceiver.INFIX -> callTypeAndReceiver.receiver
//                is CallTypeAndReceiver.TYPE, is CallTypeAndReceiver.ANNOTATION -> null // need filtering of classes with the same FQ-name
                else -> return null // TODO: support shadowed declarations filtering for callable references
            }

            val explicitReceiverValue = receiverExpression?.let {
                val type = bindingContext.getType(it) ?: return null
                ExpressionReceiver.create(it, type, bindingContext)
            }
            return ShadowedDeclarationsFilter(bindingContext, resolutionFacade, context, explicitReceiverValue)
        }
    }

    private val psiFactory = CjPsiFactory(resolutionFacade.project)
    private val dummyExpressionFactory = DummyExpressionFactory(psiFactory)

    fun <TDescriptor : DeclarationDescriptor> filter(declarations: Collection<TDescriptor>): Collection<TDescriptor> =
        declarations.groupBy { signature(it) }.values.flatMap { group -> filterEqualSignatureGroup(group) }

    fun <TDescriptor : DeclarationDescriptor> createNonImportedDeclarationsFilter(
        importedDeclarations: Collection<DeclarationDescriptor>,
        allowExpectedDeclarations: Boolean,
    ): (Collection<TDescriptor>) -> Collection<TDescriptor> {
        val importedDeclarationsSet = if (allowExpectedDeclarations) {
            importedDeclarations.asSequence().flatMap {
//                if (it is MemberDescriptor && it.isActual) {
//                    sequenceOf(it) + it.findExpects().asSequence()
//                } else {
                sequenceOf(it)
//                }
            }.toSet()
        } else {
            importedDeclarations.toSet()
        }

        val importedDeclarationsBySignature = importedDeclarationsSet.groupBy { signature(it) }

        return filter@{ declarations ->
            // optimization
            if (declarations.isEmpty() || declarations.size == 1 && importedDeclarationsBySignature[signature(
                    declarations.single()
                )] == null
            ) return@filter declarations

            val nonImportedDeclarations = declarations.filter { it !in importedDeclarationsSet }

            val notShadowed = HashSet<DeclarationDescriptor>()
            // same signature non-imported declarations from different packages do not shadow each other
            for ((pair, group) in nonImportedDeclarations.groupBy { signature(it) to packageName(it) }) {
                val imported = importedDeclarationsBySignature[pair.first]
                val all = if (imported != null) group + imported else group
                notShadowed.addAll(filterEqualSignatureGroup(all, descriptorsToImport = group))
            }
            declarations.filter { it in notShadowed }
        }
    }

    private fun signature(descriptor: DeclarationDescriptor): Any = when (descriptor) {
        is SimpleFunctionDescriptor -> FunctionSignature(descriptor)
        is VariableDescriptor -> descriptor.name
        is ClassDescriptor -> descriptor.importableFqName ?: descriptor
        else -> descriptor
    }

    private fun packageName(descriptor: DeclarationDescriptor) = descriptor.importableFqName?.parent()

    private fun <TDescriptor : DeclarationDescriptor> filterEqualSignatureGroup(
        descriptors: Collection<TDescriptor>,
        descriptorsToImport: Collection<TDescriptor> = emptyList()
    ): Collection<TDescriptor> {
        if (descriptors.size == 1) return descriptors

        val first = descriptors.firstOrNull {
            it is ClassDescriptor || it is ConstructorDescriptor || it is CallableDescriptor && !it.name.isSpecial
        } ?: return descriptors

        if (first is ClassDescriptor) { // for classes with the same FQ-name we simply take the first one
            return listOf(first)
        }

        // Optimization: if the descriptors are structurally equivalent then there is no need to run resolve.
        // This can happen when the classpath contains multiple copies of the same library.
        if (descriptors.all {
                DescriptorEquivalenceForOverrides.areEquivalent(
                    first,
                    it,
                    allowCopiesFromTheSameDeclaration = true
                )
            }) {
            return listOf(first)
        }

        val isFunction = first is FunctionDescriptor
        val name = when (first) {
            is ConstructorDescriptor -> first.constructedClass.name
            else -> first.name
        }
        val parameters = (first as CallableDescriptor).valueParameters

        val dummyArgumentExpressions = dummyExpressionFactory.createDummyExpressions(parameters.size)

        val bindingTrace = DelegatingBindingTrace(
            bindingContext, "Temporary trace for filtering shadowed declarations",
            filter = NO_DIAGNOSTICS
        )
        for ((expression, parameter) in dummyArgumentExpressions.zip(parameters)) {
            bindingTrace.recordType(expression, /*parameter.varargElementType ?:*/ parameter.type)
            bindingTrace.record(BindingContext.PROCESSED, expression, true)
        }

//        val firstVarargIndex = parameters.withIndex().firstOrNull { it.value.varargElementType != null }?.index
        val useNamedFromIndex =/*
            if (firstVarargIndex != null && firstVarargIndex != parameters.lastIndex) firstVarargIndex else*/
            parameters.size

        class DummyArgument(val index: Int) : ValueArgument {
            private val expression = dummyArgumentExpressions[index]

            private val argumentName: ValueArgumentName? = if (isNamed()) {
                object : ValueArgumentName {
                    override val asName = parameters[index].name
                    override val referenceExpression = null
                }
            } else {
                null
            }

            override fun getArgumentExpression() = expression
            override fun isNamed() = index >= useNamedFromIndex
            override fun getArgumentName() = argumentName
            override fun asElement() = expression
            override fun getSpreadElement() = null
            override fun isExternal() = false
        }

        val arguments = ArrayList<DummyArgument>()
        for (i in parameters.indices) {
            arguments.add(DummyArgument(i))
        }

        val newCall = object : Call {

            //val arguments = parameters.indices.map { DummyArgument(it) }
            val callee = psiFactory.createExpressionByPattern("$0", name, reformat = false)
            override var noValueArgument: Boolean  = false
            override var noTypeParameter: Boolean = false

            override val callOperationNode: ASTNode? = null
            override val explicitReceiver: Receiver? = explicitReceiverValue
            override val dispatchReceiver: ReceiverValue? = null
            override val calleeExpression: CjExpression = callee
            override val valueArgumentList: CjValueArgumentList? = null
            override val valueArguments: List<ValueArgument> = arguments
            override val functionLiteralArguments = emptyList<LambdaArgument>()
            override val typeArguments = emptyList<CjTypeProjection>()
            override val typeArgumentList: CjTypeArgumentList? = null
            override val callElement: CjElement = callee
            override val callType: Call.CallType = Call.CallType.DEFAULT
        }

        var scope = context.getResolutionScope(bindingContext, resolutionFacade)

        if (descriptorsToImport.isNotEmpty()) {
            scope = scope.addImportingScope(ExplicitImportsScope(descriptorsToImport))
        }

        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(context)
        val context = BasicCallResolutionContext.create(
            bindingTrace, scope, newCall, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
            ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            false, resolutionFacade.languageVersionSettings,
            resolutionFacade.dataFlowValueFactory
        )

        @OptIn(FrontendInternals::class)
        val callResolver = resolutionFacade.frontendService<CallResolver>()
        val results =
            if (isFunction) callResolver.resolveFunctionCall(context) else callResolver.resolveSimpleVariable(context)
        val resultingDescriptors = results.resultingCalls.map { it.resultingDescriptor }
        val resultingOriginals = resultingDescriptors.mapTo(HashSet<DeclarationDescriptor>()) {
            it.original

        }
        val filtered = descriptors.filter { candidateDescriptor ->
            candidateDescriptor.original in resultingOriginals /* optimization */ && resultingDescriptors.any {
                descriptorsEqualWithSubstitution(
                    it,
                    candidateDescriptor
                )
            }
        }

        // Something went wrong, none of our declarations among resolve candidates, let's not filter anything
        return filtered.ifEmpty { descriptors }
    }

    private class DummyExpressionFactory(val factory: CjPsiFactory) {
        private val expressions = ArrayList<CjExpression>()

        fun createDummyExpressions(count: Int): List<CjExpression> {
            while (expressions.size < count) {
                expressions.add(factory.createExpression("dummy"))
            }
            return expressions.take(count)
        }
    }

    private class FunctionSignature(val function: FunctionDescriptor) {
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is FunctionSignature) return false
            if (function.name != other.function.name) return false
            val parameters1 = function.valueParameters
            val parameters2 = other.function.valueParameters
            if (parameters1.size != parameters2.size) return false
            for (i in parameters1.indices) {
                val p1 = parameters1[i]
                val p2 = parameters2[i]
//                if (p1.varargElementType != p2.varargElementType) return false // both should be vararg or or both not
                if (p1.type != p2.type) return false
            }

            val typeParameters1 = function.typeParameters
            val typeParameters2 = other.function.typeParameters
            if (typeParameters1.size != typeParameters2.size) return false
            for (i in typeParameters1.indices) {
                val t1 = typeParameters1[i]
                val t2 = typeParameters2[i]
                if (t1.upperBounds != t2.upperBounds) return false
            }
            return true
        }

        override fun hashCode() = function.name.hashCode() * 17 + function.valueParameters.size
    }
}
