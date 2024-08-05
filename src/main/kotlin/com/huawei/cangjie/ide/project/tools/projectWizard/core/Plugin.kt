//package com.huawei.cangjie.ide.project.tools.projectWizard.core
//
//
//abstract class Plugin(override val context: Context) : EntityBase(),
//    ContextOwner,
//    EntitiesOwnerDescriptor,
//    EntitiesOwner<Plugin> {
//    override val descriptor get() = this
//    override val id: String get() = path
//
//    val reference = this::class
//    abstract override val path: String
//
//    abstract val properties: List<Property<*>>
//    abstract val settings: List<PluginSetting<*, *>>
//    abstract val pipelineTasks: List<PipelineTask>
//}
