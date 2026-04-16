/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.service.impl

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.nio.file.Path

/**。
 *需要将[UserDisabledFeatures]与[CjpmProjectsServiceImpl]分开存储的服务
 *需要能够将它们存储在不同的XML文件中([Storage])
 */
@State(
    name = "CangJieProjectFeatures", storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)
    ]
)
@Service(Service.Level.PROJECT)
class UserDisabledFeaturesHolder(private val project: Project) : PersistentStateComponent<Element> {
    private var loadedUserDisabledFeatures: Map<Path, UserDisabledFeatures> = emptyMap()


    fun takeLoadedUserDisabledFeatures(): Map<Path, UserDisabledFeatures> {
        val result = loadedUserDisabledFeatures
        loadedUserDisabledFeatures = emptyMap()
        return result
    }

    override fun getState(): Element {
        val state = Element("state")
//        for (cjpmProject in project.cjpmProjects.allProjects) {
//            val pkgToFeatures = cjpmProject.userDisabledFeatures
//            if (!pkgToFeatures.isEmpty()) {
//                val cjpmProjectElement = Element("cjpmProject")
//                cjpmProjectElement.setAttribute("file", cjpmProject.manifest.systemIndependentPath)
//                for ((pkg, features) in pkgToFeatures.pkgRootToDisabledFeatures) {
//                    if (features.isNotEmpty()) {
//                        val packageElement = Element("package")
//                        packageElement.setAttribute("file", pkg.systemIndependentPath)
//                        for (feature in features) {
//                            val featureElement = Element("feature")
//                            featureElement.setAttribute("name", feature)
//                            packageElement.addContent(featureElement)
//                        }
//                        cjpmProjectElement.addContent(packageElement)
//                    }
//                }
//                state.addContent(cjpmProjectElement)
//            }
//        }
        return state
    }

    override fun loadState(state: Element) {
        val projects = state.getChildren("CangJieProject")

    }
}
