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

package com.linqingying.cangjie.ide.intentions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.template.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.suggested.createSmartPointer
import com.linqingying.cangjie.builtins.StandardNames
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.PropertyDescriptor
import com.linqingying.cangjie.descriptors.annotations.fqNameOrNull
import com.linqingying.cangjie.diagnostics.Severity
import com.linqingying.cangjie.ide.IdeDescriptorRenderers
import com.linqingying.cangjie.ide.ShortenReferences
import com.linqingying.cangjie.ide.codeinsight.utils.ChooseValueExpression
import com.linqingying.cangjie.ide.isFlexibleRecursive
import com.linqingying.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.resolve.DescriptorToSourceUtils
import com.linqingying.cangjie.resolve.caches.analyze
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.scopes.getResolutionScope
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.getResolvableApproximations
import com.linqingying.cangjie.types.util.makeNotNullable
import com.linqingying.cangjie.utils.runWriteActionIfPhysical

class SpecifyTypeExplicitlyIntention : SelfTargetingRangeIntention<CjCallableDeclaration>(
    CjCallableDeclaration::class.java,
    CangJieBundle.lazyMessage("specify.type.explicitly")
), HighPriorityAction {
    override fun applicabilityRange(element: CjCallableDeclaration): TextRange? {
//        if (!ExplicitApiDeclarationChecker.returnTypeCheckIsApplicable(element)) return null
        if (element is CjParameter && element.isLoopParameter && element.destructuringDeclaration != null) return null

        val context = element.analyze(BodyResolveMode.PARTIAL)
        if (context.diagnostics.forElement(element).any { it.severity == Severity.ERROR }) return null

        // If ApiMode is on, then this intention duplicates corresponding quickfix for compiler error
        // and we disable it here.
//        if (ExplicitApiDeclarationChecker.publicReturnTypeShouldBePresentInApiMode(
//                element,
//                element.languageVersionSettings,
//                element.resolveToDescriptorIfAny()
//            )
//        ) return null

        setTextGetter(
            if (element is CjFunction)
                CangJieBundle.lazyMessage("specify.return.type.explicitly")
            else
                defaultTextGetter
        )

        val initializer = (element as? CjDeclarationWithInitializer)?.initializer
        return if (initializer != null) {
            TextRange(element.startOffset, initializer.startOffset - 1)
        } else {
            TextRange(element.startOffset, element.endOffset)
        }
    }

    override fun applyTo(element: CjCallableDeclaration, editor: Editor?) {
        val type = getTypeForDeclaration(element)
        if (type.isError) {
            if (editor != null && editor !is ImaginaryEditor) {
                HintManager.getInstance().showErrorHint(editor, CangJieBundle.message("cannot.infer.type.for.this.declaration"))
            }
            return
        }

        addTypeAnnotation(editor, element, type)
    }

    companion object {
        private val PropertyDescriptor.setterType: CangJieType?
            get() = setter?.valueParameters?.firstOrNull()?.type?.let { if (it.isError) null else it }

        fun dangerousFlexibleTypeOrNull(
            declaration: CjCallableDeclaration, publicAPIOnly: Boolean, reportPlatformArguments: Boolean
        ): CangJieType? {
            when (declaration) {
                is CjFunction -> if (declaration.isLocal || declaration.hasDeclaredReturnType()) return null
                is CjProperty -> if (declaration.isLocal || declaration.typeReference != null) return null
                else -> return null
            }

            if (declaration.containingTypeStatement?.isLocal == true) return null

            val callable = declaration.resolveToDescriptorIfAny() as? CallableDescriptor ?: return null
            if (publicAPIOnly && !callable.visibility.isPublicAPI) return null
            val type = callable.returnType ?: return null
            if (type.isDynamic()) return null
            if (reportPlatformArguments) {
                if (!type.isFlexibleRecursive()) return null
            } else {
                if (!type.isFlexible()) return null
            }

            return type
        }

        fun getTypeForDeclaration(declaration: CjCallableDeclaration): CangJieType {
            val descriptor = declaration.resolveToDescriptorIfAny()
            val type = (descriptor as? CallableDescriptor)?.let {
                if (it.overriddenDescriptors.firstOrNull()?.returnType?.isMarkedOption == false)
                    it.returnType?.makeNotNullable()
                else
                    it.returnType
            }

            if (type != null && type.isError && descriptor is PropertyDescriptor) {
                return descriptor.setterType ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_FUNCTION, declaration.text)
            }

            return type ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_FUNCTION, declaration.text)
        }

        fun createTypeExpressionForTemplate(
            exprType: CangJieType,
            contextElement: CjDeclaration,
            useTypesFromOverridden: Boolean = false
        ): Expression? {
            val resolutionFacade = contextElement.getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(contextElement, BodyResolveMode.PARTIAL)
            val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)

            fun CangJieType.toResolvableApproximations(): List<CangJieType> =
                with(getResolvableApproximations(scope, checkTypeParameters = false).toList()) {
                    when {
                        exprType.isNullabilityFlexible() -> flatMap {
                            listOf(TypeUtils.makeNotNullable(it), TypeUtils.makeOptional(it))
                        }
                        else -> this
                    }
                }

            val overriddenTypes: List<CangJieType> = if (!useTypesFromOverridden) {
                null
            } else {
                val declarationDescriptor = contextElement.resolveToDescriptorIfAny() as? CallableDescriptor
                declarationDescriptor?.overriddenDescriptors?.mapNotNull { it.returnType }
            } ?: emptyList()

            val types = (listOf(exprType) + overriddenTypes).distinct().flatMap {
                it.toResolvableApproximations()
            }.ifEmpty { return null }

            if (isUnitTestMode()) {
                // This helps to be sure no nullable types are suggested
                if (contextElement.containingCjFile.findDescendantOfType<PsiComment>()?.takeIf {
                        it.text == "// CHOOSE_NULLABLE_TYPE_IF_EXISTS"
                    } != null) {
                    val targetType = types.firstOrNull { it.isMarkedOption } ?: types.first()
                    return TypeChooseValueExpression(listOf(targetType), targetType)
                }
                // This helps to be sure something except Nothing is suggested
                if (contextElement.containingCjFile.findDescendantOfType<PsiComment>()?.takeIf {
                        it.text == "// DO_NOT_CHOOSE_NOTHING"
                    } != null
                ) {
                    val targetType = types.firstOrNull { !CangJieBuiltIns.isNothingOrNullableNothing(it) } ?: types.first()
                    return TypeChooseValueExpression(listOf(targetType), targetType)
                }
            }

            return TypeChooseValueExpression(types, types.first())
        }

        // Explicit class is used because of KT-20460
        private class TypeChooseValueExpression(
            items: List<CangJieType>, defaultItem: CangJieType
        ) : ChooseValueExpression<CangJieType>(items, defaultItem) {
            override fun getLookupString(element: CangJieType) =
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(element)

            override fun getResult(element: CangJieType): String {
                val renderType = IdeDescriptorRenderers.FQ_NAMES_IN_TYPES_WITH_NORMALIZER.renderType(element)
                val descriptor = element.constructor.declarationDescriptor
                if (descriptor?.fqNameOrNull()?.asString() == renderType) {
                    val className = (DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? CjClass)?.nameIdentifier?.text
                    if (className != null && className != className.unquoteCangJieIdentifier()) {
                        return className
                    }
                }

                return renderType
            }
        }

        fun addTypeAnnotation(editor: Editor?, declaration: CjCallableDeclaration, exprType: CangJieType) {
            if (editor != null) {
                addTypeAnnotationWithTemplate(editor, declaration, exprType)
            } else {
                declaration.setType(exprType)
            }
        }

        @JvmOverloads
        fun createTypeReferencePostprocessor(
            declaration: CjCallableDeclaration,
            iterator: Iterator<CjCallableDeclaration>? = null,
            editor: Editor? = null
        ): TemplateEditingAdapter = object : TemplateEditingAdapter() {
            override fun templateFinished(template: Template, brokenOff: Boolean) {
                val typeRef = declaration.typeReference
                if (typeRef != null && typeRef.isValid) {
                    runWriteActionIfPhysical(typeRef) {
                        ShortenReferences.DEFAULT.process(typeRef)
                        if (iterator != null && editor != null) addTypeAnnotationWithTemplate(editor, iterator)
                    }
                }
            }
        }

        fun addTypeAnnotationWithTemplate(editor: Editor, iterator: Iterator<CjCallableDeclaration>?) {
            if (iterator == null || !iterator.hasNext()) return
            val declaration = iterator.next()
            val exprType = getTypeForDeclaration(declaration)
            addTypeAnnotationWithTemplate(editor, declaration, exprType, iterator)
        }

        private fun addTypeAnnotationWithTemplate(
            editor: Editor, declaration: CjCallableDeclaration, exprType: CangJieType,
            iterator: Iterator<CjCallableDeclaration>? = null
        ) {
            assert(!exprType.isError) { "Unexpected error type, should have been checked before: " + declaration.getElementTextWithContext() + ", type = " + exprType }

            val project = declaration.project
            val expression = createTypeExpressionForTemplate(exprType, declaration, useTypesFromOverridden = true) ?: return

            declaration.setType(StandardNames.FqNames.anyUFqName.asString())
            val declarationPointer = declaration.createSmartPointer()

            // May invalidate declaration
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

            val newDeclaration = declarationPointer.element ?: return

            val newTypeRef = newDeclaration.typeReference ?: return
            val builder = TemplateBuilderImpl(newTypeRef)
            builder.replaceElement(newTypeRef, expression)

            editor.caretModel.moveToOffset(newTypeRef.node.startOffset)

            TemplateManager.getInstance(project).startTemplate(
                editor,
                builder.buildInlineTemplate(),
                createTypeReferencePostprocessor(newDeclaration, iterator, editor)
            )
        }
    }
}

