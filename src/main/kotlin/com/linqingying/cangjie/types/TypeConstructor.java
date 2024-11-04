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
