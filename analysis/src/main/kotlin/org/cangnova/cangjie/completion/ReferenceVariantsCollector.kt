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

package org.cangnova.cangjie.completion


import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.indices.CangJieIndicesHelper
import org.cangnova.cangjie.codeinsight.ReferenceVariantsHelper
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.scopes.DescriptorKindExclude
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isTypeVariableType
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.ShadowedDeclarationsFilter
import org.cangnova.cangjie.utils.decapitalizeSmartForCompiler
import org.cangnova.cangjie.utils.externalDescriptors
import com.intellij.codeInsight.completion.PrefixMatcher
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.contains
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isError
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import org.cangnova.cangjie.utils.ImportableFqNameClassifier

/**
 * 引用变体数据类
 *
 * 包含代码补全的两类候选项：
 * - [imported]: 已导入的声明（可直接使用）
 * - [notImportedExtensions]: 未导入的扩展（需要添加导入语句）
 *
 * @property imported 已导入的声明描述符集合
 * @property notImportedExtensions 未导入的可调用扩展描述符集合
 */
data class ReferenceVariants(
    val imported: Collection<DeclarationDescriptor>,
    val notImportedExtensions: Collection<CallableDescriptor>
)

/**
 * 合并两个引用变体
 */
private operator fun ReferenceVariants.plus(other: ReferenceVariants): ReferenceVariants {
    return ReferenceVariants(imported.union(other.imported), notImportedExtensions.union(other.notImportedExtensions))
}

/**
 * 引用变体收集器
 *
 * 负责收集代码补全时的所有候选项，包括：
 * - 基本变体：当前作用域内可见的声明
 * - 扩展变体：从索引中获取的顶层扩展
 *
 * ## 收集流程
 *
 * 1. **基本变体收集**: 通过 [ReferenceVariantsHelper] 收集当前作用域内的声明
 * 2. **扩展变体收集**: 通过 [CangJieIndicesHelper] 从索引中获取顶层扩展
 * 3. **过滤处理**: 应用遮蔽过滤、适用性过滤等
 * 4. **分类**: 区分已导入和未导入的声明
 *
 * ## 使用方式
 *
 * ```kotlin
 * val collector = ReferenceVariantsCollector(...)
 * collector.collectReferenceVariants(kindFilter) { variants ->
 *     // 处理收集到的变体
 * }
 * collector.collectingFinished()
 * val allVariants = collector.allCollected
 * ```
 *
 * @property referenceVariantsHelper 引用变体辅助类
 * @property indicesHelper 索引辅助类，用于从索引获取扩展
 * @property prefixMatcher 前缀匹配器
 * @property applicabilityFilter 适用性过滤器
 * @property nameExpression 名称表达式
 * @property callTypeAndReceiver 调用类型和接收者
 * @property resolutionFacade 解析门面
 * @property bindingContext 绑定上下文
 * @property importableFqNameClassifier 可导入名称分类器
 * @property configuration 补全会话配置
 * @property allowExpectedDeclarations 是否允许期望声明
 * @property runtimeReceiver 运行时接收者（可选）
 */
class ReferenceVariantsCollector(
    private val referenceVariantsHelper: ReferenceVariantsHelper,
    private val indicesHelper: CangJieIndicesHelper,
    private val prefixMatcher: PrefixMatcher,
    private val applicabilityFilter: (DeclarationDescriptor) -> Boolean,
    private val nameExpression: CjSimpleNameExpression,
    private val callTypeAndReceiver: CallTypeAndReceiver<*, *>,
    private val resolutionFacade: ResolutionFacade,
    private val bindingContext: BindingContext,
    private val importableFqNameClassifier: ImportableFqNameClassifier,
    private val configuration: CompletionSessionConfiguration,
    private val allowExpectedDeclarations: Boolean,
    private val runtimeReceiver: ExpressionReceiver? = null,
) {
    /** 标记收集是否已完成 */
    private var isCollectingFinished = false
    /** 已收集的已导入声明 */
    private val collectedImported = LinkedHashSet<DeclarationDescriptor>()

    /**
     * 引用变体收集器对
     *
     * 包含基本变体和扩展变体的懒加载收集器。
     *
     * @property basic 基本变体的懒加载收集器
     * @property extensions 扩展变体的懒加载收集器
     */
    data class ReferenceVariantsCollectors(
        val basic: Lazy<ReferenceVariants>,
        val extensions: Lazy<ReferenceVariants>
    )

    /**
     * 标记收集完成
     *
     * 必须在所有收集操作完成后调用，且只能调用一次。
     */
    fun collectingFinished() {
        assert(!isCollectingFinished){ "collectingFinished() should be called only once"  }
        isCollectingFinished = true
    }

    /**
     * 收集引用变体（带消费者回调）
     *
     * 分别收集基本变体和扩展变体，并通过消费者回调处理。
     *
     * @param descriptorKindFilter 描述符类型过滤器
     * @param consumer 变体消费者回调
     */
    fun collectReferenceVariants(descriptorKindFilter: DescriptorKindFilter, consumer: (ReferenceVariants) -> Unit) {
        assert(!isCollectingFinished)
        val config = configuration(descriptorKindFilter)

        val basic = collectBasicVariants(config).fixDescriptors()
        consumer(basic)
        val extensions = collectExtensionVariants(config, basic).fixDescriptors()
        consumer(extensions)
    }

    /**
     * 收集引用变体（直接返回）
     *
     * 收集基本变体和扩展变体，合并后返回。
     *
     * @param descriptorKindFilter 描述符类型过滤器
     * @return 合并后的引用变体
     */
    fun collectReferenceVariants(descriptorKindFilter: DescriptorKindFilter): ReferenceVariants {
        assert(!isCollectingFinished)
        val config = configuration(descriptorKindFilter)

        val basic = collectBasicVariants(config)
        return basic + collectExtensionVariants(config, basic)
    }

    /** 描述符名称过滤器 */
    private val descriptorNameFilter = prefixMatcher.asStringNameFilter()
    /** 已收集的未导入扩展 */
    private val collectedNotImportedExtensions = LinkedHashSet<CallableDescriptor>()
    /** 前缀字符串 */
    private val prefix = prefixMatcher.prefix

    /**
     * 获取所有已收集的变体
     *
     * 必须在 [collectingFinished] 调用后才能访问。
     */
    val allCollected: ReferenceVariants
        get() {
            assert(isCollectingFinished)
            return ReferenceVariants(collectedImported, collectedNotImportedExtensions)
        }

    /**
     * 收集基本变体
     *
     * 从当前作用域收集可见的声明，并添加到已收集集合中。
     *
     * @param filterConfiguration 过滤配置
     * @return 收集到的引用变体
     */
    private fun collectBasicVariants(filterConfiguration: FilterConfiguration): ReferenceVariants {
        val variants = doCollectBasicVariants(filterConfiguration)
        collectedImported += variants.imported
        return variants
    }

    /**
     * 执行基本变体收集
     *
     * 通过 [ReferenceVariantsHelper] 收集引用变体，并处理：
     * - 代码片段中的外部描述符
     * - 额外的属性名称过滤（如 get/set 前缀）
     * - 适用性过滤
     *
     * @param filterConfiguration 过滤配置
     * @return 收集到的引用变体
     */
    private fun doCollectBasicVariants(filterConfiguration: FilterConfiguration): ReferenceVariants {
        fun getReferenceVariants(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> {
            return referenceVariantsHelper.getReferenceVariants(
                nameExpression,
                kindFilter,
                nameFilter,

                filterOutShadowed = false,
                excludeNonInitializedVariable = false,
                useReceiverType = runtimeReceiver?.type
            )
        }

        val basicNameFilter = descriptorNameFilter.toNameFilter()
        val (descriptorKindFilter, additionalPropertyNameFilter) = filterConfiguration

        var runDistinct = false

        var basicVariants = getReferenceVariants(descriptorKindFilter, basicNameFilter)
        if (additionalPropertyNameFilter != null) {
            basicVariants += getReferenceVariants(
                descriptorKindFilter.intersect(DescriptorKindFilter.VARIABLES),
                additionalPropertyNameFilter.toNameFilter()
            )
            runDistinct = true
        }

        val containingCodeFragment = nameExpression.containingCjFile as? CjCodeFragment
        if (containingCodeFragment != null) {
            val externalDescriptors = containingCodeFragment.externalDescriptors
            if (externalDescriptors != null) {
                basicVariants += externalDescriptors
                    .filter { descriptorKindFilter.accepts(it) && basicNameFilter(it.name) }
            }
        }

        if (runDistinct) {
            basicVariants = basicVariants.distinct()
        }

        basicVariants = basicVariants.filter { applicabilityFilter(it) }

        return ReferenceVariants(filterConfiguration.filterVariants(basicVariants).toHashSet(), emptyList())
    }

    /**
     * 过滤变体
     *
     * 应用遮蔽声明过滤器。
     *
     * @param _variants 待过滤的变体集合
     * @return 过滤后的变体集合
     */
    private fun <TDescriptor : DeclarationDescriptor> FilterConfiguration.filterVariants(_variants: Collection<TDescriptor>): Collection<TDescriptor> {
        var variants = _variants

        if (shadowedDeclarationsFilter != null)
            variants = shadowedDeclarationsFilter.filter(variants)




        return variants
    }

    /**
     * 创建引用变体收集器对
     *
     * 创建懒加载的基本变体和扩展变体收集器。
     *
     * @param descriptorKindFilter 描述符类型过滤器
     * @return 收集器对
     */
    fun makeReferenceVariantsCollectors(descriptorKindFilter: DescriptorKindFilter): ReferenceVariantsCollectors {
        val config = configuration(descriptorKindFilter)

        val basic = lazy {
            assert(!isCollectingFinished)
            collectBasicVariants(config).fixDescriptors()
        }

        val extensions = lazy {
            assert(!isCollectingFinished)
            collectExtensionVariants(config, basic.value).fixDescriptors()
        }

        return ReferenceVariantsCollectors(basic, extensions)
    }

    /**
     * 执行扩展变体收集
     *
     * 从索引中获取顶层扩展，并区分已导入和未导入的扩展。
     *
     * @param filterConfiguration 过滤配置
     * @param basicVariants 基本变体（用于过滤已包含的声明）
     * @return 收集到的引用变体
     */
    private fun doCollectExtensionVariants(
        filterConfiguration: FilterConfiguration,
        basicVariants: ReferenceVariants
    ): ReferenceVariants {
        val (_, additionalPropertyNameFilter, shadowedDeclarationsFilter, completeExtensionsFromIndices) = filterConfiguration

        if (completeExtensionsFromIndices) {
            val nameFilter = if (additionalPropertyNameFilter != null)
                descriptorNameFilter or additionalPropertyNameFilter
            else
                descriptorNameFilter
            val extensions = if (runtimeReceiver != null)
                indicesHelper.getCallableTopLevelExtensions(
                    callTypeAndReceiver,
                    listOf(runtimeReceiver.type),
                    nameFilter
                )
            else
                indicesHelper.getCallableTopLevelExtensions(
                    callTypeAndReceiver, nameExpression, bindingContext, receiverTypeFromDiagnostic = null, nameFilter
                )

            val (extensionsVariants, notImportedExtensions) = extensions.filter { applicabilityFilter(it) }.partition {
                importableFqNameClassifier.isImportableDescriptorImported(
                    it
                )
            }

            val notImportedDeclarationsFilter =
                shadowedDeclarationsFilter?.createNonImportedDeclarationsFilter<CallableDescriptor>(
                    importedDeclarations = basicVariants.imported + extensionsVariants,
                    allowExpectedDeclarations = allowExpectedDeclarations,
                )

            val filteredImported = filterConfiguration.filterVariants(extensionsVariants + basicVariants.imported)

            val importedExtensionsVariants = filteredImported.filter { it !in basicVariants.imported }

            return ReferenceVariants(
                importedExtensionsVariants,
                notImportedExtensions.let { variants -> notImportedDeclarationsFilter?.invoke(variants) ?: variants }
            )
        }

        return ReferenceVariants(emptyList(), emptyList())
    }

    /**
     * 收集扩展变体
     *
     * 收集扩展变体并添加到已收集集合中。
     *
     * @param filterConfiguration 过滤配置
     * @param basicVariants 基本变体
     * @return 收集到的引用变体
     */
    private fun collectExtensionVariants(
        filterConfiguration: FilterConfiguration,
        basicVariants: ReferenceVariants
    ): ReferenceVariants {
        val variants = doCollectExtensionVariants(filterConfiguration, basicVariants)
        collectedImported += variants.imported
        collectedNotImportedExtensions += variants.notImportedExtensions
        return variants
    }

    /** get/set 前缀列表，用于属性名称匹配 */
    private val GET_SET_PREFIXES = listOf("get", "set", "ge", "se", "g", "s")

    /**
     * 顶层扩展排除器
     *
     * 用于从基本变体中排除顶层扩展，这些扩展会通过索引单独处理。
     */
    private object TopLevelExtensionsExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is CallableMemberDescriptor) return false
            if (descriptor.kind != CallableMemberDescriptor.Kind.DECLARATION) return false /* do not filter out synthetic extensions */
            if (descriptor.isArtificialImportAliasedDescriptor) return false // do not exclude aliased descriptors - they cannot be completed via indices
            val containingPackage = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
            // TODO: temporary solution for Android synthetic extensions
            return false
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    /**
     * 过滤配置
     *
     * 包含收集过程中使用的各种过滤器和配置选项。
     *
     * @property descriptorKindFilter 描述符类型过滤器
     * @property additionalPropertyNameFilter 额外的属性名称过滤器（用于 get/set 前缀匹配）
     * @property shadowedDeclarationsFilter 遮蔽声明过滤器
     * @property completeExtensionsFromIndices 是否从索引补全扩展
     */
    private data class FilterConfiguration(
        val descriptorKindFilter: DescriptorKindFilter,
        val additionalPropertyNameFilter: ((String) -> Boolean)?,
        val shadowedDeclarationsFilter: ShadowedDeclarationsFilter?,
        val completeExtensionsFromIndices: Boolean
    )

    /**
     * 创建过滤配置
     *
     * 根据描述符类型过滤器创建完整的过滤配置，包括：
     * - 是否从索引补全扩展
     * - 额外的属性名称过滤器（处理 get/set 前缀）
     * - 遮蔽声明过滤器
     *
     * @param descriptorKindFilter 描述符类型过滤器
     * @return 过滤配置
     */
    private fun configuration(descriptorKindFilter: DescriptorKindFilter): FilterConfiguration {
        val completeExtensionsFromIndices = descriptorKindFilter.kindMask.and(DescriptorKindFilter.CALLABLES_MASK) != 0
                && DescriptorKindExclude.Extensions !in descriptorKindFilter.excludes
                && callTypeAndReceiver !is CallTypeAndReceiver.IMPORT_DIRECTIVE

        @Suppress("NAME_SHADOWING")
        val descriptorKindFilter = if (completeExtensionsFromIndices)
            descriptorKindFilter exclude TopLevelExtensionsExclude // handled via indices
        else
            descriptorKindFilter

        val getOrSetPrefix = GET_SET_PREFIXES.firstOrNull { prefix.startsWith(it) }
        val additionalPropertyNameFilter: ((String) -> Boolean)? = getOrSetPrefix?.let {
            prefixMatcher.cloneWithPrefix(prefix.removePrefix(getOrSetPrefix).decapitalizeSmartForCompiler())
                .asStringNameFilter()
        }

        val shadowedDeclarationsFilter = if (runtimeReceiver != null)
            ShadowedDeclarationsFilter(bindingContext, resolutionFacade, nameExpression, runtimeReceiver)
        else
            ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, nameExpression, callTypeAndReceiver)

        return FilterConfiguration(
            descriptorKindFilter,
            additionalPropertyNameFilter,
            shadowedDeclarationsFilter,
            completeExtensionsFromIndices
        )
    }

    companion object {
        /**
         * 修复替换失败的描述符
         *
         * 如果描述符存在类型替换失败（如 Nothing 类型、类型变量、错误类型），
         * 则返回原始描述符。
         */
        @Suppress("UNCHECKED_CAST")
        private fun <T : DeclarationDescriptor> T.fixSubstitutionFailureIfAny(): T =
            if (hasSubstitutionFailure()) original as T else this

        /**
         * 检查描述符是否存在替换失败
         *
         * 检查可调用描述符的参数类型是否包含：
         * - Nothing 类型（原始类型中不存在）
         * - 类型变量类型
         * - 错误类型
         */
        private fun DeclarationDescriptor.hasSubstitutionFailure(): Boolean {
            val callable = this as? CallableDescriptor ?: return false
            return callable.valueParameters.any { descriptor ->
                descriptor.type.contains { it.isNothing() } && !descriptor.original.type.contains { it.isNothing() }
                        || descriptor.type.contains { it.isTypeVariableType() }
                        || descriptor.type.contains { it.isError() }
            }
        }

        /**
         * 修复引用变体中的描述符
         *
         * 对已导入和未导入的描述符都应用替换失败修复。
         */
        private fun ReferenceVariants.fixDescriptors(): ReferenceVariants {
            val importedFixed = imported.map { it.fixSubstitutionFailureIfAny() }
            val notImportedFixed = notImportedExtensions.map { it.fixSubstitutionFailureIfAny() }
            return ReferenceVariants(importedFixed, notImportedFixed)
        }
    }
}
