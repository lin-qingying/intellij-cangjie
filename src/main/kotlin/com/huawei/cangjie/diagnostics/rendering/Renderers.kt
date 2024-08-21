package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.builtins.fqNameUnsafe
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.renderer.ClassifierNamePolicy
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.getAbbreviation
import com.huawei.cangjie.types.util.contains
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

fun DescriptorRenderer.asRenderer() = SmartDescriptorRenderer(this)

object Renderers {
    private val LOG = Logger.getInstance(Renderers::class.java)

    @JvmField
    val NAME = Renderer<Named> { it.name.asString() }

    @JvmField
    val RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.FQ_NAMES_IN_TYPES.withOptions {
        parameterNamesInFunctionalTypes = false
    })

    @JvmField
    val FQ_NAMES_IN_TYPES = DescriptorRenderer.FQ_NAMES_IN_TYPES.asRenderer()

    @JvmField
    val STRING = Renderer<String> { it }

    @JvmField
    val VISIBILITY = Renderer<DescriptorVisibility> {
        it.externalDisplayName
    }

    @JvmField
    val DECL_FQNAME = Renderer<DeclarationDescriptor> {
        it.fqNameUnsafe.asString()
    }

    @JvmField
    val NAME_OF_CONTAINING_DECLARATION_OR_FILE = Renderer<DeclarationDescriptor> {
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

    @JvmField
    val TO_STRING = Renderer<Any> { element ->
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
    val ELEMENT_TEXT = Renderer<PsiElement> { it.text }

    @JvmField
    val FQNAME = Renderer<FqName> {
        it.asString()
    }

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

class SmartTypeRenderer(private val baseRenderer: DescriptorRenderer) : DiagnosticParameterRenderer<CangJieType> {
    override fun render(obj: CangJieType, renderingContext: RenderingContext): String {
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        return adaptiveRenderer.renderType(obj)
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

class SmartDescriptorRenderer(private val baseRenderer: DescriptorRenderer) :
    DiagnosticParameterRenderer<DeclarationDescriptor> {
    override fun render(obj: DeclarationDescriptor, renderingContext: RenderingContext): String {
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        return adaptiveRenderer.render(obj)
    }
}
