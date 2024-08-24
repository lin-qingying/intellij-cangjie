package com.huawei.cangjie.ide.completion

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

@Service(Service.Level.PROJECT)
class LookupCancelService {

    companion object {
        fun getInstance(project: Project): LookupCancelService = project.service()

        fun getServiceIfCreated(project: Project): LookupCancelService? = project.getServiceIfCreated(LookupCancelService::class.java)

        val AUTO_POPUP_AT = Key<Int>("LookupCancelService.AUTO_POPUP_AT")
    }

}
