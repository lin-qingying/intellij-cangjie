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

package org.cangnova.cangjie.quickfix.overrideImplement

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.findDocComment
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.renderer.*
import org.cangnova.cangjie.renderer.DescriptorRenderer.Companion.withOptions
import org.cangnova.cangjie.resolve.setSingleOverridden
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.template.TemplateKind
import org.cangnova.cangjie.template.getFunctionBodyTextFromTemplate
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.approximateFlexibleTypes

interface OverrideMemberChooserObject : ClassMember {

    val descriptor: CallableMemberDescriptor
    val immediateSuper: CallableMemberDescriptor
    val bodyType: BodyType
    val preferConstructorParameter: Boolean

    companion object {
        fun create(
            project: Project,
            descriptor: CallableMemberDescriptor,
            immediateSuper: CallableMemberDescriptor,
            bodyType: BodyType,
            preferConstructorParameter: Boolean = false
        ): OverrideMemberChooserObject {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
            return if (declaration != null) {
                create(declaration, descriptor, immediateSuper, bodyType, preferConstructorParameter)
            } else {
                WithoutDeclaration(descriptor, immediateSuper, bodyType, preferConstructorParameter)
            }
        }

        fun create(
            declaration: PsiElement,
            descriptor: CallableMemberDescriptor,
            immediateSuper: CallableMemberDescriptor,
            bodyType: BodyType,
            preferConstructorParameter: Boolean = false
        ): OverrideMemberChooserObject =
            WithDeclaration(descriptor, declaration, immediateSuper, bodyType, preferConstructorParameter)

        private class WithDeclaration(
            descriptor: CallableMemberDescriptor,
            declaration: PsiElement,
            override val immediateSuper: CallableMemberDescriptor,
            override val bodyType: BodyType,
            override val preferConstructorParameter: Boolean
        ) : DescriptorMemberChooserObject(declaration, descriptor), OverrideMemberChooserObject {

            override val descriptor: CallableMemberDescriptor
                get() = super.descriptor as CallableMemberDescriptor
        }

        private class WithoutDeclaration(
            override val descriptor: CallableMemberDescriptor,
            override val immediateSuper: CallableMemberDescriptor,
            override val bodyType: BodyType,
            override val preferConstructorParameter: Boolean
        ) : MemberChooserObjectBase(
            DescriptorMemberChooserObject.getText(descriptor), DescriptorMemberChooserObject.getIcon(null, descriptor)
        ), OverrideMemberChooserObject {

            override fun getParentNodeDelegate(): MemberChooserObject? {
                val parentClassifier = descriptor.containingDeclaration as? ClassifierDescriptor ?: return null
                return MemberChooserObjectBase(
                    DescriptorMemberChooserObject.getText(parentClassifier),
                    DescriptorMemberChooserObject.getIcon(null, parentClassifier)
                )
            }
        }
    }
}

fun OverrideMemberChooserObject.generateMember(
    targetClass: CjTypeStatement,
    copyDoc: Boolean
) = generateMember(targetClass, copyDoc, targetClass.project, mode = MemberGenerateMode.OVERRIDE)

fun OverrideMemberChooserObject.generateMember(
    targetClass: CjTypeStatement?,
    copyDoc: Boolean,
    project: Project,
    mode: MemberGenerateMode
): CjCallableDeclaration {
    val descriptor = immediateSuper

    val bodyType = when {
//        targetClass?.hasExpectModifier() == true -> BodyType.NoBody
        descriptor.extensionReceiverParameter != null && mode == MemberGenerateMode.OVERRIDE -> BodyType.FromTemplate
        else -> bodyType
    }

    val baseRenderer = when (mode) {
        MemberGenerateMode.OVERRIDE -> createOverrideRenderer(targetClass?.getContainingCjFile())
//        MemberGenerateMode.ACTUAL -> createActualRenderer(targetClass?.containingCjFile)
//        MemberGenerateMode.EXPECT -> createExpectRenderer(targetClass?.containingCjFile)
    }
    val renderer = baseRenderer.withOptions {
        if (descriptor is ClassConstructorDescriptor && descriptor.isPrimary) {

        }

//        if (MemberGenerateMode.OVERRIDE != mode) {
//            val languageVersionSettings = targetClass?.module?.languageVersionSettings ?: project.languageVersionSettings
//            if (languageVersionSettings.explicitApiEnabled && !explicitVisibilityIsNotRequired(this@generateMember.descriptor)) {
//                renderDefaultVisibility = true
//            }
//        }
    }

//    if (preferConstructorParameter && descriptor is PropertyDescriptor) {
//        return generateConstructorParameter(project, descriptor, renderer, mode == MemberGenerateMode.OVERRIDE)
//    }

    val newMember: CjCallableDeclaration = when (descriptor) {
        is FunctionDescriptor -> generateFunction(
            project,
            descriptor,
            renderer,
            bodyType,
            mode == MemberGenerateMode.OVERRIDE
        )
        is PropertyDescriptor -> generateProperty(project, descriptor, renderer, bodyType, mode == MemberGenerateMode.OVERRIDE)
        else -> error("Unknown member to override: $descriptor")
    }

//    when (mode) {
//        MemberGenerateMode.ACTUAL -> newMember.addModifier(CjTokens.ACTUAL_KEYWORD)
//        MemberGenerateMode.EXPECT -> if (targetClass == null) {
//            newMember.addModifier(CjTokens.EXPECT_KEYWORD)
//        }
//        MemberGenerateMode.OVERRIDE -> {
//            if (targetClass?.hasActualModifier() == true) {
//                val expectClassDescriptors =
//                    targetClass.resolveToDescriptorIfAny()?.expectedDescriptors()?.filterIsInstance<ClassDescriptor>().orEmpty()
//                if (expectClassDescriptors.any { expectClassDescriptor ->
//                        val expectMemberDescriptor = expectClassDescriptor.findCallableMemberBySignature(immediateSuper)
//                        expectMemberDescriptor?.isExpect == true &&
//                                expectMemberDescriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
//                    }
//                ) {
//                    newMember.addModifier(CjTokens.ACTUAL_KEYWORD)
//                }
//            }
//        }
//    }

    if (copyDoc) {
        val kDoc = when (val superDeclaration =
            DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)?.navigationElement) {
            is CjDeclaration ->
                findDocComment(superDeclaration)
//            is PsiDocCommentOwner -> {
//                val kDocText = superDeclaration.docComment?.let { IdeaDocCommentConverter.convertDocComment(it) }
//                if (kDocText.isNullOrEmpty()) null else CDocElementFactory(project).createCDocFromText(kDocText)
//            }
            else -> null
        }
        if (kDoc != null) {
            newMember.addAfter(kDoc, null)
        }
    }

    return newMember
}

enum class MemberGenerateMode {
    OVERRIDE,
//    ACTUAL,
//    EXPECT
}

private fun createOverrideRenderer(file: CjFile?) = withOptions {
    defaultParameterValueRenderer = null
    modifiers = setOf(DescriptorRendererModifier.OVERRIDE, DescriptorRendererModifier.ANNOTATIONS, DescriptorRendererModifier.VISIBILITY)
    withDefinedIn = false
    classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
    overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
    unitReturnType = false
    enhancedTypes = true
    typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
    renderUnabbreviatedType = false
//    annotationFilter = fun(annotation: AnnotationDescriptor): Boolean {
//        // Keep an annotation if the annotation itself is marked with @RequiresOptIn (or the old @Experimental), or otherwise if an
//        // extension wants to keep it.
////        val annotations = annotation.type.constructor.declarationDescriptor?.annotations
////        if (annotations?.hasAnnotation(OptInNames.REQUIRES_OPT_IN_FQ_NAME) == true ||
////            annotations?.hasAnnotation(StandardNames.FqNames.OptInFqNames.OLD_EXPERIMENTAL_FQ_NAME) == true) {
////            return true
////        }
//
//        val fqn = annotation.fqName
//        if (fqn != null && file != null) {
//            return keepAnnotationOnOverrideMember(fqn.asString(), file)
//        }
//
//        return false
//    }
    presentableUnresolvedTypes = true
    informativeErrorType = false
}

private fun generateFunction(
    project: Project,
    descriptor: FunctionDescriptor,
    renderer: DescriptorRenderer,
    bodyType: BodyType,
    forceOverride: Boolean
): CjFunction {
    val newDescriptor = descriptor.wrap(forceOverride)

    val returnType = descriptor.returnType
    val returnsNotUnit = returnType != null && !CangJieBuiltIns.isUnit(returnType)

    val body = if (bodyType != BodyType.NoBody) {
        val delegation = generateUnsupportedOrSuperCall(project, descriptor, bodyType, !returnsNotUnit)
        val returnPrefix = if (returnsNotUnit && bodyType.requiresReturn) "return " else ""
        "{$returnPrefix$delegation\n}"
    } else ""

    val factory = CjPsiFactory(project)
    val functionText = "${renderer.render(newDescriptor)} $body"
    return when (descriptor) {
//        is ClassConstructorDescriptor -> {
//            if (descriptor.isPrimary) {
//                factory.createPrimaryConstructor(functionText)
//            } else {
//                factory.createSecondaryConstructor(functionText)
//            }
//        }
        else -> factory.createFunction(functionText)
    }
}

private fun ClassConstructorDescriptor.wrap(): ClassConstructorDescriptor {
    return object : ClassConstructorDescriptor by this {
        override val modality  = Modality.FINAL

        override val returnType: CangJieType
            get() =             this@wrap.returnType.approximateFlexibleTypes(preferNotNull = true, preferStarForRaw = true)
        override val overriddenDescriptors: Collection<FunctionDescriptor>
            get() = emptyList()

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
            return visitor.visitConstructorDescriptor(this, data)
        }
    }
}

private fun FunctionDescriptor.wrap(forceOverride: Boolean): FunctionDescriptor {
    if (this is ClassConstructorDescriptor) return this.wrap()
    return object : FunctionDescriptor by this {
        override val modality: Modality
            get() =  if (forceOverride) Modality.OPEN else this@wrap.modality
        override val returnType: CangJieType?
            get() =    this@wrap.returnType?.approximateFlexibleTypes(preferNotNull = true, preferStarForRaw = true)
        override val overriddenDescriptors: Collection<FunctionDescriptor>
            get() =  if (forceOverride) listOf(this@wrap) else this@wrap.overriddenDescriptors

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {


            return visitor.visitFunctionDescriptor(this, data)
        }
    }
}

fun generateUnsupportedOrSuperCall(
    project: Project,
    descriptor: CallableMemberDescriptor,
    bodyType: BodyType,
    canBeEmpty: Boolean = true
): String {
    when (bodyType.effectiveBodyType(canBeEmpty)) {
        BodyType.EmptyOrTemplate -> return ""
        BodyType.FromTemplate -> {
            val templateKind =
                if (descriptor is FunctionDescriptor) TemplateKind.FUNCTION else TemplateKind.PROPERTY_INITIALIZER
            return getFunctionBodyTextFromTemplate(
                project,
                templateKind,
                descriptor.name.asString(),
                descriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: "Unit",
                null
            )
        }

        else -> return buildString {
            if (bodyType is BodyType.Delegate) {
                append(bodyType.receiverName)
            } else {
                append("super")
                if (bodyType == BodyType.QualifiedSuper) {
                    val superClassFqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(
                        descriptor.containingDeclaration as ClassifierDescriptor
                    )
                    append("<").append(superClassFqName).append(">")
                }
            }
            append(".").append(descriptor.name.render())

            if (descriptor is FunctionDescriptor) {
                val paramTexts = descriptor.valueParameters.map {
//                    val renderedName = it.name.render()
//                    if (it.varargElementType != null) "*$renderedName" else renderedName
                }
                paramTexts.joinTo(this, prefix = "(", postfix = ")")
            }
        }
    }
}
private fun generateProperty(
    project: Project,
    descriptor: PropertyDescriptor,
    renderer: DescriptorRenderer,
    bodyType: BodyType,
    forceOverride: Boolean
): CjProperty {
    val newDescriptor = descriptor.wrap(forceOverride)

    val returnType = descriptor.returnType
    val returnsNotUnit = returnType != null && !CangJieBuiltIns.isUnit(returnType)

    val body =
        if (bodyType != BodyType.NoBody) {
            buildString {
                append("{")
                append("\nget(){")

                append(generateUnsupportedOrSuperCall(project, descriptor, bodyType, !returnsNotUnit))
                append("}")
                if (descriptor.isVar) {
                    append("\nset(value) {}")
                }
                append("}")

            }
        } else ""
    return CjPsiFactory(project).createProperty(renderer.render(newDescriptor) + body)
}
private fun PropertyDescriptor.wrap(forceOverride: Boolean): PropertyDescriptor {
    val delegate = copy(containingDeclaration, if (forceOverride) Modality.OPEN else modality, visibility, kind, true) as PropertyDescriptor
    val newDescriptor = object : PropertyDescriptor by delegate {

    }
    if (forceOverride) {
        newDescriptor.setSingleOverridden(this)
    }
    return newDescriptor
}
