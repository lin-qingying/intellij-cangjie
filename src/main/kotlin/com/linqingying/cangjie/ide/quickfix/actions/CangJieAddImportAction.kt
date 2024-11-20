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

package com.linqingying.cangjie.ide.quickfix.actions

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.fqNameOrNull
import com.linqingying.cangjie.ide.CangJieDescriptorIconProvider
import com.linqingying.cangjie.ide.actions.ExpressionWeigher
import com.linqingying.cangjie.ide.imports.ImportInsertHelper
import com.linqingying.cangjie.ide.imports.getConstructors
import com.linqingying.cangjie.ide.imports.importableFqName
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.ide.quickfix.AutoImportVariant
import com.linqingying.cangjie.ide.quickfix.ImportFixHelper
import com.linqingying.cangjie.ide.quickfix.ImportPrioritizer
import com.linqingying.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.isOneSegmentFQN
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.references.CjSimpleNameReference
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.references.resolveMainReferenceToDescriptors
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.renderer.ParameterNameRenderingPolicy
import com.linqingying.cangjie.resolve.ImportPath
import com.linqingying.cangjie.resolve.caches.resolveImportReference
import com.linqingying.cangjie.utils.executeWriteCommand
import com.linqingying.cangjie.utils.fqname.ImportComparablePriority
import com.linqingying.cangjie.utils.runSynchronouslyWithProgress
import com.linqingying.cangjie.utils.underModalProgressOrUnderWriteActionWithNonCancellableProgressInDispatchThread
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.statistics.StatisticsInfo
import com.intellij.psi.statistics.StatisticsManager
import javax.swing.Icon
import com.linqingying.cangjie.psi.psiUtil.endOffset
import com.linqingying.cangjie.psi.psiUtil.startOffset

internal fun createSingleImportAction(
    project: Project,
    editor: Editor,
    element: CjElement,
    fqNames: Collection<FqName>
): CangJieAddImportAction {
    val file = element.getContainingCjFile()
    val prioritizer = createPrioritizerForFile(file)
    val expressionWeigher = ExpressionWeigher.createWeigher(element)

    val variants = fqNames.asSequence().mapNotNull { fqName ->
        val sameFqNameDescriptors = file.resolveImportReference(fqName)
        createVariantWithPriority(fqName, sameFqNameDescriptors, prioritizer, expressionWeigher, project)
    }.sortedWith(compareBy({ it.priority }, { it.variant.hint }))

    return CangJieAddImportAction(project, editor, element, variants)
}

private fun createVariantWithPriority(
    fqName: FqName,
    sameFqNameDescriptors: Collection<DeclarationDescriptor>,
    prioritizer: ImportPrioritizer,
    expressionWeigher: ExpressionWeigher,
    project: Project
): VariantWithPriority? {
    val descriptorsWithPriority =
        sameFqNameDescriptors.map { it to createDescriptorPriority(prioritizer, expressionWeigher, it) }.sortedBy { it.second }
    val priority = descriptorsWithPriority.firstOrNull()?.second ?: return null

    return VariantWithPriority(SingleImportVariant(fqName, descriptorsWithPriority.map { it.first }, project), priority)
}
internal data class VariantWithPriority(val variant: DescriptorBasedAutoImportVariant, val priority: ImportComparablePriority)
internal abstract class DescriptorBasedAutoImportVariant(
    val descriptorsToImport: Collection<DeclarationDescriptor>,
    override val fqName: FqName,
    project: Project,
) : AutoImportVariant {
    override val declarationToImport: PsiElement? = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptorsToImport.first())
    override val icon: Icon? = CangJieDescriptorIconProvider.getIcon(descriptorsToImport.first(), declarationToImport, 0)

    override val debugRepresentation: String
        get() = descriptorsToImport.first().variantNameForDebug()

    companion object {
        private val debugRenderer = DescriptorRenderer.DEBUG_TEXT.withOptions {
            annotationFilter = { false }
        }

        private fun DeclarationDescriptor.variantNameForDebug() = when (this) {
            is ClassDescriptor ->
                fqNameOrNull()?.toString()?.let { "class $it" } ?: debugRenderer.render(this)

            else ->
                debugRenderer.render(this)
        }
    }
}
/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 */
class CangJieAddImportAction internal constructor(
    private val project: Project,
    private val editor: Editor,
    private val element: CjElement,
    private val variants: Sequence<VariantWithPriority>
) : QuestionAction {
    private var singleImportVariant: DescriptorBasedAutoImportVariant? = null

    private fun variantsList(): List<DescriptorBasedAutoImportVariant> {
        if (singleImportVariant != null && !isUnitTestMode()) return listOf(singleImportVariant!!)

        val variantsList = {
            runReadAction {
                variants.sortedBy { it.priority }.map { it.variant }.toList()
            }
        }
        return if (isUnitTestMode()) {
            variantsList()
        } else {
            project.runSynchronouslyWithProgress(CangJieBundle.message("import.progress.text.resolve.imports"), true) {
                variantsList()
            }.orEmpty()
        }
    }

    fun showHint(): Boolean {
        val iterator = variants.iterator()
        if (!iterator.hasNext()) return false

        val first = iterator.next().variant
        val multiple = if (iterator.hasNext()) {
            true
        } else {
            singleImportVariant = first
            false
        }

        val hintText = ShowAutoImportPass.getMessage(multiple, getKind(first.declarationToImport), first.hint)
        HintManager.getInstance().showQuestionHint(editor, hintText, element.startOffset, element.endOffset, this)

        return true
    }

    private fun getKind(element: PsiElement?): String? {

        return null
    }

    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        if (!element.isValid) return false

        val variantsList = variantsList()

        if (variantsList.isEmpty()) return false

        if (variantsList.size == 1 || isUnitTestMode()) {
            addImport(variantsList.first())
            return true
        }

        return true
    }

    private fun addImport(variant: AutoImportVariant) {
        require(variant is DescriptorBasedAutoImportVariant)

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        psiDocumentManager.commitAllDocuments()

        project.executeWriteCommand(CangJieBundle.message("add.import")) {
            if (!element.isValid) return@executeWriteCommand

            val file = element.getContainingCjFile()

            val statisticsManager = StatisticsManager.getInstance()

            variant.descriptorsToImport.forEach { descriptor ->
                val statisticsInfo = CangJieStatisticsInfo.forDescriptor(descriptor)
                statisticsManager.incUseCount(statisticsInfo)

                // for class or package we use ShortenReferences because we not necessary insert an import but may want to
                // insert partly qualified name

                val importableFqName = descriptor.importableFqName
                val importAlias = importableFqName?.let { file.findAliasByFqName(it) }
                if (importableFqName?.isOneSegmentFQN() != true &&
                    (importAlias != null || descriptor is ClassDescriptor || descriptor is PackageViewDescriptor)
                ) {
                    if (element is CjSimpleNameExpression) {
                        if (importAlias != null) {
                            importAlias.nameIdentifier?.copy()?.let { element.identifier?.replace(it) }
                            val resultDescriptor = element.resolveMainReferenceToDescriptors().firstOrNull()
                            if (importableFqName == resultDescriptor?.importableFqName) {
                                return@forEach
                            }
                        }

                        if (importableFqName != null) {
                            underModalProgressOrUnderWriteActionWithNonCancellableProgressInDispatchThread(
                                project,
                                progressTitle = CangJieBundle.message("add.import.for.0", importableFqName.asString()),
                                computable = { element.mainReference.bindToFqName(importableFqName, CjSimpleNameReference.ShorteningMode.FORCED_SHORTENING) }
                            )
                        }
                    }
                } else {
                    ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
                }
            }
        }
    }
}

internal fun createPrioritizerForFile(file: CjFile, compareNames: Boolean = true): ImportPrioritizer {
    val isImportedByDefault = { fqName: FqName ->
        ImportInsertHelper.getInstance(file.project).isImportedWithDefault(ImportPath(fqName, isAllUnder = false), file)
    }
    return ImportPrioritizer(file, isImportedByDefault, compareNames)
}
  fun createDescriptorPriority(
    prioritizer: ImportPrioritizer,
    expressionWeigher: ExpressionWeigher,
    descriptor: DeclarationDescriptor
): ImportPrioritizer.Priority {

    return prioritizer.Priority(
        declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(prioritizer.file.project, descriptor),
        statisticsInfo = CangJieStatisticsInfo.forDescriptor(descriptor),
        isDeprecated = false,
        fqName = descriptor.importableFqName ?: error("Unexpected null for fully-qualified name of importable declaration"),
        expressionWeight = expressionWeigher.weigh(descriptor),
    )
}
object CangJieStatisticsInfo {
    private val SIGNATURE_RENDERER = DescriptorRenderer.withOptions {
        withDefinedIn = false
        withoutReturnType = true
        startFromName = true
        receiverAfterName = true
        modifiers = emptySet()
        defaultParameterValueRenderer = null
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    }

    fun forDescriptor(descriptor: DeclarationDescriptor, context: String = ""): StatisticsInfo {
        if (descriptor is ClassDescriptor) {
            return descriptor.importableFqName?.let { StatisticsInfo("", it.asString()) } ?: StatisticsInfo.EMPTY
        }

        val containerFqName = when (val container = descriptor.containingDeclaration) {
            is ClassDescriptor -> container.importableFqName?.asString()
            is PackageFragmentDescriptor -> container.fqName.asString()
            is ModuleDescriptor -> ""
            else -> null
        } ?: return StatisticsInfo.EMPTY
        val signature = SIGNATURE_RENDERER.render(descriptor)
        return StatisticsInfo(context, "$containerFqName###$signature")
    }
}

private class SingleImportVariant(
    fqName: FqName,
    descriptors: Collection<DeclarationDescriptor>,
    project: Project
) : DescriptorBasedAutoImportVariant(
    descriptorsToImport = listOf(
        descriptors.singleOrNull()
            ?: descriptors.minByOrNull { if (it is ClassDescriptor) 0 else 1 }
            ?: error("we create the class with not-empty descriptors always")
    ),
    fqName = fqName,
    project = project,
) {
    override val hint: String get() = fqName.asString()
}

internal fun createSingleImportActionForConstructor(
    project: Project,
    editor: Editor,
    element: CjElement,
    fqNames: Collection<FqName>
): CangJieAddImportAction {
    val file = element.getContainingCjFile()
    val prioritizer = createPrioritizerForFile(file)
    val expressionWeigher = ExpressionWeigher.createWeigher(element)

    val variants = fqNames.asSequence().mapNotNull { fqName ->
        val sameFqNameDescriptors = file.resolveImportReference(fqName.parent())
            .filterIsInstance<ClassifierDescriptor>()
            .flatMap { it.getConstructors() }
        createVariantWithPriority(fqName, sameFqNameDescriptors, prioritizer, expressionWeigher, project)
    }

    return CangJieAddImportAction(project, editor, element, variants)
}
