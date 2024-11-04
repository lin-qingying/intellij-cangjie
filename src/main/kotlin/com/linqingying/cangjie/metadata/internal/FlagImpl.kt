package com.linqingying.cangjie.metadata.internal

import com.linqingying.cangjie.metadata.deserialization.Flags as F

class FlagImpl(internal val offset: Int, internal val bitWidth: Int, internal val value: Int) {
    @IgnoreInApiDump
    internal constructor(field: F.FlagField<*>, value: Int) : this(field.offset, field.bitWidth, value)

    @IgnoreInApiDump
    internal constructor(field: F.BooleanFlagField) : this(field, 1)

    internal operator fun plus(flags: Int): Int =
        (flags and (((1 shl bitWidth) - 1) shl offset).inv()) + (value shl offset)

    operator fun invoke(flags: Int): Boolean = (flags ushr offset) and ((1 shl bitWidth) - 1) == value
}
