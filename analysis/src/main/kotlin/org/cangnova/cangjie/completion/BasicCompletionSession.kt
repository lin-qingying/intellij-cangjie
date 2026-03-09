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

import org.cangnova.cangjie.resolve.analyzeInContext
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.indices.CangJieIndicesHelper
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.codeinsight.ReferenceVariantsHelper
import org.cangnova.cangjie.completion.keywords.DefaultCompletionKeywordHandlerProvider
import org.cangnova.cangjie.completion.keywords.KeywordCompletion
import org.cangnova.cangjie.completion.keywords.KeywordValues
import org.cangnova.cangjie.completion.keywords.createLookups
import org.cangnova.cangjie.completion.smart.ExpectedInfoMatch
import org.cangnova.cangjie.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import org.cangnova.cangjie.completion.smart.SmartCompletion
import org.cangnova.cangjie.completion.smart.SmartCompletionItemPriority

import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.*
import org.cangnova.cangjie.references.CjSimpleNameReference
import org.cangnova.cangjie.references.mainReference
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.types.FuzzyType
import org.cangnova.cangjie.utils.safeAs
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.*

import com.intellij.codeInsight.completion.impl.BetterPrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key


import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.NotPropertiesService
import org.cangnova.cangjie.completion.addingPolicy.PolicyController
import org.cangnova.cangjie.completion.turboComplete.CompletionKind
import org.cangnova.cangjie.completion.turboComplete.SuggestionGeneratorConsumer
import org.cangnova.cangjie.completion.turboComplete.SuggestionGeneratorWithArtifact
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.stubindex.CangJiePackageIndexUtils
import org.cangnova.cangjie.utils.getImportableDescriptor
import org.cangnova.cangjie.utils.importableFqName
import org.cangnova.cangjie.utils.isExtension
import org.cangnova.cangjie.utils.supertypesWithAny
import kotlin.collections.isNullOrEmpty
import kotlin.getValue


/**
 * 基础代码补全会话
 *
 * 仓颉语言补全系统的核心类，负责处理 Ctrl+Space 触发的标准补全。
 * 整体流程：
 *   1. detectCompletionCategory() 分析光标位置，确定补全场景
 *   2. 对应的 CompletionCategory 将补全逻辑拆分成多个 SuggestionGenerator
 *   3. 每个 Generator 通过 suggestionGeneratorConsumer 交给执行框架按需调用
 *   4. Generator 内部调用 flushToResultSet() 将候选项写入 IDE 补全列表
 *
 * @property configuration 补全会话配置（是否包含静态成员、是否使用更好的前缀匹配等）
 * @property completionParameters IntelliJ 补全参数（包含光标位置、触发方式等）
 * @property policyController 控制补全项添加策略（如去重、优先级等）
 * @property suggestionGeneratorConsumer 接收并调度 SuggestionGenerator 的消费者
 */
class BasicCompletionSession(
    configuration: CompletionSessionConfiguration,
    completionParameters: CompletionParameters,
    private val policyController: PolicyController,
    private val suggestionGeneratorConsumer: SuggestionGeneratorConsumer,
) : CompletionSession(configuration, completionParameters, policyController.getObeyingResultSet()) {

    /**
     * 补全类别接口
     *
     * 策略模式的核心接口，每种补全场景实现此接口：
     * - KEYWORDS_ONLY：只补全关键字
     * - NAMED_ARGUMENTS_ONLY：只补全具名参数
     * - ALL：全量补全
     * - DECLARATION_NAME：声明名称补全
     * - OPERATOR_NAME：运算符名称补全
     * - SUPER_QUALIFIER：super 限定符补全
     */
    private interface CompletionCategory {
        /** 此类别接受的描述符类型过滤器，null 表示不需要描述符（如纯关键字补全） */
        val descriptorKindFilter: DescriptorKindFilter?

        /** 生成此类别的所有补全候选项，内部通过 suggestionGeneratorConsumer 提交 Generator */
        fun generateCategories()

        /** 是否禁止自动弹出补全窗口（如用户正在输入声明名称时不应自动弹出） */
        fun shouldDisableAutoPopup(): Boolean = false

        /** 向排序器添加自定义权重器，影响补全列表的排序结果 */
        fun addWeighers(sorter: CompletionSorter): CompletionSorter = sorter
    }

    /** 当前补全结果集是否为空（用于判断是否需要触发第二轮补全） */
    val isNothingAddedToResult: Boolean
        get() = collector.isResultEmpty

    /**
     * 仅具名参数补全类别
     *
     * 当且仅当光标处于函数调用的具名参数位置时使用：
     *   foo(paramName = ▌)
     * 此时只应显示剩余未填写的参数名，不显示其他符号。
     */
    private val NAMED_ARGUMENTS_ONLY = object : OneKindCompletionCategory(CangJieCompletionKindName.NAMED_ARGUMENT) {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null
        override fun fillResultSet(): Unit =
            NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
    }

    /**
     * 检测当前光标位置对应的补全类别
     *
     * 按优先级依次判断：
     * 1. nameExpression 为 null（光标不在引用表达式上）
     *    → 检查是否是声明名称位置（val/fun 后面），否则仅关键字
     * 2. 运算符名称位置（operator fun ▌）→ OPERATOR_NAME
     * 3. 只期望具名参数（foo(▌) 且编译器分析结果要求只填具名参数）→ NAMED_ARGUMENTS_ONLY
     * 4. super 表达式内（super<▌>）→ SUPER_QUALIFIER
     * 5. 其他所有情况 → ALL（全量补全）
     */
    private fun detectCompletionCategory(): CompletionCategory {
        if (nameExpression == null) {
            // 光标在声明的名称标识符上（如 val myVar 中的 myVar 位置）
            return if ((position.parent as? CjNamedDeclaration)?.nameIdentifier == position)
                DECLARATION_NAME
            else
                KEYWORDS_ONLY
        }

        if (OPERATOR_NAME.isApplicable()) {
            return OPERATOR_NAME
        }

        // 编译器通过类型推断确认此处只能填具名参数
        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression, resolutionFacade)) {
            return NAMED_ARGUMENTS_ONLY
        }

        // 光标在 super<xxx> 的类型参数位置
        if (nameExpression.getStrictParentOfType<CjSuperExpression>() != null) {
            return SUPER_QUALIFIER
        }

        return ALL
    }

    /**
     * 全量补全类别（最复杂的补全场景）
     *
     * 覆盖所有常规补全情况，按子类型分成多个 SuggestionGenerator 并行/按需执行：
     *
     * 包含的补全子类型（按 CangJieCompletionKindName 分类）：
     * - KEYWORD_ONLY：关键字
     * - SMART_ADDITIONAL_ITEM：智能补全附加项
     * - REFERENCE_BASIC：作用域内已导入的基础引用
     * - REFERENCE_EXTENSION：未导入的扩展函数
     * - PACKAGE_NAME：顶层包名
     * - NAMED_ARGUMENT：具名参数
     * - EXTENSION_FUNCTION_TYPE_VALUE：函数类型接收者的扩展
     * - CONTEXT_VARIABLE_TYPE_SC：上下文变量类型的智能补全
     * - CONTEXT_VARIABLE_TYPE_REFERENCE：上下文变量类型的引用补全
     * - STATIC_MEMBER_FROM_IMPORTS：从已导入类的静态成员
     * - NON_IMPORTED：未导入的顶层符号和类
     * - DEBUGGER_VARIANTS：调试器上下文中的运行时类型变体
     * - STATIC_MEMBER_OBJECT_MEMBER：对象成员扩展
     * - STATIC_MEMBER_EXPLICIT_INHERITED：显式继承的静态成员扩展
     * - STATIC_MEMBER_INACCESSIBLE：不可直接访问的静态成员
     */
    private val ALL = object : CompletionCategory {

        /**
         * 当前调用类型允许的描述符类型过滤器。
         * 根据 callTypeAndReceiver.callType 决定，例如：
         * - DOT 调用 → 只允许成员函数/属性
         * - DEFAULT 调用 → 允许所有可见符号
         */
        override val descriptorKindFilter: DescriptorKindFilter by lazy {
            callTypeAndReceiver.callType.descriptorKindFilter
        }

        /**
         * 判断是否在扩展接收者类型的开始位置。
         * 仓颉语言使用 extend 块语法而非 receiverTypeReference，
         * 因此此方法始终返回 null（不存在此场景）。
         */
        private fun isStartOfExtensionReceiverFor(): CjCallableDeclaration? {
            return null
        }

        override fun generateCategories() {

            /**
             * 将引用变体（已导入 + 未导入扩展）添加到补全结果集。
             *
             * @param lookupElementFactory 用于将描述符转换为 LookupElement 的工厂
             * @param referenceVariants 引用变体，分为两部分：
             *   - imported：当前作用域内已可见的符号
             *   - notImportedExtensions：需要自动添加 import 的扩展函数
             */
            fun addReferenceVariants(
                lookupElementFactory: LookupElementFactory,
                referenceVariants: ReferenceVariants
            ) {
                // 已导入符号：排除尚未初始化的变量（避免在初始化器中引用自身）
                collector.addDescriptorElements(
                    referenceVariantsHelper.excludeNonInitializedVariable(referenceVariants.imported, position),
                    lookupElementFactory, prohibitDuplicates = true
                )

                // 未导入的扩展函数：添加时标记为 notImported，选中后会自动插入 import
                collector.addDescriptorElements(
                    referenceVariants.notImportedExtensions, lookupElementFactory,
                    notImported = true, prohibitDuplicates = true
                )
            }

            /**
             * 创建引用类型的 SuggestionGenerator 列表。
             *
             * 将描述符过滤器列表拆分为两个 Generator：
             * - basicReferencesKind：处理基础引用（已导入的成员、局部变量等）
             * - extensionReferencesKind：处理扩展函数引用
             *
             * 拆分目的：让 IDE 执行框架可以按优先级分批执行，
             * 先展示基础引用，再异步补充扩展函数。
             *
             * @param descriptors 按前缀首字母大小写拆分的描述符过滤器列表
             * @param lookupElementFactory LookupElement 工厂
             */
            fun makeReferenceSuggestionGenerators(
                descriptors: List<DescriptorKindFilter>,
                lookupElementFactory: LookupElementFactory
            ): List<SuggestionGeneratorWithArtifact<Unit>> {
                // 为每个过滤器创建对应的引用变体收集器
                val generators = descriptors.map { descriptorKindFilter ->
                    referenceVariantsCollector!!.makeReferenceVariantsCollectors(descriptorKindFilter)
                }

                // Generator 1：基础引用（已导入符号 + 基础扩展）
                val basicReferencesKind =
                    suggestionGeneratorForCompletionKind(CangJieCompletionKindName.REFERENCE_BASIC) {
                        generators.forEach {
                            addReferenceVariants(lookupElementFactory, it.basic.value)
                        }
                    }

                // Generator 2：扩展函数引用（需要 import 的扩展）
                val extensionReferencesKind =
                    suggestionGeneratorForCompletionKind(CangJieCompletionKindName.REFERENCE_EXTENSION) {
                        generators.forEach {
                            addReferenceVariants(lookupElementFactory, it.extensions.value)
                        }
                    }

                return listOf(basicReferencesKind, extensionReferencesKind)
            }

            /**
             * 收集引用并将 Generator 提交给消费者，返回一个懒加载的类型集合。
             *
             * 设计要点：
             * - Generator 立即提交（pass），但不立即执行
             * - 返回的 Lazy<Set<FuzzyType>> 在被访问时才确保所有 Generator 已执行完毕
             * - 这样保证：智能补全需要知道"哪些类型被引用了"时，引用收集一定已完成
             */
            fun collectReferences(descriptors: List<DescriptorKindFilter>): Lazy<Set<FuzzyType>> {
                val provider = CollectRequiredTypesContextVariablesProvider()
                val lookupElementFactory = createLookupElementFactory(provider)
                val generators = makeReferenceSuggestionGenerators(descriptors, lookupElementFactory)

                // 将所有 Generator 提交给消费者（框架决定何时执行）
                generators.forEach { suggestionGeneratorConsumer.pass(it) }

                return lazy {
                    // 访问此 lazy 时，强制所有 Generator 执行完毕
                    generators.forEach { it.getArtifact() }
                    // 通知收集器：所有引用变体已收集完成
                    referenceVariantsCollector!!.collectingFinished()
                    provider.requiredTypes
                }
            }

            /**
             * 执行智能补全（类型感知补全）的附加项收集。
             *
             * 智能补全会根据期望类型（expectedInfos）推断最合适的候选项，
             * 例如：val x: String = ▌ 时优先显示返回 String 的函数。
             * 所有附加项必须携带 SMART_COMPLETION_ITEM_PRIORITY_KEY，
             * 以便 SmartCompletionInBasicWeigher 将其排到列表前面。
             */
            fun completeWithSmartCompletion(lookupElementFactory: LookupElementFactory) {
                if (smartCompletion != null) {
                    val (additionalItems, _) = smartCompletion!!.additionalItems(lookupElementFactory)

                    // 确保每个智能补全项都有优先级标记
                    for (item in additionalItems) {
                        if (item.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY) == null) {
                            item.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.DEFAULT)
                        }
                    }

                    collector.addElements(additionalItems)
                }
            }

            // 检查是否在扩展接收者类型位置（仓颉语言始终为 null）
            val declaration = isStartOfExtensionReceiverFor()
            if (declaration != null) {
                completeDeclarationNameFromUnresolvedOrOverride(declaration)

                // 用户可能正在输入声明名称，抑制自动弹出以避免干扰
                if (parameters.invocationCount == 0 && (
                            declaration !is CjNamedFunction && declaration !is CjVariable<*> ||
                                    prefixMatcher.prefix.let { it.isEmpty() || it[0].isLowerCase() }
                            )
                ) {
                    if (declaration is CjNamedFunction &&
                        declaration.modifierList?.allChildren.orEmpty()
                            .map { it.node.elementType }
                            .none { it is CjModifierKeywordToken && it !in CjTokens.VISIBILITY_MODIFIERS }
                    ) {
                        KEYWORDS_ONLY.generateCategories()
                    }
                    return
                }
            }

            // ── 第一步：关键字补全（最快，直接从静态列表过滤）──
            KEYWORDS_ONLY.generateCategories()

            // ── 第二步：智能补全附加项（需要类型推断，较慢）──
            val contextVariableTypesForSmartCompletion = withCollectRequiredContextVariableTypes(
                CangJieCompletionKindName.SMART_ADDITIONAL_ITEM,
                ::completeWithSmartCompletion
            )

            // ── 第三步：按前缀首字母大小写拆分描述符过滤器 ──
            // 目的：让大写开头（类名）和小写开头（函数/变量）的候选分批处理，
            // 提升前缀匹配精度和性能
            val descriptors = when {
                // 无前缀 / 有接收者 / 大小写不敏感 → 统一处理
                prefix.isEmpty() ||
                        callTypeAndReceiver.receiver != null ||
                        CodeInsightSettings.getInstance().completionCaseSensitive == CodeInsightSettings.NONE
                    -> listOf(descriptorKindFilter)

                // 小写前缀：优先处理函数/变量，再处理类名
                prefix[0].isLowerCase() -> listOf(
                    USUALLY_START_LOWER_CASE.intersect(descriptorKindFilter),
                    USUALLY_START_UPPER_CASE.intersect(descriptorKindFilter)
                )

                // 大写前缀：优先处理类名，再处理函数/变量
                else -> listOf(
                    USUALLY_START_UPPER_CASE.intersect(descriptorKindFilter),
                    USUALLY_START_LOWER_CASE.intersect(descriptorKindFilter)
                )
            }

            // 提交引用收集 Generator，返回懒加载的类型集合（供后续智能补全使用）
            val references = collectReferences(descriptors)

            // ── 第四步：包名补全（从包索引查询，不走作用域，因为作用域查根包很慢）──
            if (callTypeAndReceiver.receiver == null &&
                callTypeAndReceiver.callType.descriptorKindFilter.kindMask
                    .and(DescriptorKindFilter.PACKAGES_MASK) != 0
            ) {
                addKind(CangJieCompletionKindName.PACKAGE_NAME) {
                    val packageNames = CangJiePackageIndexUtils.getSubPackageFqNames(
                        FqName.ROOT,
                        GlobalSearchScope.allScope(project),
                        prefixMatcher.asNameFilter()
                    ).toHashSet()

                    packageNames.forEach {
                        collector.addElement(
                            basicLookupElementFactory.createLookupElementForPackage(it)
                        )
                    }
                }
            }

            // ── 第五步：具名参数补全（在引用补全之后，避免优先级混乱）──
            addKind(CangJieCompletionKindName.NAMED_ARGUMENT) {
                NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
            }

            // ── 第六步：上下文变量相关的补全（函数类型接收者、上下文变量等）──
            val contextVariablesProvider = RealContextVariablesProvider(referenceVariantsHelper, position)
            withContextVariablesProvider(contextVariablesProvider) { lookupElementFactory ->

                // 函数类型值的扩展补全（如 block: () -> Unit 类型的变量上的 invoke 等）
                if (receiverTypes != null) {
                    addKind(CangJieCompletionKindName.EXTENSION_FUNCTION_TYPE_VALUE) {
                        ExtensionFunctionTypeValueCompletion(
                            receiverTypes,
                            callTypeAndReceiver.callType,
                            lookupElementFactory
                        )
                            .processVariables(contextVariablesProvider)
                            .forEach {
                                val lookupElements = it.factory.createStandardLookupElementsForDescriptor(
                                    it.invokeDescriptor,
                                    useReceiverTypes = true,
                                )
                                collector.addElements(lookupElements)
                            }
                    }
                }

                // 上下文变量类型匹配的智能补全（仅在有匹配的函数类型变量时触发）
                addKind(CangJieCompletionKindName.CONTEXT_VARIABLE_TYPE_SC) {
                    if (contextVariableTypesForSmartCompletion.getArtifact().any {
                            contextVariablesProvider.functionTypeVariables(it).isNotEmpty()
                        }) {
                        completeWithSmartCompletion(lookupElementFactory)
                    }
                }

                // 上下文变量类型匹配的引用补全（仅在有匹配的函数类型变量时触发）
                addKind(CangJieCompletionKindName.CONTEXT_VARIABLE_TYPE_REFERENCE) {
                    if (references.value.any {
                            contextVariablesProvider.functionTypeVariables(it).isNotEmpty()
                        }) {
                        val (imported, notImported) = referenceVariantsWithSingleFunctionTypeParameter()!!
                        collector.addDescriptorElements(imported, lookupElementFactory)
                        collector.addDescriptorElements(notImported, lookupElementFactory, notImported = true)
                    }
                }

                /**
                 * 静态成员补全（懒加载）
                 * references.value 的访问确保引用收集已完成，
                 * allCollected.imported 包含了所有已收集的已导入描述符，
                 * 用于过滤掉重复的静态成员候选。
                 */
                val staticMembersCompletion = lazy {
                    references.value  // 确保引用已收集完毕
                    StaticMembersCompletion(
                        prefixMatcher,
                        resolutionFacade,
                        lookupElementFactory,
                        referenceVariantsCollector!!.allCollected.imported,
                    )
                }

                // 从已导入类中补全静态成员（DEFAULT 调用类型，如直接写类名访问静态成员）
                if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                    addKind(CangJieCompletionKindName.STATIC_MEMBER_FROM_IMPORTS) {
                        staticMembersCompletion.value.completeFromImports(file, collector)
                    }
                }

                // 未导入符号补全（顶层函数、未导入的类等，需要添加 import）
                addKind(CangJieCompletionKindName.NON_IMPORTED) {
                    contextVariableTypesForSmartCompletion.getArtifact()  // 确保智能补全类型已收集
                    references.value                                        // 确保引用已收集
                    completeNonImported(lookupElementFactory)
                }

                // 调试器专用补全（运行时实际类型的成员，仅在调试会话中启用）
                if (isDebuggerContext) {
                    addKind(CangJieCompletionKindName.DEBUGGER_VARIANTS) {
                        val variantsAndFactory = getRuntimeReceiverTypeReferenceVariants(lookupElementFactory)
                        if (variantsAndFactory != null) {
                            val variants = variantsAndFactory.first
                            val resultLookupElementFactory = variantsAndFactory.second
                            // 已导入的运行时类型成员（带接收者类型转换）
                            collector.addDescriptorElements(
                                variants.imported,
                                resultLookupElementFactory,
                                withReceiverCast = true
                            )
                            // 未导入的运行时类型扩展
                            collector.addDescriptorElements(
                                variants.notImportedExtensions,
                                resultLookupElementFactory,
                                withReceiverCast = true,
                                notImported = true
                            )
                        }
                    }
                }

                // 有接收者类型时，补全来自 object 的扩展成员和继承的扩展成员
                if (!receiverTypes.isNullOrEmpty()) {
                    val shouldCompleteExtensionsFromObjects = when (callTypeAndReceiver.callType) {
                        CallType.DEFAULT, CallType.DOT, CallType.SAFE -> true
                        else -> false
                    }

                    if (shouldCompleteExtensionsFromObjects) {
                        val receiverCangJieTypes by lazy { receiverTypes.map { it.type } }

                        // 从 object 单例中查找匹配接收者类型的扩展函数
                        addKind(CangJieCompletionKindName.STATIC_MEMBER_OBJECT_MEMBER) {
                            staticMembersCompletion.value.completeObjectMemberExtensionsFromIndices(
                                indicesHelper(mayIncludeInaccessible = false),
                                receiverCangJieTypes,
                                callTypeAndReceiver,
                                collector
                            )
                        }

                        // 从继承链和显式导入中查找匹配接收者类型的扩展函数
                        addKind(CangJieCompletionKindName.STATIC_MEMBER_EXPLICIT_INHERITED) {
                            staticMembersCompletion.value.completeExplicitAndInheritedMemberExtensionsFromIndices(
                                indicesHelper(mayIncludeInaccessible = false),
                                receiverCangJieTypes,
                                callTypeAndReceiver,
                                collector
                            )
                        }
                    }
                }

                // 有前缀时，从全局索引中查找不可直接访问的静态成员（需要限定符才能访问）
                if (configuration.staticMembers && prefix.isNotEmpty()) {
                    if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                        addKind(CangJieCompletionKindName.STATIC_MEMBER_INACCESSIBLE) {
                            staticMembersCompletion.value.completeFromIndices(indicesHelper(false), collector)
                        }
                    }
                }
            }
        }

        /**
         * 补全未导入的顶层符号和类
         *
         * 分两种情况：
         * 1. 顶层可调用符号（函数/属性）：从全局索引查找，选中后自动添加 import
         * 2. 类名补全：
         *    - DEFAULT/TYPE 位置：所有类
         *    - DOT 位置：特殊处理——将接收者解析为枚举类，补全其成员
         *      这样可以实现 `MyEnum.VALUE` 形式的补全，即使 MyEnum 未导入
         */
        private fun completeNonImported(lookupElementFactory: LookupElementFactory) {
            // 补全未导入的顶层函数和属性
            if (shouldCompleteTopLevelCallablesFromIndex()) {
                processTopLevelCallables {
                    collector.addDescriptorElements(it, lookupElementFactory, notImported = true)
                    flushToResultSet()
                }
            }

            if (callTypeAndReceiver.receiver == null && prefix.isNotEmpty()) {
                // 确定当前位置允许的类类型
                val classKindFilter: ((ClassKind) -> Boolean)? = when (callTypeAndReceiver) {
                    is CallTypeAndReceiver.DEFAULT, is CallTypeAndReceiver.TYPE -> { { true } }
                    else -> null
                }

                if (classKindFilter != null) {
                    // 使用更好的前缀匹配器（基于已有最佳匹配度动态调整）
                    val prefixMatcher = if (configuration.useBetterPrefixMatcherForNonImportedClasses)
                        BetterPrefixMatcher(prefixMatcher, collector.bestMatchingDegree)
                    else
                        prefixMatcher

                    addClassesFromIndex(
                        kindFilter = classKindFilter,
                        prefixMatcher = prefixMatcher,
                        completionParameters = parameters,
                        indicesHelper = indicesHelper(true),
                        classifierDescriptorCollector = {
                            collector.addElement(
                                basicLookupElementFactory.createLookupElement(it), notImported = true
                            )
                        },
                    )
                }

            } else if (callTypeAndReceiver is CallTypeAndReceiver.DOT) {
                // DOT 调用（foo.▌）：尝试将接收者解析为枚举类，补全枚举成员
                val qualifier = bindingContext[BindingContext.QUALIFIER, callTypeAndReceiver.receiver]
                if (qualifier != null) return  // 已经解析成功，不需要额外处理

                val receiver = callTypeAndReceiver.receiver as? CjSimpleNameExpression ?: return
                val descriptors = mutableListOf<ClassifierDescriptorWithTypeParameters>()

                // 使用精确匹配（接收者名称必须完全匹配）而非前缀匹配
                val fullTextPrefixMatcher = object : PrefixMatcher(receiver.referencedName) {
                    override fun prefixMatches(name: String): Boolean = name == prefix
                    override fun cloneWithPrefix(prefix: String): PrefixMatcher =
                        throw UnsupportedOperationException("Not implemented")
                }

                // 从索引中查找与接收者名称完全匹配的类
                addClassesFromIndex(
                    kindFilter = { true },
                    prefixMatcher = fullTextPrefixMatcher,
                    completionParameters = parameters.withPosition(receiver, receiver.startOffset),
                    indicesHelper = indicesHelper(false),
                    classifierDescriptorCollector = { descriptors += it },
                )

                val foundDescriptors = HashSet<DeclarationDescriptor>()

                // 只处理枚举类（仓颉语言中 DOT 访问常用于枚举成员）
                val classifiers = descriptors.asSequence().filter { it.kind == ClassKind.ENUM }

                for (classifier in classifiers) {
                    val scope = nameExpression?.getResolutionScope(bindingContext) ?: return

                    // 将枚举类描述符临时注入作用域，模拟"已导入"状态
                    val desc = classifier.getImportableDescriptor()
                    val newScope = scope.addImportingScope(ExplicitImportsScope(listOf(desc)))

                    // 在新作用域下重新分析表达式
                    val newContext = (nameExpression.parent as CjExpression).analyzeInContext(newScope)

                    // 创建临时的引用变体工具，在新上下文中收集成员
                    val rvHelper = ReferenceVariantsHelper(
                        newContext, resolutionFacade, moduleDescriptor,
                        isVisibleFilter, NotPropertiesService.getNotProperties(position)
                    )

                    val rvCollector = ReferenceVariantsCollector(
                        referenceVariantsHelper = rvHelper,
                        indicesHelper = indicesHelper(true),
                        prefixMatcher = prefixMatcher,
                        applicabilityFilter = applicabilityFilter,
                        nameExpression = nameExpression,
                        callTypeAndReceiver = callTypeAndReceiver,
                        resolutionFacade = resolutionFacade,
                        bindingContext = newContext,
                        importableFqNameClassifier = importableFqNameClassifier,
                        configuration = configuration,
                        allowExpectedDeclarations = allowExpectedDeclarations,
                    )

                    val receiverTypes = detectReceiverTypes(newContext, nameExpression, callTypeAndReceiver)

                    /**
                     * 创建特殊的 LookupElementFactory：
                     * 对枚举成员的 LookupElement 做额外处理——
                     * 选中时自动将接收者（如 MyEnum）绑定到正确的全限定名，
                     * 并在补全项尾部显示 "in com.example" 提示
                     */
                    val factory = lookupElementFactory.copy(
                        receiverTypes = receiverTypes,
                        standardLookupElementsPostProcessor = { lookupElement ->
                            val lookupDescriptor = lookupElement.`object`
                                .safeAs<DescriptorBasedDeclarationLookupObject>()
                                ?.descriptor as? MemberDescriptor
                                ?: return@copy lookupElement

                            // 只处理属于此枚举类的成员（排除扩展函数）
                            if (!desc.isAncestorOf(lookupDescriptor, false)) return@copy lookupElement
                            if (lookupDescriptor is CallableMemberDescriptor && lookupDescriptor.isExtension)
                                return@copy lookupElement

                            val fqNameToImport =
                                lookupDescriptor.containingDeclaration.importableFqName ?: return@copy lookupElement

                            // 包装 LookupElement，插入时自动绑定接收者到全限定名
                            object : LookupElementDecorator<LookupElement>(lookupElement) {
                                val name = fqNameToImport.shortName()
                                val packageName = fqNameToImport.parent()

                                override fun handleInsert(context: InsertionContext) {
                                    super.handleInsert(context)
                                    context.commitDocument()
                                    val file = context.file as? CjFile
                                    if (file != null) {
                                        // 找到接收者节点，将其引用绑定到枚举类的全限定名
                                        val receiverInFile = file.findElementAt(receiver.startOffset)
                                            ?.getParentOfType<CjSimpleNameExpression>(false)
                                            ?: return
                                        receiverInFile.mainReference.bindToFqName(
                                            fqNameToImport,
                                            CjSimpleNameReference.ShorteningMode.FORCED_SHORTENING
                                        )
                                    }
                                }

                                override fun renderElement(presentation: LookupElementPresentation) {
                                    super.renderElement(presentation)
                                    // 在补全项右侧显示包名提示，如 "in com.example"
                                    presentation.appendTailText(
                                        CangJieCompletionBundle.message(
                                            "presentation.tail.for.0.in.1", name, packageName,
                                        ),
                                        true,
                                    )
                                }
                            }
                        },
                    )

                    // 在新上下文中收集枚举成员，去重后加入结果
                    rvCollector.collectReferenceVariants(descriptorKindFilter) { (imported, notImportedExtensions) ->
                        val unique = imported.asSequence()
                            .filterNot { it.original in foundDescriptors }
                            .onEach { foundDescriptors += it.original }

                        val uniqueNotImportedExtensions = notImportedExtensions.asSequence()
                            .filterNot { it.original in foundDescriptors }
                            .onEach { foundDescriptors += it.original }

                        collector.addDescriptorElements(unique.toList(), factory, prohibitDuplicates = true)
                        collector.addDescriptorElements(
                            uniqueNotImportedExtensions.toList(), factory,
                            notImported = true, prohibitDuplicates = true
                        )

                        flushToResultSet()
                    }
                }
            }
        }
    }

    /**
     * 从全局索引中按类类型过滤并收集类描述符。
     * 使用 AllClassesCompletion 封装索引查询逻辑，
     * 支持 typeAlias 和 shadowed 过滤（避免被局部同名声明遮蔽）。
     *
     * @param kindFilter 类类型过滤器（如只要枚举、只要接口等）
     * @param prefixMatcher 前缀匹配器
     * @param completionParameters 补全参数（影响查询范围）
     * @param indicesHelper 索引查询助手
     * @param classifierDescriptorCollector 收集到的分类器描述符的回调
     */
    private fun addClassesFromIndex(
        kindFilter: (ClassKind) -> Boolean,
        prefixMatcher: PrefixMatcher,
        completionParameters: CompletionParameters,
        indicesHelper: CangJieIndicesHelper,
        classifierDescriptorCollector: (ClassifierDescriptorWithTypeParameters) -> Unit,
    ) {
        AllClassesCompletion(
            parameters = completionParameters,
            cangjieIndicesHelper = indicesHelper,
            prefixMatcher = prefixMatcher,
            resolutionFacade = resolutionFacade,
            kindFilter = kindFilter,
            includeTypeAliases = true,
        ).collect { processWithShadowedFilter(it, classifierDescriptorCollector) }
    }

    /** 是否禁止自动弹出补全窗口（委托给当前补全类别判断） */
    fun shouldDisableAutoPopup(): Boolean = completionKind.shouldDisableAutoPopup()

    /**
     * 智能补全实例（懒加载）
     *
     * 仅在有表达式上下文时创建，负责：
     * - 计算期望类型（expectedInfos）
     * - 提供类型匹配的附加补全项
     * - forBasicCompletion = true 表示在基础补全中内嵌智能补全，
     *   而非独立的 Ctrl+Shift+Space 触发的纯智能补全
     */
    private val smartCompletion by lazy {
        expression?.let {
            SmartCompletion(
                expression = it,
                resolutionFacade = resolutionFacade,
                bindingContext = bindingContext,
                moduleDescriptor = moduleDescriptor,
                visibilityFilter = isVisibleFilter,
                applicabilityFilter = applicabilityFilter,
                indicesHelper = indicesHelper(false),
                prefixMatcher = prefixMatcher,
                inheritorSearchScope = GlobalSearchScope.EMPTY_SCOPE,
                toFromOriginalFileMapper = toFromOriginalFileMapper,
                callTypeAndReceiver = callTypeAndReceiver,
                forBasicCompletion = true,
            )
        }
    }

    /** 当前补全类别（懒加载，通过 detectCompletionCategory() 确定） */
    private val completionKind by lazy { detectCompletionCategory() }

    /** 当前类别的描述符类型过滤器（委托给 completionKind） */
    override val descriptorKindFilter: DescriptorKindFilter? get() = completionKind.descriptorKindFilter

    /** 期望类型信息（来自智能补全分析，无智能补全时返回空列表） */
    override val expectedInfos: Collection<ExpectedInfo> get() = smartCompletion?.expectedInfos ?: emptyList()

    /**
     * 单类型补全类别的抽象基类
     *
     * 简化实现：只需提供一个 CangJieCompletionKindName 和 fillResultSet() 实现，
     * generateCategories() 自动将其包装成 SuggestionGenerator 并提交。
     *
     * 使用场景：KEYWORDS_ONLY、NAMED_ARGUMENTS_ONLY、OPERATOR_NAME、SUPER_QUALIFIER
     */
    private abstract inner class OneKindCompletionCategory(private val name: CangJieCompletionKindName) :
        CompletionCategory {
        final override fun generateCategories() {
            // 将 fillResultSet 包装成 Generator 并提交，由框架决定何时执行
            suggestionGeneratorConsumer.pass(suggestionGeneratorForCompletionKind(name) {
                fillResultSet()
            })
        }

        /** 子类实现此方法，向 collector 添加具体的补全候选项 */
        abstract fun fillResultSet()
    }

    /**
     * 完成来自未解析引用或 override 的声明名称补全
     *
     * 两种情况：
     * 1. 带 override 修饰符 → 通过 OverridesCompletion 提供可重写的父类成员列表
     * 2. 普通声明 → 通过 FromUnresolvedNamesCompletion 从同作用域内的未解析引用
     *    推断可能的命名（如参数名与变量名的匹配）
     */
    private fun completeDeclarationNameFromUnresolvedOrOverride(declaration: CjNamedDeclaration) {
        addKind(CangJieCompletionKindName.DECLARATION_NAME_FROM_UNRESOLVED_OVERRIDE) {
            if (declaration is CjCallableDeclaration && declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
                // override 场景：列出所有可重写的父类成员
                OverridesCompletion(collector, basicLookupElementFactory).complete(position, declaration)
            } else {
                // 普通声明：从作用域内的未解析引用中推断名称建议
                val referenceScope = referenceScope(declaration) ?: return@addKind
                val originalScope = toFromOriginalFileMapper.toOriginalFile(referenceScope) ?: return@addKind
                val afterOffset = if (referenceScope is CjBlockExpression) parameters.offset else null
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                FromUnresolvedNamesCompletion(collector, prefixMatcher).addNameSuggestions(
                    originalScope, afterOffset, descriptor
                )
            }
        }
    }

    /**
     * 便捷方法：将一段补全逻辑包装成指定类型的 SuggestionGenerator 并提交。
     *
     * @param name 补全类型名称（用于 TurboComplete 框架的分类和调度）
     * @param generator 实际的补全逻辑（向 collector 添加候选项）
     */
    private fun addKind(name: CangJieCompletionKindName, generator: () -> Unit) {
        suggestionGeneratorConsumer.pass(suggestionGeneratorForCompletionKind(name) {
            generator()
        })
    }

    /**
     * 运算符名称补全类别
     *
     * 仅在以下条件全部满足时激活：
     * 1. 光标在 nameExpression 上（即函数名的标识符位置）
     * 2. 父节点是 operator fun 函数
     * 3. 光标就在函数名标识符上（而非函数体内）
     * 4. operator fun 不是顶层函数（或是顶层扩展函数）
     *
     * 提供如 plus、minus、invoke、get、set 等运算符函数名建议。
     */
    private val OPERATOR_NAME = object : OneKindCompletionCategory(CangJieCompletionKindName.OPERATOR_NAME) {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null

        fun isApplicable(): Boolean {
            if (nameExpression == null || nameExpression != expression) return false
            val func = position.getParentOfType<CjNamedFunction>(strict = false) ?: return false
            val funcNameIdentifier = func.nameIdentifier ?: return false
            val identifierInNameExpression = nameExpression.nextLeaf {
                it is LeafPsiElement && it.elementType == CjTokens.IDENTIFIER
            } ?: return false

            if (!func.hasModifier(CjTokens.OPERATOR_KEYWORD) || identifierInNameExpression != funcNameIdentifier) return false
            val originalFunc = toFromOriginalFileMapper.toOriginalFile(func) ?: return false
            return !originalFunc.isTopLevel || (originalFunc.isExtensionDeclaration())
        }

        override fun fillResultSet() {
            OperatorNameCompletion.doComplete(collector, descriptorNameFilter)
        }
    }

    /**
     * super 限定符补全类别
     *
     * 用于 super<▌> 语法中，提供当前类的所有父类/接口作为候选。
     * 描述符过滤器只允许分类器（CLASSIFIERS），不允许函数和属性。
     */
    private val SUPER_QUALIFIER = object : OneKindCompletionCategory(CangJieCompletionKindName.SUPER_QUALIFIER) {
        override val descriptorKindFilter: DescriptorKindFilter
            get() = DescriptorKindFilter.CLASSIFIERS

        override fun fillResultSet() {
            // 找到当前所在的类型声明（类/接口/对象）
            val classOrObject = position.parents.firstIsInstanceOrNull<CjTypeStatement>() ?: return
            val classDescriptor =
                resolutionFacade.resolveToDescriptor(classOrObject, BodyResolveMode.PARTIAL) as ClassDescriptor

            // 获取所有父类型（含隐式的 Any）
            val superClasses = classDescriptor.defaultType.constructor.supertypesWithAny()
                .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }

            superClasses
                .map {
                    basicLookupElementFactory.createLookupElement(
                        it,
                        qualifyNestedClasses = true,      // 嵌套类显示完整路径
                        includeClassTypeArguments = false  // 不显示类型参数
                    )
                }
                .forEach { collector.addElement(it) }
        }
    }

    /**
     * 声明名称补全类别
     *
     * 处理光标在声明名称标识符上的情况，如：
     *   val ▌         → 变量名建议
     *   fun ▌         → 函数名建议（来自未解析引用）
     *   override fun ▌ → 父类成员名列表
     *   class ▌       → 文件同名类名建议
     *
     * 特殊处理：参数名补全会同时带上类型建议（NameWithTypeCompletion）
     */
    private val DECLARATION_NAME = object : CompletionCategory {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null

        override fun generateCategories() {
            val declaration = declaration()

            // CjParameter 且不需要补全参数名时，直接跳过（连关键字也不补全）
            if (declaration is CjParameter && !NameWithTypeCompletion.shouldCompleteParameter(declaration)) {
                return
            }

            // 添加后处理器：声明名称补全项不响应字符触发选择（避免误触）
            addKind(CangJieCompletionKindName.DECLARATION_NAME) {
                collector.addLookupElementPostProcessor { lookupElement ->
                    lookupElement.apply { suppressItemSelectionByCharsOnTyping = true }
                }
            }

            // 声明名称位置也需要关键字（如修饰符关键字）
            KEYWORDS_ONLY.generateCategories()

            // 来自未解析引用或 override 的名称建议
            completeDeclarationNameFromUnresolvedOrOverride(declaration)

            when (declaration) {
                // 参数名：提供 "paramName: Type" 形式的完整建议
                is CjParameter -> completeParameterOrVarNameAndType(withType = true)

                // 类型声明：提供文件同名类名建议
                is CjTypeStatement -> {
                    addKind(CangJieCompletionKindName.TOP_LEVEL_CLASS_NAME) {
                        completeTopLevelClassName()
                    }
                }
            }
        }

        override fun shouldDisableAutoPopup(): Boolean = when {
            // 有激活的代码模板时不自动弹出（避免干扰模板展开）
            TemplateManager.getInstance(project).getActiveTemplate(parameters.editor) != null -> true
            // 参数位置且最近自动弹出刚被取消时不再弹出
            declaration() is CjParameter && wasAutopopupRecentlyCancelled(parameters) -> true
            else -> false
        }

        override fun addWeighers(sorter: CompletionSorter): CompletionSorter {
            val declaration = declaration()
            // 参数名补全：在 prefix 权重之前插入 VariableOrParameterNameWithTypeCompletion.Weigher
            // 使 "name: Type" 形式的建议排在前面
            return if (declaration is CjParameter && NameWithTypeCompletion.shouldCompleteParameter(declaration))
                sorter.weighBefore("prefix", VariableOrParameterNameWithTypeCompletion.Weigher)
            else
                sorter
        }

        /**
         * 补全文件顶层类名
         * 规则：文件名（不含扩展名）首字母大写且是合法标识符，
         * 且文件中还没有同名类声明时，提供文件名作为类名建议。
         */
        private fun completeTopLevelClassName() {
            val name = parameters.originalFile.virtualFile.nameWithoutExtension
            if (!(Name.isValidIdentifier(name) && Name.identifier(name).render() == name && name[0].isUpperCase()))
                return
            if ((parameters.originalFile as CjFile).declarations.any { it is CjTypeStatement && it.name == name })
                return

            collector.addElement(LookupElementBuilder.create(name))
        }

        /** 获取当前光标所在的具名声明节点 */
        private fun declaration() = position.parent as CjNamedDeclaration
    }

    /**
     * 参数名/变量名 + 类型的联合补全
     *
     * 提供 "name: Type" 形式的补全项，来源包括：
     * 1. 当前文件中的同名参数（复用已有命名习惯）
     * 2. 已导入类名（驼峰推断，如 UserService → userService: UserService）
     * 3. 全局索引中的所有类（未导入时也可推断）
     *
     * prefixEndsWithUppercaseLetterPattern 确保用户输入大写字母时重新触发补全。
     */
    private fun completeParameterOrVarNameAndType(withType: Boolean) {
        collector.restartCompletionOnPrefixChange(NameWithTypeCompletion.prefixEndsWithUppercaseLetterPattern)
        addKind(CangJieCompletionKindName.PARAMETER_OR_VAR_NAME_AND_TYPE) {
            val nameWithTypeCompletion = VariableOrParameterNameWithTypeCompletion(
                collector, basicLookupElementFactory, prefixMatcher, resolutionFacade, withType,
            )
            nameWithTypeCompletion.addFromParametersInFile(position, resolutionFacade, isVisibleFilterCheckAlways)
            nameWithTypeCompletion.addFromImportedClasses(position, bindingContext, isVisibleFilterCheckAlways)
            nameWithTypeCompletion.addFromAllClasses(parameters, indicesHelper(false))
        }
    }

    /** 判断自动弹出是否在最近被用户取消（避免频繁打扰） */
    private fun wasAutopopupRecentlyCancelled(parameters: CompletionParameters) =
        LookupCancelService.getInstance(project)
            .wasAutoPopupRecentlyCancelled(parameters.editor, position.startOffset)

    /**
     * 仅关键字补全类别
     *
     * 使用 KeywordCompletion 根据 PSI 上下文过滤可用关键字，
     * 同时通过 KeywordValues 处理特殊关键字的值语义（如 true/false/null）。
     *
     * 特殊关键字处理：
     * - "this"：展开为 this 及所有 this@label 形式
     * - "return"：展开为 return 及所有 return@label 形式
     * - "override"：触发 OverridesCompletion 列出可重写成员
     * - "class"：仅在非可调用引用位置显示
     */
    private val KEYWORDS_ONLY = object : OneKindCompletionCategory(CangJieCompletionKindName.KEYWORD_ONLY) {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null

        private val keywordCompletion = KeywordCompletion(object : KeywordCompletion.LanguageVersionSettingProvider {
            override fun getLanguageVersionSetting(element: PsiElement) = element.languageVersionSettings
            override fun getLanguageVersionSetting(module: Module) = module.languageVersionSettings
        })

        override fun fillResultSet() {
            // 记录已由 KeywordValues 处理的关键字，避免重复添加
            val keywordsToSkip = HashSet<String>()

            val keywordValueConsumer = object : KeywordValues.Consumer {
                override fun consume(
                    lookupString: String,
                    expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
                    suitableOnPsiLevel: PsiElement.() -> Boolean,
                    priority: SmartCompletionItemPriority,
                    factory: () -> LookupElement
                ) {
                    keywordsToSkip.add(lookupString)
                    val lookupElement = factory()

                    // 判断此关键字值是否与期望类型匹配（如 true/false 在 Boolean 上下文中）
                    val matched = expectedInfos.any {
                        val match = expectedInfoMatcher(it)
                        assert(!match.makeNotNullable) { "不支持可空的关键字值" }
                        match.isMatch()
                    }

                    // 期望类型信息可能因缺少导入或未声明变量而缺失，
                    // 此时退回到 PSI 层面的适用性检查
                    if (matched || (expectedInfos.isEmpty() && position.suitableOnPsiLevel())) {
                        lookupElement.putUserData(SmartCompletionInBasicWeigher.KEYWORD_VALUE_MATCHED_KEY, Unit)
                        lookupElement.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
                    }
                    collector.addElement(lookupElement)
                }
            }

            // 处理有特殊值语义的关键字（true、false、null 等）
            KeywordValues.process(
                keywordValueConsumer, position, callTypeAndReceiver,
                bindingContext, resolutionFacade, moduleDescriptor,
            )

            // 处理普通关键字（根据 PSI 上下文过滤可用的关键字集合）
            keywordCompletion.complete(expression ?: position, collector.resultSet.prefixMatcher) { lookupElement ->
                val keyword = lookupElement.lookupString

                // 已由 KeywordValues 处理的关键字跳过
                if (keyword in keywordsToSkip) return@complete

                // 检查是否有自定义关键字处理器（如 DefaultCompletionKeywordHandlerProvider 注册的处理器）
                val completionKeywordHandler =
                    DefaultCompletionKeywordHandlerProvider.getHandlerForKeyword(keyword)
                if (completionKeywordHandler != null) {
                    val lookups = completionKeywordHandler.createLookups(parameters, expression, lookupElement, project)
                    collector.addElements(lookups)
                    return@complete
                }

                when (keyword) {
                    "this" -> {
                        if (expression != null) {
                            // 展开为 this 及所有 this@label 形式（用于 lambda 或嵌套类中区分接收者）
                            collector.addElements(
                                thisExpressionItems(bindingContext, expression, prefix, resolutionFacade)
                                    .map { it.createLookupElement() }
                            )
                        } else {
                            // 次构造函数委托调用中（this(...)）只添加普通 this
                            collector.addElement(lookupElement)
                        }
                    }

                    "return" -> {
                        if (expression != null) {
                            // 展开为 return 及所有 return@label 形式（用于嵌套 lambda 中指定返回目标）
                            collector.addElements(returnExpressionItems(bindingContext, expression))
                        }
                    }

                    "override" -> {
                        collector.addElement(lookupElement)
                        // override 关键字后自动触发可重写成员列表
                        OverridesCompletion(collector, basicLookupElementFactory).complete(position, declaration = null)
                    }

                    "class" -> {
                        // 可调用引用位置（::class）由 KeywordValues 处理，此处跳过
                        if (callTypeAndReceiver !is CallTypeAndReceiver.CALLABLE_REFERENCE) {
                            collector.addElement(lookupElement)
                        }
                    }

                    else -> collector.addElement(lookupElement)
                }
            }
        }
    }

    /**
     * 执行补全的入口方法
     *
     * 由 IntelliJ 补全框架调用，流程：
     * 1. 若为自动弹出（用户停止输入触发），添加位置标记后处理器
     * 2. 若光标在函数字面量开始处，抑制字符触发选择
     * 3. 为所有补全项附加参数列表信息（argList）
     * 4. 调用 completionKind.generateCategories() 提交所有 Generator
     */
    override fun doComplete() {
        assert(parameters.completionType == CompletionType.BASIC)

        if (parameters.isAutoPopup) {
            // 记录自动弹出位置，用于判断是否需要取消（LookupCancelService）
            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(LookupCancelService.AUTO_POPUP_AT, position.startOffset)
                lookupElement
            }

            // 函数字面量开始处（{ 后面）不应因输入字符而立即选中补全项
            if (isAtFunctionLiteralStart(position)) {
                collector.addLookupElementPostProcessor { lookupElement ->
                    lookupElement.apply { suppressItemSelectionByCharsOnTyping = true }
                }
            }
        }

        // 为所有补全项附加参数列表节点（用于插入后的参数提示）
        collector.addLookupElementPostProcessor { lookupElement ->
            position.argList?.let { lookupElement.argList = it }
            lookupElement
        }

        // 根据光标位置确定的补全类别，提交所有 Generator
        completionKind.generateCategories()
    }

    /**
     * 核心工厂方法：将一段补全逻辑包装成 SuggestionGeneratorWithArtifact
     *
     * 设计要点：
     * - 每个 Generator 对应一种 CangJieCompletionKindName（补全子类型）
     * - Generator 不立即执行，由 SuggestionGeneratorExecutor 按调度策略执行
     * - generateVariantsAndArtifact() 内部先填充结果（fillResultSet），
     *   再调用 flushToResultSet() 将缓冲区内容写入 IDE 的 CompletionResultSet
     * - 返回值 T 作为"制品"（artifact），可被后续 Generator 通过 getArtifact() 获取
     *   （用于跨 Generator 的数据依赖，如智能补全需要引用收集的类型集合）
     *
     * @param name 补全类型名称
     * @param fillResultSet 实际的补全逻辑，返回制品 T
     */
    private fun <T> suggestionGeneratorForCompletionKind(
        name: CangJieCompletionKindName,
        fillResultSet: () -> T
    ) = object : SuggestionGeneratorWithArtifact<T>(
        CompletionKind(name, CangJieKindVariety), collector.resultSet, policyController, parameters
    ) {
        override fun generateVariantsAndArtifact(): T {
            val artifact = fillResultSet()
            flushToResultSet()  // 将 collector 中缓存的候选项写入 CompletionResultSet
            return artifact
        }
    }

    /**
     * 执行一段补全逻辑并收集其所需的上下文变量类型，返回带制品的 Generator。
     *
     * 使用场景：智能补全需要知道"当前上下文需要哪些函数类型的变量"，
     * 但这个信息只能在补全逻辑执行后才能知道。
     * 通过返回 SuggestionGeneratorWithArtifact<Set<FuzzyType>>，
     * 后续的 Generator 可以通过 getArtifact() 等待并获取这些类型信息。
     *
     * @param kindName 此补全逻辑对应的补全子类型名称
     * @param action 实际的补全逻辑（接受 LookupElementFactory）
     * @return 携带所需类型集合的 Generator
     */
    private fun withCollectRequiredContextVariableTypes(
        kindName: CangJieCompletionKindName,
        action: (LookupElementFactory) -> Unit
    ): SuggestionGeneratorWithArtifact<Set<FuzzyType>> {
        val provider = CollectRequiredTypesContextVariablesProvider()
        val lookupElementFactory = createLookupElementFactory(provider)

        val actionAsKind = suggestionGeneratorForCompletionKind(kindName) {
            action(lookupElementFactory)
            flushToResultSet()
            provider.requiredTypes  // 制品：此次补全所需的函数类型集合
        }

        suggestionGeneratorConsumer.pass(actionAsKind)
        return actionAsKind
    }
}

/**
 * 是否抑制通过字符输入触发的补全项选择。
 * 用于声明名称等位置，避免用户输入时意外选中补全项。
 */
var LookupElement.suppressItemSelectionByCharsOnTyping: Boolean by NotNullableUserDataProperty(
    Key("CANGJIE_SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING"),
    defaultValue = false,
)

/**
 * 通常以小写字母开头的描述符类型过滤器
 * 包含：可调用符号（函数/属性）、包、模块
 * 不包含扩展（扩展在单独的 Generator 中处理以加速）
 */
private val USUALLY_START_LOWER_CASE = DescriptorKindFilter(
    DescriptorKindFilter.CALLABLES_MASK or DescriptorKindFilter.PACKAGES_MASK or DescriptorKindFilter.MODULES_MASK,
    listOf()
)

/**
 * 通常以大写字母开头的描述符类型过滤器
 * 包含：分类器（类/接口/枚举）、函数（构造函数首字母大写）
 * 排除扩展函数（Extensions）以加速 getReferenceVariants 查询
 */
private val USUALLY_START_UPPER_CASE = DescriptorKindFilter(
    DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.FUNCTIONS_MASK,
    listOf(
        DescriptorKindExclude.Extensions  // 排除扩展，由专门的扩展 Generator 处理
    )
)