package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.LockBasedLazyResolveStorageManager

open class LazyDeclarationResolver(
    globalContext: GlobalContext,
    delegationTrace: BindingTrace,
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val absentDescriptorHandler: AbsentDescriptorHandler
) {
    private val trace: BindingTrace

    private val bindingContext: BindingContext
        get() = trace.bindingContext

    init {
        val lockBasedLazyResolveStorageManager = LockBasedLazyResolveStorageManager(globalContext.storageManager)

        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace)
    }

    open fun getClassDescriptor(classOrObject: CjTypeStatement, location: LookupLocation): ClassDescriptor =
        findClassDescriptor(classOrObject, location)

    private fun findClassDescriptorIfAny(
        typeStatement: CjNamedDeclaration,
        location: LookupLocation
    ): ClassDescriptor? {

        if (typeStatement is CjExtend) {
            return getExtendClassDescriptor(typeStatement) as? ClassDescriptor
        }

        val scope = getMemberScopeDeclaredIn(typeStatement, location)

        // Why not use the result here. Because it may be that there is a redeclaration:
        //     class A {} class A { fun foo(): A<completion here>}
        // and if we find the class by name only, we may b-not get the right one.
        // This call is only needed to make sure the classes are written to trace
        scope.getContributedClassifier(typeStatement.nameAsSafeName, location)
        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, typeStatement)

        return descriptor as? ClassDescriptor
    }

    private fun getExtendClassDescriptor(cjExtend: CjExtend): DeclarationDescriptor {

        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, cjExtend)

        if (descriptor == null) {
            val scope = getMemberScopeDeclaredIn(cjExtend, NoLookupLocation.FROM_BUILTINS)


            scope as AbstractLazyMemberScope<*, *>

//       第一步，检查被扩展类型并获取

            val type = scope.resolveTypeByExtend(cjExtend)
//                    trace.record(BindingContext.DECLARATION_TO_DESCRIPTOR,cjExtend)
            return type
        }

        return descriptor


    }

    private fun findClassDescriptor(
        classObjectOrScript: CjNamedDeclaration,
        location: LookupLocation
    ): ClassDescriptor =
        findClassDescriptorIfAny(classObjectOrScript, location)
            ?: (absentDescriptorHandler.diagnoseDescriptorNotFound(classObjectOrScript) as ClassDescriptor)


//    fun resolveToDescriptor(declaration: CjDeclaration): DeclarationDescriptor =
//        resolveToDescriptor(declaration, /*track =*/true) ?: absentDescriptorHandler.diagnoseDescriptorNotFound(declaration)

    fun resolveToDescriptor(declaration: CjDeclaration): DeclarationDescriptor {

        return resolveToDescriptor(declaration, /*track =*/true) ?: absentDescriptorHandler.diagnoseDescriptorNotFound(
            declaration
        )


    }

    open fun getClassDescriptorIfAny(typeStatement: CjTypeStatement, location: LookupLocation): ClassDescriptor? =
        findClassDescriptorIfAny(typeStatement, location)


    private fun resolveToDescriptor(declaration: CjDeclaration, track: Boolean): DeclarationDescriptor? {
        return declaration.accept(object : CjVisitor<DeclarationDescriptor?, Nothing?>() {
            private fun lookupLocationFor(declaration: CjDeclaration, isTopLevel: Boolean): LookupLocation =
                if (isTopLevel && track) CangJieLookupLocation(declaration)
                else NoLookupLocation.MATCH_RESOLVE_DECLARATION

            override fun visitNamedFunction(function: CjNamedFunction, data: Nothing?): DeclarationDescriptor? {
                val location = lookupLocationFor(function, function.isTopLevel)
                val scopeForDeclaration = getMemberScopeDeclaredIn(function, location)
                scopeForDeclaration.getContributedFunctions(function.nameAsSafeName, location)
                return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function)
            }

            override fun visitParameter(parameter: CjParameter, data: Nothing?): DeclarationDescriptor? {
                when (val grandFather = parameter.parent.parent) {
                    is CjPrimaryConstructor -> {
                        val ktClassOrObject = grandFather.getContainingTypeStatement()
                        // This is a primary constructor parameter
                        val classDescriptor =
                            getClassDescriptorIfAny(ktClassOrObject, lookupLocationFor(ktClassOrObject, false))
                        return when {
                            classDescriptor == null -> null
                            parameter.hasLetOrVar() -> {
                                classDescriptor.defaultType.memberScope.getContributedVariables(
                                    parameter.nameAsSafeName, lookupLocationFor(parameter, false)
                                )
                                bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
                            }

                            else -> {
                                val constructor = classDescriptor.unsubstitutedPrimaryConstructor
                                    ?: error("There are constructor parameters found, so a constructor should also exist")
                                constructor.valueParameters
                                bindingContext.get(BindingContext.VALUE_PARAMETER, parameter)
                            }
                        }
                    }

                    is CjMainFunction -> {

                        val function = visitMainFunction(grandFather, data) as? FunctionDescriptor
                        function?.valueParameters
                        return bindingContext.get(BindingContext.VALUE_PARAMETER, parameter)

                    }

                    is CjNamedFunction -> {
                        val function = visitNamedFunction(grandFather, data) as? FunctionDescriptor
                        function?.valueParameters
                        return bindingContext.get(BindingContext.VALUE_PARAMETER, parameter)
                    }

                    is CjSecondaryConstructor -> {
                        val constructorDescriptor = visitSecondaryConstructor(
                            grandFather, data
                        ) as? ConstructorDescriptor
                        constructorDescriptor?.valueParameters
                        return bindingContext.get(BindingContext.VALUE_PARAMETER, parameter)
                    }

                    else -> //TODO: support parameters in accessors and other places(?)
                        return super.visitParameter(parameter, data)
                }
            }

            override fun visitMainFunction(cjMainFunction: CjMainFunction, data: Nothing?): DeclarationDescriptor? {
                val location = lookupLocationFor(cjMainFunction, true)
                val scopeForDeclaration = getMemberScopeDeclaredIn(cjMainFunction, location)

                scopeForDeclaration.getContributedFunctions(cjMainFunction.nameAsSafeName, location)
                return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, cjMainFunction)


            }

            override fun visitExtend(cjExtend: CjExtend, data: Nothing?): DeclarationDescriptor {

                return getExtendClassDescriptor(cjExtend)
            }

            override fun visitSecondaryConstructor(
                constructor: CjSecondaryConstructor,
                data: Nothing?
            ): DeclarationDescriptor? {
                getClassDescriptorIfAny(
                    constructor.parent?.parent as CjTypeStatement,
                    lookupLocationFor(constructor, false)
                )?.constructors
                return bindingContext.get(BindingContext.CONSTRUCTOR, constructor)
            }

            override fun visitPrimaryConstructor(
                constructor: CjPrimaryConstructor,
                data: Nothing?
            ): DeclarationDescriptor? {
                getClassDescriptorIfAny(
                    constructor.getContainingTypeStatement(),
                    lookupLocationFor(constructor, false)
                )?.constructors
                return bindingContext.get(BindingContext.CONSTRUCTOR, constructor)
            }

            override fun visitTypeStatement(typeStatement: CjTypeStatement, data: Nothing?): DeclarationDescriptor? =
                getClassDescriptorIfAny(typeStatement, lookupLocationFor(typeStatement, true))

            override fun visitClass(cclass: CjClass, data: Nothing?): DeclarationDescriptor? {
                return visitTypeStatement(cclass, data)
            }

            override fun visitEnumEntry(cjEnumEntry: CjEnumEntry , data: Nothing?): DeclarationDescriptor? {
                return visitTypeStatement(cjEnumEntry, data)

            }
            override fun visitInterface(cinterface: CjInterface, data: Nothing?): DeclarationDescriptor? {
                return visitTypeStatement(cinterface, data)
            }

            override fun visitEnum(cenum: CjEnum, data: Nothing?): DeclarationDescriptor? {
                return visitTypeStatement(cenum, data)
            }

            override fun visitStruct(cstruct: CjStruct, data: Nothing?): DeclarationDescriptor? {
                return visitTypeStatement(cstruct, data)
            }

            //            类型别名
            override fun visitTypeAlias(typeAlias: CjTypeAlias, data: Nothing?): DeclarationDescriptor? {
//                类型别名一定是顶层声明的
                val location = lookupLocationFor(typeAlias, /*typeAlias.isTopLevel()*/ true)
                val scopeForDeclaration = getMemberScopeDeclaredIn(typeAlias, location)
                scopeForDeclaration.getContributedClassifier(typeAlias.nameAsSafeName, location)
                return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, typeAlias)
            }

            override fun visitVariable(variable: CjVariable, data: Nothing?): DeclarationDescriptor? {
                val location = lookupLocationFor(variable, variable.isTopLevel)
                val scopeForDeclaration = getMemberScopeDeclaredIn(variable, location)
                scopeForDeclaration.getContributedVariables(variable.nameAsSafeName, location)
                return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variable)
            }

            override fun visitProperty(property: CjProperty, data: Nothing?): DeclarationDescriptor? {
                val location = lookupLocationFor(property, false)
                val scopeForDeclaration = getMemberScopeDeclaredIn(property, location)
                scopeForDeclaration.getContributedPropertys(property.nameAsSafeName, location)
                return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
            }

            override fun visitCjElement(element: CjElement, data: Nothing?): DeclarationDescriptor {
                throw IllegalArgumentException(
                    "Unsupported declaration type: " + element + " " +
                            element.getElementTextWithContext()
                )
            }
        }, null)
    }


    internal fun getMemberScopeDeclaredIn(declaration: CjDeclaration, location: LookupLocation):
            /*package*/ MemberScope {
        val parentDeclaration = CjStubbedPsiUtil.getContainingDeclaration(declaration)
        val isTopLevel = parentDeclaration == null
        if (isTopLevel) { // for top level declarations we search directly in package because of possible conflicts with imports
            val cjFile = declaration.containingFile as CjFile
            val fqName = cjFile.packageFqName
            topLevelDescriptorProvider.assertValid()
            val packageDescriptor = topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(fqName, cjFile)
            return packageDescriptor.getMemberScope()
        } else {
            return when (parentDeclaration) {
                is CjTypeStatement -> getClassDescriptor(parentDeclaration, location).unsubstitutedMemberScope

                else -> throw IllegalStateException(
                    "Don't call this method for local declarations: " + declaration + "\n" +
                            declaration.getElementTextWithContext()
                )
            }
        }
    }
}
