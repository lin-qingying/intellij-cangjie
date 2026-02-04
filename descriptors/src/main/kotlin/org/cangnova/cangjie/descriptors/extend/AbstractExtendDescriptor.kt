/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.descriptors.extend

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.types.*

/**
 * 类型信息
 */
sealed class TypeInfo {
    /** 基本类型 */
    data class Primitive(val name: String) : TypeInfo() {
        override fun toString() = name
    }

    /** 引用类型 */
    data class Reference(
        val name: String,
        val typeArguments: List<TypeInfo> = emptyList()
    ) : TypeInfo() {
        override fun toString() = if (typeArguments.isEmpty()) {
            name
        } else {
            "$name<${typeArguments.joinToString(", ")}>"
        }
    }

    /** 函数类型 */
    data class Function(
        val paramTypes: List<TypeInfo>,
        val returnType: TypeInfo,
        val isCFunc: Boolean = false
    ) : TypeInfo() {
        override fun toString() = buildString {
            if (isCFunc) append("cfunc ")
            append("(${paramTypes.joinToString(", ")}) -> $returnType")
        }
    }

    /** 元组类型 */
    data class Tuple(val fieldTypes: List<TypeInfo>) : TypeInfo() {
        override fun toString() = "(${fieldTypes.joinToString(", ")})"
    }

    /** Option 类型 */
    data class Option(val componentType: TypeInfo, val questNum: Int) : TypeInfo() {
        override fun toString() = "$componentType${"?".repeat(questNum)}"
    }

    /** 数组类型 */
    data class VArray(val elementType: TypeInfo, val size: TypeInfo?) : TypeInfo() {
        override fun toString() = "Array<$elementType>"
    }

    /** 限定类型 */
    data class Qualified(
        val baseType: TypeInfo,
        val field: String,
        val typeArguments: List<TypeInfo> = emptyList()
    ) : TypeInfo() {
        override fun toString() = buildString {
            append("$baseType.$field")
            if (typeArguments.isNotEmpty()) {
                append("<${typeArguments.joinToString(", ")}>")
            }
        }
    }

    /** this 类型 */
    object This : TypeInfo() {
        override fun toString() = "this"
    }

    /** 未知类型 */
    data class Unknown(val raw: String) : TypeInfo() {
        override fun toString() = raw
    }
}

/**
 * 仓颉 Name Demangling 工具
 *
 * 完整实现编译器 mangled 名称的解析
 * 对应编译器实现: cangjie/Mangle/ASTMangler.cpp 的逆向操作
 *
 * 支持解析：
 * - 包名和标识符
 * - 声明类型（函数、类、接口等）
 * - 泛型参数
 * - 函数参数类型
 * - 返回类型
 * - 局部变量
 * - 嵌套声明
 * - 扩展声明
 */
object NameDemangler {

    /**
     * 解析后的 mangled 名称结构
     */
    data class DemangledName(
        /** 完整包名 */
        val packageName: String,

        /** 标识符名称 */
        val identifier: String,

        /** 声明类型 */
        val declKind: DeclKind? = null,

        /** 嵌套声明链（从外到内）*/
        val nestedDecls: List<NestedDecl> = emptyList(),

        /** 泛型参数 */
        val typeParameters: List<String> = emptyList(),

        /** 函数参数类型 */
        val parameterTypes: List<TypeInfo> = emptyList(),

        /** 返回类型 */
        val returnType: TypeInfo? = null,

        /** 是否为局部变量 */
        val isLocalVar: Boolean = false,

        /** 局部变量索引 */
        val localVarIndex: Int? = null,

        /** 是否为扩展成员 */
        val isExtendMember: Boolean = false,

        /** 文件私有标识 */
        val fileHash: String? = null,

        /** 访问修饰符 */
        val visibility: Visibility? = null,

        /** 原始 mangled 字符串 */
        val rawMangled: String,

        /** 是否解析成功 */
        val isValid: Boolean = true,

        /** 解析错误信息 */
        val error: String? = null
    ) {
        /**
         * 获取完全限定名
         */
        val fullyQualifiedName: String
            get() = buildString {
                if (packageName.isNotEmpty()) {
                    append(packageName)
                    append(".")
                }

                // 添加嵌套声明路径
                nestedDecls.forEach {
                    append(it.identifier)
                    append(".")
                }

                append(identifier)
            }

        /**
         * 获取人类可读的签名
         */
        val signature: String
            get() = buildString {
                // 访问修饰符
                visibility?.let { append("${it.name.lowercase()} ") }

                // 声明类型
                when (declKind) {
                    DeclKind.FUNC -> append("func ")
                    DeclKind.CLASS -> append("class ")
                    DeclKind.INTERFACE -> append("interface ")
                    DeclKind.STRUCT -> append("struct ")
                    DeclKind.ENUM -> append("enum ")
                    DeclKind.EXTEND -> append("extend ")
                    DeclKind.PROP -> append("prop ")
                    DeclKind.VAR -> append("var ")
                    else -> {}
                }

                // 名称
                append(fullyQualifiedName)

                // 泛型参数
                if (typeParameters.isNotEmpty()) {
                    append("<${typeParameters.joinToString(", ")}>")
                }

                // 函数参数
                if (parameterTypes.isNotEmpty()) {
                    append("(${parameterTypes.joinToString(", ")})")
                }

                // 返回类型
                returnType?.let { append(": $it") }

                // 局部变量标记
                if (isLocalVar) {
                    append(" [local#$localVarIndex]")
                }
            }
    }

    /**
     * 嵌套声明信息
     */
    data class NestedDecl(
        val identifier: String,
        val declKind: DeclKind? = null,
        val typeParameters: List<String> = emptyList()
    )


    /**
     * 声明类型枚举
     */
    enum class DeclKind(val code: Char, val displayName: String) {
        FUNC('F', "func"),
        MACRO('M', "macro"),
        CLASS('C', "class"),
        INTERFACE('I', "interface"),
        EXTEND('X', "extend"),
        ENUM('U', "enum"),
        STRUCT('S', "struct"),
        TYPE_ALIAS('T', "type"),
        TYPEPARAM('Y', "typeparam"),
        VAR('V', "var"),
        PROP('P', "prop");

        companion object {
            private val codeMap = values().associateBy { it.code }
            fun fromCode(code: Char): DeclKind? = codeMap[code]
        }
    }

    /**
     * 访问修饰符
     */
    enum class Visibility(val prefix: String) {
        PUBLIC(MangleConstants.MANGLE_PUBLIC_PREFIX),
        PROTECTED(MangleConstants.MANGLE_PROTECTED_PREFIX),
        PRIVATE(MangleConstants.MANGLE_PRIVATE_PREFIX);

        companion object {
            fun fromPrefix(prefix: String): Visibility? = values().find { it.prefix == prefix }
        }
    }

    /**
     * 解析上下文
     */
    private class DemangleContext(val input: String) {
        var pos = 0
        val errors = mutableListOf<String>()

        fun hasMore(): Boolean = pos < input.length

        fun peek(): Char? = if (hasMore()) input[pos] else null

        fun peek(count: Int): String? =
            if (pos + count <= input.length) input.substring(pos, pos + count) else null

        fun consume(): Char? = if (hasMore()) input[pos++] else null

        fun consume(count: Int): String? {
            if (pos + count > input.length) return null
            val result = input.substring(pos, pos + count)
            pos += count
            return result
        }

        fun expect(expected: String): Boolean {
            if (peek(expected.length) == expected) {
                pos += expected.length
                return true
            }
            return false
        }

        fun addError(message: String) {
            errors.add("At position $pos: $message")
        }
    }

    /**
     * 解析 mangled 名称
     *
     * @param mangledName 完整的 mangled 字符串
     * @return 解析后的结构化信息
     */
    fun demangle(mangledName: String): DemangledName {
        if (mangledName.isEmpty()) {
            return createError(mangledName, "Empty mangled name")
        }

        // 检查是否为仓颉 mangled 名称
        if (!mangledName.startsWith(MangleConstants.MANGLE_CANGJIE_PREFIX)) {
            // 可能是简单标识符
            return DemangledName(
                packageName = "",
                identifier = mangledName,
                rawMangled = mangledName,
                isValid = true
            )
        }

        val ctx = DemangleContext(mangledName)

        try {
            // 跳过 "_C" 前缀
            ctx.expect(MangleConstants.MANGLE_CANGJIE_PREFIX)

            // 解析包名
            val packageName = parsePackageName(ctx)

            // 检查是否有嵌套标记
            val nestedDecls = parseNestedDecls(ctx)

            // 检查访问修饰符
            val visibility = parseVisibility(ctx)

            // 检查是否为扩展成员
            val isExtendMember = parseExtendMemberMarker(ctx)

            // 解析主标识符
            val identifier = parseLengthPrefixedName(ctx) ?: ""

            // 解析泛型参数
            val typeParameters = parseGenericParameters(ctx)

            // 解析声明类型
            val declKind = parseDeclKind(ctx)

            // 解析其他信息（参数、返回类型等）
            val (paramTypes, returnType) = parseCallableInfo(ctx, declKind)

            // 解析局部变量信息
            val (isLocalVar, localVarIndex) = parseLocalVarInfo(ctx)

            // 解析文件哈希
            val fileHash = parseFileHash(ctx)

            // 期望结束标记
            ctx.expect(MangleConstants.MANGLE_SUFFIX)

            return DemangledName(
                packageName = packageName,
                identifier = identifier,
                declKind = declKind,
                nestedDecls = nestedDecls,
                typeParameters = typeParameters,
                parameterTypes = paramTypes,
                returnType = returnType,
                isLocalVar = isLocalVar,
                localVarIndex = localVarIndex,
                isExtendMember = isExtendMember,
                fileHash = fileHash,
                visibility = visibility,
                rawMangled = mangledName,
                isValid = ctx.errors.isEmpty(),
                error = ctx.errors.joinToString("; ").takeIf { it.isNotEmpty() }
            )

        } catch (e: Exception) {
            return createError(mangledName, "Parse exception: ${e.message}")
        }
    }

    /**
     * 解析包名
     */
    private fun parsePackageName(ctx: DemangleContext): String {
        return parseLengthPrefixedName(ctx) ?: ""
    }

    /**
     * 解析嵌套声明链
     */
    private fun parseNestedDecls(ctx: DemangleContext): List<NestedDecl> {
        val result = mutableListOf<NestedDecl>()

        while (ctx.peek() != null) {
            // 检查是否为扩展声明
            if (ctx.peek()?.isDigit() == true || ctx.peek() == '<') {
                // 可能是嵌套声明的标识符
                val savedPos = ctx.pos
                val identifier = parseLengthPrefixedName(ctx)

                if (identifier != null) {
                    val typeParams = parseGenericParameters(ctx)
                    val kind = parseDeclKind(ctx)

                    // 如果有声明类型，这是嵌套声明
                    if (kind != null) {
                        result.add(NestedDecl(identifier, kind, typeParams))
                    } else {
                        // 否则回退，这是主标识符
                        ctx.pos = savedPos
                        break
                    }
                } else {
                    ctx.pos = savedPos
                    break
                }
            } else {
                break
            }
        }

        return result
    }

    /**
     * 解析访问修饰符
     */
    private fun parseVisibility(ctx: DemangleContext): Visibility? {
        // 这里需要根据实际的 mangle 格式调整
        // 编译器可能在 MangleAccessibility 中使用特定前缀
        return null
    }

    /**
     * 解析扩展成员标记
     */
    private fun parseExtendMemberMarker(ctx: DemangleContext): Boolean {
        val marker = "${MangleConstants.MANGLE_EXIG_PREFIX.length}${MangleConstants.MANGLE_EXIG_PREFIX}"
        if (ctx.peek(marker.length) == marker) {
            ctx.consume(marker.length)
            return true
        }
        return false
    }

    /**
     * 解析长度前缀的名称
     *
     * 格式: <数字><名称>
     * 例如: "3foo" -> "foo"
     */
    private fun parseLengthPrefixedName(ctx: DemangleContext): String? {
        val startPos = ctx.pos

        // 读取长度（数字）
        val lengthBuilder = StringBuilder()
        while (ctx.peek()?.isDigit() == true) {
            lengthBuilder.append(ctx.consume())
        }

        if (lengthBuilder.isEmpty()) {
            return null
        }

        val length = lengthBuilder.toString().toIntOrNull()
        if (length == null || length <= 0) {
            ctx.pos = startPos
            return null
        }

        // 读取名称
        val name = ctx.consume(length)
        if (name == null) {
            ctx.addError("Expected $length characters for name, but not enough remaining")
            ctx.pos = startPos
            return null
        }

        return name
    }

    /**
     * 解析泛型参数
     *
     * 格式: <<param1><param2>...>
     * 或者对于非 extend: <<count>>
     */
    private fun parseGenericParameters(ctx: DemangleContext): List<String> {
        if (!ctx.expect(MangleConstants.MANGLE_LT_PREFIX)) {
            return emptyList()
        }

        val params = mutableListOf<String>()

        // 检查是否为简化格式（只有数量）
        val savedPos = ctx.pos
        val countStr = StringBuilder()
        while (ctx.peek()?.isDigit() == true) {
            countStr.append(ctx.consume())
        }

        if (countStr.isNotEmpty() && ctx.peek() == '>') {
            // 简化格式: <N>
            ctx.expect(MangleConstants.MANGLE_GT_PREFIX)
            val count = countStr.toString().toInt()
            repeat(count) {
                params.add("T$it")  // 生成占位符名称
            }
            return params
        }

        // 完整格式: <name1><name2>...>
        ctx.pos = savedPos
        while (!ctx.expect(MangleConstants.MANGLE_GT_PREFIX)) {
            val param = parseLengthPrefixedName(ctx)
            if (param != null) {
                params.add(param)
            } else {
                ctx.addError("Expected generic parameter name")
                break
            }
        }

        return params
    }

    /**
     * 解析声明类型
     */
    private fun parseDeclKind(ctx: DemangleContext): DeclKind? {
        val char = ctx.peek() ?: return null
        val kind = DeclKind.fromCode(char)
        if (kind != null) {
            ctx.consume()
        }
        return kind
    }

    /**
     * 解析可调用成员信息（函数参数和返回类型）
     */
    private fun parseCallableInfo(
        ctx: DemangleContext,
        declKind: DeclKind?
    ): Pair<List<TypeInfo>, TypeInfo?> {
        if (declKind != DeclKind.FUNC && declKind != DeclKind.MACRO) {
            return emptyList<TypeInfo>() to null
        }

        val paramTypes = mutableListOf<TypeInfo>()
        var returnType: TypeInfo? = null

        // 查找 $$ 分隔符（标记参数和返回类型部分）
        if (ctx.expect(MangleConstants.MANGLE_DOLLAR_PREFIX + MangleConstants.MANGLE_DOLLAR_PREFIX)) {
            // 解析参数类型（直到遇到下一个 $ 或其他终止符）
            while (ctx.hasMore() && ctx.peek(1)?.startsWith("$") != true && ctx.peek() != 'E') {
                val type = parseType(ctx)
                if (type != null) {
                    paramTypes.add(type)
                } else {
                    break
                }
            }

            // 解析返回类型
            if (ctx.hasMore() && ctx.peek() != 'E') {
                returnType = parseType(ctx)
            }
        }

        return paramTypes to returnType
    }

    /**
     * 解析类型
     *
     * 对应编译器: MangleType
     */
    private fun parseType(ctx: DemangleContext): TypeInfo? {
        val char = ctx.peek() ?: return null

        return when {
            // 基本类型（单字符）
            MangleConstants.isPrimitivePrefix(char) -> {
                ctx.consume()
                TypeInfo.Primitive(getPrimitiveTypeName(char))
            }

            // this 类型
            char == 't' -> {
                ctx.consume()
                TypeInfo.This
            }

            // 函数类型
            char == 'F' -> {
                ctx.consume()
                parseFunctionType(ctx, isCFunc = false)
            }

            // C 函数类型
            ctx.peek(2) == "FC" -> {
                ctx.consume(2)
                parseFunctionType(ctx, isCFunc = true)
            }

            // 元组类型
            char == 'T' -> {
                ctx.consume()
                parseTupleType(ctx)
            }

            // Option 类型
            char == 'O' -> {
                ctx.consume()
                parseOptionType(ctx)
            }

            // 数组类型
            char == 'V' -> {
                ctx.consume()
                parseVArrayType(ctx)
            }

            // 限定类型
            char == 'Q' -> {
                ctx.consume()
                parseQualifiedType(ctx)
            }

            // 引用类型（长度前缀）
            char.isDigit() -> {
                parseReferenceType(ctx)
            }

            else -> {
                ctx.addError("Unknown type prefix: $char")
                null
            }
        }
    }

    /**
     * 解析引用类型
     */
    private fun parseReferenceType(ctx: DemangleContext): TypeInfo.Reference? {
        val name = parseLengthPrefixedName(ctx) ?: return null

        // 检查是否有泛型参数
        val typeArgs = if (ctx.expect(MangleConstants.MANGLE_LT_PREFIX)) {
            val args = mutableListOf<TypeInfo>()
            while (!ctx.expect(MangleConstants.MANGLE_GT_PREFIX)) {
                val arg = parseType(ctx)
                if (arg != null) {
                    args.add(arg)
                } else {
                    break
                }
            }
            args
        } else {
            emptyList()
        }

        return TypeInfo.Reference(name, typeArgs)
    }

    /**
     * 解析函数类型
     */
    private fun parseFunctionType(ctx: DemangleContext, isCFunc: Boolean): TypeInfo.Function? {
        // 返回类型
        val returnType = parseType(ctx) ?: return null

        // 参数类型
        val paramTypes = mutableListOf<TypeInfo>()
        while (ctx.hasMore() && ctx.peek() != 'E' && ctx.peek() != '$') {
            val savedPos = ctx.pos
            val param = parseType(ctx)
            if (param != null) {
                paramTypes.add(param)
            } else {
                ctx.pos = savedPos
                break
            }
        }

        return TypeInfo.Function(paramTypes, returnType, isCFunc)
    }

    /**
     * 解析元组类型
     */
    private fun parseTupleType(ctx: DemangleContext): TypeInfo.Tuple? {
        // 读取字段数量
        val countBuilder = StringBuilder()
        while (ctx.peek()?.isDigit() == true) {
            countBuilder.append(ctx.consume())
        }

        val count = countBuilder.toString().toIntOrNull() ?: return null

        // 跳过通配符
        ctx.expect(MangleConstants.MANGLE_WILDCARD_PREFIX)

        // 读取字段类型
        val fieldTypes = mutableListOf<TypeInfo>()
        repeat(count) {
            val type = parseType(ctx)
            if (type != null) {
                fieldTypes.add(type)
            }
        }

        return TypeInfo.Tuple(fieldTypes)
    }

    /**
     * 解析 Option 类型
     */
    private fun parseOptionType(ctx: DemangleContext): TypeInfo.Option? {
        // 读取问号数量
        val questNumBuilder = StringBuilder()
        while (ctx.peek()?.isDigit() == true) {
            questNumBuilder.append(ctx.consume())
        }

        val questNum = questNumBuilder.toString().toIntOrNull() ?: 1

        // 跳过通配符
        ctx.expect(MangleConstants.MANGLE_WILDCARD_PREFIX)

        // 读取组件类型
        val componentType = parseType(ctx) ?: return null

        return TypeInfo.Option(componentType, questNum)
    }

    /**
     * 解析数组类型
     */
    private fun parseVArrayType(ctx: DemangleContext): TypeInfo.VArray? {
        // 跳过通配符
        ctx.expect(MangleConstants.MANGLE_WILDCARD_PREFIX)

        // 元素类型
        val elementType = parseType(ctx) ?: return null

        // 大小（可能是常量类型）
        val size = parseType(ctx)

        return TypeInfo.VArray(elementType, size)
    }

    /**
     * 解析限定类型
     */
    private fun parseQualifiedType(ctx: DemangleContext): TypeInfo.Qualified? {
        // 基类型
        val baseType = parseType(ctx) ?: return null

        // 跳过点号
        ctx.expect(MangleConstants.MANGLE_DOT_PREFIX)

        // 字段名
        val field = parseLengthPrefixedName(ctx) ?: return null

        // 类型参数（可选）
        val typeArgs = if (ctx.expect(MangleConstants.MANGLE_LT_PREFIX)) {
            val args = mutableListOf<TypeInfo>()
            while (!ctx.expect(MangleConstants.MANGLE_GT_PREFIX)) {
                val arg = parseType(ctx)
                if (arg != null) {
                    args.add(arg)
                } else {
                    break
                }
            }
            args
        } else {
            emptyList()
        }

        return TypeInfo.Qualified(baseType, field, typeArgs)
    }

    /**
     * 解析局部变量信息
     */
    private fun parseLocalVarInfo(ctx: DemangleContext): Pair<Boolean, Int?> {
        if (ctx.expect("K")) {
            // 读取索引
            val indexBuilder = StringBuilder()
            while (ctx.peek()?.isDigit() == true) {
                indexBuilder.append(ctx.consume())
            }

            // 跳过结束的下划线
            ctx.expect(MangleConstants.MANGLE_WILDCARD_PREFIX)

            val index = indexBuilder.toString().toIntOrNull()
            return true to index
        }
        return false to null
    }

    /**
     * 解析文件哈希
     */
    private fun parseFileHash(ctx: DemangleContext): String? {
        if (ctx.expect(MangleConstants.MANGLE_FILE_ID_PREFIX)) {
            return ctx.consume(MangleConstants.FILE_HASH_LEN)
        }
        return null
    }

    /**
     * 获取基本类型名称
     */
    private fun getPrimitiveTypeName(code: Char): String {
        return when (code) {
            'n' -> "Nothing"
            'u' -> "Unit"
            'c' -> "Rune"
            'b' -> "Bool"
            'f' -> "Float32"
            'd' -> "Float64"
            'a' -> "Int8"
            's' -> "Int16"
            'i' -> "Int32"
            'l' -> "Int64"
            'q' -> "IntNative"
            'h' -> "UInt8"
            't' -> "UInt16"
            'j' -> "UInt32"
            'm' -> "UInt64"
            'r' -> "UIntNative"
            'v' -> "void"
            else -> "Unknown($code)"
        }
    }

    /**
     * 创建错误结果
     */
    private fun createError(mangledName: String, message: String): DemangledName {
        return DemangledName(
            packageName = "",
            identifier = mangledName,
            rawMangled = mangledName,
            isValid = false,
            error = message
        )
    }

    /**
     * 简化接口：只提取标识符
     */
    fun extractIdentifier(mangledName: String): String? {
        return demangle(mangledName).takeIf { it.isValid }?.identifier
    }

    /**
     * 简化接口：只提取完全限定名
     */
    fun extractFullyQualifiedName(mangledName: String): String? {
        return demangle(mangledName).takeIf { it.isValid }?.fullyQualifiedName
    }
}

/**
 * 扩展声明 ID 解析器
 *
 * 专门用于解析扩展声明的 ID
 * 格式: <MangledPackage><MangledExtendedType><:><Interfaces>[@G<Constraints>]X
 */
object ExtendIdDemangler {

    /**
     * 解析后的扩展 ID 结构
     */
    data class DemangledExtendId(
        /** 包名 */
        val packageName: String,

        /** 被扩展的类型 */
        val extendedType: TypeInfo,

        /** 继承的接口列表 */
        val inheritedTypes: List<TypeInfo>,

        /** 泛型约束 */
        val genericConstraints: List<GenericConstraint>,

        /** 原始扩展 ID */
        val rawExtendId: String,

        /** 是否解析成功 */
        val isValid: Boolean = true,

        /** 错误信息 */
        val error: String? = null
    ) {
        /**
         * 获取被扩展类型的名称（简单形式）
         */
        val extendedTypeName: String
            get() = when (extendedType) {
                is TypeInfo.Reference -> extendedType.name
                is TypeInfo.Primitive -> extendedType.name
                else -> extendedType.toString()
            }

        /**
         * 获取被扩展类型的完全限定名
         */
        val extendedTypeFullName: String
            get() = when (extendedType) {
                is TypeInfo.Reference -> {
                    if (packageName.isNotEmpty()) {
                        "$packageName.${extendedType.name}"
                    } else {
                        extendedType.name
                    }
                }
                else -> extendedType.toString()
            }

        /**
         * 获取人类可读的签名
         */
        val signature: String
            get() = buildString {
                append("extend ")

                // 被扩展类型
                append(extendedType)

                // 继承的接口
                if (inheritedTypes.isNotEmpty()) {
                    append(" <: ")
                    append(inheritedTypes.joinToString(" & "))
                }

                // 泛型约束
                if (genericConstraints.isNotEmpty()) {
                    append(" where ")
                    append(genericConstraints.joinToString(", ") {
                        "${it.typeParam} <: ${it.upperBounds.joinToString(" & ")}"
                    })
                }
            }
    }

    /**
     * 泛型约束
     */
    data class GenericConstraint(
        val typeParam: TypeInfo,
        val upperBounds: List<TypeInfo>
    )



    /**
     * 解析上下文
     */
    private class ExtendDemangleContext(val input: String) {
        var pos = 0
        val errors = mutableListOf<String>()

        fun hasMore(): Boolean = pos < input.length
        fun peek(): Char? = if (hasMore()) input[pos] else null
        fun peek(count: Int): String? =
            if (pos + count <= input.length) input.substring(pos, pos + count) else null
        fun consume(): Char? = if (hasMore()) input[pos++] else null
        fun consume(count: Int): String? {
            if (pos + count > input.length) return null
            val result = input.substring(pos, pos + count)
            pos += count
            return result
        }
        fun expect(expected: String): Boolean {
            if (peek(expected.length) == expected) {
                pos += expected.length
                return true
            }
            return false
        }
        fun addError(message: String) {
            errors.add("At position $pos: $message")
        }
    }

    /**
     * 解析扩展 ID
     *
     * @param extendId 扩展 ID 字符串
     * @return 解析后的结构化信息
     */
    fun demangle(extendId: String): DemangledExtendId {
        if (extendId.isEmpty()) {
            return createError(extendId, "Empty extend ID")
        }

        val ctx = ExtendDemangleContext(extendId)

        try {
            // 1. 解析包名（长度前缀格式）
            val packageName = parseLengthPrefixedName(ctx) ?: ""

            // 2. 解析被扩展的类型
            val extendedType = parseType(ctx)
            if (extendedType == null) {
                return createError(extendId, "Failed to parse extended type")
            }

            // 3. 期望分隔符 "<:"
            if (!ctx.expect(MangleConstants.MANGLE_LT_COLON_PREFIX)) {
                return createError(extendId, "Expected '<:' separator")
            }

            // 4. 解析继承的接口（用 & 分隔）
            val inheritedTypes = parseInheritedTypes(ctx)

            // 5. 解析泛型约束（如果有）
            val genericConstraints = parseGenericConstraints(ctx)

            // 6. 期望结束标记 'X'
            if (!ctx.expect(MangleConstants.DECL_KIND_EXTEND.toString())) {
                ctx.addError("Expected 'X' at end of extend ID")
            }

            return DemangledExtendId(
                packageName = packageName,
                extendedType = extendedType,
                inheritedTypes = inheritedTypes,
                genericConstraints = genericConstraints,
                rawExtendId = extendId,
                isValid = ctx.errors.isEmpty(),
                error = ctx.errors.joinToString("; ").takeIf { it.isNotEmpty() }
            )

        } catch (e: Exception) {
            return createError(extendId, "Parse exception: ${e.message}")
        }
    }

    /**
     * 解析继承的接口类型（用 & 分隔）
     */
    private fun parseInheritedTypes(ctx: ExtendDemangleContext): List<TypeInfo> {
        val result = mutableListOf<TypeInfo>()

        // 如果紧接着是 @G 或 X，说明没有继承的接口
        if (ctx.peek(2) == "@G" || ctx.peek() == 'X') {
            return result
        }

        while (ctx.hasMore()) {
            // 检查是否到达泛型约束或结束标记
            if (ctx.peek(2) == "@G" || ctx.peek() == 'X') {
                break
            }

            // 解析一个类型
            val type = parseType(ctx)
            if (type != null) {
                result.add(type)
            } else {
                break
            }

            // 检查是否有 & 分隔符
            if (ctx.peek() == '&') {
                ctx.consume()
            } else {
                break
            }
        }

        return result
    }

    /**
     * 解析泛型约束
     *
     * 格式: @G<TypeParam>:<Bound1>:<Bound2>...
     */
    private fun parseGenericConstraints(ctx: ExtendDemangleContext): List<GenericConstraint> {
        val result = mutableListOf<GenericConstraint>()

        while (ctx.expect("@G")) {
            // 解析类型参数
            val typeParam = parseType(ctx)
            if (typeParam == null) {
                ctx.addError("Expected type parameter in generic constraint")
                break
            }

            // 解析上界（用 : 分隔）
            val upperBounds = mutableListOf<TypeInfo>()
            while (ctx.peek() == ':') {
                ctx.consume()
                val bound = parseType(ctx)
                if (bound != null) {
                    upperBounds.add(bound)
                } else {
                    break
                }
            }

            if (upperBounds.isNotEmpty()) {
                result.add(GenericConstraint(typeParam, upperBounds))
            }
        }

        return result
    }

    /**
     * 解析长度前缀的名称
     */
    private fun parseLengthPrefixedName(ctx: ExtendDemangleContext): String? {
        val startPos = ctx.pos

        // 读取长度（数字）
        val lengthBuilder = StringBuilder()
        while (ctx.peek()?.isDigit() == true) {
            lengthBuilder.append(ctx.consume())
        }

        if (lengthBuilder.isEmpty()) {
            return null
        }

        val length = lengthBuilder.toString().toIntOrNull()
        if (length == null || length <= 0) {
            ctx.pos = startPos
            return null
        }

        // 读取名称
        val name = ctx.consume(length)
        if (name == null) {
            ctx.addError("Expected $length characters for name")
            ctx.pos = startPos
            return null
        }

        return name
    }

    /**
     * 解析类型（复用 NameDemangler 的逻辑）
     */
    private fun parseType(ctx: ExtendDemangleContext): TypeInfo? {
        val char = ctx.peek() ?: return null

        return when {
            // 基本类型
            MangleConstants.isPrimitivePrefix(char) -> {
                ctx.consume()
                TypeInfo.Primitive(getPrimitiveTypeName(char))
            }

            // this 类型
            char == 't' -> {
                ctx.consume()
                TypeInfo.This
            }

            // 引用类型（长度前缀）
            char.isDigit() -> {
                parseReferenceType(ctx)
            }

            else -> {
                ctx.addError("Unknown type prefix: $char")
                null
            }
        }
    }

    /**
     * 解析引用类型
     */
    private fun parseReferenceType(ctx: ExtendDemangleContext): TypeInfo.Reference? {
        val name = parseLengthPrefixedName(ctx) ?: return null

        // 检查是否有泛型参数 <...>
        val typeArgs = if (ctx.expect(MangleConstants.MANGLE_LT_PREFIX)) {
            val args = mutableListOf<TypeInfo>()
            while (!ctx.expect(MangleConstants.MANGLE_GT_PREFIX)) {
                val arg = parseType(ctx)
                if (arg != null) {
                    args.add(arg)
                } else {
                    break
                }
            }
            args
        } else {
            emptyList()
        }

        return TypeInfo.Reference(name, typeArgs)
    }

    /**
     * 获取基本类型名称
     */
    private fun getPrimitiveTypeName(code: Char): String {
        return when (code) {
            'n' -> "Nothing"
            'u' -> "Unit"
            'c' -> "Rune"
            'b' -> "Bool"
            'f' -> "Float32"
            'd' -> "Float64"
            'a' -> "Int8"
            's' -> "Int16"
            'i' -> "Int32"
            'l' -> "Int64"
            'q' -> "IntNative"
            'h' -> "UInt8"
            't' -> "UInt16"
            'j' -> "UInt32"
            'm' -> "UInt64"
            'r' -> "UIntNative"
            else -> "Unknown($code)"
        }
    }

    /**
     * 创建错误结果
     */
    private fun createError(extendId: String, message: String): DemangledExtendId {
        return DemangledExtendId(
            packageName = "",
            extendedType = TypeInfo.Unknown(extendId),
            inheritedTypes = emptyList(),
            genericConstraints = emptyList(),
            rawExtendId = extendId,
            isValid = false,
            error = message
        )
    }

    /**
     * 简化接口：只提取被扩展类型名称
     */
    fun extractExtendedTypeName(extendId: String): String? {
        return demangle(extendId).takeIf { it.isValid }?.extendedTypeName
    }

    /**
     * 简化接口：只提取被扩展类型的完全限定名
     */
    fun extractExtendedTypeFullName(extendId: String): String? {
        return demangle(extendId).takeIf { it.isValid }?.extendedTypeFullName
    }
}

/**
 * 扩展函数：解析扩展 ID
 */
fun String.demangleExtendId(): ExtendIdDemangler.DemangledExtendId {
    return ExtendIdDemangler.demangle(this)
}
/**
 * 扩展函数：解析 mangled 名称
 */
fun String.demangle(): NameDemangler.DemangledName {
    return NameDemangler.demangle(this)
}

/**
 * Name Mangling 常量定义
 *
 * 对应编译器实现: cangjie/Mangle/MangleUtils.h
 *
 * 这些常量必须与编译器保持严格一致，否则会导致链接失败
 */
object MangleConstants {
    // ==================== 基础前缀 ====================
    const val MANGLE_CANGJIE_PREFIX = "_C"
    const val MANGLE_NESTED_PREFIX = "N"
    const val MANGLE_SUFFIX = "E"

    // ==================== 初始化相关 ====================
    const val MANGLE_GLOBAL_VARIABLE_INIT_PREFIX = "GV"
    const val MANGLE_GLOBAL_PACKAGE_INIT_PREFIX = "GP"
    const val MANGLE_GLOBAL_FILE_INIT_PREFIX = "GF"
    const val MANGLE_FUNC_PARA_INIT_PREFIX = "PI"


    const val MANGLE_PUBLIC_PREFIX = "pb"
    const val MANGLE_PROTECTED_PREFIX = "pt"
    const val MANGLE_PRIVATE_PREFIX = "pv"


    const val SPECIAL_NAME_FOR_INIT_FUNCTION = "ii"
    const val SPECIAL_NAME_FOR_INIT_RESET_FUNCTION = "ir"
    const val SPECIAL_NAME_FOR_INIT_LITERAL_FUNCTION = "il"
    const val SPECIAL_NAME_FOR_INIT_FLAG_RESET_FUNCTION = "if"
    const val SPECIAL_NAME_FOR_GET = "get"
    const val SPECIAL_NAME_FOR_SET = "set"

    // ==================== 函数和类型前缀 ====================
    const val MANGLE_CFUNC_PREFIX = "FC"
    const val MANGLE_GENERAL_FUNC_PREFIX = "F0"
    const val MANGLE_FILE_ID_PREFIX = "U"
    const val MANGLE_TYPE_STRUCT_PREFIX = "R"
    const val MANGLE_TYPE_CLASS_PREFIX = "C"
    const val MANGLE_TYPE_ENUM_PREFIX = "N"
    const val MANGLE_TYPE_ARRAY_PREFIX = "A"
    const val MANGLE_FUNC_PARAM_TYPE_PREFIX = "H"
    const val MANGLE_VOID_TY_SUFFIX = "v"
    const val MANGLE_LOCAL_VAR_PREFIX = "K"
    const val MANGLE_LAMBDA_PREFIX = "L"
    const val MANGLE_EXTEND_PREFIX = "X"
    const val USER_MAIN_MANGLED_NAME = "user.main"
    const val MANGLE_GENERIC_PREFIX = "I"
    const val MANGLE_ANONYMOUS_VARIABLE_PREFIX = "0"
    const val MANGLE_COMPRESSED_PREFIX = "Y"
    const val MANGLE_POINTER_PREFIX = "P"

    // ==================== 类型修饰前缀 ====================
    const val MANGLE_VARRAY_PREFIX = "V"          // 数组类型
    const val MANGLE_TUPLE_PREFIX = "T"           // 元组类型
    const val MANGLE_GENERIC_TYPE_PREFIX = "G"    // 泛型类型
    const val MANGLE_AT_PREFIX = "@"              // @ 符号
    const val MANGLE_DOLLAR_PREFIX = "$"          // $ 符号（常量）
    const val MANGLE_WILDCARD_PREFIX = "_"        // 通配符
    const val MANGLE_DOT_PREFIX = "."             // 点号
    const val MANGLE_LT_PREFIX = "<"              // 泛型左括号
    const val MANGLE_GT_PREFIX = ">"              // 泛型右括号 >
    const val MANGLE_LT_COLON_PREFIX = "<:"       // extend 分隔符

    // ==================== 泛型约束前缀（重要修正）====================
    /**
     * 泛型约束前缀
     *
     * 对应编译器: MANGLE_AT_PREFIX + MANGLE_GENERIC_TYPE_PREFIX = "@G"
     *
     * 注意：不是 "G$"，而是 "@G"
     */
    const val MANGLE_GEXTEND_PREFIX = "@G"

    // ==================== AST 类型标记的字符串形式 ====================
    /**
     * 扩展中使用的特殊前缀
     *
     * 对应编译器中的 MANGLE_EXIG_PREFIX
     * 用于标记扩展成员
     */
    const val MANGLE_EXIG_PREFIX = "exig"

    // ==================== 长度常量 ====================
    const val MANGLE_PREFIX_LEN = 2
    const val MANGLE_CHAR_LEN = 1
    const val MANGLE_SPECIAL_NAME_LEN = 2
    const val MANGLE_PROP_LEN = 3
    const val FILE_HASH_LEN = 13

    // ==================== 声明类型标记 (对应 ASTKind) ====================
    /**
     * 对应 ASTMangler.cpp::MangleDeclAstKind
     *
     * 索引映射:
     * - 4: FUNC_DECL -> 'F'
     * - 5: MACRO_DECL -> 'M'
     * - 7: CLASS_DECL -> 'C'
     * - 8: INTERFACE_DECL -> 'I'
     * - 9: EXTEND_DECL -> 'X'
     * - 10: ENUM_DECL -> 'U'
     * - 11: STRUCT_DECL -> 'S'
     * - 12: TYPE_ALIAS_DECL -> 'T'
     * - 13: TYPEPARAM_DECL -> 'Y'
     * - 15: VAR_DECL -> 'V'
     * - 16: PROP_DECL -> 'P'
     */
    const val DECL_KIND_FUNC = 'F'
    const val DECL_KIND_MACRO = 'M'
    const val DECL_KIND_CLASS = 'C'
    const val DECL_KIND_INTERFACE = 'I'
    const val DECL_KIND_EXTEND = 'X'
    const val DECL_KIND_ENUM = 'U'
    const val DECL_KIND_STRUCT = 'S'
    const val DECL_KIND_TYPE_ALIAS = 'T'
    const val DECL_KIND_TYPEPARAM = 'Y'
    const val DECL_KIND_VAR = 'V'
    const val DECL_KIND_PROP = 'P'

    // ==================== 基本类型映射 ====================
    /**
     * 基本类型的 mangled 名称映射
     *
     * 对应编译器实现: ASTMangler.cpp::MangleBuiltinTypeImpl
     */
    private val PRIMITIVE_TYPE_MAP = mapOf(
        "Nothing" to "n",
        "Unit" to "u",
        "Rune" to "c",
        "Bool" to "b",
        "Float16" to "Dh",
        "Float32" to "f",
        "Float64" to "d",
        "Int8" to "a",
        "Int16" to "s",
        "Int32" to "i",
        "Int64" to "l",
        "IntNative" to "q",
        "UInt8" to "h",
        "UInt16" to "t",
        "UInt32" to "j",
        "UInt64" to "m",
        "UIntNative" to "r",
        "CString" to "CString",
        "CFunc" to "CFunc",
        "CPointer" to "CPointer"
    )

    /**
     * 基本类型前缀字符集合
     *
     * 对应编译器: PRIMITIVE_PREFIX_SET
     */
    val PRIMITIVE_PREFIX_SET = setOf(
        'n', 'u', 'c', 'b', 'f', 'd', 'a', 's', 'i', 'l', 'q',
        'h', 't', 'j', 'm', 'r', 'D', 'v'
    )

    /**
     * 获取基本类型的 mangled 名称
     *
     * @param typeName 类型名称（如 "Int32"）
     * @return mangled 名称（如 "i"），如果不是基本类型则返回 null
     */
    fun getPrimitiveTypeMangle(typeName: String): String? {
        return PRIMITIVE_TYPE_MAP[typeName]
    }

    /**
     * 检查字符是否为基本类型前缀
     */
    fun isPrimitivePrefix(c: Char): Boolean {
        return c in PRIMITIVE_PREFIX_SET
    }
}

/**
 * 扩展描述符的抽象基类
 *
 * 提供扩展描述符的通用实现，包括：
 * - 扩展 ID 的生成（严格遵循编译器的 name mangling 策略）
 * - 成员作用域的类型替换
 * - this 接收者参数的创建
 *
 * 扩展 ID 格式（对应编译器 ASTMangler.cpp::MangleExtendDecl）：
 * ```
 * <MangledPackage><MangledExtendedType><:><MangledInterface1>&<MangledInterface2>&...[@G<Constraints>]X
 * ```
 *
 * 示例：
 * ```
 * extend<T> List<T> <: Comparable<T> & Hashable where T <: Number
 * →  14std.collection4List<1T><:10Comparable<1T>&8Hashable@G1T:6NumberX
 * ```
 */
abstract class AbstractExtendDescriptor(
    protected val storageManager: StorageManager,
) : ExtendDescriptor {

    private val _thisAsReceiverParameter: NotNullLazyValue<ReceiverParameterDescriptor> =
        storageManager.createLazyValue {
            LazyExtendReceiverParameterDescriptor(this@AbstractExtendDescriptor)
        }

    override val thisAsReceiverParameter: ReceiverParameterDescriptor
        get() = _thisAsReceiverParameter.invoke()

    // ==================== Extend ID 生成 ====================

    /**
     * 扩展的唯一标识符（延迟计算）
     *
     * 遵循编译器的 name mangling 策略（ASTMangler.cpp::MangleExtendDecl），格式：
     * ```
     * <MangledPackage><MangledExtendedType><:><Interfaces>[@G<Constraints>]X
     * ```
     */
    private val _extendId: NotNullLazyValue<String> = storageManager.createLazyValue {
        buildExtendId()
    }

    override val extendId: String
        get() = _extendId.invoke()

    /**
     * 构建扩展的唯一标识符
     *
     * 对应编译器实现: ASTMangler.cpp::MangleExtendDecl
     *
     * 格式: `<MangledPackage><MangledExtendedType><:><Interface1>&<Interface2>&...[@G<Constraints>]X`
     *
     * 步骤：
     * 1. Mangle 包名（长度前缀）
     * 2. Mangle 被扩展类型
     * 3. 添加 "<:" 分隔符
     * 4. Mangle 并排序继承的接口（用 & 连接）
     * 5. Mangle 泛型约束（如果有）
     * 6. 添加 'X' 类型标记
     */
    protected open fun buildExtendId(): String {
        val builder = StringBuilder()

        // 1. Mangle 包名
        val packageFqName = (containingDeclaration as? PackageData)?.fqName?.asString() ?: ""
        builder.append(mangleName(packageFqName))

        // 2. Mangle 被扩展类型
        builder.append(mangleType(extendType))

        // 3. 添加分隔符（对应 MANGLE_LT_COLON_PREFIX）
        builder.append(MangleConstants.MANGLE_LT_COLON_PREFIX)

        // 4. 排序并 Mangle 继承的接口类型（用 & 连接）
        val mangledInterfaces = superTypes
            .map { mangleType(it) }
            .sorted()  // 稳定排序，确保一致性
        builder.append(mangledInterfaces.joinToString("&"))

        // 5. Mangle 泛型约束（如果有）
        val constraintsMangled = mangleGenericConstraints()
        if (constraintsMangled.isNotEmpty()) {
            builder.append(constraintsMangled)
        }

        // 6. 添加声明类型标记（ExtendDecl -> 'X'）
        builder.append(MangleConstants.DECL_KIND_EXTEND)

        return builder.toString()
    }

    /**
     * Mangle 名称（添加长度前缀）
     *
     * 对应: MangleUtils::MangleName
     *
     * 格式: `<length><name>`
     * 示例: `"std.collection"` → `"14std.collection"`
     */
    private fun mangleName(name: String): String {
        return "${name.length}$name"
    }

    /**
     * Mangle 类型
     *
     * 对应编译器实现: ASTMangler.cpp::MangleType + MangleRefTypeAnnotation
     *
     * 将类型转换为 mangled 字符串表示，格式：
     * - 基本类型: 使用预定义映射（如 `Int32` → `i`）
     * - 简单引用类型: `<length><name>`
     * - 泛型类型: `<length><name><<Arg1><Arg2>...>`
     * - 类型参数: `<length><name>`
     *
     * 示例：
     * - `Int32` → `i`
     * - `List` → `4List`
     * - `List<Int32>` → `4List<i>`
     * - `Array<T>` → `5Array<1T>`
     */
    protected fun mangleType(type: CangJieType): String {
        return when (type) {
            is SimpleType -> {
                val constructor = type.constructor
                val descriptor = constructor.declarationDescriptor

                when (descriptor) {
                    // 类型参数：使用 mangleName
                    is TypeParameterDescriptor -> {
                        mangleName(descriptor.name.asString())
                    }

                    // 类、接口、结构体等
                    is ClassDescriptor -> {
                        // 检查是否为基本类型
                        val simpleName = descriptor.name.asString()
                        val primitiveMangled = MangleConstants.getPrimitiveTypeMangle(simpleName)
                        if (primitiveMangled != null && descriptor.containingDeclaration is PackageFragmentDescriptor) {
                            val packageFqName = (descriptor.containingDeclaration as PackageFragmentDescriptor)
                                .fqName.asString()
                            // 基本类型通常在 std.builtin 包中
                            if (packageFqName == "std.builtin" || packageFqName.isEmpty()) {
                                return primitiveMangled
                            }
                        }

                        // 非基本类型：使用完全限定名
                        val fqName = descriptor.fqNameUnsafe.asString()
                        val baseName = mangleName(fqName)

                        if (type.arguments.isEmpty()) {
                            baseName
                        } else {
                            // 泛型类型：添加 <...> 包裹
                            val argsBuilder = StringBuilder()
                            for (arg in type.arguments) {
                                when (arg) {
                                    is TypeArgumentImpl -> {
                                        argsBuilder.append(mangleType(arg.type))
                                    }

                                    else -> {
                                        // 星投影或其他，使用通配符
                                        argsBuilder.append(MangleConstants.MANGLE_WILDCARD_PREFIX)
                                    }
                                }
                            }
                            "$baseName${MangleConstants.MANGLE_LT_PREFIX}$argsBuilder${MangleConstants.MANGLE_GT_PREFIX}"
                        }
                    }

                    // 类型别名：解析为实际类型
                    is TypeAliasDescriptor -> {
                        mangleType(descriptor.expandedType)
                    }

                    // 其他情况：使用完整字符串表示
                    else -> {
                        val str = type.toString()
                        mangleName(str)
                    }
                }
            }

            // 灵活类型、动态类型等：使用字符串表示
            else -> {
                val str = type.toString()
                mangleName(str)
            }
        }
    }

    /**
     * Mangle 泛型约束
     *
     * 对应编译器实现: ASTMangler.cpp::MangleGenericConstraints
     *
     * 格式: `@G<MangledTypeParam>:<MangledBound1>:<MangledBound2>...`
     *
     * 重要：前缀是 "@G" 而不是 "G$"
     *
     * 特点：
     * 1. 按类型参数的 mangled 名称排序
     * 2. 每个类型参数的 upper bounds 也要排序
     * 3. 使用冒号分隔 bounds
     *
     * 示例：
     * ```
     * where T <: Comparable<T> & Hashable, U <: Number
     * →  @G1T:10Comparable<1T>:8Hashable@G1U:6Number
     * ```
     */
    protected fun mangleGenericConstraints(): String {
        if (declaredTypeParameters.isEmpty()) {
            return ""
        }

        val builder = StringBuilder()

        // 数据类：存储类型参数及其约束
        data class ConstraintGroup(
            val param: TypeParameterDescriptor,
            val mangledParam: String,
            val upperBounds: List<String>  // 已 mangle 且排序的上界
        )

        // 收集所有约束并排序
        val groups = declaredTypeParameters.mapNotNull { param ->
            // 过滤掉没有约束的类型参数（只有默认的 Any 上界）
            val nonTrivialBounds = param.upperBounds.filter { bound ->
                // 检查是否为非平凡约束（不是 Any 或 Nothing）
                val descriptor = (bound as? SimpleType)?.constructor?.declarationDescriptor
                val isAny = descriptor is ClassDescriptor && descriptor.name.asString() == "Any"
                val isNothing = descriptor is ClassDescriptor && descriptor.name.asString() == "Nothing"
                !isAny && !isNothing
            }

            if (nonTrivialBounds.isEmpty()) {
                null
            } else {
                val mangledParam = mangleType(param.defaultType)
                val mangledBounds = nonTrivialBounds
                    .map { mangleType(it) }
                    .sorted()  // 稳定排序 upper bounds
                ConstraintGroup(param, mangledParam, mangledBounds)
            }
        }.sortedBy { it.mangledParam }  // 按类型参数的 mangled 名称排序

        // 生成约束字符串
        for (group in groups) {
            builder.append(MangleConstants.MANGLE_GEXTEND_PREFIX)  // "@G"
            builder.append(group.mangledParam)

            for (bound in group.upperBounds) {
                builder.append(":")
                builder.append(bound)
            }
        }

        return builder.toString()
    }

    // ==================== Visitor 支持 ====================

    override fun <R, D> accept(
        visitor: DeclarationDescriptorVisitor<R, D>,
        data: D
    ): R? {
        return visitor.visitExtendDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
        visitor.visitExtendDescriptor(this, null)
    }

    // ==================== 类型相关 ====================

    override val extendTypeConstructor: TypeConstructor
        get() = extendType.constructor

    override fun getMemberScope(
        typeArguments: List<TypeArgument>,
    ): MemberScope {
        assert(typeArguments.size == declaredTypeParameters.size) {
            "Illegal number of type arguments: expected ${declaredTypeParameters.size} but was ${typeArguments.size} for ${declaredTypeParameters}"
        }
        if (typeArguments.isEmpty()) return unsubstitutedMemberScope

        val substitutorMap = declaredTypeParameters.zip(typeArguments).associate { (param, arg) ->
            param.typeConstructor to arg.type.unwrap()
        }
        val substitutor = ComposableTypeSubstitutor.create(substitutorMap)

        return SubstitutingScope(unsubstitutedMemberScope, substitutor)
    }

    override fun getMemberScope(
        substitutor: ComposableTypeSubstitutor,
    ): MemberScope {
        if (substitutor.isEmpty) return unsubstitutedMemberScope

        return SubstitutingScope(unsubstitutedMemberScope, substitutor)
    }

    override val staticScope: MemberScope
        get() = MemberScope.Empty

    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.PUBLIC

    override val original: ExtendDescriptor
        get() = this
}

/**
 * 扩展描述符的具体实现
 *
 * 主要用于测试和简单场景
 */
class ExtendDescriptorImpl(
    override val extendType: CangJieType,
    override val superTypes: List<CangJieType>,
    override val declaredTypeParameters: List<TypeParameterDescriptor>,
    override val containingDeclaration: DeclarationDescriptor,
    storageManager: StorageManager,
) : AbstractExtendDescriptor(storageManager) {

    override val unsubstitutedMemberScope: MemberScope
        get() = MemberScope.Empty  // 简化实现，返回空作用域

    override val modality: Modality
        get() = Modality.FINAL  // 扩展没有继承概念，默认为 FINAL

    override val declaredCallableMembers: Collection<CallableMemberDescriptor>
        get() = emptyList()  // 简化实现，返回空列表
}

/**
 * 扩展 ID 工具类
 *
 * 提供与扩展 ID 相关的辅助功能
 */
object ExtendIdUtils {
    /**
     * 截断扩展 ID，提取被扩展类型部分
     *
     * 对应编译器实现: ASTMangler.cpp::TruncateExtendMangledName
     *
     * 示例：
     * ```
     * "14std.collection4List<:10Comparable&8HashableX"
     * → "14std.collection4List"
     * ```
     */
    fun truncateExtendId(extendId: String): String? {
        val separatorIndex = extendId.indexOf(MangleConstants.MANGLE_LT_COLON_PREFIX)
        if (separatorIndex == -1) {
            return null
        }
        return extendId.substring(0, separatorIndex)
    }

    /**
     * 检查名称是否为扩展成员
     *
     * 扩展成员的 mangled 名称中会包含 "exig" 前缀
     * 对应 mangleName("exig") = "4exig"
     */
    fun isExtendMember(mangledName: String): Boolean {
        val mangledExig = "${MangleConstants.MANGLE_EXIG_PREFIX.length}${MangleConstants.MANGLE_EXIG_PREFIX}"
        return mangledName.contains(mangledExig)
    }

    /**
     * 从 mangled 名称中提取包名
     *
     * 示例：
     * ```
     * "14std.collection4List" → "std.collection"
     * ```
     */
    fun extractPackageName(mangledName: String): String? {
        // 查找第一个数字序列
        val lengthMatch = Regex("^(\\d+)").find(mangledName) ?: return null
        val length = lengthMatch.value.toIntOrNull() ?: return null
        val start = lengthMatch.value.length

        if (start + length > mangledName.length) {
            return null
        }

        return mangledName.substring(start, start + length)
    }
}