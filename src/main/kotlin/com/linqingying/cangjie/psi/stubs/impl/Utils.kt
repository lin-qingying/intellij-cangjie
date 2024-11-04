package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.util.io.StringRef


    object Utils {
        fun wrapStrings(names: List<String>): Array<StringRef> {
            return Array(names.size) { i -> StringRef.fromString(names[i])!! }
        }
    }


