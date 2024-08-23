package com.huawei.cangjie.ide

import com.huawei.cangjie.name.FqName
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.IncorrectOperationException
import java.util.*
private const val SECONDARY_CONSTRUCTOR_BODY_TEMPLATE = "New CangJie Secondary Constructor Body.cj"

private const val FUNCTION_BODY_TEMPLATE = "New CangJie Function Body.cj"
private const val PROPERTY_INITIALIZER_TEMPLATE = "New CangJie Property Initializer.cj"
private const val ATTRIBUTE_FUNCTION_NAME = "FUNCTION_NAME"
private const val ATTRIBUTE_PROPERTY_NAME = "PROPERTY_NAME"

enum class TemplateKind(val templateFileName: String) {
    FUNCTION(FUNCTION_BODY_TEMPLATE),
    SECONDARY_CONSTRUCTOR(SECONDARY_CONSTRUCTOR_BODY_TEMPLATE),
    PROPERTY_INITIALIZER(PROPERTY_INITIALIZER_TEMPLATE)
}
fun getFunctionBodyTextFromTemplate(
    project: Project,
    kind: TemplateKind,
    name: String?,
    returnType: String,
    classFqName: FqName? = null
): String {
    val fileTemplate = FileTemplateManager.getInstance(project)!!.getCodeTemplate(kind.templateFileName)

    val properties = Properties()
    properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, returnType)
    if (classFqName != null) {
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, classFqName.asString())
        properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, classFqName.shortName().asString())
    }
    if (name != null) {
        val attribute = when (kind) {
            TemplateKind.FUNCTION, TemplateKind.SECONDARY_CONSTRUCTOR -> ATTRIBUTE_FUNCTION_NAME
            TemplateKind.PROPERTY_INITIALIZER -> ATTRIBUTE_PROPERTY_NAME
        }
        properties.setProperty(attribute, name)
    }

    return try {
        fileTemplate.getText(properties)
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        // TODO: This is dangerous.
        // Is there any way to avoid catching all exceptions?
        throw IncorrectOperationException("Failed to parse file template", e)
    }
}
