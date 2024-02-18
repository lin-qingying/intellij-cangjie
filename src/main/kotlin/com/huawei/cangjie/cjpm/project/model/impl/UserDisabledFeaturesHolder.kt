package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.io.systemIndependentPath
import org.jdom.Element
import java.nio.file.Path

/**。
 *需要将[UserDisabledFeatures]与[CjpmProjectsServiceImpl]分开存储的服务
 *需要能够将它们存储在不同的XML文件中([Storage])
 */
@State(
    name = "CjpmProjectFeatures", storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)
    ]
)
@Service
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
        val cjpmProjects = state.getChildren("cjpmProject")

    }
}