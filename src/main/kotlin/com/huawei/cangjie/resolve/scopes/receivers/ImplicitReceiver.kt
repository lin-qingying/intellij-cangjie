package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.Name

/**
 * Describes an implicit "this" receiver
 */
interface ImplicitReceiver : ReceiverValue {
    val declarationDescriptor: DeclarationDescriptor
}


interface ImplicitContextReceiver : ImplicitReceiver {
    val customLabelName: Name?
}
