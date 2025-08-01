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

package cn.cangnova.cangjie.resolve.scopes

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.resolve.selectMostSpecificInEachOverridableGroup
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.Printer


class TypeIntersectionScope private constructor(private val debugName: String, override val workerScope: MemberScope) : AbstractScopeAdapter() {
    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        super.getContributedFunctions(name, location).selectMostSpecificInEachOverridableGroup { this }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        super.getContributedVariables(name, location).selectMostSpecificInEachOverridableGroup { this }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val (callables, other) = super.getContributedDescriptors(kindFilter, nameFilter).partition { it is CallableDescriptor }

        @Suppress("UNCHECKED_CAST")
        return (callables as Collection<CallableDescriptor>).selectMostSpecificInEachOverridableGroup { this } + other
    }

    override fun printScopeStructure(p: Printer) {
        p.print("TypeIntersectionScope for: " + debugName)
        super.printScopeStructure(p)
    }

    companion object {
        @JvmStatic
        fun create(message: String, types: Collection<CangJieType>): MemberScope {
            val nonEmptyScopes = listOfNonEmptyScopes(types.map { it.memberScope })
            val chainedOrSingle = ChainedMemberScope.createOrSingle(message, nonEmptyScopes)

            if (nonEmptyScopes.size <= 1) return chainedOrSingle

            return TypeIntersectionScope(message, chainedOrSingle)
        }
    }
}
