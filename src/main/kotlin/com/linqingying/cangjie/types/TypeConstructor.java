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

package com.linqingying.cangjie.types;

import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.descriptors.ClassifierDescriptor;
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor;
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner;
import com.linqingying.cangjie.types.model.TypeConstructorMarker;
import com.linqingying.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface TypeConstructor extends TypeConstructorMarker {
    //    父类型
    @NotNull
    @ReadOnly
    Collection<CangJieType> getSupertypes();

    //    扩展的父类型 需要一个排除的扩展id
    @NotNull
    @ReadOnly
    default Collection<CangJieType> getExtendSupertypes(@Nullable String extendId ) {
        return Collections.emptyList();
    }

    @NotNull
    CangJieBuiltIns getBuiltIns();

    /**
     * If the type is non-denotable, it can't be written in code directly, it only can appear internally inside a type checker.
     * Examples: intersection type or number value type.
     */
    boolean isDenotable();

    @Nullable
    ClassifierDescriptor getDeclarationDescriptor();

    /**
     * Returns TypeConstructor, refined with passed refined, if that makes sense for this specific typeConstructor
     * <p>
     * Contract:
     * - returned TypeConstructor has refined supertypes, i.e. it has correct supertypes resolved as if
     * we were looking at them from refiner's module
     * - IT DOES NOT ADD PLATFORM DECLARED SUPERTYPES!!!!!!!!
     * - all other similar sources of CangJieTypes/Descriptors should return refined instances as well
     * <p>
     * That method is part of internal refinement infrastructure, so IT SHOULD NOT BE CALLED from anywhere except
     * methods from refinement (like methods of CangJieTypeRefinerImpl or CangJieType.refine
     * <p>
     * Implementation notice:
     * - the most interesting part happens in 'AbstractTypeConstructor': it returns 'ModuleViewTypeConstructor', which
     * will refine supertypes when queried for them
     * - also, there are several typeConstructors, which do not inherit AbstractTypeConstructor, but have some component
     * types/descriptors (e.g. IntersectionTypeConstructor) -- they refine their content manually by recursing using refiner
     * - finally, most special typeConstructors have no meaningful refinement and return null (i.e. UninferredTypeParameterConstructor)
     */
    @TypeRefinement
    @NotNull
    TypeConstructor refine(@NotNull CangJieTypeRefiner cangjieTypeRefiner);

    /**
     * Cannot have subtypes.
     */
    boolean isFinal();
    /**
     * It may differ from ClassDescriptor.declaredParameters if the class is inner, in such case
     * it also contains additional parameters from outer declarations.
     *
     * @return list of parameters for type constructor, both from current declaration and the outer one
     */
    @NotNull
    @ReadOnly
    List<TypeParameterDescriptor> getParameters();
}
