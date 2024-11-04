package com.linqingying.cangjie.serialization.deserialization


interface EnumEntriesDeserializationSupport {
    /**
     * Determines whether `Enum.entries` property can be synthesized for enums in this module,
     * when this property is not present in compiled code.
     * Returns `null` if it's not known.
     */
    fun canSynthesizeEnumEntries(): Boolean?

    object Default : EnumEntriesDeserializationSupport {
        override fun canSynthesizeEnumEntries(): Boolean? = null
    }
}
