package org.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.packageFragments
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.EnumWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedEnumescriptor

interface ClassDataFinder {
    fun findClassData(classId: ClassId): ClassData?
    val allClassIds: Collection<ClassId>
}

data class ClassData(
    val classDecl: ClassDeclWrapper,
    val `package`: PackageWrapper,
    val metadataVersion: BinaryVersion,
    val sourceElement: SourceElement
)

class EnumDeserializer(private val components: DeserializationComponents) {
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

    private val enums: (ClassKey) -> EnumDescriptor? =
        components.storageManager.createMemoizedFunctionWithNullableValues { key -> createEnum(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeEnum(
        classId: ClassId,
        classData: ClassData? = null
    ): EnumDescriptor? =
        enums(ClassKey(classId, classData))

    private fun createEnum(key: ClassKey): EnumDescriptor? {
        val classId = key.classId
        for (factory in components.fictitiousClassDescriptorFactories) {
            factory.createEnum(classId)?.let { return it }
        }
        if (classId in ClassDeserializer.Companion.BLACK_LIST) return null

        val (decl, `package`, metadataVersion, sourceElement) = key.classData
            ?: components.classDataFinder.findClassData(classId)
            ?: return null

        val outerClassId = classId.outerClassId
        val outerContext = if (outerClassId != null) {
            val outerClass = deserializeEnum(outerClassId) as? DeserializedEnumescriptor ?: return null


            outerClass.c
        } else {
            val fragments = components.packageFragmentProvider.packageFragments(classId.packageFqName)
            val fragment =
                fragments.firstOrNull  ()
                    ?: return null

            components.createContext(
                fragment, `package`,


                metadataVersion,
                containerSource = null
            )
        }

        return DeserializedEnumescriptor(outerContext, decl as EnumWrapper, metadataVersion, sourceElement)

    }

    private class ClassKey(val classId: ClassId, val classData: ClassData?) {
        // classData *intentionally* not used in equals() / hashCode()
        override fun equals(other: Any?) = other is ClassKey && classId == other.classId

        override fun hashCode() = classId.hashCode()
    }
}

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
    fun deserializeClass(
        classId: ClassId,
        classData: ClassData? = null
    ): ClassDescriptor? =
        classes(ClassKey(classId, classData))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        for (factory in components.fictitiousClassDescriptorFactories) {
            factory.createClass(classId)?.let { return it }
        }
        if (classId in ClassDeserializer.Companion.BLACK_LIST) return null

        val (decl, `package`, metadataVersion, sourceElement) = key.classData
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
            val fragment =
                fragments.firstOrNull { it !is DeserializedPackageFragment || it.hasTopLevelClass(classId.shortClassName) }
                    ?: return null

            components.createContext(
                fragment, `package`,


                metadataVersion,
                containerSource = null
            )
        }

        return DeserializedClassDescriptor(outerContext, decl, metadataVersion, sourceElement)

    }


}

internal class ClassKey(val classId: ClassId, val classData: ClassData?) {
    // classData *intentionally* not used in equals() / hashCode()
    override fun equals(other: Any?) = other is ClassKey && classId == other.classId

    override fun hashCode() = classId.hashCode()
}