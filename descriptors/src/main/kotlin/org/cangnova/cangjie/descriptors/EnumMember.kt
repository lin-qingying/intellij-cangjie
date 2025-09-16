package org.cangnova.cangjie.descriptors

/**
 * 枚举成员标记接口，用于表示属于枚举类型的声明（方法，属性，相关成员）。
 *
 * 该接口作为类型标记，便于在处理描述符时区分普通声明与枚举相关声明。
 */
interface EnumMember : DeclarationDescriptor
