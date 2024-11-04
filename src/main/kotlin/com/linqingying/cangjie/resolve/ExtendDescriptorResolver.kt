package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.psi.CjExtend
import com.linqingying.cangjie.resolve.lazy.FileScopeProvider
import com.linqingying.cangjie.resolve.lazy.LazyDeclarationResolver
import com.linqingying.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.storage.StorageManager

class ExtendDescriptorResolver(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val fileScopeProvider: FileScopeProvider,
    private val descriptorResolver: DescriptorResolver,

    private val typeResolver: TypeResolver,

    private val overloadChecker: OverloadChecker,
    private val builtIns: CangJieBuiltIns,

    private val storageManager: StorageManager

) {

    fun getExtendDescriptor(cjExtend: CjExtend): ClassDescriptorWithResolutionScopes {
        //        val receiverTypeReceiver = cjExtend.receiverTypeReceiver ?: return
        val scope = lazyDeclarationResolver.getMemberScopeDeclaredIn(cjExtend, NoLookupLocation.FROM_BUILTINS)
//        val lexicalScope = fileScopeProvider.getFileResolutionScope(cjExtend.getContainingCjFile())


        scope as AbstractLazyMemberScope<*, *>


        val type  = scope.resolveTypeByExtend(cjExtend)

        return type
    }

    fun check(c: TopDownAnalysisContext, cjExtend: CjExtend) {
        val type = getExtendDescriptor(cjExtend)
        type ?: return
        type as LazyExtendClassDescriptor
        if (type.getSourceClassKind() == ClassKind.INTERFACE) {

            cjExtend.receiverTypeReceiver?.let { trace.report(Errors.EXTEND_CANNOT_INTERFACE.on(it)) }
            return
        }

        c.declaredClasses[cjExtend] = type

//        val classDescriptor = type.constructor.declarationDescriptor as? LazyClassDescriptor ?: return


//        classDescriptor.setExtendData(cjExtend,trace,lexicalScope)
//        classDescriptor as LazyClassDescriptor
//        c.declaredClasses[cjExtend] = classDescriptor


//        第二步，获取被扩展的类型


//

    }
}
