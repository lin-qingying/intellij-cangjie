package com.linqingying.cangjie.resolve.calls.model


sealed class ResolvedCallArgument {
    abstract val arguments: List<CangJieCallArgument>

    object DefaultArgument : ResolvedCallArgument() {
        override val arguments: List<CangJieCallArgument>
            get() = emptyList()

    }

    class SimpleArgument(val callArgument: CangJieCallArgument) : ResolvedCallArgument() {
        override val arguments: List<CangJieCallArgument>
            get() = listOf(callArgument)

    }

    class VarargArgument(override val arguments: List<CangJieCallArgument>) : ResolvedCallArgument()
}
