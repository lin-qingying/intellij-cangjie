package com.linqingying.cangjie.resolve.lazy.descriptors

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.AbstractClassDescriptor
import com.linqingying.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.reportOnDeclarationOrFail
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.incremental.record
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.descriptorUtil.reportOnDeclarationAs
import com.linqingying.cangjie.resolve.lazy.LazyClassContext
import com.linqingying.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.NullableLazyValue
import com.linqingying.cangjie.storage.getValue
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeRefinement
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewCangJieTypeCheckerImpl

open class LazyClassMemberScope(
    c: LazyClassContext,
    declarationProvider: ClassMemberDeclarationProvider,
    thisClass: ClassDescriptorWithResolutionScopes,
    trace: BindingTrace,
    private val cangjieTypeRefiner: CangJieTypeRefiner = c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner,
    scopeForDeclaredMembers: LazyClassMemberScope? = null
) : AbstractLazyMemberScope<ClassDescriptorWithResolutionScopes, ClassMemberDeclarationProvider>(
    c, declarationProvider, thisClass, trace, scopeForDeclaredMembers
) {
    override fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope =
        thisDescriptor.scopeForInitializerResolution

    protected open fun createPropertiesFromPrimaryConstructorParameters(
        name: Name,
        result: MutableSet<VariableDescriptor>
    ) {

        // From primary constructor parameters
        val primaryConstructor = getPrimaryConstructor() ?: return

        val valueParameterDescriptors = primaryConstructor.valueParameters
        val primaryConstructorParameters = declarationProvider.primaryConstructorParameters
        assert(valueParameterDescriptors.size == primaryConstructorParameters.size) {
            "From descriptor: ${valueParameterDescriptors.size} but from PSI: ${primaryConstructorParameters.size}"
        }

        for (valueParameterDescriptor in valueParameterDescriptors) {
            if (name != valueParameterDescriptor.name) continue

            val parameter = primaryConstructorParameters.get(valueParameterDescriptor.index)
            if (parameter.hasLetOrVar()) {
                val propertyDescriptor =
                    trace.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
                        ?: c.descriptorResolver.resolvePrimaryConstructorParameterToAVariable(
                            // TODO: can't test because we get types from cache for this case
                            thisDescriptor,
                            valueParameterDescriptor,
                            thisDescriptor.scopeForConstructorHeaderResolution,
                            parameter,
                            trace
                        )
                result.add(propertyDescriptor)
            }
        }
    }

    override fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>) {
        createPropertiesFromPrimaryConstructorParameters(name, result)

    }
    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {


        // Members from supertypes
        val fromSupertypes = ArrayList<PropertyDescriptor>()
        for (supertype in supertypes) {
            fromSupertypes.addAll(
                supertype.memberScope.getContributedPropertys(
                    name,
                    NoLookupLocation.FOR_ALREADY_TRACKED
                )
            )
        }
//        result.addAll(generateDelegatingDescriptors(name, EXTRACT_PROPERTIES, result))
        c.syntheticResolveExtension.generateSyntheticProperties(
            thisDescriptor,
            name,
            trace.bindingContext,
            fromSupertypes,
            result
        )
        generateFakeOverrides(name, fromSupertypes, result, PropertyDescriptor::class.java)
    }

    private val allClassifierDescriptors = storageManager.createLazyValue {
        doClassifierDescriptors(ALL_NAME_FILTER)
    }
    private val allDescriptors = storageManager.createLazyValue {
        doDescriptors(ALL_NAME_FILTER)
    }
    val supertypes by storageManager.createLazyValue {
        @OptIn(TypeRefinement::class)
        cangjieTypeRefiner.refineSupertypes(thisDescriptor)
    }
    private val secondaryConstructors: NotNullLazyValue<Collection<ClassConstructorDescriptor>> =
        c.storageManager.createLazyValue { doGetConstructors() }

    private val endSecondaryConstructors: NotNullLazyValue<Collection<ClassConstructorDescriptor>> =
        c.storageManager.createLazyValue { doGetEndConstructors() }


    val extendClassDescriptors: List<LazyExtendClassDescriptor>
        get() {
            return (thisDescriptor as AbstractClassDescriptor).extendClass.toList()
        }

    val extendDeclarationProvider: List<ClassMemberDeclarationProvider>
        get() = extendClassDescriptors.map {
            it.declarationProvider
        }

    private fun doClassifierDescriptors(nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val result = computeDescriptorsFromDeclaredElements(
            DescriptorKindFilter.CLASSIFIERS,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )
//        addSyntheticCompanionObject(result, NoLookupLocation.FOR_ALREADY_TRACKED)
//        addSyntheticNestedClasses(result, NoLookupLocation.FOR_ALREADY_TRACKED)
        return result.toList()
    }

    private fun addSyntheticNestedClasses(result: MutableCollection<DeclarationDescriptor>, location: LookupLocation) {
//        result.addAll(c.syntheticResolveExtension.getSyntheticNestedClassNames(thisDescriptor).mapNotNull {
//            getContributedClassifier(
//                it,
//                location
//            )
//        }.toList())
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return when (kindFilter) {
            DescriptorKindFilter.CLASSIFIERS ->
                if (nameFilter == ALL_NAME_FILTER || allClassifierDescriptors.isComputed() || allClassifierDescriptors.isComputing()) {
                    allClassifierDescriptors()
                } else {
                    storageManager.compute {
                        doClassifierDescriptors(nameFilter)
                    }
                }

            else ->
                if (nameFilter == ALL_NAME_FILTER || allDescriptors.isComputed() || allDescriptors.isComputing()) {
                    allDescriptors()
                } else {
                    storageManager.compute {
                        doDescriptors(nameFilter)
                    }
                }
        }

    }

    private fun doDescriptors(nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val result = computeDescriptorsFromDeclaredElements(
            DescriptorKindFilter.ALL,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )
        computeExtraDescriptors(result, NoLookupLocation.FOR_ALREADY_TRACKED)
        return result.toList()
    }

    protected open fun computeExtraDescriptors(
        result: MutableCollection<DeclarationDescriptor>,
        location: LookupLocation
    ) {
        for (supertype in supertypes) {
            for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                if (descriptor is FunctionDescriptor) {
                    result.addAll(getContributedFunctions(descriptor.name, location))
                } else if (descriptor is PropertyDescriptor) {
                    result.addAll(getContributedPropertys(descriptor.name, location))
                }
                // Nothing else is inherited
            }
        }

//        addDataClassMethods(result, location)
//        addSyntheticFunctions(result, location)
//        addSyntheticVariables(result, location)
//        addSyntheticCompanionObject(result, location)
        addSyntheticNestedClasses(result, location)
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker.record(location, thisDescriptor, name)

    }

    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration): LexicalScope =
        thisDescriptor.scopeForMemberDeclarationResolution

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
        val location = NoLookupLocation.FOR_ALREADY_TRACKED

        val fromSupertypes = arrayListOf<SimpleFunctionDescriptor>()
        for (supertype in supertypes) {
            fromSupertypes.addAll(supertype.memberScope.getContributedFunctions(name, location))
        }


//        扩展

//        if (thisDescriptor !is LazyExtendClassDescriptor) {
//            for (extend in extendClassDescriptors) {
//                result.addAll(extend.unsubstitutedMemberScope.getContributedFunctions(name, location))
//            }
//        }


//
//        result.addAll(generateDelegatingDescriptors(name, EXTRACT_FUNCTIONS, result))
// //数据类
//        generateDataClassMethods(result, name, location, fromSupertypes)

//        值类的函数生成
//        generateFunctionsFromAnyForValueClass(result, name, fromSupertypes)
//        c.syntheticResolveExtension.generateSyntheticMethods(
//            thisDescriptor,
//            name,
//            trace.bindingContext,
//            fromSupertypes,
//            result
//        )

//        c.additionalClassPartsProvider.generateAdditionalMethods(thisDescriptor, result, name, location, fromSupertypes)

        generateFakeOverrides(name, fromSupertypes, result, SimpleFunctionDescriptor::class.java)
    }

    //重写
    private fun <D : CallableMemberDescriptor> generateFakeOverrides(
        name: Name,
        fromSupertypes: Collection<D>,
        result: MutableCollection<D>,
        exactDescriptorClass: Class<out D>
    ) {
        NewCangJieTypeCheckerImpl(cangjieTypeRefiner).overridingUtil.generateOverridesInFunctionGroup(
            name,
            fromSupertypes,
            ArrayList(result),
            thisDescriptor,
            object : OverridingStrategy() {
                override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                    assert(exactDescriptorClass.isInstance(fakeOverride)) { "Wrong descriptor type in an override: " + fakeOverride + " while expecting " + exactDescriptorClass.simpleName }
                    @Suppress("UNCHECKED_CAST")
                    result.add(fakeOverride as D)
                }

                override fun overrideConflict(
                    fromSuper: CallableMemberDescriptor,
                    fromCurrent: CallableMemberDescriptor
                ) {
                    reportOnDeclarationOrFail(
                        trace,
                        fromCurrent
                    ) { Errors.CONFLICTING_OVERLOADS.on(it, listOf(fromCurrent, fromSuper)) }
                }

                override fun inheritanceConflict(
                    first: CallableMemberDescriptor,
                    second: CallableMemberDescriptor
                ) {
                    reportOnDeclarationAs<CjTypeStatement>(
                        trace,
                        thisDescriptor
                    ) { type ->
                        Errors.CONFLICTING_INHERITED_MEMBERS.on(
                            type,
                            thisDescriptor,
                            listOf(first, second)
                        )
                    }
                }
            })
        for (descriptor in result) {
            if (descriptor !is FunctionDescriptorImpl) continue
            for (overriddenFunction in descriptor.overriddenDescriptors) {
                if (overriddenFunction !is FunctionDescriptorImpl) continue
                val conflictedDescriptor = overriddenFunction.getUserData(
                    DeserializedDeclarationsFromSupertypeConflictDataKey
                ) ?: continue
                reportOnDeclarationAs<CjTypeStatement>(
                    trace,
                    thisDescriptor
                ) { type ->
                    Errors.CONFLICTING_INHERITED_MEMBERS_WARNING.on(
                        type,
                        thisDescriptor,
                        listOf(overriddenFunction, conflictedDescriptor)
                    )
                }
            }
        }
        OverrideResolver.resolveUnknownVisibilities(result, trace)
    }

//    private fun <T : CallableMemberDescriptor> generateDelegatingDescriptors(
//        name: Name,
//        extractor: MemberExtractor<T>,
//        existingDescriptors: Collection<CallableDescriptor>
//    ): Collection<T> {
//        val classOrObject = declarationProvider.correspondingClassOrObject ?: return setOf()
//
//        val lazyTypeResolver = object : DelegationResolver.TypeResolver {
//            override fun resolve(reference: CjTypeReference): CangJieType =
//                c.typeResolver.resolveType(thisDescriptor.scopeForClassHeaderResolution, reference, trace, false)
//        }
//        val lazyMemberExtractor = object : DelegationResolver.MemberExtractor<T> {
//            override fun getMembersByType(type: CangJieType): Collection<T> =
//                extractor.extract(type, name)
//        }
//        return DelegationResolver.generateDelegatedMembers(
//            classOrObject, thisDescriptor, existingDescriptors, trace, lazyMemberExtractor,
//            lazyTypeResolver, c.delegationFilter, c.languageVersionSettings
//        )
//    }

    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>) {
//        generateSyntheticCompanionObject(name, result)
//        c.syntheticResolveExtension.generateSyntheticClasses(thisDescriptor, name, c, declarationProvider, result)

    }

    private val enumEntryPrimaryConstructor: NullableLazyValue<ClassConstructorDescriptor> =
        c.storageManager.createNullableLazyValue { resolveEnumEntryPrimaryConstructor() }

    private val primaryConstructor: NullableLazyValue<ClassConstructorDescriptor> =
        c.storageManager.createNullableLazyValue { resolvePrimaryConstructor() }

    protected fun resolveEnumEntryPrimaryConstructor(): ClassConstructorDescriptor {
        val enumEntry = declarationProvider.correspondingClassOrObject as CjEnumEntry

        val descriptor =  c.enumDescriptorResolver.resolbeEnumEntryConstructorDescriptor(
            thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor,
            enumEntry , trace, c.languageVersionSettings, c.inferenceSession
        )
        setDeferredReturnType(descriptor)
        return descriptor

    }

    protected open fun resolvePrimaryConstructor(): ClassConstructorDescriptor? {
        val classOrObject = declarationProvider.correspondingClassOrObject ?: return null
//        if(classOrObject is CjEnum) return null
        val primarys = classOrObject.primaryConstructors.map { constructor ->
            val descriptor = c.functionDescriptorResolver.resolveConstructorDescriptor(
                thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor,
                constructor, trace, c.languageVersionSettings, c.inferenceSession
            )
            setDeferredReturnType(descriptor)
            descriptor
        }
        val hasPrimaryConstructor = classOrObject.hasExplicitPrimaryConstructor()
        if (!hasPrimaryConstructor) {
            if (thisDescriptor.isExpect && !DescriptorUtils.isEnumEntry(thisDescriptor)) return null
            if (DescriptorUtils.isInterface(thisDescriptor)) return null
        }

        if (DescriptorUtils.canHaveDeclaredConstructors(thisDescriptor) || hasPrimaryConstructor) {
//            val constructor = c.functionDescriptorResolver.resolvePrimaryConstructorDescriptor(
//                thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor,
//                classOrObject, trace, c.languageVersionSettings, c.inferenceSession
//            )
            val constructor = if (primarys.isEmpty()) {
                c.functionDescriptorResolver.resolvePrimaryConstructorDescriptor(
                    thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor,
                    classOrObject, trace, c.languageVersionSettings, c.inferenceSession
                )
            } else {
                primarys.first()
            }
            constructor ?: return null
            setDeferredReturnType(constructor)
            return constructor
        }

        val constructor =
            DescriptorResolver.createAndRecordPrimaryConstructorForObject(classOrObject, thisDescriptor, trace)
        setDeferredReturnType(constructor)
        return constructor

    }

    private fun resolveSecondaryConstructors(): Collection<ClassConstructorDescriptor> {
        val classOrObject = declarationProvider.correspondingClassOrObject ?: return emptyList()

        return classOrObject.secondaryConstructors.map { constructor ->
            val descriptor = c.functionDescriptorResolver.resolveConstructorDescriptor(
                thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor,
                constructor, trace, c.languageVersionSettings, c.inferenceSession
            )
            setDeferredReturnType(descriptor)
            descriptor
        }
    }

    protected fun setDeferredReturnType(descriptor: ClassConstructorDescriptorImpl) {
        descriptor.returnType = c.wrappedTypeFactory.createDeferredType(trace, { thisDescriptor.defaultType })
    }

    private fun addSyntheticSecondaryConstructors(result: MutableCollection<ClassConstructorDescriptor>) {
        c.syntheticResolveExtension.generateSyntheticSecondaryConstructors(thisDescriptor, trace.bindingContext, result)
    }

    private fun doGetEndConstructors(): Collection<ClassConstructorDescriptor> {
        val result = mutableListOf<ClassConstructorDescriptor>()
        result.addAll(resolveEndSecondaryConstructors())
        addSyntheticSecondaryConstructors(result)
        return result
    }

    private fun resolveEndSecondaryConstructors(): Collection<ClassConstructorDescriptor> {
        val classOrObject = declarationProvider.correspondingClassOrObject ?: return emptyList()

        return classOrObject.endSecondaryConstructors.map { constructor ->
            val descriptor = c.functionDescriptorResolver.resolveConstructorDescriptor(
                thisDescriptor.scopeForConstructorHeaderResolution, thisDescriptor,
                constructor, trace, c.languageVersionSettings, c.inferenceSession, true
            )
            setDeferredReturnType(descriptor)
            descriptor
        }
    }

    private fun doGetConstructors(): Collection<ClassConstructorDescriptor> {
        val result = mutableListOf<ClassConstructorDescriptor>()
        result.addAll(resolveSecondaryConstructors())
        addSyntheticSecondaryConstructors(result)
        return result
    }

    //    主构造函数
    fun getEnumEntryPrimaryConstructor(): ClassConstructorDescriptor? =
        (mainScope as LazyClassMemberScope?)?.enumEntryPrimaryConstructor?.invoke() ?: enumEntryPrimaryConstructor()

    //    主构造函数
    fun getPrimaryConstructor(): ClassConstructorDescriptor? {
        if(DescriptorUtils.isEnumEntry(thisDescriptor)){
            return getEnumEntryPrimaryConstructor()
        }

        return (mainScope as LazyClassMemberScope?)?.primaryConstructor?.invoke() ?: primaryConstructor()
    }

    fun getConstructors(): Collection<ClassConstructorDescriptor> {
        val result = (mainScope as LazyClassMemberScope?)?.secondaryConstructors?.invoke() ?: secondaryConstructors()
        val primaryConstructor = getPrimaryConstructor()
        return if (primaryConstructor == null) result else result + primaryConstructor
    }

    fun getEndConstructors(): Collection<ClassConstructorDescriptor> {
        val result =
            (mainScope as LazyClassMemberScope?)?.endSecondaryConstructors?.invoke() ?: endSecondaryConstructors()

        return result
    }

    private interface MemberExtractor<out T : CallableMemberDescriptor> {
        fun extract(extractFrom: CangJieType, name: Name): Collection<T>
    }

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for class ${thisDescriptor.name}"

    companion object {
        private val EXTRACT_FUNCTIONS: MemberExtractor<SimpleFunctionDescriptor> =
            object : MemberExtractor<SimpleFunctionDescriptor> {
                override fun extract(extractFrom: CangJieType, name: Name): Collection<SimpleFunctionDescriptor> {
                    return extractFrom.memberScope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED)
                }
            }
        //
//    private val EXTRACT_VARIABLES: MemberExtractor<PropertyDescriptor> = object : MemberExtractor<PropertyDescriptor> {
//        override fun extract(extractFrom: CangJieType, name: Name): Collection<PropertyDescriptor> {
//            return extractFrom.memberScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED)
//        }
//
//    private val EXTRACT_PROPERTIES: MemberExtractor<PropertyDescriptor> = object : MemberExtractor<PropertyDescriptor> {
//        override fun extract(extractFrom: CangJieType, name: Name): Collection<PropertyDescriptor> {
//            return extractFrom.memberScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED)
//        }
    }
}

