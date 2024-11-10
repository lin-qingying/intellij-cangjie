package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.LexicalScopeKind
import com.linqingying.cangjie.resolve.scopes.LexicalWritableScope
import com.linqingying.cangjie.resolve.scopes.LocalRedeclarationChecker
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import jakarta.inject.Inject

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
                trace.report(Errors.MAIN_FUNCTION_RETURN_TYPE.on(function.firstChild))
            }
        }
        //            检查参数
        if (functionDescriptor.valueParameters.size > 1) {
//        参数过多
            trace.report(Errors.MAIN_FUNCTION_PARAMETER_COUNT.on((function as CjCallableDeclaration).valueParameterList))
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
                    trace.report(Errors.MAIN_FUNCTION_PARAMETER_TYPE.on(it as CjParameter))
                }
            }

        }


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




        if (c.getTopDownAnalysisMode().isLocalDeclarations) {
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
        val controlFlowInformationProvider:  ControlFlowInformationProvider =
            controlFlowInformationProviderFactory.createControlFlowInformationProvider(
                function, trace, languageVersionSettings,/* diagnosticSuppressor, enumWhenTracker*/
            )
        if (c.getTopDownAnalysisMode().isLocalDeclarations) {
            controlFlowInformationProvider.checkForLocalClassOrObjectMode()
            return
        }
        controlFlowInformationProvider.checkDeclaration()
        controlFlowInformationProvider.checkFunction(expectedReturnType)
    }
    private fun checkProperty(
        c:  BodiesResolveContext,
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
            val returnType :CangJieType?  = accessorDescriptor.returnType
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
            inferredFunctionReturnType(it, function, functionDescriptor, c.getOuterDataFlowInfo())
        }

    }

    private fun checkDeclarationContainer(c: BodiesResolveContext, declarationContainer: CjDeclarationContainer) {
        // A pseudocode of class/object initialization corresponds to a class/object
        // or initialization of properties corresponds to a package declared in a file
        val controlFlowInformationProvider = controlFlowInformationProviderFactory.createControlFlowInformationProvider(
            declarationContainer as CjElement, trace, languageVersionSettings, /*diagnosticSuppressor, enumWhenTracker*/
        )
        if (c.getTopDownAnalysisMode().isLocalDeclarations) {
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



        for ((function, functionDescriptor) in c.mainFunctions.entries) {
            inferredFunctionReturnType(c, function, functionDescriptor)


            checkMainFunction(c, function, functionDescriptor)
        }
        for ((property, propertyDescriptor) in c.properties.entries) {
            checkProperty(c, property, propertyDescriptor)
        }
    }

}
