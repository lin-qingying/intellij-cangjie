package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.ide.CangJieIndicesHelper
import com.linqingying.cangjie.resolve.ResolutionFacade
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.resolve.scopes.getDescriptorsFiltered
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher


class AllClassesCompletion(
    private val parameters: CompletionParameters,
    private val cangjieIndicesHelper: CangJieIndicesHelper,
    private val prefixMatcher: PrefixMatcher,
    private val resolutionFacade: ResolutionFacade,
    private val kindFilter: (ClassKind) -> Boolean,
    private val includeTypeAliases: Boolean,

    ) {
    fun collect(classifierDescriptorCollector: (ClassifierDescriptorWithTypeParameters) -> Unit ) {

        //TODO: this is a temporary solution until we have built-ins in indices
        // we need only nested classes because top-level built-ins are all added through default imports
        for (builtInPackage in resolutionFacade.moduleDescriptor.builtIns.builtInPackagesImportedByDefault.invoke()) {
            collectClassesFromScope(builtInPackage.memberScope) {
                if (it.containingDeclaration is ClassDescriptor) {
                    classifierDescriptorCollector(it)
                }
            }
        }

        cangjieIndicesHelper.processCangJieClasses(
            { prefixMatcher.prefixMatches(it) },
            kindFilter = kindFilter,
            processor = classifierDescriptorCollector
        )

        if (includeTypeAliases) {
            cangjieIndicesHelper.processTopLevelTypeAliases(prefixMatcher.asStringNameFilter(), classifierDescriptorCollector)
        }


    }

    private fun collectClassesFromScope(scope: MemberScope, collector: (ClassDescriptor) -> Unit) {
        for (descriptor in scope.getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)) {
            if (descriptor is ClassDescriptor) {
                if (kindFilter(descriptor.kind) && prefixMatcher.prefixMatches(descriptor.name.asString())) {
                    collector(descriptor)
                }

                collectClassesFromScope(descriptor.unsubstitutedInnerClassesScope, collector)
            }
        }
    }




}
