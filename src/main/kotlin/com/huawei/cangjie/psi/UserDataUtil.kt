package com.huawei.cangjie.psi

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty


class UserDataProperty<in R : UserDataHolder, T : Any>(val key: Key<T>) {
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.getUserData(key)

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T?) = thisRef.putUserData(key, value)
}
class NotNullableUserDataProperty<in R : UserDataHolder, T : Any>(val key: Key<T>, val defaultValue: T) {
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.getUserData(key) ?: defaultValue

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
        thisRef.putUserData(key, if (value != defaultValue) value else null)
    }
}
class NotNullablePsiCopyableUserDataProperty<in R : PsiElement, T : Any>(val key: Key<T>, val defaultValue: T) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key) ?: defaultValue

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        thisRef.putCopyableUserData(key, if (value != defaultValue) value else null)
    }
}
