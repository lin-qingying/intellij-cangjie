package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.storage.MemoizedFunctionToNotNull
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
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
    private val classDescriptors: MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> =
        storageManager.createMemoizedFunction { doGetClasses(it) }

    private val propertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> =
        storageManager.createMemoizedFunction { doGetProperties(it) }

    private val variableDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> =
        storageManager.createMemoizedFunction { doGetVariables(it) }


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

    private fun getDeclaredVariables(
        name: Name
    ): Collection<VariableDescriptor> {
        if (mainScope != null) return mainScope.declaredVariableDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
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

    fun resolveTypeByExtend(declaration: CjExtend): CangJieType? {
        val scope = getScopeForMemberDeclarationResolution(declaration)
        val typeReceiver = declaration.receiverTypeReceiver ?: return null
//   scope.findFirstClassifierWithDeprecationStatus
        val type = c.typeResolver.resolveType(scope, typeReceiver, trace, false)

        return type


    }

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
//        val variableDeclarations = declarationProvider.getVariableDeclarations(name)
//        for (variableDeclaration in variableDeclarations) {
//            val propertyDescriptor = c.descriptorResolver.resolvePropertyDescriptor(
//                thisDescriptor,
//                getScopeForMemberDeclarationResolution(variableDeclaration),
//                getScopeForInitializerResolution(variableDeclaration),
//                variableDeclaration,
//                trace,
//                c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(variableDeclaration),
//                c.inferenceSession ?: InferenceSession.default
//            )
//            result.add(propertyDescriptor)
//        }


        return emptyList()
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
                        result.addAll(getContributedVariables(name, location))
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

    protected abstract fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration): LexicalScope

    private fun getDeclaredFunctions(
        name: Name
    ): Collection<SimpleFunctionDescriptor> {
        // TODO: do we really need to copy descriptors?
        if (mainScope != null) return mainScope.declaredFunctionDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }

        val result = linkedSetOf<SimpleFunctionDescriptor>()
        val declarations = declarationProvider.getFunctionDeclarations(name)
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

    private fun doGetFunctions(name: Name): Collection<SimpleFunctionDescriptor> {
        val result = LinkedHashSet(declaredFunctionDescriptors.invoke(name))

        getNonDeclaredFunctions(name, result)

        return result.toList()
    }

    protected abstract fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>)

    private fun doGetClasses(name: Name): List<ClassDescriptor> {
        mainScope?.classDescriptors?.invoke(name)?.let { return it }

        val result = linkedSetOf<ClassDescriptor>()
        declarationProvider.getTypeStatementDeclarations(name).mapTo(result) {
            val isExternal = /*it.modifierList?.hasModifier(CjTokens.EXTERNAL_KEYWORD) ?:*/ false
            LazyClassDescriptor(c, thisDescriptor, name, it, isExternal)
        }
        getNonDeclaredClasses(name, result)
        return result.toList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        recordLookup(name, location)
        return functionDescriptors(name)
    }
//    override fun getExtendContributedClassifier(element: CjExtend, location: LookupLocation) {
//     c.extendDescriptorResolver
//
//
//        TODO()
//    }

    protected abstract fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>)

}
