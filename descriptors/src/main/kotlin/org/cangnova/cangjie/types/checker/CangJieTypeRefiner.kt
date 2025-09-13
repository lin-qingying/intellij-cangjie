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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.EnumType


sealed class TypeRefinementSupport(val isEnabled: Boolean) {
    object Disabled : TypeRefinementSupport(isEnabled = false)
    object EnabledUninitialized : TypeRefinementSupport(isEnabled = true)
    class Enabled(val typeRefiner: CangJieTypeRefiner) : TypeRefinementSupport(isEnabled = true)
}

@TypeRefinement
val REFINER_CAPABILITY = ModuleCapability<Ref<TypeRefinementSupport>>("CangJieTypeRefiner")
class Ref<T : Any>(var value: T)

@TypeRefinement
fun CangJieTypeRefiner.refineTypes(types: Iterable<CangJieType>): List<CangJieType> = types.map { refineType(it) }

@DefaultImplementation(impl = AbstractTypeRefiner.Default::class)
abstract class CangJieTypeRefiner : AbstractTypeRefiner(){
    @TypeRefinement
    abstract override fun refineType(type: CangJieTypeMarker): CangJieType

    @TypeRefinement
    abstract fun refineSupertypes(classDescriptor: ClassifierDescriptorWithKind): Collection<CangJieType>

    @TypeRefinement
    abstract fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor?
    @TypeRefinement
    abstract fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean
    @TypeRefinement
    abstract fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean
    @TypeRefinement
    abstract fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassifierDescriptor, compute: () -> S): S
    
    /**
     * 精化枚举类型
     * 
     * @param enumType 要精化的枚举类型
     * @return 精化后的枚举类型
     */
    @TypeRefinement
    abstract fun refineEnumType(enumType: EnumType): EnumType

    object Default : CangJieTypeRefiner() {
        @TypeRefinement
        override fun refineType(type: CangJieTypeMarker): CangJieType = type as CangJieType

        @TypeRefinement
        override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor? {
            return null

        }
//
        @TypeRefinement
        override fun refineSupertypes(classDescriptor: ClassifierDescriptorWithKind): Collection<CangJieType> {
            return classDescriptor.typeConstructor.supertypes
        }
//
//        @TypeRefinement
//        override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassDescriptor? {
//            return null
//        }
//
//        @TypeRefinement
//        override fun findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
//            return null
//        }
//
        @TypeRefinement
        override fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean {
            return false
        }

        @TypeRefinement
        override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
            return false
        }

        @TypeRefinement
        override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassifierDescriptor, compute: () -> S): S {
            return compute()
        }
        
        @TypeRefinement
        override fun refineEnumType(enumType: EnumType): EnumType {
            return enumType
        }
    }
}
