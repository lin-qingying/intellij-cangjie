package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.storage.getValue

open class DeserializedAnnotations(
    storageManager: StorageManager,
    compute: () -> List<AnnotationDescriptor>
) : Annotations {
    private val annotations by storageManager.createLazyValue(compute)

    override fun isEmpty(): Boolean = annotations.isEmpty()

    override fun iterator(): Iterator<AnnotationDescriptor> = annotations.iterator()
}

class NonEmptyDeserializedAnnotations(
    storageManager: StorageManager,
    compute: () -> List<AnnotationDescriptor>
) : DeserializedAnnotations(storageManager, compute) {
    override fun isEmpty(): Boolean = false
}
