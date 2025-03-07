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

package cn.cangnova.cangjie.ide.projectStructure.moduleInfo

import cn.cangnova.cangjie.analyzer.ModuleInfo
import cn.cangnova.cangjie.analyzer.ModuleOrigin
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices
import cn.cangnova.cangjie.resolve.PlatformDependentAnalyzerServicesImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
interface NonSourceModuleInfoBase : ModuleInfo


class NotUnderContentRootModuleInfo(
    override val project: Project,
    file: CjFile?
) :   ModuleInfo, NonSourceModuleInfoBase {
    @Deprecated("Backing 'CjFile' expected")
    constructor(project: Project) : this(project, null)

    private val filePointer: SmartPsiElementPointer<CjFile>? = file?.let { SmartPointerManager.createPointer(it.originalFile as CjFile) }

    val file: CjFile?
        get() = filePointer?.element

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER

    override val name: Name = Name.special("<special module for files not under source root>")

//    override val displayedName: String
//        get() = CangJieBaseProjectStructureBundle.message("special.module.for.files.not.under.source.root")
//
    override val contentScope: GlobalSearchScope
        get() = file?.let(GlobalSearchScope::fileScope) ?: GlobalSearchScope.EMPTY_SCOPE

    //TODO: (module refactoring) dependency on runtime can be of use here
//    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)
//    override fun dependenciesWithoutSelf(): Sequence<IdeaModuleInfo> = emptySequence()
//
//    override val platform: TargetPlatform
//        get() = JvmPlatforms.defaultJvmPlatform
//
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = PlatformDependentAnalyzerServicesImpl

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotUnderContentRootModuleInfo

        if (project != other.project) return false
        return filePointer == other.filePointer
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + (filePointer?.hashCode() ?: 0)
        return result
    }
}
