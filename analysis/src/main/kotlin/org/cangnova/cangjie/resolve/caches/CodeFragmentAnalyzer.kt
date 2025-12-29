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

package org.cangnova.cangjie.resolve.caches

import jakarta.inject.Inject

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.resolve.analyzeInContext
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypes3
import org.cangnova.cangjie.psi.psiUtil.lastBlockStatementOrThis
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.USED_AS_EXPRESSION
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.binding.getDataFlowInfoAfter
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolverFacade
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.utils.externalDescriptors

class CodeFragmentAnalyzer(
    private val resolveSession: ResolveSession,
    private val qualifierResolver: QualifiedExpressionResolverFacade,
    private val typeResolver: TypeResolver,
    private val expressionTypingServices: ExpressionTypingServices
) {

    private lateinit var resolveElementCache: ResolveElementCache

    @Inject
    fun setResolveElementCache(resolveElementCache: ResolveElementCache) {
        this.resolveElementCache = resolveElementCache
    }

    fun analyzeCodeFragment(codeFragment: CjCodeFragment, bodyResolveMode: BodyResolveMode): BindingTrace {
        val contextAnalysisResult = analyzeCodeFragmentContext(codeFragment, bodyResolveMode)
        return doAnalyzeCodeFragment(codeFragment, contextAnalysisResult)
    }

    private fun doAnalyzeCodeFragment(codeFragment: CjCodeFragment, contextInfo: ContextInfo): BindingTrace {
        val (bindingContext, scope, dataFlowInfo) = contextInfo
        val bindingTrace = DelegatingBindingTrace(bindingContext, "For code fragment analysis")

        when (val contentElement = codeFragment.getContentElement()) {
            is CjExpression -> {
                val expectedType = codeFragment.getUserData(EXPECTED_TYPE_KEY) ?: TypeUtils.NO_EXPECTED_TYPE
                contentElement.analyzeInContext(
                    scope,
                    trace = bindingTrace,
                    dataFlowInfo = dataFlowInfo,
                    expectedType = expectedType,
                    expressionTypingServices = expressionTypingServices
                )

                // 将片段表达式（或片段主体中的最后一个表达式）标记为已使用，
// 这样它就不会在控制流图（CFG）分析中被报告为未使用

                bindingTrace.record(USED_AS_EXPRESSION, contentElement.lastBlockStatementOrThis())

                analyzeControlFlow(resolveSession, contentElement, bindingTrace)
            }

            is CjTypeReference -> {
                val context = TypeResolutionContext(
                    scope, bindingTrace,
                    true, true, codeFragment.suppressDiagnosticsInDebugMode()
                ).noBareTypes()

                typeResolver.resolvePossiblyBareType(context, contentElement)
            }
        }

        return bindingTrace
    }

    private data class ContextInfo(
        val bindingContext: BindingContext,
        val scope: LexicalScope,
        val dataFlowInfo: DataFlowInfo
    )

    private fun analyzeCodeFragmentContext(
        codeFragment: CjCodeFragment,
        bodyResolveMode: BodyResolveMode
    ): ContextInfo {
        fun resolutionFactory(element: CjElement): BindingContext {
            return resolveElementCache.resolveToElement(element, bodyResolveMode)
        }

        val context = refineContextElement(codeFragment.context)
        val info = getContextInfo(context, ::resolutionFactory)
        return info.copy(scope = enrichScopeWithImports(info.scope, codeFragment))
    }

    private tailrec fun getContextInfo(
        context: PsiElement?,
        resolutionFactory: (CjElement) -> BindingContext
    ): ContextInfo {
        var bindingContext: BindingContext = BindingContext.EMPTY
        var dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY
        var scope: LexicalScope? = null

        when (context) {
            is CjPrimaryConstructor -> {
                val containingClass = context.getContainingTypeStatement()
                val resolutionResult = getClassDescriptor(containingClass, resolutionFactory)
                if (resolutionResult != null) {
                    bindingContext = resolutionResult.bindingContext
                    scope = resolutionResult.descriptor.scopeForInitializerResolution
                }
            }

            is CjSecondaryConstructor -> {
                val expression = context.bodyExpression ?: context.getDelegationCallOrNull()
                if (expression != null) {
                    bindingContext = resolutionFactory(expression)
                    scope = bindingContext[BindingContext.LEXICAL_SCOPE, expression]
                }
            }

            is CjTypeStatement -> {
                val resolutionResult = getClassDescriptor(context, resolutionFactory)
                if (resolutionResult != null) {
                    bindingContext = resolutionResult.bindingContext
                    scope = resolutionResult.descriptor.scopeForMemberDeclarationResolution
                }
            }

            is CjFunction -> {
                val bindingContextForFunction = resolutionFactory(context)
                val functionDescriptor = bindingContextForFunction[BindingContext.FUNCTION, context]
                if (functionDescriptor != null) {
                    bindingContext = bindingContextForFunction

                    @Suppress("NON_TAIL_RECURSIVE_CALL")
                    val outerScope =
                        getContextInfo(context.getParentOfType<CjDeclaration>(true), resolutionFactory).scope

                    val localRedeclarationChecker = LocalRedeclarationChecker.DO_NOTHING
                    scope = FunctionDescriptorUtil.getFunctionInnerScope(
                        outerScope,
                        functionDescriptor,
                        localRedeclarationChecker
                    )
                }
            }

            is CjFile -> {
                bindingContext = resolveSession.bindingContext
                scope = resolveSession.fileScopeProvider.getFileResolutionScope(context)
            }

            is CjElement -> {
                bindingContext = resolutionFactory(context)
                scope = context.getResolutionScope(bindingContext)
                dataFlowInfo = bindingContext.getDataFlowInfoAfter(context)
            }
        }

        if (scope == null) {
            val parentDeclaration = context?.getParentOfTypes3<CjDeclaration, CjFile, CjExpression>()
            if (parentDeclaration != null) {
                return getContextInfo(parentDeclaration, resolutionFactory)
            }
        }

        return ContextInfo(bindingContext, scope ?: createEmptyScope(resolveSession.moduleDescriptor), dataFlowInfo)
    }

    private data class ClassResolutionResult(
        val bindingContext: BindingContext,
        val descriptor: ClassDescriptorWithResolutionScopes
    )

    private fun getClassDescriptor(
        classOrObject: CjTypeStatement,
        resolutionFactory: (CjElement) -> BindingContext
    ): ClassResolutionResult? {
        val bindingContext: BindingContext
        val classDescriptor: ClassDescriptor?

        if (!CjPsiUtil.isLocal(classOrObject)) {
            bindingContext = resolveSession.bindingContext
            classDescriptor = resolveSession.getClassDescriptor(classOrObject, NoLookupLocation.FROM_IDE)
        } else {
            bindingContext = resolutionFactory(classOrObject)
            classDescriptor =
                bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject] as ClassDescriptor?
        }

        return (classDescriptor as? ClassDescriptorWithResolutionScopes)?.let {
            ClassResolutionResult(
                bindingContext,
                it
            )
        }
    }

    private fun refineContextElement(context: PsiElement?): CjElement? {
        return when (context) {
            is CjParameter -> context.getParentOfType<CjFunction>(true)
//            is CjProperty -> context.delegateExpressionOrInitializer
            is CjConstructor<*> -> context
            is CjFunctionLiteral -> context.bodyExpression?.statements?.lastOrNull()
            is CjDeclarationWithBody -> context.bodyExpression
            is CjBlockExpression -> context.statements.lastOrNull()
            else -> null
        } ?: context as? CjElement
    }

    private fun enrichScopeWithImports(scope: LexicalScope, codeFragment: CjCodeFragment): LexicalScope {
        val additionalImportingScopes = mutableListOf<ImportingScope>()

        val externalDescriptors = codeFragment.externalDescriptors ?: emptyList()
        if (externalDescriptors.isNotEmpty()) {
            additionalImportingScopes += ExplicitImportsScope(externalDescriptors)
        }

        val importList = codeFragment.importsAsImportList()
        if (importList != null && importList.imports.isNotEmpty()) {
            additionalImportingScopes += createImportScopes(importList)
        }

        if (additionalImportingScopes.isNotEmpty()) {
            return scope.addImportingScopes(additionalImportingScopes)
        }

        return scope
    }

    private fun createImportScopes(importList: CjImportList): List<ImportingScope> {
        return importList.importItems.mapNotNull {

            qualifierResolver.processImportReference(
                it, resolveSession.moduleDescriptor, resolveSession.trace,
                excludedImportNames = emptyList(), packageFragmentForVisibilityCheck = null
            )
        }
    }

    private fun createEmptyScope(moduleDescriptor: ModuleDescriptor): LexicalScope {
        return LexicalScope.Base(ImportingScope.Empty, moduleDescriptor)
    }

    companion object {
        val EXPECTED_TYPE_KEY = Key<CangJieType>("EXPECTED_TYPE")
    }
}
