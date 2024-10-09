package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.ide.completion.smart.SmartCompletion
import com.huawei.cangjie.ide.fuzzyType
import com.huawei.cangjie.resolve.sam.SamConstructorDescriptorKindExclude
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet

class SmartCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    resultSet: CompletionResultSet
) : CompletionSession(configuration, parameters, resultSet) {


    private val smartCompletion by lazy(LazyThreadSafetyMode.NONE) {
        expression?.let {
            SmartCompletion(
                it, resolutionFacade, bindingContext, moduleDescriptor, isVisibleFilter, applicabilityFilter,
                indicesHelper(false), prefixMatcher, searchScope, toFromOriginalFileMapper,
                callTypeAndReceiver
            )
        }
    }
    override val descriptorKindFilter: DescriptorKindFilter by lazy {
        // we do not include SAM-constructors because they are handled separately and adding them requires iterating of java classes
        val filter = DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude

        val referenceToConstructorIsApplicable = smartCompletion?.expectedInfos.orEmpty().any {
            it.fuzzyType?.type?.isFunctionType == true
        }

        if (referenceToConstructorIsApplicable) {
            filter.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        } else {
            filter
        }
    }
    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    override fun doComplete() {

    }

}
