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

package com.linqingying.cangjie.ide.overrideImplement

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.quickfix.overrideImplement.BodyType
import com.linqingying.cangjie.ide.quickfix.overrideImplement.GenerateMembersHandler
import com.linqingying.cangjie.ide.quickfix.overrideImplement.OverrideMemberChooserObject
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.resolve.fqNameSafe
import com.intellij.openapi.project.Project
import com.intellij.util.SmartList


class OverrideMembersHandler(private val preferConstructorParameters: Boolean = false) : GenerateMembersHandler(false) {
    override fun collectMembersToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject> {
        val result = ArrayList<OverrideMemberChooserObject>()
        for (member in descriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
            if (member is CallableMemberDescriptor && (member.kind != CallableMemberDescriptor.Kind.DECLARATION)) {
                val overridden = member.overriddenDescriptors
                if (overridden.any { it.modality == Modality.FINAL || !it.isVisibleFrom(descriptor) }) continue

                if (DescriptorUtils.isInterface(descriptor) && overridden.any { descriptor.builtIns.isMemberOfAny(it) }) continue


                class Data(
                    val realSuper: CallableMemberDescriptor,
                    val immediateSupers: MutableList<CallableMemberDescriptor> = SmartList()
                )

                val byOriginalRealSupers = LinkedHashMap<CallableMemberDescriptor, Data>()
                for (immediateSuper in overridden) {
                    for (realSuper in toRealSupers(immediateSuper)) {
                        byOriginalRealSupers.getOrPut(realSuper.original) { Data(realSuper) }.immediateSupers.add(immediateSuper)
                    }
                }

                var realSupers = byOriginalRealSupers.values.map(Data::realSuper)
//                if (realSupers.size > 1) {
//                    realSupers = realSupers.filter { it.fqNameSafe !in METHODS_OF_ANY }
//                }

                val nonAbstractRealSupers = realSupers.filter { it.modality != Modality.ABSTRACT }
                val realSupersToUse = if (nonAbstractRealSupers.isNotEmpty()) {
                    nonAbstractRealSupers
                } else {
                    listOf(realSupers.firstOrNull() ?: continue)
                }

                for (realSuper in realSupersToUse) {
                    val immediateSupers = byOriginalRealSupers[realSuper.original]!!.immediateSupers
                    assert(immediateSupers.isNotEmpty())

                    val immediateSuperToUse = if (immediateSupers.size == 1) {
                        immediateSupers.single()
                    } else {
                        immediateSupers.singleOrNull { (it.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.CLASS }
                            ?: immediateSupers.first()
                    }

                    val bodyType = when {
                        descriptor.kind == ClassKind.INTERFACE && realSuper.builtIns.isMemberOfAny(realSuper) ->
                            BodyType.NoBody
                        immediateSuperToUse.modality == Modality.ABSTRACT ->
                            BodyType.FromTemplate
                        realSupersToUse.size == 1 ->
                            BodyType.Super
                        else ->
                            BodyType.QualifiedSuper
                    }

                    result.add(
                        OverrideMemberChooserObject.create(
                            project,
                            realSuper,
                            immediateSuperToUse,
                            bodyType,
                            preferConstructorParameters
                        )
                    )
                }
            }
        }
        return result
    }

    private fun CallableMemberDescriptor.isVisibleFrom(classDescriptor: ClassDescriptor): Boolean {

        return !DescriptorVisibilities.isPrivate(visibility.normalize())
    }

    private fun toRealSupers(immediateSuper: CallableMemberDescriptor): Collection<CallableMemberDescriptor> {
        if (immediateSuper.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return listOf(immediateSuper)
        }
        val overridden = immediateSuper.overriddenDescriptors
        assert(overridden.isNotEmpty())
        return overridden.flatMap { toRealSupers(it) }.distinctBy { it.original }
    }

    override fun getChooserTitle() =CangJieBundle.message("override.members.handler.title")

    override fun getNoMembersFoundHint() = CangJieBundle.message("override.members.handler.no.members.hint")
}
