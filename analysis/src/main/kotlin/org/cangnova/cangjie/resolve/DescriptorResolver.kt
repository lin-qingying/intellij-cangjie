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

package org.cangnova.cangjie.resolve

import com.google.common.collect.Lists
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isNothing
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isUnit
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationSplitter
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.CompositeAnnotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl.Companion.createForFurtherModification
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl.Companion.createWithDestructuringDeclarations
import org.cangnova.cangjie.descriptors.impl.VariableDescriptorImpl.Companion.create
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.ClassId.Companion.fromString
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames.anonymousParameterName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.DescriptorUtils.getDispatchReceiverParameterIfNeeded
import org.cangnova.cangjie.resolve.DescriptorUtils.getParentOfType
import org.cangnova.cangjie.resolve.DescriptorUtils.isAnonymousObject
import org.cangnova.cangjie.resolve.DescriptorUtils.isLocal
import org.cangnova.cangjie.resolve.DescriptorUtils.isSubclass
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveMemberModalityFromModifiers
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import org.cangnova.cangjie.resolve.VariableAsPropertyInfo.Companion.createFromProperty
import org.cangnova.cangjie.resolve.VariableTypeAndInitializerResolver.Companion.getTypeForVariableWithoutReturnType
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.util.isSingleUnderscore
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil.forceResolveAllContents
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyTypeAliasDescriptor.Companion.create
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.ScopeUtils.makeScopeForPropertyInitializer
import org.cangnova.cangjie.resolve.scopes.ScopeUtils.makeScopeForVariableInitializer
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.ErrorUtils.invalidType
import org.cangnova.cangjie.types.TypeUtils.equalTypes
import org.cangnova.cangjie.types.TypeUtils.getClassDescriptor
import org.cangnova.cangjie.types.checker.TypeIntersector
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.FunctionsTypingVisitor
import org.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor
import java.util.*

class DescriptorResolver(
    private val annotationResolver: AnnotationResolver,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager,
    private val typeResolver: TypeResolver,
    private val supertypeLoopsResolver: SupertypeLoopChecker,
    private val variableTypeAndInitializerResolver: VariableTypeAndInitializerResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val overloadChecker: OverloadChecker,
    private val languageVersionSettings: LanguageVersionSettings,

    private val functionsTypingVisitor: FunctionsTypingVisitor,

    private val modifiersChecker: ModifiersChecker,
    private val wrappedTypeFactory: WrappedTypeFactory,
    project: Project,
    private val typeApproximator: TypeApproximator,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>,

    ) {


    init {

    }

    fun resolvePrimaryConstructorParameterToAVariable(
        classDescriptor: ClassDescriptor,
        valueParameter: ValueParameterDescriptor,
        scope: LexicalScope,
        parameter: CjParameter,
        trace: BindingTrace
    ): VariableDescriptor {
        val type = resolveParameterType(scope, parameter, trace)
        val name = parameter.nameAsSafeName
        val isMutable = parameter.isMutable
        val modifierList = parameter.modifierList

        if (modifierList != null) {
            if (modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
                trace.report(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.on(parameter))
            }
        }

        val allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, parameter.annotations, trace)
        val targetSet: MutableSet<AnnotationUseSiteTarget> = EnumSet.of(
            AnnotationUseSiteTarget.PROPERTY,
            AnnotationUseSiteTarget.PROPERTY_GETTER,
            AnnotationUseSiteTarget.FIELD,
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER,
            AnnotationUseSiteTarget.PROPERTY_SETTER
        )
        if (isMutable) {
            targetSet.add(AnnotationUseSiteTarget.PROPERTY_SETTER)
            targetSet.add(AnnotationUseSiteTarget.SETTER_PARAMETER)
        }
        AnnotationSplitter(
            storageManager, allAnnotations, targetSet
        )

        //        Annotations propertyAnnotations = new CompositeAnnotations(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY),
//                annotationSplitter.getOtherAnnotations()
//        );
        val variableDescriptor = create(
            classDescriptor,
            name,
            resolveVisibilityFromModifiers(parameter, getDefaultVisibility(parameter, classDescriptor)),
            isMutable,


            parameter.toSourceElement()


        )
        variableDescriptor.setType(
            type, emptyList(), getDispatchReceiverParameterIfNeeded(classDescriptor)
        )


        //
//        Annotations setterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
//        Annotations getterAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER)));
//
        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, variableDescriptor)
        trace.record(BindingContext.VALUE_PARAMETER_AS_VARIABLE, valueParameter, variableDescriptor)
        return variableDescriptor
    }

    fun resolveLocalVariableDescriptor(
        parameter: CjParameterBase,
        type: CangJieType,
        trace: BindingTrace,
        scope: LexicalScope
    ): VariableDescriptor {
        val approximatedType = typeApproximator.approximateDeclarationType(type, true)
        val variableDescriptor: VariableDescriptor = LocalVariableDescriptor(
            scope.ownerDescriptor,
            annotationResolver.resolveAnnotationsWithArguments(scope, parameter.annotations, trace),
            CjPsiUtil.safeName(parameter.name),
            approximatedType,
            false,
            parameter.toSourceElement()
        )
        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor)
        // Type annotations also should be resolved
        forceResolveAllContents(type.annotations)
        return variableDescriptor
    }

    fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        parameter: CjParameterBase,
        expression: CjExpression?,
        trace: BindingTrace
    ): VariableDescriptor {
        var type: CangJieType? = null
        if (expression != null) {
            type = expressionTypingServices.getTypeInfo(
                scope, expression, trace
            ).type
        }
        if (type == null) {
            type = invalidType
        }
        return resolveLocalVariableDescriptor(parameter, type, trace, scope)
    }

    fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        parameter: CjParameterBase,
        trace: BindingTrace
    ): VariableDescriptor {
        val type = resolveParameterType(scope, parameter, trace)
        return resolveLocalVariableDescriptor(parameter, type, trace, scope)
    }

    private fun resolveParameterType(
        scope: LexicalScope,
        parameter: CjParameterBase,
        trace: BindingTrace
    ): CangJieType {
        val typeReference = parameter.typeReference
        val type = if (typeReference != null) {
            typeResolver.resolveType(scope, typeReference, trace, true)
        } else if (parameter is CjCatchParameter && !parameter.typeReferences.isEmpty()) {
            typeResolver.resolveType(scope, parameter.typeReferences, trace, true)
        } else {
            // Error is reported by the parser
            createErrorType(ErrorTypeKind.NO_TYPE_SPECIFIED, parameter.text)
        }
        if (type is MultipleSupertypeTypeInferenceFailure) {
            trace.report(
                TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(
                    parameter,
                    type.intersectedTypes
                )
            )
        }
        return type
    }

    private fun getDefaultSupertype(
        classDescriptor: ClassDescriptor,
        supertypes: List<CangJieType>,
        classId: ClassId?
    ): CangJieType? {
        //        根据仓颉继承规则，做如下配置
//        1. 如果类型为Class且没有类型为class的父类，则默认继承为Object
//        2. 如果类型为Interface,Struct,enum且没有类型为interface的父类，则默认继承为Any

        if (classDescriptor.kind == ClassKind.CLASS) {
            if (fromString("std/core/Object").equals(classId)) {
                return builtIns.stdlibTypes.anyType
            }
            if (supertypes.isEmpty()) {
                return builtIns.stdlibTypes.objectType
            }

            for (supertype in supertypes) {
                if (supertype.classKind == ClassKind.CLASS) {
                    return null
                }
            }
            return builtIns.stdlibTypes.objectType
        }

        //        if (classDescriptor.getKind() == ClassKind.ENUM_CONSTRUCTOR) {
//            return ((ClassDescriptor) classDescriptor.getContainingDeclaration()).getDefaultType();
//        } else if (classDescriptor.getKind() == ClassKind.CLASS) {
//            return builtIns.getObjectType();
//        }

//当没有父类时，返回any接口
// TODO 注：当获取超类型时，并且为可扩展的类型，考虑到扩展接口，需按情况去除any接口，以保证类型推导正常
        if (supertypes.isEmpty()) {
            return builtIns.stdlibTypes.anyType
        }
        return null
    }

    fun resolveSupertypes(
        scope: LexicalScope,
        classDescriptor: ClassDescriptor,
        typeStatement: CjPureTypeStatement?,
        trace: BindingTrace
    ): List<CangJieType> {
//        builtIns.setSourcesModuleDescriptor(DescriptorUtilsKt.getModule(scope.getOwnerDescriptor()));

        val supertypes: MutableList<CangJieType> = Lists.newArrayList()
        val delegationSpecifiers =
            typeStatement?.superTypeListEntries ?: emptyList()
        val declaredSupertypes = resolveSuperTypeListEntries(
            scope,
            delegationSpecifiers,
            typeResolver, trace, false
        )

        for (declaredSupertype in declaredSupertypes) {
            addValidSupertype(supertypes, declaredSupertype)
        }


        val classId = (typeStatement as CjClassLikeDeclaration).getClassId()


        //不为Any类型默认继承
        if (!fromString("std/core/Any").equals(classId)) {
            val defualtType = getDefaultSupertype(classDescriptor, supertypes, classId)
            if (defualtType != null) {
                addValidSupertype(supertypes, defualtType)
            }
        }


        return supertypes
    }

    private fun resolveAsPropertyDescriptor(
        container: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
        scopeForInitializerResolution: LexicalScope,
        variableDeclaration: CjVariableDeclaration,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?,
        propertyInfo: VariableAsPropertyInfo
    ): PropertyDescriptor {
        val annotations =
            variableDeclaration.annotations
        variableDeclaration.modifierList
        val isVar = variableDeclaration.isVar

        val visibility =
            resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container))
        val modality = if (container is ClassDescriptor)
            resolveMemberModalityFromModifiers(
                variableDeclaration,
                getDefaultModality(container, visibility, propertyInfo.hasBody),
                trace.bindingContext, container
            )
        else
            Modality.FINAL

        val allAnnotations =
            annotationResolver.resolveAnnotationsWithoutArguments(scopeForDeclarationResolution, annotations, trace)
        val targetSet: MutableSet<AnnotationUseSiteTarget> = EnumSet.of(
            AnnotationUseSiteTarget.PROPERTY,
            AnnotationUseSiteTarget.PROPERTY_GETTER,
            AnnotationUseSiteTarget.FIELD
        )
        if (isVar) {
            targetSet.add(AnnotationUseSiteTarget.PROPERTY_SETTER)
            targetSet.add(AnnotationUseSiteTarget.SETTER_PARAMETER)
        }

        val annotationSplitter = AnnotationSplitter(
            storageManager, allAnnotations, targetSet
        )

        CompositeAnnotations(
            listOf(
                annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY),
                annotationSplitter.getOtherAnnotations()
            )
        )

        val propertyDescriptor = PropertyDescriptorImpl.create(
            container,  /*      propertyAnnotations,*/
            Annotations.EMPTY,
            modality,
            visibility,
            isVar,
            CjPsiUtil.safeName(variableDeclaration.name),
            CallableMemberDescriptor.Kind.DECLARATION,
            variableDeclaration.toSourceElement() //                modifierList != null && modifierList.hasModifier(CjTokens.LATEINIT_KEYWORD),
            //                modifierList != null && modifierList.hasModifier(CjTokens.CONST_KEYWORD),
            //                modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) && container instanceof PackageFragmentDescriptor ||
            //                        container instanceof ClassDescriptor && ((ClassDescriptor) container).isExpect(),
            //                modifierList != null && PsiUtilsKt.hasActualModifier(modifierList),
            //                modifierList != null && modifierList.hasModifier(CjTokens.EXTERNAL_KEYWORD),
            //                propertyInfo.getHasDelegate()
        )

        var typeParameterDescriptors: List<TypeParameterDescriptorImpl>
        var scopeForDeclarationResolutionWithTypeParameters: LexicalScope
        var scopeForInitializerResolutionWithTypeParameters: LexicalScope?
        var receiverType: CangJieType? = null

        run {
            val typeParameters = variableDeclaration.typeParameters
            if (typeParameters.isEmpty()) {
                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution
                typeParameterDescriptors = emptyList()
            } else {
                val writableScopeForDeclarationResolution = LexicalWritableScope(
                    scopeForDeclarationResolution, container, false, TraceBasedLocalRedeclarationChecker(
                        trace,
                        overloadChecker
                    ),
                    LexicalScopeKind.PROPERTY_HEADER
                )
                val writableScopeForInitializerResolution = LexicalWritableScope(
                    scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING,
                    LexicalScopeKind.PROPERTY_HEADER
                )
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                    propertyDescriptor,
                    scopeForDeclarationResolution, typeParameters, trace
                )
                for (descriptor in typeParameterDescriptors) {
                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor)
                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor)
                }
                writableScopeForDeclarationResolution.freeze()
                writableScopeForInitializerResolution.freeze()
                resolveGenericBounds(
                    variableDeclaration,
                    propertyDescriptor,
                    writableScopeForDeclarationResolution,
                    typeParameterDescriptors,
                    trace
                )
                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution
            }
        }

        val receiverTypeRef = variableDeclaration.receiverTypeReference
        if (receiverTypeRef != null) {
            receiverType =
                typeResolver.resolveType(scopeForDeclarationResolutionWithTypeParameters, receiverTypeRef, trace, true)
            // 在仓颉语言中，extend 成员使用 dispatchReceiver，不需要创建 extensionReceiver
            // dispatchReceiver 会通过 getDispatchReceiverParameterIfNeeded(container) 设置
        }




        val scopeForInitializer = makeScopeForPropertyInitializer(
            scopeForInitializerResolutionWithTypeParameters!!, propertyDescriptor
        )
        val propertyType = propertyInfo.variableType
        val typeIfKnown = propertyType
            ?: variableTypeAndInitializerResolver.resolveTypeOptional(
                propertyDescriptor, scopeForInitializer,
                variableDeclaration, dataFlowInfo, inferenceSession!!,
                trace,  /* local = */false
            )

        val getter = resolvePropertyGetterDescriptor(
            scopeForDeclarationResolutionWithTypeParameters,
            variableDeclaration,
            propertyDescriptor,
            annotationSplitter,
            trace,
            typeIfKnown,
            propertyInfo.propertyGetter,

            inferenceSession
        )
        //
        val type = checkNotNull(
            typeIfKnown ?: getter.returnType
        ) { "At least getter type must be initialized via resolvePropertyGetterDescriptor" }

        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
            propertyDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, inferenceSession!!, trace
        )

        propertyDescriptor.setType(
            type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container)
        )

        val setter = resolvePropertySetterDescriptor(
            scopeForDeclarationResolutionWithTypeParameters,
            variableDeclaration,
            propertyDescriptor,
            annotationSplitter,
            trace,
            propertyInfo.propertySetter,

            inferenceSession
        )

        propertyDescriptor.initialize(
            getter, setter

        )
        trace.record(BindingContext.VARIABLE, variableDeclaration, propertyDescriptor)
        return propertyDescriptor
    }

    private fun resolvePropertySetterDescriptor(
        scopeWithTypeParameters: LexicalScope,
        property: CjVariableDeclaration,
        propertyDescriptor: PropertyDescriptor,
        annotationSplitter: AnnotationSplitter,
        trace: BindingTrace,
        setter: CjPropertyAccessor?,

        inferenceSession: InferenceSession?
    ): PropertySetterDescriptor? {
        var setterDescriptor: PropertySetterDescriptorImpl? = null
        val setterTargetedAnnotations =
            annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY_SETTER)
        val parameterTargetedAnnotations =
            annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.SETTER_PARAMETER)
        if (setter != null) {
            val annotations: Annotations = CompositeAnnotations(
                listOf(
                    setterTargetedAnnotations,
                    annotationResolver.resolveAnnotationsWithoutArguments(
                        scopeWithTypeParameters,
                        setter.annotations,
                        trace
                    )
                )
            )
            val parameter = setter.parameter

            setterDescriptor = PropertySetterDescriptorImpl(
                propertyDescriptor, annotations,
                resolveMemberModalityFromModifiers(
                    setter, propertyDescriptor.modality,
                    trace.bindingContext, propertyDescriptor.containingDeclaration
                ),
                resolveVisibilityFromModifiers(setter, propertyDescriptor.visibility),  /* isDefault = */

                CallableMemberDescriptor.Kind.DECLARATION, null, setter.toSourceElement()
            )
            val returnTypeReference = setter.returnTypeReference
            if (returnTypeReference != null) {
                val returnType = typeResolver.resolveType(scopeWithTypeParameters, returnTypeReference, trace, true)
                if (!isUnit(returnType)) {
                    trace.report(WRONG_SETTER_RETURN_TYPE.on(returnTypeReference))
                }
            }

            if (parameter != null) {
                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case

                if (parameter.hasDefaultValue()) {
                    parameter.defaultValue?.let { trace.report(SETTER_PARAMETER_WITH_DEFAULT_VALUE.on(it)) }
                }

                val type: CangJieType
                val typeReference = parameter.typeReference
                if (typeReference == null) {
                    type = propertyDescriptor.type // TODO : this maybe unknown at this point
                } else {
                    type = typeResolver.resolveType(scopeWithTypeParameters, typeReference, trace, true)
                    val inType = propertyDescriptor.type
                    if (!equalTypes(type, inType)) {
                        trace.report(WRONG_SETTER_PARAMETER_TYPE.on(typeReference, inType, type))
                    }
                }

                val valueParameterDescriptor = resolveValueParameterDescriptor(
                    scopeWithTypeParameters,
                    setterDescriptor,
                    parameter,
                    0,
                    type,
                    trace,
                    parameterTargetedAnnotations,
                    inferenceSession
                )
                setterDescriptor.initialize(valueParameterDescriptor)
            } else {
                setterDescriptor.initializeDefault()
            }

            trace.record(BindingContext.PROPERTY_ACCESSOR, setter, setterDescriptor)
        } else if (property.isVar) {
            setterDescriptor = DescriptorFactory.createSetter(
                propertyDescriptor, setterTargetedAnnotations, parameterTargetedAnnotations,
                setterTargetedAnnotations.isEmpty() && parameterTargetedAnnotations.isEmpty(),

                propertyDescriptor.source
            )
        }

        if (!property.isVar) {
            if (setter != null) {
                trace.report(LET_WITH_SETTER.on(setter))
            }
        }
        return setterDescriptor
    }

    private fun resolvePropertyGetterDescriptor(
        scopeForDeclarationResolution: LexicalScope,
        property: CjVariableDeclaration,
        propertyDescriptor: PropertyDescriptor,
        annotationSplitter: AnnotationSplitter,
        trace: BindingTrace,
        propertyTypeIfKnown: CangJieType?,
        getter: CjPropertyAccessor?,

        inferenceSession: InferenceSession?
    ): PropertyGetterDescriptorImpl {
        val getterDescriptor: PropertyGetterDescriptorImpl
        val getterType: CangJieType?
        val getterTargetedAnnotations =
            annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY_GETTER)
        if (getter != null) {
            val getterAnnotations: Annotations = CompositeAnnotations(
                listOf(
                    getterTargetedAnnotations,
                    annotationResolver.resolveAnnotationsWithoutArguments(
                        scopeForDeclarationResolution,
                        getter.annotations,
                        trace
                    )
                )
            )

            getterDescriptor = PropertyGetterDescriptorImpl(
                propertyDescriptor, getterAnnotations,
                resolveMemberModalityFromModifiers(
                    getter, propertyDescriptor.modality,
                    trace.bindingContext, propertyDescriptor.containingDeclaration
                ),
                resolveVisibilityFromModifiers(getter, propertyDescriptor.visibility),  /* isDefault = */


                CallableMemberDescriptor.Kind.DECLARATION, null, getter.toSourceElement()
            )
            getterType = determineGetterReturnType(
                scopeForDeclarationResolution, trace, getterDescriptor, getter, propertyTypeIfKnown, inferenceSession
            )
        } else {
            getterDescriptor = DescriptorFactory.createGetter(
                propertyDescriptor, getterTargetedAnnotations,
                getterTargetedAnnotations.isEmpty()

            )
            getterType = propertyTypeIfKnown
        }

        getterDescriptor.initialize(
            getterType
                ?: getTypeForVariableWithoutReturnType(propertyDescriptor.name.asString())
        )

        if (getter != null) {
            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor)
        }

        return getterDescriptor
    }

    private fun determineGetterReturnType(
        scope: LexicalScope,
        trace: BindingTrace,
        getterDescriptor: PropertyGetterDescriptor,
        getter: CjPropertyAccessor,
        propertyTypeIfKnown: CangJieType?,
        inferenceSession: InferenceSession?
    ): CangJieType? {
        val returnTypeReference = getter.returnTypeReference
        if (returnTypeReference != null) {
            val explicitReturnType = typeResolver.resolveType(scope, returnTypeReference, trace, true)
            if (propertyTypeIfKnown != null && !equalTypes(explicitReturnType, propertyTypeIfKnown)) {
                trace.report(
                    WRONG_GETTER_RETURN_TYPE.on(
                        returnTypeReference,
                        propertyTypeIfKnown,
                        explicitReturnType
                    )
                )
            }
            return explicitReturnType
        }

        // If a property has no type specified in the PSI but the getter does (or has an initializer e.g. "val x get() = ..."),
        // infer the correct type for the getter but leave the error type for the property.
        // This is useful for an IDE quick fix which would add the type to the property
        val property = getter.property
        if (property.typeReference == null &&
            getter.hasBody() && !getter.hasBlockBody()
        ) {
            return inferReturnTypeFromExpressionBody(
                trace,
                scope,
                DataFlowInfoFactory.EMPTY,
                getter,
                getterDescriptor,
                inferenceSession
            )
        }

        return propertyTypeIfKnown
    }

    fun resolvePropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
        scopeForInitializerResolution: LexicalScope,
        property: CjProperty,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): PropertyDescriptor {
        return resolveAsPropertyDescriptor(
            containingDeclaration,
            scopeForDeclarationResolution,
            scopeForInitializerResolution,
            property,
            trace,
            dataFlowInfo,
            inferenceSession,
            createFromProperty(property)
        )
    }


    fun resolveGenericBounds(
        declaration: CjTypeParameterListOwner,
        descriptor: DeclarationDescriptor,
        scope: LexicalScope,
        parameters: List<TypeParameterDescriptorImpl>,
        trace: BindingTrace
    ) {
        val upperBoundCheckRequests: MutableList<UpperBoundCheckRequest> = Lists.newArrayList()

        val typeParameters = declaration.typeParameters
        val parameterByName: MutableMap<Name, TypeParameterDescriptorImpl> = HashMap()
        for (i in typeParameters.indices) {
            val cjTypeParameter = typeParameters[i]
            val typeParameterDescriptor = parameters[i]

            parameterByName[typeParameterDescriptor.name] = typeParameterDescriptor

            val extendsBound = cjTypeParameter.extendsBound
            if (extendsBound != null) {
                val type = typeResolver.resolveType(scope, extendsBound, trace, false)
                typeParameterDescriptor.addUpperBound(type)
                upperBoundCheckRequests.add(UpperBoundCheckRequest(cjTypeParameter.nameAsName, extendsBound, type))
            }
        }
        for (constraint in declaration.typeConstraints) {
            val subjectTypeParameterName = constraint.subjectTypeParameterName ?: continue
            val referencedName = subjectTypeParameterName.referencedNameAsName
            val typeParameterDescriptor = parameterByName[referencedName]


            val boundTypeReferences = constraint.boundTypeReferences
            for (boundTypeReference in boundTypeReferences) {
                var bound: CangJieType? = null

                    bound = typeResolver.resolveType(scope, boundTypeReference, trace, false)
                    upperBoundCheckRequests.add(UpperBoundCheckRequest(referencedName, boundTypeReference, bound))


                if (typeParameterDescriptor != null) {
                    trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor)

                        typeParameterDescriptor.addUpperBound(bound)

                }
            }
        }

        for (parameter in parameters) {
            parameter.addDefaultUpperBound()
            parameter.setInitialized()
        }

        for (parameter in parameters) {
            checkConflictingUpperBounds(
                trace, parameter,
                typeParameters[parameter.index]
            )
        }

        if (declaration !is CjClass) {
            checkUpperBoundTypes(trace, upperBoundCheckRequests, declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD))
            checkNamesInConstraints(declaration, descriptor, scope, trace)
        }
    }


    fun checkNamesInConstraints(
        declaration: CjTypeParameterListOwner,
        descriptor: DeclarationDescriptor,
        scope: LexicalScope,
        trace: BindingTrace
    ) {
        for (constraint in declaration.typeConstraints) {
            val nameExpression = constraint.subjectTypeParameterName ?: continue

            val name = nameExpression.referencedNameAsName

            val classifier = scope.findClassifier(name, NoLookupLocation.FOR_NON_TRACKED_SCOPE)
            if (classifier is TypeParameterDescriptor && classifier.containingDeclaration === descriptor) continue

            if (classifier != null) {
                // To tell the user that we look only for locally defined type parameters
                trace.report(
                    NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(
                        nameExpression,
                        constraint,
                        declaration
                    )
                )
                trace.record(BindingContext.REFERENCE_TARGET, nameExpression, classifier)
            } else {
                trace.report(UNRESOLVED_REFERENCE.on(nameExpression, nameExpression))
            }

            val boundTypeReference = constraint.boundTypeReference
            if (boundTypeReference != null) {
                typeResolver.resolveType(scope, boundTypeReference, trace, true)
            }
        }
    }

    fun inferReturnTypeFromExpressionBody(
        trace: BindingTrace,
        scope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        inferenceSession: InferenceSession?
    ): CangJieType {


        return wrappedTypeFactory.createRecursionIntolerantDeferredType(trace) {
            PreliminaryDeclarationVisitor.createForDeclaration(function, trace, languageVersionSettings)

            val type = expressionTypingServices.getBodyExpressionType(
                trace, scope, dataFlowInfo, function, functionDescriptor, inferenceSession
            )

            val publicType = transformAnonymousTypeIfNeeded(
                functionDescriptor, function, type, trace, anonymousTypeTransformers, languageVersionSettings
            )

            val approximatedType = typeApproximator.approximateDeclarationType(publicType, false)

            val sanitizedType = declarationReturnTypeSanitizer.sanitizeReturnType(
                approximatedType, wrappedTypeFactory, trace, languageVersionSettings
            )

            functionsTypingVisitor.checkTypesForReturnStatements(function, trace, sanitizedType)

            sanitizedType
        }

    }


    fun resolveValueParameterDescriptor(
        scope: LexicalScope,
        owner: FunctionDescriptor,
        valueParameter: CjParameter,
        index: Int,
        type: CangJieType,
        trace: BindingTrace,
        additionalAnnotations: Annotations,
        inferenceSession: InferenceSession?
    ): ValueParameterDescriptorImpl {
//        CangJieType varargElementType = null;
        val variableType = type

        //        if (valueParameter.hasModifier(VARARG_KEYWORD)) {
//            varargElementType = type;
//            variableType = getVarargParameterType(type);
//        }
        val valueParameterAnnotations =
            resolveValueParameterAnnotations(scope, valueParameter, trace, additionalAnnotations)


        // NB: let/var for parameter is only allowed in primary constructors where single underscore names are still prohibited.
        // The problem with let/var is that when lazy resolveName try to find their descriptor, it searches through the member scope
        // of containing class where, it can not find a descriptor with special name.
        // Thus, to preserve behavior, we don't use a special name for let/var.
        val parameterName =
            if (!valueParameter.hasLetOrVar() && valueParameter.isSingleUnderscore)
                anonymousParameterName(index)
            else
                CjPsiUtil.safeName(valueParameter.name)

        val valueParameterDescriptor = createWithDestructuringDeclarations(
            owner,
            null,
            index,
            valueParameterAnnotations,
            parameterName,
            valueParameter.isNamed,
            variableType,
            valueParameter.hasDefaultValue(),  //                varargElementType,

            valueParameter.toSourceElement(),

        )

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor)
        return valueParameterDescriptor
    }

    private fun resolveValueParameterAnnotations(
        scope: LexicalScope,
        parameter: CjParameter,
        trace: BindingTrace,
        additionalAnnotations: Annotations
    ): Annotations {
        parameter.modifierList ?: return additionalAnnotations
        val annotations = parameter.annotations
        val allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, annotations, trace)
        if (!parameter.hasLetOrVar()) {
            return CompositeAnnotations(allAnnotations, additionalAnnotations)
        }

        val splitter = AnnotationSplitter(
            storageManager, allAnnotations, setOf(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER)
        )
        return CompositeAnnotations(
            splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER),
            additionalAnnotations
        )
    }

    fun resolveTypeParametersForDescriptor(
        containingDescriptor: DeclarationDescriptor,
        extensibleScope: LexicalWritableScope,
        scopeForAnnotationsResolve: LexicalScope,
        typeParameters: List<CjTypeParameter>,
        trace: BindingTrace
    ): List<TypeParameterDescriptorImpl> {
        val descriptors =
            resolveTypeParametersForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameters, trace)
        for (descriptor in descriptors) {
            extensibleScope.addClassifierDescriptor(descriptor)
        }
        return descriptors
    }

    private fun resolveTypeParametersForDescriptor(
        containingDescriptor: DeclarationDescriptor,
        scopeForAnnotationsResolve: LexicalScope,
        typeParameters: List<CjTypeParameter>,
        trace: BindingTrace
    ): List<TypeParameterDescriptorImpl> {
        assert(
            containingDescriptor is FunctionDescriptor ||  //                containingDescriptor instanceof PropertyDescriptor ||
                    containingDescriptor is TypeAliasDescriptor
        ) { "This method should be called for functions, properties, or type aliases, got $containingDescriptor" }

        val result: MutableList<TypeParameterDescriptorImpl> = ArrayList()
        var i = 0
        val typeParametersSize = typeParameters.size
        while (i < typeParametersSize) {
            val typeParameter = typeParameters[i]
            result.add(
                resolveTypeParameterForDescriptor(
                    containingDescriptor,
                    scopeForAnnotationsResolve,
                    typeParameter,
                    i,
                    trace
                )
            )
            i++
        }
        return result
    }

    private fun resolveTypeParameterForDescriptor(
        containingDescriptor: DeclarationDescriptor,
        scopeForAnnotationsResolve: LexicalScope,
        typeParameter: CjTypeParameter,
        index: Int,
        trace: BindingTrace
    ): TypeParameterDescriptorImpl {
        if (typeParameter.variance != Variance.INVARIANT) {
            trace.report(VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.on(typeParameter))
        }

        val annotations =
            annotationResolver.resolveAnnotationsWithArguments(
                scopeForAnnotationsResolve,
                typeParameter.annotations,
                trace
            )

        val typeParameterDescriptor = createForFurtherModification(
            containingDescriptor,
            annotations,

            typeParameter.variance,
            CjPsiUtil.safeName(typeParameter.name),
            index,
            typeParameter.toSourceElement(),
            { type: CangJieType? ->
                if (containingDescriptor !is TypeAliasDescriptor) {
                    trace.report(CYCLIC_GENERIC_UPPER_BOUND.on(typeParameter))
                }
                null
            },
            supertypeLoopsResolver,
            storageManager
        )
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor)
        return typeParameterDescriptor
    }

    fun resolveTypeAliasDescriptor(
        containingDeclaration: DeclarationDescriptor,
        scope: LexicalScope,
        typeAlias: CjTypeAlias,
        trace: BindingTrace
    ): TypeAliasDescriptor {
        //        if (!(containingDeclaration instanceof PackageFragmentDescriptor) &&
//                !(containingDeclaration instanceof ScriptDescriptor)) {
//            trace.report(TOPLEVEL_TYPEALIASES_ONLY.on(typeAlias));
//        }

        val annotations = typeAlias.annotations
        typeAlias.modifierList
        val visibility =
            resolveVisibilityFromModifiers(typeAlias, getDefaultVisibility(typeAlias, containingDeclaration))

        val allAnnotations = annotationResolver.resolveAnnotationsWithArguments(scope, annotations, trace)
        val name = CjPsiUtil.safeName(typeAlias.name)
        val sourceElement = typeAlias.toSourceElement()
        val typeAliasDescriptor = create(
            storageManager, trace, containingDeclaration, allAnnotations, name, sourceElement, visibility
        )

        var typeParameterDescriptors: List<TypeParameterDescriptorImpl>
        var scopeWithTypeParameters: LexicalScope?
        run {
            val typeParameters = typeAlias.typeParameters
            if (typeParameters.isEmpty()) {
                scopeWithTypeParameters = scope
                typeParameterDescriptors = emptyList()
            } else {
                val writableScope = LexicalWritableScope(
                    scope, containingDeclaration, false, TraceBasedLocalRedeclarationChecker(
                        trace,
                        overloadChecker
                    ),
                    LexicalScopeKind.TYPE_ALIAS_HEADER
                )
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                    typeAliasDescriptor, writableScope, scope, typeParameters, trace
                )
                writableScope.freeze()
                checkNoGenericBoundsOnTypeAliasParameters(typeAlias, trace)
                resolveGenericBounds(typeAlias, typeAliasDescriptor, writableScope, typeParameterDescriptors, trace)
                scopeWithTypeParameters = writableScope
            }
        }

        val typeReference = typeAlias.getTypeReference()
        if (typeReference == null) {
            typeAliasDescriptor.initialize(
                typeParameterDescriptors,
                createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()),
                createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString())
            )
        } else {
            typeAliasDescriptor.initialize(
                typeParameterDescriptors,
                storageManager.createRecursionTolerantLazyValue(
                    { typeResolver.resolveAbbreviatedType(scopeWithTypeParameters!!, typeReference, trace) },
                    createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.name.asString())
                ),
                storageManager.createRecursionTolerantLazyValue(
                    { typeResolver.resolveExpandedTypeForTypeAlias(typeAliasDescriptor) },
                    createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.name.asString())
                )
            )
        }

        trace.record(BindingContext.TYPE_ALIAS, typeAlias, typeAliasDescriptor)
        return typeAliasDescriptor
    }

    fun resolveVariableDescriptorByPattern(
        name: Name,
        container: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
//        scopeForInitializerResolution: LexicalScope,
        variableDeclaration: CjVariable<*>,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): List<VariableDescriptor> {
        val context =
            expressionTypingServices.getNewContext(scopeForDeclarationResolution, trace, dataFlowInfo, inferenceSession)



        expressionTypingServices.expressionTypingComponents.patternMatchingTypingVisitor.visitVariable(
            variableDeclaration, context
        )
////当前声明具有的所有变量声明
        val variables = when (variableDeclaration) {
            is CjPatternVariable -> variableDeclaration.pattern.getAllBindings().mapNotNull {
                trace[BindingContext.VARIABLE, it]
            }
            is CjFieldVariable -> listOfNotNull(trace[BindingContext.VARIABLE, variableDeclaration])
            else -> emptyList()
        }

        return variables.filter {
            it.name == name
        }

//        模式会返回多个声明
        /*、
          1.获取期望类型
          2.获取表达式类型
         */
//
//        val expectedType = variableDeclaration.typeReference?.let {
//            typeResolver.resolveType(
//                scopeForDeclarationResolution, it, trace, true
//            )
//        } ?: NO_EXPECTED_TYPE
//
//        val expressionType = variableDeclaration.initializer?.let {
//            expressionTypingServices.getTypeInfo(scopeForInitializerResolution, it, trace)
//        } ?: ErrorUtils.errorVariableType
//        val type = if (expectedType != NO_EXPECTED_TYPE) expressionType else expressionType
//
//        TODO()
    }

    fun resolveVariableDescriptor(
        container: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
        scopeForInitializerResolution: LexicalScope,
        variableDeclaration: CjVariable<*>,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): VariableDescriptor {
        val variableInfo = createFromProperty(variableDeclaration)

        variableDeclaration.modifierList
        val isVar = variableDeclaration.isVar

        val visibility =
            resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container))

        if (container is ClassDescriptor)
            resolveMemberModalityFromModifiers(
                variableDeclaration,
                getDefaultModality(container, visibility, variableInfo.hasBody),
                trace.bindingContext, container
            )
        else
            Modality.FINAL
        //暂时使用EMPTY

        val variableDescriptor = create(
            container,
            CjPsiUtil.safeName(variableDeclaration.name),


            visibility,
            isVar,


            variableDeclaration.toSourceElement()

        )

        var typeParameterDescriptors: List<TypeParameterDescriptorImpl>
        var scopeForDeclarationResolutionWithTypeParameters: LexicalScope?
        var scopeForInitializerResolutionWithTypeParameters: LexicalScope?
        var receiverType: CangJieType? = null
        run {
            val typeParameters = variableDeclaration.typeParameters
            if (typeParameters.isEmpty()) {
                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution
                typeParameterDescriptors = emptyList()
            } else {
                val writableScopeForDeclarationResolution = LexicalWritableScope(
                    scopeForDeclarationResolution, container, false, TraceBasedLocalRedeclarationChecker(
                        trace,
                        overloadChecker
                    ),
                    LexicalScopeKind.PROPERTY_HEADER
                )
                val writableScopeForInitializerResolution = LexicalWritableScope(
                    scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING,
                    LexicalScopeKind.PROPERTY_HEADER
                )
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                    variableDescriptor,
                    scopeForDeclarationResolution, typeParameters, trace
                )
                for (descriptor in typeParameterDescriptors) {
                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor)
                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor)
                }
                writableScopeForDeclarationResolution.freeze()
                writableScopeForInitializerResolution.freeze()
                resolveGenericBounds(
                    variableDeclaration,
                    variableDescriptor,
                    writableScopeForDeclarationResolution,
                    typeParameterDescriptors,
                    trace
                )
                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution
            }
        }

        val receiverTypeRef = variableDeclaration.receiverTypeReference
        if (receiverTypeRef != null) {
            receiverType = typeResolver.resolveType(
                scopeForDeclarationResolutionWithTypeParameters!!,
                receiverTypeRef,
                trace,
                true
            )
            // 在仓颉语言中，extend 成员使用 dispatchReceiver，不需要创建 extensionReceiver
            // dispatchReceiver 会通过 getDispatchReceiverParameterIfNeeded(container) 设置
        }


        val scopeForInitializer = makeScopeForVariableInitializer(
            scopeForInitializerResolutionWithTypeParameters!!, variableDescriptor
        )
        val propertyType = variableInfo.variableType
        val typeIfKnown = propertyType
            ?: variableTypeAndInitializerResolver.resolveTypeOptional(
                variableDescriptor, scopeForInitializer,
                variableDeclaration, dataFlowInfo, inferenceSession,
                trace,  /* local = */false
            )
        val type = typeIfKnown ?: builtIns.unitType
        //        assert type != null : "At least getter type must be initialized via resolvePropertyGetterDescriptor";
        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
            variableDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, inferenceSession, trace
        )

        variableDescriptor.setType(
            type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container)
        )


        trace.record(BindingContext.VARIABLE, variableDeclaration, variableDescriptor)


        if (container is ClassDescriptor && container.kind == ClassKind.INTERFACE) {
            trace.report(INTERFACE_BODY_NO_VARIABLES.on(variableDeclaration))
        }

        return variableDescriptor
    }

    class UpperBoundCheckRequest(
        val typeParameterName: Name?,
        val upperBound: CjTypeReference,
        val upperBoundType: CangJieType
    )

    companion object {
        private fun addValidSupertype(supertypes: MutableList<CangJieType>, declaredSupertype: CangJieType) {
            if (!declaredSupertype.isError) {
                supertypes.add(declaredSupertype)
            }
        }

        private fun containsClass(result: Collection<CangJieType>): Boolean {
            for (type in result) {
                val descriptor = type.constructor.declarationDescriptor
                if (descriptor is ClassDescriptor && descriptor.kind != ClassKind.INTERFACE) {
                    return true
                }
            }
            return false
        }

        fun createAndRecordPrimaryConstructorForObject(
            `object`: CjPureTypeStatement?,
            classDescriptor: ClassDescriptor,
            trace: BindingTrace
        ): ClassConstructorDescriptorImpl {
            val constructorDescriptor =
                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, `object`.toSourceElement())
            if (`object` is PsiElement) {
                val primaryConstructor = `object`.primaryConstructor
                trace.record(
                    BindingContext.CONSTRUCTOR,
                    primaryConstructor ?: `object` as PsiElement, constructorDescriptor
                )
            }
            return constructorDescriptor
        }

        private fun isInsideOuterClassOrItsSubclass(nested: DeclarationDescriptor?, outer: ClassDescriptor): Boolean {
            if (nested == null) return false

            if (nested is ClassDescriptor && isSubclass(nested, outer)) return true

            return isInsideOuterClassOrItsSubclass(nested.containingDeclaration, outer)
        }

        fun checkHasOuterClassInstance(
            scope: LexicalScope,
            trace: BindingTrace,
            reportErrorsOn: PsiElement,
            target: ClassDescriptor
        ): Boolean {
            var classDescriptor = getContainingClass(scope)


            if (!isInsideOuterClassOrItsSubclass(classDescriptor, target)) {
                return true
            }

            while (classDescriptor != null) {
                if (isSubclass(classDescriptor, target)) {
                    return true
                }
                classDescriptor = getParentOfType(
                    classDescriptor,
                    ClassDescriptor::class.java, true
                )
            }
            return true
        }

        fun getContainingClass(scope: LexicalScope): ClassDescriptor? {
            return getParentOfType(
                scope.ownerDescriptor,
                ClassDescriptor::class.java, false
            )
        }

        fun getDefaultVisibility(
            modifierListOwner: CjModifierListOwner?,
            containingDescriptor: DeclarationDescriptor?
        ): DescriptorVisibility {


            if (containingDescriptor is ClassDescriptor && containingDescriptor.kind == ClassKind.INTERFACE) {
                return DescriptorVisibilities.PUBLIC
            }

            return DescriptorVisibilities.INTERNAL
        }

        fun getDefaultModality(
            containingDescriptor: DeclarationDescriptor?,
            visibility: DescriptorVisibility,
            isBodyPresent: Boolean
        ): Modality {
            val defaultModality: Modality
            if (containingDescriptor is ClassDescriptor) {
                val isTrait = containingDescriptor.kind == ClassKind.INTERFACE
                val isDefinitelyAbstract = isTrait && !isBodyPresent
                val basicModality =
                    if (isTrait && !DescriptorVisibilities.isPrivate(visibility)) Modality.OPEN else Modality.FINAL
                defaultModality = if (isDefinitelyAbstract) Modality.ABSTRACT else basicModality
            } else {
                defaultModality = Modality.FINAL
            }
            return defaultModality
        }

        fun checkConflictingUpperBounds(
            trace: BindingTrace,
            parameter: TypeParameterDescriptor,
            typeParameter: CjTypeParameter
        ) {
            if (isNothing(TypeIntersector.getUpperBoundsAsType(parameter))) {
                trace.report(CONFLICTING_UPPER_BOUNDS.on(typeParameter, parameter))
            }
        }

        //    @NotNull
        //    /*package*/ static CangJieType transformAnonymousTypeIfNeeded(
        //            @NotNull DeclarationDescriptorWithVisibility descriptor,
        //            @NotNull CjDeclaration declaration,
        //            @NotNull CangJieType type,
        //            @NotNull BindingTrace trace,
        //            @NotNull Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers,
        //            @NotNull LanguageVersionSettings languageVersionSettings
        //    ) {
        //        for (DeclarationSignatureAnonymousTypeTransformer transformer : anonymousTypeTransformers) {
        //            CangJieType transformedType = transformer.transformAnonymousType(descriptor, type);
        //            if (transformedType != null) {
        //                return transformedType;
        //            }
        //        }
        //
        //        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        //        if (classifier == null /*|| !DescriptorUtils.isAnonymousObject(classifier)*/ || DescriptorUtils.isLocal(descriptor)) {
        //            return type;
        //        }
        //
        //        boolean isPrivate = DescriptorVisibilities.isPrivate(descriptor.getVisibility());
        //        boolean isInlineFunction = descriptor instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) descriptor).isInline();
        //        boolean isAnonymousReturnTypesInPrivateInlineFunctionsForbidden =
        //                languageVersionSettings.supportsFeature(LanguageFeature.ApproximateAnonymousReturnTypesInPrivateInlineFunctions);
        //
        //        if (!isPrivate || (isInlineFunction && isAnonymousReturnTypesInPrivateInlineFunctionsForbidden)) {
        //            if (type.getConstructor().getSupertypes().size() == 1) {
        //                CangJieType approximatingSuperType = type.getConstructor().getSupertypes().iterator().next();
        //                CangJieType substitutedSuperType;
        //                MemberScope memberScope = type.getMemberScope();
        //
        //                if (memberScope instanceof SubstitutingScope) {
        //                    substitutedSuperType = ((SubstitutingScope) memberScope).substitute(approximatingSuperType);
        //                } else {
        //                    substitutedSuperType = approximatingSuperType;
        //                }
        //
        //                UnwrappedType unwrapped = type.unwrap();
        //                boolean lowerNullable = FlexibleTypesKt.lowerIfFlexible(unwrapped).isMarkedOption();
        //                boolean upperNullable = FlexibleTypesKt.upperIfFlexible(unwrapped).isMarkedOption();
        //                if (languageVersionSettings.supportsFeature(LanguageFeature.KeepNullabilityWhenApproximatingLocalType)) {
        //                    if (lowerNullable != upperNullable) {
        //                        return CangJieTypeFactory.flexibleType(
        //                                FlexibleTypesKt.lowerIfFlexible(substitutedSuperType),
        //                                FlexibleTypesKt.upperIfFlexible(substitutedSuperType).makeOptionalAsSpecified(true));
        //                    }
        //                    return TypeUtils.makeOptionalIfNeeded(substitutedSuperType, upperNullable);
        //                } else if (upperNullable) {
        //                    if (lowerNullable) {
        //                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE.on(declaration, substitutedSuperType));
        //                    } else {
        //                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE.on(declaration, substitutedSuperType));
        //                    }
        //                }
        //                return substitutedSuperType;
        //            }
        //            else {
        //                trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.getConstructor().getSupertypes()));
        //            }
        //        }
        //
        //        return type;
        //    }
        fun checkUpperBoundTypes(
            trace: BindingTrace,
            requests: List<UpperBoundCheckRequest>,
            hasOverrideModifier: Boolean
        ) {
            if (requests.isEmpty()) return

            val classBoundEncountered: MutableSet<Name?> = HashSet()
            val allBounds: MutableSet<Pair<Name?, TypeConstructor>> = HashSet()

            for (request in requests) {
                val typeParameterName = request.typeParameterName
                val upperBound = request.upperBoundType
                val upperBoundElement = request.upperBound

                if (!upperBound.isError) {
                    if (!allBounds.add(Pair<Name?, TypeConstructor>(typeParameterName, upperBound.constructor))) {
                        trace.report(REPEATED_BOUND.on(upperBoundElement))
                    } else {
                        val classDescriptor = getClassDescriptor(upperBound)
                        if (classDescriptor != null) {
                            val kind = classDescriptor.kind
                            if (kind == ClassKind.CLASS || kind == ClassKind.ENUM || kind == ClassKind.STRUCT) {
                                if (!classBoundEncountered.add(typeParameterName)) {
                                    trace.report(ONLY_ONE_CLASS_BOUND_ALLOWED.on(upperBoundElement))
                                }
                            }
                        }
                    }
                }

                checkUpperBoundType(upperBoundElement, upperBound, trace, hasOverrideModifier)
            }
        }

        fun checkUpperBoundType(
            upperBound: CjTypeReference?,
            upperBoundType: CangJieType,
            trace: BindingTrace?,
            hasOverrideModifier: Boolean
        ) {
//        if (!hasOverrideModifier && !TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, upperBoundType)) {
//            trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
//        }
//        if (DynamicTypesKt.isDynamic(upperBoundType)) {
//            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
//        }
//        if (FunctionTypesKt.isExtensionFunctionType(upperBoundType)) {
//            trace.report(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE.on(upperBound));
//        }
//        if (DefinitelyNonNullableTypesKt.containsIncorrectExplicitDefinitelyNonNullableType(upperBoundType)) {
//            trace.report(INCORRECT_LEFT_COMPONENT_OF_INTERSECTION.on(upperBound));
//        }
        }

        private fun checkNoGenericBoundsOnTypeAliasParameters(typeAlias: CjTypeAlias, trace: BindingTrace) {
            for (typeParameter in typeAlias.typeParameters) {
                val bound = typeParameter.extendsBound
                if (bound != null) {
                    trace.report(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED.on(bound))
                }
            }
        }

        private fun resolveSuperTypeListEntries(
            extensibleScope: LexicalScope,
            delegationSpecifiers: List<CjSuperTypeListEntry>,
            resolver: TypeResolver,
            trace: BindingTrace,
            checkBounds: Boolean
        ): Collection<CangJieType> {
            if (delegationSpecifiers.isEmpty()) {
                return emptyList()
            }
            val result: MutableCollection<CangJieType> = Lists.newArrayList()
            for (delegationSpecifier in delegationSpecifiers) {
                val typeReference = delegationSpecifier.typeReference
                if (typeReference != null) {
                    val supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds)
                    if (supertype.isDynamic()) {
                        trace.report(DYNAMIC_SUPERTYPE.on(typeReference))
                    } else {
                        result.add(supertype)
                        val bareSuperType =
                            checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.typeElement)
                        checkProjectionsInImmediateArguments(trace, bareSuperType, supertype)
                    }
                } else {
                    result.add(createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, delegationSpecifier.text))
                }
            }
            return result
        }

        private fun checkProjectionsInImmediateArguments(
            trace: BindingTrace,
            typeElement: CjTypeElement?,
            type: CangJieType
        ) {
//        if (typeElement == null) return;

//        boolean hasProjectionsInWrittenArguments = false;
//        处理类型投影
//        if (typeElement instanceof CjUserType userType) {
//            List<CjTypeProjection> typeArguments = userType.getTypeArguments();
//            for (CjTypeProjection typeArgument : typeArguments) {
//                if (typeArgument.getProjectionKind() != CjProjectionKind.NONE) {
//                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
//                    hasProjectionsInWrittenArguments = true;
//                }
//            }
//        }

            // If we have an abbreviated type (written with a type alias), it still can contain type projections in top-level arguments.
//        if (!CangJieTypeKt.isError(type) && SpecialTypesKt.getAbbreviatedType(type) != null && !hasProjectionsInWrittenArguments) {
            // Only interface inheritance should be checked here.
            // Corresponding check for classes is performed for type alias constructor calls in CandidateResolver.
//            if (TypeUtilKt.isInterface(type) && TypeUtilKt.containsTypeProjectionsInTopLevelArguments(type)) {
//                trace.report(EXPANDED_TYPE_CANNOT_BE_INHERITED.on(typeElement, type));
//            }
//        }
        }

        private fun checkNullableSupertypeAndStripQuestionMarks(
            trace: BindingTrace,
            typeElement: CjTypeElement?
        ): CjTypeElement? {
//        while (typeElement instanceof CjNullableType) {
//            CjNullableType nullableType = (CjNullableType) typeElement;
//            typeElement = nullableType.getInnerType();
//            // report only for innermost '?', the rest gets a 'redundant' warning
//            if (!(typeElement instanceof CjNullableType) && typeElement != null) {
//                trace.report(NULLABLE_SUPERTYPE.on(nullableType));
//            }
//        }
            return typeElement
        }

        fun transformAnonymousTypeIfNeeded(
            descriptor: DeclarationDescriptorWithVisibility,
            declaration: CjDeclaration,
            type: CangJieType,
            trace: BindingTrace,
            anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>,
            languageVersionSettings: LanguageVersionSettings
        ): CangJieType {
            for (transformer in anonymousTypeTransformers) {
                val transformedType = transformer.transformAnonymousType(descriptor, type)
                if (transformedType != null) {
                    return transformedType
                }
            }

            val classifier = type.constructor.declarationDescriptor
            if (classifier == null || !isAnonymousObject(classifier) || isLocal(descriptor)) {
                return type
            }

            val isPrivate = DescriptorVisibilities.isPrivate(descriptor.visibility)

            if (!isPrivate) {
//            if(type instanceof  BasicType) return type;
                if (type.constructor.supertypes.size == 1) {
                    val approximatingSuperType = type.constructor.supertypes.iterator().next()
                    val substitutedSuperType: CangJieType
                    val memberScope = type.memberScope

                    substitutedSuperType = if (memberScope is SubstitutingScope) {
                        memberScope.substitute(approximatingSuperType)
                    } else {
                        approximatingSuperType
                    }

                    //                UnwrappedType unwrapped = type.unwrap();
//                boolean lowerNullable = FlexibleTypesKt.lowerIfFlexible(unwrapped).isMarkedNullable();
//                boolean upperNullable = FlexibleTypesKt.upperIfFlexible(unwrapped).isMarkedNullable();
//                if (languageVersionSettings.supportsFeature(LanguageFeature.KeepNullabilityWhenApproximatingLocalType)) {
//                    if (lowerNullable != upperNullable) {
//                        return CangJieTypeFactory.flexibleType(
//                                FlexibleTypesKt.lowerIfFlexible(substitutedSuperType),
//                                FlexibleTypesKt.upperIfFlexible(substitutedSuperType).makeNullableAsSpecified(true));
//                    }
//                    return TypeUtils.makeOptionalIfNeeded(substitutedSuperType, upperNullable);
//                } else if (upperNullable) {
//                    if (lowerNullable) {
//                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE.on(declaration, substitutedSuperType));
//                    } else {
//                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE.on(declaration, substitutedSuperType));
//                    }
//                }
                    return substitutedSuperType
                } else {
                    trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.constructor.supertypes))
                }
            }

            return type
        }
    }
}
