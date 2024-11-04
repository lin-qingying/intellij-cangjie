package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.PackageFragmentProvider
import com.linqingying.cangjie.descriptors.packageFragments
import com.linqingying.cangjie.name.ClassId


class DeserializedClassDataFinder(private val packageFragmentProvider: PackageFragmentProvider) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val packageFragments = packageFragmentProvider.packageFragments(classId.packageFqName)
        for (fragment in packageFragments) {
            if (fragment !is DeserializedPackageFragment) continue

            fragment.classDataFinder.findClassData(classId)?.let { return it }
        }
        return null
    }
}
