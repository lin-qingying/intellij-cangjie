package com.huawei.cangjie.renderer


import com.huawei.cangjie.builtins.*
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.ClassKind.*
import com.huawei.cangjie.descriptors.annotations.Annotated
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.ArrayValue
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.resolve.descriptorUtil.annotationClass
import com.huawei.cangjie.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
import com.huawei.cangjie.types.util.isUnresolvedType
import com.huawei.cangjie.utils.toLowerCaseAsciiOnly
import java.lang.reflect.Modifier
import kotlin.jvm.internal.PropertyReference1Impl
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty

enum class ParameterNameRenderingPolicy {
    ALL,
    ONLY_NON_SYNTHESIZED,
    NONE
}

//object ExcludedTypeAnnotations {
//    val internalAnnotationsForResolve = setOf(
//        FqName("cangjie.internal.NoInfer"),
//        FqName("cangjie.internal.Exact")
//    )
//}
enum class OverrideRenderingPolicy {
    RENDER_OVERRIDE,
    RENDER_OPEN,
    RENDER_OPEN_OVERRIDE
}

enum class PropertyAccessorRenderingPolicy {
    PRETTY,
    DEBUG,
    NONE
}

enum class AnnotationArgumentsRenderingPolicy(
    val includeAnnotationArguments: Boolean = false,
    val includeEmptyAnnotationArguments: Boolean = false
) {
    NO_ARGUMENTS,
    UNLESS_EMPTY(true),
    ALWAYS_PARENTHESIZED(includeAnnotationArguments = true, includeEmptyAnnotationArguments = true)
}

abstract class DescriptorRenderer {
    fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
        val options = (this as DescriptorRendererImpl).options.copy()
        options.changeOptions()
        options.lock()
        return DescriptorRendererImpl(options)
    }

    abstract fun renderMessage(message: String): String

    abstract fun renderType(type: CangJieType): String

    abstract fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String

    abstract fun renderTypeArguments(typeArguments: List<TypeProjection>): String

    abstract fun renderTypeProjection(typeProjection: TypeProjection): String

    abstract fun renderTypeConstructor(typeConstructor: TypeConstructor): String

    abstract fun renderClassifierName(klass: ClassifierDescriptor): String

    abstract fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget? = null): String

    abstract fun render(declarationDescriptor: DeclarationDescriptor): String

    abstract fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ): String

    fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String =
        renderValueParameters(
            functionDescriptor.valueParameters,
            false /*functionDescriptor.hasSynthesizedParameterNames()*/
        )

    abstract fun renderName(name: Name, rootRenderedElement: Boolean): String

    abstract fun renderFqName(fqName: FqNameUnsafe): String

    interface ValueParametersHandler {
        fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder)
        fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder)

        fun appendBeforeValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        )

        fun appendAfterValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        )

        object DEFAULT : ValueParametersHandler {
            override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append("(")
            }

            override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append(")")
            }

            override fun appendBeforeValueParameter(
                parameter: ValueParameterDescriptor,
                parameterIndex: Int,
                parameterCount: Int,
                builder: StringBuilder
            ) {
            }

            override fun appendAfterValueParameter(
                parameter: ValueParameterDescriptor,
                parameterIndex: Int,
                parameterCount: Int,
                builder: StringBuilder
            ) {
                if (parameterIndex != parameterCount - 1) {
                    builder.append(", ")
                }
            }
        }
    }

    companion object {
        fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
            val options = DescriptorRendererOptionsImpl()
            options.changeOptions()
            options.lock()
            return DescriptorRendererImpl(options)
        }

        @JvmField
        val WITHOUT_MODIFIERS: DescriptorRenderer = withOptions {
            modifiers = emptySet()
        }

        @JvmField
        val COMPACT_WITH_MODIFIERS: DescriptorRenderer = withOptions {
            withDefinedIn = false
        }

        @JvmField
        val COMPACT: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
        }

        @JvmField
        val COMPACT_WITHOUT_SUPERTYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            withoutSuperTypes = true
        }

        @JvmField
        val COMPACT_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField
        val ONLY_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            withoutTypeParameters = true
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            receiverAfterName = true
            renderCompanionObjectName = true
            withoutSuperTypes = true
            startFromName = true
        }

        @JvmField
        val FQ_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
        }

        @JvmField
        val FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField
        val SHORT_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField
        val DEBUG_TEXT: DescriptorRenderer = withOptions {
            debugMode = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField
        val HTML: DescriptorRenderer = withOptions {
            textFormat = RenderingFormat.HTML
            modifiers = DescriptorRendererModifier.ALL
        }

        fun getClassifierKindPrefix(classifier: ClassifierDescriptorWithTypeParameters): String = when (classifier) {
            is TypeAliasDescriptor ->
                "typealias"

            is ClassDescriptor ->
                when (classifier.kind) {
                    STRUCT -> "struct"
                    CLASS -> "class"
                    INTERFACE -> "interface"
                    ENUM -> "enum"
                    EXTEND -> "extend"
                    ANNOTATION_CLASS -> "annotation class"
                    ENUM_ENTRY -> "enum entry"
                    BASIC -> "basic type"
                }

            else ->
                throw AssertionError("Unexpected classifier: $classifier")
        }
    }
}


enum class RenderingFormat {
    PLAIN {
        override fun escape(string: String) = string
    },
    HTML {
        override fun escape(string: String) = string.replace("<", "&lt;").replace(">", "&gt;")
    };

    abstract fun escape(string: String): String
}

enum class DescriptorRendererModifier(val includeByDefault: Boolean) {
    VISIBILITY(true),
    MODALITY(true),
    OVERRIDE(true),
    ANNOTATIONS(false),

    //    INNER(true),
    MEMBER_KIND(true),

    //    INLINE(true),
//    EXPECT(true),
//    ACTUAL(true),
    CONST(true),

    //    LATEINIT(true),
    FUNC(true),
    VALUE(true)
    ;

    companion object {
        @JvmField
        val ALL_EXCEPT_ANNOTATIONS = entries.filter { it.includeByDefault }.toSet()

        @JvmField
        val ALL = entries.toSet()
    }
}

interface DescriptorRendererOptions {
    var classifierNamePolicy: ClassifierNamePolicy
    var withDefinedIn: Boolean
    var withSourceFileForTopLevel: Boolean
    var modifiers: Set<DescriptorRendererModifier>
    var startFromName: Boolean
    var startFromDeclarationKeyword: Boolean
    var debugMode: Boolean
    var classWithPrimaryConstructor: Boolean
    var verbose: Boolean
    var unitReturnType: Boolean
    var enhancedTypes: Boolean
    var withoutReturnType: Boolean
    var normalizedVisibilities: Boolean
    var renderDefaultVisibility: Boolean
    var renderDefaultModality: Boolean
    var renderConstructorDelegation: Boolean
    var renderPrimaryConstructorParametersAsProperties: Boolean
    var actualPropertiesInPrimaryConstructor: Boolean
    var uninferredTypeParameterAsName: Boolean

    var overrideRenderingPolicy: OverrideRenderingPolicy
    var valueParametersHandler: DescriptorRenderer.ValueParametersHandler
    var textFormat: RenderingFormat
    var excludedAnnotationClasses: Set<FqName>

    //    var excludedTypeAnnotationClasses: Set<FqName>
    var annotationFilter: ((AnnotationDescriptor) -> Boolean)?
    var eachAnnotationOnNewLine: Boolean

    var annotationArgumentsRenderingPolicy: AnnotationArgumentsRenderingPolicy
    val includeAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeAnnotationArguments
    val includeEmptyAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeEmptyAnnotationArguments

    var boldOnlyForNamesInHtml: Boolean

    var includePropertyConstant: Boolean
    var propertyConstantRenderer: ((ConstantValue<*>) -> String?)?
    var parameterNameRenderingPolicy: ParameterNameRenderingPolicy
    var withoutTypeParameters: Boolean
    var receiverAfterName: Boolean
    var renderCompanionObjectName: Boolean
    var withoutSuperTypes: Boolean
    var typeNormalizer: (CangJieType) -> CangJieType
    var defaultParameterValueRenderer: ((ValueParameterDescriptor) -> String)?
    var secondaryConstructorsAsPrimary: Boolean
    var propertyAccessorRenderingPolicy: PropertyAccessorRenderingPolicy
    var renderDefaultAnnotationArguments: Boolean
    var alwaysRenderModifiers: Boolean
    var renderConstructorKeyword: Boolean
    var renderUnabbreviatedType: Boolean
    var renderTypeExpansions: Boolean

    /**
     * If [renderTypeExpansions] is `true`, additionally renders the abbreviated type as a comment behind the expanded type.
     */
    var renderAbbreviatedTypeComments: Boolean

    var includeAdditionalModifiers: Boolean
    var parameterNamesInFunctionalTypes: Boolean
    var renderFunctionContracts: Boolean
    var presentableUnresolvedTypes: Boolean
    var informativeErrorType: Boolean
}


internal class DescriptorRendererOptionsImpl : DescriptorRendererOptions {
    var isLocked: Boolean = false
        private set

    fun lock() {
        assert(!isLocked)
        isLocked = true
    }

    fun copy(): DescriptorRendererOptionsImpl {
        val copy = DescriptorRendererOptionsImpl()

        //TODO: use CangJie reflection
        for (field in this::class.java.declaredFields) {
            if (field.modifiers.and(Modifier.STATIC) != 0) continue
            field.isAccessible = true
            val property = field.get(this) as? ObservableProperty<*> ?: continue
            assert(!field.name.startsWith("is")) { "Fields named is* are not supported here yet" }
            val value = property.getValue(
                this,
                PropertyReference1Impl(
                    DescriptorRendererOptionsImpl::class,
                    field.name,
                    "get" + field.name.replaceFirstChar(Char::uppercaseChar)
                )
            )
            field.set(copy, copy.property(value))
        }

        return copy
    }

    private fun <T> property(initialValue: T): ReadWriteProperty<DescriptorRendererOptionsImpl, T> {
        return Delegates.vetoable(initialValue) { _, _, _ ->
            if (isLocked) {
                throw IllegalStateException("Cannot modify readonly DescriptorRendererOptions")
            } else {
                true
            }
        }
    }

    override var classifierNamePolicy: ClassifierNamePolicy by property(ClassifierNamePolicy.SOURCE_CODE_QUALIFIED)
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
    override var propertyConstantRenderer: ((ConstantValue<*>) -> String?)? by property(null)
    override var withoutTypeParameters by property(false)
    override var withoutSuperTypes by property(false)
    override var typeNormalizer by property<(CangJieType) -> CangJieType>({ it })
    override var defaultParameterValueRenderer by property<((ValueParameterDescriptor) -> String)?>({ "..." })
    override var secondaryConstructorsAsPrimary by property(true)
    override var overrideRenderingPolicy by property(OverrideRenderingPolicy.RENDER_OPEN)
    override var valueParametersHandler: DescriptorRenderer.ValueParametersHandler by property(DescriptorRenderer.ValueParametersHandler.DEFAULT)
    override var textFormat by property(RenderingFormat.PLAIN)
    override var parameterNameRenderingPolicy by property(ParameterNameRenderingPolicy.ALL)
    override var receiverAfterName by property(false)
    override var renderCompanionObjectName by property(false)
    override var propertyAccessorRenderingPolicy by property(PropertyAccessorRenderingPolicy.DEBUG)
    override var renderDefaultAnnotationArguments by property(false)

    override var eachAnnotationOnNewLine: Boolean by property(false)

    override var excludedAnnotationClasses by property(emptySet<FqName>())

//    override var excludedTypeAnnotationClasses by property(ExcludedTypeAnnotations.internalAnnotationsForResolve)

    override var annotationFilter: ((AnnotationDescriptor) -> Boolean)? by property(null)

    override var annotationArgumentsRenderingPolicy by property(AnnotationArgumentsRenderingPolicy.NO_ARGUMENTS)

    override var alwaysRenderModifiers by property(false)

    override var renderConstructorKeyword by property(true)

    override var renderUnabbreviatedType: Boolean by property(true)

    override var renderTypeExpansions: Boolean by property(false)

    override var renderAbbreviatedTypeComments: Boolean by property(false)

    override var includeAdditionalModifiers: Boolean by property(true)

    override var parameterNamesInFunctionalTypes: Boolean by property(true)

    override var renderFunctionContracts: Boolean by property(false)

    override var presentableUnresolvedTypes: Boolean by property(false)

    override var boldOnlyForNamesInHtml: Boolean by property(false)

    override var informativeErrorType: Boolean by property(true)
}


internal class DescriptorRendererImpl(
    val options: DescriptorRendererOptionsImpl
) : DescriptorRenderer(), DescriptorRendererOptions by options/* this gives access to options without qualifier */ {
    init {
        assert(options.isLocked)
    }

    private val functionTypeAnnotationsRenderer: DescriptorRendererImpl by lazy {
        withOptions {
            /*     excludedTypeAnnotationClasses += */listOf(
            StandardNames.FqNames.extensionFunctionType,
            StandardNames.FqNames.contextFunctionTypeParams
        )
        } as DescriptorRendererImpl
    }

    /* FORMATTING */
    private fun renderKeyword(keyword: String): String = when (textFormat) {
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

    private fun renderName(descriptor: DeclarationDescriptor, builder: StringBuilder, rootRenderedElement: Boolean) {
        builder.append(renderName(descriptor.name, rootRenderedElement))
    }

    private fun renderCompanionObjectName(descriptor: DeclarationDescriptor, builder: StringBuilder) {
        if (renderCompanionObjectName) {
            if (startFromName) {
                builder.append("companion object")
            }
            renderSpaceIfNeeded(builder)
            val containingDeclaration = descriptor.containingDeclaration
            if (containingDeclaration != null) {
                builder.append("of ")
                builder.append(renderName(containingDeclaration.name, false))
            }
        }
        if (verbose || descriptor.name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            if (!startFromName) renderSpaceIfNeeded(builder)
            builder.append(renderName(descriptor.name, true))
        }
    }

    override fun renderFqName(fqName: FqNameUnsafe) = renderFqName(fqName.pathSegments())

    private fun renderFqName(pathSegments: List<Name>) =
        escape(com.huawei.cangjie.renderer.renderFqName(pathSegments))

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
//            classifierNamePolicy.renderClassifier(builtIns.collection, this).substringBefore("Collection")
        val mutablePrefix = "Mutable"
        // java.util.List<Foo> -> (Mutable)List<Foo!>!
        val simpleCollection = replacePrefixesInTypeRepresentations(
            lowerRendered,
            cangjieCollectionsPrefix + mutablePrefix,
            upperRendered,
            cangjieCollectionsPrefix,
            "$cangjieCollectionsPrefix($mutablePrefix)"
        )
        if (simpleCollection != null) return simpleCollection
        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
        val mutableEntry = replacePrefixesInTypeRepresentations(
            lowerRendered,
            cangjieCollectionsPrefix + "MutableMap.MutableEntry",
            upperRendered,
            cangjieCollectionsPrefix + "Map.Entry",
            "$cangjieCollectionsPrefix(Mutable)Map.(Mutable)Entry"
        )
        if (mutableEntry != null) return mutableEntry

        val cangjiePrefix = ""
//            classifierNamePolicy.renderClassifier(builtIns.array, this).substringBefore("Array")
        // Foo[] -> Array<(out) Foo!>!
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

    override fun renderTypeArguments(typeArguments: List<TypeProjection>): String =
        if (typeArguments.isEmpty()) ""
        else buildString {
            append(lt())
            this.appendTypeProjections(typeArguments)
            append(gt())
        }

    private fun StringBuilder.renderDefaultType(type: CangJieType) {
        this.renderAnnotations(type)

        val originalTypeOfDefNotNullType = (type as? DefinitelyNotNullType)?.original

        if (type.isMarkedOption) {
            append("?")
        }

        when {
            type is OptionType -> renderSimpleType(type.getType() as SimpleType)
            type.isMarkedOption -> renderSimpleType(type.arguments[0].type as SimpleType)

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



        if (type.isDefinitelyNotNullType) {
            append(" & Any")
        }
    }

    private fun StringBuilder.renderTypeConstructorAndArguments(
        type: CangJieType,
        typeConstructor: TypeConstructor = type.constructor
    ) {
//        if (type.isMarkedOption) {
//            renderTypeConstructorAndArguments(type.arguments[0].type)
//        } else {
        append(renderTypeConstructor(typeConstructor))
        append(renderTypeArguments(type.arguments))
//        }
//        val possiblyInnerType = type.buildPossiblyInnerType()
//        if (possiblyInnerType == null) {

//        return
//        }

//        renderPossiblyInnerType(possiblyInnerType)
    }
//    private fun StringBuilder.renderPossiblyInnerType(possiblyInnerType: PossiblyInnerType) {
//        possiblyInnerType.outerType?.let {
//            renderPossiblyInnerType(it)
//            append('.')
//            append(renderName(possiblyInnerType.classifierDescriptor.name, false))
//        } ?: append(renderTypeConstructor(possiblyInnerType.classifierDescriptor.typeConstructor))
//
//        append(renderTypeArguments(possiblyInnerType.arguments))
//    }
    /*    private fun StringBuilder.renderPossiblyInnerType(possiblyInnerType: PossiblyInnerType) {
            possiblyInnerType.outerType?.let {
                renderPossiblyInnerType(it)
                append('.')
                append(renderName(possiblyInnerType.classifierDescriptor.name, false))
            } ?: append(renderTypeConstructor(possiblyInnerType.classifierDescriptor.typeConstructor))

            append(renderTypeArguments(possiblyInnerType.arguments))
        }*/

    override fun renderTypeConstructor(typeConstructor: TypeConstructor): String =
        when (val cd = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor, is ClassDescriptor, is TypeAliasDescriptor -> renderClassifierName(cd)
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
            if (it.isStarProjection) {
                "*"
            } else {
                val type = renderType(it.type)
                if (it.projectionKind == Variance.INVARIANT) type else "${it.projectionKind} $type"
            }
        }
    }

    private fun StringBuilder.renderTupleType(type: CangJieType) {
        append("(")
        appendTypeProjections(type.arguments)
        append(")")
    }

    private fun StringBuilder.renderFunctionType(type: CangJieType) {
        val lengthBefore = length
        // we need special renderer to skip @ExtensionFunctionType and @ContextFunctionTypeParams
        with(functionTypeAnnotationsRenderer) {
            renderAnnotations(type)
        }
        val hasAnnotations = length != lengthBefore

        val receiverType = type.getReceiverTypeFromFunctionType()
        val contextReceiversTypes = type.getContextReceiverTypesFromFunctionType()


        val isNullable = type.isMarkedOption

        val needParenthesis = isNullable || (hasAnnotations && receiverType != null)
        if (needParenthesis) {

            if (hasAnnotations) {
                assert(last().isWhitespace())
                if (get(lastIndex - 1) != ')') {
                    // last annotation rendered without parenthesis - need to add them otherwise parsing will be incorrect
                    insert(lastIndex, "()")
                }
            }

            append("(")

        }

        if (contextReceiversTypes.isNotEmpty()) {
            append("context(")
            val withoutLast = contextReceiversTypes.subList(0, contextReceiversTypes.lastIndex)
            for (contextReceiverType in withoutLast) {
                renderNormalizedType(contextReceiverType)
                append(", ")
            }
            renderNormalizedType(contextReceiversTypes.last())
            append(") ")
        }


        if (receiverType != null) {
            val surroundReceiver = shouldRenderAsPrettyFunctionType(receiverType) && !receiverType.isMarkedOption ||
//                    receiverType.hasModifiersOrAnnotations() ||
                    receiverType is DefinitelyNotNullType
            if (surroundReceiver) {
                append("(")
            }
            renderNormalizedType(receiverType)
            if (surroundReceiver) {
                append(")")
            }
            append(".")
        }

        append("(")

        if (type.isBuiltinExtensionFunctionalType && type.arguments.size <= 1) {
            append("???")
        } else {
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
        }

        append(") ").append(arrow()).append(" ")
        renderNormalizedType(type.getReturnTypeFromFunctionType())

        if (needParenthesis) append(")")

        if (isNullable) append("?")
    }


    /* METHODS FOR ALL KINDS OF DESCRIPTORS */
    private fun StringBuilder.appendDefinedIn(descriptor: DeclarationDescriptor) {
        if (descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
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

        val excluded =/* if (annotated is CangJieType) excludedTypeAnnotationClasses else*/ excludedAnnotationClasses

        val annotationFilter = annotationFilter
        for (annotation in annotated.annotations) {
            if (annotation.fqName !in excluded
                && !annotation.isParameterName()
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

    private fun AnnotationDescriptor.isParameterName(): Boolean {
        return fqName == StandardNames.FqNames.parameterName
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
//                        || annotationType.constructor.declarationDescriptor is NotFoundClasses.MockClassDescriptor
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
            ?.filter { it.declaresDefaultValue() }
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
//            is AnnotationValue -> renderAnnotation(value.value).removePrefix("@")
//            is KClassValue -> when (val classValue = value.value) {
//                is KClassValue.Value.LocalClass -> "${classValue.type}::class"
//                is KClassValue.Value.NormalClass -> {
//                    var type = classValue.classId.asSingleFqName().asString()
//                    repeat(classValue.arrayDimensions) { type = "kotlin.Array<$type>" }
//                    "$type::class"
//                }
//            }
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
        if (!renderDefaultModality && modality == defaultModality) return
        renderModifier(builder, DescriptorRendererModifier.MODALITY in modifiers, modality.name.toLowerCaseAsciiOnly())
    }

    private fun MemberDescriptor.implicitModalityWithoutExtensions(): Modality {
        if (this is ClassDescriptor) {
            return if (kind == INTERFACE) Modality.ABSTRACT else Modality.FINAL
        }
        val containingClassDescriptor = containingDeclaration as? ClassDescriptor ?: return Modality.FINAL
        if (this !is CallableMemberDescriptor) return Modality.FINAL
        if (this.overriddenDescriptors.isNotEmpty()) {
            if (containingClassDescriptor.modality != Modality.FINAL) return Modality.OPEN
        }
        return if (containingClassDescriptor.kind == INTERFACE && this.visibility != DescriptorVisibilities.PRIVATE) {
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
                renderModifier(builder, true, "override")
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
//        renderModifier(builder, descriptor.isExternal, "external")
//        renderModifier(builder, DescriptorRendererModifier.EXPECT in modifiers && descriptor.isExpect, "expect")
//        renderModifier(builder, DescriptorRendererModifier.ACTUAL in modifiers && descriptor.isActual, "actual")
    }

    private fun renderAdditionalModifiers(functionDescriptor: FunctionDescriptor, builder: StringBuilder) {
        val isOperator =
            functionDescriptor.isOperator && (functionDescriptor.overriddenDescriptors.none { it.isOperator } || alwaysRenderModifiers)




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
        val upperBoundsCount = typeParameter.upperBounds.size
        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
            val upperBound = typeParameter.upperBounds.iterator().next()
            if (!CangJieBuiltIns.isDefaultBound(upperBound)) {
                builder.append(" : ").append(renderType(upperBound))
            }
        } else if (topLevel) {
            var first = true
            for (upperBound in typeParameter.upperBounds) {
                if (CangJieBuiltIns.isDefaultBound(upperBound)) {
                    continue
                }
                if (first) {
                    builder.append(" : ")
                } else {
                    builder.append(" & ")
                }
                builder.append(renderType(upperBound))
                first = false
            }
        } else {
            // rendered with "where"
        }

        if (topLevel) {
            builder.append(gt())
        }
    }

    private fun renderTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        builder: StringBuilder,
        withSpace: Boolean
    ) {
        if (withoutTypeParameters) return

        if (typeParameters.isNotEmpty()) {
            builder.append(lt())
            renderTypeParameterList(builder, typeParameters)
            builder.append(gt())
            if (withSpace) {
                builder.append(" ")
            }
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
                renderContextReceivers(function.contextReceiverParameters, builder)
                builder.renderAnnotations(function)
                renderVisibility(function.visibility, builder)
                renderModalityForCallable(function, builder)

                if (includeAdditionalModifiers) {
                    renderMemberModifiers(function, builder)
                }


                renderOverride(function, builder)

                if (includeAdditionalModifiers) {
                    renderAdditionalModifiers(function, builder)
                }/* else {
                    renderSuspendModifier(function, builder)
                }*/

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

            builder.append(renderKeyword("func")).append(" ")
            renderTypeParameters(function.typeParameters, builder, true)
            renderReceiver(function, builder)
        }

        renderName(function, builder, true)

        renderValueParameters(function.valueParameters, false/*function.hasSynthesizedParameterNames()*/, builder)

        renderReceiverAfterName(function, builder)

        val returnType = function.returnType
        if (!withoutReturnType && (unitReturnType || (returnType == null || !CangJieBuiltIns.isUnit(returnType)))) {
            builder.append(": ").append(if (returnType == null) "[NULL]" else renderType(returnType))
        }

        renderWhereSuffix(function.typeParameters, builder)
    }

    private fun renderReceiverAfterName(callableDescriptor: CallableDescriptor, builder: StringBuilder) {
        if (!receiverAfterName) return

        val receiver = callableDescriptor.extensionReceiverParameter
        if (receiver != null) {
            builder.append(" on ").append(renderType(receiver.type))
        }
    }

    private fun CangJieType.renderForReceiver(): String {
        var result = renderType(this)
        if ((shouldRenderAsPrettyFunctionType(this) && !TypeUtils.isNullableType(this)) || this is DefinitelyNotNullType) {
            result = "($result)"
        }
        return result
    }

    private fun renderContextReceivers(contextReceivers: List<ReceiverParameterDescriptor>, builder: StringBuilder) {
        if (contextReceivers.isNotEmpty()) {
            builder.append("context(")
        } else {
            return
        }
        for ((i, contextReceiver) in contextReceivers.withIndex()) {
            builder.renderAnnotations(contextReceiver, AnnotationUseSiteTarget.RECEIVER)
            val typeString = contextReceiver.type.renderForReceiver()
            builder.append(typeString)
            if (i == contextReceivers.lastIndex) {
                builder.append(") ")
            } else {
                builder.append(", ")
            }
        }
    }

    private fun renderReceiver(callableDescriptor: CallableDescriptor, builder: StringBuilder) {
        val receiver = callableDescriptor.extensionReceiverParameter
        if (receiver != null) {
            builder.renderAnnotations(receiver, AnnotationUseSiteTarget.RECEIVER)

            val typeString = receiver.type.renderForReceiver()
            builder.append(typeString).append(".")
        }
    }


    private fun renderWhereSuffix(typeParameters: List<TypeParameterDescriptor>, builder: StringBuilder) {
        if (withoutTypeParameters) return

        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in typeParameters) {
            typeParameter.upperBounds
                .drop(1) // first parameter is rendered by renderTypeParameter
                .mapTo(upperBoundStrings) { renderName(typeParameter.name, false) + " : " + renderType(it) }
        }

        if (upperBoundStrings.isNotEmpty()) {
            builder.append(" ").append(renderKeyword("where")).append(" ")
            upperBoundStrings.joinTo(builder, ", ")
        }
    }

    override fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ) = buildString {
        renderValueParameters(parameters, synthesizedParameterNames, this)
    }

    private fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean,
        builder: StringBuilder
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

    private fun shouldRenderParameterNames(synthesizedParameterNames: Boolean): Boolean =
        when (parameterNameRenderingPolicy) {
            ParameterNameRenderingPolicy.ALL -> true
            ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED -> !synthesizedParameterNames
            ParameterNameRenderingPolicy.NONE -> false
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
                    (if (debugMode) valueParameter.declaresDefaultValue() else valueParameter.declaresOrInheritsDefaultValue())
        if (withDefaultValue) {
            builder.append(" = ${defaultParameterValueRenderer!!(valueParameter)}")
        }
    }

    private fun renderValVarPrefix(
        variable: VariableDescriptorBase,
        builder: StringBuilder,
        isInPrimaryConstructor: Boolean = false
    ) {
        if (isInPrimaryConstructor || variable !is ValueParameterDescriptor) {
            builder.append(renderKeyword(if (variable.isVar) "var" else "val")).append(" ")
        }
    }

    private fun renderVariable(
        variable: VariableDescriptorBase,
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
            renderValVarPrefix(variable, builder, isInPrimaryConstructor)
        }

        if (includeName) {
            renderName(variable, builder, topLevel)
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
        renderMemberModifiers(typeAlias, builder)
        builder.append(renderKeyword("typealias")).append(" ")
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

    /* CLASSES */
    private fun renderClass(klass: ClassDescriptor, builder: StringBuilder) {
        val isEnumEntry = klass.kind == ENUM_ENTRY

        if (!startFromName) {
//            renderContextReceivers(klass.contextReceivers, builder)
            builder.renderAnnotations(klass)
            if (!isEnumEntry) {
                renderVisibility(klass.visibility, builder)
            }
            if (!(klass.kind == INTERFACE && klass.modality == Modality.ABSTRACT ||
                        klass.kind.isStruct && klass.modality == Modality.FINAL)
            ) {
                renderModality(klass.modality, builder, klass.implicitModalityWithoutExtensions())
            }
            renderMemberModifiers(klass, builder)
//            renderModifier(builder, DescriptorRendererModifier.INNER in modifiers && klass.isInner, "inner")
//            renderModifier(builder, DescriptorRendererModifier.DATA in modifiers && klass.isData, "data")
//            renderModifier(builder, DescriptorRendererModifier.INLINE in modifiers && klass.isInline, "inline")
            renderModifier(builder, DescriptorRendererModifier.VALUE in modifiers && klass.isValue, "value")
            renderModifier(builder, DescriptorRendererModifier.FUNC in modifiers && klass.isFun, "fun")
            renderClassKindPrefix(klass, builder)
        }

//        if (!isCompanionObject(klass)) {
//            if (!startFromName) renderSpaceIfNeeded(builder)
        renderName(klass, builder, true)
//        } else {
//            renderCompanionObjectName(klass, builder)
//        }

        if (isEnumEntry) return

        val typeParameters = klass.declaredTypeParameters
        renderTypeParameters(typeParameters, builder, false)
        renderCapturedTypeParametersIfRequired(klass, builder)

        if (!klass.kind.isStruct && classWithPrimaryConstructor) {
            val primaryConstructor = klass.unsubstitutedPrimaryConstructor
            if (primaryConstructor != null) {
                builder.append(" ")
                builder.renderAnnotations(primaryConstructor)
                renderVisibility(primaryConstructor.visibility, builder)
                builder.append(renderKeyword("constructor"))
                renderValueParameters(
                    primaryConstructor.valueParameters,
                    false,
//                    primaryConstructor.hasSynthesizedParameterNames(),
                    builder
                )
            }
        }

        renderSuperTypes(klass, builder)
        renderWhereSuffix(typeParameters, builder)
    }

    private fun renderSuperTypes(klass: ClassDescriptor, builder: StringBuilder) {
        if (withoutSuperTypes) return

        if (CangJieBuiltIns.isNothing(klass.defaultType)) return

        val supertypes = klass.typeConstructor.supertypes
        if (supertypes.isEmpty() || supertypes.size == 1 && CangJieBuiltIns.isAny(
                supertypes.iterator().next()
            )
        ) return

        renderSpaceIfNeeded(builder)
        builder.append(": ")
        supertypes.joinTo(builder, ", ") { renderType(it) }
    }

    private fun renderClassKindPrefix(klass: ClassDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword(getClassifierKindPrefix(klass)))
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
            visitVariableDescriptorBase(descriptor, builder)
        }

        override fun visitVariableDescriptorBase(descriptor: VariableDescriptorBase, builder: StringBuilder?) {
            builder?.let { renderVariable(descriptor, true, it, true) }

        }

        //
        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder?) {
//            renderProperty(descriptor, builder)
        }
//
//        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder) {
//            visitPropertyAccessorDescriptor(descriptor, builder, "getter")
//        }
//
//        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder) {
//            visitPropertyAccessorDescriptor(descriptor, builder, "setter")
//        }

//        private fun visitPropertyAccessorDescriptor(
//            descriptor: PropertyAccessorDescriptor,
//            builder: StringBuilder,
//            kind: String
//        ) {
//            when (propertyAccessorRenderingPolicy) {
//                PropertyAccessorRenderingPolicy.PRETTY -> {
//                    /**/renderAccessorModifiers(descriptor, builder)
//                    builder.append("$kind for ")
//                    renderProperty(descriptor.correspondingProperty, builder)
//                }
//
//                PropertyAccessorRenderingPolicy.DEBUG -> {
//                    visitFunctionDescriptor(descriptor, builder)
//                }
//
//                PropertyAccessorRenderingPolicy.NONE -> {
//                }
//            }
//        }

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


        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder?) {
            builder?.let { renderClass(descriptor, it) }
        }


        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder?) {
            builder?.let { renderTypeAlias(descriptor, it) }

        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder?) {
//            renderConstructor(constructorDescriptor, builder)

        }
//
//        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder) {
//            renderTypeAlias(descriptor, builder)
//        }
    }

    private fun renderSpaceIfNeeded(builder: StringBuilder) {
        val length = builder.length
        if (length == 0 || builder[length - 1] != ' ') {
            builder.append(' ')
        }
    }

    private fun overridesSomething(callable: CallableMemberDescriptor) = !callable.overriddenDescriptors.isEmpty()
}
