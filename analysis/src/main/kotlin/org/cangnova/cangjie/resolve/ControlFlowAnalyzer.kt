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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.LocalRedeclarationChecker
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import jakarta.inject.Inject
import org.cangnova.cangjie.diagnostics.infos.errors.MAIN_FUNCTION_PARAMETER_COUNT
import org.cangnova.cangjie.diagnostics.infos.errors.MAIN_FUNCTION_PARAMETER_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.MAIN_FUNCTION_RETURN_TYPE
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.BodiesResolveContext
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE

class ControlFlowAnalyzer(
    val trace: BindingTrace,
    val builtIns: CangJieBuiltIns,
    val languageVersionSettings: LanguageVersionSettings,
//  val      diagnosticSuppressor: PlatformDiagnosticSuppressor,
    val controlFlowInformationProviderFactory: ControlFlowInformationProvider.Factory,
//   val     enumWhenTracker: EnumWhenTracker
) {
    private lateinit var functionReturnResolver: FunctionReturnResolver
    private lateinit var expressionTypingServices: ExpressionTypingServices

    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    @Inject
    fun setFunctionReturnResolver(functionReturnResolver: FunctionReturnResolver) {
        this.functionReturnResolver = functionReturnResolver
    }

    fun inferredLocalFunctionReturnType(
        parentScope: LexicalScope,
        controlFlowInformationProvider: ControlFlowInformationProviderImpl?
    ) {
        controlFlowInformationProvider?.getLocalFunctions()?.forEach { (function, functionDescriptor) ->


            functionDescriptor?.let {
                /**TODO
                 *   是否应该按照嵌套层次创建scope  例如
                 *  fo a{
                 *      fo b{
                 *          fo c{
                 *      }
                 *  }
                 *
                 */
                val scope = LexicalWritableScope(
                    FunctionDescriptorUtil.getFunctionInnerScope(
                        parentScope,
                        functionDescriptor,
                        LocalRedeclarationChecker.DO_NOTHING
                    ),/*parentScope*/
                    functionDescriptor as DeclarationDescriptor,
                    false,
                    LocalRedeclarationChecker.DO_NOTHING,
                    LexicalScopeKind.CODE_BLOCK
                )
                inferredFunctionReturnType(scope, function, it)
            }

        }

    }

    //    检查返回值，检查参数，符合main方法要求
    private fun checkMainFunction(
        c: BodiesResolveContext,
        function: CjDeclarationWithBody,
        functionDescriptor: SimpleFunctionDescriptor
    ) {

        val controlFlowInformationProvider: ControlFlowInformationProvider =
            controlFlowInformationProviderFactory.createControlFlowInformationProvider(
                function, trace, languageVersionSettings, /*diagnosticSuppressor, enumWhenTracker*/
            )
        controlFlowInformationProvider.checkDeclaration()


        functionDescriptor.returnType?.let {
//            检查类型
            if (!(CangJieBuiltIns.isInt64(it) || CangJieBuiltIns.isUnit(it))) {
                trace.report(MAIN_FUNCTION_RETURN_TYPE.on(function.firstChild))
            }
        }
        //            检查参数
        if (functionDescriptor.valueParameters.size > 1) {
//        参数过多
            (function as CjCallableDeclaration).valueParameterList?.let {
                trace.report(
                    MAIN_FUNCTION_PARAMETER_COUNT.on(
                        it
                    )
                )
            }
        } else {
            functionDescriptor.valueParameters.forEach { parameterDescriptor ->
                parameterDescriptor.returnType?.let {
                    if (CangJieBuiltIns.isArray(it)) {
                        if (it.arguments.size == 1) {
                            if (CangJieBuiltIns.isString(it.arguments[0].type)) {
                                return@forEach
                            }
                        }
                    }
                }

                parameterDescriptor.source.getPsi()?.let {
                    trace.report(MAIN_FUNCTION_PARAMETER_TYPE.on(it as CjParameter))
                }
            }

        }


    }

    private fun checkMacroDeclaration(
        c: BodiesResolveContext,
        macro: CjDeclarationWithBody,
        macroDeclaration: MacroDescriptor
    ) {
        val controlFlowInformationProvider: ControlFlowInformationProvider =
            controlFlowInformationProviderFactory.createControlFlowInformationProvider(
                macro, trace, languageVersionSettings, /*diagnosticSuppressor, enumWhenTracker*/
            )


//        if (c.getTopDownAnalysisMode().isLocalDeclarations) {
//            controlFlowInformationProvider.checkForLocalClassOrObjectMode()
//            return
//        }
        controlFlowInformationProvider.checkDeclaration()


        val expectedReturnType = macroDeclaration.returnType

        controlFlowInformationProvider.checkFunction(expectedReturnType)
    }

    private fun checkFunction(
        c: BodiesResolveContext,
        function: CjDeclarationWithBody,
        functionDescriptor: SimpleFunctionDescriptor
    ) {
        val controlFlowInformationProvider: ControlFlowInformationProvider =
            controlFlowInformationProviderFactory.createControlFlowInformationProvider(
                function, trace, languageVersionSettings, /*diagnosticSuppressor, enumWhenTracker*/
            )

        c.getDeclaringScope(function)?.let {

            inferredLocalFunctionReturnType(
                LexicalWritableScope(
                    FunctionDescriptorUtil.getFunctionInnerScope(
                        it,
                        functionDescriptor,
                        LocalRedeclarationChecker.DO_NOTHING
                    ),
                    functionDescriptor as DeclarationDescriptor,
                    false,
                    LocalRedeclarationChecker.DO_NOTHING,
                    LexicalScopeKind.CODE_BLOCK
                ), /*it*/

                controlFlowInformationProvider as? ControlFlowInformationProviderImpl
            )


        }




        if (c.topDownAnalysisMode.isLocalDeclarations) {
            controlFlowInformationProvider.checkForLocalClassOrObjectMode()
            return
        }
        controlFlowInformationProvider.checkDeclaration()


        val expectedReturnType =
            if (!function.hasBlockBody() && !function.hasDeclaredReturnType())
                NO_EXPECTED_TYPE
            else
                functionDescriptor.returnType

        controlFlowInformationProvider.checkFunction(expectedReturnType)
    }

    private fun checkFunction(
        c: BodiesResolveContext,
        function: CjDeclarationWithBody,
        expectedReturnType: CangJieType?
    ) {
        val controlFlowInformationProvider: ControlFlowInformationProvider =
            controlFlowInformationProviderFactory.createControlFlowInformationProvider(
                function, trace, languageVersionSettings,/* diagnosticSuppressor, enumWhenTracker*/
            )
        if (c.topDownAnalysisMode.isLocalDeclarations) {
            controlFlowInformationProvider.checkForLocalClassOrObjectMode()
            return
        }
        controlFlowInformationProvider.checkDeclaration()
        controlFlowInformationProvider.checkFunction(expectedReturnType)
    }

    private fun checkProperty(
        c: BodiesResolveContext,
        property: CjProperty,
        propertyDescriptor: PropertyDescriptor
    ) {
        for (accessor in property.accessors) {
            val accessorDescriptor: PropertyAccessorDescriptor = checkNotNull(
                if (accessor.isGetter)
                    propertyDescriptor.getter
                else
                    propertyDescriptor.setter
            ) { "no property accessor descriptor " + accessor.text }
            val returnType: CangJieType? = accessorDescriptor.returnType
            checkFunction(c, accessor, returnType)
        }
    }

    fun inferredFunctionReturnType(
        scope: LexicalScope,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY
    ) {

//        if (function is CjFunction && function.hasBody() && function.typeReference == null && functionDescriptor is FunctionDescriptorImpl) {
//
//            val context = expressionTypingServices.createContext(
//                scope,
//                dataFlowInfo, NO_EXPECTED_TYPE,
//                trace
//            )
//            functionReturnResolver.resolveFunctionReturn(function, context)?.let {
//
//                if (it != functionDescriptor.returnType) {
//                    functionDescriptor.setReturnType(it)
//
//                }
//            }
//
//
//        }
    }

    fun inferredFunctionReturnType(
        c: BodiesResolveContext,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,

        ) {
        c.getDeclaringScope(function)?.let {
            inferredFunctionReturnType(it, function, functionDescriptor, c.outerDataFlowInfo)
        }

    }

    private fun checkDeclarationContainer(c: BodiesResolveContext, declarationContainer: CjDeclarationContainer) {
        // A pseudocode of class/object initialization corresponds to a class/object
        // or initialization of properties corresponds to a package declared in a file
        val controlFlowInformationProvider = controlFlowInformationProviderFactory.createControlFlowInformationProvider(
            declarationContainer as CjElement, trace, languageVersionSettings, /*diagnosticSuppressor, enumWhenTracker*/
        )
        if (c.topDownAnalysisMode.isLocalDeclarations) {
            controlFlowInformationProvider.checkForLocalClassOrObjectMode()
            return
        }
        controlFlowInformationProvider.checkDeclaration()
    }

    // SomeFile.kt
    private fun checkSecondaryConstructor(constructor: CjSecondaryConstructor) {
        val controlFlowInformationProvider = controlFlowInformationProviderFactory.createControlFlowInformationProvider(
            constructor, trace, languageVersionSettings,/* diagnosticSuppressor, enumWhenTracker*/
        )
        controlFlowInformationProvider.checkDeclaration()
        controlFlowInformationProvider.checkFunction(builtIns.unitType)
    }

    fun process(c: BodiesResolveContext) {

        for (file in c.files) {
            checkDeclarationContainer(c, file)
        }
        for (aClass in c.declaredClasses.keys) {
            checkDeclarationContainer(c, aClass)
        }

        for (constructor in c.secondaryConstructors.keys) {
            checkSecondaryConstructor(constructor)
        }
        for ((function, functionDescriptor) in c.functions.entries) {
            inferredFunctionReturnType(c, function, functionDescriptor)


            checkFunction(c, function, functionDescriptor)
        }

        for ((function, macroDescriptor) in c.macros.entries) {
            inferredFunctionReturnType(c, function, macroDescriptor)


            checkMacroDeclaration(c, function, macroDescriptor)
        }


        for ((function, functionDescriptor) in c.mainFunctions.entries) {
            inferredFunctionReturnType(c, function, functionDescriptor)


            checkMainFunction(c, function, functionDescriptor)
        }
        for ((property, propertyDescriptor) in c.properties.entries) {
            checkProperty(c, property, propertyDescriptor)
        }
    }

}
