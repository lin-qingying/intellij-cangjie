package com.huawei.cangjie.descriptors

// For sealed classes, isOverridable is false but isOverridableByMembers is true
enum class Modality {
    // THE ORDER OF ENTRIES MATTERS HERE
    FINAL,
    // NB: class can be sealed but not function or property
    SEALED,
    OPEN,
    ABSTRACT;

    companion object {
        fun convertFromFlags(sealed: Boolean, abstract: Boolean, open: Boolean): Modality {
            return when {
                sealed -> SEALED
                abstract -> ABSTRACT
                open -> OPEN
                else -> FINAL
            }
        }
    }
}
