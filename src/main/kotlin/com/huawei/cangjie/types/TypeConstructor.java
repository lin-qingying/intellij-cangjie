package com.huawei.cangjie.types;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import com.huawei.cangjie.types.model.TypeConstructorMarker;
import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface TypeConstructor extends TypeConstructorMarker {
    @NotNull
    @ReadOnly
    Collection<CangJieType> getSupertypes();


    @NotNull
    CangJieBuiltIns getBuiltIns();

    @Nullable
    ClassifierDescriptor getDeclarationDescriptor();
    /**
     * Returns TypeConstructor, refined with passed refined, if that makes sense for this specific typeConstructor
     *
     * Contract:
     * - returned TypeConstructor has refined supertypes, i.e. it has correct supertypes resolved as if
     *   we were looking at them from refiner's module
     * - IT DOES NOT ADD PLATFORM DECLARED SUPERTYPES!!!!!!!!
     * - all other similar sources of KotlinTypes/Descriptors should return refined instances as well
     *
     * That method is part of internal refinement infrastructure, so IT SHOULD NOT BE CALLED from anywhere except
     *   methods from refinement (like methods of KotlinTypeRefinerImpl or KotlinType.refine
     *
     * Implementation notice:
     * - the most interesting part happens in 'AbstractTypeConstructor': it returns 'ModuleViewTypeConstructor', which
     *   will refine supertypes when queried for them
     * - also, there are several typeConstructors, which do not inherit AbstractTypeConstructor, but have some component
     *   types/descriptors (e.g. IntersectionTypeConstructor) -- they refine their content manually by recursing using refiner
     * - finally, most special typeConstructors have no meaningful refinement and return null (i.e. UninferredTypeParameterConstructor)
     */
    @TypeRefinement
    @NotNull
    TypeConstructor refine(@NotNull CangJieTypeRefiner kotlinTypeRefiner);
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