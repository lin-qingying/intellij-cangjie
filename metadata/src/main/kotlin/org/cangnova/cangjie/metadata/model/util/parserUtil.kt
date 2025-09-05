package org.cangnova.cangjie.metadata.model.util

import org.cangnova.cangjie.name.Name


fun String.toName(): Name {
    return Name.identifier(this)
}