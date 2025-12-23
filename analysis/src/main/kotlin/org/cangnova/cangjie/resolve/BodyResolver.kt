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

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.descriptors.impl.SyntheticFieldDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.ACCESSOR_PARAMETER_NAME_SHADOWING
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.CONSTRUCTOR_RESOLVED_DELEGATION_CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.TYPE
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.BodiesResolveContext
import org.cangnova.cangjie.resolve.binding.ObservableBindingTrace
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor
import org.cangnova.cangjie.types.expressions.ValueParameterResolver

class BodyResolver(
    project: Project,
    private val bodyResolveCache: BodyResolveCache,
    private val callResolver: CallResolver,
    private val controlFlowAnalyzer: ControlFlowAnalyzer,
    private val declarationsChecker: DeclarationsChecker,
    private val expressionTypingServices: ExpressionTypingServices,
    trace: BindingTrace,
    private val valueParameterResolver: ValueParameterResolver,
    private val builtIns: CangJieBuiltIns,
    private val overloadChecker: OverloadChecker,
    private val languageVersionSettings: LanguageVersionSettings
) {
    private val trace: ObservableBindingTrace = ObservableBindingTrace(trace)

    var hasExtendSource = false

    // 解决扩展的原类污染报错
    private val hasExtendSourceMap = mutableMapOf<CjTypeReference, Boolean>()

    private fun resolveVariableDeclarationBodies(c: BodiesResolveContext) {
        // Member variable
        val processed = mutableSetOf<CjVariable>()

        for ((typeStatement, _) in c.declaredClasses) {
            for (variable in typeStatement.variables) {
                val variableDescriptor = c.variables[variable]
                requireNotNull(variableDescriptor)

                resolveVariable(c, variable, variableDescriptor)
                processed.add(variable)
            }
        }

        // Top-level properties & properties of objects
        for ((variable, variableDescriptor) in c.variables) {
            if (variable in processed) continue
            resolveVariable(c, variable, variableDescriptor)
        }

        for ((variable, variableDescriptors) in c.variablesByPattern) {
            if (variable in processed) continue
            variableDescriptors.forEach { variableDescriptor ->
                resolveVariable(c, variable, variableDescriptor)
            }
        }
    }

    private fun resolveConstructorDelegationCall(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        scope: LexicalScope,
        constructor: CjConstructor<*>,
        descriptor: ClassConstructorDescriptor,
        inferenceSession: InferenceSession?
    ): DataFlowInfo? {
        if (descriptor.containingDeclaration.kind == ClassKind.STRUCT &&
            descriptor.isPrimary && constructor is CjPrimaryConstructor
        ) {
            return DataFlowInfo.EMPTY
        }


        return try {
            val results = callResolver.resolveConstructorDelegationCall(
                trace, scope, outerDataFlowInfo,
                descriptor, constructor.delegationCall, inferenceSession
            )

            if (results?.isSingleResult == true) {
                val resolvedCall = results.resultingCall
                recordConstructorDelegationCall(trace, descriptor, resolvedCall)
                resolvedCall.dataFlowInfoForArguments.resultInfo
            } else null
        } catch (e: NullPointerException) {
            null
        }
    }

    private fun checkPrimaryConstructorIsThis(constructor: CjPrimaryConstructor) {
        constructor.getDelegationCallOrNull()?.let { delegationCall ->
            delegationCall.calleeExpression?.let { callee ->
                if (callee.isThis) {
                    trace.report(INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR.on(callee))
                }
            }
        }
    }

    fun resolvePrimaryConstructorBody(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        constructor: CjPrimaryConstructor,
        descriptor: ClassConstructorDescriptor,
        declaringScope: LexicalScope,
        localContext: ExpressionTypingContext?
    ) {
        checkPrimaryConstructorIsThis(constructor)
        resolveConstructorBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope, localContext)
    }

    fun resolveConstructorBody(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        constructor: CjConstructor<*>,
        descriptor: ClassConstructorDescriptor,
        declaringScope: LexicalScope,
        localContext: ExpressionTypingContext?
    ) {
        ForceResolveUtil.forceResolveAllContents(descriptor.annotations)

        resolveFunctionBody(
            outerDataFlowInfo, trace, constructor, descriptor, declaringScope,
            beforeBlockBody = { headerInnerScope ->
                resolveConstructorDelegationCall(
                    outerDataFlowInfo, trace, headerInnerScope, constructor,
                    descriptor, localContext?.inferenceSession
                )
            },
            headerScopeFactory = { scope ->
                LexicalScopeImpl(
                    scope, descriptor, scope.isOwnerDescriptorAccessibleByLabel, scope.implicitReceiver,
                    scope.contextReceiversGroup, LexicalScopeKind.CONSTRUCTOR_HEADER
                )
            },
            localContext = localContext
        )
    }

    fun resolveEndSecondaryConstructorBody(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        constructor: CjEndSecondaryConstructor,
        descriptor: ClassConstructorDescriptor,
        declaringScope: LexicalScope,
        localContext: ExpressionTypingContext?
    ) {
        resolveConstructorBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope, localContext)
    }

    fun resolveSecondaryConstructorBody(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        constructor: CjSecondaryConstructor,
        descriptor: ClassConstructorDescriptor,
        declaringScope: LexicalScope,
        localContext: ExpressionTypingContext?
    ) {
        resolveConstructorBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope, localContext)
    }

    private fun resolveVariableInitializer(
        outerDataFlowInfo: DataFlowInfo,
        variable: CjVariable,
        variableDescriptor: VariableDescriptor,
        initializer: CjExpression,
        propertyHeader: LexicalScope,
        inferenceSession: InferenceSession?
    ) {
        val propertyDeclarationInnerScope =
            ScopeUtils.makeScopeForVariableInitializer(propertyHeader, variableDescriptor)
        val expectedTypeForInitializer =
            if (variable.typeReference != null) variableDescriptor.type else NO_EXPECTED_TYPE

        if (variableDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(
                propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                outerDataFlowInfo, inferenceSession ?: InferenceSession.default, trace
            )
        }
    }

    private fun resolvePropertyInitializer(
        outerDataFlowInfo: DataFlowInfo,
        property: CjProperty,
        propertyDescriptor: PropertyDescriptor,
        initializer: CjExpression,
        propertyHeader: LexicalScope,
        inferenceSession: InferenceSession?
    ) {
        val propertyDeclarationInnerScope =
            ScopeUtils.makeScopeForPropertyInitializer(propertyHeader, propertyDescriptor)
        val expectedTypeForInitializer =
            if (property.typeReference != null) propertyDescriptor.type else NO_EXPECTED_TYPE

        if (propertyDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(
                propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                outerDataFlowInfo, inferenceSession ?: InferenceSession.default, trace
            )
        }
    }

    private fun createFieldTrackingTrace(propertyDescriptor: PropertyDescriptor): ObservableBindingTrace =
        ObservableBindingTrace(trace).addHandler(REFERENCE_TARGET) { _, expression, descriptor ->
            if (expression is CjSimpleNameExpression && descriptor is SyntheticFieldDescriptor) {
                trace.record(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)
            }
        }

    private fun resolvePropertyAccessors(
        c: BodiesResolveContext,
        property: CjProperty,
        propertyDescriptor: PropertyDescriptor
    ) {
        val fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor)

        val getter = property.getter
        val getterDescriptor = propertyDescriptor.getter

        val forceResolveAnnotations = true
        if (getterDescriptor != null) {
            if (getter != null) {
                val accessorScope = makeScopeForPropertyAccessor(c, getter, propertyDescriptor)
                resolveFunctionBody(
                    c.outerDataFlowInfo,
                    fieldAccessTrackingTrace,
                    getter,
                    getterDescriptor,
                    accessorScope,
                    c.localContext
                )
            }

            if (getter != null || forceResolveAnnotations) {
                ForceResolveUtil.forceResolveAllContents(getterDescriptor.annotations)
            }
        }

        val setter = property.setter
        val setterDescriptor = propertyDescriptor.setter

        if (setterDescriptor != null) {
            if (setter != null) {
                val accessorScope = makeScopeForPropertyAccessor(c, setter, propertyDescriptor)
                resolveFunctionBody(
                    c.outerDataFlowInfo,
                    fieldAccessTrackingTrace,
                    setter,
                    setterDescriptor,
                    accessorScope,
                    c.localContext
                )
            }

            if (setter != null || forceResolveAnnotations) {
                ForceResolveUtil.forceResolveAllContents(setterDescriptor.annotations)
            }
        }
    }

    private fun resolveProperty(c: BodiesResolveContext, property: CjProperty, propertyDescriptor: PropertyDescriptor) {
        computeDeferredType(propertyDescriptor.returnType)
        PreliminaryDeclarationVisitor.createForDeclaration(property, trace, languageVersionSettings)

        resolvePropertyAccessors(c, property, propertyDescriptor)
    }

      fun resolveVariable(c: BodiesResolveContext, variable: CjVariable, variableDescriptor: VariableDescriptor) {
        computeDeferredType(variableDescriptor.returnType)
        PreliminaryDeclarationVisitor.createForDeclaration(variable, trace, languageVersionSettings)

        val initializer = variable.initializer
        val variableHeaderScope =
            ScopeUtils.makeScopeForVariableHeader(getScopeForVariable(c, variable), variableDescriptor)
        val context = c.localContext

        if (initializer != null) {
            resolveVariableInitializer(
                c.outerDataFlowInfo, variable, variableDescriptor,
                initializer, variableHeaderScope, context?.inferenceSession
            )
        }
    }

    private fun resolveSuperTypeEntryLists(c: BodiesResolveContext) {
        for ((typeStatement, descriptor) in c.declaredClasses) {
            val localContext = c.localContext

            resolveSuperTypeEntryList(
                c.outerDataFlowInfo, typeStatement, descriptor,
                descriptor.unsubstitutedPrimaryConstructor,
                descriptor.scopeForConstructorHeaderResolution,
                descriptor.scopeForMemberDeclarationResolution,
                localContext?.inferenceSession
            )
        }
    }

    fun resolveConstructorParameterDefaultValues(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        constructor: CjPrimaryConstructor,
        constructorDescriptor: ConstructorDescriptor,
        declaringScope: LexicalScope,
        inferenceSession: InferenceSession?
    ) {
        val valueParameters = constructor.valueParameters
        val valueParameterDescriptors = constructorDescriptor.valueParameters

        val scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor)

        valueParameterResolver.resolveValueParameters(
            valueParameters, valueParameterDescriptors, scope, outerDataFlowInfo, trace, inferenceSession
        )
    }

    /**
     * 检查主构造函数数量
     * 检查非class struct的意外构造函数
     */
    private fun checkConstructorSize(c: BodiesResolveContext) {
        for ((typeStatement, descriptor) in c.declaredClasses) {
            for (constructor in typeStatement.endSecondaryConstructors) {
                if (typeStatement !is CjClass) {
                    constructor.identifyingElement?.let {
                        trace.report(
                            UNEXPECTED_FINALIZER_IN_BODY_ERROR.on(
                                it,
                                typeStatement.typeName
                            )
                        )
                    }
                } else {
                    if (constructor.valueParameters.isNotEmpty()) {
                        constructor.valueParameterList?.let { trace.report(FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR.on(it)) }
                    }
                }
            }

//            if (typeStatement.isExtend) {
//                for (constructor in typeStatement.constructors) {
//                    trace.report(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR.on(constructor.identifyingElement, "extend"))
//                }
//            }
//
//            if (typeStatement.isEnum) {
//                for (constructor in typeStatement.constructors) {
//                    trace.report(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR.on(constructor.identifyingElement, "enum"))
//                }
//            }

            for (constructor in typeStatement.primaryConstructors) {
                if (constructor.name != descriptor.name.asString()) {
                    // 主构造函数名称不一致
                    trace.report(CONSTRUCTOR_NAME_INCONSISTENCY.on(constructor))
                }

                if (typeStatement.primaryConstructors.size > 1) {
                    trace.report(MULTIPLE_PRIMARY_CONSTRUCTORS.on(constructor, descriptor))
                }
            }
        }
    }

    private fun resolvePrimaryConstructorParameters(c: BodiesResolveContext) {
        for ((constructor, descriptor) in c.primaryConstructors) {
            val declaringScope = c.getDeclaringScope(constructor)
            requireNotNull(declaringScope) { "Declaring scope should be registered before body resolveName" }

            resolvePrimaryConstructorBody(
                c.outerDataFlowInfo,
                trace,
                constructor,
                descriptor,
                declaringScope,
                c.localContext
            )
        }

        if (c.primaryConstructors.isEmpty()) return

        val visitedConstructors = mutableSetOf<ConstructorDescriptor>()
        for ((_, descriptor) in c.primaryConstructors) {
            checkCyclicConstructorDelegationCall(descriptor, visitedConstructors)
        }
    }

    private fun resolveEndSecondaryConstructors(c: BodiesResolveContext) {
        for ((constructor, descriptor) in c.endSecondaryConstructors) {
            val declaringScope = c.getDeclaringScope(constructor)
            requireNotNull(declaringScope) { "Declaring scope should be registered before body resolveName" }

            resolveEndSecondaryConstructorBody(
                c.outerDataFlowInfo,
                trace,
                constructor,
                descriptor,
                declaringScope,
                c.localContext
            )
        }
    }

    private fun resolveSecondaryConstructors(c: BodiesResolveContext) {
        for ((constructor, descriptor) in c.secondaryConstructors) {
            val declaringScope = c.getDeclaringScope(constructor)
            requireNotNull(declaringScope) { "Declaring scope should be registered before body resolveName" }

            resolveSecondaryConstructorBody(
                c.outerDataFlowInfo,
                trace,
                constructor,
                descriptor,
                declaringScope,
                c.localContext
            )
        }

        if (c.secondaryConstructors.isEmpty()) return

        val visitedConstructors = mutableSetOf<ConstructorDescriptor>()
        for ((_, descriptor) in c.secondaryConstructors) {
            checkCyclicConstructorDelegationCall(descriptor, visitedConstructors)
        }
    }

    private fun getDelegatedConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? {
        val call = trace[CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor]
        return if (call == null || !call.status.isSuccess) null else call.resultingDescriptor.original
    }

    private fun reportEachConstructorOnCycle(startConstructor: ConstructorDescriptor) {
        var currentConstructor: ConstructorDescriptor? = startConstructor
        do {
            val constructorToReport = currentConstructor?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) }
            if (constructorToReport != null) {
                val call = (constructorToReport as CjConstructor<*>).delegationCall
                requireNotNull(call?.calleeExpression) {
                    "Callee expression of delegation call should not be null on cycle as there should be explicit 'this' calls"
                }
                trace.report(CYCLIC_CONSTRUCTOR_DELEGATION_CALL.on(call.calleeExpression ?: return))
            }

            currentConstructor = getDelegatedConstructor(currentConstructor ?: return)
            requireNotNull(currentConstructor) { "Delegated constructor should not be null in cycle" }
        } while (startConstructor != currentConstructor)
    }

    private fun checkCyclicConstructorDelegationCall(
        constructorDescriptor: ConstructorDescriptor,
        visitedConstructors: MutableSet<ConstructorDescriptor>
    ) {
        if (constructorDescriptor in visitedConstructors) return

        // if visit constructor that is already in current chain
        // such constructor is on cycle
        val visitedInCurrentChain = mutableSetOf<ConstructorDescriptor>()
        var currentConstructorDescriptor: ConstructorDescriptor? = constructorDescriptor

        while (true) {
            ProgressManager.checkCanceled()

            visitedInCurrentChain.add(currentConstructorDescriptor ?: return)
            val delegatedConstructorDescriptor = getDelegatedConstructor(currentConstructorDescriptor) ?: break

            // if next delegation call is super or primary constructor or already visited
            if (constructorDescriptor.containingDeclaration != delegatedConstructorDescriptor.containingDeclaration ||
                delegatedConstructorDescriptor.isPrimary ||
                delegatedConstructorDescriptor in visitedConstructors
            ) {
                break
            }

            if (delegatedConstructorDescriptor in visitedInCurrentChain) {
                reportEachConstructorOnCycle(delegatedConstructorDescriptor)
                break
            }
            currentConstructorDescriptor = delegatedConstructorDescriptor
        }
        visitedConstructors.addAll(visitedInCurrentChain)
    }

    private fun resolveBehaviorDeclarationBodies(c: BodiesResolveContext) {
        resolveSuperTypeEntryLists(c)
        resolvePropertyDeclarationBodies(c)
        resolveVariableDeclarationBodies(c)
        checkConstructorSize(c)
        resolvePrimaryConstructorParameters(c)
        resolveSecondaryConstructors(c)
        //TODO 析构函数
        resolveEndSecondaryConstructors(c)
        resolveMainFunctionBodies(c)
        resolveFunctionBodies(c)
        resolveMacroBodies(c)
    }

    private fun resolveMainFunctionBodies(c: BodiesResolveContext) {
        for ((declaration, descriptor) in c.mainFunctions) {
            val scope = c.getDeclaringScope(declaration)
            requireNotNull(scope) { "Scope is null: ${declaration.getElementTextWithContext()}" }

            if (!c.topDownAnalysisMode.isLocalDeclarations &&
                bodyResolveCache !is BodyResolveCache.ThrowException &&
                expressionTypingServices.statementFilter != StatementFilter.NONE
            ) {
                bodyResolveCache.resolveMainFunctionBody(declaration).addOwnDataTo(trace, true)
            } else {
                resolveFunctionBody(c.outerDataFlowInfo, trace, declaration, descriptor, scope, c.localContext)
            }
        }
    }

    private fun resolvePropertyDeclarationBodies(c: BodiesResolveContext) {
        // Member properties
        val processed = mutableSetOf<CjProperty>()

        for ((typeStatement, _) in c.declaredClasses) {
            if (typeStatement is CjEnumConstructor) continue

            for (property in typeStatement.properties) {
                val propertyDescriptor = c.properties[property]
                requireNotNull(propertyDescriptor)

                resolveProperty(c, property, propertyDescriptor)
                processed.add(property)
            }
        }

        // Top-level properties & properties of objects
        for ((property, propertyDescriptor) in c.properties) {
            if (property in processed) continue
            resolveProperty(c, property, propertyDescriptor)
        }
    }

    private fun resolveMacroBodies(c: BodiesResolveContext) {
        for ((declaration, descriptor) in c.macros) {
            val scope = c.getDeclaringScope(declaration)
            requireNotNull(scope) { "Scope is null: ${declaration.getElementTextWithContext()}" }

            if (!c.topDownAnalysisMode.isLocalDeclarations &&
                bodyResolveCache !is BodyResolveCache.ThrowException &&
                expressionTypingServices.statementFilter != StatementFilter.NONE
            ) {
                bodyResolveCache.resolveMacroBody(declaration).addOwnDataTo(trace, true)
            } else {
                resolveFunctionBody(c.outerDataFlowInfo, trace, declaration, descriptor, scope, c.localContext)
            }
        }
    }

    private fun resolveFunctionBodies(c: BodiesResolveContext) {
        for ((declaration, descriptor) in c.functions) {
            val scope = c.getDeclaringScope(declaration)
            requireNotNull(scope) { "Scope is null: ${declaration.getElementTextWithContext()}" }

            if (!c.topDownAnalysisMode.isLocalDeclarations &&
                bodyResolveCache !is BodyResolveCache.ThrowException &&
                expressionTypingServices.statementFilter != StatementFilter.NONE
            ) {
                bodyResolveCache.resolveFunctionBody(declaration).addOwnDataTo(trace, true)
            } else {
                resolveFunctionBody(c.outerDataFlowInfo, trace, declaration, descriptor, scope, c.localContext)
            }
        }
    }

    fun resolveFunctionBody(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        declaringScope: LexicalScope,
        localContext: ExpressionTypingContext?
    ) {
        resolveFunctionBody(
            outerDataFlowInfo,
            trace,
            function,
            functionDescriptor,
            declaringScope,
            null,
            null,
            localContext
        )
        //TODO 检查返回值
        requireNotNull(functionDescriptor.returnType)
    }

    private fun resolveFunctionBody(
        outerDataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        scope: LexicalScope,
        beforeBlockBody: ((LexicalScope) -> DataFlowInfo?)?,
        // Creates wrapper scope for header resolution if necessary (see resolveSecondaryConstructorBody)
        headerScopeFactory: ((LexicalScope) -> LexicalScope)?,
        localContext: ExpressionTypingContext?
    ) {
        ProgressManager.checkCanceled()

        PreliminaryDeclarationVisitor.createForDeclaration(function, trace, languageVersionSettings)
        var innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace, overloadChecker)
        val valueParameters = function.valueParameters
        val valueParameterDescriptors = functionDescriptor.valueParameters

        val headerScope = headerScopeFactory?.invoke(innerScope) ?: innerScope
        valueParameterResolver.resolveValueParameters(
            valueParameters, valueParameterDescriptors, headerScope, outerDataFlowInfo, trace,
            localContext?.inferenceSession
        )

        if (functionDescriptor is PropertyAccessorDescriptor &&
            functionDescriptor.extensionReceiverParameter == null &&
            functionDescriptor.contextReceiverParameters.isEmpty()
        ) {
            val property = function.parent.parent as CjProperty
            val propertySourceElement = property.toSourceElement()
            val fieldDescriptor = SyntheticFieldDescriptor(functionDescriptor, propertySourceElement)
            innerScope = LexicalScopeImpl(
                innerScope, functionDescriptor, true, null, emptyList(),
                LexicalScopeKind.PROPERTY_ACCESSOR_BODY,
                LocalRedeclarationChecker.DO_NOTHING
            ) {
                addVariableDescriptor(fieldDescriptor)
            }

            // Check parameter name shadowing
            for (parameter in function.valueParameters) {
                if (SyntheticFieldDescriptor.NAME == parameter.nameAsName) {
                    trace.report(ACCESSOR_PARAMETER_NAME_SHADOWING.on(parameter))
                }
            }
        }

        var dataFlowInfo: DataFlowInfo? = null

        if (beforeBlockBody != null) {
            dataFlowInfo = beforeBlockBody(headerScope)
        }

        if (function.hasBody()) {
            expressionTypingServices.checkFunctionReturnType(
                innerScope, function, functionDescriptor,
                dataFlowInfo ?: outerDataFlowInfo, null, trace, localContext
            )
        } else {
            functionDescriptor.returnType
        }
        //TODO 检查返回值
        requireNotNull(functionDescriptor.returnType)
    }

    private fun checkRedeclarationsInClassHeaderWithoutPrimaryConstructor(
        descriptor: ClassDescriptor,
        scopeForConstructorResolution: LexicalScope
    ) {
        // Initializing a scope will report errors if any.
        LexicalScopeImpl(
            scopeForConstructorResolution, descriptor, true, null, emptyList(),
            LexicalScopeKind.CLASS_HEADER,
            TraceBasedLocalRedeclarationChecker(trace, overloadChecker)
        ) {
            // If a class has no primary constructor, it still can have type parameters declared in header.
            for (typeParameter in descriptor.declaredTypeParameters) {
                addClassifierDescriptor(typeParameter)
            }
        }
    }

    private fun checkPrimaryConstructor(unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?): Boolean =
        unsubstitutedPrimaryConstructor != null &&
                unsubstitutedPrimaryConstructor.source.getPsi() != null &&
                unsubstitutedPrimaryConstructor.source.getPsi() !is CjPrimaryConstructor

    fun resolveSuperTypeEntryList(
        outerDataFlowInfo: DataFlowInfo,
        typeStatement: CjTypeStatement,
        descriptor: ClassDescriptor,
        primaryConstructor: ConstructorDescriptor?,
        scopeForConstructorResolution: LexicalScope,
        scopeForMemberResolution: LexicalScope,
        inferenceSession: InferenceSession?
    ) {
        ProgressManager.checkCanceled()

        primaryConstructor?.let {
            FunctionDescriptorUtil.getFunctionInnerScope(scopeForConstructorResolution, it, trace, overloadChecker)
        }

        if (primaryConstructor == null) {
            checkRedeclarationsInClassHeaderWithoutPrimaryConstructor(descriptor, scopeForConstructorResolution)
        }

        val supertypes = linkedMapOf<CjTypeReference, CangJieType>()
        val primaryConstructorDelegationCall = arrayOfNulls<ResolvedCall<*>>(1)

        val visitor = object : CjVisitorUnit() {
            private fun recordSupertype(typeReference: CjTypeReference, supertype: CangJieType?) {
                if (supertype == null) return
                hasExtendSourceMap[typeReference] = hasExtendSource
                supertypes[typeReference] = supertype
            }

            override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry) {
                val typeReference = specifier.typeReference ?: return
                val supertype = trace.bindingContext[TYPE, typeReference]
                recordSupertype(typeReference, supertype)
                if (supertype == null) return

                val superClass = TypeUtils.getClassDescriptor(supertype) ?: return
                if (superClass.kind.isObject) {
                    // A "singleton in supertype" diagnostic will be reported later
                    return
                }

                if (descriptor.kind != ClassKind.INTERFACE &&
                    checkPrimaryConstructor(descriptor.unsubstitutedPrimaryConstructor) &&
                    superClass.kind != ClassKind.INTERFACE &&

                    !ErrorUtils.isError(superClass) && TypeUtils.checkConstructorsNotParameter(superClass)
                ) {
                    trace.report(SUPERTYPE_NOT_INITIALIZED.on(specifier, supertype))
                }
            }

            override fun visitCjElement(element: CjElement) {
                throw UnsupportedOperationException("${element.text} : $element")
            }
        }

        val sourceSuperClass = mutableSetOf<CangJieType>()

        for (delegationSpecifier in typeStatement.superTypeListEntries) {
            ProgressManager.checkCanceled()
            delegationSpecifier.accept(visitor)
        }

        if (primaryConstructorDelegationCall[0] != null && primaryConstructor != null) {
            recordConstructorDelegationCall(trace, primaryConstructor, primaryConstructorDelegationCall[0] ?: return)
        }

        checkSupertypeList(descriptor, supertypes, typeStatement, sourceSuperClass)
    }

    // Returns a set of enum or sealed types of which supertypeOwner is an entry or a member
    private fun getAllowedFinalSupertypes(
        descriptor: ClassDescriptor,
        supertypes: Map<CjTypeReference, CangJieType>,
        typeStatement: CjTypeStatement
    ): Set<TypeConstructor> = emptySet()

    private fun checkSupertypeList(
        supertypeOwner: ClassDescriptor,
        supertypes: Map<CjTypeReference, CangJieType>,
        typeStatement: CjTypeStatement,
        sourceSuperClass: Set<CangJieType>
    ) {
        val allowedFinalSupertypes = getAllowedFinalSupertypes(supertypeOwner, supertypes, typeStatement)
        val typeConstructors = mutableSetOf<TypeConstructor>()
        var classAppeared = false

        for ((typeReference, supertype) in supertypes) {
            val typeElement = typeReference.typeElement
            if (typeElement is CjFunctionType) {
                for (parameter in typeElement.parameters) {
                    parameter.nameIdentifier?.let { nameIdentifier ->
                        trace.report(
                            UNSUPPORTED.on(
                                nameIdentifier,
                                "named parameter in function type in supertype position"
                            )
                        )
                    }
                }
            }

            var addSupertype = true

            val classDescriptor = TypeUtils.getClassDescriptor(supertype)
            if (classDescriptor != null) {
                if (ErrorUtils.isError(classDescriptor)) continue

                if (classDescriptor.kind != ClassKind.INTERFACE) {
                    when {
                        supertypeOwner.kind == ClassKind.INTERFACE &&
                                !classAppeared && !supertype.isDynamic() ->
                            trace.report(INTERFACE_WITH_SUPERCLASS.on(typeReference)).also { addSupertype = false }

                        supertypeOwner.kind == ClassKind.EXTEND &&
                                !hasExtendSourceMap[typeReference]!! && !supertype.isDynamic() -> {
                            trace.report(EXTEND_WITH_SUPERCLASS.on(typeReference))
                            addSupertype = false
                            return
                        }

                        supertypeOwner.kind == ClassKind.STRUCT &&
                                !classAppeared && !supertype.isDynamic() -> {
                            trace.report(STRUCT_WITH_SUPERCLASS.on(typeReference))
                            addSupertype = false
                            return
                        }
                    }

                    if (classAppeared && supertypeOwner.kind != ClassKind.EXTEND) {
                        trace.report(MANY_CLASSES_IN_SUPERTYPE_LIST.on(typeReference))
                    } else {
                        classAppeared = true
                    }
                }
            } else {
                trace.report(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.on(typeReference))
            }

            val constructor = supertype.constructor
            if (addSupertype && !typeConstructors.add(constructor)) {
                trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference))
            }

            // 验证原类超类型
            for (_supertype in sourceSuperClass) {
                val _constructor = _supertype.constructor
                if (addSupertype && !typeConstructors.add(_constructor) && _constructor == constructor) {
                    trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference))
                }
            }

            if (classDescriptor == null) return

            when {
                classDescriptor.kind.isEnum -> {
                    if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                        trace.report(ENUM_IN_SUPERTYPE.on(typeReference))
                    }
                }

                classDescriptor.kind.isObject -> {
                    if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                        trace.report(STRUCT_IN_SUPERTYPE.on(typeReference))
                    }
                }

                constructor !in allowedFinalSupertypes -> {
                    when {
                        DescriptorUtils.isSealedClass(classDescriptor) -> {
                            var containingDescriptor: DeclarationDescriptor? = supertypeOwner.containingDeclaration
                            while (containingDescriptor != null && containingDescriptor != classDescriptor) {
                                containingDescriptor = containingDescriptor.containingDeclaration
                            }
                        }

                        classDescriptor.isFinalOrEnum -> {
                            trace.report(FINAL_SUPERTYPE.on(typeReference, classDescriptor.defaultType))
                        }
                    }
                }
            }
        }
    }

    fun resolveBodies(c: BodiesResolveContext) {
        // 解析行为声明体
        resolveBehaviorDeclarationBodies(c)
        // 处理控制流分析
        controlFlowAnalyzer.process(c)
        // 处理声明检查
        declarationsChecker.process(c)
    }

    companion object {
        private fun getScopeForVariable(c: BodiesResolveContext, variable: CjVariable): LexicalScope =
            getScopeForDeclaration(c, variable)

        private fun getScopeForDeclaration(c: BodiesResolveContext, declaration: CjDeclaration): LexicalScope {
            val scope = c.getDeclaringScope(declaration)
            requireNotNull(scope) { "Scope for property ${declaration.text} should exists" }
            return scope
        }

        private fun getScopeForProperty(c: BodiesResolveContext, property: CjProperty): LexicalScope =
            getScopeForDeclaration(c, property)

        fun computeDeferredType(type: CangJieType?) {
            // handle type inference loop: function or property body contains a reference to itself
            // fun f() = { f() }
            // val x = x
            // type resolution must be started before body resolution
            if (type is DeferredType && !type.isComputed()) {
                type.delegate
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun recordConstructorDelegationCall(
            trace: BindingTrace,
            constructor: ConstructorDescriptor,
            call: ResolvedCall<*>
        ) {
            trace.record(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor, call as ResolvedCall<ConstructorDescriptor>)
        }

        private fun getPrimaryConstructorParametersScope(
            originalScope: LexicalScope,
            unsubstitutedPrimaryConstructor: ConstructorDescriptor
        ): LexicalScope = LexicalScopeImpl(
            originalScope, unsubstitutedPrimaryConstructor, false, null,
            emptyList(), LexicalScopeKind.DEFAULT_VALUE, LocalRedeclarationChecker.DO_NOTHING
        ) {
            for (valueParameter in unsubstitutedPrimaryConstructor.valueParameters) {
                addVariableDescriptor(valueParameter)
            }
        }

        private fun makeScopeForPropertyAccessor(
            c: BodiesResolveContext,
            accessor: CjPropertyAccessor,
            descriptor: PropertyDescriptor
        ): LexicalScope {
            val accessorDeclaringScope = c.getDeclaringScope(accessor)
            requireNotNull(accessorDeclaringScope) { "Scope for accessor ${accessor.text} should exists" }
            val headerScope = ScopeUtils.makeScopeForPropertyHeader(accessorDeclaringScope, descriptor)
            return LexicalScopeImpl(
                headerScope, descriptor, true, descriptor.extensionReceiverParameter,
                descriptor.contextReceiverParameters, LexicalScopeKind.PROPERTY_ACCESSOR_BODY
            )
        }
    }
}
