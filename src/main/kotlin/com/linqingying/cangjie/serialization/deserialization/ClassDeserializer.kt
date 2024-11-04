package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.packageFragments
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.metadata.deserialization.VersionRequirementTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedClassDescriptor

data class ClassData(
    val nameResolver: NameResolver,
    val classProto: ProtoBuf.Class,
    val metadataVersion: BinaryVersion,
    val sourceElement: SourceElement
)

class ClassDeserializer(private val components: DeserializationComponents) {
    companion object {
        /**
         * FQ names of classes that should be ignored during deserialization.
         *
         * We ignore kotlin.Cloneable because since Kotlin 1.1, the descriptor for it is created via JvmBuiltInClassDescriptorFactory,
         * but the metadata is still serialized for kotlin-reflect 1.0 to work (see BuiltInsSerializer.kt).
         */
        val BLACK_LIST = setOf(
            ClassId.topLevel(StandardNames.FqNames.cloneable.toSafe())
        )
    }
    private val classes: (ClassKey) -> ClassDescriptor? =
        components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeClass(classId: ClassId, classData: ClassData? = null): ClassDescriptor? =
        classes(ClassKey(classId, classData))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        for (factory in components.fictitiousClassDescriptorFactories) {
            factory.createClass(classId)?.let { return it }
        }
        if (classId in BLACK_LIST) return null

        val (nameResolver, classProto, metadataVersion, sourceElement) = key.classData
            ?: components.classDataFinder.findClassData(classId)
            ?: return null

        val outerClassId = classId.outerClassId
        val outerContext = if (outerClassId != null) {
            val outerClass = deserializeClass(outerClassId) as? DeserializedClassDescriptor ?: return null

            // Find the outer class first and check if he knows anything about the nested class we're looking for
            if (!outerClass.hasNestedClass(classId.shortClassName)) return null

            outerClass.c
        } else {
            val fragments = components.packageFragmentProvider.packageFragments(classId.packageFqName)
            val fragment = fragments.firstOrNull { it !is DeserializedPackageFragment || it.hasTopLevelClass(classId.shortClassName) }
                ?: return null

            components.createContext(
                fragment, nameResolver,
                TypeTable(classProto.typeTable),
                VersionRequirementTable.create(classProto.versionRequirementTable),
                metadataVersion,
                containerSource = null
            )
        }

        return DeserializedClassDescriptor(outerContext, classProto, nameResolver, metadataVersion, sourceElement)
    }
    private class ClassKey(val classId: ClassId, val classData: ClassData?) {
        // classData *intentionally* not used in equals() / hashCode()
        override fun equals(other: Any?) = other is ClassKey && classId == other.classId

        override fun hashCode() = classId.hashCode()
    }
}
