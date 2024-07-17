package com.huawei.cangjie.descriptors

import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue

abstract class DescriptorVisibility protected constructor(){

    abstract val delegate: Visibility

    val name: String
        get() = delegate.name

    val isPublicAPI: Boolean
        get() = delegate.isPublicAPI

    abstract val internalDisplayName: String

    abstract fun normalize(): DescriptorVisibility
    /**
     * @param receiver can be used to determine callee accessibility for some special receiver value
     *
     * 'null'-value basically means that receiver is absent in current call
     *
     * In case if it's needed to perform basic checks ignoring ones considering receiver (e.g. when checks happen beyond any call),
     * special value Visibilities.ALWAYS_SUITABLE_RECEIVER should be used.
     * If it's needed to determine whether visibility accepts any receiver, Visibilities.IRRELEVANT_RECEIVER should be used.
     *
     * NB: Currently Visibilities.IRRELEVANT_RECEIVER has the same effect as 'null'
     *
     * Also it's important that implementation that take receiver into account do aware about these special values.
     */
//    abstract fun isVisible(
//        receiver: ReceiverValue?,
//        what: DeclarationDescriptorWithVisibility,
//        from: DeclarationDescriptor,
//        useSpecialRulesForPrivateSealedConstructors: Boolean
//    ): Boolean
    /**
     * @return null if the answer is unknown
     */
    fun compareTo(visibility: DescriptorVisibility): Int? {
        return delegate.compareTo(visibility.delegate)
    }
}

abstract class DelegatedDescriptorVisibility(override val delegate: Visibility) : DescriptorVisibility() {
//    override fun mustCheckInImports(): Boolean {
//        return delegate.mustCheckInImports()
//    }
//
//    // internal representation for descriptors
    override val internalDisplayName: String
        get() = delegate.internalDisplayName

//    // external representation for diagnostics
//    override val externalDisplayName: String
//        get() = delegate.externalDisplayName
//
    override fun normalize(): DescriptorVisibility = DescriptorVisibilities.toDescriptorVisibility(delegate.normalize())
}
