package com.linqingying.cangjie.ide.cache.trackers

import com.linqingying.cangjie.lang.CangJieLanguage
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
