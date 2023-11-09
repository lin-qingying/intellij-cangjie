//// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
//package com.huawei.cangjie.idea.project.tools.projectWizard.wizard
//
//import com.intellij.framework.library.FrameworkLibraryVersionFilter
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.projectRoots.Sdk
//
//
//import kotlin.properties.ReadWriteProperty
//import kotlin.reflect.KProperty
//
//class IdeWizard(
//    createPlugins: PluginsCreator,
//    initialServices: List<WizardService>,
//    isUnitTestMode: Boolean
//) : Wizard(
//    createPlugins,
//    ServicesManager(initialServices) { services ->
//        services.firstOrNull { it is IdeaWizardService }
//            ?: services.firstOrNull()
//    },
//    isUnitTestMode
//) {
//    init {
//        initPluginSettingsDefaultValues()
//    }
//
//    override fun createComponents(): ContextComponents = ContextComponents(
//        WizardLoggingSession::class to WizardLoggingSession.createWithRandomId(),
//    )
//
//    val jpsData by lazy {
//        val libraryDescription = JavaRuntimeLibraryDescription(null)
//        val librariesContainer = LibrariesContainerFactory.createContainer(null as Project?)
//        val libraryOptionsPanel = LibraryOptionsPanel(libraryDescription, "", FrameworkLibraryVersionFilter.ALL, librariesContainer, false)
//        JpsData(libraryDescription, librariesContainer, libraryOptionsPanel)
//    }
//
//    var jdk: Sdk? = null
//
//    var projectPath by setting(StructurePlugin.projectPath.reference)
//    var projectName by setting(StructurePlugin.name.reference)
//
//    var groupId by setting(StructurePlugin.groupId.reference)
//    var artifactId by setting(StructurePlugin.artifactId.reference)
//    var buildSystemType by setting(BuildSystemPlugin.type.reference)
//
//    var projectTemplate by setting(ProjectTemplatesPlugin.template.reference)
//
//    private fun <V : Any, T : SettingType<V>> setting(reference: SettingReference<V, T>) =
//        object : ReadWriteProperty<Any?, V?> {
//            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
//                if (value == null) return
//                context.writeSettings {
//                    reference.setValue(value)
//                }
//            }
//
//            override fun getValue(thisRef: Any?, property: KProperty<*>): V? = context.read {
//                reference.notRequiredSettingValue
//            }
//        }
//
//    data class JpsData(
//      val libraryDescription: JavaRuntimeLibraryDescription,
//      val librariesContainer: LibrariesContainer,
//      val libraryOptionsPanel: LibraryOptionsPanel,
//    )
//}
//
