package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.psi.CjExtend
import com.huawei.cangjie.resolve.lazy.FileScopeProvider
import com.huawei.cangjie.resolve.lazy.LazyDeclarationResolver
import com.huawei.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import com.huawei.cangjie.storage.StorageManager

class ExtendDescriptorResolver(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val fileScopeProvider: FileScopeProvider,

    private val typeResolver: TypeResolver,

    private val overloadChecker: OverloadChecker,
    private val builtIns: CangJieBuiltIns,

    private val storageManager: StorageManager

) {

    fun getExtendDescriptor(cjExtend: CjExtend): ClassDescriptorWithResolutionScopes? {
        //        val receiverTypeReceiver = cjExtend.receiverTypeReceiver ?: return
        val scope = lazyDeclarationResolver.getMemberScopeDeclaredIn(cjExtend, NoLookupLocation.FROM_BUILTINS)
//        val lexicalScope = fileScopeProvider.getFileResolutionScope(cjExtend.getContainingCjFile())


        scope as AbstractLazyMemberScope<*, *>

//       第一步，检查被扩展类型并获取

        val type = scope.resolveTypeByExtend(cjExtend)
        return type
    }

    fun check(c: TopDownAnalysisContext, cjExtend: CjExtend) {
        val type = getExtendDescriptor(cjExtend)
        c.declaredClasses[cjExtend] = type ?: return

//        val classDescriptor = type.constructor.declarationDescriptor as? LazyClassDescriptor ?: return


//        classDescriptor.setExtendData(cjExtend,trace,lexicalScope)
//        classDescriptor as LazyClassDescriptor
//        c.declaredClasses[cjExtend] = classDescriptor


//        第二步，获取被扩展的类型


//

    }
}
