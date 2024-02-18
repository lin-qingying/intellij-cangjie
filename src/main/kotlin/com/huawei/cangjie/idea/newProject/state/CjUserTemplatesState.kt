package com.huawei.cangjie.idea.newProject.state

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "CjUserTemplatesState", storages = [Storage("cangjie.usertemplates.xml")])
class CjUserTemplatesState : PersistentStateComponent<CjUserTemplatesState> {

    var templates = mutableListOf<CjUserTemplate>()

    override fun getState(): CjUserTemplatesState = this

    override fun loadState(state: CjUserTemplatesState) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        fun getInstance(): CjUserTemplatesState = service()
    }
}

data class CjUserTemplate(@NlsContexts.ListItem var name: String = "", var url: String = "")
