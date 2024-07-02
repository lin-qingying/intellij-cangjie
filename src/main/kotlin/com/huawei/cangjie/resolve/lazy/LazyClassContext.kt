package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.resolve.DescriptorResolver
import com.huawei.cangjie.resolve.FunctionDescriptorResolver
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.storage.StorageManager

interface LazyClassContext {
    val inferenceSession: InferenceSession?
    val descriptorResolver: DescriptorResolver
    val lookupTracker: LookupTracker?
    val moduleDescriptor: ModuleDescriptor

    val storageManager: StorageManager
    val functionDescriptorResolver: FunctionDescriptorResolver
    val declarationScopeProvider: DeclarationScopeProvider

}