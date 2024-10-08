package com.huawei.cangjie.ide.codeinsight.quickDoc.cdoc

import com.huawei.cangjie.builtins.*
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import com.huawei.cangjie.descriptors.impl.PropertyAccessorDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.renderer.*
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.ArrayValue
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
import com.huawei.cangjie.types.util.isUnresolvedType
import com.huawei.cangjie.utils.toLowerCaseAsciiOnly
import java.lang.reflect.Modifier
import java.util.*
import kotlin.jvm.internal.PropertyReference1Impl
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty


interface ClassifierNamePolicyEx : ClassifierNamePolicy {

    fun renderClassifierWithType(
        classifier: ClassifierDescriptor,
        renderer: DescriptorRenderer,
        type: CangJieType
    ): String
}

/**
 * Almost copy-paste from [ClassifierNamePolicy.SOURCE_CODE_QUALIFIED]
 *
 * for local declarations qualified up to function scope
 */
object SourceCodeQualified : ClassifierNamePolicyEx {

    override fun renderClassifierWithType(
        classifier: ClassifierDescriptor,
        renderer: DescriptorRenderer,
        type: CangJieType
    ): String =
        qualifiedNameForSourceCode(classifier, type)

    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
        qualifiedNameForSourceCode(classifier)

    private fun qualifiedNameForSourceCode(descriptor: ClassifierDescriptor, type: CangJieType? = null): String {
        val nameString =
            descriptor.name.render() + (type?.takeIf { it.isDefinitelyNotNullType }?.let { " & Any" } ?: "")
        if (descriptor is TypeParameterDescriptor) {
            return nameString
        }
        val qualifier = qualifierName(descriptor.containingDeclaration)
        return if (qualifier != null && qualifier != "") "$qualifier.$nameString" else nameString
    }

    private fun qualifierName(descriptor: DeclarationDescriptor): String? = when (descriptor) {
        is ClassDescriptor -> qualifiedNameForSourceCode(descriptor)
        is PackageFragmentDescriptor -> descriptor.fqName.toUnsafe().render()
        else -> null
    }
}

open class CangJieIdeDescriptorOptions : DescriptorRendererOptions {

    var isLocked: Boolean = false
        private set

    fun lock() {
        check(!isLocked) { "options have been already locked to prevent mutability" }
        isLocked = true
    }

    protected fun <T> property(initialValue: T): ReadWriteProperty<CangJieIdeDescriptorOptions, T> {
        return Delegates.vetoable(initialValue) { _, _, _ ->
            check(!isLocked) { "Cannot modify readonly DescriptorRendererOptions" }
            true
        }
    }

    fun copy(): CangJieIdeDescriptorOptions {
        val copy = CangJieIdeDescriptorOptions()

        //TODO: use Kotlin reflection
        for (field in this::class.java.declaredFields) {
            if (field.modifiers.and(Modifier.STATIC) != 0) continue
            field.isAccessible = true
            val property = field.get(this) as? ObservableProperty<*> ?: continue
            assert(!field.name.startsWith("is")) { "Fields named is* are not supported here yet" }
            val value = property.getValue(
                this,
                PropertyReference1Impl(
                    CangJieIdeDescriptorOptions::class,
                    field.name,
                    "get" + field.name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    })
            )
            field.set(copy, copy.property(value))
        }

        return copy
    }

    var bold by property(false)
    var dataClassWithPrimaryConstructor by property(true)

    var highlightingManager by property(CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING)

    override var classifierNamePolicy: ClassifierNamePolicy by property(SourceCodeQualified)
    override var withDefinedIn by property(true)
    override var withSourceFileForTopLevel by property(true)
    override var modifiers: Set<DescriptorRendererModifier> by property(DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS)
    override var startFromName by property(false)
    override var startFromDeclarationKeyword by property(false)
    override var debugMode by property(false)
    override var classWithPrimaryConstructor by property(false)
    override var verbose by property(false)
    override var unitReturnType by property(true)
    override var withoutReturnType by property(false)
    override var enhancedTypes by property(false)
    override var normalizedVisibilities by property(false)
    override var renderDefaultVisibility by property(true)
    override var renderDefaultModality by property(true)
    override var renderConstructorDelegation by property(false)
    override var renderPrimaryConstructorParametersAsProperties by property(false)
    override var actualPropertiesInPrimaryConstructor: Boolean by property(false)
    override var uninferredTypeParameterAsName by property(false)
    override var includePropertyConstant by property(false)
    override var withoutTypeParameters by property(false)
    override var withoutSuperTypes by property(false)
    override var typeNormalizer by property<(CangJieType) -> CangJieType> { it }
    override var defaultParameterValueRenderer by property<((ValueParameterDescriptor) -> String)?> { "..." }
    override var secondaryConstructorsAsPrimary by property(true)
    override var overrideRenderingPolicy by property(OverrideRenderingPolicy.RENDER_OPEN)
    override var valueParametersHandler: DescriptorRenderer.ValueParametersHandler by property(DescriptorRenderer.ValueParametersHandler.DEFAULT)
    override var textFormat by property(RenderingFormat.PLAIN)
    override var parameterNameRenderingPolicy by property(ParameterNameRenderingPolicy.ALL)
    override var receiverAfterName by property(false)
    override var renderCompanionObjectName by property(false)
    override var propertyAccessorRenderingPolicy by property(PropertyAccessorRenderingPolicy.DEBUG)
    override var propertyConstantRenderer: ((ConstantValue<*>) -> String?)? by property(null)
    override var renderDefaultAnnotationArguments by property(false)
    override var eachAnnotationOnNewLine by property(false)
    override var excludedAnnotationClasses by property(emptySet<FqName>())

    override var annotationFilter: ((AnnotationDescriptor) -> Boolean)? by property(null)
    override var annotationArgumentsRenderingPolicy by property(AnnotationArgumentsRenderingPolicy.NO_ARGUMENTS)
    override var alwaysRenderModifiers by property(false)
    override var renderConstructorKeyword by property(true)
    override var renderUnabbreviatedType by property(true)
    override var renderTypeExpansions by property(false)
    override var renderAbbreviatedTypeComments by property(false)
    override var includeAdditionalModifiers by property(true)
    override var parameterNamesInFunctionalTypes by property(true)
    override var renderFunctionContracts by property(false)
    override var presentableUnresolvedTypes by property(false)
    override var boldOnlyForNamesInHtml by property(false)
    override var informativeErrorType by property(true)
}

open class CangJieIdeDescriptorRenderer(
    val options: CangJieIdeDescriptorOptions
) : DescriptorRenderer(), DescriptorRendererOptions by options {


    fun withIdeOptions(changeOptions: CangJieIdeDescriptorOptions.() -> Unit): CangJieIdeDescriptorRenderer {
        val options = this.options.copy()
        options.changeOptions()
        options.lock()
        return CangJieIdeDescriptorRenderer(options)
    }

    companion object {
        fun withOptions(changeOptions: CangJieIdeDescriptorOptions.() -> Unit): CangJieIdeDescriptorRenderer {
            val options = CangJieIdeDescriptorOptions()
            options.changeOptions()
            options.lock()
            return CangJieIdeDescriptorRenderer(options)
        }

    }

    private var overriddenHighlightingManager: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>? =
        null
        get() = field ?: options.highlightingManager

    private fun highlight(
        value: String,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.() -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ): String {
        return with(overriddenHighlightingManager!!) { buildString { appendHighlighted(value, attributesBuilder()) } }
    }

    override fun renderMessage(message: String): String {
        val highlighted = highlight(message) { asInfo }
        return when (textFormat) {
            RenderingFormat.PLAIN -> highlighted
            RenderingFormat.HTML -> "<i>$highlighted</i>"
        }
    }

    override fun renderType(type: CangJieType): String = buildString {
        appendNormalizedType(typeNormalizer(type))
    }

    private fun StringBuilder.appendNormalizedTypeAsIs(type: CangJieType) {
        if (type is WrappedType && debugMode && !type.isComputed()) {
            appendHighlighted("<Not computed yet>") { asInfo }
            return
        }
        when (val unwrappedType = type.unwrap()) {
            is BasicType -> appendHighlighted(unwrappedType.typeName.toString()) { asKeyword }

            is FlexibleType -> append(
                unwrappedType.render(
                    this@CangJieIdeDescriptorRenderer,
                    this@CangJieIdeDescriptorRenderer
                )
            )

            is SimpleType -> appendSimpleType(unwrappedType)
        }
    }

    protected fun shouldRenderAsPrettyFunctionType(type: CangJieType): Boolean {
        return type.isBuiltinFunctionalType
    }

    protected fun renderError(message: String): String {
        val highlighted = highlight(message) { asError }
        return when (textFormat) {
            RenderingFormat.PLAIN -> highlighted
            RenderingFormat.HTML -> if (options.bold) "<b>$highlighted</b>" else highlighted
        }
    }

    private fun StringBuilder.appendSimpleType(type: SimpleType) {
        if (type == CANNOT_INFER_FUNCTION_PARAM_TYPE || TypeUtils.isDontCarePlaceholder(type)) {
            appendHighlighted("???") { asError }
            return
        }
        if (ErrorUtils.isUninferredTypeVariable(type)) {
            if (uninferredTypeParameterAsName) {
                append(renderError((type.constructor as ErrorTypeConstructor).getParam(0)))
            } else {
                appendHighlighted("???") { asError }
            }
            return
        }

        if (type.isError) {
            appendDefaultType(type)
            return
        }
        if (shouldRenderAsPrettyFunctionType(type)) {
            appendFunctionType(type)
        } else {
            appendDefaultType(type)
        }
    }

    private fun renderKeyword(keyword: String): String {
        val highlighted = highlight(keyword) { asKeyword }
        return when (textFormat) {
            RenderingFormat.PLAIN -> highlighted
            RenderingFormat.HTML -> if (options.bold && !boldOnlyForNamesInHtml) "<b>$highlighted</b>" else highlighted
        }
    }

    private fun StringBuilder.appendModifier(value: Boolean, modifier: String) {
        if (value) {
            append(renderKeyword(modifier))
            append(" ")
        }
    }

    private fun StringBuilder.appendFunctionType(type: CangJieType) {
        val lengthBefore = length
        // we need special renderer to skip @ExtensionFunctionType
//        with(functionTypeAnnotationsRenderer) {
//            appendAnnotations(type)
//        }
        val hasAnnotations = length != lengthBefore


        val isNullable = type.isMarkedOption
        val receiverType = type.getReceiverTypeFromFunctionType()

        val needParenthesis = isNullable || (hasAnnotations && receiverType != null)
        if (needParenthesis) {

            if (hasAnnotations) {
                assert(last().isWhitespace())
                if (get(lastIndex - 1) != ')') {
                    // last annotation rendered without parenthesis - need to add them otherwise parsing will be incorrect
                    insert(lastIndex, highlight("()") { asParentheses })
                }
            }

            appendHighlighted("(") { asParentheses }

        }



        if (receiverType != null) {
            val surroundReceiver = shouldRenderAsPrettyFunctionType(receiverType) && !receiverType.isMarkedOption/* ||
                    receiverType.hasModifiersOrAnnotations()*/
            if (surroundReceiver) {
                appendHighlighted("(") { asParentheses }
            }
            appendNormalizedType(receiverType)
            if (surroundReceiver) {
                appendHighlighted(")") { asParentheses }
            }
            appendHighlighted(".") { asDot }
        }

        appendHighlighted("(") { asParentheses }

        val parameterTypes = type.getValueParameterTypesFromFunctionType()
        for ((index, typeProjection) in parameterTypes.withIndex()) {
            if (index > 0) appendHighlighted(", ") { asComma }

            val name =
                if (parameterNamesInFunctionalTypes) typeProjection.type.extractParameterNameFromFunctionTypeArgument() else null
            if (name != null) {
                appendHighlighted(renderName(name, false)) { asParameter }
                appendHighlighted(": ") { asColon }
            }

            append(renderTypeProjection(typeProjection))
        }

        appendHighlighted(") ") { asParentheses }
        append(arrow())
        append(" ")
        appendNormalizedType(type.getReturnTypeFromFunctionType())

        if (needParenthesis) appendHighlighted(")") { asParentheses }

        if (isNullable) appendHighlighted("?") { asNullityMarker }
    }

    //    private fun StringBuilder.appendTypeConstructorAndArguments(
//        type: CangJieType,
//        typeConstructor: TypeConstructor = type.constructor
//    ) {
//        val possiblyInnerType = type.buildPossiblyInnerType()
//        if (possiblyInnerType == null) {
//            append(renderTypeConstructorOfType(typeConstructor, type))
//            append(renderTypeArguments(type.arguments))
//            return
//        }
//
//        appendPossiblyInnerType(possiblyInnerType)
//    }

    protected fun arrow(): String {
        return highlight(escape("->")) { asArrow }
    }

    private fun StringBuilder.appendTypeConstructorAndArguments(
        type: CangJieType,
        typeConstructor: TypeConstructor = type.constructor
    ) {

        append(renderTypeConstructorOfType(typeConstructor, type))
        append(renderTypeArguments(type.arguments))


    }

    private fun StringBuilder.appendDefaultType(type: CangJieType) {
//        appendAnnotations(type)

        if (type.isMarkedOption) {
            appendHighlighted("?") { asNullityMarker }
        }
        if (type.isError) {
            if (isUnresolvedType(type) && presentableUnresolvedTypes) {
                appendHighlighted(ErrorUtils.unresolvedTypeAsItIs(type)) { asError }
            } else {
                if (type is ErrorType && !informativeErrorType) {
                    appendHighlighted(type.debugMessage) { asError }
                } else {
                    appendHighlighted(type.constructor.toString()) { asError } // Debug name of an error type is more informative
                }
                appendHighlighted(renderTypeArguments(type.arguments)) { asError }
            }
        } else {
            appendTypeConstructorAndArguments(type)
        }


        if (classifierNamePolicy !is ClassifierNamePolicyEx) {
            if (type.isDefinitelyNotNullType) {
                appendHighlighted(" & Any") { asNonNullAssertion }
            }
        }
    }

    private fun StringBuilder.appendHighlighted(
        value: String,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.() -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ) {
        return with(overriddenHighlightingManager!!) {
            this@appendHighlighted.appendHighlighted(
                value,
                attributesBuilder()
            )
        }
    }

    private fun StringBuilder.appendNormalizedType(type: CangJieType) {
        val abbreviated = type.unwrap() as? AbbreviatedType
        if (abbreviated != null) {
            if (renderTypeExpansions) {
                appendNormalizedTypeAsIs(abbreviated.expandedType)
            } else {
                // TODO nullability is lost for abbreviated type?
                appendNormalizedTypeAsIs(abbreviated.abbreviation)
                if (renderUnabbreviatedType) {
                    appendAbbreviatedTypeExpansion(abbreviated)
                }
            }
            return
        }

        appendNormalizedTypeAsIs(type)
    }

    private inline fun <R> withNoHighlighting(action: () -> R): R {
        val old = overriddenHighlightingManager
        overriddenHighlightingManager = CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING
        val result = action()
        overriddenHighlightingManager = old
        return result
    }

    protected fun escape(string: String) = textFormat.escape(string)

    override fun renderFqName(fqName: FqNameUnsafe) = renderFqName(fqName.pathSegments())

    private fun renderFqName(pathSegments: List<Name>): String {
        val rendered = buildString {
            for (element in pathSegments) {
                if (isNotEmpty()) {
                    appendHighlighted(".") { asDot }
                }
                appendHighlighted(element.render()) { asClassName }
            }
        }
        return escape(rendered)
    }

    private fun StringBuilder.appendAbbreviatedTypeExpansion(abbreviated: AbbreviatedType) {

        if (textFormat == RenderingFormat.HTML) {
            append("<i>")
        }
        val expandedType = withNoHighlighting { buildString { appendNormalizedTypeAsIs(abbreviated.expandedType) } }
        appendHighlighted(" /* = $expandedType */") { asInfo }
        if (textFormat == RenderingFormat.HTML) {
            append("</i></font>")
        }
    }

    override fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String {
        TODO("Not yet implemented")
    }

    protected fun lt() = highlight(escape("<")) { asOperationSign }
    protected fun gt() = highlight(escape(">")) { asOperationSign }
    private fun StringBuilder.appendTypeProjections(typeProjections: List<TypeProjection>) {
        typeProjections.joinTo(this, highlight(", ") { asComma }) {

            val type = renderType(it.type)
            if (it.projectionKind == Variance.INVARIANT) type
            else "${highlight(it.projectionKind.toString()) { asKeyword }} $type"

        }
    }

    override fun renderTypeArguments(typeArguments: List<TypeProjection>): String = if (typeArguments.isEmpty()) ""
    else buildString {
        append(lt())
        appendTypeProjections(typeArguments)
        append(gt())
    }

    override fun renderTypeProjection(typeProjection: TypeProjection): String  = buildString {
        appendTypeProjections(listOf(typeProjection))
    }

    override fun renderTypeConstructor(typeConstructor: TypeConstructor): String =
        when (val cd = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor -> highlight(renderClassifierName(cd)) { asTypeParameterName }
            is ClassDescriptor -> highlight(renderClassifierName(cd)) { asClassName }
            is TypeAliasDescriptor -> highlight(renderClassifierName(cd)) { asTypeAlias }
            null -> highlight(escape(typeConstructor.toString())) { asClassName }
            else -> error("Unexpected classifier: " + cd::class.java)
        }

    fun renderClassifierNameWithType(klass: ClassifierDescriptor, type: CangJieType): String =
        if (ErrorUtils.isError(klass)) {
            klass.typeConstructor.toString()
        } else {
            val policy = classifierNamePolicy
            if (policy is ClassifierNamePolicyEx) {
                policy.renderClassifierWithType(klass, this, type)
            } else {
                policy.renderClassifier(klass, this)
            }
        }

    private fun renderTypeConstructorOfType(typeConstructor: TypeConstructor, type: CangJieType): String =
        when (val cd = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor -> highlight(renderClassifierNameWithType(cd, type)) { asTypeParameterName }
            is ClassDescriptor -> highlight(renderClassifierNameWithType(cd, type)) { asClassName }
            is TypeAliasDescriptor -> highlight(renderClassifierNameWithType(cd, type)) { asTypeAlias }
            null -> highlight(escape(typeConstructor.toString())) { asClassName }
            else -> error("Unexpected classifier: " + cd::class.java)
        }

    override fun renderClassifierName(klass: ClassifierDescriptor): String = if (ErrorUtils.isError(klass)) {
        klass.typeConstructor.toString()
    } else
        classifierNamePolicy.renderClassifier(klass, this)

    override fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget?): String {
        return ""
    }

    private fun StringBuilder.appendDefinedIn(descriptor: DeclarationDescriptor) {
        if (descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
            return
        }
        if (descriptor is ModuleDescriptor) {
            append(renderMessage(" is a module"))
            return
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration != null && containingDeclaration !is ModuleDescriptor) {
            append(renderMessage(" defined in "))
            val fqName = DescriptorUtils.getFqName(containingDeclaration)
            append(if (fqName.isRoot) "root package" else renderFqName(fqName))

            if (withSourceFileForTopLevel &&
                containingDeclaration is PackageFragmentDescriptor &&
                descriptor is DeclarationDescriptorWithSource
            ) {
                descriptor.source.containingFile.name?.let { sourceFileName ->
                    append(renderMessage(" in file "))
                    append(sourceFileName)
                }
            }
        }
    }

    private fun StringBuilder.appendTypeParameter(typeParameter: TypeParameterDescriptor, topLevel: Boolean) {
        if (topLevel) {
            append(lt())
        }

        if (verbose) {
            appendHighlighted("/*${typeParameter.index}*/ ") { asInfo }
        }

        val variance = typeParameter.variance.label
        appendModifier(variance.isNotEmpty(), variance)

//        appendAnnotations(typeParameter)

        appendName(typeParameter, topLevel) { asTypeParameterName }
        val upperBoundsCount = typeParameter.upperBounds.size
        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
            val upperBound = typeParameter.upperBounds.iterator().next()
            if (!CangJieBuiltIns.isDefaultBound(upperBound)) {
                appendHighlighted(" : ") { asColon }
                append(renderType(upperBound))
            }
        } else if (topLevel) {
            var first = true
            for (upperBound in typeParameter.upperBounds) {
                if (CangJieBuiltIns.isDefaultBound(upperBound)) {
                    continue
                }
                if (first) {
                    appendHighlighted(" : ") { asColon }
                } else {
                    appendHighlighted(" & ") { asOperationSign }
                }
                append(renderType(upperBound))
                first = false
            }
        } else {
            // rendered with "where"
        }

        if (topLevel) {
            append(gt())
        }
    }

    private fun StringBuilder.appendTypeParameterList(typeParameters: List<TypeParameterDescriptor>) {
        val iterator = typeParameters.iterator()
        while (iterator.hasNext()) {
            val typeParameterDescriptor = iterator.next()
            appendTypeParameter(typeParameterDescriptor, false)
            if (iterator.hasNext()) {
                appendHighlighted(", ") { asComma }
            }
        }
    }

    private fun StringBuilder.appendTypeParameters(typeParameters: List<TypeParameterDescriptor>, withSpace: Boolean) {
        if (withoutTypeParameters) return

        if (typeParameters.isNotEmpty()) {
            append(lt())
            appendTypeParameterList(typeParameters)
            append(gt())
            if (withSpace) {
                append(" ")
            }
        }
    }

    private fun StringBuilder.appendVariable(property: VariableDescriptor) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {

                appendVisibility(property.visibility)
                appendModifier(DescriptorRendererModifier.CONST in modifiers && property.isConst, "const")
                appendMemberModifiers(property)


            }
            appendLetVarPrefix(property)
            appendTypeParameters(property.typeParameters, true)
            appendReceiver(property)
        }

        appendName(property, true) { asInstanceProperty }
        appendHighlighted(": ") { asColon }
        append(renderType(property.type))

        appendReceiverAfterName(property)

        appendInitializer(property)

        appendWhereSuffix(property.typeParameters)
    }

    private fun StringBuilder.appendProperty(property: PropertyDescriptor) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
                appendPropertyAnnotations(property)
                appendVisibility(property.visibility)
                appendModifier(DescriptorRendererModifier.CONST in modifiers && property.isConst, "const")
                appendMemberModifiers(property)
                appendModalityForCallable(property)
                appendOverride(property)

                appendMemberKind(property)
            }
            appendLetVarPrefix(property)
            appendTypeParameters(property.typeParameters, true)
            appendReceiver(property)
        }

        appendName(property, true) { asInstanceProperty }
        appendHighlighted(": ") { asColon }
        append(renderType(property.type))

        appendReceiverAfterName(property)

        appendInitializer(property)

        appendWhereSuffix(property.typeParameters)
    }

    private fun StringBuilder.appendPropertyAnnotations(property: PropertyDescriptor) {
        if (DescriptorRendererModifier.ANNOTATIONS !in modifiers) return

//        appendAnnotations(property, eachAnnotationOnNewLine)

//        property.backingField?.let { appendAnnotations(it, target = AnnotationUseSiteTarget.FIELD) }
//        property.delegateField?.let { appendAnnotations(it, target = AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }

        if (propertyAccessorRenderingPolicy == PropertyAccessorRenderingPolicy.NONE) {
            property.getter?.let {
//                appendAnnotations(it, target = AnnotationUseSiteTarget.PROPERTY_GETTER)
            }
            property.setter?.let { setter ->
//                appendAnnotations(setter, target = AnnotationUseSiteTarget.PROPERTY_SETTER)
                setter.valueParameters.single().let {
//                    appendAnnotations(it, target = AnnotationUseSiteTarget.SETTER_PARAMETER)
                }
            }
        }
    }

    private fun StringBuilder.appendReceiver(callableDescriptor: CallableDescriptor) {
        val receiver = callableDescriptor.extensionReceiverParameter
        if (receiver != null) {
//            appendAnnotations(receiver, target = AnnotationUseSiteTarget.RECEIVER)

            val type = receiver.type
            var result = renderType(type)
            if (shouldRenderAsPrettyFunctionType(type) && !TypeUtils.isNullableType(type)) {
                result = "${highlight("(") { asParentheses }}$result${highlight(")") { asParentheses }}"
            }
            append(result)
            appendHighlighted(".") { asDot }
        }
    }

    private fun StringBuilder.appendVisibility(visibility: DescriptorVisibility): Boolean {
        @Suppress("NAME_SHADOWING")
        var visibility = visibility
        if (DescriptorRendererModifier.VISIBILITY !in modifiers) return false
        if (normalizedVisibilities) {
            visibility = visibility.normalize()
        }
        if (!renderDefaultVisibility && visibility == DescriptorVisibilities.DEFAULT_VISIBILITY) return false
        append(renderKeyword(visibility.internalDisplayName))
        append(" ")
        return true
    }

    private fun StringBuilder.appendMemberModifiers(descriptor: MemberDescriptor) {
    }

    private fun StringBuilder.appendOverride(callableMember: CallableMemberDescriptor) {
        if (DescriptorRendererModifier.OVERRIDE !in modifiers) return
        if (overridesSomething(callableMember)) {
            if (overrideRenderingPolicy != OverrideRenderingPolicy.RENDER_OPEN) {
                appendModifier(true, "override")
                if (verbose) {
                    appendHighlighted("/*${callableMember.overriddenDescriptors.size}*/ ") { asInfo }
                }
            }
        }
    }

    private fun overridesSomething(callable: CallableMemberDescriptor) = !callable.overriddenDescriptors.isEmpty()
    private fun StringBuilder.appendModality(modality: Modality, defaultModality: Modality) {
        if (!renderDefaultModality && modality == defaultModality) return
        appendModifier(DescriptorRendererModifier.MODALITY in modifiers, modality.name.toLowerCaseAsciiOnly())
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

    private fun StringBuilder.appendModalityForCallable(callable: CallableMemberDescriptor) {
        if (!DescriptorUtils.isTopLevelDeclaration(callable) || callable.modality != Modality.FINAL) {
            if (overrideRenderingPolicy == OverrideRenderingPolicy.RENDER_OVERRIDE && callable.modality == Modality.OPEN &&
                overridesSomething(callable)
            ) {
                return
            }
            if(callable.modality != Modality.FINAL) {
                appendModality(callable.modality, callable.implicitModalityWithoutExtensions())

            }
        }
    }

    private fun StringBuilder.appendMemberKind(callableMember: CallableMemberDescriptor) {
        if (DescriptorRendererModifier.MEMBER_KIND !in modifiers) return
        if (verbose && callableMember.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            appendHighlighted("/*${callableMember.kind.name.toLowerCaseAsciiOnly()}*/ ") { asInfo }
        }
    }

    private fun StringBuilder.appendFunction(function: FunctionDescriptor) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
//                appendAnnotations(function, eachAnnotationOnNewLine)
                appendVisibility(function.visibility)
                appendModalityForCallable(function)

                if (includeAdditionalModifiers) {
                    appendMemberModifiers(function)
                }

                appendOverride(function)

//                if (includeAdditionalModifiers) {
//                    appendAdditionalModifiers(function)
//                } else {
//                    appendSuspendModifier(function)
//                }

                appendMemberKind(function)

                if (verbose) {
                    if (function.isHiddenToOvercomeSignatureClash) {
                        appendHighlighted("/*isHiddenToOvercomeSignatureClash*/ ") { asInfo }
                    }

                    if (function.isHiddenForResolutionEverywhereBesideSupercalls) {
                        appendHighlighted("/*isHiddenForResolutionEverywhereBesideSupercalls*/ ") { asInfo }
                    }
                }
            }

            append(renderKeyword("func"))
            append(" ")

            appendReceiver(function)
        }

        appendName(function, true) { asFunDeclaration }
        appendTypeParameters(function.typeParameters, true)
        appendValueParameters(function.valueParameters, function.hasSynthesizedParameterNames())

        appendReceiverAfterName(function)

        val returnType = function.returnType
        if (!withoutReturnType && (unitReturnType || (returnType == null || !CangJieBuiltIns.isUnit(returnType)))) {
            appendHighlighted(": ") { asColon }
            append(if (returnType == null) highlight("[NULL]") { asError } else renderType(returnType))
        }

        appendWhereSuffix(function.typeParameters)
    }

    private fun StringBuilder.appendWhereSuffix(typeParameters: List<TypeParameterDescriptor>) {
        if (withoutTypeParameters) return

        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in typeParameters) {
            typeParameter.upperBounds
                .drop(1) // first parameter is rendered by renderTypeParameter
                .mapTo(upperBoundStrings) {
                    buildString {
                        appendHighlighted(renderName(typeParameter.name, false)) { asTypeParameterName }
                        appendHighlighted(" : ") { asColon }
                        append(renderType(it))
                    }
                }
        }

        if (upperBoundStrings.isNotEmpty()) {
            append(" ")
            append(renderKeyword("where"))
            append(" ")
            upperBoundStrings.joinTo(this, highlight(", ") { asComma })
        }
    }

    private fun StringBuilder.appendReceiverAfterName(callableDescriptor: CallableDescriptor) {
        if (!receiverAfterName) return

        val receiver = callableDescriptor.extensionReceiverParameter
        if (receiver != null) {
            appendHighlighted(" on ") { asInfo }
            append(renderType(receiver.type))
        }
    }

    private fun StringBuilder.appendPackageView(packageView: PackageViewDescriptor) {
        appendPackageHeader(packageView.fqName, "package")
        if (debugMode) {
            appendHighlighted(" in context of ") { asInfo }
            appendName(packageView.module, false) { asPackageName }
        }
    }

    private fun StringBuilder.appendPackageHeader(fqName: FqName, fragmentOrView: String) {
        append(renderKeyword(fragmentOrView))
        val fqNameString = renderFqName(fqName.toUnsafe())
        if (fqNameString.isNotEmpty()) {
            append(" ")
            append(fqNameString)
        }
    }

    private fun StringBuilder.appendConstructor(constructor: ConstructorDescriptor) {
//        appendAnnotations(constructor)
        val visibilityRendered =
            (options.renderDefaultVisibility || constructor.constructedClass.modality != Modality.SEALED)
                    && appendVisibility(constructor.visibility)
        appendMemberKind(constructor)

        val constructorKeywordRendered = renderConstructorKeyword || !constructor.isPrimary || visibilityRendered
        if (constructorKeywordRendered) {
            append(renderKeyword("constructor"))
        }
        val classDescriptor = constructor.containingDeclaration
        if (secondaryConstructorsAsPrimary) {
            if (constructorKeywordRendered) {
                append(" ")
            }
            appendName(classDescriptor, true) { asClassName }
            appendTypeParameters(constructor.typeParameters, false)
        }

        appendValueParameters(constructor.valueParameters, constructor.hasSynthesizedParameterNames())

        if (renderConstructorDelegation && !constructor.isPrimary && classDescriptor is ClassDescriptor) {
            val primaryConstructor = classDescriptor.unsubstitutedPrimaryConstructor
            if (primaryConstructor != null) {
                val parametersWithoutDefault = primaryConstructor.valueParameters.filter {
                    !it.declaresDefaultValue() && it.varargElementType == null
                }
                if (parametersWithoutDefault.isNotEmpty()) {
                    appendHighlighted(" : ") { asColon }
                    append(renderKeyword("this"))
                    append(parametersWithoutDefault.joinToString(
                        prefix = highlight("(") { asParentheses },
                        separator = highlight(", ") { asComma },
                        postfix = highlight(")") { asParentheses }
                    ) { "" }
                    )
                }
            }
        }

        if (secondaryConstructorsAsPrimary) {
            appendWhereSuffix(constructor.typeParameters)
        }
    }


    private fun StringBuilder.appendClassKindPrefix(klass: ClassDescriptor) {
        append(renderKeyword(getClassifierKindPrefix(klass)))
    }

    private fun StringBuilder.appendClass(cclass: ClassDescriptor) {
        val isEnumEntry = cclass.kind == ClassKind.ENUM_ENTRY

        if (!startFromName) {
//            appendAnnotations(cclass, eachAnnotationOnNewLine)
            if (!isEnumEntry) {
                appendVisibility(cclass.visibility)
            }
            if (!(cclass.kind == ClassKind.INTERFACE && cclass.modality == Modality.ABSTRACT) && cclass.modality != Modality.FINAL
            ) {
                appendModality(cclass.modality, cclass.implicitModalityWithoutExtensions())
            }
            appendMemberModifiers(cclass)

            appendModifier(DescriptorRendererModifier.VALUE in modifiers && cclass.isValue, "value")

            appendClassKindPrefix(cclass)
        }


        if (isEnumEntry) return
append(" ")
        appendName(cclass, true) { asClassName }

        val typeParameters = cclass.declaredTypeParameters
        appendTypeParameters(typeParameters, false)


        appendSuperTypes(cclass, prefix = "\n    ")


        appendWhereSuffix(typeParameters)
    }

    private fun StringBuilder.appendSuperTypes(cclass: ClassDescriptor, prefix: String = " ", indent: String = "    ") {
        if (withoutSuperTypes) return

        if (CangJieBuiltIns.isNothing(cclass.defaultType)) return

        val supertypes = cclass.typeConstructor.supertypes.toMutableList()

        if (supertypes.isEmpty() || supertypes.size == 1 && CangJieBuiltIns.isAnyOrNullableAny(
                supertypes.iterator().next()
            )
        ) return

        append(prefix)
        appendHighlighted("&lt;: ") { asLtColon }
        val separator = when {
            supertypes.size <= 3 -> ", "
            else -> ",\n$indent  "
        }
        supertypes.joinTo(this, highlight(separator) { asComma }) { renderType(it) }
    }

    private fun StringBuilder.appendTypeAlias(typeAlias: TypeAliasDescriptor) {
//        appendAnnotations(typeAlias, eachAnnotationOnNewLine)
        appendVisibility(typeAlias.visibility)
        appendMemberModifiers(typeAlias)
        append(renderKeyword("typealias"))
        append(" ")
        appendName(typeAlias, true) { asTypeAlias }

        appendTypeParameters(typeAlias.declaredTypeParameters, false)

        appendHighlighted(" = ") { asOperationSign }
        append(renderType(typeAlias.underlyingType))
    }

    private fun StringBuilder.appendAccessorModifiers(descriptor: PropertyAccessorDescriptor) {
        appendMemberModifiers(descriptor)
    }

    private fun StringBuilder.appendPackageFragment(fragment: PackageFragmentDescriptor) {
        appendPackageHeader(fragment.fqName, "package-fragment")
        if (debugMode) {
            appendHighlighted(" in ") { asInfo }
            appendName(fragment.containingDeclaration, false) { asPackageName }
        }
    }

    private inner class RenderDeclarationDescriptorVisitor : DeclarationDescriptorVisitor<Unit, StringBuilder> {
        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, builder: StringBuilder?) {
            builder?.appendValueParameter(descriptor, true, true)
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: StringBuilder?) {
            builder?.appendVariable(descriptor, true, true)
        }

        override fun visitVariableDescriptorBase(descriptor: VariableDescriptor, builder: StringBuilder?) {

            builder?.appendVariable(descriptor)
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder?) {
            builder?.appendProperty(descriptor)
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder?) {
            builder?.let { visitPropertyAccessorDescriptor(descriptor, it, "getter") }
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder?) {
            builder?.let { visitPropertyAccessorDescriptor(descriptor, it, "setter") }
        }

        private fun visitPropertyAccessorDescriptor(
            descriptor: PropertyAccessorDescriptor,
            builder: StringBuilder,
            kind: String
        ) {
            when (propertyAccessorRenderingPolicy) {
                PropertyAccessorRenderingPolicy.PRETTY -> {
                    builder.appendAccessorModifiers(descriptor)
                    builder.appendHighlighted("$kind for ") { asInfo }
                    builder.appendProperty(descriptor.correspondingProperty)
                }

                PropertyAccessorRenderingPolicy.DEBUG -> {
                    visitFunctionDescriptor(descriptor, builder)
                }

                PropertyAccessorRenderingPolicy.NONE -> {
                }
            }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: StringBuilder?) {
            builder?.appendFunction(descriptor)
        }

        override fun visitReceiverParameterDescriptor(
            descriptor: ReceiverParameterDescriptor,
            builder: StringBuilder?
        ) {
            builder?.append(descriptor.name) // renders <this>
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder?) {
            builder?.appendConstructor(constructorDescriptor)
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: StringBuilder?) {
            builder?.appendTypeParameter(descriptor, true)
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, builder: StringBuilder?) {
            builder?.appendPackageFragment(descriptor)
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: StringBuilder?) {
            builder?.appendPackageView(descriptor)
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: StringBuilder?) {
            builder?.appendName(descriptor, true) { asPackageName }
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder?) {
            builder?.appendClass(descriptor)
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder?) {
            builder?.appendTypeAlias(descriptor)
        }


    }


    override fun render(declarationDescriptor: DeclarationDescriptor): String {
        return buildString {
            declarationDescriptor.accept(RenderDeclarationDescriptorVisitor(), this)

            if (withDefinedIn) {
                appendDefinedIn(declarationDescriptor)
            }
        }
    }

    override fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ) = buildString {
        appendValueParameters(parameters, synthesizedParameterNames)
    }

    private fun shouldRenderParameterNames(synthesizedParameterNames: Boolean): Boolean =
        when (parameterNameRenderingPolicy) {
            ParameterNameRenderingPolicy.ALL -> true
            ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED -> !synthesizedParameterNames
            ParameterNameRenderingPolicy.NONE -> false
        }

    private fun StringBuilder.appendValueParameter(
        valueParameter: ValueParameterDescriptor,
        includeName: Boolean,
        topLevel: Boolean
    ) {
        if (topLevel) {
            append(renderKeyword("value-parameter"))
            append(" ")
        }

        if (verbose) {
            appendHighlighted("/*${valueParameter.index}*/ ") { asInfo }
        }


        val isPrimaryConstructor = renderPrimaryConstructorParametersAsProperties &&
                (valueParameter.containingDeclaration as? ClassConstructorDescriptor)?.isPrimary == true
        if (isPrimaryConstructor) {
            appendModifier(actualPropertiesInPrimaryConstructor, "actual")
        }

        appendVariable(valueParameter, includeName, topLevel, isPrimaryConstructor)

        val withDefaultValue =
            defaultParameterValueRenderer != null &&
                    (if (debugMode) valueParameter.declaresDefaultValue() else valueParameter.declaresOrInheritsDefaultValue())
        if (withDefaultValue) {
            appendHighlighted(" = ") { asOperationSign }
            append(highlightByLexer(defaultParameterValueRenderer!!(valueParameter)))
        }
    }

    private fun StringBuilder.appendLetVarPrefix(
        variable: VariableDescriptor,
        isInPrimaryConstructor: Boolean = false
    ) {
        if (isInPrimaryConstructor || variable !is ValueParameterDescriptor) {
            if (variable.isVar) {
                appendHighlighted("var") { asVar }
            } else {
                appendHighlighted("let") { asLet }
            }
            append(" ")
        }
    }


    private fun StringBuilder.appendVariable(
        variable: VariableDescriptor,
        includeName: Boolean,
        topLevel: Boolean,
        isInPrimaryConstructor: Boolean = false
    ) {
        val realType = variable.type

        val varargElementType = (variable as? ValueParameterDescriptor)?.varargElementType
        val typeToRender = varargElementType ?: realType
        appendModifier(varargElementType != null, "vararg")

        if (isInPrimaryConstructor || topLevel && !startFromName) {
            appendLetVarPrefix(variable, isInPrimaryConstructor)
        }

        if (includeName) {
            appendName(variable, topLevel) { asLocalVarOrLet }
            appendHighlighted(": ") { asColon }
        }

        append(renderType(typeToRender))

        appendInitializer(variable)

        if (verbose && varargElementType != null) {
            val expandedType = withNoHighlighting { renderType(realType) }
            appendHighlighted(" /*${expandedType}*/") { asInfo }
        }
    }

    protected fun StringBuilder.appendName(descriptor: DeclarationDescriptor, rootRenderedElement: Boolean) {
        append(renderName(descriptor.name, rootRenderedElement))
    }

    private fun StringBuilder.appendName(
        descriptor: DeclarationDescriptor,
        rootRenderedElement: Boolean,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.() -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ) {

        return with(options.highlightingManager) {
            this@appendName.appendHighlighted(renderName(descriptor.name, rootRenderedElement), attributesBuilder())
        }
    }


    private fun StringBuilder.appendInitializer(variable: VariableDescriptor) {
        if (includePropertyConstant) {
            variable.getCompileTimeInitializer()?.let { constant ->
                appendHighlighted(" = ") { asOperationSign }
                append(escape(renderConstant(constant)))
            }
        }
    }

    private fun highlightByLexer(value: String): String {
        return with(overriddenHighlightingManager!!) { buildString { appendCodeSnippetHighlightedByLexer(value) } }
    }

    private fun renderConstant(value: ConstantValue<*>): String {
        return when (value) {
            is ArrayValue -> {
                buildString {
                    appendHighlighted("{") { asBraces }
                    value.value.joinTo(this, highlight(", ") { asComma }) { renderConstant(it) }
                    appendHighlighted("}") { asBraces }
                }
            }

            else -> highlightByLexer(value.toString())
        }
    }

    private fun StringBuilder.appendValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ) {
        val includeNames = shouldRenderParameterNames(synthesizedParameterNames)
        val parameterCount = parameters.size
        valueParametersHandler.appendBeforeValueParameters(parameterCount, this)
        for ((index, parameter) in parameters.withIndex()) {
            valueParametersHandler.appendBeforeValueParameter(parameter, index, parameterCount, this)
            appendValueParameter(parameter, includeNames, false)
            valueParametersHandler.appendAfterValueParameter(parameter, index, parameterCount, this)
        }
        valueParametersHandler.appendAfterValueParameters(parameterCount, this)
    }

    override fun renderName(name: Name, rootRenderedElement: Boolean): String {
        val escaped = escape(name.render())
        return if (options.bold && boldOnlyForNamesInHtml && textFormat == RenderingFormat.HTML && rootRenderedElement) {
            "<b>$escaped</b>"
        } else
            escaped
    }


}
