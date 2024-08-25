package com.huawei.cangjie.descriptors

import com.huawei.cangjie.psi.psiUtil.firstIsInstanceOrNull
import com.huawei.cangjie.resolve.descriptorUtil.parents


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
