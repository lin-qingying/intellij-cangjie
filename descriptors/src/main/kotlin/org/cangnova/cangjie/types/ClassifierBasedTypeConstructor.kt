/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.impl.PrimitiveClassDescriptor
import org.cangnova.cangjie.resolve.DescriptorUtils


abstract class ClassifierBasedTypeConstructor : TypeConstructor {
    private var hashCode = 0

    abstract override val declarationDescriptor: ClassifierDescriptor
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
        // Cast to Any is needed as a workaround
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

                a is PrimitiveClassDescriptor -> return b is PrimitiveClassDescriptor && a.name == b.name
                b is PrimitiveClassDescriptor -> return false

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
