package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.ResolutionFacade
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface CangJieCacheService {
    fun getResolutionFacade(element: CjElement): ResolutionFacade
    fun getResolutionFacade(elements: List<CjElement>): ResolutionFacade
    fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo): ResolutionFacade

    companion object {
        fun getInstance(project: Project): CangJieCacheService = project.service()
    }
}
