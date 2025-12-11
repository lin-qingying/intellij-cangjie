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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.Substitutable
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.sure
import org.cangnova.cangjie.resolve.calls.inference.wrapWithCapturingSubstitution
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.safeSubstitute
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.newLinkedHashSetWithExpectedSize

class SubstitutingScope(private val workerScope: MemberScope, givenSubstitutor: TypeSubstitutor) : MemberScope {
    val substitutor by lazy { givenSubstitutor.substitution.buildSubstitutor() }

    private val capturingSubstitutor = givenSubstitutor.substitution.wrapWithCapturingSubstitution().buildSubstitutor()
    private var substitutedDescriptors: MutableMap<DeclarationDescriptor, DeclarationDescriptor>? = null

    private val _allDescriptors by lazy { substitute(workerScope.getContributedDescriptors()) }
    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()
    override fun getPropertyNames() = workerScope.getPropertyNames()
    fun substitute(type: CangJieType): CangJieType {
        if (capturingSubstitutor.isEmpty) return type
        return capturingSubstitutor.safeSubstitute(type) as CangJieType
    }

    private fun <D : DeclarationDescriptor> substitute(descriptor: D): D {
        if (capturingSubstitutor.isEmpty) return descriptor

        if (substitutedDescriptors == null) {
            substitutedDescriptors = HashMap<DeclarationDescriptor, DeclarationDescriptor>()
        }

        val substituted = substitutedDescriptors!!.getOrPut(descriptor) {
            when (descriptor) {
                is Substitutable<*> -> descriptor.substitute(capturingSubstitutor).sure {
                    "We expect that no conflict should happen while substitution is guaranteed to generate invariant projection, " +
                            "but $descriptor substitution fails"
                }

                else -> error("Unknown descriptor in scope: $descriptor")
            }
        }

        @Suppress("UNCHECKED_CAST")
        return substituted as D
    }

    private fun <D : DeclarationDescriptor> substitute(descriptors: Collection<D>): Collection<D> {
        if (capturingSubstitutor.isEmpty) return descriptors
        if (descriptors.isEmpty()) return descriptors

        val result = newLinkedHashSetWithExpectedSize<D>(descriptors.size)
        for (descriptor in descriptors) {
            val substitute = substitute(descriptor)
            result.add(substitute)
        }

        return result
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        substitute(workerScope.getContributedVariables(name, location))

    override fun getContributedPropertys(name: Name, location: LookupLocation) =
        substitute(workerScope.getContributedPropertys(name, location))

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        workerScope.getContributedClassifiers(name, location).map { substitute(it) }

    override fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        workerScope.getContributedEnumEntrys(name, location).map { substitute(it) }

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        workerScope.getContributedClassifier(name, location)?.let { substitute(it) }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return substitute(workerScope.getExtendClass(name)).toList()
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
        substitute(workerScope.getContributedMacros(name, location))

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        substitute(workerScope.getContributedFunctions(name, location))


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return _allDescriptors.filter {
            nameFilter.invoke(it.name)
        }
    }

//    override fun getFunctionNames() = workerScope.getFunctionNames()
//    override fun getVariableNames() = workerScope.getVariableNames()
//    override fun getClassifierNames() = workerScope.getClassifierNames()

    override fun definitelyDoesNotContainName(name: Name) = workerScope.definitelyDoesNotContainName(name)

    //
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("substitutor = ")
        p.pushIndent()
        p.println(capturingSubstitutor)
        p.popIndent()

        p.print("workerScope = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
