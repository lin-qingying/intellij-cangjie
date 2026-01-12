/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.indices.CangJieIndicesHelper
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.codeinsight.ReferenceVariantsHelper
import org.cangnova.cangjie.completion.handlers.InsertHandlerProvider
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.indices.isExcludedFromAutoImport
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.isInsideAnnotationEntryArgumentList
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.references.mainReference
import org.cangnova.cangjie.references.resolveCDocLink
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.resolve.calls.util.receiverTypesWithIndex
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.resolve.scopes.getResolveScope
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver

import org.cangnova.cangjie.types.checker.CangJieTypeChecker

import org.cangnova.cangjie.utils.*
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.cangnova.cangjie.NotPropertiesService
import org.cangnova.cangjie.imports.ImportInsertHelper
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.moduleinfo.ModuleOrigin
import org.cangnova.cangjie.moduleinfo.OriginCapability
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.resolve.calls.util.ReceiverType
import org.cangnova.cangjie.resolve.denotedClassDescriptor
import org.cangnova.cangjie.resolve.deprecation.compareDescriptors
import org.cangnova.cangjie.resolve.deprecation.isVisible
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.makeNonOption


/**
 * 代码补全会话配置
 *
 * @property useBetterPrefixMatcherForNonImportedClasses 是否对未导入的类使用更好的前缀匹配器
 * @property nonAccessibleDeclarations 是否包含不可访问的声明
 * @property staticMembers 是否包含静态成员
 */
class CompletionSessionConfiguration(
    val useBetterPrefixMatcherForNonImportedClasses: Boolean,
    val nonAccessibleDeclarations: Boolean,
    val staticMembers: Boolean,
)

/**
 * 根据补全参数创建会话配置
 *
 * 根据调用次数动态调整配置:
 * - invocationCount < 2: 只显示基本建议
 * - invocationCount >= 2: 显示更多建议(不可访问的声明、静态成员等)
 */
fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
    useBetterPrefixMatcherForNonImportedClasses = parameters.invocationCount < 2,
    nonAccessibleDeclarations = parameters.invocationCount >= 2,
    staticMembers = parameters.invocationCount >= 2,
)

/**
 * 代码补全会话的抽象基类
 *
 * 负责管理代码补全的整个生命周期,包括:
 * - 收集候选项(从作用域、索引、运行时类型等)
 * - 过滤和排序结果
 * - 处理接收者类型和上下文信息
 * - 与 IntelliJ 补全系统集成
 *
 * @property configuration 补全会话配置
 * @property parameters 经过处理的补全参数(包含修正后的位置)
 * @property resultSet 补全结果集
 */
abstract class CompletionSession(
    protected val configuration: CompletionSessionConfiguration,
    originalParameters: CompletionParameters,
    resultSet: CompletionResultSet
) {
    init {
        CompletionBenchmarkSink.instance.onCompletionStarted(this)
    }

    // 补全参数: 修正位置后的参数(添加参数类型信息)
    protected val parameters = run {
        val fixedPosition = addParamTypesIfNeeded(originalParameters.position)
        originalParameters.withPosition(fixedPosition, fixedPosition.textOffset)
    }

    // 原始文件到合成文件的映射器(用于处理代码片段)
    protected val toFromOriginalFileMapper = ToFromOriginalFileMapper.create(this.parameters)

    // 当前补全位置
    protected val position = this.parameters.position

    // 当前文件
    protected val file = position.containingFile as CjFile

    // 解析门面(提供类型推导、作用域分析等功能)
    protected val resolutionFacade = file.getResolutionFacade()

    // 模块描述符
    protected val moduleDescriptor = resolutionFacade.moduleDescriptor

    // 当前项目
    protected val project = position.project

    // 是否在调试器上下文中(代码片段评估)
    protected val isDebuggerContext = file is CjCodeFragment

    // 补全位置的名称表达式(如 foo.ba|r 中的 bar)
    protected val nameExpression: CjSimpleNameExpression?

    // 补全位置的完整表达式
    protected val expression: CjExpression?

    // 声明适用性过滤器(根据上下文决定哪些声明可以建议)
    protected val applicabilityFilter: (DeclarationDescriptor) -> Boolean

    // 初始化名称表达式、完整表达式和适用性过滤器
    init {
        val reference = (position.parent as? CjSimpleNameExpression)?.mainReference
        if (reference != null) {
            if (reference.expression is CjLabelReferenceExpression) {
                // 标签引用: 表达式是包含标签的语句
                this.nameExpression = null
                this.expression =
                    reference.expression.parents.match(CjContainerNode::class, last = CjExpressionWithLabel::class)
            } else {
                // 普通引用: 名称表达式就是引用表达式本身
                this.nameExpression = reference.expression
                this.expression = nameExpression
            }
        } else {
            this.nameExpression = null
            this.expression = null
        }

        // 在注解参数列表中需要特殊过滤(只允许编译期常量)
        if (position.isInsideAnnotationEntryArgumentList()) {
            applicabilityFilter = { suggestDescriptorInsideAnnotationEntryArgumentList(it, expectedInfos) }
        } else {
            applicabilityFilter = { true }
        }
    }

    // 可导入的全限定名分类器(判断符号是否已通过默认导入)
    protected val importableFqNameClassifier = ImportableFqNameClassifier(file) {
        ImportInsertHelper.getInstance(file.project).isImportedWithDefault(ImportPath(it, false), file)
    }

    // 可见性过滤器: 判断声明是否对当前位置可见
    protected val isVisibleFilter: (DeclarationDescriptor) -> Boolean =
        { isVisibleDescriptor(it, completeNonAccessible = configuration.nonAccessibleDeclarations) }

    // 补全前缀(用户已输入的文本)
    protected val prefix = CompletionUtil.findIdentifierPrefix(
        originalParameters.position.containingFile,
        originalParameters.offset,
        cangjieIdentifierPartPattern(),
        cangjieIdentifierStartPattern()
    )

    // 绑定上下文(包含类型推导、解析信息等)
    protected val bindingContext =
        CompletionBindingContextProvider.getInstance(project).getBindingContext(position, resolutionFacade)

    // 前缀匹配器(支持驼峰匹配)
    protected val prefixMatcher = CamelHumpMatcher(prefix)

    // 调用类型和接收者(判断补全位置的上下文: 普通调用、点调用、安全调用等)
    protected val callTypeAndReceiver =
        if (nameExpression == null) CallTypeAndReceiver.UNKNOWN else CallTypeAndReceiver.detect(nameExpression)

    // 是否允许建议预期声明(目前总是 false)
    protected val allowExpectedDeclarations = false

    // LookupElement 收集器(延迟初始化,因为需要子类的 createSorter 方法)
    protected val collector: LookupElementsCollector by lazy(LazyThreadSafetyMode.NONE) {
        LookupElementsCollector(
            { CompletionBenchmarkSink.instance.onFlush(this) },
            prefixMatcher, originalParameters, resultSet,
            createSorter(), (file as? CjCodeFragment)?.extraCompletionFilter,
            allowExpectedDeclarations,
        )
    }

    // 基础 LookupElement 工厂(创建补全项的 UI 表示)
    protected val basicLookupElementFactory =
        BasicLookupElementFactory(
            project,
            InsertHandlerProvider(callTypeAndReceiver.callType, parameters.editor) { expectedInfos })

    // 当前补全位置所在的描述符(函数、类、文件等)
    private val inDescriptor = position.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

    // 接收者类型列表(用于判断成员补全时的接收者,如 foo.bar 中 foo 的类型)
    protected val receiverTypes: List<ReceiverType>? =
        nameExpression?.let { detectReceiverTypes(bindingContext, nameExpression, callTypeAndReceiver) }
            ?: (position.parent as? CDocName)?.let { detectReceiverTypesForCDocName(bindingContext, it) }

    // 描述符名称过滤器(根据前缀过滤候选项名称)
    protected val descriptorNameFilter: (String) -> Boolean = prefixMatcher.asStringNameFilter()

    // 描述符类型过滤器(由子类实现,决定接受哪些类型的声明)
    protected abstract val descriptorKindFilter: DescriptorKindFilter?

    // 预期信息集合(根据补全上下文推断的预期类型、名称等)
    protected abstract val expectedInfos: Collection<ExpectedInfo>

    // 搜索作用域(决定在哪些文件中查找候选项)
    protected val searchScope: GlobalSearchScope =
        getResolveScope(originalParameters.originalFile as CjFile)

    // 严格的可见性过滤器(总是不包含不可访问的声明)
    protected val isVisibleFilterCheckAlways: (DeclarationDescriptor) -> Boolean =
        { isVisibleDescriptor(it, completeNonAccessible = false) }

    // 引用变体助手(从作用域收集可用的引用)
    protected val referenceVariantsHelper = ReferenceVariantsHelper(
        bindingContext,
        resolutionFacade,
        moduleDescriptor,
        isVisibleFilter,
        NotPropertiesService.getNotProperties(position)
    )

    // 阴影声明过滤器(过滤被同名导入声明遮蔽的未导入声明)
    protected val shadowedFilter: ((Collection<DeclarationDescriptor>) -> Collection<DeclarationDescriptor>)? by lazy {
        ShadowedDeclarationsFilter.create(
            bindingContext = bindingContext,
            resolutionFacade = resolutionFacade,
            context = nameExpression!!,
            callTypeAndReceiver = callTypeAndReceiver,
        )?.createNonImportedDeclarationsFilter(
            importedDeclarations = referenceVariantsCollector!!.allCollected.imported,
            allowExpectedDeclarations = allowExpectedDeclarations,
        )
    }

    // 引用变体收集器(收集所有可能的引用候选项)
    protected val referenceVariantsCollector = if (nameExpression != null) {
        ReferenceVariantsCollector(
            referenceVariantsHelper = referenceVariantsHelper,
            indicesHelper = indicesHelper(true),
            prefixMatcher = prefixMatcher,
            applicabilityFilter = applicabilityFilter,
            nameExpression = nameExpression,
            callTypeAndReceiver = callTypeAndReceiver,
            resolutionFacade = resolutionFacade,
            bindingContext = bindingContext,
            importableFqNameClassifier = importableFqNameClassifier,
            configuration = configuration,
            allowExpectedDeclarations = allowExpectedDeclarations,
        )
    } else {
        null
    }

    /**
     * 判断描述符是否对当前位置可见
     *
     * @param descriptor 待检查的声明描述符
     * @param completeNonAccessible 是否允许不可访问的声明(取决于调用次数)
     * @return true 如果声明可见或允许不可访问的声明
     */
    private fun isVisibleDescriptor(descriptor: DeclarationDescriptor, completeNonAccessible: Boolean): Boolean {
        // 类型参数需要在其声明范围内才可见
        if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val visible = descriptor.isVisible(
                position,
                callTypeAndReceiver.receiver as? CjExpression,
                bindingContext,
                resolutionFacade
            )
            if (visible) return true
            // 允许不可访问的声明,但库中的声明只在调试器上下文中允许
            return completeNonAccessible && (!descriptor.isFromLibrary() || isDebuggerContext)
        }

        // 检查是否在排除列表中(如自动导入黑名单)
        val fqName = descriptor.importableFqName
        return fqName == null || !fqName.isExcludedFromAutoImport(project, file)
    }

    /**
     * 判断类型参数是否在当前位置可见
     *
     * 类型参数只在其声明的类或函数内部可见
     */
    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
        val owner = typeParameter.containingDeclaration
        var parent: DeclarationDescriptor? = inDescriptor
        while (parent != null) {
            if (parent == owner) return true
            // 遇到类边界时停止(类型参数不能跨类使用)
            if (parent is ClassDescriptor) return false
            parent = parent.containingDeclaration
        }
        return true
    }

    /**
     * 判断描述符是否来自库
     *
     * 库中的声明在可见性检查时有特殊处理
     */
    private fun DeclarationDescriptor.isFromLibrary(): Boolean {
        if (module.getCapability(OriginCapability) == ModuleOrigin.LIBRARY) return true

        // 假重写的成员:检查所有被重写的声明是否来自库
        if (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return overriddenDescriptors.all { it.isFromLibrary() }
        }

        return false
    }

    /**
     * 获取运行时接收者类型的引用变体
     *
     * 在调试器上下文中,表达式的运行时类型可能比静态类型更具体
     * 例如: val x: Any = "hello",运行时 x 的类型是 String,可以补全 String 的方法
     *
     * @param lookupElementFactory LookupElement 工厂
     * @return 运行时类型的引用变体和新的工厂,如果无法获取运行时类型则返回 null
     */
    protected fun getRuntimeReceiverTypeReferenceVariants(lookupElementFactory: LookupElementFactory): Pair<ReferenceVariants, LookupElementFactory>? {
        val evaluator = file.getCopyableUserData(CodeFragmentUtils.RUNTIME_TYPE_EVALUATOR) ?: return null
        val referenceVariants = referenceVariantsCollector?.allCollected ?: return null

        val explicitReceiver = callTypeAndReceiver.receiver as? CjExpression ?: return null
        val type = bindingContext.getType(explicitReceiver) ?: return null
        // 如果类型不能有子类型(如 final 类),则不需要运行时类型
        if (!TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, type)) return null

        val runtimeType = evaluator(explicitReceiver)
        if (runtimeType == null || runtimeType == type) return null

        val expressionReceiver = ExpressionReceiver.create(explicitReceiver, runtimeType, bindingContext)
        val (variants, notImportedExtensions) = ReferenceVariantsCollector(
            referenceVariantsHelper = referenceVariantsHelper,
            indicesHelper = indicesHelper(true),
            prefixMatcher = prefixMatcher,
            applicabilityFilter = applicabilityFilter,
            nameExpression = nameExpression!!,
            callTypeAndReceiver = callTypeAndReceiver,
            resolutionFacade = resolutionFacade,
            bindingContext = bindingContext,
            importableFqNameClassifier = importableFqNameClassifier,
            configuration = configuration,
            allowExpectedDeclarations = allowExpectedDeclarations,
            runtimeReceiver = expressionReceiver,
        ).collectReferenceVariants(descriptorKindFilter!!)

        val filteredVariants = filterVariantsForRuntimeReceiverType(variants, referenceVariants.imported)
        val filteredNotImportedExtensions =
            filterVariantsForRuntimeReceiverType(notImportedExtensions, referenceVariants.notImportedExtensions)

        val runtimeVariants = ReferenceVariants(filteredVariants, filteredNotImportedExtensions)
        return Pair(runtimeVariants, lookupElementFactory.copy(receiverTypes = listOf(ReceiverType(runtimeType, 0))))
    }

    /**
     * 处理顶层可调用声明(包级函数、属性等)
     *
     * @param processor 处理每个找到的声明的回调
     */
    protected fun processTopLevelCallables(processor: (DeclarationDescriptor) -> Unit) {
        indicesHelper(true).processTopLevelCallables({ prefixMatcher.prefixMatches(it) }) {
            processWithShadowedFilter(it, processor)
        }
    }

    /**
     * 是否应该从索引中补全顶层可调用声明
     *
     * @return true 如果当前上下文适合补全顶层可调用声明
     */
    protected open fun shouldCompleteTopLevelCallablesFromIndex(): Boolean {
        if (nameExpression == null) return false
        if ((descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.CALLABLES_MASK) == 0) return false
        if (callTypeAndReceiver is CallTypeAndReceiver.IMPORT_DIRECTIVE) return false
        return callTypeAndReceiver.receiver == null
    }

    /**
     * 使用阴影声明过滤器处理描述符
     *
     * 过滤掉被同名导入声明遮蔽的未导入声明
     */
    protected inline fun <reified T : DeclarationDescriptor> processWithShadowedFilter(
        descriptor: T,
        processor: (T) -> Unit
    ) {
        val shadowedFilter = shadowedFilter
        val element = if (shadowedFilter != null) {
            shadowedFilter(listOf(descriptor)).singleOrNull()?.let { it as T }
        } else {
            descriptor
        }

        element?.let(processor)
    }

    /**
     * 使用上下文变量提供器执行操作
     *
     * @param contextVariablesProvider 提供上下文变量的提供器
     * @param action 使用 LookupElement 工厂执行的操作
     */
    protected fun withContextVariablesProvider(
        contextVariablesProvider: ContextVariablesProvider,
        action: (LookupElementFactory) -> Unit
    ) {
        val lookupElementFactory = createLookupElementFactory(contextVariablesProvider)
        action(lookupElementFactory)
    }

    /**
     * 过滤运行时接收者类型的变体
     *
     * 只保留运行时类型新增的成员(基础类型中没有的)
     */
    private fun <TDescriptor : DeclarationDescriptor> filterVariantsForRuntimeReceiverType(
        runtimeVariants: Collection<TDescriptor>,
        baseVariants: Collection<TDescriptor>
    ): Collection<TDescriptor> {
        val baseVariantsByName = baseVariants.groupBy { it.name }
        val result = ArrayList<TDescriptor>()
        for (variant in runtimeVariants) {
            val candidates = baseVariantsByName[variant.name]
            if (candidates == null || candidates.none { compareDescriptors(project, variant, it) }) {
                result.add(variant)
            }
        }
        return result
    }

    /**
     * 获取具有单个函数类型参数的引用变体
     *
     * 用于 lambda 表达式的智能补全
     */
    protected fun referenceVariantsWithSingleFunctionTypeParameter(): ReferenceVariants? {
        val variants = referenceVariantsCollector?.allCollected ?: return null
        val filter = { descriptor: DeclarationDescriptor ->
            descriptor is FunctionDescriptor && LookupElementFactory.hasSingleFunctionTypeParameter(descriptor)
        }
        return ReferenceVariants(variants.imported.filter(filter), variants.notImportedExtensions.filter(filter))
    }

    /**
     * 创建 LookupElement 工厂
     *
     * @param contextVariablesProvider 上下文变量提供器
     * @return 配置好的 LookupElement 工厂
     */
    protected open fun createLookupElementFactory(contextVariablesProvider: ContextVariablesProvider): LookupElementFactory {
        return LookupElementFactory(
            basicLookupElementFactory, parameters.editor, receiverTypes,
            callTypeAndReceiver.callType, inDescriptor, contextVariablesProvider
        )
    }

    /**
     * 将收集的补全项刷新到结果集
     */
    protected fun flushToResultSet() {
        collector.flushToResultSet()
    }

    /**
     * 检测接收者类型
     *
     * @param bindingContext 绑定上下文
     * @param nameExpression 名称表达式
     * @param callTypeAndReceiver 调用类型和接收者
     * @return 接收者类型列表,如果无法检测则返回 null
     */
    protected fun detectReceiverTypes(
        bindingContext: BindingContext,
        nameExpression: CjSimpleNameExpression,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>
    ): List<ReceiverType>? {
        var receiverTypes = callTypeAndReceiver.receiverTypesWithIndex(
            bindingContext, nameExpression, moduleDescriptor, resolutionFacade,
            stableSmartCastsOnly = true, /* 对于不稳定的接收者值,不包含智能转换类型,以标记成员为灰色 */
            withImplicitReceiversWhenExplicitPresent = true
        )

        // 安全调用时,将接收者类型转换为非可选类型
        if (callTypeAndReceiver is CallTypeAndReceiver.SAFE || isDebuggerContext) {
            receiverTypes = receiverTypes?.map { ReceiverType(it.type.makeNonOption(), it.receiverIndex) }
        }

        return receiverTypes
    }

    /**
     * 检测 CDoc 名称的接收者类型
     *
     * 用于文档注释中的引用补全
     */
    private fun detectReceiverTypesForCDocName(
        context: BindingContext,
        cDocName: CDocName,
    ): List<ReceiverType>? {
        val cDocLink = cDocName.getStrictParentOfType<CDocLink>() ?: return null
        val cDocOwner = cDocName.getContainingDoc().getOwner()
        val cDocOwnerDescriptor = cDocOwner?.resolveToDescriptorIfAny() ?: return null

        return resolveCDocLink(
            context,
            resolutionFacade,
            cDocOwnerDescriptor,
            cDocLink,
            cDocLink.getTagIfSubject(),
            cDocLink.qualifier
        )
            .filterIsInstance<ClassifierDescriptorWithTypeParameters>()
            .mapNotNull { it.denotedClassDescriptor }
            .flatMap { listOfNotNull(it) }
            .map { ReceiverType(it.defaultType, receiverIndex = 0) }
    }

    /**
     * 执行补全
     *
     * @return true 如果找到至少一个补全项
     */
    fun complete(): Boolean {
        return try {
            _complete().also {
                CompletionBenchmarkSink.instance.onCompletionEnded(this, false)
            }
        } catch (pce: ProcessCanceledException) {
            CompletionBenchmarkSink.instance.onCompletionEnded(this, true)
            throw pce
        }
    }

    /**
     * 计算统计信息的上下文
     *
     * 用于补全项的统计排序
     */
    private fun calcContextForStatisticsInfo(): String? {
        TODO()
//        if (expectedInfos.isEmpty()) return null
//
//        var context = expectedInfos
//            .mapNotNull { it.fuzzyType?.type?.constructor?.declarationDescriptor?.importableFqName }
//            .distinct()
//            .singleOrNull()
//            ?.let { "expectedType=$it" }
//
//        if (context == null) {
//            context = expectedInfos
//                .mapNotNull { it.expectedName }
//                .distinct()
//                .singleOrNull()
//                ?.let { "expectedName=$it" }
//        }
//
//        return context
    }

    /**
     * 内部补全实现
     */
    private fun _complete(): Boolean {
        val prefixPattern = StandardPatterns.string().with(
            object : PatternCondition<String>("get or set prefix") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix == "get" || prefix == "set"
            }
        )
        collector.restartCompletionOnPrefixChange(prefixPattern)

        val statisticsContext = calcContextForStatisticsInfo()
        if (statisticsContext != null) {
            collector.addLookupElementPostProcessor { lookupElement ->
                // 将数据放入原始元素,因为 DecoratorCompletionStatistician 需要
                lookupElement.putUserDataDeep(STATISTICS_INFO_CONTEXT_KEY, statisticsContext)
                lookupElement
            }
        }

        doComplete()
        flushToResultSet()
        return !collector.isResultEmpty
    }

    /**
     * 添加 LookupElement 后处理器
     *
     * @param processor 处理 LookupElement 的函数
     */
    fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        collector.addLookupElementPostProcessor(processor)
    }

    /**
     * 执行实际的补全逻辑
     *
     * 由子类实现具体的补全策略
     */
    protected abstract fun doComplete()

    /**
     * 创建补全项排序器
     *
     * 配置补全项的排序规则,包括:
     * - 前缀匹配权重
     * - 可见性和导入状态
     * - 类型匹配度
     * - 统计信息等
     */
    protected open fun createSorter(): CompletionSorter {
        var sorter = CompletionSorter.defaultSorter(parameters, prefixMatcher)!!

        // 在 "lift.shorter" 之后添加真实前缀匹配权重器
        // (默认排序器中的 RealPrefixMatchingWeigher 位置不佳,在 "stats" 之后)
        sorter = sorter.weighAfter("lift.shorter", RealPrefixMatchingWeigher())

        return sorter
    }

    /**
     * 收集需要的上下文变量类型
     *
     * 用于智能补全时推断需要的变量类型
     *
     * @param action 使用 LookupElement 工厂执行的操作
     * @return 收集到的需要的类型集合
     */
    protected fun withCollectRequiredContextVariableTypes(action: (LookupElementFactory) -> Unit) {
        val provider = CollectRequiredTypesContextVariablesProvider()
        val lookupElementFactory = createLookupElementFactory(provider)
        action(lookupElementFactory)

    }

    /**
     * 创建索引助手
     *
     * @param mayIncludeInaccessible 是否可能包含不可访问的声明
     * @return 配置好的索引助手
     */
    protected fun indicesHelper(mayIncludeInaccessible: Boolean): CangJieIndicesHelper {
        val visibilityFilter = if (mayIncludeInaccessible) isVisibleFilter else isVisibleFilterCheckAlways
        return CangJieIndicesHelper(
            resolutionFacade,
            searchScope,
            visibilityFilter,
            applicabilityFilter = applicabilityFilter,
            filterOutPrivate = !mayIncludeInaccessible,
            declarationTranslator = { toFromOriginalFileMapper.toSyntheticFile(it) },
            file = file
        )
    }

    companion object {
        /**
         * 判断描述符是否适合在注解参数列表中建议
         *
         * 注解参数只能是编译期常量或特殊的数组构造函数
         */
        private fun suggestDescriptorInsideAnnotationEntryArgumentList(
            descriptor: DeclarationDescriptor,
            expectedInfos: Collection<ExpectedInfo>,
        ): Boolean {
            if (descriptor !is CallableDescriptor) return true

            return when (descriptor) {
                is FunctionDescriptor -> {
                    TODO()
//                    val fuzzyType = descriptor.returnType?.toFuzzyType(descriptor.typeParameters) ?: return false
//                    expectedInfos.isEmpty() || expectedInfos.any { it.fuzzyType?.checkIsSubtypeOf(fuzzyType) != null }
                }

                else -> false
            }
        }
    }
}

/**
 * CDocLink 的限定符
 *
 * 获取文档链接的限定部分(不包括最后一段)
 * 例如: "foo.bar.baz" -> ["foo", "bar"]
 */
val CDocLink.qualifier: List<String> get() = getLinkText().split('.').dropLast(1)
