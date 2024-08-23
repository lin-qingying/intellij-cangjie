package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.*

class AnnotationChecker {
//    fun getDeclarationSiteActualTargetList(annotated: CjElement, descriptor: ClassDescriptor?, context: BindingContext):
//            List<CangJieTarget> {
//        return getActualTargetList(annotated, descriptor, context).defaultTargets
//    }

    fun check(annotated: CjAnnotated, trace: BindingTrace, descriptor: DeclarationDescriptor? = null) {
//        val actualTargets = getActualTargetList(annotated, descriptor, trace.bindingContext)
//        checkEntries(annotated.annotationEntries, actualTargets, trace, annotated)
//        if (annotated is CjProperty) {
//            checkPropertyUseSiteTargetAnnotations(annotated, trace)
//        }
//        if (annotated is CjClassOrObject) {
//            checkSuperTypeAnnotations(annotated, trace)
//        }
//        if (annotated is CjCallableDeclaration) {
//            annotated.typeReference?.let { check(it, trace) }
//            annotated.receiverTypeReference?.let { check(it, trace) }
//        }
//        if (annotated is CjTypeAlias) {
//            annotated.getTypeReference()?.let { check(it, trace) }
//        }
//        if (
//            annotated is CjTypeParameterListOwner &&
//            (annotated is CjCallableDeclaration || languageVersionSettings.supportsFeature(ProperCheckAnnotationsTargetInTypeUsePositions) ||
//                    (annotated is CjClass && languageVersionSettings.supportsFeature(ClassTypeParameterAnnotations)))
//        ) {
//            if (annotated is CjClass && languageVersionSettings.supportsFeature(ClassTypeParameterAnnotations)) {
//                (descriptor as? ClassDescriptor)?.declaredTypeParameters?.forEach {
//                    //force annotation resolve to obtain targets
//                    ForceResolveUtil.forceResolveAllContents(it.annotations)
//                }
//            }
//
//            annotated.typeParameters.forEach { check(it, trace) }
//            for (typeParameter in annotated.typeParameters) {
//                typeParameter.extendsBound?.let {
//                    checkTypeReference(
//                        it,
//                        trace,
//                        shouldCheckReferenceItself = true,
//                        checkWithoutLanguageFeature = annotated is CjCallableDeclaration
//                    )
//                }
//            }
//            for (typeConstraint in annotated.typeConstraints) {
//                typeConstraint.boundTypeReference?.let { checkTypeReference(it, trace, shouldCheckReferenceItself = true) }
//            }
//        }
//        if (annotated is CjTypeReference) {
//            if (languageVersionSettings.supportsFeature(ProperCheckAnnotationsTargetInTypeUsePositions)) {
//                checkTypeReference(annotated, trace)
//            } else {
//                annotated.typeElement?.typeArgumentsAsTypes?.filterNotNull()?.forEach { check(it, trace) }
//            }
//        }
//        if (annotated is CjDeclarationWithBody) {
//            // CjFunction or CjPropertyAccessor
//            for (parameter in annotated.valueParameters) {
//                if (!parameter.hasLetOrVar()) {
//                    check(parameter, trace)
//                    if (annotated is CjFunctionLiteral) {
//                        parameter.typeReference?.let { check(it, trace) }
//                    }
//                }
//            }
//        }
    }
    fun checkExpression(expression: CjExpression, trace: BindingTrace) {
//        checkEntries(
//            expression.getAnnotationEntries(),
//            getActualTargetList(expression, null, trace.bindingContext),
//            trace,
//            expression.parent as? CjAnnotated
//        )
//        if (expression is CjCallElement  ) {
//            val typeArguments = expression.typeArguments.mapNotNull { it.typeReference }
//            for (typeArgument in typeArguments) {
//                checkEntries(typeArgument.annotationEntries, getActualTargetList(typeArgument, null, trace.bindingContext), trace)
//            }
//        }
//        if (expression is CjLambdaExpression) {
//            for (parameter in expression.valueParameters) {
//                parameter.typeReference?.let { check(it, trace) }
//            }
//        }
    }
}
