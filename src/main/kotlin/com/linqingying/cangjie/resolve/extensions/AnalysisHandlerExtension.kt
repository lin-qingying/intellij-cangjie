package com.linqingying.cangjie.resolve.extensions

import com.intellij.openapi.project.Project
import com.linqingying.cangjie.analyzer.AnalysisResult
import com.linqingying.cangjie.container.ComponentProvider
import com.linqingying.cangjie.context.ProjectContext
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor
import com.linqingying.cangjie.psi.CjFile

interface AnalysisHandlerExtension {
    companion object : ProjectExtensionDescriptor<AnalysisHandlerExtension>(
        "com.linqingying.cangjie.analyzeCompleteHandlerExtension",
        AnalysisHandlerExtension::class.java
    )

    fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<CjFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? = null

    fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<CjFile>
    ): AnalysisResult? = null
}
