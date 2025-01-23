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

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.DiagnosticFactory
import com.linqingying.cangjie.ide.CangJieIndicesHelper
import com.linqingying.cangjie.ide.actions.ExpressionWeigher
import com.linqingying.cangjie.ide.imports.*
import com.linqingying.cangjie.ide.intentions.getCallableDescriptor
import com.linqingying.cangjie.ide.quickfix.actions.*
import com.linqingying.cangjie.ide.util.substituteExtensionIfCallable
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.CjPsiUtil.isSelectorInQualified
import com.linqingying.cangjie.psi.psiUtil.isImportDirectiveExpression
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.caches.analyze
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.caches.resolveImportReference
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.linqingying.cangjie.resolve.calls.util.receiverTypesWithIndex
import com.linqingying.cangjie.resolve.calls.util.singleLambdaArgumentExpression
import com.linqingying.cangjie.resolve.descriptorUtil.fqNameSafe
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.scopes.getResolveScope
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.isSubtypeOf
import com.linqingying.cangjie.utils.CallType
import com.linqingying.cangjie.utils.CallTypeAndReceiver
import com.linqingying.cangjie.utils.CangJieExceptionWithAttachments
import com.linqingying.cangjie.utils.safeAs
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.HintAction
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.createSmartPointer
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.Processors
import com.linqingying.cangjie.resolve.calls.components.getDescriptorKind
import org.jetbrains.annotations.TestOnly


/**
 * 抽象基类，用于处理未解析引用的导入修复。
 *
 * 该类提供了基础功能和方法来检测未解析引用并尝试自动修复。它实现了多个接口以支持提示、高优先级操作等功能。
 *
 * @param expression 需要分析和修复的表达式
 * @param expressionToAnalyzePointer 指向需要分析的表达式的智能指针，可选参数
 * @param factory 工厂对象，用于创建具体的修复实例
 */
abstract class ImportFixBase<T : CjExpression> protected constructor(
    expression: T,
    private val expressionToAnalyzePointer: SmartPsiElementPointer<CjExpression>?,
    factory: Factory
) : CangJieImportQuickFixAction<T>(expression), HintAction, HighPriorityAction {

    /**
     * 简化构造函数，不指定需要分析的表达式。
     */
    constructor(expression: T, factory: Factory) :
            this(expression, null, factory)

    /**
     * 构造函数，指定需要分析的具体表达式。
     */
    constructor(expression: T, expressionToAnalyze: CjExpression, factory: Factory) :
            this(expression, expressionToAnalyze.createSmartPointer<CjExpression>(), factory)

    /**
     * 获取当前项目的上下文。
     */
    private val project = expression.project

    /**
     * 记录创建时的 PSI 修改计数，用于判断是否过期。
     */
    private val modificationCountOnCreate = PsiModificationTracker.getInstance(project).modificationCount

    /**
     * 获取需要分析的表达式，优先使用智能指针指向的元素。
     */
    private val expressionToAnalyze: CjExpression?
        get() = expressionToAnalyzePointer?.element ?: element

    /**
     * 存储建议的完全限定名集合，延迟初始化。
     */
    protected lateinit var suggestions: Collection<FqName>

    /**
     * 存储意图文本，延迟初始化。
     */
    @IntentionName
    private lateinit var text: String

    /**
     * 内部方法，计算并填充建议列表。
     */
    internal fun computeSuggestions() {
        // 收集建议描述符，并根据描述符生成建议列表和意图文本。
        val suggestionDescriptors = collectSuggestionDescriptors()
        suggestions = collectSuggestions(suggestionDescriptors)
        text = calculateText(suggestionDescriptors)
    }

    /**
     * 支持的错误类型，默认为工厂提供的错误类型集合。
     */
    protected open val supportedErrors = factory.supportedErrors.toSet()

    /**
     * 需要导入的名字集合，具体实现由子类提供。
     */
    protected abstract val importNames: Collection<Name>

    /**
     * 获取调用类型和接收者信息，具体实现由子类提供。
     */
    protected abstract fun getCallTypeAndReceiver(): CallTypeAndReceiver<*, *>?

    /**
     * 根据诊断信息计算接收者类型，默认返回 null。
     */
    protected open fun calculateReceiverTypeFromDiagnostic(diagnostic: Collection<Diagnostic>): CangJieType? = null

    /**
     * 根据诊断信息获取接收者类型。
     */
    protected fun getReceiverTypeFromDiagnostic(): CangJieType? {
        // 不能将诊断信息作为类属性保存，以防止泄漏 IdeaResolverForProject。
        // 在应用快速修复时需要重新计算诊断信息。
        val expression = expressionToAnalyze
        val bindingContext = expression?.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS) ?: return null
        val diagnostics = bindingContext.diagnostics.forElement(expression)
        return calculateReceiverTypeFromDiagnostic(diagnostics)
    }

    /**
     * 显示提示信息。
     */
    override fun showHint(editor: Editor): Boolean {
        // 检查元素是否有效，是否过期，以及是否有可用的建议。
        val element = element?.takeIf(PsiElement::isValid) ?: return false
        if (isOutdated()) return false
        if (ApplicationManager.getApplication().isHeadlessEnvironment ||
            !DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled ||
            HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)
        ) return false
        if (suggestions.isEmpty()) return false
        return createAction(editor, element, suggestions).showHint()
    }

    /**
     * 计算意图文本。
     */
    @IntentionName
    private fun calculateText(suggestionDescriptors: Collection<DeclarationDescriptor>): String {
        // 将建议描述符转换为意图文本。
        val descriptors =
            suggestionDescriptors.mapTo(hashSetOf()) { it.original }.takeIf { it.isNotEmpty() } ?: return ""
        val cjFile = element?.getContainingCjFile() ?: return CangJieBundle.message("fix.import")
        val prioritizer = createPrioritizerForFile(cjFile)
        val expressionWeigher = ExpressionWeigher.createWeigher(element)
        val importInfos = descriptors.mapNotNull { descriptor ->
            val kind = when {
                descriptor is PropertyDescriptor -> ImportFixHelper.ImportKind.PROPERTY
                descriptor is VariableDescriptor -> ImportFixHelper.ImportKind.VARIABLE
                descriptor is ClassConstructorDescriptor && descriptor.containingDeclaration.kind.isInterface -> ImportFixHelper.ImportKind.INTERFACE
                descriptor is ClassConstructorDescriptor && descriptor.containingDeclaration.kind.isStruct -> ImportFixHelper.ImportKind.STRUCT
                descriptor is ClassConstructorDescriptor -> ImportFixHelper.ImportKind.CLASS
                descriptor is TypeAliasConstructorDescriptor -> ImportFixHelper.ImportKind.TYPE_ALIAS
                descriptor is FunctionDescriptor && descriptor.isOperator -> ImportFixHelper.ImportKind.OPERATOR
                descriptor is FunctionDescriptor && descriptor.isExtension -> ImportFixHelper.ImportKind.EXTENSION_FUNCTION
                descriptor is FunctionDescriptor -> ImportFixHelper.ImportKind.FUNCTION
                descriptor is ClassDescriptor && descriptor.kind.isInterface -> ImportFixHelper.ImportKind.INTERFACE
                descriptor is ClassDescriptor && descriptor.kind.isStruct -> ImportFixHelper.ImportKind.STRUCT
                descriptor is ClassDescriptor && descriptor.kind.isEnum -> ImportFixHelper.ImportKind.ENUM
                descriptor is ClassDescriptor -> ImportFixHelper.ImportKind.CLASS
                descriptor is TypeAliasDescriptor -> ImportFixHelper.ImportKind.TYPE_ALIAS
                else -> null
            } ?: return@mapNotNull null
            val name = buildString {
                descriptor.safeAs<CallableDescriptor>()?.let { callableDescriptor ->
                    val extensionReceiverParameter = callableDescriptor.extensionReceiverParameter
                    if (extensionReceiverParameter != null) {
                        extensionReceiverParameter.type.constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.name?.let {
                            append(it.asString())
                        }
                    } else {
                        callableDescriptor.containingDeclaration.safeAs<ClassifierDescriptor>()?.name?.let {
                            append(it.asString())
                        }
                    }
                }
                descriptor.name.takeUnless { it.isSpecial }?.let {
                    if (this.isNotEmpty()) append('.')
                    append(it.asString())
                }
            }
            val priority = createDescriptorPriority(prioritizer, expressionWeigher, descriptor)
            ImportFixHelper.ImportInfo(kind, name, priority)
        }
        return ImportFixHelper.calculateTextForFix(importInfos, suggestions)
    }

    /**
     * 获取意图文本。
     */
    override fun getText(): String = text

    /**
     * 获取意图家族名称。
     */
    override fun getFamilyName() = CangJieBundle.message("fix.import")

    /**
     * 判断修复是否可用。
     */
    override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean {
        return element != null && suggestions.isNotEmpty()
    }

    /**
     * 执行修复操作。
     */
    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(editor!!, element, suggestions).execute()
        }
    }

    /**
     * 是否在写入操作中启动。
     */
    override fun startInWriteAction() = false

    /**
     * 判断是否过期。
     */
    private fun isOutdated() =
        modificationCountOnCreate != PsiModificationTracker.getInstance(project).modificationCount

    /**
     * 创建具体的导入修复动作。
     */
    protected open fun createAction(
        editor: Editor,
        element: CjExpression,
        suggestions: Collection<FqName>
    ): CangJieAddImportAction {
        return createSingleImportAction(element.project, editor, element, suggestions)
    }

    /**
     * 创建导入动作。
     */
    override fun createImportAction(editor: Editor, file: CjFile): QuestionAction? =
        element?.let { createAction(editor, it, suggestions) }

    /**
     * 创建自动导入动作。
     */
    override fun createAutoImportAction(
        editor: Editor,
        file: CjFile,
        filterSuggestions: (Collection<FqName>) -> Collection<FqName>,
    ): QuestionAction? {
        val suggestions = filterSuggestions(suggestions)
        if (suggestions.isEmpty() || !ImportFixHelper.suggestionsAreFromSameParent(suggestions)) return null
        // 不自动导入嵌套类，因为这可能会导致文本中的资格问题，使用户感到困惑。
        if (suggestions.any { suggestion ->
                file.resolveImportReference(suggestion).any(::isNestedClassifier)
            }) return null
        return element?.let { createAction(editor, it, suggestions) }
    }

    /**
     * 判断是否为嵌套类。
     */
    private fun isNestedClassifier(declaration: DeclarationDescriptor): Boolean =
        declaration is ClassifierDescriptor && declaration.containingDeclaration is ClassifierDescriptor

    /**
     * 收集建议描述符。
     */
    private fun collectSuggestionDescriptors(): Collection<DeclarationDescriptor> {
        element?.takeIf(PsiElement::isValid)?.takeIf { it.containingFile is CjFile } ?: return emptyList()
        val callTypeAndReceiver = getCallTypeAndReceiver() ?: return emptyList()
        if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) return emptyList()
        return importNames.flatMap { collectSuggestionsForName(it, callTypeAndReceiver) }
    }

    /**
     * 收集建议的完全限定名。
     */
    private fun collectSuggestions(suggestionDescriptors: Collection<DeclarationDescriptor>): Collection<FqName> =
        suggestionDescriptors
            .asSequence()
            .map { it.fqNameSafe }
            .distinct()
            .toList()

    /**
     * 根据名字收集建议描述符。
     */
    private fun collectSuggestionsForName(
        name: Name,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>
    ): Collection<DeclarationDescriptor> {
        val element = element ?: return emptyList()
        val expressionToAnalyze = expressionToAnalyze ?: return emptyList()
        val nameStr = name.asString()
        if (nameStr.isEmpty()) return emptyList()
        val file = element.getContainingCjFile()
        val bindingContext = expressionToAnalyze.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        if (!checkErrorStillPresent(bindingContext)) return emptyList()
        val searchScope = getResolveScope(file)
        val resolutionFacade = file.getResolutionFacade()
        fun isVisible(descriptor: DeclarationDescriptor): Boolean =
            descriptor.safeAs<DeclarationDescriptorWithVisibility>()
                ?.isVisible(element, callTypeAndReceiver.receiver as? CjExpression, bindingContext, resolutionFacade)
                ?: true

        val indicesHelper = CangJieIndicesHelper(resolutionFacade, searchScope, ::isVisible, file = file)
        var result = fillCandidates(nameStr, callTypeAndReceiver, bindingContext, indicesHelper)
        // 对于默认调用类型，如果没有括号则不包含函数。
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val isCall = element.parent is CjCallExpression
            if (!isCall) {
                result = result.filter { it !is FunctionDescriptor }
            }
        }
        return if (result.size > 1)
            reduceCandidatesBasedOnDependencyRuleViolation(result, file)
        else
            result
    }

    /**
     * 检查错误是否仍然存在。
     */
    private fun checkErrorStillPresent(bindingContext: BindingContext): Boolean {
        val errors = supportedErrors
        val elementsToCheckDiagnostics = elementsToCheckDiagnostics()
        for (psiElement in elementsToCheckDiagnostics) {
            if (bindingContext.diagnostics.forElement(psiElement).any { it.factory in errors }) return true
        }
        return false
    }

    /**
     * 获取需要检查诊断信息的元素集合。
     */
    protected open fun elementsToCheckDiagnostics(): Collection<PsiElement> = listOfNotNull(element)

    /**
     * 填充候选描述符，具体实现由子类提供。
     */
    abstract fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor>

    /**
     * 根据依赖规则过滤候选描述符。
     */
    private fun reduceCandidatesBasedOnDependencyRuleViolation(
        candidates: Collection<DeclarationDescriptor>, file: PsiFile
    ): Collection<DeclarationDescriptor> {
        val project = file.project
        val validationManager = DependencyValidationManager.getInstance(project)
        return candidates.filter {
            val targetFile =
                DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)?.containingFile ?: return@filter true
            validationManager.getViolatorDependencyRules(file, targetFile).isEmpty()
        }
    }

    /**
     * 工厂类，用于创建具体的导入修复实例。
     */
    abstract class Factory : CangJieSingleIntentionActionFactory() {
        /**
         * 支持的错误类型集合。
         */
        val supportedErrors: Collection<DiagnosticFactory<*>> by lazy { QuickFixes.getInstance().getDiagnostics(this) }

        /**
         * 是否适用于代码片段。
         */
        override fun isApplicableForCodeFragment() = true

        /**
         * 创建具体的导入修复动作。
         */
        abstract fun createImportAction(diagnostic: Diagnostic): ImportFixBase<*>?

        /**
         * 判断是否有可用的动作。
         */
        override fun areActionsAvailable(diagnostic: Diagnostic): Boolean {
            val element = diagnostic.psiElement
            return element is CjExpression && element.references.isNotEmpty()
        }

        /**
         * 创建针对所有问题的导入修复动作列表。
         */
        open fun createImportActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<ImportFixBase<*>> =
            emptyList()

        /**
         * 创建具体的意图动作。
         */
        final override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return try {
                createImportAction(diagnostic)?.also { it.computeSuggestions() }
            } catch (ex: CangJieExceptionWithAttachments) {
                // 有时会失败，例如：
                // <production sources for module light_idea_test_case> 是一个模块 [ModuleDescriptorImpl@508c55a2]，不在解析器中...
                // TODO: 当问题解决后移除 try-catch
                if (AbstractImportFixInfo.IGNORE_MODULE_ERROR &&
                    ex.message?.contains("<production sources for module light_idea_test_case>") == true
                ) null
                else throw ex
            }
        }

        /**
         * 创建针对所有问题的所有意图动作。
         */
        override fun doCreateActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> =
            createImportActionsForAllProblems(sameTypeDiagnostics).onEach { it.computeSuggestions() }
    }

    /**
     * 继承自 Factory 的抽象类，用于处理未解析引用的快速修复。
     */
    abstract class FactoryWithUnresolvedReferenceQuickFix : Factory(), UnresolvedReferenceQuickFixFactory
}


/**
 * 抽象基类，用于处理普通导入修复问题，专注于处理表达式的导入相关问题。
 *
 * @param T 表达式的类型，必须是 CjExpression 的子类。
 * @param expression 需要处理的表达式对象。
 * @param factory 用于创建新表达式对象的工厂。
 */
abstract class OrdinaryImportFixBase<T : CjExpression>(expression: T, factory: Factory) :
    ImportFixBase<T>(expression, factory) {

    /**
     * 填充候选列表，返回符合条件的声明描述符列表。
     *
     * @param name 要查找的名称。
     * @param callTypeAndReceiver 调用类型及其接收者。
     * @param bindingContext 绑定上下文，用于解析表达式。
     * @param indicesHelper 索引助手，用于查找符号和顶级可调用项。
     * @return 符合条件的声明描述符列表。
     */
    override fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> {
        val expression = element ?: return emptyList()

        val result = ArrayList<DeclarationDescriptor>()

        // 处理简单名称表达式，过滤掉导入指令和限定选择器
        if (expression is CjSimpleNameExpression) {
            if (!expression.isImportDirectiveExpression() && !isSelectorInQualified(expression)) {
                ProgressManager.checkCanceled()
                val filterByCallType = callTypeAndReceiver.toFilter()

                // 查找分类器并过滤到结果中
                indicesHelper.getClassifiersByName(expression, name).filterTo(result, filterByCallType)

                // 查找顶级可调用项并过滤到结果中
                indicesHelper.getTopLevelCallablesByName(name).filterTo(result, filterByCallType)
            }
            // 处理操作符调用类型
            if (callTypeAndReceiver.callType == CallType.OPERATOR) {
                val type = expression.getCallableDescriptor()?.returnType ?: getReceiverTypeFromDiagnostic()
                if (type != null) {
                    // 添加顶级扩展函数到结果中
                    result.addAll(
                        indicesHelper.getCallableTopLevelExtensions(
                            callTypeAndReceiver,
                            listOf(type),
                            { it == name })
                    )
                }
            }
        }
        ProgressManager.checkCanceled()

        // 添加顶级扩展函数到结果中
        result.addAll(
            indicesHelper.getCallableTopLevelExtensions(
                callTypeAndReceiver,
                expression,
                bindingContext,
                findReceiverForDelegate(expression, callTypeAndReceiver.callType)
            ) { it == name }
        )

        // 获取包含文件，并计算默认和排除的导入项
        val cjFile = element?.getContainingCjFile() ?: return emptyList()
        val importedFqNamesAsAlias = getImportedFqNamesAsAlias(cjFile)
        val (defaultImports, excludedImports) = ImportInsertHelperImpl.computeDefaultAndExcludedImports(cjFile)

        // 过滤结果，确保导入路径不与默认或排除的导入项冲突
        return result.filter {
            val descriptor =
                it.takeUnless { expression.parent is CjCallExpression && it.isSealed() } ?: return@filter false
            val importableFqName = descriptor.importableFqName ?: return@filter true
            val importPath = ImportPath(importableFqName, isAllUnder = false)
            !importPath.isImported(defaultImports, excludedImports) || importableFqName in importedFqNamesAsAlias
        }
    }

    /**
     * 查找委托调用的接收者类型。
     *
     * @param expression 需要处理的表达式。
     * @param callType 调用类型。
     * @return 接收者类型，如果找不到则返回 null。
     */
    private fun findReceiverForDelegate(expression: CjExpression, callType: CallType<*>): CangJieType? {
        if (callType != CallType.DELEGATE) return null

        val receiverTypeFromDiagnostic = getReceiverTypeFromDiagnostic()
        if (receiverTypeFromDiagnostic?.constructor is TypeVariableTypeConstructor) {
            if (receiverTypeFromDiagnostic == expression.getCallableDescriptor()?.returnType) {
                // 如果整个 lambda 表达式无法解析，尝试独立分析最后一个表达式以猜测接收者类型
                return tryFindReceiverFromLambda(expression)
            }
        }

        return receiverTypeFromDiagnostic
    }

    /**
     * 尝试从 lambda 表达式中查找接收者类型。
     *
     * @param expression 需要处理的表达式。
     * @return 接收者类型，如果找不到则返回 null。
     */
    private fun tryFindReceiverFromLambda(expression: CjExpression): CangJieType? {
        if (expression !is CjCallExpression) return null
        val lambdaExpression = expression.singleLambdaArgumentExpression() ?: return null

        val lastStatement = CjPsiUtil.getLastStatementInABlock(lambdaExpression.bodyExpression) ?: return null
        val bindingContext = lastStatement.analyze(bodyResolveMode = BodyResolveMode.PARTIAL)
        return bindingContext.getType(lastStatement)
    }

    /**
     * 获取导入的 FQ 名称别名。
     *
     * @param cjFile 包含文件。
     * @return 导入的 FQ 名称别名列表。
     */
    private fun getImportedFqNamesAsAlias(cjFile: CjFile) =
        cjFile.importDirectivesItem
            .filter { it.alias != null }
            .mapNotNull { it.importedFqName }
}

// This is required to be abstract to reduce bunch file size

/**
 * 抽象类，用于修复 CangJie 简单名称表达式的导入问题。
 *
 * 该类继承自 [OrdinaryImportFixBase]，旨在为 CangJie 简单名称表达式的导入问题提供通用解决方案。
 * 主要功能包括检测调用类型和接收者、收集导入名称以及筛选候选声明以进行导入。
 *
 * @param expression 需要修复导入的 CangJie 简单名称表达式。
 * @param factory 用于创建新表达式或元素的工厂。
 */
abstract class AbstractImportFix(expression: CjSimpleNameExpression, factory: Factory) :
    OrdinaryImportFixBase<CjSimpleNameExpression>(expression, factory) {

    /**
     * 覆盖方法，用于检测当前元素的调用类型和接收者。
     *
     * 此方法对于确定表达式的使用方式至关重要，这将影响导入策略和候选选择。
     *
     * @return 表示元素调用类型和接收者的 [CallTypeAndReceiver] 对象。
     */
    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.detect(it) }

    /**
     * 收集成员的导入名称。
     *
     * 该方法检查元素是否具有有效的标识符，并返回相应的名称列表。如果元素无效，则返回空列表。
     *
     * @return 包含导入名称的集合。
     */
    private fun importNamesForMembers(): Collection<Name> {
        val element = element ?: return emptyList()

        if (element.identifier != null) {
            val name = element.referencedName
            if (Name.isValidIdentifier(name)) {
                return listOf(Name.identifier(name))
            }
        }

        return emptyList()
    }

    /**
     * 获取需要导入的名称集合。
     *
     * 该属性结合了主引用解析的名称和成员导入名称，并去重后返回。
     */
    override val importNames: Collection<Name> =
        ((element?.mainReference?.resolvesByNames ?: emptyList()) + importNamesForMembers()).distinct()

    /**
     * 收集成员候选声明。
     *
     * 该方法根据给定的名称、调用类型和接收者、绑定上下文以及索引助手，收集符合条件的声明描述符。
     *
     * @param name 要查找的名称。
     * @param callTypeAndReceiver 调用类型和接收者。
     * @param bindingContext 绑定上下文。
     * @param indicesHelper 索引助手。
     * @return 符合条件的声明描述符列表。
     */
    private fun collectMemberCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> {
        val element = element ?: return emptyList()
        if (element.isImportDirectiveExpression()) return emptyList()

        val result = ArrayList<DeclarationDescriptor>()

        val filterByCallType = callTypeAndReceiver.toFilter()

        indicesHelper.getCangJieEnumsByName(name).filterTo(result, filterByCallType)

        ProgressManager.checkCanceled()

        val actualReceivers = getReceiversForExpression(element, callTypeAndReceiver, bindingContext)

        if (isSelectorInQualified(element) && actualReceivers.explicitReceivers.isEmpty()) {
            // 如果元素是限定的，并且没有找到任何显式接收者，这意味着限定符不是值（例如，可能是类型名）。
            // 在这种情况下，我们不建议任何导入修复，因为不可能导入一个可以在非值限定符上调用的函数。
            // 这样的函数（例如静态函数）应该在没有导入的情况下成功解析。
            return emptyList()
        }

        val checkDispatchReceiver = when (callTypeAndReceiver) {
            is CallTypeAndReceiver.OPERATOR -> true
            else -> false
        }

        val processor = { descriptor: CallableDescriptor ->
            ProgressManager.checkCanceled()
            if (descriptor.canBeReferencedViaImport() && filterByCallType(descriptor)) {
                if (descriptor.extensionReceiverParameter != null) {
                    result.addAll(
                        descriptor.substituteExtensionIfCallable(
                            actualReceivers.explicitReceivers.ifEmpty { actualReceivers.allReceivers },
                            callTypeAndReceiver.callType
                        )
                    )
                } else if (descriptor.isValidByReceiversFor(actualReceivers, checkDispatchReceiver)) {
                    result.add(descriptor)
                }
            }
        }

        return result
    }

    /**
     * 当前最多只能在一个表达式中使用一个显式接收者，但将来可能会改变，
     * 因此我们使用 `Collection` 来表示显式接收者。
     */
    private class Receivers(val explicitReceivers: Collection<CangJieType>, val allReceivers: Collection<CangJieType>)

    /**
     * 获取表达式的接收者。
     *
     * 该方法根据给定的元素、调用类型和接收者、绑定上下文，获取实际的接收者类型。
     *
     * @param element CangJie 简单名称表达式。
     * @param callTypeAndReceiver 调用类型和接收者。
     * @param bindingContext 绑定上下文。
     * @return 包含显式接收者和所有接收者的 [Receivers] 对象。
     */
    private fun getReceiversForExpression(
        element: CjSimpleNameExpression,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext
    ): Receivers {
        val resolutionFacade = element.getResolutionFacade()
        val actualReceiverTypes = callTypeAndReceiver
            .receiverTypesWithIndex(
                bindingContext, element,
                resolutionFacade.moduleDescriptor, resolutionFacade,
                stableSmartCastsOnly = false,
                withImplicitReceiversWhenExplicitPresent = true
            ).orEmpty()

        val explicitReceiverType = actualReceiverTypes.filterNot { it.implicit }

        return Receivers(
            explicitReceiverType.map { it.type },
            actualReceiverTypes.map { it.type }
        )
    }

    /**
     * 检查可调用描述符是否可以根据给定的实际接收者在作用域内调用。
     *
     * 该方法仅接受没有扩展接收者的可调用描述符，因为它忽略了泛型并且不执行任何替换。
     *
     * @param actualReceivers 实际接收者。
     * @param checkDispatchReceiver 是否检查分发接收者。
     * @return 如果描述符可以在给定的实际接收者范围内调用，则返回 true。
     */
    private fun CallableDescriptor.isValidByReceiversFor(
        actualReceivers: Receivers,
        checkDispatchReceiver: Boolean
    ): Boolean {
        require(extensionReceiverParameter == null) { "此方法仅适用于非扩展可调用描述符，但得到了 $this" }

        val dispatcherReceiver = dispatchReceiverParameter.takeIf { checkDispatchReceiver }

        return if (dispatcherReceiver == null) {
            actualReceivers.explicitReceivers.isEmpty()
        } else {
            val typesToCheck = with(actualReceivers) { explicitReceivers.ifEmpty { allReceivers } }
            typesToCheck.any { it.isSubtypeOf(dispatcherReceiver.type) }
        }
    }

    /**
     * 填充候选声明。
     *
     * 该方法结合父类的候选声明和成员候选声明，返回最终的候选声明列表。
     *
     * @param name 要查找的名称。
     * @param callTypeAndReceiver 调用类型和接收者。
     * @param bindingContext 绑定上下文。
     * @param indicesHelper 索引助手。
     * @return 候选声明描述符列表。
     */
    override fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> =
        super.fillCandidates(name, callTypeAndReceiver, bindingContext, indicesHelper) + collectMemberCandidates(
            name,
            callTypeAndReceiver,
            bindingContext,
            indicesHelper
        )
}

/**
 * 用于管理导入修复相关的配置信息的对象。
 */
object AbstractImportFixInfo {
    /**
     * 是否忽略模块错误的标志，使用 @Volatile 注解确保线程安全。
     */
    @Volatile
    internal var IGNORE_MODULE_ERROR = false

    /**
     * 在测试环境中忽略模块错误。
     * 设置 IGNORE_MODULE_ERROR 为 true，并注册一个回调函数，在 disposable 被处理时将其重置为 false。
     *
     * @param disposable Disposable 对象，用于管理生命周期。
     */
    @TestOnly
    fun ignoreModuleError(disposable: Disposable) {
        IGNORE_MODULE_ERROR = true
        Disposer.register(disposable) { IGNORE_MODULE_ERROR = false }
    }

}

/**
 * 将 CallTypeAndReceiver 转换为过滤器函数，用于筛选符合调用类型的声明描述符。
 */
private fun CallTypeAndReceiver<*, *>.toFilter() = { descriptor: DeclarationDescriptor ->
    callType.descriptorKindFilter.accepts(descriptor)
}

/**
 * 根据名称获取分类描述符（ClassifierDescriptor）集合。
 *
 * @param useSiteExpression 使用位置表达式。
 * @param name 分类描述符的名称。
 * @return 包含所有匹配的分类描述符的集合。
 */
private fun CangJieIndicesHelper.getClassifiersByName(
    useSiteExpression: CjExpression,
    name: String,
): Collection<ClassifierDescriptor> = buildList {
    // 添加所有与名称匹配的类描述符
    addAll(getClassesByName(useSiteExpression, name))

    // 处理顶层类型别名并添加到结果列表中
    processTopLevelTypeAliases({ it == name }, { add(it) })
}

/**
 * 根据名称获取类描述符（ClassDescriptor）集合。
 *
 * @param expressionForPlatform 表达式用于平台。
 * @param name 类描述符的名称。
 * @return 包含所有匹配的类描述符的集合。
 */
private fun CangJieIndicesHelper.getClassesByName(
    expressionForPlatform: CjExpression,
    name: String
): Collection<ClassDescriptor> {

    val result = mutableListOf<ClassDescriptor>()
    val processor = Processors.cancelableCollectProcessor(result)

    // 处理类，排除枚举条目
    processCangJieClasses(
        nameFilter = { it == name },
        psiFilter = { cjDeclaration -> cjDeclaration !is CjEnumEntry },
        kindFilter = { kind -> kind != ClassKind.ENUM_ENTRY },
        processor = processor::process
    )
    return result
}

/**
 * 导入构造函数引用修复类，继承自 ImportFixBase。
 *
 * @param expression 构造函数引用表达式。
 */
internal class ImportConstructorReferenceFix(expression: CjSimpleNameExpression) :
    ImportFixBase<CjSimpleNameExpression>(expression, MyFactory) {

    override fun getCallTypeAndReceiver() = null

    /**
     * 填充候选的声明描述符列表。
     *
     * @param name 名称。
     * @param callTypeAndReceiver 调用类型和接收者。
     * @param bindingContext 绑定上下文。
     * @param indicesHelper 索引帮助器。
     * @return 包含所有匹配的声明描述符的列表。
     */
    override fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> {
        val expression = element ?: return emptyList()

        val filterByCallType = callTypeAndReceiver.toFilter()
        return indicesHelper.getClassifiersByName(expression, name)
            .asSequence()
            .flatMap { it.getConstructors() }
            .filter { it.importableFqName != null }
            .filter(filterByCallType)
            .toList()
    }

    /**
     * 创建导入操作。
     *
     * @param editor 编辑器。
     * @param element 元素。
     * @param suggestions 建议的完全限定名集合。
     * @return 返回导入操作。
     */
    override fun createAction(
        editor: Editor,
        element: CjExpression,
        suggestions: Collection<FqName>
    ): CangJieAddImportAction {
        return createSingleImportActionForConstructor(element.project, editor, element, suggestions)
    }

    /**
     * 获取导入名称列表。
     */
    override val importNames = element?.mainReference?.resolvesByNames ?: emptyList()

    /**
     * 工厂伴生对象，用于创建导入操作。
     */
    companion object MyFactory : FactoryWithUnresolvedReferenceQuickFix() {
        override fun createImportAction(diagnostic: Diagnostic) =
            diagnostic.psiElement.safeAs<CjSimpleNameExpression>()?.let(::ImportConstructorReferenceFix)
    }
}
