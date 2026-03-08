/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.lazy.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_INHERITED_MEMBERS
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_OVERLOADS
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_STATIC
import org.cangnova.cangjie.diagnostics.infos.warnings.CONFLICTING_INHERITED_MEMBERS_WARNING
import org.cangnova.cangjie.diagnostics.reportOnDeclarationOrFail
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.NullableLazyValue
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.DefaultCangJieTypeChecker
import org.cangnova.cangjie.utils.reportOnDeclarationAs

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
            c,
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

        cangjieTypeRefiner.refineSupertypes(thisDescriptor)
    }
    private val secondaryConstructors: NotNullLazyValue<Collection<ClassConstructorDescriptor>> =
        c.storageManager.createLazyValue { doGetConstructors() }

    private val endSecondaryConstructors: NotNullLazyValue<Collection<ClassConstructorDescriptor>> =
        c.storageManager.createLazyValue { doGetEndConstructors() }


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

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return null
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

        c.syntheticResolveExtension.generateSyntheticMethods(
            thisDescriptor, name, c, trace.bindingContext, fromSupertypes, result
        )

        generateFakeOverrides(name, fromSupertypes, result, SimpleFunctionDescriptor::class.java)
    }

    override fun getNonDeclaredMacros(name: Name, result: MutableSet<MacroDescriptor>) {

    }

    /**
     * 为可调用成员生成伪重写，通过处理超类描述符并处理冲突。
     *
     * 此函数是重写解析过程的一部分，它为需要重写但在当前类中未显式定义的方法生成伪重写。
     * 该函数遍历提供的超类描述符，检查冲突并生成相应的重写。
     *
     * @param name 要重写的成员的名称。
     * @param fromSupertypes 来自超类的可调用成员描述符集合。
     * @param result 用于存储生成的伪重写的集合。
     * @param exactDescriptorClass 预期的描述符类，指定 `fromSupertypes` 集合中的描述符类型。
     *
     * @throws AssertionError 如果伪重写的类型与预期的描述符类不匹配。
     */
    private fun <D : CallableMemberDescriptor> generateFakeOverrides(
        name: Name,
        fromSupertypes: Collection<D>,
        result: MutableCollection<D>,
        exactDescriptorClass: Class<out D>
    ) {
        DefaultCangJieTypeChecker(cangjieTypeRefiner).overridingUtil.generateOverridesInFunctionGroup(
            name,
            fromSupertypes,
            ArrayList(result),
            thisDescriptor,
            object : OverridingStrategy() {
                /**
                 * 将伪重写添加到结果集合，确保类型与预期的类匹配。
                 *
                 * @param fakeOverride 伪重写的描述符。
                 */
                override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                    assert(exactDescriptorClass.isInstance(fakeOverride)) { "Wrong descriptor type in an override: " + fakeOverride + " while expecting " + exactDescriptorClass.simpleName }
                    @Suppress("UNCHECKED_CAST")
                    result.add(fakeOverride as D)
                }

                override fun staticConflict(
                    fromSuper: CallableMemberDescriptor,
                    fromCurrent: CallableMemberDescriptor,
                    message: String
                ) {
                    reportOnDeclarationOrFail(
                        trace,
                        fromCurrent
                    ) { CONFLICTING_STATIC.on(it, listOf(fromCurrent, fromSuper), message) }
                }

                /**
                 * 处理重写冲突，报告冲突的重载。
                 *
                 * @param fromSuper 超类中的描述符。
                 * @param fromCurrent 当前类中的描述符。
                 */
                override fun overrideConflict(
                    fromSuper: CallableMemberDescriptor,
                    fromCurrent: CallableMemberDescriptor
                ) {
                    reportOnDeclarationOrFail(
                        trace,
                        fromCurrent
                    ) { CONFLICTING_OVERLOADS.on(it, listOf(fromCurrent, fromSuper)) }
                }

                /**
                 * 处理继承冲突，报告冲突的成员。
                 *
                 * @param first 第一个冲突的描述符。
                 * @param second 第二个冲突的描述符。
                 */
                override fun inheritanceConflict(
                    first: CallableMemberDescriptor,
                    second: CallableMemberDescriptor
                ) {
                    reportOnDeclarationAs<CjTypeStatement>(
                        trace,
                        thisDescriptor
                    ) { type ->
                        CONFLICTING_INHERITED_MEMBERS.on(
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
                    CONFLICTING_INHERITED_MEMBERS_WARNING.on(
                        type,
                        thisDescriptor,
                        listOf(overriddenFunction, conflictedDescriptor)
                    )
                }
            }
        }
        OverrideResolver.resolveUnknownVisibilities(result, trace)
    }


    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassAndEnumDescriptor>) {
//        generateSyntheticCompanionObject(name, result)
        val syntheticClasses = mutableSetOf<ClassDescriptor>()
        c.syntheticResolveExtension.generateSyntheticNestedClasses(thisDescriptor, name, c, declarationProvider, syntheticClasses)
        result.addAll(syntheticClasses)
    }


    private val primaryConstructor: NullableLazyValue<ClassConstructorDescriptor> =
        c.storageManager.createNullableLazyValue { resolvePrimaryConstructor() }



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
        descriptor.setReturnType(c.wrappedTypeFactory.createDeferredType(trace, { thisDescriptor.defaultType }))
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
//            setDeferredReturnType(descriptor)
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
    fun getPrimaryConstructor(): ClassConstructorDescriptor? {


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

