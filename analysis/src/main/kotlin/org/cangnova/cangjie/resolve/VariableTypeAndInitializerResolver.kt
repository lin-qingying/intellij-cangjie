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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.impl.VariableDescriptorWithInitializerImpl
import org.cangnova.cangjie.diagnostics.infos.errors.NON_CONST_LET_USED_IN_CONSTANT_EXPRESSION
import org.cangnova.cangjie.diagnostics.infos.errors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjVariableDeclaration
import org.cangnova.cangjie.resolve.DescriptorResolver.Companion.transformAnonymousTypeIfNeeded
import org.cangnova.cangjie.resolve.binding.BindingTrace

import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor

class VariableTypeAndInitializerResolver(

    private val storageManager: StorageManager,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeResolver: TypeResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
//    private val delegatedPropertyResolver: DelegatedPropertyResolver,
    private val wrappedTypeFactory: WrappedTypeFactory,
    private val typeApproximator: TypeApproximator,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer,
    private val languageVersionSettings: LanguageVersionSettings,
    private val anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>

) {
    companion object {
        
        fun getTypeForPropertyWithoutReturnType(property: String): SimpleType =
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_PROPERTY, property)

        
        fun getTypeForVariableWithoutReturnType(property: String): SimpleType =
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_VARIABLE, property)
    }

    fun resolveType(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scopeForInitializer: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): CangJieType {
        resolveTypeOptional(
            variableDescriptor, scopeForInitializer, variable, dataFlowInfo, inferenceSession, trace, local
        )?.let { return it }

        if (local) {
            trace.report(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.on(variable))
        }

        return getTypeForVariableWithoutReturnType(variableDescriptor.name.asString())
    }

    fun setConstantForVariableIfNeeded(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scope: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        variableType: CangJieType,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ) {
        if (!variable.hasInitializer() || variable.isVar) return
        variableDescriptor.setCompileTimeInitializerFactory {
            storageManager.createRecursionTolerantNullableLazyValue(
                computeInitializer@{
                    if (!DescriptorUtils.shouldRecordInitializerForProperty(
                            variableDescriptor,
                            variableType
                        )
                    ) return@computeInitializer null

                    val initializer = variable.initializer
                    val initializerType =
                        expressionTypingServices.safeGetType(
                            scope,
                            initializer!!,
                            variableType,
                            dataFlowInfo,
                            inferenceSession,
                            trace
                        )
                    val constant = constantExpressionEvaluator.evaluateExpression(initializer, trace, initializerType)
                        ?: return@computeInitializer null

                    if (constant.usesNonConstValAsConstant && variableDescriptor.isConst) {
                        trace.report(NON_CONST_LET_USED_IN_CONSTANT_EXPRESSION.on(initializer))
                    }

                    constant.toConstantValue(initializerType)
                },
                null
            )
        }
    }

//    private fun resolveDelegatedPropertyType(
//        property: CjVariableDeclaration,
//        variableDescriptor: PropertyDescriptorWithAccessors,
//        scopeForInitializer: LexicalScope,
//        dataFlowInfo: DataFlowInfo,
//        inferenceSession: InferenceSession,
//        trace: BindingTrace,
//        local: Boolean
//    ) = wrappedTypeFactory.createRecursionIntolerantDeferredType(trace) {
//        val delegateExpression = property.delegateExpression!!
//        val type = delegatedPropertyResolver.resolveDelegateExpression(
//            delegateExpression, property, variableDescriptor, scopeForInitializer, trace, dataFlowInfo, inferenceSession
//        )
//
//        val getterReturnType = delegatedPropertyResolver.getGetValueMethodReturnType(
//            variableDescriptor, delegateExpression, type, trace, scopeForInitializer, dataFlowInfo, inferenceSession
//        )
//
//        val delegatedType = getterReturnType?.let { approximateType(it, local) }
//            ?: ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_DELEGATION, delegateExpression.text)
//
//        transformAnonymousTypeIfNeeded(
//            variableDescriptor, property, delegatedType, trace, anonymousTypeTransformers, languageVersionSettings
//        )
//    }


    fun resolveTypeOptional(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scopeForInitializer: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): CangJieType? {
        val propertyTypeRef = variable.typeReference
        return when {
            propertyTypeRef != null -> typeResolver.resolveType(scopeForInitializer, propertyTypeRef, trace, true)


            variable.hasInitializer() -> when {
                !local ->
                    wrappedTypeFactory.createRecursionIntolerantDeferredType(
                        trace
                    ) {
                        PreliminaryDeclarationVisitor.createForDeclaration(
                            variable, trace,
                            expressionTypingServices.languageVersionSettings
                        )
                        val initializerType = resolveInitializerType(
                            scopeForInitializer, variable.initializer!!, dataFlowInfo, inferenceSession, trace, local
                        )
                        transformAnonymousTypeIfNeeded(
                            variableDescriptor,
                            variable,
                            initializerType,
                            trace,
                            anonymousTypeTransformers,
                            languageVersionSettings
                        )
                    }

                else -> resolveInitializerType(
                    scopeForInitializer,
                    variable.initializer!!,
                    dataFlowInfo,
                    inferenceSession,
                    trace,
                    local
                )
            }

            else -> null
        }
    }

    private fun resolveInitializerType(
        scope: LexicalScope,
        initializer: CjExpression,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): CangJieType {
        val inferredType = expressionTypingServices.safeGetType(
            scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession, trace
        )
        val approximatedType = approximateType(inferredType, local)
        return declarationReturnTypeSanitizer.sanitizeReturnType(
            approximatedType,
            wrappedTypeFactory,
            trace,
            languageVersionSettings
        )
    }

    private fun approximateType(type: CangJieType, local: Boolean): UnwrappedType =
        typeApproximator.approximateDeclarationType(type, local)
//    private fun resolveInitializerType(
//        scope: LexicalScope,
//        initializer: CjExpression,
//        dataFlowInfo: DataFlowInfo,
//        inferenceSession: InferenceSession,
//        trace: BindingTrace,
//        local: Boolean
//    ): CangJieType {
//        val inferredType = expressionTypingServices.safeGetType(
//            scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession, trace
//        )
//        val approximatedType = approximateType(inferredType, local)
//        return declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings)
//    }
}
