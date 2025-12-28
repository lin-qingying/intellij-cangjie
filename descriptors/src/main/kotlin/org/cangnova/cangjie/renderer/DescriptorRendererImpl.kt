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

package org.cangnova.cangjie.renderer

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotated
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorString
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.annotationClass
import org.cangnova.cangjie.resolve.constants.ArrayValue
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.resolve.declaresOrInheritsDefaultValue
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly

open class DescriptorRendererImpl(
    val options: DescriptorRendererOptionsImpl
) : DescriptorRenderer(), DescriptorRendererOptions by options/* this gives access to options without qualifier */ {
    init {
        assert(options.isLocked)
    }


    companion object {
        fun withOptions(changeOptions: DescriptorRendererOptionsImpl.() -> Unit): DescriptorRendererImpl {
            val options = DescriptorRendererOptionsImpl()
            options.changeOptions()
            options.lock()
            return DescriptorRendererImpl(options)
        }

    }

    private val functionTypeAnnotationsRenderer: DescriptorRendererImpl by lazy {
        withOptions {
            // CangJie does not have extension function types or context function types
            /*     excludedTypeAnnotationClasses += */emptyList<FqName>()
        } as DescriptorRendererImpl
    }

    /* FORMATTING */
    fun renderKeyword(keyword: String): String = when (textFormat) {
        RenderingFormat.PLAIN -> keyword
        RenderingFormat.HTML -> if (boldOnlyForNamesInHtml) keyword else "<b>$keyword</b>"
    }

    private fun renderError(keyword: String): String = when (textFormat) {
        RenderingFormat.PLAIN -> keyword
        RenderingFormat.HTML -> "<font color=red><b>$keyword</b></font>"
    }

    private fun escape(string: String) = textFormat.escape(string)

    private fun lt() = escape("<")
    private fun gt() = escape(">")

    private fun arrow(): String = when (textFormat) {
        RenderingFormat.PLAIN -> escape("->")
        RenderingFormat.HTML -> "&rarr;"
    }

    override fun renderMessage(message: String): String = when (textFormat) {
        RenderingFormat.PLAIN -> message
        RenderingFormat.HTML -> "<i>$message</i>"
    }

    /* NAMES RENDERING */
    override fun renderName(name: Name, rootRenderedElement: Boolean): String {
        val escaped = escape(name.render())
        return if (boldOnlyForNamesInHtml && textFormat == RenderingFormat.HTML && rootRenderedElement) {
            "<b>$escaped</b>"
        } else
            escaped
    }

    private fun renderName(
        descriptor: DeclarationDescriptor,
        builder: StringBuilder,
        rootRenderedElement: Boolean,
        isEnd: Boolean = false
    ) {
        if (isEnd) {
            builder.append("~")

        }
        if (descriptor is FunctionDescriptor && descriptor.isOperator) {
            builder.append(descriptor.name.asOperatorString())

        } else {
            builder.append(renderName(descriptor.name, rootRenderedElement))

        }
    }


    override fun renderFqName(fqName: FqNameUnsafe) = renderFqName(fqName.pathSegments())

    private fun renderFqName(pathSegments: List<Name>) =
        escape(org.cangnova.cangjie.renderer.renderFqName(pathSegments))

    override fun renderClassifierName(cclass: ClassifierDescriptor): String = if (ErrorUtils.isError(cclass)) {
        cclass.typeConstructor.toString()
    } else
        classifierNamePolicy.renderClassifier(cclass, this)

    /* TYPES RENDERING */
    override fun renderType(type: CangJieType): String = buildString {
        renderNormalizedType(typeNormalizer(type))
    }

    private fun StringBuilder.renderNormalizedType(type: CangJieType) {
        val abbreviated = type.unwrap() as? AbbreviatedType
        if (abbreviated != null) {
            if (renderTypeExpansions) {
                renderNormalizedTypeAsIs(abbreviated.expandedType)
                if (renderAbbreviatedTypeComments) {
                    renderAbbreviatedTypeComment(abbreviated)
                }
            } else {
                // TODO nullability is lost for abbreviated type?
                renderNormalizedTypeAsIs(abbreviated.abbreviation)
                if (renderUnabbreviatedType) {
                    renderExpandedTypeComment(abbreviated)
                }
            }
            return
        }

        renderNormalizedTypeAsIs(type)
    }

    private fun StringBuilder.renderAbbreviatedTypeComment(abbreviated: AbbreviatedType) {
        renderInBlockComment {
            append("from: ")
            renderNormalizedTypeAsIs(abbreviated.abbreviation)
        }
    }

    private fun StringBuilder.renderExpandedTypeComment(abbreviated: AbbreviatedType) {
        renderInBlockComment {
            append("= ")
            renderNormalizedTypeAsIs(abbreviated.expandedType)
        }
    }

    private inline fun StringBuilder.renderInBlockComment(renderBody: () -> Unit) {
        if (textFormat == RenderingFormat.HTML) {
            append("<font color=\"808080\"><i>")
        }
        append(" /* ")
        renderBody()
        append(" */")
        if (textFormat == RenderingFormat.HTML) {
            append("</i></font>")
        }
    }

    private fun StringBuilder.renderNormalizedTypeAsIs(type: CangJieType) {
        if (type is WrappedType && debugMode && !type.isComputed()) {
            append("<Not computed yet>")
            return
        }
        when (val unwrappedType = type.unwrap()) {

            is FlexibleType -> append(unwrappedType.render(this@DescriptorRendererImpl, this@DescriptorRendererImpl))
            is BasicType -> renderBasicType(unwrappedType)

            is SimpleType -> renderSimpleType(unwrappedType)

        }
    }

    private fun StringBuilder.renderBasicType(basicType: BasicType) {
        append(basicType.typeName)
        return
    }

    private fun StringBuilder.renderSimpleType(type: SimpleType) {
        if (type == CANNOT_INFER_FUNCTION_PARAM_TYPE) {
            append("???")
            return
        }
        if (ErrorUtils.isUninferredTypeVariable(type)) {
            if (uninferredTypeParameterAsName) {
                append(renderError((type.constructor as ErrorTypeConstructor).getParam(0)))
            } else {
                append("???")
            }
            return
        }

        if (type.isError) {
            renderDefaultType(type)
            return
        }
        if (shouldRenderAsPrettyFunctionType(type)) {
            renderFunctionType(type)
        } else if (shouldRenderAsPrettyTupleType(type)) {
            renderTupleType(type)
        } else {
            renderDefaultType(type)
        }
    }

    private fun shouldRenderAsPrettyTupleType(type: CangJieType): Boolean {
        return type.isBuiltinTupleType

    }

    private fun shouldRenderAsPrettyFunctionType(type: CangJieType): Boolean {
        return type.isBuiltinFunctionalType /*&& type.arguments.none { it.isStarProjection }*/

    }

    override fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String {
        if (typeStringsDifferOnlyInNullability(lowerRendered, upperRendered)) {
            if (upperRendered.startsWith("(")) {
                // the case of complex type, e.g. (() -> Unit)?
                return "($lowerRendered)!"
            }
            return "$lowerRendered!"
        }

        val cangjieCollectionsPrefix = ""
        val mutablePrefix = "Mutable"
        val simpleCollection = replacePrefixesInTypeRepresentations(
            lowerRendered,
            cangjieCollectionsPrefix + mutablePrefix,
            upperRendered,
            cangjieCollectionsPrefix,
            "$cangjieCollectionsPrefix($mutablePrefix)"
        )
        if (simpleCollection != null) return simpleCollection
        val mutableEntry = replacePrefixesInTypeRepresentations(
            lowerRendered,
            cangjieCollectionsPrefix + "MutableMap.MutableEntry",
            upperRendered,
            cangjieCollectionsPrefix + "Map.Entry",
            "$cangjieCollectionsPrefix(Mutable)Map.(Mutable)Entry"
        )
        if (mutableEntry != null) return mutableEntry

        val cangjiePrefix = ""
        val array = replacePrefixesInTypeRepresentations(
            lowerRendered,
            cangjiePrefix + escape("Array<"),
            upperRendered,
            cangjiePrefix + escape("Array<out "),
            cangjiePrefix + escape("Array<(out) ")
        )
        if (array != null) return array

        return "($lowerRendered..$upperRendered)"
    }

    override fun renderTypeArguments(typeArguments: List<TypeProjection>, other: (StringBuilder) -> Unit): String =
        if (typeArguments.isEmpty()) ""
        else buildString {
            append(lt())
            this.appendTypeProjections(typeArguments)
            other(this)
            append(gt())
        }

    private fun StringBuilder.renderDefaultType(type: CangJieType) {
        this.renderAnnotations(type)

        val originalTypeOfDefNotNullType = (type as? DefinitelyNonOptionType)?.original

        if (type is OptionType) {
            append("?")
        }

        when {
            type is OptionType -> renderSimpleType(type.innerType as SimpleType)

            type.isError -> {
                if (isUnresolvedType(type) && presentableUnresolvedTypes) {
                    append(renderError(ErrorUtils.unresolvedTypeAsItIs(type)))
                } else {
                    if (type is ErrorType && !informativeErrorType) {
                        append(type.debugMessage)
                    } else {
                        append(type.constructor.toString()) // Debug name of an error type is more informative
                    }

                    append(renderTypeArguments(type.arguments))
                }
            }

            type is StubTypeForBuilderInference ->
                append(type.originalTypeVariable.toString())

            originalTypeOfDefNotNullType is StubTypeForBuilderInference ->
                append(originalTypeOfDefNotNullType.originalTypeVariable.toString())

            else -> renderTypeConstructorAndArguments(type)
        }



        if (type.isDefinitelyNonOptionType) {
            append(" & Any")
        }
    }

    private fun StringBuilder.renderTypeConstructorAndArguments(
        type: CangJieType,
        typeConstructor: TypeConstructor = type.constructor
    ) {

        append(renderTypeConstructor(typeConstructor))
        append(renderTypeArguments(type.arguments))

    }

    override fun renderTypeConstructor(typeConstructor: TypeConstructor): String =
        when (val cd = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor, is ClassifierDescriptorWithTypeConstructor, is TypeAliasDescriptor -> renderClassifierName(
                cd
            )

            null -> {
                if (typeConstructor is IntersectionTypeConstructor) {
                    typeConstructor.makeDebugNameForIntersectionType { if (it is StubTypeForBuilderInference) it.originalTypeVariable else it }
                } else typeConstructor.toString()
            }

            else -> error("Unexpected classifier: " + cd::class.java)
        }

    override fun renderTypeProjection(typeProjection: TypeProjection) = buildString {
        appendTypeProjections(listOf(typeProjection))
    }

    private fun StringBuilder.appendTypeProjections(typeProjections: List<TypeProjection>) {
        typeProjections.joinTo(this, ", ") {

            val type = renderType(it.type)
            if (it.projectionKind == Variance.INVARIANT) type else "${it.projectionKind} $type"

        }
    }

    private fun StringBuilder.renderTupleType(type: CangJieType) {
        append("(")
        appendTypeProjections(type.arguments)
        append(")")
    }

    private fun StringBuilder.renderFunctionType(type: CangJieType) {
        val lengthBefore = length
        // 仓颉不需要跳过扩展函数类型注解（因为没有这个特性）
        with(functionTypeAnnotationsRenderer) {
            renderAnnotations(type)
        }
        val hasAnnotations = length != lengthBefore

        val isOption = type.isOption

        // 仓颉没有扩展函数类型的接收器，简化括号逻辑
        val needParenthesis = isOption
        if (needParenthesis) {
            if (hasAnnotations) {
                assert(last().isWhitespace())
                if (get(lastIndex - 1) != ')') {
                    insert(lastIndex, "()")
                }
            }
            append("(")
        }

        append("(")

        // 仓颉没有扩展函数类型，直接渲染参数类型
        val parameterTypes = type.getValueParameterTypesFromFunctionType()
        for ((index, typeProjection) in parameterTypes.withIndex()) {
            if (index > 0) append(", ")

            val name =
                if (parameterNamesInFunctionalTypes) typeProjection.type.extractParameterNameFromFunctionTypeArgument() else null
            if (name != null) {
                append(renderName(name, false))
                append(": ")
            }

            append(renderTypeProjection(typeProjection))
        }

        append(") ").append(arrow()).append(" ")
        renderNormalizedType(type.getReturnTypeFromFunctionType())

        if (needParenthesis) append(")")

        if (isOption) append("?")
    }


    /* METHODS FOR ALL KINDS OF DESCRIPTORS */
    private fun StringBuilder.appendDefinedIn(descriptor: DeclarationDescriptor) {
        if ( descriptor is ModuleDescriptor || descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
            return
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration != null && containingDeclaration !is ModuleDescriptor) {
            append(" ").append(renderMessage("defined in")).append(" ")
            val fqName = DescriptorUtils.getFqName(containingDeclaration)
            append(if (fqName.isRoot) "root package" else renderFqName(fqName))

            if (withSourceFileForTopLevel &&
                containingDeclaration is PackageFragmentDescriptor &&
                descriptor is DeclarationDescriptorWithSource
            ) {
                descriptor.source.containingFile.name?.let { sourceFileName ->
                    append(" ").append(renderMessage("in file")).append(" ").append(sourceFileName)
                }
            }
        }
    }

    private fun StringBuilder.renderAnnotations(annotated: Annotated, target: AnnotationUseSiteTarget? = null) {
        if (DescriptorRendererModifier.ANNOTATIONS !in modifiers) return

        val excluded = excludedAnnotationClasses

        val annotationFilter = annotationFilter
        for (annotation in annotated.annotations) {
            if (annotation.fqName !in excluded

                && (annotationFilter == null || annotationFilter(annotation))
            ) {
                append(renderAnnotation(annotation, target))
                if (eachAnnotationOnNewLine) {
                    appendLine()
                } else {
                    append(" ")
                }
            }
        }
    }


    override fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget?): String {
        return buildString {
            append('@')
            if (target != null) {
                append(target.renderName + ":")
            }
            val annotationType = annotation.type
            append(renderType(annotationType))

            if (includeAnnotationArguments) {
                val arguments = renderAndSortAnnotationArguments(annotation)
                if (includeEmptyAnnotationArguments || arguments.isNotEmpty()) {
                    arguments.joinTo(this, ", ", "(", ")")
                }
            }

            if (verbose && (annotationType.isError
                        )
            ) {
                append(" /* annotation class not found */")
            }
        }
    }

    private fun renderAndSortAnnotationArguments(descriptor: AnnotationDescriptor): List<String> {
        val allValueArguments = descriptor.allValueArguments
        val classDescriptor = if (renderDefaultAnnotationArguments) descriptor.annotationClass else null
        val parameterDescriptorsWithDefaultValue = classDescriptor?.unsubstitutedPrimaryConstructor?.valueParameters
            ?.filter { it.declaresDefaultValue }
            ?.map { it.name }
            .orEmpty()
        val defaultList =
            parameterDescriptorsWithDefaultValue.filter { it !in allValueArguments }.map { "${it.asString()} = ..." }
        val argumentList = allValueArguments.entries
            .map { (name, value) ->
                "${name.asString()} = ${if (name !in parameterDescriptorsWithDefaultValue) renderConstant(value) else "..."}"
            }
        return (defaultList + argumentList).sorted()
    }

    private fun renderConstant(value: ConstantValue<*>): String? {
        options.propertyConstantRenderer?.let { return it.invoke(value) }
        return when (value) {
            is ArrayValue -> value.value.mapNotNull { renderConstant(it) }.joinToString(", ", "{", "}")

            else -> value.toString()
        }
    }

    private fun renderVisibility(visibility: DescriptorVisibility, builder: StringBuilder): Boolean {
        @Suppress("NAME_SHADOWING")

        var visibility = visibility
        if (DescriptorRendererModifier.VISIBILITY !in modifiers) return false
        if (normalizedVisibilities) {
            visibility = visibility.normalize()
        }
        if (!renderDefaultVisibility && visibility == DescriptorVisibilities.DEFAULT_VISIBILITY) return false
        builder.append(renderKeyword(visibility.internalDisplayName)).append(" ")
        return true
    }

    private fun renderModality(modality: Modality, builder: StringBuilder, defaultModality: Modality) {
        if (modality == Modality.FINAL) return
        if (!renderDefaultModality && modality == defaultModality) return
        renderModifier(builder, DescriptorRendererModifier.MODALITY in modifiers, modality.name.toLowerCaseAsciiOnly())
    }


    private fun MemberDescriptor.implicitModalityWithoutExtensions(): Modality {
        if (this is ClassDescriptor) {
            return if (kind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
        }
        val containingClassDescriptor = containingDeclaration as? ClassDescriptor ?: return Modality.FINAL
        if (this !is CallableMemberDescriptor) return Modality.FINAL
        if (this.overriddenDescriptors.isNotEmpty()) {
            if (containingClassDescriptor.modality != Modality.FINAL) return Modality.OPEN
        }
        return if (containingClassDescriptor.kind == ClassKind.INTERFACE && this.visibility != DescriptorVisibilities.PRIVATE) {
            if (this.modality == Modality.ABSTRACT) Modality.ABSTRACT else Modality.OPEN
        } else
            Modality.FINAL
    }

    private fun renderModalityForCallable(callable: CallableMemberDescriptor, builder: StringBuilder) {
        if (!DescriptorUtils.isTopLevelDeclaration(callable) || callable.modality != Modality.FINAL) {
            if (overrideRenderingPolicy == OverrideRenderingPolicy.RENDER_OVERRIDE && callable.modality == Modality.OPEN &&
                overridesSomething(callable)
            ) {
                return
            }
            renderModality(callable.modality, builder, callable.implicitModalityWithoutExtensions())
        }
    }

    private fun renderOverride(callableMember: CallableMemberDescriptor, builder: StringBuilder) {
        if (DescriptorRendererModifier.OVERRIDE !in modifiers) return
        if (overridesSomething(callableMember)) {
            if (overrideRenderingPolicy != OverrideRenderingPolicy.RENDER_OPEN) {
                if (callableMember.isStatic) {
                    renderModifier(builder, true, "redef")

                } else {
                    renderModifier(builder, true, "override")

                }
                if (verbose) {
                    builder.append("/*").append(callableMember.overriddenDescriptors.size).append("*/ ")
                }
            }
        }
    }

    private fun renderMemberKind(callableMember: CallableMemberDescriptor, builder: StringBuilder) {
        if (DescriptorRendererModifier.MEMBER_KIND !in modifiers) return
        if (verbose && callableMember.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            builder.append("/*").append(callableMember.kind.name.toLowerCaseAsciiOnly()).append("*/ ")
        }
    }

    private fun renderModifier(builder: StringBuilder, value: Boolean, modifier: String) {
        if (value) {
            builder.append(renderKeyword(modifier))
            builder.append(" ")
        }
    }


    private fun renderMemberModifiers(descriptor: MemberDescriptor, builder: StringBuilder) {

        renderModifier(builder, DescriptorRendererModifier.STATIC in modifiers && descriptor.isStatic, "static")

        renderModifier(builder, DescriptorRendererModifier.UNSAFE in modifiers && descriptor.isUnsafe, "unsafe")

    }

    private fun renderAdditionalModifiers(functionDescriptor: FunctionDescriptor, builder: StringBuilder) {
        val isOperator =
            functionDescriptor.isOperator



        renderModifier(builder, isOperator, "operator")
    }


    override fun render(declarationDescriptor: DeclarationDescriptor): String {
        return buildString {
            declarationDescriptor.accept(RenderDeclarationDescriptorVisitor(), this)

            if (withDefinedIn) {
                appendDefinedIn(declarationDescriptor)
            }
        }
    }


    /* TYPE PARAMETERS */
    private fun renderTypeParameter(typeParameter: TypeParameterDescriptor, builder: StringBuilder, topLevel: Boolean) {
        if (topLevel) {
            builder.append(lt())
        }

        if (verbose) {
            builder.append("/*").append(typeParameter.index).append("*/ ")
        }

//        renderModifier(builder, typeParameter.isReified, "reified")
        val variance = typeParameter.variance.label
        renderModifier(builder, variance.isNotEmpty(), variance)

        builder.renderAnnotations(typeParameter)

        renderName(typeParameter, builder, topLevel)


        if (topLevel) {
            builder.append(gt())
        }
    }

    private fun renderTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        builder: StringBuilder,
        withSpace: Boolean = false
    ) {
        if (withoutTypeParameters) return

        if (typeParameters.isNotEmpty()) {
            builder.append(lt())
            renderTypeParameterList(builder, typeParameters)
            builder.append(gt())

        }
    }

    private fun renderTypeParameterList(builder: StringBuilder, typeParameters: List<TypeParameterDescriptor>) {
        val iterator = typeParameters.iterator()
        while (iterator.hasNext()) {
            val typeParameterDescriptor = iterator.next()
            renderTypeParameter(typeParameterDescriptor, builder, false)
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
    }

    /* FUNCTIONS */
    private fun renderFunction(function: FunctionDescriptor, builder: StringBuilder) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
                builder.renderAnnotations(function)
                renderVisibility(function.visibility, builder)


                renderModalityForCallable(function, builder)

                if (includeAdditionalModifiers) {
                    renderMemberModifiers(function, builder)
                }


                renderOverride(function, builder)

                if (includeAdditionalModifiers) {
                    renderAdditionalModifiers(function, builder)
                }

                renderMemberKind(function, builder)

                if (verbose) {
                    if (function.isHiddenToOvercomeSignatureClash) {
                        builder.append("/*isHiddenToOvercomeSignatureClash*/ ")
                    }

                    if (function.isHiddenForResolutionEverywhereBesideSupercalls) {
                        builder.append("/*isHiddenForResolutionEverywhereBesideSupercalls*/ ")
                    }
                }
            }
            if (function.isConst) {
                builder.append("const ")
            }

            if (function.isStatic) {
                builder.append("static ")
            }

//            if (function is SimpleFunctionDescriptorForExtendImpl) {
//
//                renderTypeParameters(function.typeParametersForExtend, builder, true)
//                renderWhereSuffix(function.typeParametersForExtend, builder)
//
//            }
            if (function is MacroDescriptor) {
                builder.append(renderKeyword("macro"))
            } else {
                builder.append(renderKeyword("func"))
            }.append(" ")
        }

        renderName(function, builder, true)
        renderTypeParameters(function.typeParameters, builder, true)

        renderValueParameters(function.valueParameters, builder, false/*function.hasSynthesizedParameterNames()*/)


        val returnType = function.returnType
//        if (!withoutReturnType && (unitReturnType || (returnType == null /*|| !CangJieBuiltIns.isUnit(returnType)*/))) {
        builder.append(": ").append(if (returnType == null) "[NULL]" else renderType(returnType))
//        }

        renderWhereSuffix(function.typeParameters, builder)
    }


    private fun CangJieType.renderForReceiver(): String {
        var result = renderType(this)
        if ((shouldRenderAsPrettyFunctionType(this) && !TypeUtils.isOptionType(this)) || this is DefinitelyNonOptionType) {
            result = "($result)"
        }
        return result
    }



    private fun renderWhereSuffix(typeParameters: List<TypeParameterDescriptor>, builder: StringBuilder) {
        if (withoutTypeParameters) return

        val upperBoundStrings = ArrayList<String>(0)



        typeParameters.mapTo(upperBoundStrings) {
            renderName(it.name, false) + " <: " + it.upperBounds.joinToString(" & ") { uit ->
                renderType(uit)
            }
        }
//        for (typeParameter in typeParameters) {
//            typeParameter.upperBounds
//
//                .mapTo(upperBoundStrings) { renderName(typeParameter.name, false) + " <: " + renderType(it) }
//        }

        if (upperBoundStrings.isNotEmpty()) {
            builder.append(" ").append(renderKeyword("where")).append(" ")
            upperBoundStrings.joinTo(builder, ", ")
        }
    }

    override fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ) = buildString {
        renderValueParameters(parameters, this, synthesizedParameterNames)
    }

    private fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,

        builder: StringBuilder,
        synthesizedParameterNames: Boolean = false,
    ) {
        val includeNames = shouldRenderParameterNames(synthesizedParameterNames)
        val parameterCount = parameters.size
        valueParametersHandler.appendBeforeValueParameters(parameterCount, builder)
        for ((index, parameter) in parameters.withIndex()) {
            valueParametersHandler.appendBeforeValueParameter(parameter, index, parameterCount, builder)
            renderValueParameter(parameter, includeNames, builder, false)
            valueParametersHandler.appendAfterValueParameter(parameter, index, parameterCount, builder)
        }
        valueParametersHandler.appendAfterValueParameters(parameterCount, builder)
    }

    /**
     * 判断是否应该渲染参数名称
     * @param synthesizedParameterNames 参数名称是否为合成的
     * @return 是否应该渲染参数名称
     */
    private fun shouldRenderParameterNames(synthesizedParameterNames: Boolean): Boolean =
        when (parameterNameRenderingPolicy) {
            ParameterNameRenderingPolicy.ALL -> true // 渲染所有参数名称
            ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED -> !synthesizedParameterNames // 仅渲染非合成的参数名称
            ParameterNameRenderingPolicy.NONE -> false // 不渲染参数名称
        }

    /* VARIABLES */
    private fun renderValueParameter(
        valueParameter: ValueParameterDescriptor,
        includeName: Boolean,
        builder: StringBuilder,
        topLevel: Boolean
    ) {
        if (topLevel) {
            builder.append(renderKeyword("value-parameter")).append(" ")
        }

        if (verbose) {
            builder.append("/*").append(valueParameter.index).append("*/ ")
        }

        builder.renderAnnotations(valueParameter)
//        renderModifier(builder, valueParameter.isCrossinline, "crossinline")
//        renderModifier(builder, valueParameter.isNoinline, "noinline")

        val isPrimaryConstructor = renderPrimaryConstructorParametersAsProperties &&
                (valueParameter.containingDeclaration as? ClassConstructorDescriptor)?.isPrimary == true
        if (isPrimaryConstructor) {
            renderModifier(builder, actualPropertiesInPrimaryConstructor, "actual")
        }

        renderVariable(valueParameter, includeName, builder, topLevel, isPrimaryConstructor)

        val withDefaultValue =
            defaultParameterValueRenderer != null &&
                    (if (debugMode) valueParameter.declaresDefaultValue else valueParameter.declaresOrInheritsDefaultValue())
        if (withDefaultValue) {
            builder.append(" = ${defaultParameterValueRenderer!!(valueParameter)}")
        }
    }

    private fun renderPropertyKeyword(
        variable: PropertyDescriptor,
        builder: StringBuilder,

        ) {

        builder.append(renderKeyword(if (variable.isVar) "mut  prop" else "prop")).append(" ")

    }

    private fun renderLetVarPrefix(
        variable: VariableDescriptor,
        builder: StringBuilder,
        isInPrimaryConstructor: Boolean = false
    ) {
        if (isInPrimaryConstructor || variable !is ValueParameterDescriptor) {
            builder.append(renderKeyword(if (variable.isVar) "var" else "let")).append(" ")
        }
    }

    private fun renderVariableS(
        variable: VariableDescriptor,
        includeName: Boolean,
        builder: StringBuilder,
        topLevel: Boolean,
        isInPrimaryConstructor: Boolean = false
    ) {
        val realType = variable.type
        renderVisibility(variable.visibility, builder)

        renderMemberModifiers(variable, builder)

        renderLetVarPrefix(variable, builder, isInPrimaryConstructor)
        if (includeName) {
            renderName(variable, builder, topLevel)

            builder.append(": ")
        }
        builder.append(renderType(realType))
        if (verbose) {
            builder.append(" /*").append(renderType(realType)).append("*/")
        }
    }

    private fun renderVariable(
        variable: VariableDescriptor,
        includeName: Boolean,
        builder: StringBuilder,
        topLevel: Boolean,
        isInPrimaryConstructor: Boolean = false
    ) {
        val realType = variable.type


//        val varargElementType = (variable as? ValueParameterDescriptor)?.varargElementType
//        val typeToRender = varargElementType ?: realType
//        renderModifier(builder, varargElementType != null, "vararg")

        if (isInPrimaryConstructor || topLevel && !startFromName) {
            renderLetVarPrefix(variable, builder, isInPrimaryConstructor)
        }

        if (includeName) {
            renderName(variable, builder, topLevel)
            if (variable is ValueParameterDescriptor && variable.isNamed) {
                builder.append("! ")
            }
            builder.append(": ")
        }

        builder.append(renderType(realType))

//        renderInitializer(variable, builder)

        if (verbose) {
            builder.append(" /*").append(renderType(realType)).append("*/")
        }
    }


    private fun renderTypeAlias(typeAlias: TypeAliasDescriptor, builder: StringBuilder) {
        builder.renderAnnotations(typeAlias)
        renderVisibility(typeAlias.visibility, builder)

        builder.append(renderKeyword("type")).append(" ")
        renderName(typeAlias, builder, true)

        renderTypeParameters(typeAlias.declaredTypeParameters, builder, false)
        renderCapturedTypeParametersIfRequired(typeAlias, builder)

        builder.append(" = ").append(renderType(typeAlias.underlyingType))
    }

    private fun renderCapturedTypeParametersIfRequired(
        classifier: ClassifierDescriptorWithTypeParameters,
        builder: StringBuilder
    ) {
        val typeParameters = classifier.declaredTypeParameters
        val typeConstructorParameters = classifier.typeConstructor.parameters

        if (verbose && typeConstructorParameters.size > typeParameters.size) {
            builder.append(" /*captured type parameters: ")
            renderTypeParameterList(
                builder,
                typeConstructorParameters.subList(typeParameters.size, typeConstructorParameters.size)
            )
            builder.append("*/")
        }
    }


    private fun renderExtend(extend: ExtendDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword("extend"))
        renderTypeParameters(extend.declaredTypeParameters, builder)
        builder.append("  ")

//        被扩展类型
        builder.append(renderType(extend.extendType))

        renderSuperTypes(extend.superTypes.toList(), builder)
        renderWhereSuffix(extend.declaredTypeParameters, builder)
    }

    /* CLASSES */
    private fun renderClass(cclass: ClassifierDescriptorWithTypeConstructor, builder: StringBuilder) {

        if (!startFromName) {
//
            builder.renderAnnotations(cclass)

            renderVisibility(cclass.visibility, builder)

            if (!(cclass.kind == ClassKind.INTERFACE && cclass.modality == Modality.ABSTRACT ||
                        cclass.modality == Modality.FINAL)
            ) {
                renderModality(cclass.modality, builder, cclass.implicitModalityWithoutExtensions())
            }
            renderMemberModifiers(cclass, builder)
            renderClassKindPrefix(cclass, builder)
        }


        renderName(cclass, builder, true)


        val typeParameters = cclass.declaredTypeParameters
        renderTypeParameters(typeParameters, builder, false)
        renderCapturedTypeParametersIfRequired(cclass, builder)



        renderSuperTypes(cclass, builder)
        renderWhereSuffix(typeParameters, builder)
    }

    private fun renderSuperTypes(types: List<CangJieType>, builder: StringBuilder) {
        if (withoutSuperTypes) return



        if (types.isEmpty() || types.size == 1 && CangJieBuiltIns.isAny(
                types.iterator().next()
            )
        ) return

        renderSpaceIfNeeded(builder)
        builder.append("<: ")
        types.joinTo(builder, "& ") { renderType(it) }
    }

    private fun renderSuperTypes(cclass: ClassifierDescriptor, builder: StringBuilder) {
        if (withoutSuperTypes) return

        if (CangJieBuiltIns.isNothing(cclass.defaultType)) return

        val supertypes = cclass.typeConstructor.supertypes.toList()
        renderSuperTypes(supertypes, builder)
    }

    private fun renderClassKindPrefix(cclass: ClassifierDescriptorWithTypeConstructor, builder: StringBuilder) {
        builder.append(renderKeyword(getClassifierKindPrefix(cclass)))
        builder.append("  ")
    }


    /* OTHER */
    private fun renderPackageView(packageView: PackageViewDescriptor, builder: StringBuilder) {
        renderPackageHeader(packageView.fqName, "package", builder)
        if (debugMode) {
            builder.append(" in context of ")
            renderName(packageView.module, builder, false)
        }
    }

    private fun renderPackageFragment(fragment: PackageFragmentDescriptor, builder: StringBuilder) {
        renderPackageHeader(fragment.fqName, "package-fragment", builder)
        if (debugMode) {
            builder.append(" in ")
            renderName(fragment.containingDeclaration, builder, false)
        }
    }

    private fun renderPackageHeader(fqName: FqName, fragmentOrView: String, builder: StringBuilder) {
        builder.append(renderKeyword(fragmentOrView))
        val fqNameString = renderFqName(fqName.toUnsafe())
        if (fqNameString.isNotEmpty()) {
            builder.append(" ")
            builder.append(fqNameString)
        }
    }

//    private fun renderAccessorModifiers(descriptor: PropertyAccessorDescriptor, builder: StringBuilder) {
//        renderMemberModifiers(descriptor, builder)
//    }

    /* STUPID DISPATCH-ONLY VISITOR */
    private inner class RenderDeclarationDescriptorVisitor : DeclarationDescriptorVisitor<Unit, StringBuilder> {
        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, builder: StringBuilder?) {
            builder?.let { renderValueParameter(descriptor, true, it, true) }
        }

        //
        override fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: StringBuilder?) {


            builder?.let { renderVariableS(descriptor, true, it, true) }

        }

        override fun visitVariableDescriptorBase(descriptor: VariableDescriptor, builder: StringBuilder?) {
            builder?.let { renderVariable(descriptor, true, it, true) }

        }

        //
        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder?) {
            builder?.let { renderProperty(descriptor, it) }
        }


        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: StringBuilder?) {
            builder?.let { renderFunction(descriptor, it) }
        }

        override fun visitReceiverParameterDescriptor(
            descriptor: ReceiverParameterDescriptor,
            builder: StringBuilder?
        ) {
            builder?.append(descriptor.name) // renders <this>
        }


        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: StringBuilder?) {
            builder?.let { renderTypeParameter(descriptor, it, true) }
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, builder: StringBuilder?) {
            builder?.let { renderPackageFragment(descriptor, it) }
        }


        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: StringBuilder?) {
            builder?.let { renderPackageView(descriptor, it) }
        }


        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: StringBuilder?) {
            builder?.let { renderName(descriptor, it, true) }
        }

//        override fun visitEnumClassCallDescriptor(descriptor: EnumClassCallableDescriptor, builder: StringBuilder?) {
//            descriptor.type.accept(this, builder)
//        }

//        override fun visitClassCallDescriptor(descriptor: ClassCallableDescriptor, builder: StringBuilder?) {
//            descriptor.type.accept(this, builder)
//        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder?) {
            builder?.let { renderClass(descriptor, it) }
        }

        override fun visitEnumDescriptor(
            descriptor: EnumDescriptor,
            builder: StringBuilder?
        ) {
            builder?.let { renderClass(descriptor, it) }
        }


        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder?) {
            builder?.let { renderTypeAlias(descriptor, it) }

        }

        private fun renderPrimaryConstructor(constructor: ConstructorDescriptor, builder: StringBuilder) {
            builder.append(" ")
            builder.renderAnnotations(constructor)
            renderVisibility(constructor.visibility, builder)
            builder.append(constructor.constructedClass.name)
            renderValueParameters(
                constructor.valueParameters,
//                    primaryConstructor.hasSynthesizedParameterNames(),
                builder,
                false
            )


        }

        private fun renderEnumConstructor(constructor: EnumConstructorDescriptor, builder: StringBuilder) {

            builder.append(renderName(constructor.name, false))
            if (!constructor.valueParameters.isEmpty()) {

                renderValueParameters(
                    constructor.valueParameters, builder, true
                )


            }

        }


        private fun renderConstructor(constructor: ConstructorDescriptor, builder: StringBuilder) {
            if (constructor.isPrimary) {
                renderPrimaryConstructor(constructor, builder)
                return
            }
            builder.renderAnnotations(constructor)
            val visibilityRendered =
                (options.renderDefaultVisibility || constructor.constructedClass.modality != Modality.SEALED)
                        && renderVisibility(constructor.visibility, builder)
            renderMemberKind(constructor, builder)

            val constructorKeywordRendered = renderConstructorKeyword || !constructor.isPrimary || visibilityRendered
            if (constructorKeywordRendered) {

                builder.append(renderKeyword("init"))

            }
            val classDescriptor = constructor.containingDeclaration
            if (secondaryConstructorsAsPrimary) {
                if (constructorKeywordRendered) {
                    builder.append(" ")
                }
                renderName(classDescriptor, builder, true, constructor.isEnd)
                renderTypeParameters(constructor.typeParameters, builder, false)
            }

            renderValueParameters(constructor.valueParameters, builder, constructor.hasSynthesizedParameterNames())

            if (renderConstructorDelegation && !constructor.isPrimary && classDescriptor is ClassDescriptor) {
                val primaryConstructor = classDescriptor.unsubstitutedPrimaryConstructor
                if (primaryConstructor != null) {
                    val parametersWithoutDefault = primaryConstructor.valueParameters.filter {
                        !it.declaresDefaultValue && it.varargElementType == null
                    }
                    if (parametersWithoutDefault.isNotEmpty()) {
                        builder.append(" : ").append(renderKeyword("this"))
                        builder.append(
                            parametersWithoutDefault.joinToString(
                                prefix = "(",
                                postfix = ")",
                                separator = ", "
                            ) { "" })
                    }
                }
            }

            if (secondaryConstructorsAsPrimary) {
                renderWhereSuffix(constructor.typeParameters, builder)
            }
        }

        override fun visitEnumConstructorDescriptor(
            descriptor: EnumConstructorDescriptor,
            builder: StringBuilder?
        ) {
            builder?.let { renderEnumConstructor(descriptor, it) }

        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder?) {
            builder?.let { renderConstructor(constructorDescriptor, it) }

        }

        override fun visitExtendDescriptor(
            descriptor: ExtendDescriptor,
            builder: StringBuilder?
        ) {
            builder?.let { renderExtend(descriptor, it) }


        }

        private fun renderProperty(property: PropertyDescriptor, builder: StringBuilder) {
            if (!startFromName) {
                if (!startFromDeclarationKeyword) {
//                    renderContextReceivers(property.contextReceiverParameters, builder)
//                    renderPropertyAnnotations(property, builder)
                    renderVisibility(property.visibility, builder)
                    renderModifier(builder, DescriptorRendererModifier.CONST in modifiers && property.isConst, "const")
                    renderMemberModifiers(property, builder)
                    renderModalityForCallable(property, builder)
                    renderOverride(property, builder)
//                    renderModifier(builder, DescriptorRendererModifier.LATEINIT in modifiers && property.isLateInit, "lateinit")
                    renderMemberKind(property, builder)
                }
                renderPropertyKeyword(property, builder)
//                renderLetVarPrefix(property, builder)
                renderTypeParameters(property.typeParameters, builder, true)
            }

            renderName(property, builder, true)
            builder.append(": ").append(renderType(property.type))

            renderWhereSuffix(property.typeParameters, builder)
        }

        private fun renderAccessorModifiers(descriptor: PropertyAccessorDescriptor, builder: StringBuilder) {
            renderMemberModifiers(descriptor, builder)
        }

        private fun visitPropertyAccessorDescriptor(
            descriptor: PropertyAccessorDescriptor,
            builder: StringBuilder,
            kind: String
        ) {
            when (propertyAccessorRenderingPolicy) {
                PropertyAccessorRenderingPolicy.PRETTY -> {
                    renderAccessorModifiers(descriptor, builder)
                    builder.append("$kind for ")
                    renderProperty(descriptor.correspondingProperty, builder)
                }

                PropertyAccessorRenderingPolicy.DEBUG -> {
                    visitFunctionDescriptor(descriptor, builder)
                }

                PropertyAccessorRenderingPolicy.NONE -> {
                }
            }
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder?) {
            builder?.let { visitPropertyAccessorDescriptor(descriptor, it, "getter") }

        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder?) {
            builder?.let { visitPropertyAccessorDescriptor(descriptor, it, "setter") }

        }


    }

    private fun renderSpaceIfNeeded(builder: StringBuilder) {
        val length = builder.length
        if (length == 0 || builder[length - 1] != ' ') {
            builder.append(' ')
        }
    }

    private fun overridesSomething(callable: CallableMemberDescriptor) = !callable.overriddenDescriptors.isEmpty()
}
