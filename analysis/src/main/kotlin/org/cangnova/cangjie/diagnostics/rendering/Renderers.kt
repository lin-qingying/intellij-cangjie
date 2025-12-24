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

package org.cangnova.cangjie.diagnostics.rendering


import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.MatchMissingCase
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.identifier
import org.cangnova.cangjie.renderer.ClassifierNamePolicy
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.MemberComparator

import org.cangnova.cangjie.types.CangJieType

import org.cangnova.cangjie.types.getAbbreviation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.types.contains
import org.cangnova.cangjie.types.fqNameUnsafe


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

    
    val NAME = renderer<Name> { it.asString() }

    
    val INT = renderer<Int> {
        it.toString()
    }

    
    val AMBIGUOUS_CALLABLE_REFERENCES = renderer { references: Collection<CallableDescriptor> ->
        renderAmbiguousDescriptors(references)
    }

    
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

    
    val TYPE_AND_NAMED = renderer<DeclarationDescriptor> {

        getDescriptorName(it) + ":" + NAME.render(it.name)
    }

    
    val NAMED = renderer<Named> {
        NAME.render(it.name)
    }

    
    val RENDER_CLASS_OR_OBJECT_NAME = renderer<ClassifierDescriptorWithTypeParameters> { it.renderKindWithName() }

    
    val COMPACT_WITHOUT_SUPERTYPES = DescriptorRenderer.COMPACT_WITHOUT_SUPERTYPES.asRenderer()

    
    val RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.FQ_NAMES_IN_TYPES.withOptions {
        parameterNamesInFunctionalTypes = false
    })

    private val List<MatchMissingCase>.assumesElseBranchOnly: Boolean
        get() = any { it == MatchMissingCase.Unknown || it is MatchMissingCase.ConditionTypeIsExpect }


    //    private val List<Pattern>.assumesElseBranchOnlyByPattern: Boolean
//        get() = any { it.kind is PatternKind.Enum }
    private val MATCH_MISSING_LIMIT = 7

//    
//    val RENDER_MATCH_MISSING_CASES_PATTERN = renderer<List<Pattern>> {
//
//
////        if (it.assumesElseBranchOnlyByPattern) {
//        val list = it.joinToString(", ", limit = MATCH_MISSING_LIMIT) {
////            "'${it.kind.showString()}'"
//            "'${it.text(null)}'"
//
//        }
//        val branches = if (it.size > 1) "branches" else "branch"
//        "$list $branches or 'else' branch instead"
////        } else {
////            "'else' branch"
////        }
//
//    }

    
    val RENDER_MATCH_MISSING_CASES = renderer<List<MatchMissingCase>> {
        if (!it.assumesElseBranchOnly) {
            val list = it.joinToString(", ", limit = MATCH_MISSING_LIMIT) { "'$it'" }
            val branches = if (it.size > 1) "branches" else "branch"
            "$list $branches or 'else' branch instead"
        } else {
            "'else' branch"
        }
    }

    
    val CLASS_NAME = renderer { cclass: ClassDescriptor ->


        NAME.render(cclass.name)
    }

    
    val NAMES_TO_STRING = renderer { names: Collection<Name> ->
        names.joinToString(", ", "{", "}") { type ->

            NAME.render(type)
        }

    }

    
    val VISIBLITYS_NAMES = renderer {

            visiblitys: List<DescriptorVisibility> ->
        visiblitys.joinToString(" or ") {

            "'${it.name}'"
        }


    }

    
    val RENDER_COLLECTION_OF_TYPES = renderer { types: List<CangJieType> ->

        types.joinToString(", ", "{ ", " }") { type ->

            RENDER_TYPE.render(type, RenderingContext.of(type))
        }


    }

    
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

    
    val FQ_NAMES_IN_TYPES = DescriptorRenderer.FQ_NAMES_IN_TYPES.asRenderer()

    
    val COMPACT_WITH_MODIFIERS = DescriptorRenderer.COMPACT_WITH_MODIFIERS.asRenderer()

    
    val DESCRIPTOR_KIND_NAME = renderer { kind: DescriptorKind ->
        kind.kind
    }

    

    val PSI_NAMED_TYPE_NAM = renderer { declaration: CjDeclaration ->

        when (declaration) {
            is CjNamedFunction -> "function"
            is CjProperty -> "property"
            is CjVariable<*> -> "variable"
            else -> ""
        }
    }

    //根据表达式类型显示
    
    val EXPRESSION_TYPE_TEXT = renderer<PsiElement> {

        when (it) {
            is CjThisExpression -> "this"
            is CjSuperExpression -> "super"
            else -> ""
        }
    }

    
    val MODALITY_NAME = renderer { modality: Modality ->
        modality.name.lowercase()
    }

    
    val VISIBILITY = renderer<DescriptorVisibility> {
        it.externalDisplayName
    }

    
    val DECL_FQNAME = renderer<DeclarationDescriptor> {
        it.fqNameUnsafe.asString()
    }

    
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

//    
//    val AMBIGUOUS_CALLS = renderer { calls: Collection<ResolvedCall<*>> ->
//        val descriptors = calls.map { it.resultingDescriptor }
//        renderAmbiguousDescriptors(descriptors)
//    }

    
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

    
    val ELEMENT_TEXT = renderer<PsiElement> {
        it.text
    }

    
    val ELEMENT_IDENTIFIER_TEXT = renderer<PsiElement> {
        it.identifier?.text ?: ""
    }

    
    val FQNAMES = renderer<List<FqName>> {
        it.joinToString(" , ") { it.asString() }
    }

    
    val FQNAME = renderer<FqName> {
        it.asString()
    }

    
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

