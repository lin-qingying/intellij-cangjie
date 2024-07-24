package com.huawei.cangjie.analyzer

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.intellij.util.messages.Topic


interface ModuleDescriptorListener {
    fun moduleDescriptorInvalidated(moduleDescriptor: ModuleDescriptor)

    companion object {
        @JvmField
        val TOPIC: Topic<ModuleDescriptorListener> =
            Topic.create("ModuleDescriptorListener", ModuleDescriptorListener::class.java)
    }
}
