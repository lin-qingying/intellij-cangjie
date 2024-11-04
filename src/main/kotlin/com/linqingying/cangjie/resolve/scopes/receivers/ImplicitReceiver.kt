package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.name.Name

/**
 * Describes an implicit "this" receiver
 */
interface ImplicitReceiver : ReceiverValue {
    val declarationDescriptor: DeclarationDescriptor
}


interface ImplicitContextReceiver : ImplicitReceiver {
    val customLabelName: Name?
}
