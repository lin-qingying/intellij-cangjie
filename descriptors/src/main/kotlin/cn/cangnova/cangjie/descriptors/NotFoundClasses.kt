/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.impl.ClassDescriptorBase
import cn.cangnova.cangjie.descriptors.impl.EmptyPackageFragmentDescriptor
import cn.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.module
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.ClassTypeConstructorImpl
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner

class NotFoundClasses(private val storageManager: StorageManager, private val module: ModuleDescriptor) {
    /**
     * @param typeParametersCount list of numbers of type parameters in this class and all its outer classes, starting from this class
     */
    private data class ClassRequest(val classId: ClassId, val typeParametersCount: List<Int>)

    private val packageFragments = storageManager.createMemoizedFunction<FqName, PackageFragmentDescriptor> { fqName ->
        EmptyPackageFragmentDescriptor(module, fqName)
    }

    private val classes =
        storageManager.createMemoizedFunction<ClassRequest, ClassDescriptor> { (classId, typeParametersCount) ->
            if (classId.isLocal) {
                throw UnsupportedOperationException("Unresolved local class: $classId")
            }

            val container = classId.outerClassId?.let { outerClassId ->
                getClass(outerClassId, typeParametersCount.drop(1))
            } ?: packageFragments(classId.packageFqName)

            // Treat a class with a nested ClassId as inner for simplicity, otherwise the outer type cannot have generic arguments
            val isInner = classId.isNestedClass

            MockClassDescriptor(
                storageManager,
                container,
                classId.shortClassName,
                typeParametersCount.firstOrNull() ?: 0
            )
        }

    class MockClassDescriptor internal constructor(
        storageManager: StorageManager,
        container: DeclarationDescriptor,
        name: Name,

        numberOfDeclaredTypeParameters: Int
    ) : ClassDescriptorBase(storageManager, container, name, SourceElement.NO_SOURCE  ) {
        override val declaredTypeParameters: List<TypeParameterDescriptor> = (0 until numberOfDeclaredTypeParameters).map { index ->
            TypeParameterDescriptorImpl.createWithDefaultBound(
                this,
                Annotations.EMPTY, /*false,*/
                Variance.INVARIANT,
                Name.identifier("T$index"),
                index,
                storageManager
            )
        }

        override val typeConstructor =
            ClassTypeConstructorImpl(
                this,
                computeConstructorTypeParameters(),
                setOf(module.builtIns.anyType),
                storageManager
            )

        override val kind: ClassKind
            get() = ClassKind.CLASS
        override var modality: Modality
            get() = Modality.FINAL
            set(value) {}
        override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC


        override val annotations: Annotations get() = Annotations.EMPTY

        override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = MemberScope.Empty


        override val staticScope: MemberScope
            get() = MemberScope.Empty

        override val constructors: Collection<ClassConstructorDescriptor>
            get() = emptySet()
        override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
            get() = null
        override val endConstructors: Collection<ClassConstructorDescriptor>
            get() = emptySet()

        override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

        override fun toString() = "class $name (not found)"
    }

    // We create different ClassDescriptor instances for types with the same ClassId but different number of type arguments.
    // (This may happen when a class with the same FQ name is instantiated with different type arguments in different modules.)
    // It's better than creating just one descriptor because otherwise would fail in multiple places where it's asserted that
    // the number of type arguments in a type must be equal to the number of the type parameters of the class
    fun getClass(classId: ClassId, typeParametersCount: List<Int>): ClassDescriptor {
        return classes(ClassRequest(classId, typeParametersCount))
    }
}
