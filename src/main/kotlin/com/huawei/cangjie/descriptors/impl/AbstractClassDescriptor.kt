//package com.huawei.cangjie.descriptors.impl
//
//import com.huawei.cangjie.descriptors.*
//import com.huawei.cangjie.name.Name
//import com.huawei.cangjie.resolve.DescriptorUtils.getContainingModule
//import com.huawei.cangjie.resolve.descriptorUtil.getCangJieTypeRefiner
//import com.huawei.cangjie.resolve.scopes.InnerClassesScopeWrapper
//import com.huawei.cangjie.resolve.scopes.MemberScope
//import com.huawei.cangjie.resolve.scopes.SubstitutingScope
//import com.huawei.cangjie.storage.NotNullLazyValue
//import com.huawei.cangjie.storage.StorageManager
//import com.huawei.cangjie.types.*
//import com.huawei.cangjie.types.CangJieTypeFactory.computeExpandedType
//import com.huawei.cangjie.types.TypeConstructorSubstitution.Companion.create
//import com.huawei.cangjie.types.TypeSubstitutor.Companion.create
//import com.huawei.cangjie.types.checker.CangJieTypeRefiner
//import com.huawei.cangjie.types.util.TypeUtils
//import com.huawei.cangjie.types.util.TypeUtils.getDefaultTypeProjections
//import com.huawei.cangjie.types.util.TypeUtils.makeUnsubstitutedType
//
//@OptIn(TypeRefinement::class)
//abstract class AbstractClassDescriptor(storageManager: StorageManager, override val name: Name) :
//    ModuleAwareClassDescriptor() {
//    protected val defaultType: NotNullLazyValue<SimpleType>
//    private val unsubstitutedInnerClassesScope: NotNullLazyValue<MemberScope>
//    private val thisAsReceiverParameter: NotNullLazyValue<ReceiverParameterDescriptor>
//
//    init {
//        this.defaultType = storageManager.createLazyValue {
//            makeUnsubstitutedType(
//                this@AbstractClassDescriptor, unsubstitutedMemberScope
//            ) { cangjieTypeRefiner: CangJieTypeRefiner? ->
//                val descriptor =
//                    cangjieTypeRefiner!!.refineDescriptor(this@AbstractClassDescriptor)
//                        ?: return@makeUnsubstitutedType defaultType.invoke()
//                // If we've refined descriptor
//
//                if (descriptor is TypeAliasDescriptor) {
//                    return@makeUnsubstitutedType descriptor.computeExpandedType(
//                        getDefaultTypeProjections(
//                            descriptor.getTypeConstructor().parameters
//                        )
//                    )
//                }
//
//                if (descriptor is ModuleAwareClassDescriptor) {
//                    val refinedConstructor =
//                        descriptor.getTypeConstructor().refine(cangjieTypeRefiner)
//                    return@makeUnsubstitutedType    makeUnsubstitutedType(
//                        refinedConstructor,
//                        descriptor.getUnsubstitutedMemberScope(
//                            cangjieTypeRefiner
//                        ),
//                        this
//                    )
//                }
//                descriptor.defaultType
//            }
//        }
//        this.unsubstitutedInnerClassesScope = storageManager.createLazyValue<MemberScope> {
//            InnerClassesScopeWrapper(
//                unsubstitutedMemberScope
//            )
//        }
//        this.thisAsReceiverParameter = storageManager.createLazyValue<ReceiverParameterDescriptor> {
//            LazyClassReceiverParameterDescriptor(this@AbstractClassDescriptor)
//        }
//    }
//
//    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor {
//        return thisAsReceiverParameter.invoke()
//    }
//
//    override fun getContextReceivers(): List<ReceiverParameterDescriptor> {
//        return emptyList()
//    }
//
//    override val visibility: DescriptorVisibility
//        get() = DescriptorVisibilities.PUBLIC
//
//    override fun getUnsubstitutedMemberScope(): MemberScope {
//        return getUnsubstitutedMemberScope(getContainingModule(this).getCangJieTypeRefiner())
//    }
//
//    override fun getMemberScope(
//        typeArguments: List<TypeProjection>,
//        cangjieTypeRefiner: CangJieTypeRefiner
//    ): MemberScope {
//        assert(typeArguments.size == typeConstructor.parameters.size) {
//            ("Illegal number of type arguments: expected "
//                    + typeConstructor.parameters.size + " but was " + typeArguments.size
//                    + " for " + typeConstructor + " " + typeConstructor.parameters)
//        }
//        if (typeArguments.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner)
//
//        val substitutor = create(
//            typeConstructor, typeArguments
//        ).buildSubstitutor()
//        return SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor)
//    }
//
//
//
//    override fun getMemberScope(
//        typeSubstitution: TypeSubstitution,
//        cangjieTypeRefiner: CangJieTypeRefiner
//    ): MemberScope {
//        if (typeSubstitution.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner)
//
//        val substitutor = create(typeSubstitution)
//        return SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor)
//    }
//
//    override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope {
//        return getMemberScope(typeArguments, getContainingModule(this).getCangJieTypeRefiner())
//    }
//
//    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
//        return getMemberScope(typeSubstitution, getContainingModule(this).getCangJieTypeRefiner())
//    }
//
//
//    override val original: ClassDescriptor
//        get() = this
//
//    override fun getUnsubstitutedInnerClassesScope(): MemberScope {
////        throw new UnsupportedOperationException("Should not be called on " + getClass());
//        return unsubstitutedInnerClassesScope.invoke()
//    }
//
//    //    @NotNull
//    //    @Override
//    //    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
//    //        return thisAsReceiverParameter.invoke();
//    //    }
//    //
//    //    @NotNull
//    //    @Override
//    //    public List<ReceiverParameterDescriptor> getContextReceivers() {
//    //        return Collections.emptyList();
//    //    }
//    //
//    //    @NotNull
//    //    @Override
//    //    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments, @NotNull CangJieTypeRefiner kotlinTypeRefiner) {
//    //        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Illegal number of type arguments: expected "
//    //                + getTypeConstructor().getParameters().size() + " but was " + typeArguments.size()
//    //                + " for " + getTypeConstructor() + " " + getTypeConstructor().getParameters();
//    //        if (typeArguments.isEmpty()) return getUnsubstitutedMemberScope(kotlinTypeRefiner);
//    //
//    //        TypeSubstitutor substitutor = TypeConstructorSubstitution.create(getTypeConstructor(), typeArguments).buildSubstitutor();
//    //        return new SubstitutingScope(getUnsubstitutedMemberScope(kotlinTypeRefiner), substitutor);
//    //    }
//    //
//    //    @NotNull
//    //    @Override
//    //    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution, @NotNull CangJieTypeRefiner kotlinTypeRefiner) {
//    //        if (typeSubstitution.isEmpty()) return getUnsubstitutedMemberScope(kotlinTypeRefiner);
//    //
//    //        TypeSubstitutor substitutor = TypeSubstitutor.create(typeSubstitution);
//    //
//    //        return new SubstitutingScope(getUnsubstitutedMemberScope(kotlinTypeRefiner), substitutor);
//    //    }
//    //    @NotNull
//    //    @Override
//    //    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
//    //        return getMemberScope(typeArguments, DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));
//    //    }
//    //    @NotNull
//    //    @Override
//    //    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
//    //        return getMemberScope(typeSubstitution, DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));
//    //    }
//    //    @NotNull
//    //    @Override
//    //    public MemberScope getUnsubstitutedMemberScope() {
//    //        return getUnsubstitutedMemberScope(DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));
//    //    }
//    //
//    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor {
//        if (substitutor.isEmpty) {
//            return this
//        }
//        return LazySubstitutingClassDescriptor(this, substitutor)
//    }
//
//    override fun getDefaultType(): SimpleType {
////        throw new UnsupportedOperationException("Should not be called on " + getClass());
//        return defaultType.invoke()
//    }
//
//    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
//        visitor.visitClassDescriptor(this, null)
//    }
//
//    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
//        return visitor.visitClassDescriptor(this, data)
//    }
//
//    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? {
//        return null
//    }
//
//    override fun isDefinitelyNotSamInterface(): Boolean {
//        return false
//    }
//}
