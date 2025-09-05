package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.name.CangJieClassName

interface FacadeClassSource {
    val className: CangJieClassName
    val facadeClassName: CangJieClassName?
}
