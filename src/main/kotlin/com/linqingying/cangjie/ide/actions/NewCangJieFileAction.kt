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

package com.linqingying.cangjie.ide.actions

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.ide.configuration.CangJieProjectConfigurator
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.parsing.CangJieParserDefinition.Util.STD_SCRIPT_SUFFIX
import com.linqingying.cangjie.psi.CjClass
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.linqingying.cangjie.psi.psiUtil.startOffset
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.actions.CreateFromTemplateAction
import com.intellij.ide.actions.CreateTemplateInPackageAction
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.linqingying.utils.toCamelCase
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import java.util.*


internal const val CANGJIE_WORKSHEET_TEMPLATE_NAME: String = "CangJie Worksheet"
const val CANGJIE_WORKSHEET_EXTENSION: String = "ws.cj"

class NewCangJieFileAction : AbstractNewCangJieFileAction(), DumbAware {


    override fun isAvailable(dataContext: DataContext): Boolean {
        if (!super.isAvailable(dataContext)) return false
        val ideView = LangDataKeys.IDE_VIEW.getData(dataContext) ?: return false
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
        val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        return ideView.directories.any {
            projectFileIndex.isInSourceContent(it.virtualFile) ||
                    CreateTemplateInPackageAction.isInContentRoot(it.virtualFile, projectFileIndex)
        }
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
//        val sealedTemplatesEnabled = RegistryManager.getInstance().`is`("cangjie.create.sealed.templates.enabled")
        builder.setTitle(CangJieBundle.message("action.new.file.dialog.title"))

        builder
            .addKind(
                CangJieBundle.message("action.new.file.dialog.class.title"),
//
                CangJieIcons.CLASS,
                "CangJie Class"
            )
            .addKind(
                CangJieBundle.message("action.new.file.dialog.file.title"),
                CangJieIcons.CANGJIE_FILE,
                "CangJie File"
            )
            .addKind(
                CangJieBundle.message("action.new.file.dialog.interface.title"),
                CangJieIcons.INTERFACE,
                "CangJie Interface"
            )
            .addKind(
                CangJieBundle.message("action.new.file.dialog.enum.title"),
                CangJieIcons.ENUM,
                "CangJie Enum"
            )

            .addKind(
                CangJieBundle.message("action.new.file.dialog.struct.title"),
                CangJieIcons.STRUCT,
                "CangJie Struct"
            )
            .addKind(
                CangJieBundle.message("action.new.file.dialog.extend.title"),
                CangJieIcons.CANGJIE_FILE,
                "CangJie Extend"
            )
        builder.setValidator(NewCangJieFileNameValidator)

    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String =
        CangJieBundle.message("action.CangJie.NewFile.text")

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other is NewCangJieFileAction
}


abstract class AbstractNewCangJieFileAction : CreateFileFromTemplateAction() {

    private fun CjFile.editor(): Editor? =
        FileEditorManager.getInstance(this.project).selectedTextEditor?.takeIf { it.document == this.viewProvider.document }

    override fun postProcess(
        createdElement: PsiFile,
        templateName: String?,
        customProperties: MutableMap<String, String>?
    ) {
        super.postProcess(createdElement, templateName, customProperties)

        val module = ModuleUtilCore.findModuleForPsiElement(createdElement)
        if (createdElement is CjFile) {
            if (module != null) {
                for (hook in NewCangJieFileHook.EP_NAME.extensions) {
                    hook.postProcess(createdElement, module)
                }
            }

            val cjClass = createdElement.declarations.singleOrNull() as? CjNamedDeclaration
            if (cjClass != null) {
                if (cjClass is CjClass) {
                    val primaryConstructor = cjClass.primaryConstructor
                    if (primaryConstructor != null) {
                        createdElement.editor()?.caretModel?.moveToOffset(primaryConstructor.startOffset + 1)
                        return
                    }
                }
                CreateFromTemplateAction.moveCaretAfterNameIdentifier(cjClass)
            } else {
                val editor = createdElement.editor() ?: return
                val lineCount = editor.document.lineCount
                if (lineCount > 0) {
                    editor.caretModel.moveToLogicalPosition(LogicalPosition(lineCount - 1, 0))
                }
            }
        }
    }


    override fun startInWriteAction() = false

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
        val targetTemplate = if (CANGJIE_WORKSHEET_TEMPLATE_NAME != template.name) {
            template
        } else {
            object : FileTemplate by template {
                override fun getExtension(): String = CANGJIE_WORKSHEET_EXTENSION
            }
        }
        return createFileFromTemplateWithStat(name, targetTemplate, dir)
    }
}


object NewCangJieFileNameValidator : InputValidatorEx {
    override fun getErrorText(inputString: String): String? {
        if (inputString.trim().isEmpty()) {
            return CangJieBundle.message("action.new.file.error.empty.name")
        }

        val parts: List<String> = inputString.split(*FQNAME_SEPARATORS)
        if (parts.any { it.trim().isEmpty() }) {
            return CangJieBundle.message("action.new.file.error.empty.name.part")
        }

        return null
    }

    override fun checkInput(inputString: String): Boolean = true

    override fun canClose(inputString: String): Boolean = getErrorText(inputString) == null
}

private val FQNAME_SEPARATORS: CharArray = charArrayOf('/', '\\', '.')
private val FILE_SEPARATORS: CharArray = charArrayOf('/', '\\')


abstract class NewCangJieFileHook {
    companion object {
        val EP_NAME: ExtensionPointName<NewCangJieFileHook> =
            ExtensionPointName.create("com.linqingying.cangjie.newFileHook")
    }

    abstract fun postProcess(createdElement: CjFile, module: Module)
}


internal fun createFileFromTemplateWithStat(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
//    CangJieJ2KOnboardingFUSCollector.logFirstCjFileCreated(dir.project) // implementation checks if it is actually the first
//    CangJieCreateFileFUSCollector.logFileTemplate(template.name)
    return createCangJieFileFromTemplate(name, template, dir)
}

internal fun createCangJieFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
    val directorySeparators = when (template.name) {
        "CangJie File" -> FILE_SEPARATORS
        "CangJie Worksheet" -> FILE_SEPARATORS
        "CangJie Script" -> FILE_SEPARATORS
        else -> FQNAME_SEPARATORS
    }

    val (className, targetDir) = findOrCreateTarget(dir, name, directorySeparators)


    template.fileName = name

    val service = DumbService.getInstance(dir.project)
    return service.computeWithAlternativeResolveEnabled<PsiFile?, Throwable> {
        val adjustedDir = CreateTemplateInPackageAction.adjustDirectory(targetDir, JavaModuleSourceRootTypes.SOURCES)
        val psiFile = createCangJieFileFromTemplate(adjustedDir, className, template)
        if (psiFile is CjFile) {
            val singleClass = psiFile.declarations.singleOrNull() as? CjClass
            if (singleClass != null && name.contains("Abstract")) {
                runWriteAction {
                    singleClass.addModifier(CjTokens.ABSTRACT_KEYWORD)
                }
            }
        }
//        JavaCreateTemplateInPackageAction.setupJdk(adjustedDir, psiFile)
        val module = ModuleUtil.findModuleForFile(psiFile)
        val configurator = CangJieProjectConfigurator.EP_NAME.extensions.firstOrNull()
//        if (module != null && configurator != null) {
//            DumbService.getInstance(module.project).runWhenSmart {
//                if (configurator.getStatus(module.toModuleGroup()) == ConfigureCangJieStatus.CAN_BE_CONFIGURED) {
//                    configurator.configure(module.project, emptyList())
//                }
//            }
//        }
        return@computeWithAlternativeResolveEnabled psiFile
    }
}

private fun findOrCreateTarget(
    dir: PsiDirectory,
    name: String,
    directorySeparators: CharArray
): Pair<String, PsiDirectory> {
    var className = removeCangJieExtensionIfPresent(name).toCamelCase()
    var targetDir = dir

    for (splitChar in directorySeparators) {
        if (splitChar in className) {
            val names = className.trim().split(splitChar)

            for (dirName in names.dropLast(1)) {
                targetDir = targetDir.findSubdirectory(dirName) ?: runWriteAction {
                    targetDir.createSubdirectory(dirName)
                }
            }

            className = names.last()
            break
        }
    }
    return Pair(className, targetDir)
}

private fun removeCangJieExtensionIfPresent(name: String): String = when {
    name.endsWith(".$CANGJIE_WORKSHEET_EXTENSION") -> name.removeSuffix(".$CANGJIE_WORKSHEET_EXTENSION")
    name.endsWith(".$STD_SCRIPT_SUFFIX") -> name.removeSuffix(".$STD_SCRIPT_SUFFIX")
    name.endsWith(".${CangJieFileType.EXTENSION}") -> name.removeSuffix(".${CangJieFileType.EXTENSION}")
    else -> name
}

private fun createCangJieFileFromTemplate(dir: PsiDirectory, className: String, template: FileTemplate): PsiFile? {
    val project = dir.project
    val defaultProperties = FileTemplateManager.getInstance(project).defaultProperties

    val properties = Properties(defaultProperties)

    val element = try {
        CreateFromTemplateDialog(
            project, dir, template,
            AttributesDefaults(className).withFixedName(true),
            properties
        ).create()
    } catch (e: IncorrectOperationException) {
        throw e
    } catch (e: Exception) {
        logger<NewCangJieFileAction>().error(e)
        return null
    }

    return element?.containingFile
}
