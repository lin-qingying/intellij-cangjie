package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.intellij.openapi.project.Project

interface CodeAnalyzerInitializer {
    fun createTrace(): BindingTrace

    companion object {
        fun getInstance(project: Project): CodeAnalyzerInitializer =
            project.getService(CodeAnalyzerInitializer::class.java)!!
    }
}

class DummyCodeAnalyzerInitializer : CodeAnalyzerInitializer {
    override fun createTrace(): BindingTrace = BindingTraceContext(true)
}
