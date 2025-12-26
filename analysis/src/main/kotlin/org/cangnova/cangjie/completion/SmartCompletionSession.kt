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

import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.completion.smart.SmartCompletion
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.resolve.sam.SamConstructorDescriptorKindExclude
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import org.cangnova.cangjie.types.isFunctionType

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
