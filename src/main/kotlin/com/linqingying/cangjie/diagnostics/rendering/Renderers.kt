package com.linqingying.cangjie.diagnostics.rendering

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.builtins.fqNameUnsafe
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.MatchMissingCase
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.identifier
import com.linqingying.cangjie.renderer.ClassifierNamePolicy
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.MemberComparator
import com.linqingying.cangjie.resolve.calls.components.DescriptorKind
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.getAbbreviation
import com.linqingying.cangjie.types.util.contains


/**
 * 获取声明对应的字符串
 */
fun getDescriptorName(descriptor: DeclarationDescriptor): String {
    return when (descriptor) {
        is ClassDescriptor -> descriptor.kind.codeRepresentation ?: ""

        is FunctionDescriptor -> "Function"
        is PropertyDescriptor -> "Property"
        is VariableDescriptor -> "Variable"


        else -> descriptor.toString()
    }
}

fun DescriptorRenderer.asRenderer() = SmartDescriptorRenderer(this)


object Renderers {
    private val LOG = Logger.getInstance(Renderers::class.java)

    @JvmField
    val NAME = renderer<Name> { it.asString() }

    @JvmField
    val INT = renderer<Int> {
        it.toString()
    }

    @JvmField
    val NAMED_ADN_PARAMETER = renderer { d: DeclarationDescriptor ->
        NAME.render(d.name) + when (d) {
            is FunctionDescriptor -> {
                "(" +
                        d.valueParameters.joinToString(",") {
                            RENDER_TYPE.render(it.type, RenderingContext.Empty)
                        } +
                        ")"
            }

            else -> ""
        }
    }

    @JvmField
    val TYPE_AND_NAMED = renderer<DeclarationDescriptor> {

        getDescriptorName(it) + ":" + NAME.render(it.name)
    }

    @JvmField
    val NAMED = renderer<Named> {
        NAME.render(it.name)
    }

    @JvmField
    val RENDER_CLASS_OR_OBJECT_NAME = renderer<ClassifierDescriptorWithTypeParameters> { it.renderKindWithName() }

    @JvmField
    val COMPACT_WITHOUT_SUPERTYPES = DescriptorRenderer.COMPACT_WITHOUT_SUPERTYPES.asRenderer()

    @JvmField
    val RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.FQ_NAMES_IN_TYPES.withOptions {
        parameterNamesInFunctionalTypes = false
    })

    private val List<MatchMissingCase>.assumesElseBranchOnly: Boolean
        get() = any { it == MatchMissingCase.Unknown || it is MatchMissingCase.ConditionTypeIsExpect }
    private val MATCH_MISSING_LIMIT = 7

    @JvmField
    val RENDER_MATCH_MISSING_CASES = renderer<List<MatchMissingCase>> {
        if (!it.assumesElseBranchOnly) {
            val list = it.joinToString(", ", limit = MATCH_MISSING_LIMIT) { "'$it'" }
            val branches = if (it.size > 1) "branches" else "branch"
            "$list $branches or 'else' branch instead"
        } else {
            "'else' branch"
        }
    }

    @JvmField
    val CLASS_NAME = renderer { cclass: ClassDescriptor ->


        NAME.render(cclass.name)
    }

    @JvmField
    val NAMES_TO_STRING = renderer { names: Collection<Name> ->
        names.joinToString(", ", "{", "}") { type ->

            NAME.render(type)
        }

    }

    @JvmField
    val VISIBLITYS_NAMES = renderer {

            visiblitys: List<DescriptorVisibility> ->
        visiblitys.joinToString(" or ") {

            "'${it.name}'"
        }


    }

    @JvmField
    val RENDER_COLLECTION_OF_TYPES = renderer { types: List<CangJieType> ->

        types.joinToString(", ", "{ ", " }") { type ->

            RENDER_TYPE.render(type, RenderingContext.of(type))
        }


    }

    @JvmField
    val RENDER_TYPE_STATMENT = renderer { classOrObject: CjTypeStatement ->
        val name = classOrObject.name?.let { " ${it.wrapIntoQuotes()}" } ?: ""
        when (classOrObject) {
            is CjClass -> "Class$name"
            is CjInterface -> "Interface$name"
            is CjStruct -> "Struct$name"
            is CjEnum -> "Enum$name"
            else -> "Class$name"
        }
    }

    @JvmField
    val FQ_NAMES_IN_TYPES = DescriptorRenderer.FQ_NAMES_IN_TYPES.asRenderer()

    @JvmField
    val COMPACT_WITH_MODIFIERS = DescriptorRenderer.COMPACT_WITH_MODIFIERS.asRenderer()

    @JvmField
    val DESCRIPTOR_KIND_NAME = renderer { kind: DescriptorKind ->
        kind.kind
    }

    //根据表达式类型显示
    @JvmField
    val EXPRESSION_TYPE_TEXT = renderer<PsiElement> {

        when (it) {
            is CjThisExpression -> "this"
            is CjSuperExpression -> "super"
            else -> ""
        }
    }

    @JvmField
    val MODALITY_NAME = renderer { modality: Modality ->
        modality.name.lowercase()
    }

    @JvmField
    val VISIBILITY = renderer<DescriptorVisibility> {
        it.externalDisplayName
    }

    @JvmField
    val DECL_FQNAME = renderer<DeclarationDescriptor> {
        it.fqNameUnsafe.asString()
    }

    @JvmField
    val NAME_OF_CONTAINING_DECLARATION_OR_FILE = renderer<DeclarationDescriptor> {
        if (DescriptorUtils.isTopLevelDeclaration(it) && it is DeclarationDescriptorWithVisibility && it.visibility == DescriptorVisibilities.PRIVATE) {
            "file"
        } else {
            val containingDeclaration = it.containingDeclaration
            if (containingDeclaration is PackageData) {
                containingDeclaration.fqName.asString().wrapIntoQuotes()
            } else {
                containingDeclaration!!.name.asString().wrapIntoQuotes()
            }
        }
    }

    private fun renderAmbiguousDescriptors(descriptors: Collection<CallableDescriptor>): String {
        val context = RenderingContext.Impl(descriptors)
        return descriptors
            .sortedWith(MemberComparator)
            .joinToString(separator = "\n", prefix = "\n") {
                FQ_NAMES_IN_TYPES.render(it, context)
            }
    }

    @JvmField
    val AMBIGUOUS_CALLS = renderer { calls: Collection<ResolvedCall<*>> ->
        val descriptors = calls.map { it.resultingDescriptor }
        renderAmbiguousDescriptors(descriptors)
    }

    @JvmField
    val TO_STRING = renderer<Any> { element ->
        if (element is DeclarationDescriptor) {
            LOG.warn(
                "Diagnostic renderer TO_STRING was used to render an instance of DeclarationDescriptor.\n"
                        + "This is usually a bad idea, because descriptors' toString() includes some debug information, "
                        + "which should not be seen by the user.\nDescriptor: " + element
            )
        }
        element.toString()
    }

    @JvmField
    val ELEMENT_TEXT = renderer<PsiElement> {
        it.text
    }

    @JvmField
    val ELEMENT_IDENTIFIER_TEXT = renderer<PsiElement> {
        it.identifier?.text ?: ""
    }

    @JvmField
    val FQNAME = renderer<FqName> {
        it.asString()
    }

    @JvmField
    val FQ_NAMES_IN_TYPES_ANNOTATIONS_WHITELIST =
        DescriptorRenderer.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS.withAnnotationsWhitelist()

    private fun String.wrapIntoQuotes(): String = "'$this'"

}

val RenderingContext.adaptiveClassifierPolicy: ClassifierNamePolicy
    get() = this[ADAPTIVE_CLASSIFIER_POLICY_KEY]

private fun collectClassifiersFqNames(objectsToRender: Collection<Any?>): Set<FqNameUnsafe> =
    LinkedHashSet<FqNameUnsafe>().apply {
        collectMentionedClassifiersFqNames(objectsToRender, this)
    }

private val ADAPTIVE_CLASSIFIER_POLICY_KEY =
    object : RenderingContext.Key<ClassifierNamePolicy>("ADAPTIVE_CLASSIFIER_POLICY") {
        override fun compute(objectsToRender: Collection<Any?>): ClassifierNamePolicy {
            val ambiguousNames =
                collectClassifiersFqNames(objectsToRender).groupBy { it.shortNameOrSpecial() }
                    .filter { it.value.size > 1 }.map { it.key }
            return AdaptiveClassifierNamePolicy(ambiguousNames)
        }
    }


private class AdaptiveClassifierNamePolicy(private val ambiguousNames: List<Name>) : ClassifierNamePolicy {
    private val renderedParameters = mutableMapOf<Name, LinkedHashSet<TypeParameterDescriptor>>()

    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
        return when {
            hasUniqueName(classifier) -> ClassifierNamePolicy.SHORT.renderClassifier(classifier, renderer)
            classifier is ClassDescriptor ||
                    classifier is TypeAliasDescriptor ->
                ClassifierNamePolicy.FULLY_QUALIFIED.renderClassifier(classifier, renderer)

            classifier is TypeParameterDescriptor -> {
                val name = classifier.name
                val typeParametersWithSameName = renderedParameters.getOrPut(name) { LinkedHashSet() }
                val isFirstOccurence = typeParametersWithSameName.add(classifier)
                val index = typeParametersWithSameName.indexOf(classifier)
                renderer.renderAmbiguousTypeParameter(classifier, index + 1, isFirstOccurence)
            }

            else -> error("Unexpected classifier: ${classifier::class.java}")
        }
    }

    private fun hasUniqueName(classifier: ClassifierDescriptor): Boolean {
        return classifier.name !in ambiguousNames
    }

    private fun DescriptorRenderer.renderAmbiguousTypeParameter(
        typeParameter: TypeParameterDescriptor, index: Int, firstOccurence: Boolean
    ) = buildString {
        append(typeParameter.name)
        append("#$index")
        if (firstOccurence) {
            append(renderMessage(" (type parameter of ${renderFqName(typeParameter.containingDeclaration.fqNameUnsafe)})"))
        }
    }
}

private fun collectMentionedClassifiersFqNames(contextObjects: Iterable<Any?>, result: MutableSet<FqNameUnsafe>) {
    fun CangJieType.addMentionedTypeConstructor() {
        constructor.declarationDescriptor?.let { result.add(it.fqNameUnsafe) }
    }

    contextObjects.filterIsInstance<CangJieType>().forEach { diagnosticType ->
        diagnosticType.contains { innerType ->
            innerType.addMentionedTypeConstructor()
            innerType.getAbbreviation()?.addMentionedTypeConstructor()
            false
        }
    }

    contextObjects.filterIsInstance<Iterable<*>>().forEach {
        collectMentionedClassifiersFqNames(it, result)
    }
    contextObjects.filterIsInstance<ClassifierDescriptor>().forEach {
        result.add(it.fqNameUnsafe)
    }
    contextObjects.filterIsInstance<TypeParameterDescriptor>().forEach {
        collectMentionedClassifiersFqNames(it.upperBounds, result)
    }
    contextObjects.filterIsInstance<CallableDescriptor>().forEach {
        collectMentionedClassifiersFqNames(
            listOf(
                it.typeParameters,
                it.returnType,
                it.valueParameters,
                it.dispatchReceiverParameter?.type,
                it.extensionReceiverParameter?.type
            ), result
        )
    }
}

