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

package com.linqingying.cangjie.ide.completion


import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.ide.CangJieIndicesHelper
import com.linqingying.cangjie.ide.codeinsight.ReferenceVariantsHelper
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjCodeFragment
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.ResolutionFacade
import com.linqingying.cangjie.resolve.scopes.DescriptorKindExclude
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.linqingying.cangjie.types.checker.SimpleClassicTypeSystemContext.isTypeVariableType
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.contains
import com.linqingying.cangjie.types.util.isNothing
import com.linqingying.cangjie.utils.CallTypeAndReceiver
import com.linqingying.cangjie.utils.ShadowedDeclarationsFilter
import com.linqingying.cangjie.utils.decapitalizeSmartForCompiler
import com.linqingying.cangjie.utils.externalDescriptors
import com.linqingying.cangjie.utils.fqname.ImportableFqNameClassifier
import com.intellij.codeInsight.completion.PrefixMatcher

data class ReferenceVariants(
    val imported: Collection<DeclarationDescriptor>,
    val notImportedExtensions: Collection<CallableDescriptor>
)
private operator fun ReferenceVariants.plus(other: ReferenceVariants): ReferenceVariants {
    return ReferenceVariants(imported.union(other.imported), notImportedExtensions.union(other.notImportedExtensions))
}
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
    private var isCollectingFinished = false
    private val collectedImported = LinkedHashSet<DeclarationDescriptor>()

    data class ReferenceVariantsCollectors(
        val basic: Lazy<ReferenceVariants>,
        val extensions: Lazy<ReferenceVariants>
    )

    fun collectingFinished() {
        assert(!isCollectingFinished){ "collectingFinished() should be called only once"  }
        isCollectingFinished = true
    }
    fun collectReferenceVariants(descriptorKindFilter: DescriptorKindFilter, consumer: (ReferenceVariants) -> Unit) {
        assert(!isCollectingFinished)
        val config = configuration(descriptorKindFilter)

        val basic = collectBasicVariants(config).fixDescriptors()
        consumer(basic)
        val extensions = collectExtensionVariants(config, basic).fixDescriptors()
        consumer(extensions)
    }
    fun collectReferenceVariants(descriptorKindFilter: DescriptorKindFilter): ReferenceVariants {
        assert(!isCollectingFinished)
        val config = configuration(descriptorKindFilter)

        val basic = collectBasicVariants(config)
        return basic + collectExtensionVariants(config, basic)
    }
    private val descriptorNameFilter = prefixMatcher.asStringNameFilter()
    private val collectedNotImportedExtensions = LinkedHashSet<CallableDescriptor>()
    private val prefix = prefixMatcher.prefix

    val allCollected: ReferenceVariants
        get() {
            assert(isCollectingFinished)
            return ReferenceVariants(collectedImported, collectedNotImportedExtensions)
        }

    private fun collectBasicVariants(filterConfiguration: FilterConfiguration): ReferenceVariants {
        val variants = doCollectBasicVariants(filterConfiguration)
        collectedImported += variants.imported
        return variants
    }

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

    private fun <TDescriptor : DeclarationDescriptor> FilterConfiguration.filterVariants(_variants: Collection<TDescriptor>): Collection<TDescriptor> {
        var variants = _variants

        if (shadowedDeclarationsFilter != null)
            variants = shadowedDeclarationsFilter.filter(variants)




        return variants
    }

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

    private fun collectExtensionVariants(
        filterConfiguration: FilterConfiguration,
        basicVariants: ReferenceVariants
    ): ReferenceVariants {
        val variants = doCollectExtensionVariants(filterConfiguration, basicVariants)
        collectedImported += variants.imported
        collectedNotImportedExtensions += variants.notImportedExtensions
        return variants
    }

    private val GET_SET_PREFIXES = listOf("get", "set", "ge", "se", "g", "s")

    private object TopLevelExtensionsExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is CallableMemberDescriptor) return false
            if (descriptor.extensionReceiverParameter == null) return false
            if (descriptor.kind != CallableMemberDescriptor.Kind.DECLARATION) return false /* do not filter out synthetic extensions */
            if (descriptor.isArtificialImportAliasedDescriptor) return false // do not exclude aliased descriptors - they cannot be completed via indices
            val containingPackage = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
            // TODO: temporary solution for Android synthetic extensions
            return !containingPackage.fqName.asString().startsWith("kotlinx.android.synthetic.")
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    private data class FilterConfiguration(
        val descriptorKindFilter: DescriptorKindFilter,
        val additionalPropertyNameFilter: ((String) -> Boolean)?,
        val shadowedDeclarationsFilter: ShadowedDeclarationsFilter?,
        val completeExtensionsFromIndices: Boolean
    )

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
        @Suppress("UNCHECKED_CAST")
        private fun <T : DeclarationDescriptor> T.fixSubstitutionFailureIfAny(): T =
            if (hasSubstitutionFailure()) original as T else this

        private fun DeclarationDescriptor.hasSubstitutionFailure(): Boolean {
            val callable = this as? CallableDescriptor ?: return false
            return callable.valueParameters.any { descriptor ->
                descriptor.type.contains { it.isNothing() } && !descriptor.original.type.contains { it.isNothing() }
                        || descriptor.type.contains { it.isTypeVariableType() }
                        || descriptor.type.contains { it.isError }
            }
        }

        private fun ReferenceVariants.fixDescriptors(): ReferenceVariants {
            val importedFixed = imported.map { it.fixSubstitutionFailureIfAny() }
            val notImportedFixed = notImportedExtensions.map { it.fixSubstitutionFailureIfAny() }
            return ReferenceVariants(importedFixed, notImportedFixed)
        }
    }
}
