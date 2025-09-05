package org.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.descriptors.packageFragments
import org.cangnova.cangjie.name.ClassId

class DeserializedClassDataFinder(private val packageFragmentProvider: PackageFragmentProvider) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val packageFragments = packageFragmentProvider.packageFragments(classId.packageFqName)
        for (fragment in packageFragments) {
            if (fragment !is DeserializedPackageFragment) continue

            fragment.classDataFinder.findClassData(classId)?.let { return it }
        }
        return null
    }

    override val allClassIds: Collection<ClassId>
        get() =   emptyList()
}
