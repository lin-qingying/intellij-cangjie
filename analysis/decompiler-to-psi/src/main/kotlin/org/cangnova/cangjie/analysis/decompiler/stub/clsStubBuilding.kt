package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames

fun computeParameterName(name: Name): Name {
    return when {
        name == SpecialNames.IMPLICIT_SET_PARAMETER -> StandardNames.DEFAULT_VALUE_PARAMETER
        SpecialNames.isAnonymousParameterName(name) -> Name.identifier("_")
        else -> name
    }
}