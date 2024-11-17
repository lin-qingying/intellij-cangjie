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

package com.linqingying.cangjie.metadata

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.Modality
import com.linqingying.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.linqingying.cangjie.metadata.decompiler.COMPILED_DEFAULT_INITIALIZER
import com.linqingying.cangjie.metadata.decompiler.COMPILED_DEFAULT_PARAMETER_VALUE
import com.linqingying.cangjie.metadata.decompiler.DecompiledText
import com.linqingying.cangjie.metadata.decompiler.computeParameterName
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.psiUtil.quoteIfNeeded
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.renderer.DescriptorRendererModifier
import com.linqingying.cangjie.renderer.DescriptorRendererOptions
import com.linqingying.cangjie.renderer.render
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.DescriptorUtils.isEnumEntry
import com.linqingying.cangjie.resolve.secondaryConstructors
import com.linqingying.cangjie.types.isFlexible

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private const val DECOMPILED_COMMENT_FOR_PARAMETER = "/* = compiled code */"
private const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"
private const val DECOMPILED_CONTRACT_STUB = "contract { /* compiled contract */ }"
fun DescriptorRendererOptions.defaultDecompilerRendererOptions() {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
//    excludedTypeAnnotationClasses = emptySet()
    alwaysRenderModifiers = true
    parameterNamesInFunctionalTypes =
        false // to support parameters names in decompiled text we need to load annotation arguments
    defaultParameterValueRenderer = { _ -> COMPILED_DEFAULT_PARAMETER_VALUE }
    includePropertyConstant = true
    propertyConstantRenderer = { _ -> COMPILED_DEFAULT_INITIALIZER }
}

fun buildDecompiledText(
    packageFqName: FqName,
    descriptors: List<DeclarationDescriptor>,
    descriptorRenderer: DescriptorRenderer,
): DecompiledText {

    val builder = StringBuilder()

    fun appendDecompiledTextAndPackageName() {
        builder.append("// IntelliJ API Decompiler stub source generated from a class file\n" + "// Implementation of methods is not available")
        builder.append("\n\n")
        if (!packageFqName.isRoot) {
            builder.append("package ").append(packageFqName.render()).append("\n\n")
        }
    }

    fun appendDescriptor(descriptor: DeclarationDescriptor, indent: String, lastEnumEntry: Boolean? = null) {
        if (isEnumEntry(descriptor)) {
            for (annotation in descriptor.annotations) {
                builder.append(descriptorRenderer.renderAnnotation(annotation))
                builder.append(" ")
            }

            builder.append(descriptor.name.asString().quoteIfNeeded())
            (descriptor as? ClassDescriptor)?.let {
                if (descriptor.unsubstitutedPrimaryConstructor is EnumEntryConstructorDescriptor && (descriptor.unsubstitutedPrimaryConstructor as EnumEntryConstructorDescriptor).getConstructorTypes()
                        .isNotEmpty()
                ) {
                    builder.append("(")
                    builder.append(
                        (descriptor.unsubstitutedPrimaryConstructor as EnumEntryConstructorDescriptor).getConstructorTypes()
                            .joinToString(",") {
                                descriptorRenderer.renderType(it)

                            }
                    )
                    builder.append(")")
                }
            }

            builder.append(if (lastEnumEntry!!) ";" else "|")
        } else {
            builder.append(descriptorRenderer.render(descriptor).replace("= ...", DECOMPILED_COMMENT_FOR_PARAMETER))
        }

        if (descriptor is CallableDescriptor) {
            //NOTE: assuming that only return types can be flexible
            if (descriptor.returnType!!.isFlexible()) {
                builder.append(" ").append(FLEXIBLE_TYPE_COMMENT)
            }
        }

        if (descriptor is FunctionDescriptor || descriptor is PropertyDescriptor) {
            if ((descriptor as MemberDescriptor).modality != Modality.ABSTRACT) {
                if (descriptor is FunctionDescriptor) {
                    with(builder) {
                        append(" { ")
//                        if (descriptor.getUserData(ContractProviderKey)?.getContractDescription() != null) {
//                            append(DECOMPILED_CONTRACT_STUB).append("; ")
//                        }
                        append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                } else {
                    // descriptor instanceof PropertyDescriptor
                    builder.append(" ").append(DECOMPILED_CODE_COMMENT)
                }
            }
            if (descriptor is PropertyDescriptor) {
                builder.append("{")

                for (accessor in descriptor.accessors) {
                    if (accessor.isDefault) continue
                    builder.append("\n$indent    ")


                    if (accessor is PropertyGetterDescriptor) {
                        builder.append("get()")
                        builder.append(" {").append(DECOMPILED_CODE_COMMENT).append(" }")

                    } else if (accessor is PropertySetterDescriptor) {
                        builder.append("set(")
                        val parameterDescriptor = accessor.valueParameters[0]
                        for (annotation in parameterDescriptor.annotations) {
                            builder.append(descriptorRenderer.renderAnnotation(annotation))
                            builder.append(" ")
                        }
                        val parameterName = computeParameterName(parameterDescriptor.name)
                        builder.append(parameterName.asString()).append("<: ")
                            .append(descriptorRenderer.renderType(parameterDescriptor.type))
                        builder.append(")")
                        builder.append(" {").append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                }
                builder.append("}")
            }
        } else if (descriptor is ClassDescriptor && !isEnumEntry(descriptor)) {
            builder.append(" {\n")

            val subindent = "$indent    "

            var firstPassed = false
            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                } else {
                    firstPassed = true
                }
            }

            val allDescriptors =
                descriptor.secondaryConstructors + descriptor.defaultType.memberScope.getContributedDescriptors() +
                        if (descriptor.unsubstitutedPrimaryConstructor != null) {
                            listOf(descriptor.unsubstitutedPrimaryConstructor!!)
                        } else {
                            listOf()
                        }
            val (enumEntries, members) = allDescriptors.partition(::isEnumEntry)

            for ((index, enumEntry) in enumEntries.withIndex()) {
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(enumEntry, subindent, index == enumEntries.lastIndex)
            }



            for (member in members) {
                if (DescriptorUtils.isEnum(descriptor) && member is ConstructorDescriptor) {
                    continue
                }
                if (member.containingDeclaration != descriptor) {
                    continue
                }

                if (member is CallableMemberDescriptor && member.mustNotBeWrittenToDecompiledText()) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")
        }

        builder.append("\n")
    }

    appendDecompiledTextAndPackageName()
    for (member in descriptors) {
        appendDescriptor(member, "")
        builder.append("\n")
    }

    return DecompiledText(builder.toString())
}

/**
 * @see com.linqingying.cangjie.analysis.decompiler.stub.mustNotBeWrittenToStubs
 */
internal fun CallableMemberDescriptor.mustNotBeWrittenToDecompiledText(): Boolean {
    return when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION, CallableMemberDescriptor.Kind.DELEGATION -> false
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> true
        CallableMemberDescriptor.Kind.SYNTHESIZED -> syntheticMemberMustNotBeWrittenToDecompiledText()
    }
}

private fun CallableMemberDescriptor.syntheticMemberMustNotBeWrittenToDecompiledText(): Boolean {
    val containingClass = containingDeclaration as? ClassDescriptor ?: return false

    return when {


        else -> false
    }
}
