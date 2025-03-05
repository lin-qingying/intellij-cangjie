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

package cn.cangnova.cangjie.ide.actions


import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.parsing.CangJieParserDefinition.Util.STD_SCRIPT_SUFFIX
import cn.cangnova.cangjie.psi.CjClass
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjNamedDeclaration
import cn.cangnova.cangjie.psi.psiUtil.startOffset
import cn.cangnova.cangjie.utils.getCjpmProjectDirectory
import cn.cangnova.cangjie.utils.toCamelCase
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


import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import java.util.*


internal const val CANGJIE_WORKSHEET_TEMPLATE_NAME: String = "CangJie Worksheet"
const val CANGJIE_WORKSHEET_EXTENSION: String = "ws.cj"

/**
 * 新建CangJie文件的操作类，继承自AbstractNewCangJieFileAction，并实现DumbAware接口
 */
class NewCangJieFileAction : AbstractNewCangJieFileAction(), DumbAware {

    /**
     * 检查操作是否可用
     * @param dataContext 数据上下文，用于获取IDE视图和项目信息
     * @return 如果操作可用返回true，否则返回false
     */
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

    /**
     * 构建创建文件对话框
     * @param project 项目实例
     * @param directory 目录实例
     * @param builder 对话框构建器
     */
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

    /**
     * 获取操作名称
     * @param directory 目录实例，可能为null
     * @param newName 新文件名
     * @param templateName 模板名，可能为null
     * @return 操作名称
     */
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String =
        CangJieBundle.message("action.CangJie.NewFile.text")

    /**
     * 重写hashCode方法
     * @return 哈希码，此处始终返回0
     */
    override fun hashCode(): Int = 0

    /**
     * 重写equals方法
     * @param other 另一个对象
     * @return 如果other是NewCangJieFileAction的实例，返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean = other is NewCangJieFileAction
}


/**
 * 抽象的创建CangJie文件操作类，继承自CreateFileFromTemplateAction
 */
abstract class AbstractNewCangJieFileAction : CreateFileFromTemplateAction() {

    /**
     * 获取CjFile的编辑器，如果当前选中的文本编辑器的文档与CjFile的文档相同，则返回该编辑器
     * @param cjFile CjFile实例
     * @return 编辑器实例，可能为null
     */
    private fun CjFile.editor(): Editor? =
        FileEditorManager.getInstance(this.project).selectedTextEditor?.takeIf { it.document == this.viewProvider.document }

    /**
     * 后处理创建的元素，调用父类的postProcess方法，然后执行额外的处理逻辑
     * @param createdElement 创建的PsiFile实例
     * @param templateName 模板名，可能为null
     * @param customProperties 自定义属性，可能为null
     */
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

    /**
     * 是否在写操作中启动，此处重写为false
     */
    override fun startInWriteAction() = false

    /**
     * 从模板创建文件，如果模板名为CANGJIE_WORKSHEET_TEMPLATE_NAME，则使用特殊处理
     * @param name 文件名
     * @param template 文件模板
     * @param dir 目录实例
     * @return 创建的PsiFile实例，可能为null
     */
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


/**
 * 新建CangJie文件名验证器，实现InputValidatorEx接口
 */
object NewCangJieFileNameValidator : InputValidatorEx {
    /**
     * 获取错误信息
     * @param inputString 输入的文件名
     * @return 错误信息，如果没有错误返回null
     */
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

    /**
     * 检查输入是否有效，此处始终返回true
     * @param inputString 输入的文件名
     * @return 始终返回true
     */
    override fun checkInput(inputString: String): Boolean = true

    /**
     * 是否可以关闭对话框
     * @param inputString 输入的文件名
     * @return 如果没有错误信息返回true，否则返回false
     */
    override fun canClose(inputString: String): Boolean = getErrorText(inputString) == null
}


private val FQNAME_SEPARATORS: CharArray = charArrayOf('/', '\\', '.')
private val FILE_SEPARATORS: CharArray = charArrayOf('/', '\\')

/**
 * 新仓颉文件钩子的抽象类，用于在文件创建后进行处理
 */
abstract class NewCangJieFileHook {
    companion object {
        /**
         * 扩展点名称，用于查找所有实现此接口的扩展
         */
        val EP_NAME: ExtensionPointName<NewCangJieFileHook> =
            ExtensionPointName.create("cn.cangnova.cangjie.newFileHook")
    }

    /**
     * 在创建文件后调用此方法进行处理
     *
     * @param createdElement 创建的文件元素
     * @param module 文件所属的模块
     */
    abstract fun postProcess(createdElement: CjFile, module: Module)
}


/**
 * 根据模板和状态创建文件
 *
 * @param name 文件名
 * @param template 文件模板
 * @param dir 文件创建的目录
 * @return 创建的PsiFile对象，如果失败则返回null
 */
internal fun createFileFromTemplateWithStat(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
    return createCangJieFileFromTemplate(name, template, dir)
}

/**
 * 根据模板创建仓颉文件
 *
 * @param name 文件名
 * @param template 文件模板
 * @param dir 文件创建的目录
 * @return 创建的PsiFile对象，如果失败则返回null
 */
internal fun createCangJieFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
    // 根据模板名称选择合适的目录分隔符
    val directorySeparators = when (template.name) {
        "CangJie File" -> FILE_SEPARATORS
        "CangJie Worksheet" -> FILE_SEPARATORS
        "CangJie Script" -> FILE_SEPARATORS
        else -> FQNAME_SEPARATORS
    }

    // 查找或创建目标目录，并获取处理后的类名
    val (className, targetDir) = findOrCreateTarget(dir, name, directorySeparators)

    // 设置模板的文件名
    template.fileName = name

    // 获取项目的服务，并在禁用智能模式下执行操作
    val service = DumbService.getInstance(dir.project)
    return service.computeWithAlternativeResolveEnabled<PsiFile?, Throwable> {
        // 调整目录并创建文件
        val adjustedDir = CreateTemplateInPackageAction.adjustDirectory(targetDir, JavaModuleSourceRootTypes.SOURCES)
        val psiFile = createCangJieFileFromTemplate(adjustedDir, className, template)
        // 如果创建的是仓颉文件，并且包含单个类声明，则根据文件名添加抽象修饰符
        if (psiFile is CjFile) {
            val singleClass = psiFile.declarations.singleOrNull() as? CjClass
            if (singleClass != null && name.contains("Abstract")) {
                runWriteAction {
                    singleClass.addModifier(CjTokens.ABSTRACT_KEYWORD)
                }
            }
        }

        // 返回创建的文件
        return@computeWithAlternativeResolveEnabled psiFile
    }
}

/**
 * 查找或创建目标目录，并获取处理后的类名
 *
 * @param dir 初始目录
 * @param name 文件名
 * @param directorySeparators 目录分隔符数组
 * @return 包含处理后的类名和目标目录的Pair对象
 */
private fun findOrCreateTarget(
    dir: PsiDirectory,
    name: String,
    directorySeparators: CharArray
): Pair<String, PsiDirectory> {
    var className = removeCangJieExtensionIfPresent(name).toCamelCase()
    var targetDir = dir

    // 遍历目录分隔符，查找或创建目标目录
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

/**
 * 如果文件名包含仓颉文件扩展名，则移除扩展名
 *
 * @param name 文件名
 * @return 移除扩展名后的文件名
 */
private fun removeCangJieExtensionIfPresent(name: String): String = when {
    name.endsWith(".$CANGJIE_WORKSHEET_EXTENSION") -> name.removeSuffix(".$CANGJIE_WORKSHEET_EXTENSION")
    name.endsWith(".$STD_SCRIPT_SUFFIX") -> name.removeSuffix(".$STD_SCRIPT_SUFFIX")
    name.endsWith(".${CangJieFileType.EXTENSION}") -> name.removeSuffix(".${CangJieFileType.EXTENSION}")
    else -> name
}

/**
 * 获取模板属性
 *
 */
fun getTemplateProperties(project: Project, dir: PsiDirectory): Properties {
    val defaultProperties = FileTemplateManager.getInstance(project).defaultProperties
    val cjpmProjectDirectory = dir.getCjpmProjectDirectory()
    // 设置模板属性
    val properties = Properties(defaultProperties)
    properties.setProperty("CANGJIE_MODULE_NAME", cjpmProjectDirectory.first)


    return properties
}

/**
 * 根据模板创建仓颉文件
 *
 * @param dir 文件创建的目录
 * @param className 文件中的类名
 * @param template 文件模板
 * @return 创建的PsiFile对象，如果失败则返回null
 */
private fun createCangJieFileFromTemplate(dir: PsiDirectory, className: String, template: FileTemplate): PsiFile? {
    val project = dir.project
    val properties = getTemplateProperties(project, dir = dir)

    // 创建文件元素
    val element = try {
        val templateDialog = CreateFromTemplateDialog(
            project, dir, template,
            AttributesDefaults(className).withFixedName(true),
            properties
        )


        templateDialog.create()
    } catch (e: IncorrectOperationException) {
        throw e
    } catch (e: Exception) {
        logger<NewCangJieFileAction>().error(e)
        return null
    }

    // 返回创建的文件
    return element?.containingFile
}
