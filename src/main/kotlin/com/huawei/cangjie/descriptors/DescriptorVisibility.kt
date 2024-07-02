package com.huawei.cangjie.descriptors

abstract class DescriptorVisibility protected constructor(){

    abstract val delegate: Visibility

    val name: String
        get() = delegate.name

    val isPublicAPI: Boolean
        get() = delegate.isPublicAPI


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
//    override val internalDisplayName: String
//        get() = delegate.internalDisplayName
//
//    // external representation for diagnostics
//    override val externalDisplayName: String
//        get() = delegate.externalDisplayName
//
//    override fun normalize(): DescriptorVisibility = DescriptorVisibilities.toDescriptorVisibility(delegate.normalize())
}
