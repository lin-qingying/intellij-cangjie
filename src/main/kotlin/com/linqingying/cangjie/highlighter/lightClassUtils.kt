package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement


// Returns original declaration if given PsiElement is a CangJie light element, and element itself otherwise
val PsiElement.unwrapped: PsiElement?
    get() = when (this) {
//        is PsiElementWithOrigin<*> -> origin
        is CjLightElement<*, *> -> cangjieOrigin
//        is CjLightElementBase -> cangjieOrigin
        else -> this
    }
val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getNonStrictParentOfType()
//
//fun CjElement.toLightElements(): List<PsiNamedElement> = when (this) {
//    is CjTypeStatement -> listOfNotNull(toLightClass())
//    is CjNamedFunction,
//    is CjConstructor<*> -> LightClassUtil.getLightClassMethods(this as CjFunction)
//    is CjProperty -> LightClassUtil.getLightClassPropertyMethods(this).allDeclarations
//    is CjPropertyAccessor -> listOfNotNull(LightClassUtil.getLightClassAccessorMethod(this))
//    is CjParameter -> mutableListOf<PsiNamedElement>().also { elements ->
//        toPsiParameters().toCollection(elements)
//        LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)
//        toAnnotationLightMethod()?.let(elements::add)
//    }
//
//    is CjTypeParameter -> toPsiTypeParameters()
//    is CjFile -> listOfNotNull(findFacadeClass())
//    else -> listOf()
//}
//
//fun CjElement.toLightElements(): List<PsiNamedElement> = when (this) {
//    is CjClassOrObject -> listOfNotNull(toLightClass())
//    is CjNamedFunction,
//    is CjConstructor<*> -> LightClassUtil.getLightClassMethods(this as CjFunction)
//    is CjProperty -> LightClassUtil.getLightClassPropertyMethods(this).allDeclarations
//    is CjPropertyAccessor -> listOfNotNull(LightClassUtil.getLightClassAccessorMethod(this))
//    is CjParameter -> mutableListOf<PsiNamedElement>().also { elements ->
//        toPsiParameters().toCollection(elements)
//        LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)
//        toAnnotationLightMethod()?.let(elements::add)
//    }
//
//    is CjTypeParameter -> toPsiTypeParameters()
//    is CjFile -> listOfNotNull(findFacadeClass())
//    else -> listOf()
//}
