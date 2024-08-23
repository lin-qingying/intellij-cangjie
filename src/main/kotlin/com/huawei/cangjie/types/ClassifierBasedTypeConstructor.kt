package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.resolve.DescriptorUtils


abstract class ClassifierBasedTypeConstructor : TypeConstructor {
    private var hashCode = 0

    abstract override fun getDeclarationDescriptor(): ClassifierDescriptor

    override fun hashCode(): Int {
        val cachedHashCode = hashCode
        if (cachedHashCode != 0) return cachedHashCode

        val descriptor = declarationDescriptor
        val computedHashCode = if (hasMeaningfulFqName(descriptor)) {
            DescriptorUtils.getFqName(descriptor).hashCode()
        } else {
            System.identityHashCode(this)
        }

        return computedHashCode.also { hashCode = it }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeConstructor) return false

        // performance optimization: getFqName is slow method
        // Cast to Any is needed as a workaround for KT-45008.
        if ((other as Any).hashCode() != hashCode()) return false

        // Sometimes we can get two classes from different modules with different counts of type parameters.
        // To avoid problems in type checker we suppose that it is different type constructors.
        if (other.parameters.size != parameters.size) return false

        val myDescriptor = declarationDescriptor
        val otherDescriptor = other.declarationDescriptor ?: return false
        if (!hasMeaningfulFqName(myDescriptor) || !hasMeaningfulFqName(otherDescriptor)) {
            // All error types and local classes have the same descriptor,
            // but we've already checked identity equality in the beginning of the method
            return false
        }

        return isSameClassifier(otherDescriptor)
    }

    protected abstract fun isSameClassifier(classifier: ClassifierDescriptor): Boolean

    protected fun areFqNamesEqual(first: ClassifierDescriptor, second: ClassifierDescriptor): Boolean {
        if (first.name != second.name) return false
        var a: DeclarationDescriptor? = first.containingDeclaration
        var b: DeclarationDescriptor? = second.containingDeclaration
        while (a != null && b != null) {
            when {
                a is ModuleDescriptor -> return b is ModuleDescriptor
                b is ModuleDescriptor -> return false
                a is PackageFragmentDescriptor -> return b is PackageFragmentDescriptor && a.fqName == b.fqName
                b is PackageFragmentDescriptor -> return false

                a is BasicTypeDescriptor -> return b is BasicTypeDescriptor && a.name == b.name
                b is BasicTypeDescriptor -> return false


                a.name != b.name -> return false
                else -> {
                    a = a.containingDeclaration
                    b = b.containingDeclaration
                }
            }
        }
        return true
    }

    private fun hasMeaningfulFqName(descriptor: ClassifierDescriptor): Boolean =
        !ErrorUtils.isError(descriptor)
//                && !DescriptorUtils.isLocal(descriptor)
}
