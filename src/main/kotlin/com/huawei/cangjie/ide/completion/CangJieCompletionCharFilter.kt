package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.psi.NotNullableUserDataProperty
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Key


var LookupElement.hideLookupOnColon: Boolean by NotNullableUserDataProperty(
    Key("CANGJIE_HIDE_LOOKUP_ON_COLON"),
    defaultValue = false,
)
