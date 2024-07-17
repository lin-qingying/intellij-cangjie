package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.resolve.BindingContext
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

    open fun getClassDescriptor(classOrObject: CjClassOrStruct, location: LookupLocation): ClassDescriptor =
        findClassDescriptor(classOrObject, location)
    private fun findClassDescriptorIfAny(
        classObjectOrScript: CjNamedDeclaration,
        location: LookupLocation
    ): ClassDescriptor? {
        val scope = getMemberScopeDeclaredIn(classObjectOrScript, location)

        // Why not use the result here. Because it may be that there is a redeclaration:
        //     class A {} class A { fun foo(): A<completion here>}
        // and if we find the class by name only, we may b-not get the right one.
        // This call is only needed to make sure the classes are written to trace
        scope.getContributedClassifier(classObjectOrScript.nameAsSafeName, location)
        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classObjectOrScript)

        return descriptor as? ClassDescriptor
    }

    private fun findClassDescriptor(
        classObjectOrScript: CjNamedDeclaration,
        location: LookupLocation
    ): ClassDescriptor =
        findClassDescriptorIfAny(classObjectOrScript, location)
            ?: (absentDescriptorHandler.diagnoseDescriptorNotFound(classObjectOrScript) as ClassDescriptor)

    fun resolveToDescriptor(declaration: CjDeclaration): DeclarationDescriptor

    {

        val a = resolveToDescriptor(declaration, /*track =*/true) ?: absentDescriptorHandler.diagnoseDescriptorNotFound(
            declaration
        )

        a.toString()
      return  a

    }
    private fun resolveToDescriptor(declaration: CjDeclaration, track: Boolean): DeclarationDescriptor? {
        return declaration.accept(object : CjVisitor<DeclarationDescriptor?, Nothing?>() {
            private fun lookupLocationFor(declaration: CjDeclaration, isTopLevel: Boolean): LookupLocation =
                if (isTopLevel && track) CangJieLookupLocation(declaration)
                else NoLookupLocation.WHEN_RESOLVE_DECLARATION

            override fun visitNamedFunction(function: CjNamedFunction, data: Nothing?): DeclarationDescriptor? {
                val location = lookupLocationFor(function, function.isTopLevel)
                val scopeForDeclaration = getMemberScopeDeclaredIn(function, location)
                scopeForDeclaration.getContributedFunctions(function.nameAsSafeName, location)
                return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function)
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
                is CjClassOrStruct -> getClassDescriptor(parentDeclaration, location).unsubstitutedMemberScope

                else -> throw IllegalStateException(
                    "Don't call this method for local declarations: " + declaration + "\n" +
                            declaration.getElementTextWithContext()
                )
            }
        }
    }
}
