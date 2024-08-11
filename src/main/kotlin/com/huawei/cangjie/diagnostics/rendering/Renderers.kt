package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.builtins.fqNameUnsafe
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.renderer.ClassifierNamePolicy
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.getAbbreviation
import com.huawei.cangjie.types.util.contains
import com.intellij.psi.PsiElement
import java.util.LinkedHashSet
fun DescriptorRenderer.asRenderer() = SmartDescriptorRenderer(this)

object Renderers {

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
    val ELEMENT_TEXT = Renderer<PsiElement> { it.text }
}

val RenderingContext.adaptiveClassifierPolicy: ClassifierNamePolicy
    get() = this[ADAPTIVE_CLASSIFIER_POLICY_KEY]
private fun collectClassifiersFqNames(objectsToRender: Collection<Any?>): Set<FqNameUnsafe> = LinkedHashSet<FqNameUnsafe>().apply {
    collectMentionedClassifiersFqNames(objectsToRender, this)
}
private val ADAPTIVE_CLASSIFIER_POLICY_KEY = object : RenderingContext.Key<ClassifierNamePolicy>("ADAPTIVE_CLASSIFIER_POLICY") {
    override fun compute(objectsToRender: Collection<Any?>): ClassifierNamePolicy {
        val ambiguousNames =
            collectClassifiersFqNames(objectsToRender).groupBy { it.shortNameOrSpecial() }.filter { it.value.size > 1 }.map { it.key }
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

class SmartDescriptorRenderer(private val baseRenderer: DescriptorRenderer) : DiagnosticParameterRenderer<DeclarationDescriptor> {
    override fun render(obj: DeclarationDescriptor, renderingContext: RenderingContext): String {
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        return adaptiveRenderer.render(obj)
    }
}
