//// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
//package com.huawei.cangjie.ide.project.tools.projectWizard.wizard
//
//import com.huawei.cangjie.ide.project.tools.projectWizard.core.ContextComponents
//import com.huawei.cangjie.ide.project.tools.projectWizard.core.PluginsCreator
//import com.huawei.cangjie.ide.project.tools.projectWizard.core.services.ServicesManager
//import com.intellij.framework.library.FrameworkLibraryVersionFilter
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.projectRoots.Sdk
//import com.jetbrains.rd.util.reactive.TaskResult
//
//
//import kotlin.properties.ReadWriteProperty
//import kotlin.reflect.KProperty
//
//
//abstract class Wizard(createPlugins: PluginsCreator, servicesManager: ServicesManager, isUnitTestMode: Boolean) {
//    open fun createComponents(): ContextComponents = ContextComponents()
//
//    val context = Context(createPlugins, servicesManager, isUnitTestMode, createComponents())
//
//    private fun checkAllRequiredSettingPresent(phases: Set<GenerationPhase>): TaskResult<Unit> = context.read {
//        getUnspecifiedSettings(phases).let { unspecifiedSettings ->
//            if (unspecifiedSettings.isEmpty()) UNIT_SUCCESS
//            else Failure(RequiredSettingsIsNotPresentError(unspecifiedSettings.map { it.path }))
//        }
//    }
//
//    private fun initNonPluginDefaultValues() {
//        context.writeSettings {
//            CangJiePlugin.modules.notRequiredSettingValue
//                ?.withAllSubModules(includeSourcesets = true)
//                ?.forEach { module ->
//                    with(module) { initDefaultValuesForSettings() }
//                }
//        }
//    }
//
//    protected fun initPluginSettingsDefaultValues() {
//        context.writeSettings {
//            for (setting in pluginSettings) {
//                setting.reference.setSettingValueToItsDefaultIfItIsNotSetValue()
//            }
//        }
//    }
//
//    fun validate(phases: Set<GenerationPhase>, toBeValidated: (PluginSetting<*, *>.() -> Boolean) = { true }): ValidationResult =
//        context.read {
//            pluginSettings.map { setting ->
//                val value = setting.notRequiredSettingValue ?: return@map ValidationResult.OK
//                if (setting.toBeValidated() && setting.neededAtPhase in phases && setting.isActive(this)) {
//                    val validator = setting.validator.safeAs<SettingValidator<Any>>()
//                    validator?.validate?.let { it(this, value) } ?: ValidationResult.OK
//                }
//                else ValidationResult.OK
//            }.fold()
//        }
//
//    private fun saveSettingValues(phases: Set<GenerationPhase>) = context.read {
//        for (setting in pluginSettings) {
//            if (setting.neededAtPhase !in phases) continue
//            if (!setting.isSavable) continue
//            if (!setting.isAvailable(this)) continue
//            val serializer = setting.type.serializer as? SettingSerializer.Serializer<Any> ?: continue
//            service<SettingSavingWizardService>().saveSettingValue(
//                setting.path,
//                serializer.toString(setting.settingValue)
//            )
//        }
//    }
//
//    open fun apply(
//        services: List<WizardService>,
//        phases: Set<GenerationPhase>,
//        onTaskExecuting: (PipelineTask) -> Unit = {}
//    ): TaskResult<Unit> = computeM {
//        initPluginSettingsDefaultValues()
//        initNonPluginDefaultValues()
//        checkAllRequiredSettingPresent(phases).ensure()
//        validate(phases) { validateOnProjectCreation }.toResult().ensure()
//        saveSettingValues(phases)
//        val (tasksSorted) = context.sortTasks().map { tasks ->
//            tasks.groupBy { it.phase }.toList().sortedBy { it.first }.flatMap { it.second }
//        }
//
//        context.withAdditionalServices(services).write {
//            tasksSorted
//                // We should take only one task of each type as all tasks with the same path are considered to be the same
//                .distinctBy { it.path }
//                .asSequence()
//                .filter { task -> task.phase in phases }
//                .filter { task -> task.isAvailable(this) }
//                .map { task -> onTaskExecuting(task); task.action(this) }
//                .sequenceFailFirst()
//                .ignore()
//        }
//    }
//}
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
