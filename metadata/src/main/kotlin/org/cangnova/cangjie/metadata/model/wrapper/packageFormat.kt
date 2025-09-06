package org.cangnova.cangjie.metadata.model.wrapper

import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.deserialization.DeclTable
import org.cangnova.cangjie.metadata.deserialization.TypeTable
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.metadata.model.fb.FbAnno
import org.cangnova.cangjie.metadata.model.fb.FbConstraint
import org.cangnova.cangjie.metadata.model.fb.FbDecl
import org.cangnova.cangjie.metadata.model.fb.FbDeclInfo
import org.cangnova.cangjie.metadata.model.fb.FbDeclKind
import org.cangnova.cangjie.metadata.model.fb.FbDeclKind.*
import org.cangnova.cangjie.metadata.model.fb.FbOperatorKind
import org.cangnova.cangjie.metadata.model.fb.FbPackage
import org.cangnova.cangjie.metadata.model.fb.FbSemaTy
import org.cangnova.cangjie.metadata.model.util.toName
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

//对FbPackage进行二次封装


class PackageWrapper(
    val original: FbPackage,

    ) {
    val declTable = DeclTable(original.decls)
    val typeTable = TypeTable(original.types)


    // 预定义需要的声明类型集合，避免重复创建
    private val targetDeclKinds = setOf(
        FbDeclKind.ClassDecl, FbDeclKind.InterfaceDecl, FbDeclKind.StructDecl,
        FbDeclKind.EnumDecl, FbDeclKind.TypeAliasDecl, FbDeclKind.FuncDecl, FbDeclKind.VarDecl
    )

    private val classDeclKinds = setOf(
        FbDeclKind.ClassDecl, FbDeclKind.InterfaceDecl,
        FbDeclKind.StructDecl, FbDeclKind.EnumDecl
    )

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


    val allClassDecls: List<ClassDeclWrapper> = classs + interfaces + structs + enums

    val functions: List<FunctionWrapper> = allToplevelDecl[FbDeclKind.FuncDecl]
        ?.map { FunctionWrapper(it, declTable, typeTable) } ?: emptyList()

    val variables: List<VariableWrapper> = allToplevelDecl[FbDeclKind.VarDecl]
        ?.map { VariableWrapper(it, declTable, typeTable) } ?: emptyList()

    val typeAliass: List<TypeAliasWrapper> = allToplevelDecl[FbDeclKind.TypeAliasDecl]
        ?.map { TypeAliasWrapper(it, declTable, typeTable) } ?: emptyList()
}

interface ClassDeclWrapper {
    val constructors: List<ConstructorWrapper> get() = emptyList()
    val functions: List<FunctionWrapper> get() = emptyList()
    val variables: List<VariableWrapper> get() = emptyList()
    val typeParameters: List<TypeParameterWrapper> get() = emptyList()
    val contracts: List<ContractWrapper> get() = emptyList()
    val propertys: List<PropertyWrapper> get() = emptyList()
    val superTypes: List<FbSemaTy> get() = emptyList()
    val annotations: List<AnnotationWrapper> get() = emptyList()
    val kind: ClassKind
    val name: Name

    val visibility: DescriptorVisibility
}

class EnumWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val name: Name = original.name
    override val kind: ClassKind = ClassKind.ENUM


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

    val entrys: List<EnumEntryWrapper> = (declByTypeKind[FbDeclKind.VarDecl]?.map {
        EnumEntryWrapper(it, declTable, typeTable)
    } ?: emptyList()) + (declByTypeKind[FbDeclKind.FuncDecl]?.filter {
        !it.attributePack.testAttr(Attribute.CONSTRUCTOR) && !it.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR) && it.attributePack.testAttr(
            Attribute.ENUM_CONSTRUCTOR
        )
    }?.map {
        EnumEntryWrapper(it, declTable, typeTable)
    } ?: emptyList())

    override val propertys: List<PropertyWrapper> = declByTypeKind[FbDeclKind.PropDecl]?.map {
        PropertyWrapper(it, declTable, typeTable)
    } ?: emptyList()

    override val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map { TypeParameterWrapper(declTable.get(it), declTable, typeTable) }
    }

    override val contracts: List<ContractWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) }
    }

    override val superTypes = typeTable.get(original.info.inheritedTypes)
    val isNonExhaustive = info.nonExhaustive
    val hasArguments = info.hasArguments
}

class EnumEntryWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val kind: ClassKind = ClassKind.ENUM
    val name = original.name


}

class InterfaceWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val name: Name = original.name
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
        original.generic.typeParameters.map { TypeParameterWrapper(declTable.get(it), declTable, typeTable) }
    }
    override val contracts: List<ContractWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) }
    }
    override val superTypes = typeTable.get(original.info.inheritedTypes)

}

class StructWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
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
        original.generic.typeParameters.map { TypeParameterWrapper(declTable.get(it), declTable, typeTable) }
    }
    override val contracts: List<ContractWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) }
    }
    override val superTypes = typeTable.get(original.info.inheritedTypes)
}

class ClassWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) : ClassDeclWrapper {
    override val name: Name = original.name
    override val kind: ClassKind = ClassKind.CLASS
    override val visibility: DescriptorVisibility = when {
        original.attributePack.testAttr(Attribute.PUBLIC) -> DescriptorVisibilities.PUBLIC
        original.attributePack.testAttr(Attribute.INTERNAL) -> DescriptorVisibilities.INTERNAL
        original.attributePack.testAttr(Attribute.PRIVATE) -> DescriptorVisibilities.PRIVATE
        original.attributePack.testAttr(Attribute.PROTECTED) -> DescriptorVisibilities.PROTECTED

        else -> DescriptorVisibilities.INTERNAL
    }
    val info = original.info as FbDeclInfo.ClassInfo
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
        original.generic.typeParameters.map { TypeParameterWrapper(declTable.get(it), declTable, typeTable) }
    }
    override val contracts: List<ContractWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) }
    }
    override val superTypes = typeTable.get(original.info.inheritedTypes)
    val isAnnotations = info.isAnno
}

class PropertyWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
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

    val isVar = info.isMutable

    val isConst = info.isConst

    val returnType = typeTable.get(original.type)
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

}

class VariableWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val name: Name = original.name
    private val info = original.info as FbDeclInfo.VarInfo
    val isVar = info.isVar
    val isConst = info.isConst
    val isMemberParam = info.isMemberParam
    val isStatic = original.attributePack.testAttr(Attribute.STATIC)

    val isTopLevel = original.isTopLevel

    val returnType = typeTable.get(original.type)
    val kind: CallableMemberDescriptor.Kind = when {

        else -> CallableMemberDescriptor.Kind.DECLARATION
    }

}

class TypeAliasWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    private val info = original.info as FbDeclInfo.AliasInfo

    val name: Name = original.name

    val underlyingType = typeTable.get(info.aliasedTy)
}

class ConstructorWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {

    val isPrimary = original.attributePack.testAttr(Attribute.PRIMARY_CONSTRUCTOR)


}

class TypeParameterWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val name: Name = original.name

    val ownType = typeTable.get(original.type)

}

class FunctionWrapper(

    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {

    private val info = original.info as FbDeclInfo.FuncInfo


    val name: Name = original.name
    val modality: Modality = when {
        original.attributePack.testAttr(Attribute.ABSTRACT) -> Modality.ABSTRACT
        original.attributePack.testAttr(Attribute.OPEN) -> Modality.OPEN
        original.attributePack.testAttr(Attribute.SEALED) -> Modality.SEALED

        else -> Modality.FINAL
    }


    val isOverride = original.attributePack.testAttr(Attribute.OVERRIDE)
    val isRedef = original.attributePack.testAttr(Attribute.REDEF)
    val annotations: List<AnnotationWrapper> = original.annotations.map {
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

    val returnType = typeTable.get(info.funcBody.retType)
    val ownType = typeTable.get(original.type)

    val typeParameters: List<TypeParameterWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.typeParameters.map { TypeParameterWrapper(declTable.get(it), declTable, typeTable) }
    }

    val contracts: List<ContractWrapper> = if (original.generic == null) emptyList()
    else {
        original.generic.constraints.map { ContractWrapper(it, declTable, typeTable) }
    }

    val valueParameters: List<ValueParameterWrapper> = info.funcBody.params.map {
        ValueParameterWrapper(declTable.get(it), declTable, typeTable)
    }


    //接口
    val isInInterfaceAndDefault = original.attributePack.testAttr(Attribute.DEFAULT)
}

class ContractWrapper(
    val original: FbConstraint,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val type = typeTable.get(original.type)
    val uppers = typeTable.get(original.uppers)
}

class AnnotationWrapper(
    val original: FbAnno,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val name = original.name
}

class ValueParameterWrapper(
    val original: FbDecl,
    val declTable: DeclTable,
    val typeTable: TypeTable,
) {
    val name = original.name
    val info = original.info as FbDeclInfo.ParamInfo

    val type = typeTable.get(original.type)

    val isNamedParam = info.isNamedParam
    val isMemberParam = info.isMemberParam

}