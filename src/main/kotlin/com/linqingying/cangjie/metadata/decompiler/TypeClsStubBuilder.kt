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

package com.linqingying.cangjie.metadata.decompiler


import com.google.protobuf.MessageLite
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.isBuiltinFunctionClass

import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.ProtoBuf.Type
import com.linqingying.cangjie.metadata.ProtoBuf.Type.Argument.Projection
import com.linqingying.cangjie.metadata.ProtoBuf.TypeParameter.Variance
import com.linqingying.cangjie.metadata.deserialization.*
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name

import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.impl.*
import com.linqingying.cangjie.resolve.constants.StringValue
import com.linqingying.cangjie.serialization.deserialization.*
import com.linqingying.cangjie.utils.doNothing

// TODO: see DescriptorRendererOptions.excludedTypeAnnotationClasses for decompiler
private val ANNOTATIONS_NOT_LOADED_FOR_TYPES = setOf(StandardNames.FqNames.parameterName)
private val CONTEXT_FUNCTION_TYPE_PARAMS_ARGUMENT_NAME = Name.identifier("count")

const val COMPILED_DEFAULT_PARAMETER_VALUE = "COMPILED_CODE"

class TypeClsStubBuilder(private val c: ClsStubBuilderContext) {
    fun createTypeReferenceStub(
        parent: StubElement<out PsiElement>,
        type: Type,
        additionalAnnotations: () -> List<AnnotationWithTarget> = { emptyList() },
        loadTypeAnnotations: (Type) -> List<AnnotationWithArgs> = {
            c.components.annotationLoader.loadTypeAnnotations(
                it,
                c.nameResolver
            )
        }
    ) {
        val typeReference = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)

        val allAnnotationsInType = loadTypeAnnotations(type)
        val annotations = allAnnotationsInType.filterNot {
            val isTopLevelClass = !it.classId.isNestedClass
            isTopLevelClass && it.classId.asSingleFqName() in ANNOTATIONS_NOT_LOADED_FOR_TYPES
        }

        val allAnnotations = additionalAnnotations() + annotations.map { AnnotationWithTarget(it, null) }

        when {
            type.hasClassName() || type.hasTypeAliasName() ->
                createClassReferenceTypeStub(typeReference, type, allAnnotations)

            type.hasTypeParameter() ->
                createTypeParameterStub(typeReference, type, c.typeParameters[type.typeParameter], allAnnotations)

            type.hasTypeParameterName() ->
                createTypeParameterStub(
                    typeReference,
                    type,
                    c.nameResolver.getName(type.typeParameterName),
                    allAnnotations
                )

            else -> {
                doNothing()
            }
        }
    }

    private fun nullableTypeParent(parent: CangJieStubBaseImpl<*>, type: Type): CangJieStubBaseImpl<*> =
        if (type.nullable)
            CangJiePlaceHolderStubImpl<CjOptionType>(parent, CjStubElementTypes.OPTIONAL_TYPE)
        else
            parent

    private fun createTypeParameterStub(
        parent: CangJieStubBaseImpl<*>,
        type: Type,
        name: Name,
        annotations: List<AnnotationWithTarget>
    ) {
        createTypeAnnotationStubs(parent, type, annotations)
        val upperBoundType = if (type.hasFlexibleTypeCapabilitiesId()) {
            createCangJieTypeBean(type.flexibleUpperBound(c.typeTable)!!)
        } else null

        val typeParameterClassId = ClassId.topLevel(FqName.topLevel(name))
//        if (Flags.DEFINITELY_NOT_NULL_TYPE.get(type.flags)) {
//            createDefinitelyNotNullTypeStub(parent, typeParameterClassId, upperBoundType)
//        } else {
        val nullableParentWrapper = nullableTypeParent(parent, type)
        createStubForTypeName(typeParameterClassId, nullableParentWrapper, upperBoundFun = { upperBoundType })
//        }
    }
//
//    private fun createDefinitelyNotNullTypeStub(
//        parent: CangJieStubBaseImpl<*>,
//        classId: ClassId,
//        upperBoundType: CangJieTypeBean?
//    ) {
//        val intersectionType =
//            CangJiePlaceHolderStubImpl<CjIntersectionType>(parent, CjStubElementTypes.INTERSECTION_TYPE)
//        val leftReference =
//            CangJiePlaceHolderStubImpl<CjTypeReference>(intersectionType, CjStubElementTypes.TYPE_REFERENCE)
//        createStubForTypeName(classId, leftReference, upperBoundFun = { upperBoundType })
//        val rightReference =
//            CangJiePlaceHolderStubImpl<CjTypeReference>(intersectionType, CjStubElementTypes.TYPE_REFERENCE)
//        val userType = CangJieUserTypeStubImpl(rightReference)
//        CangJieNameReferenceExpressionStubImpl(userType, StandardNames.FqNames.anyUFqName.shortName().ref(), true)
//    }

    private fun createClassReferenceTypeStub(
        parent: CangJieStubBaseImpl<*>,
        type: Type,
        annotations: List<AnnotationWithTarget>,
    ) {
//        if (type.hasFlexibleTypeCapabilitiesId()) {
//            val id = c.nameResolver.getString(type.flexibleTypeCapabilitiesId)
//
//            if (id == DYNAMIC_TYPE_DESERIALIZER_ID) {
//                CangJiePlaceHolderStubImpl<CjDynamicType>(nullableTypeParent(parent, type), CjStubElementTypes.DYNAMIC_TYPE)
//                return
//            }
//        }

        assert(type.hasClassName() || type.hasTypeAliasName()) {
            "Class reference stub must have either class or type alias name"
        }

        val classId = c.nameResolver.getClassId(if (type.hasClassName()) type.className else type.typeAliasName)
        val abbreviatedType = type.abbreviatedType(c.typeTable)?.let { createCangJieClassTypeBean(it) }

        val shouldBuildAsFunctionType =
            isBuiltinFunctionClass(classId)
        if (shouldBuildAsFunctionType) {
            val (extensionAnnotations, notExtensionAnnotations) = annotations.partition {
                it.annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.extensionFunctionType
            }

            val (contextReceiverAnnotations, otherAnnotations) = notExtensionAnnotations.partition {
                it.annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.contextFunctionTypeParams
            }

            val isExtension = extensionAnnotations.isNotEmpty()
            val isSuspend = Flags.SUSPEND_TYPE.get(type.flags)

            val nullableWrapper = if (isSuspend) {
                val wrapper = nullableTypeParent(parent, type)
                createTypeAnnotationStubs(wrapper, type, otherAnnotations)
                wrapper
            } else {
                createTypeAnnotationStubs(parent, type, otherAnnotations)
                nullableTypeParent(parent, type)
            }

            val numContextReceivers = if (contextReceiverAnnotations.isEmpty()) {
                0
            } else {
                annotations.map { it.annotationWithArgs }.find { annotationWithArgs ->
                    annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.contextFunctionTypeParams
                }?.args?.get(CONTEXT_FUNCTION_TYPE_PARAMS_ARGUMENT_NAME)?.value as? Int
                    ?: error("Error: can't get type parameter count from ${StandardNames.FqNames.contextFunctionTypeParams}")
            }
            createFunctionTypeStub(nullableWrapper, type, isExtension, isSuspend, numContextReceivers, abbreviatedType)

            return
        }

        createTypeAnnotationStubs(parent, type, annotations)

        val outerTypeChain = generateSequence(type) { it.outerType(c.typeTable) }.toList()

        createStubForTypeName(
            classId,
            nullableTypeParent(parent, type),
            abbreviatedType = abbreviatedType,
            upperBoundFun = { level ->
                if (level == 0) createCangJieTypeBean(type.flexibleUpperBound(c.typeTable))
                else createCangJieTypeBean(outerTypeChain.getOrNull(level)?.flexibleUpperBound(c.typeTable))
            },
            bindTypeArguments = { userTypeStub, index ->
                outerTypeChain.getOrNull(index)?.let { createTypeArgumentListStub(userTypeStub, it.argumentList) }
            },
        )
    }

    fun createCangJieTypeBean(type: Type?): CangJieTypeBean? {
        if (type == null) return null
        val definitelyNotNull = Flags.DEFINITELY_NOT_NULL_TYPE.get(type.flags)
        val lowerBound = when {
            type.hasTypeParameter() ->
                CangJieTypeParameterTypeBean(
                    c.typeParameters[type.typeParameter].asString(),
                    type.nullable,
                    definitelyNotNull
                )

            type.hasTypeParameterName() ->
                CangJieTypeParameterTypeBean(
                    c.nameResolver.getString(type.typeParameterName),
                    type.nullable,
                    definitelyNotNull
                )

            else -> createCangJieClassTypeBean(type)
        }
        val upperBoundBean = createCangJieTypeBean(type.flexibleUpperBound(c.typeTable))
        return if (upperBoundBean != null) {
            CangJieFlexibleTypeBean(lowerBound, upperBoundBean)
        } else lowerBound
    }

    private fun createCangJieClassTypeBean(type: Type): CangJieClassTypeBean {
        val classId = c.nameResolver.getClassId(if (type.hasClassName()) type.className else type.typeAliasName)

        val arguments = type.argumentList.map { argument ->
            val kind = argument.projection.toProjectionKind()
            CangJieTypeArgumentBean(
                kind,
                createCangJieTypeBean(argument.type(c.typeTable))
            )
        }

        val abbreviatedType = type.abbreviatedType(c.typeTable)?.let { createCangJieClassTypeBean(it) }

        return CangJieClassTypeBean(classId, arguments, type.nullable, abbreviatedType)
    }

    private fun createTypeAnnotationStubs(
        parent: CangJieStubBaseImpl<*>,
        type: Type,
        annotations: List<AnnotationWithTarget>
    ) {
        val typeModifiers = getTypeModifiersAsWritten(type)
        if (annotations.isEmpty() && typeModifiers.isEmpty()) return
        val typeModifiersMask = ModifierMaskUtils.computeMask { it in typeModifiers }
        val modifiersList = CangJieModifierListStubImpl(parent, typeModifiersMask, CjStubElementTypes.MODIFIER_LIST)
        createTargetedAnnotationStubs(annotations, modifiersList)
    }

    private fun getTypeModifiersAsWritten(type: Type): Set<CjModifierKeywordToken> {
        if (!type.hasClassName() && !type.hasTypeAliasName()) return emptySet()

        val result = hashSetOf<CjModifierKeywordToken>()



        return result
    }

    private fun createTypeArgumentListStub(typeStub: CangJieUserTypeStub, typeArgumentProtoList: List<Type.Argument>) {
        if (typeArgumentProtoList.isEmpty()) {
            return
        }
        val typeArgumentsListStub =
            CangJiePlaceHolderStubImpl<CjTypeArgumentList>(typeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
        typeArgumentProtoList.forEach { typeArgumentProto ->
            val projectionKind = typeArgumentProto.projection.toProjectionKind()
            val typeProjection = CangJieTypeProjectionStubImpl(typeArgumentsListStub, projectionKind.ordinal)

            val modifierKeywordToken = projectionKind.token as? CjModifierKeywordToken
            createModifierListStub(typeProjection, listOfNotNull(modifierKeywordToken))
            createTypeReferenceStub(typeProjection, typeArgumentProto.type(c.typeTable)!!)

        }
    }

    private fun Projection.toProjectionKind() = when (this) {

        Projection.INV -> CjProjectionKind.NONE

    }

    private fun createFunctionTypeStub(
        parent: StubElement<out PsiElement>,
        type: Type,
        isExtensionFunctionType: Boolean,
        isSuspend: Boolean,
        numContextReceivers: Int,
        abbreviatedType: CangJieClassTypeBean?,
    ) {
        val typeArgumentList = type.argumentList
        val functionType = CangJieFunctionTypeStubImpl(parent, abbreviatedType)
        var processedTypes = 0

        if (numContextReceivers != 0) {
            ContextReceiversListStubBuilder(c).createContextReceiverStubs(
                functionType,
                typeArgumentList.subList(
                    processedTypes,
                    processedTypes + numContextReceivers
                ).map { it.type(c.typeTable)!! })
            processedTypes += numContextReceivers
        }

        if (isExtensionFunctionType) {
            val functionTypeReceiverStub =
                CangJiePlaceHolderStubImpl<CjFunctionTypeReceiver>(
                    functionType,
                    CjStubElementTypes.FUNCTION_TYPE_RECEIVER
                )
            val receiverTypeProto = typeArgumentList[processedTypes].type(c.typeTable)!!
            createTypeReferenceStub(functionTypeReceiverStub, receiverTypeProto)
            processedTypes++
        }

        val parameterList =
            CangJiePlaceHolderStubImpl<CjParameterList>(functionType, CjStubElementTypes.VALUE_PARAMETER_LIST)
        val typeArgumentsWithoutReceiverAndReturnType =
            typeArgumentList.subList(processedTypes, typeArgumentList.size - 1)
        var suspendParameterType: Type? = null

        for ((index, argument) in typeArgumentsWithoutReceiverAndReturnType.withIndex()) {
            val parameterType = argument.type(c.typeTable)!!
            if (isSuspend && index == typeArgumentsWithoutReceiverAndReturnType.size - 1) {
                if (parameterType.hasClassName() && parameterType.argumentCount == 1) {
                    val classId = c.nameResolver.getClassId(parameterType.className)
                    val fqName = classId.asSingleFqName()
                    assert(
                        fqName == FqName("kotlin.coroutines.Continuation") ||
                                fqName == FqName("kotlin.coroutines.experimental.Continuation")
                    ) {
                        "Last parameter type of suspend function must be Continuation, but it is $fqName"
                    }
                    suspendParameterType = parameterType
                    continue
                }
            }
            val annotations = c.components.annotationLoader.loadTypeAnnotations(parameterType, c.nameResolver)

            fun getFunctionTypeParameterName(annotations: List<AnnotationWithArgs>): String? {
                for (annotationWithArgs in annotations) {
                    if (annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.parameterName) {
                        return (annotationWithArgs.args.values.firstOrNull() as? StringValue)?.value
                    }
                }
                return null
            }

            val parameter = CangJieParameterStubImpl(
                parameterList,
                fqName = null,
                name = null,
                isMutable = false,
                hasValOrVar = false,
                hasDefaultValue = false,
                functionTypeParameterName = getFunctionTypeParameterName(annotations)
            )
            createTypeReferenceStub(parameter, parameterType, loadTypeAnnotations = { annotations })
        }


        if (suspendParameterType == null) {
            val returnType = typeArgumentList.last().type(c.typeTable)!!
            createTypeReferenceStub(functionType, returnType)
        } else {
            val continuationArgumentType = suspendParameterType.getArgument(0).type(c.typeTable)!!
            createTypeReferenceStub(functionType, continuationArgumentType)
        }
    }

    fun createValueParameterListStub(
        parent: StubElement<out PsiElement>,
        callableProto: MessageLite,
        parameters: List<ProtoBuf.ValueParameter>,
        container: ProtoContainer,
        callableKind: AnnotatedCallableKind = callableProto.annotatedCallableKind
    ) {
        val parameterListStub =
            CangJiePlaceHolderStubImpl<CjParameterList>(parent, CjStubElementTypes.VALUE_PARAMETER_LIST)
        for ((index, valueParameterProto) in parameters.withIndex()) {
            val parameterName = computeParameterName(c.nameResolver.getName(valueParameterProto.name))
            val hasDefaultValue = Flags.DECLARES_DEFAULT_VALUE.get(valueParameterProto.flags)
            val parameterStub = CangJieParameterStubImpl(
                parameterListStub,
                name = parameterName.ref(),
                fqName = null,
                hasDefaultValue = hasDefaultValue,
                hasValOrVar = false,
                isMutable = false
            )
            val varargElementType = valueParameterProto.varargElementType(c.typeTable)
            val typeProto = varargElementType ?: valueParameterProto.type(c.typeTable)
            val modifiers = arrayListOf<CjModifierKeywordToken>()

            if (varargElementType != null) {
                modifiers.add(CjTokens.VARARG_KEYWORD)
            }


            val modifierList = createModifierListStub(parameterStub, modifiers)

            if (Flags.HAS_ANNOTATIONS.get(valueParameterProto.flags)) {
                val parameterAnnotations = c.components.annotationLoader.loadValueParameterAnnotations(
                    container, callableProto, callableKind, index, valueParameterProto
                )
                if (parameterAnnotations.isNotEmpty()) {
                    createAnnotationStubs(
                        parameterAnnotations,
                        modifierList ?: createEmptyModifierListStub(parameterStub)
                    )
                }
            }

            createTypeReferenceStub(parameterStub, typeProto)
            if (hasDefaultValue) {
                CangJieNameReferenceExpressionStubImpl(
                    parameterStub,
                    StringRef.fromString(COMPILED_DEFAULT_PARAMETER_VALUE)
                )
            }
        }
    }

    fun createTypeParameterListStub(
        parent: StubElement<out PsiElement>,
        typeParameterProtoList: List<ProtoBuf.TypeParameter>
    ): List<Pair<Name, Type>> {
        if (typeParameterProtoList.isEmpty()) return listOf()

        val typeParameterListStub =
            CangJiePlaceHolderStubImpl<CjTypeParameterList>(parent, CjStubElementTypes.TYPE_PARAMETER_LIST)
        val protosForTypeConstraintList = arrayListOf<Pair<Name, Type>>()
        for (proto in typeParameterProtoList) {
            val name = c.nameResolver.getName(proto.name)
            val typeParameterStub = CangJieTypeParameterStubImpl(
                typeParameterListStub,
                name = name.ref(),

                )
            createTypeParameterModifierListStub(typeParameterStub, proto)
            val upperBoundProtos = proto.upperBounds(c.typeTable)
            if (upperBoundProtos.isNotEmpty()) {
                val upperBound = upperBoundProtos.first()
                if (!upperBound.isDefaultUpperBound()) {
                    createTypeReferenceStub(typeParameterStub, upperBound)
                }
                protosForTypeConstraintList.addAll(upperBoundProtos.drop(1).map { Pair(name, it) })
            }
        }
        return protosForTypeConstraintList
    }

    fun createTypeConstraintListStub(
        parent: StubElement<out PsiElement>,
        protosForTypeConstraintList: List<Pair<Name, Type>>
    ) {
        if (protosForTypeConstraintList.isEmpty()) {
            return
        }
        val typeConstraintListStub =
            CangJiePlaceHolderStubImpl<CjTypeConstraintList>(parent, CjStubElementTypes.TYPE_CONSTRAINT_LIST)
        for ((name, type) in protosForTypeConstraintList) {
            val typeConstraintStub =
                CangJiePlaceHolderStubImpl<CjTypeConstraint>(typeConstraintListStub, CjStubElementTypes.TYPE_CONSTRAINT)
            CangJieNameReferenceExpressionStubImpl(typeConstraintStub, name.ref())
            createTypeReferenceStub(typeConstraintStub, type)
        }
    }

    private fun createTypeParameterModifierListStub(
        typeParameterStub: CangJieTypeParameterStubImpl,
        typeParameterProto: ProtoBuf.TypeParameter
    ) {
        val modifiers = ArrayList<CjModifierKeywordToken>()
        when (typeParameterProto.variance) {

            Variance.INV -> { /* do nothing */
            }

            null -> { /* do nothing */
            }
        }


        val modifierList = createModifierListStub(typeParameterStub, modifiers)

        val annotations = c.components.annotationLoader.loadTypeParameterAnnotations(typeParameterProto, c.nameResolver)
        if (annotations.isNotEmpty()) {
            createAnnotationStubs(
                annotations,
                modifierList ?: createEmptyModifierListStub(typeParameterStub)
            )
        }
    }

    private fun Type.isDefaultUpperBound(): Boolean {
        return this.hasClassName() &&
                c.nameResolver.getClassId(className)
                    .let { StandardNames.FqNames.anyUFqName == it.asSingleFqName().toUnsafe() } &&
                this.nullable
    }
}
