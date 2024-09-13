package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.resolve.controlFlow.ControlFlowInformationProvider
import com.huawei.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.huawei.cangjie.psi.CjDeclarationWithBody
import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.resolve.scopes.LocalRedeclarationChecker
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
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
                functionDescriptor.getReturnType()

        controlFlowInformationProvider.checkFunction(expectedReturnType)
    }

    fun inferredFunctionReturnType(
        scope: LexicalScope,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY
    ) {

        if (function is CjFunction && function.hasBody() && function.getTypeReference() == null && functionDescriptor is FunctionDescriptorImpl) {

            val context = expressionTypingServices.createContext(
                scope,
                dataFlowInfo, NO_EXPECTED_TYPE,
                trace
            )
            functionReturnResolver.resolveFunctionReturn(function, context)?.let {
                functionDescriptor.setReturnType(it)
            }


        }
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

    fun process(c: BodiesResolveContext) {


        for ((function, functionDescriptor) in c.functions.entries) {
            inferredFunctionReturnType(c, function, functionDescriptor)


            checkFunction(c, function, functionDescriptor)
        }
    }

}
