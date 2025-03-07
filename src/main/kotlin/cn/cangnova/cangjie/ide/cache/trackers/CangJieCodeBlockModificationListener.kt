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

package cn.cangnova.cangjie.ide.cache.trackers

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.util.PsiModificationTracker
import cn.cangnova.cangjie.lang.CangJieLanguage

val CANGJIE_CONSOLE_KEY = Key.create<Boolean>("cangjie.console")

/**
 * Tested in [OutOfBlockModificationTestGenerated]
 */
@Service(Service.Level.PROJECT)
class CangJieCodeBlockModificationListener(project: Project) : PsiTreeChangePreprocessor, Disposable {

    private val cangjieOutOfCodeBlockTrackerImpl = SimpleModificationTracker()

    val cangjieOutOfCodeBlockTracker: ModificationTracker = cangjieOutOfCodeBlockTrackerImpl

    companion object {
        fun getInstance(project: Project): CangJieCodeBlockModificationListener = project.service()
    }

    private val modificationTrackerImpl: PsiModificationTracker =
        PsiModificationTracker.getInstance(project)

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if (!PsiModificationTrackerImpl.canAffectPsi(event)) {
            return
        }

        // Copy logic from PsiModificationTrackerImpl.treeChanged(). Some out-of-code-block events are written to language modification
        // tracker in PsiModificationTrackerImpl but don't have correspondent PomModelEvent. Increase cangjieOutOfCodeBlockTracker
        // manually if needed.
        val outOfCodeBlock = when (event.code) {
            PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED ->
                event.propertyName === PsiTreeChangeEvent.PROP_UNLOADED_PSI || event.propertyName === PsiTreeChangeEvent.PROP_ROOTS

            PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED -> event.oldParent is PsiDirectory || event.newParent is PsiDirectory
            else -> event.parent is PsiDirectory
        }

        if (outOfCodeBlock) {
            incModificationCount()
        }
    }

    fun incModificationCount() {
        cangjieOutOfCodeBlockTrackerImpl.incModificationCount()
    }

    @Volatile
    private var cangjieModificationCount: Long = 0

    init {
        val messageBusConnection = project.messageBus.connect(this)

        val perModuleOutOfCodeBlockTrackerUpdater =
            CangJieModuleOutOfCodeBlockModificationTracker.getUpdaterInstance(project)

        PureCangJieCodeBlockModificationListener.getInstance(project) //ensure pom listener is attached as well

        (PsiManager.getInstance(project) as PsiManagerImpl).addTreeChangePreprocessor(this)

        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            val cangjieTrackerInternalIDECount = modificationTrackerImpl.forLanguage(CangJieLanguage).modificationCount
            if (cangjieModificationCount == cangjieTrackerInternalIDECount) {
                // Some update that we are not sure is from CangJie language, as CangJie language tracker wasn't changed
                incModificationCount()
            } else {
                cangjieModificationCount = cangjieTrackerInternalIDECount
            }

            perModuleOutOfCodeBlockTrackerUpdater.onPsiModificationTrackerUpdate()
        })

        messageBusConnection.subscribe(DynamicPluginListener.TOPIC, object : DynamicPluginListener {
            override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
                incModificationCount()
            }

            override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
                incModificationCount()
            }
        })
    }


    override fun dispose() = Unit
}
