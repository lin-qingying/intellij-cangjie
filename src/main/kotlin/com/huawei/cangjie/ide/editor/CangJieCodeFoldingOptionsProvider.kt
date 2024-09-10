package com.huawei.cangjie.ide.editor

import com.huawei.cangjie.CangJieBundle.message
import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.BeanConfigurable
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NonNls

private val editorOptions get() = CangJieEditorOptions.getInstance()

@NonNls
const val ID = "editor.title.cangjie"

@Service(Service.Level.APP)
@State(name = "CangJieEditorOptions", storages = [Storage("cangjie.editor.codeinsight.xml")])
class CangJieEditorOptions : PersistentStateComponent<CangJieEditorOptions> {
    companion object {
        fun getInstance(): CangJieEditorOptions {
            return ApplicationManager.getApplication().getService(CangJieEditorOptions::class.java)
        }
    }

    override fun getState(): CangJieEditorOptions {
        return this

    }

    override fun loadState(state: CangJieEditorOptions) {
        XmlSerializerUtil.copyBean(state, this)

    }
}

class CangJieEditorOptionsConfigurable : BeanConfigurable<CangJieEditorOptions>(editorOptions, message(ID)) ,
    CodeFoldingOptionsProvider {



}
