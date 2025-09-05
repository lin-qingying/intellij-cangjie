package org.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.packageFragments
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.metadata.model.Decl
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedClassDescriptor
import kotlin.hashCode

interface ClassDataFinder {
    fun findClassData(classId: ClassId): ClassData?
    val allClassIds: Collection<ClassId>
}

data class ClassData(
    val classDecl: Decl,
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

    private class ClassKey(val classId: ClassId, val classData: ClassData?) {
        // classData *intentionally* not used in equals() / hashCode()
        override fun equals(other: Any?) = other is ClassKey && classId == other.classId

        override fun hashCode() = classId.hashCode()
    }
}
