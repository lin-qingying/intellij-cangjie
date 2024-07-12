//package com.huawei.cangjie.resolve
//
//import com.huawei.cangjie.descriptors.*
//import com.huawei.cangjie.descriptors.annotations.AnnotationSplitter
//import com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget
//import com.huawei.cangjie.descriptors.annotations.Annotations
//import com.huawei.cangjie.descriptors.annotations.CompositeAnnotations
//import com.huawei.cangjie.descriptors.impl.TypeParameterDescriptorImpl
//import com.huawei.cangjie.descriptors.impl.ValueParameterDescriptorImpl
//import com.huawei.cangjie.lexer.CjTokens
//import com.huawei.cangjie.name.Name
//import com.huawei.cangjie.name.SpecialNames.anonymousParameterName
//import com.huawei.cangjie.psi.*
//import com.huawei.cangjie.resolve.calls.components.InferenceSession
//import com.huawei.cangjie.resolve.calls.util.isSingleUnderscore
//import com.huawei.cangjie.resolve.scopes.LexicalScope
//import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
//import com.huawei.cangjie.resolve.source.toSourceElement
//import com.huawei.cangjie.storage.StorageManager
//import com.huawei.cangjie.types.CangJieType
//import com.huawei.cangjie.types.Variance
//
//class DescriptorResolver(
//    val storageManager: StorageManager,
//    val annotationResolver: AnnotationResolver,
//    val typeResolver: TypeResolver,
//    val supertypeLoopsResolver: SupertypeLoopChecker
//) {
//    private fun resolveValueParameterAnnotations(
//        scope: LexicalScope,
//        parameter: CjParameter,
//        trace: BindingTrace,
//        additionalAnnotations: Annotations
//    ): Annotations {
//        val modifierList = parameter.getModifierList()
//            ?: return additionalAnnotations
//
//        val allAnnotations =
//            annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace)
//        if (!parameter.hasValOrVar()) {
//            return CompositeAnnotations(
//                allAnnotations,
//                additionalAnnotations
//            )
//        }
//
//        val splitter =
//            AnnotationSplitter(
//                storageManager,
//                allAnnotations,
//                setOf(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER)
//            )
//        return CompositeAnnotations(
//            splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER),
//            additionalAnnotations
//        )
//    }
//
//    fun resolveValueParameterDescriptor(
//        scope: LexicalScope,
//        owner: FunctionDescriptor,
//        valueParameter: CjParameter,
//        index: Int,
//        type: CangJieType,
//        trace: BindingTrace,
//        additionalAnnotations: Annotations,
//        inferenceSession: InferenceSession?
//    ): ValueParameterDescriptorImpl {
////        var varargElementType: CangJieType? = null
////
////        if (valueParameter.hasModifier(CjTokens.VARARG_KEYWORD)) {
////            varargElementType = type
////            variableType = getVarargParameterType(type)
////        }
//        val variableType = type
//
//        val destructuringDeclaration =
//            valueParameter.destructuringDeclaration
//
//
//
////        if (valueParameter.hasModifier(CjTokens.VARARG_KEYWORD)) {
////            varargElementType = type
////            variableType = getVarargParameterType(type)
////        }
//        val valueParameterAnnotations =
//            resolveValueParameterAnnotations(scope, valueParameter, trace, additionalAnnotations)
//        val parameterName: Name = if (destructuringDeclaration == null) {
//            // NB: val/var for parameter is only allowed in primary constructors where single underscore names are still prohibited.
//            // The problem with val/var is that when lazy resolve try to find their descriptor, it searches through the member scope
//            // of containing class where, it can not find a descriptor with special name.
//            // Thus, to preserve behavior, we don't use a special name for val/var.
//            if (!valueParameter.hasValOrVar() && valueParameter.isSingleUnderscore
//            ) anonymousParameterName(index)
//            else CjPsiUtil.safeName(valueParameter.name)
//        } else {
//            Name.special("<name for destructuring parameter $index>")
//        }
//        val destructuringVariables: Function0<List<VariableDescriptor>>?
//        if (destructuringDeclaration != null) {
////
//
//            destructuringVariables = {
//
//
//                listOf()
////                val scopeForDestructuring = ScopeUtils.createScopeForDestructuring(scope, owner.extensionReceiverParameter)
////
////                val result: List<VariableDescriptor> =
////                    destructuringDeclarationResolver.resolveLocalVariablesFromDestructuringDeclaration(
////                        scope,
////                        destructuringDeclaration,
////                        TransientReceiver(type),  /* initializer = */
////                        null,
////                        expressions.ExpressionTypingContext.newContext(
////                            trace, scopeForDestructuring, EMPTY, TypeUtils.NO_EXPECTED_TYPE,
////                            languageVersionSettings, dataFlowValueFactory, inferenceSession
////                        )
////                    )
////
////                modifiersChecker.withTrace(trace).checkModifiersForDestructuringDeclaration(destructuringDeclaration)
////                result
//            }
//        } else {
//            destructuringVariables = null
//        }
//
//
//        val valueParameterDescriptor =
//            ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
//                owner,
//                null,
//                index,
//                valueParameterAnnotations,
//                parameterName,
//                variableType,
//                valueParameter.hasDefaultValue(),
////                valueParameter.hasModifier( CjTokens.CROSSINLINE_KEYWORD),
////                valueParameter.hasModifier( CjTokens.NOINLINE_KEYWORD),
////                varargElementType,
//                valueParameter.toSourceElement(),
//                destructuringVariables
//            )
//        return valueParameterDescriptor
//
//    }
//
//    fun resolveTypeParametersForDescriptor(
//        containingDescriptor: DeclarationDescriptor,
//        extensibleScope: LexicalWritableScope,
//        scopeForAnnotationsResolve: LexicalScope,
//        typeParameters: List<CjTypeParameter>,
//        trace: BindingTrace
//    ): List<TypeParameterDescriptorImpl> {
//
////        TODO()
//        val descriptors =
//            resolveTypeParametersForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameters, trace)
//        for (descriptor in descriptors) {
//            extensibleScope.addClassifierDescriptor(descriptor)
//        }
//        return descriptors
//    }
//
//    private fun resolveTypeParametersForDescriptor(
//        containingDescriptor: DeclarationDescriptor,
//        scopeForAnnotationsResolve: LexicalScope,
//        typeParameters: List<CjTypeParameter>,
//        trace: BindingTrace
//    ): List<TypeParameterDescriptorImpl> {
//        assert(
//            containingDescriptor is FunctionDescriptor ||
////                    containingDescriptor is  PropertyDescriptor ||
//                    containingDescriptor is TypeAliasDescriptor
//        ) { "This method should be called for functions, properties, or type aliases, got $containingDescriptor" }
//
//        val result = mutableListOf<TypeParameterDescriptorImpl>()
//
//        for (i in typeParameters.indices) {
//            val typeParameter = typeParameters[i]
//            result.add(
//                resolveTypeParameterForDescriptor(
//                    containingDescriptor,
//                    scopeForAnnotationsResolve,
//                    typeParameter,
//                    i,
//                    trace
//                )
//            )
//        }
//
//        return result
//    }
//
//    private fun resolveTypeParameterForDescriptor(
//        containingDescriptor: DeclarationDescriptor,
//        scopeForAnnotationsResolve: LexicalScope,
//        typeParameter: CjTypeParameter,
//        index: Int,
//        trace: BindingTrace
//    ): TypeParameterDescriptorImpl {
//        if (typeParameter.variance != Variance.INVARIANT) {
//            trace.report(Errors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.on(typeParameter))
//        }
//
//        val annotations: Annotations =
//            annotationResolver.resolveAnnotationsWithArguments(
//                scopeForAnnotationsResolve,
//                typeParameter.modifierList,
//                trace
//            )
//
//
//        val typeParameterDescriptor: TypeParameterDescriptorImpl =
//            TypeParameterDescriptorImpl.createForFurtherModification(
//                containingDescriptor,
//                annotations,
//                false,
//                typeParameter.variance,
//                CjPsiUtil.safeName(typeParameter.name),
//                index,
//                typeParameter.toSourceElement(),
//                {
////                    type: CangJieType? ->
////                    if (containingDescriptor !is  TypeAliasDescriptor) {
////                        trace.report( Errors.CYCLIC_GENERIC_UPPER_BOUND.on(typeParameter))
////                    }
//                    null
//                },
//
//                supertypeLoopsResolver,
//                storageManager
//            )
//        return typeParameterDescriptor
//
//    }
//
//    fun resolveTypeAliasDescriptor(
//        containingDeclaration: DeclarationDescriptor,
//        scope: LexicalScope,
//        typeAlias: CjTypeAlias,
//        trace: BindingTrace
//    ): TypeAliasDescriptor {
//        TODO()
//    }
//
//
//    companion object {
//
//        fun getDefaultModality(
//            containingDescriptor: DeclarationDescriptor,
//            visibility: DescriptorVisibility,
//            isBodyPresent: Boolean
//        ): Modality {
//            val defaultModality: Modality
//            if (containingDescriptor is ClassDescriptor) {
//                val isTrait =
//                    containingDescriptor.getKind() == ClassKind.INTERFACE
//                val isDefinitelyAbstract = isTrait && !isBodyPresent
//                val basicModality: Modality =
//                    if (isTrait && !DescriptorVisibilities.isPrivate(visibility)) Modality.OPEN else Modality.FINAL
//                defaultModality =
//                    if (isDefinitelyAbstract) Modality.ABSTRACT else basicModality
//            } else {
//                defaultModality = Modality.FINAL
//            }
//            return defaultModality
//        }
//
//        fun getDefaultVisibility(
//            modifierListOwner: CjModifierListOwner,
//            containingDescriptor: DeclarationDescriptor?
//        ): DescriptorVisibility {
//            val defaultVisibility: DescriptorVisibility
//            if (containingDescriptor is ClassDescriptor) {
//                val modifierList: CjModifierList? = modifierListOwner.getModifierList()
//                defaultVisibility =
//
//                    DescriptorVisibilities.DEFAULT_VISIBILITY
//            } else if (containingDescriptor is FunctionDescriptor /*|| containingDescriptor is PropertyDescriptor*/) {
//                defaultVisibility = DescriptorVisibilities.LOCAL
//            } else {
//                defaultVisibility = DescriptorVisibilities.DEFAULT_VISIBILITY
//            }
//            return defaultVisibility
//        }
//    }
//}
