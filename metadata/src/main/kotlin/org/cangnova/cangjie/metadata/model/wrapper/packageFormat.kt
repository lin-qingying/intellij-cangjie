/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.metadata.model.wrapper

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.deserialization.DeclTable
import org.cangnova.cangjie.metadata.deserialization.TypeTable
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.metadata.model.fb.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

//对FbPackage进行二次封装


interface DeclarationWrapper {

    val annotations: List<AnnotationWrapper> get() = emptyList()
}
// 预定义需要的声明类型集合，避免重复创建
private val targetDeclKinds = setOf(
    FbDeclKind.ClassDecl, FbDeclKind.InterfaceDecl, FbDeclKind.StructDecl,
    FbDeclKind.EnumDecl, FbDeclKind.TypeAliasDecl, FbDeclKind.FuncDecl, FbDeclKind.VarDecl,
    FbDeclKind.ExtendDecl
)

private val classDeclKinds = setOf(
    FbDeclKind.ClassDecl, FbDeclKind.InterfaceDecl,
    FbDeclKind.StructDecl, FbDeclKind.EnumDecl
)
class PackageWrapper(
    val original: FbPackage,

    ) : DeclarationWrapper {
    val declTable = DeclTable(original.decls)
    val typeTable = TypeTable(original.types)

    val cjoVersion: BuiltInsBinaryVersion = original.cjoVersion
    val files: List<String> = original.files

    val importPackageFqNames: List<FqName> = original.imports.map { FqName.fromString(it) }



    // 一次性过滤所有顶层声明，避免多次遍历
    private val allToplevelDecl = declTable.byTypeKind
        .filterKeys { it in targetDeclKinds }
        .mapValues { (_, declList) -> declList.filter { it.isTopLevel } }
        .filterValues { it.isNotEmpty() }

    val packageName: FqName = original.packageName

    val interfaces =
        allToplevelDecl[FbDeclKind.InterfaceDecl]?.map { decl -> InterfaceWrapper(decl, declTable, typeTable) }
            ?: emptyList()

    val structs =
        allToplevelDecl[FbDeclKind.StructDecl]?.map { decl -> StructWrapper(decl, declTable, typeTable) } ?: emptyList()
    val enums =
        allToplevelDecl[FbDeclKind.EnumDecl]?.map { decl -> EnumWrapper(decl, declTable, typeTable) } ?: emptyList()


    // 使用预先过滤的结果，避免重复过滤
    val classs: List<ClassWrapper> =
        allToplevelDecl[FbDeclKind.ClassDecl]?.map { decl -> ClassWrapper(decl, declTable, typeTable) }
            ?: emptyList()


    val extends: List<ExtendWrapper> =
        allToplevelDecl[FbDeclKind.ExtendDecl]?.map { decl -> ExtendWrapper(decl, declTable, typeTable) }
            ?: emptyList()
    val allClassDecls: List<ClassDeclWrapper> = classs + interfaces + structs + enums

    val functions: List<FunctionWrapper> = allToplevelDecl[FbDeclKind.FuncDecl]
        ?.map { FunctionWrapper(it, declTable, typeTable) } ?: emptyList()

    val variables: List<VariableWrapper> = allToplevelDecl[FbDeclKind.VarDecl]
        ?.map { VariableWrapper(it, declTable, typeTable) } ?: emptyList()

    val typeAliass: List<TypeAliasWrapper> = allToplevelDecl[FbDeclKind.TypeAliasDecl]
        ?.map { TypeAliasWrapper(it, declTable, typeTable) } ?: emptyList()


}

interface ClassDeclWrapper : DeclarationWrapper, TypeParameter {
    val constructors: List<ConstructorWrapper> get() = emptyList()
    val functions: List<FunctionWrapper> get() = emptyList()
    val variables: List<VariableWrapper> get() = emptyList()
    override val typeParameters: List<TypeParameterWrapper> get() = emptyList()
    val propertys: List<PropertyWrapper> get() = emptyList()
    val superTypes: List<TypeWrapper> get() = emptyList()
    override val annotations: List<AnnotationWrapper> get() = emptyList()
    val kind: ClassKind
    val name: Name
    val modality: Modality
    val visibility: DescriptorVisibility

    val classId: ClassId
}


class EnumWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val name: Name = original.name
    override val classId: ClassId = ClassId.topLevel(original.packagefqName)

    override val kind: ClassKind = ClassKind.ENUM
    override val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED

        else -> Modality.FINAL
    }


    override val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }

    val info = original.info as FbDeclInfo.EnumInfo
    val bodyDecls: List<FbDecl> = declTable.get(original.info.body)
    val declByTypeKind = bodyDecls.groupBy { it.kind }

    override val annotations: List<AnnotationWrapper> = original.annotations.map {
        AnnotationWrapper(it, declTable, typeTable)
    }

    override val constructors: List<ConstructorWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        it.attributePack.testAttr(Attribute.CONSTRUCTOR) || it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)
    }?.map {
        ConstructorWrapper(it, declTable, typeTable)
    } ?: emptyList()

    override val functions: List<FunctionWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {

        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR) && !it.attributePack.testAttr(
            Attribute.ENUM_CONSTRUCTOR
        )
    }?.map {
        FunctionWrapper(it, declTable, typeTable)
    } ?: emptyList()

    val entrys: List<EnumConstructorWrapper> = (declByTypeKind[FbDeclKind.VarDecl]?.map {
        EnumConstructorWrapper(it, declTable, typeTable)
    } ?: emptyList()) + (declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR) && it.attributePack.testAttr(
            Attribute.ENUM_CONSTRUCTOR
        )
    }?.map {
        EnumConstructorWrapper(it, declTable, typeTable)
    } ?: emptyList())

    override val propertys: List<PropertyWrapper> = declByTypeKind[FbDeclKind.PropDecl]?.map {
        PropertyWrapper(it, declTable, typeTable)
    } ?: emptyList()

    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }


    override val superTypes = typeTable.get(original.info.inheritedTypes).map { TypeWrapper(it, declTable, typeTable) }
    val isNonExhaustive = info.nonExhaustive
    val hasArguments = info.hasArguments
}

class EnumConstructorWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val kind: ClassKind = ClassKind.ENUM
    val name = original.name

    val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val valueParameters = if (original.kind == FbDeclKind.FuncDecl) {
        FunctionWrapper(original, declTable, typeTable).valueParameters
    } else emptyList()


}

class InterfaceWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val name: Name = original.name
    override val classId: ClassId = ClassId.topLevel(original.packagefqName)
    override val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED

        else -> Modality.OPEN
    }

    override val kind: ClassKind = ClassKind.INTERFACE
    override val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val info = original.info as FbDeclInfo.InterfaceInfo
    val bodyDecls: List<FbDecl> = declTable.get(original.info.body)
    val declByTypeKind = bodyDecls.groupBy { it.kind }

    override val annotations: List<AnnotationWrapper> = original.annotations.map {
        AnnotationWrapper(it, declTable, typeTable)
    }
    override val constructors: List<ConstructorWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        it.attributePack.testAttr(Attribute.CONSTRUCTOR) || it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)

    }?.map {
        ConstructorWrapper(it, declTable, typeTable)
    } ?: emptyList()

    override val functions: List<FunctionWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)
    }?.map {
        FunctionWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val variables: List<VariableWrapper> = declByTypeKind[FbDeclKind.VarDecl]?.map {
        VariableWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val propertys: List<PropertyWrapper> = declByTypeKind[FbDeclKind.PropDecl]?.map {
        PropertyWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }

    override val superTypes = typeTable.get(original.info.inheritedTypes).map { TypeWrapper(it, declTable, typeTable) }

}

interface TypeParameter {
    val typeParameters: List<TypeParameterWrapper>
}

class StructWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val classId: ClassId = ClassId.topLevel(original.packagefqName)
    override val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED

        else -> Modality.FINAL
    }

    override val name: Name = original.name
    override val kind: ClassKind = ClassKind.STRUCT
    override val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val info = original.info as FbDeclInfo.StructInfo
    val bodyDecls: List<FbDecl> = declTable.get(original.info.body)
    val declByTypeKind = bodyDecls.groupBy { it.kind }

    override val annotations: List<AnnotationWrapper> = original.annotations.map {
        AnnotationWrapper(it, declTable, typeTable)
    }
    override val constructors: List<ConstructorWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        it.attributePack.testAttr(Attribute.CONSTRUCTOR) || it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)

    }?.map {
        ConstructorWrapper(it, declTable, typeTable)
    } ?: emptyList()

    override val functions: List<FunctionWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)
    }?.map {
        FunctionWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val variables: List<VariableWrapper> = declByTypeKind[FbDeclKind.VarDecl]?.map {
        VariableWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val propertys: List<PropertyWrapper> = declByTypeKind[FbDeclKind.PropDecl]?.map {
        PropertyWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }

    override val superTypes = typeTable.get(original.info.inheritedTypes).map { TypeWrapper(it, declTable, typeTable) }
}

fun FbDecl.toClassDeclWrapper(declTable: DeclTable, typeTable: TypeTable): ClassDeclWrapper? {
    return when (this.info) {
        is FbDeclInfo.ClassInfo -> ClassWrapper(this, declTable, typeTable)
        is FbDeclInfo.EnumInfo -> EnumWrapper(this, declTable, typeTable)
        is FbDeclInfo.InterfaceInfo -> InterfaceWrapper(this, declTable, typeTable)

        is FbDeclInfo.StructInfo -> StructWrapper(this, declTable, typeTable)
        else -> null
    }
}

/**
 * 将 FbDecl 转换为 DeclarationWrapper
 *
 * 根据声明类型创建对应的 Wrapper 对象
 */
fun FbDecl.toDeclarationWrapper(declTable: DeclTable, typeTable: TypeTable): DeclarationWrapper? {
    return when (this.info) {
        is FbDeclInfo.ClassInfo -> ClassWrapper(this, declTable, typeTable)
        is FbDeclInfo.EnumInfo -> EnumWrapper(this, declTable, typeTable)
        is FbDeclInfo.InterfaceInfo -> InterfaceWrapper(this, declTable, typeTable)
        is FbDeclInfo.StructInfo -> StructWrapper(this, declTable, typeTable)
        is FbDeclInfo.FuncInfo -> {
            // 区分构造函数和普通函数
            if (this.attributePack.testAttr(Attribute.CONSTRUCTOR) ||
                this.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)) {
                ConstructorWrapper(this, declTable, typeTable)
            } else {
                FunctionWrapper(this, declTable, typeTable)
            }
        }
        is FbDeclInfo.VarInfo -> VariableWrapper(this, declTable, typeTable)
        is FbDeclInfo.PropInfo -> PropertyWrapper(this, declTable, typeTable)
        is FbDeclInfo.AliasInfo -> TypeAliasWrapper(this, declTable, typeTable)
        is FbDeclInfo.ExtendInfo -> ExtendWrapper(this, declTable, typeTable)
        is FbDeclInfo.ParamInfo -> ValueParameterWrapper(this, declTable, typeTable)
        else -> null
    }
}

class ClassWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val classId: ClassId = ClassId.topLevel(original.packagefqName)
    override val name: Name = original.name
    override val kind: ClassKind = ClassKind.CLASS
    override val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    private val info = original.info as FbDeclInfo.ClassInfo
    val bodyDecls: List<FbDecl> = declTable.get(original.info.body)
    val declByTypeKind = bodyDecls.groupBy { it.kind }
    override val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED

        else -> Modality.FINAL
    }

    override val annotations: List<AnnotationWrapper> = original.annotations.map {
        AnnotationWrapper(it, declTable, typeTable)
    }
    override val constructors: List<ConstructorWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        it.attributePack.testAttr(Attribute.CONSTRUCTOR) || it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)

    }?.map {
        ConstructorWrapper(it, declTable, typeTable)
    } ?: emptyList()

    override val functions: List<FunctionWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)
    }?.map {
        FunctionWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val variables: List<VariableWrapper> = declByTypeKind[FbDeclKind.VarDecl]?.map {
        VariableWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val propertys: List<PropertyWrapper> = declByTypeKind[FbDeclKind.PropDecl]?.map {
        PropertyWrapper(it, declTable, typeTable)
    } ?: emptyList()
    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }
    override val superTypes = typeTable.get(original.info.inheritedTypes).map { TypeWrapper(it, declTable, typeTable) }
    val isAnnotations = info.isAnno
}

class ExtendWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper{
    val packageFqName = original.packagefqName

    //    被扩展类型
    val type = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }
    private val info = original.info as FbDeclInfo.ExtendInfo

    //继承列表
    val superTypes = typeTable.get(info.inheritedTypes).map {
        TypeWrapper(it, declTable, typeTable)
    }

    val bodyDecls: List<FbDecl> = declTable.get(original.info.body)
    val declByTypeKind = bodyDecls.groupBy { it.kind }
    val functions: List<FunctionWrapper> = declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)
    }?.map {
        FunctionWrapper(it, declTable, typeTable)
    } ?: emptyList()
    val variables: List<VariableWrapper> = declByTypeKind[FbDeclKind.VarDecl]?.map {
        VariableWrapper(it, declTable, typeTable)
    } ?: emptyList()
    val propertys: List<PropertyWrapper> = declByTypeKind[FbDeclKind.PropDecl]?.map {
        PropertyWrapper(it, declTable, typeTable)
    } ?: emptyList()
    val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }

    val id = original.exportId!!

    override val annotations: List<AnnotationWrapper> = original.annotations.map { AnnotationWrapper(it, declTable, typeTable) }
}

class PropertyWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val name: Name = original.name
    val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED
        else -> Modality.FINAL
    }
    val kind: CallableMemberDescriptor.Kind = when {

        else -> CallableMemberDescriptor.Kind.DECLARATION
    }
    private val info = original.info as FbDeclInfo.PropInfo
    val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val isVar = info.isMutable

    val isConst = info.isConst

    val isOverride = original.attributePack.testAttr(Attribute.OVERRIDE)
    val isRedef = original.attributePack.testAttr(Attribute.REDEF)

    val returnType = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }
    val getter = info.getter?.let {
        declTable.get(it).let {
            FunctionWrapper(
                it,
                declTable,
                typeTable
            )
        }
    }

    val setter = info.setter?.let {
        declTable.get(it).let {
            FunctionWrapper(
                it,
                declTable,
                typeTable
            )
        }
    }

    override val annotations: List<AnnotationWrapper> = original.annotations.map { AnnotationWrapper(it, declTable, typeTable) }
}

class TypeWrapper(
    val original: FbSemaTy,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val kind = original.kind
    val info = original.info

    val typeArgs get() = original.typeArgs.map { TypeWrapper(typeTable.get(it), declTable, typeTable) }

    fun hasTypeAlias(): Boolean {
        return original.kind == FbTypeKind.Type
    }
}

class VariableWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val name: Name = original.name
    private val info = original.info as FbDeclInfo.VarInfo
    val isVar = info.isVar
    val isConst = info.isConst
    val isMemberParam = info.isMemberParam
    val isStatic = original.attributePack.testAttr(Attribute.STATIC)
    val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED
        else -> Modality.FINAL
    }
    val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }

    val isTopLevel = original.isTopLevel

    val returnType = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }
    val kind: CallableMemberDescriptor.Kind = when {

        else -> CallableMemberDescriptor.Kind.DECLARATION
    }
    val declaresDefaultValue = info.initializer != 0

    override val annotations: List<AnnotationWrapper> = original.annotations.map { AnnotationWrapper(it, declTable, typeTable) }
}

class TypeAliasWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    private val info = original.info as FbDeclInfo.AliasInfo
    val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val name: Name = original.name

    /**
     * 展开类型，即递归展开所有类型别名后的最终类型。
     * 例如：
     * ```cangjie
     * type StringList = List<String>;
     * type MyStringList = StringList
     * ```
     * 对于 MyStringList，underlyingType 是 StringList，expandedType 是 List<String>
     *
     * 但是仓颉序列化文件目前不支持underlyingType，只有expandedType完全递归展开后的
     */
    val underlyingType = typeTable.get(info.aliasedTy).let { TypeWrapper(it, declTable, typeTable) }


    val expandedType = typeTable.get(info.aliasedTy).let { TypeWrapper(it, declTable, typeTable) }
}

class ConstructorWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper, TypeParameter {
    private val info = original.info as FbDeclInfo.FuncInfo

    val isPrimary = original.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)
    val returnType = typeTable.get(info.funcBody.retType).let { TypeWrapper(it, declTable, typeTable) }
    val ownType = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }
    val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }

//    val contracts: List<ContractWrapper> = if (original.generic == null) emptyList()
//    else {
//        original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) }
//    }

    val valueParameters: List<ValueParameterWrapper> = info.funcBody.params.map {
        ValueParameterWrapper(declTable.get(it), declTable, typeTable)
    }

}

//class ContractWrapper(
//    val original: FbConstraint,
//    val declTable: DeclTable,
//    val typeTable: TypeTable,
//) : DeclarationWrapper {
//    val type = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }
//    val uppers = typeTable.get(original.uppers).map { TypeWrapper(it, declTable, typeTable) }
//}
class ContractWrapper(
    val original: FbConstraint,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val type = typeTable.get(original.type)
    val uppers = typeTable.get(original.uppers).map { TypeWrapper(it, declTable, typeTable) }

}

class TypeParameterWrapper(

    val original: FbDecl,
    contracts: List<ContractWrapper>,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val name = original.name
    val type = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }

    //    val name: Name = original.name
    val uppers = mutableListOf<TypeWrapper>()

    init {
//        查找对应的约束

        val contract = contracts.find { it.original.type == original.type }
        if (contract != null) {
            uppers.addAll(contract.uppers)
        }


    }


//    val ownType = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }

}

class FunctionWrapper(

    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper, TypeParameter {

    private val info = original.info as FbDeclInfo.FuncInfo

    val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val name: Name = original.name
    val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED

        else -> Modality.FINAL
    }


    val isOverride = original.attributePack.testAttr(Attribute.OVERRIDE)
    val isRedef = original.attributePack.testAttr(Attribute.REDEF)
    override val annotations: List<AnnotationWrapper> = original.annotations.map {
        AnnotationWrapper(it, declTable, typeTable)
    }

    val isStatic = original.attributePack.testAttr(Attribute.STATIC)
    val isOperator = original.attributePack.testAttr(Attribute.OPERATOR)
    val kind: CallableMemberDescriptor.Kind = when {

        else -> CallableMemberDescriptor.Kind.DECLARATION
    }

    val isConst = info.isConst
    val isInLine = info.isInline
    val isFastNative = info.isFastNative
    val isMacro = original.attributePack.testAttr(Attribute.MACRO_FUNC)
    val isMainEntry = original.attributePack.testAttr(Attribute.MAIN_ENTRY)

    val returnType = typeTable.get(info.funcBody.retType).let { TypeWrapper(it, declTable, typeTable) }
    val ownType = typeTable.get(original.type).let { TypeWrapper(it, declTable, typeTable) }

    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map {
            TypeParameterWrapper(
                declTable.get(it),
                original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) },
                declTable,
                typeTable
            )
        }
    }

    val valueParameters: List<ValueParameterWrapper> = info.funcBody.params.map {
        ValueParameterWrapper(declTable.get(it), declTable, typeTable)
    }


    //接口
    val isInInterfaceAndDefault = original.attributePack.testAttr(Attribute.DEFAULT)
}


class AnnotationWrapper(
    val original: FbAnno,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val name = original.name
}

class ValueParameterWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : DeclarationWrapper {
    val name = original.name
    val info = original.info as FbDeclInfo.ParamInfo

    val type = TypeWrapper(typeTable.get(original.type), declTable, typeTable)

    val isNamedParam = info.isNamedParam
    val isMemberParam = info.isMemberParam

    override val annotations: List<AnnotationWrapper> = original.annotations.map { AnnotationWrapper(it, declTable, typeTable) }

    val declaresDefaultValue = info.defaultVal != 0
}