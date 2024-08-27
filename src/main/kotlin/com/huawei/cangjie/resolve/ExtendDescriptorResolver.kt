package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.psi.CjExtend
import com.huawei.cangjie.resolve.lazy.LazyDeclarationResolver
import com.huawei.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.error.ErrorType

class ExtendDescriptorResolver(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,

    private val typeResolver: TypeResolver,

    private val overloadChecker: OverloadChecker,
    private val builtIns: CangJieBuiltIns,

    private val storageManager: StorageManager

) {
    fun test(c: TopDownAnalysisContext, cjExtend: CjExtend) {
        val scope = lazyDeclarationResolver.getMemberScopeDeclaredIn(cjExtend, NoLookupLocation.FROM_BUILTINS)

        scope as AbstractLazyMemberScope<*, *>

//       第一步，检查被扩展类型并获取

        val type = scope.resolveTypeByExtend(cjExtend)
        if (type is ErrorType) return


        val classDescriptor = type?.constructor?.declarationDescriptor as? ClassDescriptorWithResolutionScopes ?: return

//        classDescriptor as LazyClassDescriptor
        c.declaredClasses[cjExtend] = classDescriptor


//        第二步，获取被扩展的类型


//

    }
}
