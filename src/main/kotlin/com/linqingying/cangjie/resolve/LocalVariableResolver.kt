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

package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.descriptors.impl.LocalVariableDescriptor
import com.linqingying.cangjie.descriptors.impl.VariableDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.VariableDescriptorWithInitializerImpl
import com.linqingying.cangjie.diagnostics.Errors.LOCAL_EXTENSION_VARIABLE
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.psi.CjVariable
import com.linqingying.cangjie.psi.CjVariableDeclaration
import com.linqingying.cangjie.resolve.DescriptorResolver.Companion.getDefaultVisibility
import com.linqingying.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.source.toSourceElement
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.DataFlowAnalyzer
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ExpressionTypingFacade
import com.linqingying.cangjie.types.expressions.ExpressionTypingUtils
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

class LocalVariableResolver(
    private val modifiersChecker: ModifiersChecker,
    private val identifierChecker: IdentifierChecker,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val annotationResolver: AnnotationResolver,
    private val variableTypeAndInitializerResolver: VariableTypeAndInitializerResolver,
//    private val delegatedVariableResolver: DelegatedVariableResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    fun processForPattern(
        variable: CjVariable,
        typingContext: ExpressionTypingContext,
        scope: LexicalScope,
        facade: ExpressionTypingFacade
    ): Pair<CangJieTypeInfo, List<VariableDescriptor>> {

        TODO()
    }

    /**
     * 处理局部变量声明，解析其描述符和类型信息。
     *
     * 该函数负责处理局部变量声明，包括解析变量描述符、确定类型信息以及处理初始化值的绑定。它会报告局部扩展变量的诊断信息，
     * 并在处理过程中管理上下文依赖关系。
     *
     * @param variable 要处理的局部变量声明。
     * @param typingContext 表达式类型的上下文，用于在处理过程中管理类型状态。
     * @param scope 变量声明所在的词法作用域，用于解析变量描述符。
     * @param facade 表达式类型的外观，提供类型信息和推断功能的访问。
     * @return 一个包含变量类型信息和描述符的Pair对象。
     */
    fun process(
        variable: CjVariable,
        typingContext: ExpressionTypingContext,
        scope: LexicalScope,
        facade: ExpressionTypingFacade
    ): Pair<CangJieTypeInfo, VariableDescriptor> {
        // 更新上下文依赖关系和作用域
        val context = typingContext.replaceContextDependency(ContextDependency.INDEPENDENT).replaceScope(scope)

        // 检查接收器类型引用并报告诊断信息
        val receiverTypeRef = variable.receiverTypeReference
        if (receiverTypeRef != null) {
            context.trace.report(LOCAL_EXTENSION_VARIABLE.on(receiverTypeRef))
        }

        // 解析局部变量描述符
        val variableDescriptor =
            resolveLocalVariableDescriptor(
                scope,
                variable,
                context.dataFlowInfo,
                context.inferenceSession,
                context.trace
            )

        // 处理变量初始化
        val initializer = variable.initializer
        var typeInfo: CangJieTypeInfo
        if (initializer != null) {
            val outType = variableDescriptor.type
            typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType))
            val dataFlowInfo = typeInfo.dataFlowInfo
            val type = typeInfo.type
            if (type != null) {
                val initializerDataFlowValue = dataFlowValueFactory.createDataFlowValue(initializer, type, context)
                if (!variableDescriptor.isVar && initializerDataFlowValue.canBeBound) {
                    context.trace.record(
                        BindingContext.BOUND_INITIALIZER_VALUE,
                        variableDescriptor,
                        initializerDataFlowValue
                    )
                }
                // 当变量有显式类型时，不考虑初始化值的影响
                if (variable.typeReference == null) {
                    val variableDataFlowValue = dataFlowValueFactory.createDataFlowValueForVariable(
                        variable, variableDescriptor, context.trace.bindingContext,
                        DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor)
                    )
                    typeInfo = typeInfo.replaceDataFlowInfo(
                        dataFlowInfo.assign(
                            variableDataFlowValue, initializerDataFlowValue,
//                        languageVersionSettings
                        )
                    )
                }
            }
        } else {
            typeInfo = noTypeInfo(context)
        }

        // 检查局部变量声明
        checkLocalVariableDeclaration(context, variableDescriptor, variable)

        // 返回类型信息和变量描述符
        return Pair(typeInfo.replaceType(dataFlowAnalyzer.checkStatementType(variable, context)), variableDescriptor)
    }


    private fun checkLocalVariableDeclaration(
        context: ExpressionTypingContext,
        descriptor: VariableDescriptor,
        cjVariable: CjVariable
    ) {
        ExpressionTypingUtils.checkVariableShadowing(context.scope, context.trace, descriptor)

        modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(cjVariable, descriptor)
        identifierChecker.checkDeclaration(cjVariable, context.trace)
//
//        LateinitModifierApplicabilityChecker.checkLateinitModifierApplicability(
//            context.trace,
//            cjVariable,
//            descriptor,
//            languageVersionSettings
//        )
    }

    /**
     * 解析局部变量的描述符。
     *
     * 该函数负责在给定的作用域内解析局部变量的描述符，并设置其类型和其他相关信息。
     *
     * @param scope 词法作用域，用于解析变量的上下文
     * @param variable 局部变量声明对象
     * @param dataFlowInfo 数据流信息，用于类型推断
     * @param inferenceSession 推断会话，用于类型推断
     * @param trace 绑定跟踪，用于记录解析过程中的绑定信息
     * @return 解析后的变量描述符
     */
    private fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ): VariableDescriptor {
        val containingDeclaration = scope.ownerDescriptor
        val result: VariableDescriptorWithInitializerImpl
        val type: CangJieType

        val variableDescriptor = resolveLocalVariableDescriptorWithType(scope, variable, null, trace)
        // 解析变量类型
        type = variableTypeAndInitializerResolver.resolveType(
            variableDescriptor, scope, variable, dataFlowInfo, inferenceSession, trace, local = true
        )
        variableDescriptor.setOutType(type)
        result = variableDescriptor

//    if (inferenceSession is BuilderInferenceSession) {
//        inferenceSession.addExpression(variable)
//    }
        // 设置变量的常量值（如果需要）
        variableTypeAndInitializerResolver
            .setConstantForVariableIfNeeded(result, scope, variable, dataFlowInfo, type, inferenceSession, trace)
        // 强制解析类型注解
        ForceResolveUtil.forceResolveAllContents(type.annotations)
        return result
    }


    private fun initializeWithDefaultGetterSetter(variableDescriptor: VariableDescriptorImpl) {
//        var getter = variableDescriptor.getter
//        if (getter == null && !DescriptorVisibilities.isPrivate(variableDescriptor.visibility)) {
//            getter = DescriptorFactory.createDefaultGetter(variableDescriptor, Annotations.EMPTY)
//            getter.initialize(variableDescriptor.type)
//        }
//
//        var setter = variableDescriptor.setter
//        if (setter == null && variableDescriptor.isVar) {
//            setter = DescriptorFactory.createDefaultSetter(variableDescriptor, Annotations.EMPTY, Annotations.EMPTY)
//        }
//        variableDescriptor.initialize(getter, setter)
    }

    fun resolveVariableDescriptorWithType(
        scope: LexicalScope,
        variable: CjVariableDeclaration,
        type: CangJieType?,
        trace: BindingTrace,
        isVar: Boolean? = null,

        visibility: DescriptorVisibility?
    ): VariableDescriptor {

        val variableDescriptor = VariableDescriptorImpl(
            scope.ownerDescriptor,

            CjPsiUtil.safeName(variable.name),
            type,
            isVar ?: variable.isVar,

            variable.toSourceElement(),
            visibility ?: resolveVisibilityFromModifiers(
                variable,
                getDefaultVisibility(variable, scope.ownerDescriptor)
            ),
        )
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor)
        return variableDescriptor
    }

    fun resolveLocalVariableDescriptorWithType(
        scope: LexicalScope,
        variable: CjVariableDeclaration,
        type: CangJieType?,
        trace: BindingTrace,
        isVar: Boolean? = null
    ): LocalVariableDescriptor {

        val variableDescriptor = LocalVariableDescriptor(
            scope.ownerDescriptor,
            annotationResolver.resolveAnnotationsWithArguments(scope, variable.modifierList, trace),

            CjPsiUtil.safeName(variable.name),
            type,
            isVar ?: variable.isVar,

            variable.toSourceElement()
        )
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor)
        return variableDescriptor
    }

//    private fun VariableAccessorDescriptor.updateAccessorFlagsFromResolvedCallForDelegatedVariable(trace: BindingTrace) {
//        if (this is FunctionDescriptorImpl) {
//            val resultingDescriptor = trace.bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, this)?.resultingDescriptor
//            if (resultingDescriptor != null) {
//                setSuspend(resultingDescriptor.isSuspend)
//            }
//        }
//    }

}
