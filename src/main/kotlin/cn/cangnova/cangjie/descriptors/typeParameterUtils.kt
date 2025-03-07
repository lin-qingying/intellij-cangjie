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

package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.psi.psiUtil.firstIsInstanceOrNull
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.descriptorUtil.parents
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils
import cn.cangnova.cangjie.types.TypeProjection

class PossiblyInnerType(
    val classifierDescriptor: ClassifierDescriptorWithTypeParameters,
    val arguments: List<TypeProjection>,
    val outerType: PossiblyInnerType?
) {
    val classDescriptor: ClassDescriptor
        get() = classifierDescriptor as ClassDescriptor

    fun segments(): List<PossiblyInnerType> = outerType?.segments().orEmpty() + this
}

fun CangJieType.buildPossiblyInnerType(): PossiblyInnerType? {
    return buildPossiblyInnerType(constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters, 0)
}

fun ClassifierDescriptorWithTypeParameters.computeConstructorTypeParameters(): List<TypeParameterDescriptor> {
    val declaredParameters = declaredTypeParameters

//    if (!isInner && containingDeclaration !is CallableDescriptor) return declaredParameters

    val parametersFromContainingFunctions =
        parents.takeWhile { it is CallableDescriptor }
            .filter { it !is ConstructorDescriptor }
            .flatMap { (it as CallableDescriptor).typeParameters.asSequence() }
            .toList()

    val containingClassTypeConstructorParameters =
        parents.firstIsInstanceOrNull<ClassDescriptor>()?.typeConstructor?.parameters.orEmpty()
    if (parametersFromContainingFunctions.isEmpty() && containingClassTypeConstructorParameters.isEmpty()) return declaredTypeParameters

    val additional =
        (parametersFromContainingFunctions + containingClassTypeConstructorParameters)
            .map { it.capturedCopyForInnerDeclaration(this, declaredParameters.size) }

    return declaredParameters + additional
}

private fun TypeParameterDescriptor.capturedCopyForInnerDeclaration(
    declarationDescriptor: DeclarationDescriptor,
    declaredTypeParametersCount: Int
) = CapturedTypeParameterDescriptor(this, declarationDescriptor, declaredTypeParametersCount)

private class CapturedTypeParameterDescriptor(
    private val originalDescriptor: TypeParameterDescriptor,
    private val declarationDescriptor: DeclarationDescriptor,
    private val declaredTypeParametersCount: Int
) : TypeParameterDescriptor by originalDescriptor {
    override fun isCapturedFromOuterDeclaration() = true

    override val original: TypeParameterDescriptor
        get() = originalDescriptor.original
    override val containingDeclaration: DeclarationDescriptor
        get() = declarationDescriptor

    override fun getIndex() = declaredTypeParametersCount + originalDescriptor.index
    override fun toString() = "$originalDescriptor[inner-copy]"
}

private fun CangJieType.buildPossiblyInnerType(
    classifierDescriptor: ClassifierDescriptorWithTypeParameters?,
    index: Int
): PossiblyInnerType? {
    if (classifierDescriptor == null || ErrorUtils.isError(classifierDescriptor)) return null

    val toIndex = classifierDescriptor.declaredTypeParameters.size + index
    if (!classifierDescriptor.isInner) {
    assert(toIndex == arguments.size || DescriptorUtils.isLocal(classifierDescriptor)) {
        "${arguments.size - toIndex} trailing arguments were found in $this type"
    }

    return PossiblyInnerType(classifierDescriptor, arguments.subList(index, arguments.size), null)
    }

    val argumentsSubList = arguments.subList(index, toIndex)
    return PossiblyInnerType(
        classifierDescriptor, argumentsSubList,
        buildPossiblyInnerType(
            classifierDescriptor.containingDeclaration as? ClassifierDescriptorWithTypeParameters,
            toIndex
        )
    )
}
