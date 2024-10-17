package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames.FqNames.core
import com.huawei.cangjie.builtins.StandardNames.MAIN
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.enumd.LazyEnumDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.findParentOfType
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.descriptorUtil.fqNameSafe
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjClassInfoUtil
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import com.huawei.cangjie.resolve.lazy.data.CjTypeStatementInfo
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.storage.MemoizedFunctionToNotNull
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.utils.Printer

abstract class AbstractLazyMemberScope<out D : DeclarationDescriptor, out DP : DeclarationProvider>
protected constructor(
    protected val c: LazyClassContext,
    protected val declarationProvider: DP,
    protected val thisDescriptor: D,
    protected val trace: BindingTrace,
    protected val mainScope: AbstractLazyMemberScope<D, DP>? = null
) : MemberScopeImpl() {
    protected val storageManager: StorageManager = c.storageManager
    private val functionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { doGetFunctions(it) }
    private val mainFunctionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { doGetMainFunctions() }
    private val classDescriptors: MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> =
        storageManager.createMemoizedFunction { doGetClasses(it) }

    private val extendclassDescriptors: MemoizedFunctionToNotNull<Name, List<LazyExtendClassDescriptor>> =
        storageManager.createMemoizedFunction { doGetExtendClasses(it) }

    private val propertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> =
        storageManager.createMemoizedFunction { doGetProperties(it) }

    private val variableDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> =
        storageManager.createMemoizedFunction { doGetVariables(it) }

    private val declaredMainFunctionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { getMainDeclaredFunctions() }
    private val declaredFunctionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredFunctions(it) }
    private val typeAliasDescriptors: MemoizedFunctionToNotNull<Name, Collection<TypeAliasDescriptor>> =
        storageManager.createMemoizedFunction({ doGetTypeAliases(it) }, onRecursiveCall = { _, _ -> emptyList() })
    private val declaredPropertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredProperties(it) }
    private val declaredVariableDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredVariables(it) }

    private fun doGetTypeAliases(name: Name): Collection<TypeAliasDescriptor> {
        mainScope?.typeAliasDescriptors?.invoke(name)?.let { return it }

        return declarationProvider.getTypeAliasDeclarations(name).map { cjTypeAlias ->
            c.descriptorResolver.resolveTypeAliasDescriptor(
                thisDescriptor,
                getScopeForMemberDeclarationResolution(cjTypeAlias),
                cjTypeAlias,
                trace
            )
        }.toList()
    }

    protected abstract fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope

    private fun getDeclaredProperties(
        name: Name
    ): Collection<PropertyDescriptor> {

        // TODO: do we really need to copy descriptors?
        if (mainScope != null) return mainScope.declaredPropertyDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }
        val result = LinkedHashSet<PropertyDescriptor>()
        val propDeclarations = declarationProvider.getPropertyDeclarations(name)
        for (propertyDeclaration in propDeclarations) {
            val propertyDescriptor = c.descriptorResolver.resolvePropertyDescriptor(
                thisDescriptor,
                getScopeForMemberDeclarationResolution(propertyDeclaration),
                getScopeForInitializerResolution(propertyDeclaration),
                propertyDeclaration,
                trace,
                c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(propertyDeclaration),
                c.inferenceSession ?: InferenceSession.default
            )
            result.add(propertyDescriptor)
        }


        return result
    }

    private fun getDeclaredVariables(
        name: Name
    ): Collection<VariableDescriptor> {
        if (mainScope != null) return mainScope.declaredVariableDescriptors(name).map {
//            it.newCopyBuilder().setPreserveSourceElement().build()!!
            it
        }

        val result = LinkedHashSet<VariableDescriptor>()

        val declarations = declarationProvider.getVariableDeclarations(name)
        for (variableDeclaration in declarations) {
            val variableDescriptor = c.descriptorResolver.resolveVariableDescriptor(
                thisDescriptor,
                getScopeForMemberDeclarationResolution(variableDeclaration),
                getScopeForInitializerResolution(variableDeclaration),
                variableDeclaration,
                trace,
                c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(variableDeclaration),
                c.inferenceSession ?: InferenceSession.default
            )
            result.add(variableDescriptor)
        }
        return result

    }

    fun resolveTypeByExtend(declaration: CjExtend, isgetExtend: Boolean = true): ClassDescriptorWithResolutionScopes {
        val scope = getScopeForMemberDeclarationResolution(declaration)

        val classInfo = CjClassInfoUtil.createClassLikeInfo(declaration)

        val extendDescriptor = LazyExtendClassDescriptor(
            c, classInfo, thisDescriptor, declaration.nameAsName, isgetExtend, scope
        )



        return extendDescriptor


    }


    private fun doGetProperties(name: Name): Collection<PropertyDescriptor> {

//        这里返回变量声明和属性声明
        val result = LinkedHashSet(declaredPropertyDescriptors(name))

        getNonDeclaredProperties(name, result)

        return result.toList()
    }


    private fun doGetVariables(name: Name): Collection<VariableDescriptor> {
        val result = LinkedHashSet(declaredVariableDescriptors(name))

//        getNonDeclaredProperties(name, result)

        return result.toList()
    }


    protected abstract fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>)

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        recordLookup(name, location)
        // NB we should resolve type alias descriptors even if a class descriptor with corresponding name is present
        val classes = classDescriptors(name)
        val typeAliases = typeAliasDescriptors(name)

        return classes + typeAliases
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)
        // NB we should resolve type alias descriptors even if a class descriptor with corresponding name is present
        val classes = classDescriptors(name)
        val typeAliases = typeAliasDescriptors(name)
        // See getFirstClassifierDiscriminateHeaders()
        var result: ClassifierDescriptor? = null
        for (cclass in classes) {
//            if (!cclass.isExpect) return cclass
            if (result == null) result = cclass
        }
        for (typeAlias in typeAliases) {

            if (result == null) result = typeAlias
        }
//        if ((result?.source as? CangJieSourceElement)?.psi?.isValid == false) {
//            throw AssertionError("PSI is invalidated for contributed classifier ${result.fqNameSafe}")
//        }


        return result
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        recordLookup(name, location)
        return variableDescriptors(name)
    }

    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PropertyDescriptor> {
        recordLookup(name, location)
        return propertyDescriptors(name)
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("thisDescriptor = ", thisDescriptor)

        p.popIndent()
        p.println("}")
    }

    protected fun computeDescriptorsFromDeclaredElements(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        location: LookupLocation
    ): MutableSet<DeclarationDescriptor> {
        val declarations = declarationProvider.getDeclarations(kindFilter, nameFilter)
        val result = LinkedHashSet<DeclarationDescriptor>(declarations.size)
        for (declaration in declarations) {
            when (declaration) {
                is CjTypeStatement -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(classDescriptors(name))
                    }
                }

                is CjFunction -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedFunctions(name, location))
                    }
                }

                is CjVariable -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedVariables(name, location))
                    }
                }

                is CjProperty -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedPropertys(name, location))
                    }
                }

                is CjParameter -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedVariables(name, location))
                    }
                }

                is CjTypeAlias -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedTypeAliasDescriptors(name, location))
                    }
                }

                is CjDestructuringDeclaration -> {
                    collectDescriptorsFromDestructingDeclaration(result, declaration, nameFilter, location)
                }

                else -> throw IllegalArgumentException("Unsupported declaration kind: " + declaration)
            }
        }
        return result
    }

    protected open fun collectDescriptorsFromDestructingDeclaration(
        result: MutableSet<DeclarationDescriptor>,
        declaration: CjDestructuringDeclaration,
        nameFilter: (Name) -> Boolean,
        location: LookupLocation,
    ) {
        // MultiDeclarations are not supported on global level by default
    }

    protected fun getContributedTypeAliasDescriptors(
        name: Name,
        location: LookupLocation
    ): Collection<TypeAliasDescriptor> {
        recordLookup(name, location)
        return typeAliasDescriptors(name)
    }

    abstract fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration): LexicalScope
    private fun getMainDeclaredFunctions(

    ): Collection<SimpleFunctionDescriptor> {
        if (mainScope != null) return mainScope.declaredMainFunctionDescriptors(MAIN).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }
        val result = linkedSetOf<SimpleFunctionDescriptor>()

        val declarations = declarationProvider.getMainFunctionDeclarations()
        for (functionDeclaration in declarations) {
            result.add(
                c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(functionDeclaration),
                    functionDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration),
                    c.inferenceSession
                )
            )
        }

        return result
    }

    private fun getDeclaredFunctions(
        name: Name
    ): Collection<SimpleFunctionDescriptor> {
        // TODO: do we really need to copy descriptors?
        if (mainScope != null) return mainScope.declaredFunctionDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }

        val result = linkedSetOf<SimpleFunctionDescriptor>()
        val declarations = declarationProvider.getFunctionDeclarations(name).toMutableList().apply {
//            搜索扩展
//            if (this@AbstractLazyMemberScope is LazyClassMemberScope) {
//                extendDeclarationProvider.forEach {
//                    addAll(it.getFunctionDeclarations(name))
//                }
//            }
        }
        for (functionDeclaration in declarations) {
            result.add(
                c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(functionDeclaration),
                    functionDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration),
                    c.inferenceSession
                )
            )
        }

        return result
    }

    private fun doGetMainFunctions(): Collection<SimpleFunctionDescriptor> {
        return LinkedHashSet(declaredMainFunctionDescriptors.invoke(MAIN))
    }

    private fun doGetFunctions(name: Name): Collection<SimpleFunctionDescriptor> {
        val result = LinkedHashSet(declaredFunctionDescriptors.invoke(name))

        getNonDeclaredFunctions(name, result)

        return result.toList()
    }

    protected abstract fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>)

    private fun createClassDescriptor(name: Name, types: Collection<CjTypeStatementInfo<*>>): List<ClassDescriptor> {
        val result = mutableListOf<ClassDescriptor>()

        val enumList = mutableListOf<CjEnmuEntryInfo>()
        val isExternal = /*it.modifierList?.hasModifier(CjTokens.EXTERNAL_KEYWORD) ?:*/ false

        types.forEach {

            when (it.classKind) {
                ClassKind.ENUM -> {
                    result.add(LazyEnumDescriptor(c, it, thisDescriptor, name))
                }

                ClassKind.ENUM_ENTRY -> {
                    result.add(
                        c.enumDescriptorResolver.resolveEnumEntryDescriptor(
                            c, thisDescriptor, name, it as CjEnmuEntryInfo, isExternal
                        )
                    )
                }

                else -> {
                    result.add(LazyClassDescriptor(c, thisDescriptor, name, it, isExternal))

                }
            }

//            }
        }

//        if (enumList.isNotEmpty()) {
//            result.add(
//                c.enumDescriptorResolver.resolveLazyEnumEntryDescriptor(
//                    c, thisDescriptor, name, enumList, isExternal
//                )
//            )
//        }

        return result.toList()
    }

    private fun doGetClasses(name: Name): List<ClassDescriptor> {
        mainScope?.classDescriptors?.invoke(name)?.let { return it }

        val result = linkedSetOf<ClassDescriptor>()
        val result1 = linkedSetOf<ClassDescriptor>()

        result.addAll(createClassDescriptor(name, declarationProvider.getTypeStatementDeclarations(name)))

        declarationProvider.getEnumEntryDeclarations(name).groupBy {
            it.findParentOfType<CjEnum>()
        }.forEach { cjenum, cjentry ->

            val entryName = Name.identifier(cjenum?.name + "")
            createClassDescriptor(
                entryName,
                declarationProvider.getTypeStatementDeclarations(entryName)
            ).forEach { classDescriptor ->
                classDescriptor.unsubstitutedMemberScope.getContributedClassifiers(
                    name,
                    NoLookupLocation.FROM_IDE
                ).forEach { enumEntryClassDescriptor ->
                    (enumEntryClassDescriptor as? ClassDescriptor)?.let { it1 -> result1.add(it1) }
                }


            }
        }
//        declarationProvider.getEnumEntryDeclarations(name).forEach {
//
//            val enum = it.findParentOfType<CjEnum>()
//            val entryName = Name.identifier(enum?.name + "")
//
//            createClassDescriptor(
//                entryName,
//                declarationProvider.getTypeStatementDeclarations(entryName)
//            ).forEach { classDescriptor ->
//                classDescriptor.unsubstitutedMemberScope.getContributedClassifiers(
//                    name,
//                    NoLookupLocation.FROM_IDE
//                ).forEach { enumEntryClassDescriptor ->
//                    (enumEntryClassDescriptor as? ClassDescriptor)?.let { it1 -> result1.add(it1) }
//                }
//
//
//            }
//
//
//        }
        getNonDeclaredClasses(name, result)


//        为std.core添加内置类型
        if (this.thisDescriptor.fqNameSafe == core) {
            try {
                CangJieBuiltIns.Companion.BuiltCangJieTypeName.entries.filter {
                    it.typeName == name
                }.forEach { _ ->
                    result.add(c.moduleDescriptor.builtIns.getPrimitiveClassBuiltInDescriptor(name))

                }

            } catch (_: AssertionError) {

            }
        }


        return result.toList() + result1.toList()
    }


    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return extendclassDescriptors(name)
    }


//    override fun getFunctionClassDescriptor(parameterCount: Int): FunctionClassDescriptor {
//
//        return FunctionClassDescriptor.create(storageManager, thisDescriptor, FunctionTypeKind.Function, parameterCount)
//    }

    private fun doGetExtendClasses(name: Name): List<LazyExtendClassDescriptor> {
        mainScope?.extendclassDescriptors?.invoke(name)?.let { return it }


        val result = linkedSetOf<LazyExtendClassDescriptor>()
        declarationProvider.getExtendTypeStatementDeclarations(name).mapTo(result) {
//            val isExternal = /*it.modifierList?.hasModifier(CjTokens.EXTERNAL_KEYWORD) ?:*/ false
//            LazyClassDescriptor(c, thisDescriptor, name, it, isExternal)


            resolveTypeByExtend(it.scopeAnchor as CjExtend, false) as LazyExtendClassDescriptor
        }

        return result.toList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        recordLookup(name, location)

        return if (name == MAIN) {
            mainFunctionDescriptors(MAIN)
        } else {
// TODO 如果在推断方法返回值类型时，方法返回了自己，那么这里会报出递归错误
            functionDescriptors(name)
        }

    }
//    override fun getExtendContributedClassifier(element: CjExtend, location: LookupLocation) {
//     c.extendDescriptorResolver
//
//
//        TODO()
//    }

    protected abstract fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>)

}
