package org.cangnova.cangjie.metadata.model.parser

import org.cangnova.cangjie.metadata.PackageFormat.CompositeTyInfo
import org.cangnova.cangjie.metadata.PackageFormat.FuncTyInfo
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

fun ByteBuffer.toPackage(): org.cangnova.cangjie.metadata.model.Package {
    val packageFb = org.cangnova.cangjie.metadata.PackageFormat.Package.getRootAsPackage(this)
    return packageFb.parser()
}

fun InputStream.toPackage(): org.cangnova.cangjie.metadata.model.Package {
    return ByteBuffer.wrap(this.readBytes()).toPackage()
}

fun ByteArrayInputStream.toPackage(): org.cangnova.cangjie.metadata.model.Package {

    return ByteBuffer.wrap(this.readAllBytes()).toPackage()
}

fun org.cangnova.cangjie.metadata.PackageFormat.Package.parser(): org.cangnova.cangjie.metadata.model.Package {
//TODO 低版本可能没有fullPkgName
    return org.cangnova.cangjie.metadata.model.Package(
        cjcVersion = this.version ?: "0.0.0",
        cjoVersion = this.cjoVersion?.toBinaryVersion() ?: BuiltInsBinaryVersion.INSTANCE,
        fullPkgName = this.fullPkgName ?: "",
        pkgDepInfo = this.pkgDepInfo ?: "",
        imports = this.imports,
        files = this.files,
        fileImports = this.fileImports,
        types = this.types,
        exprs = this.exprs,
        decls = this.decls,
        values = this.values,
        moduleName = this.moduleName ?: "",


        )


}

val org.cangnova.cangjie.metadata.PackageFormat.Package.fileImports: List<Imports>
    get() = (0 until this.allFileImportsLength).mapNotNull { this.allFileImports(it)?.parser() }

fun org.cangnova.cangjie.metadata.PackageFormat.Imports.parser(): Imports {
    return Imports(
        importSpecs = this.importSpecs
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Imports.importSpecs: List<ImportSpec>
    get() = (0 until this.importSpecsLength).mapNotNull { this.importSpecs(it)?.parser() }

fun org.cangnova.cangjie.metadata.PackageFormat.ImportSpec.parser(): ImportSpec {
    return ImportSpec(
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        prefixPaths = this.prefixPaths,
        identifier = this.identifier ?: "",
        asIdentifier = this.asIdentifier ?: "",
        reExport = AccessModifier.fromUByte(this.reExport)

    )
}

val org.cangnova.cangjie.metadata.PackageFormat.ImportSpec.prefixPaths: List<String>
    get() = (0 until this.prefixPathsLength).mapNotNull { this.prefixPaths(it) }

fun org.cangnova.cangjie.metadata.PackageFormat.Position.parser(): org.cangnova.cangjie.metadata.model.Position {
    return org.cangnova.cangjie.metadata.model.Position(
        file = this.file.toInt(),
        line = this.line.toInt(),
        column = this.column.toInt(),
        pkgId = this.pkgId.toInt(),
        ignore = this.ignore,

        )
}

val org.cangnova.cangjie.metadata.PackageFormat.Expr.operands: List<UInt>
    get() {
        return (0 until this.operandsLength).mapNotNull { this.operands(it) }
    }

fun org.cangnova.cangjie.metadata.PackageFormat.Expr.parser(): Expr {
    return Expr(
        kind = ExprKind.fromUShort(this.kind),
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        mapExpr = this.mapExpr,
        operands = this.operands,
        type = this.type,
        overflowPolicy = OverflowPolicy.fromUByte(this.overflowPolicy),
        info = this.info
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.Decl.parser(): Decl {
    return Decl(
        kind = DeclKind.fromUShort(this.kind),
        isTopLevel = this.isTopLevel,
        fullPkgName = this.fullPkgName ?: "",
        genericDecl = this.genericDecl?.parser(),
        generic = this.generic?.parser(),
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        identifier = this.identifier ?: "error_name",
        identifierPos = this.identifierPos?.parser(),
        attributes = this.attributes,
        annotations = this.annotations,
        type = this.type,
        mangledName = this.mangledName,
        exportId = this.exportId,
        mangledBeforeSema = this.mangledBeforeSema,
        hash = this.hash?.parser(),
        info = this.info,

        )

}

val org.cangnova.cangjie.metadata.PackageFormat.Decl.info: DeclInfo
    get() {
        return when (this.infoType) {
            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.ClassInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ClassInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ClassInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.InterfaceInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.InterfaceInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.InterfaceInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.StructInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.StructInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.StructInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.EnumInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.EnumInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.EnumInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.ExtendInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ExtendInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ExtendInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.PropInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.PropInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.PropInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.VarInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.VarInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.VarInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.VarWithPatternInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.VarWithPatternInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.VarWithPatternInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.ParamInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ParamInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ParamInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.FuncInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.FuncInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.FuncInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.BuiltInInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.BuiltInInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.BuiltInInfo)?.parser()
                ?: DeclInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.DeclInfo.AliasInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.AliasInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.AliasInfo)?.parser()
                ?: DeclInfo.None

            else -> DeclInfo.None
        }
    }


fun org.cangnova.cangjie.metadata.PackageFormat.ClassInfo.parser(): DeclInfo.Class {
    return DeclInfo.Class(
        ClassInfo(
            inheritedTypes = this.inheritedTypes,
            body = this.bodys,
            adInfo = this.adInfo?.parser(),
            isAnno = this.isAnno,
            annoTargets = this.annoTargets,
            runtimeVisible = this.runtimeVisible,
            annoTargets2 = this.annoTargets2
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.InterfaceInfo.parser(): DeclInfo.Interface {
    return DeclInfo.Interface(
        InterfaceInfo(
            inheritedTypes = this.inheritedTypes,
            body = this.bodys
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.StructInfo.parser(): DeclInfo.Struct {
    return DeclInfo.Struct(
        StructInfo(
            inheritedTypes = this.inheritedTypes,
            body = this.bodys,
            adInfo = this.adInfo?.parser() ?: AutoDiffInfo(false, false, "", emptyList(), emptyList(), 0u)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.EnumInfo.parser(): DeclInfo.Enum {
    return DeclInfo.Enum(
        EnumInfo(
            inheritedTypes = this.inheritedTypes,
            body = this.bodys,
            adInfo = this.adInfo?.parser() ?: AutoDiffInfo(false, false, "", emptyList(), emptyList(), 0u),
            hasArguments = this.hasArguments,
            nonExhaustive = this.nonExhaustive,
            ellipsisPos = this.ellipsisPos?.parser() ?: Position(0, 0, 0, 0, false)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.ExtendInfo.parser(): DeclInfo.Extend {
    return DeclInfo.Extend(
        ExtendInfo(
            inheritedTypes = this.inheritedTypes,
            body = this.bodys
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.PropInfo.parser(): DeclInfo.Prop {
    return DeclInfo.Prop(
        PropInfo(
            isConst = this.isConst,
            isMutable = this.isMutable,
            setters = this.setters,
            getters = this.getters
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.PropInfo.setters: List<UInt>
    get() {
        return (0 until this.settersLength).mapNotNull { this.setters(it) }
    }
val org.cangnova.cangjie.metadata.PackageFormat.PropInfo.getters: List<UInt>
    get() {
        return (0 until this.gettersLength).mapNotNull { this.getters(it) }
    }

fun org.cangnova.cangjie.metadata.PackageFormat.VarInfo.parser(): DeclInfo.Var {
    return DeclInfo.Var(
        VarInfo(
            isVar = this.isVar,
            isConst = this.isConst,
            isMemberParam = this.isMemberParam,
            initializer = this.initializer.toInt(),
            value = this.parserValue()
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.VarInfo.parserValue(): ConstValue {
    return when (this.valueType) {
        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int8Value -> {
            val int8Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int8Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int8Value
            ConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt8Value -> {
            val uint8Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt8Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt8Value
            ConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int16Value -> {
            val int16Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int16Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int16Value
            ConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt16Value -> {
            val uint16Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt16Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt16Value
            ConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int32Value -> {
            val int32Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int32Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int32Value
            ConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt32Value -> {
            val uint32Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt32Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt32Value
            ConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int64Value -> {
            val int64Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int64Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int64Value
            ConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt64Value -> {
            val uint64Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt64Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt64Value
            ConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float32Value -> {
            val float32Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Float32Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Float32Value
            ConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float64Value -> {
            val float64Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Float64Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Float64Value
            ConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.ArrayValue -> {
            val arrayValue =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.ArrayValue()) as? org.cangnova.cangjie.metadata.PackageFormat.ArrayValue
            ConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.StringValue -> {
            ConstValue.StringValue(this.value(this).toString())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.CompositeValue -> {
            val compositeValue =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex()) as? org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex
            ConstValue.CompositeValue(CompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> ConstValue.None
    }
}

fun org.cangnova.cangjie.metadata.PackageFormat.VarWithPatternInfo.parser(): DeclInfo.VarWithPattern {
    return DeclInfo.VarWithPattern(
        VarWithPatternInfo(
            isVar = this.isVar,
            isConst = this.isConst,
            irrefutablePattern = this.irrefutablePattern?.parser() ?: Pattern(
                kind = PatternKind.InvalidPattern,
                begin = Position(0, 0, 0, 0, false),
                end = Position(0, 0, 0, 0, false)
            ),
            initializer = this.initializer.toInt()
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.ParamInfo.parser(): DeclInfo.Param {
    return DeclInfo.Param(
        ParamInfo(
            isNamedParam = this.isNamedParam,
            isMemberParam = this.isMemberParam,
            defaultVal = this.defaultVal.toInt()
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.FuncInfo.parser(): DeclInfo.Func {
    return DeclInfo.Func(
        FuncInfo(
            funcBody = this.funcBody?.parser() ?: FuncBody(emptyList(), 0, 0, false, 0u),
            overflowPolicy = OverflowPolicy.fromUByte(this.overflowPolicy),
            op = OperatorKind.fromUByte(this.op),
            adInfo = this.adInfo?.parser() ?: AutoDiffInfo(false, false, "", emptyList(), emptyList(), 0u),
            isConst = this.isConst,
            isInline = this.isInline,
            isFastNative = this.isFastNative
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.BuiltInInfo.parser(): DeclInfo.BuiltIn {
    return DeclInfo.BuiltIn(
        BuiltInInfo(
            builtInType = BuiltInType.fromUByte(this.builtInType)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.AliasInfo.parser(): DeclInfo.Alias {
    return DeclInfo.Alias(
        AliasInfo(
            aliasedTy = this.aliasedTy.toInt()
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.AutoDiffInfo.parser(): AutoDiffInfo {
    return AutoDiffInfo(
        isDiff = this.isDiff,
        isAdj = this.isAdj,
        primal = this.primal,
        excepts = this.excepts,
        includes = this.includes,
        stage = this.stage
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.AutoDiffInfo.includes: List<String>
    get() {
        return (0 until this.includesLength).mapNotNull { this.includes(it) }
    }
val org.cangnova.cangjie.metadata.PackageFormat.AutoDiffInfo.excepts: List<String>
    get() {
        return (0 until this.exceptsLength).mapNotNull { this.excepts(it) }
    }
val org.cangnova.cangjie.metadata.PackageFormat.InterfaceInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.StructInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.ClassInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.StructInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.InterfaceInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.ClassInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.EnumInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.EnumInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.ExtendInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.ExtendInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }

fun org.cangnova.cangjie.metadata.PackageFormat.DeclHash.parser(): DeclHash {
    return DeclHash(
        instVar = this.instVar,
        virt = this.virt,
        sig = this.sig,
        srcUse = this.srcUse,
        bodyHash = this.bodyHash
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.Anno.parser(): Anno {
    return org.cangnova.cangjie.metadata.model.Anno(
        kind = AnnoKind.fromUShort(this.kind),
        identifier = this.identifier,
        args = this.args
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Anno.args: List<AnnoArg>
    get() {
        return (0 until this.argsLength).mapNotNull { this.args(it)?.parser() }
    }

fun org.cangnova.cangjie.metadata.PackageFormat.AnnoArg.parser(): AnnoArg {
    return AnnoArg(
        name = this.name ?: "",
        expr = this.expr
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Decl.annotations: List<Anno>
    get() = (0 until this.annotationsLength).mapNotNull { this.annotations(it)?.parser() }
val org.cangnova.cangjie.metadata.PackageFormat.Decl.attributes: List<ULong>
    get() = (0 until this.attributesLength).mapNotNull { this.attributes(it) }

fun org.cangnova.cangjie.metadata.PackageFormat.SemaTy.parser(): SemaTy {
    return SemaTy(
        kind = TypeKind.fromUShort(this.kind),
        typeArgs = this.typeArgs,
        info = this.info
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.SemaTy.info: SemaTyInfo
    get() {
        val a = this.info(FuncTyInfo())
        return when (this.infoType) {
            org.cangnova.cangjie.metadata.PackageFormat.SemaTyInfo.FuncTyInfo -> (this.info(FuncTyInfo()) as? FuncTyInfo)?.parser()
                ?: SemaTyInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.SemaTyInfo.CompositeTyInfo -> (this.info(CompositeTyInfo()) as? CompositeTyInfo)
                ?.parser() ?: SemaTyInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.SemaTyInfo.GenericTyInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.GenericTyInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.GenericTyInfo)?.parser()
                ?: SemaTyInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.SemaTyInfo.ArrayTyInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ArrayTyInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ArrayTyInfo)?.parser()
                ?: SemaTyInfo.None


            else -> SemaTyInfo.None
        }

    }

val org.cangnova.cangjie.metadata.PackageFormat.Expr.info: ExprInfo
    get() {


        return when (this.infoType) {
            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.CallInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.CallInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.CallInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.UnaryInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.UnaryInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.UnaryInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.BinaryInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.BinaryInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.BinaryInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.IncOrDecInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.IncOrDecInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.IncOrDecInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.LitConstInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.LitConstInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.LitConstInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.ReferenceInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ReferenceInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ReferenceInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.LambdaInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.LambdaInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.LambdaInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.AssignInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.AssignInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.AssignInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.ArrayInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ArrayInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ArrayInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.JumpInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.JumpInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.JumpInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.FuncArgInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.FuncArgInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.FuncArgInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.SubscriptInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.SubscriptInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.SubscriptInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.MatchInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.MatchInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.MatchInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.BlockInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.BlockInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.BlockInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.TryInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.TryInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.TryInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.LetPatternDestructorInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.LetPatternDestructorInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.LetPatternDestructorInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.ForInInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.ForInInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.ForInInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.MatchCaseInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.MatchCaseInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.MatchCaseInfo)?.parser()
                ?: ExprInfo.None

            org.cangnova.cangjie.metadata.PackageFormat.ExprInfo.SpawnInfo -> (this.info(org.cangnova.cangjie.metadata.PackageFormat.SpawnInfo()) as? org.cangnova.cangjie.metadata.PackageFormat.SpawnInfo)?.parser()
                ?: ExprInfo.None

            else -> ExprInfo.None
        }


    }

fun org.cangnova.cangjie.metadata.PackageFormat.CallInfo.parser(): ExprInfo.Call {
    return ExprInfo.Call(
        CallInfo(
            hasSideEffect = this.hasSideEffect,
            callKind = CallKind.fromUByte(this.callKind),
        )
    )
}


fun org.cangnova.cangjie.metadata.PackageFormat.ArrayTyInfo.parser(): SemaTyInfo.Array {
    return SemaTyInfo.Array(
        org.cangnova.cangjie.metadata.model.ArrayTyInfo(
            dimsOrSize = this.dimsOrSize

        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.GenericTyInfo.parser(): SemaTyInfo.Generic {
    return SemaTyInfo.Generic(
        org.cangnova.cangjie.metadata.model.GenericTyInfo(
            declPtr = this.declPtr?.parser(),
            upperBounds = this.upperBounds

        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.GenericTyInfo.upperBounds: List<Int>
    get() {
        return (0 until this.upperBoundsLength).mapNotNull { this.upperBounds(it).toInt() }
    }

fun org.cangnova.cangjie.metadata.PackageFormat.FuncTyInfo.parser(): SemaTyInfo.Func {
    return SemaTyInfo.Func(
        org.cangnova.cangjie.metadata.model.FuncTyInfo(
            retType = this.retType.toInt(),
            isC = this.isC,
            hasVariableLenArg = this.hasVariableLenArg,
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.Generic.parser(): Generic {
    return Generic(
        typeParameters = this.typeParameters,
        constraints = this.constraints
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.Constraint.parser(): Constraint? {
    return Constraint(
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        type = this.type,
        uppers = this.uppers
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Constraint.uppers: List<Int>
    get() {

        return (0 until this.uppersLength).mapNotNull { this.uppers(it).toInt() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.Generic.constraints: List<Constraint>
    get() {
        return (0 until this.constraintsLength).mapNotNull { this.constraints(it)?.parser() }
    }
val org.cangnova.cangjie.metadata.PackageFormat.Generic.typeParameters: List<Int>
    get() = (0 until this.typeParametersLength).mapNotNull { this.typeParameters(it).toInt() }

fun org.cangnova.cangjie.metadata.PackageFormat.FullId.parser(): FullId {
    return FullId(
        pkgId = this.pkgId,
        decl = try {
            this.decl ?: ""
        } catch (e: IndexOutOfBoundsException) {
            println("Warning: Failed to read decl field from FullId: ${e.message}")
            ""
        },
        index = this.index.toInt()
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.CompositeTyInfo.parser(): SemaTyInfo.Composite {
    return SemaTyInfo.Composite(
        org.cangnova.cangjie.metadata.model.CompositeTyInfo(
            declPtr = this.declPtr?.parser(),
            isThisTy = this.isThisTy
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Package.decls: List<Decl>
    get() = (0 until this.allDeclsLength).mapNotNull { this.allDecls(it)?.parser() }

val org.cangnova.cangjie.metadata.PackageFormat.SemaTy.typeArgs: List<Int>
    get() = (0 until this.typeArgsLength).mapNotNull { this.typeArgs(it).toInt() }

val org.cangnova.cangjie.metadata.PackageFormat.Package.types: List<SemaTy>
    get() = (0 until this.allTypesLength).mapNotNull { this.allTypes(it)?.parser() }
val org.cangnova.cangjie.metadata.PackageFormat.Package.exprs: List<Expr>
    get() = (0 until this.allExprsLength).mapNotNull { this.allExprs(it)?.parser() }
val org.cangnova.cangjie.metadata.PackageFormat.Package.values: List<CompositeValue>
    get() = (0 until this.allValuesLength).mapNotNull {


        this.allValues(it)?.parser()

    }
val org.cangnova.cangjie.metadata.PackageFormat.CompositeValue.fields: List<MemberValue>
    get() {
        return (0 until this.fieldsLength).mapNotNull { this.fields(it)?.parser() }


    }

fun org.cangnova.cangjie.metadata.PackageFormat.MemberValue.parser(): MemberValue {
    return MemberValue(
        field = this.field ?: "",
        type = this.type,
        value = this.parserValue()
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.CompositeValue.parser(): CompositeValue {
    return CompositeValue(
        type = this.type,
        fields = this.fields
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Package.files: List<String>
    get() = (0 until this.allFilesLength).mapNotNull { this.allFiles(it) }

val org.cangnova.cangjie.metadata.PackageFormat.Package.imports: List<String>
    get() = (0 until this.importsLength).mapNotNull { this.imports(it) }

fun org.cangnova.cangjie.metadata.PackageFormat.CjoVersion.toBinaryVersion(): BuiltInsBinaryVersion {
    return BuiltInsBinaryVersion(
        this.majorNum.toInt(),
        this.minorNum.toInt(),
        this.patchNum.toInt(),

        )
}


fun org.cangnova.cangjie.metadata.PackageFormat.UnaryInfo.parser(): ExprInfo.Unary {
    return ExprInfo.Unary(
        UnaryInfo(
            op = OperatorKind.fromUByte(this.op)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.BinaryInfo.parser(): ExprInfo.Binary {
    return ExprInfo.Binary(
        BinaryInfo(
            op = OperatorKind.fromUByte(this.op)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.IncOrDecInfo.parser(): ExprInfo.IncOrDec {
    return ExprInfo.IncOrDec(
        IncOrDecInfo(
            op = OperatorKind.fromUByte(this.op)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.LitConstInfo.parser(): ExprInfo.LitConst {
    return ExprInfo.LitConst(
        LitConstInfo(
            strValue = this.strValue ?: "",
            constKind = LitConstKind.fromUByte(this.constKind),
            strKind = StringKind.fromUByte(this.strKind)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.ReferenceInfo.parser(): ExprInfo.Reference {
    return ExprInfo.Reference(
        ReferenceInfo(
            reference = this.reference ?: "",
            target = this.target?.parser() ?: FullId(0, "", 0),
            instTys = this.instTys,
            matchedParentTy = this.matchedParentTy
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.ReferenceInfo.instTys: List<UInt>
    get() = (0 until this.instTysLength).mapNotNull { this.instTys(it) }

fun org.cangnova.cangjie.metadata.PackageFormat.LambdaInfo.parser(): ExprInfo.Lambda {
    return ExprInfo.Lambda(
        LambdaInfo(
            funcBody = this.funcBody?.parser() ?: FuncBody(emptyList(), 0, 0, false, 0u),
            supportMock = this.supportMock
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.AssignInfo.parser(): ExprInfo.Assign {
    return ExprInfo.Assign(
        AssignInfo(
            isCompound = this.isCompound,
            op = OperatorKind.fromUByte(this.op)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.ArrayInfo.parser(): ExprInfo.Array {
    return ExprInfo.Array(
        ArrayInfo(
            initFunc = this.initFunc?.parser() ?: FullId(0, "", 0),
            isValueArray = this.isValueArray
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.JumpInfo.parser(): ExprInfo.Jump {
    return ExprInfo.Jump(
        JumpInfo(
            isBreak = this.isBreak
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.FuncArgInfo.parser(): ExprInfo.FuncArg {
    return ExprInfo.FuncArg(
        FuncArgInfo(
            withInout = this.withInout,
            isDefaultVal = this.isDefaultVal
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.SubscriptInfo.parser(): ExprInfo.Subscript {
    return ExprInfo.Subscript(
        SubscriptInfo(
            isTupleAccess = this.isTupleAccess
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.MatchInfo.parser(): ExprInfo.Match {
    return ExprInfo.Match(
        MatchInfo(
            matchMode = this.matchMode
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.BlockInfo.parser(): ExprInfo.Block {
    return ExprInfo.Block(
        BlockInfo(
            isExpr = this.isExpr
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.BlockInfo.isExpr: List<Boolean>
    get() = (0 until this.isExprLength).mapNotNull { this.isExpr(it) }

fun org.cangnova.cangjie.metadata.PackageFormat.TryInfo.parser(): ExprInfo.Try {
    return ExprInfo.Try(
        TryInfo(
            resources = this.resources,
            patterns = this.patterns
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.TryInfo.resources: List<FullId>
    get() = (0 until this.resourcesLength).mapNotNull { this.resources(it)?.parser() }

val org.cangnova.cangjie.metadata.PackageFormat.TryInfo.patterns: List<Pattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

fun org.cangnova.cangjie.metadata.PackageFormat.LetPatternDestructorInfo.parser(): ExprInfo.LetPatternDestructor {
    return ExprInfo.LetPatternDestructor(
        LetPatternDestructorInfo(
            patterns = this.patterns
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.LetPatternDestructorInfo.patterns: List<Pattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

fun org.cangnova.cangjie.metadata.PackageFormat.ForInInfo.parser(): ExprInfo.ForIn {
    return ExprInfo.ForIn(
        ForInInfo(
            pattern = this.pattern?.parser() ?: Pattern(
                kind = PatternKind.InvalidPattern,
                begin = Position(0, 0, 0, 0, false),
                end = Position(0, 0, 0, 0, false)
            ),
            forInKind = ForInKind.fromUByte(this.forInKind)
        )
    )
}

fun org.cangnova.cangjie.metadata.PackageFormat.MatchCaseInfo.parser(): ExprInfo.MatchCase {
    return ExprInfo.MatchCase(
        MatchCaseInfo(
            patterns = this.patterns
        )
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.MatchCaseInfo.patterns: List<Pattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

fun org.cangnova.cangjie.metadata.PackageFormat.SpawnInfo.parser(): ExprInfo.Spawn {
    return ExprInfo.Spawn(
        SpawnInfo(
            future = this.future?.parser() ?: FullId(0, "", 0)
        )
    )
}

// 添加Pattern的parser方法
fun org.cangnova.cangjie.metadata.PackageFormat.Pattern.parser(): Pattern {
    return Pattern(
        kind = PatternKind.fromByte(this.kind),
        begin = this.begin?.parser() ?: Position(0, 0, 0, 0, false),
        end = this.end?.parser() ?: Position(0, 0, 0, 0, false),
        patterns = this.patterns,
        types = this.types,
        exprs = this.exprs,
        values = this.values,
        matchBeforeRuntime = this.matchBeforeRuntime,
        needRuntimeTypeCheck = this.needRuntimeTypeCheck
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.Pattern.patterns: List<Pattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

val org.cangnova.cangjie.metadata.PackageFormat.Pattern.types: List<Int>
    get() = (0 until this.typesLength).mapNotNull { this.types(it).toInt() }

val org.cangnova.cangjie.metadata.PackageFormat.Pattern.exprs: List<Int>
    get() = (0 until this.exprsLength).mapNotNull { this.exprs(it).toInt() }

val org.cangnova.cangjie.metadata.PackageFormat.Pattern.values: List<ConstValue>
    get() = (0 until this.valuesLength).mapNotNull {
        this.parserValue(it)
    }

// 添加FuncBody的parser方法
fun org.cangnova.cangjie.metadata.PackageFormat.FuncBody.parser(): FuncBody {
    return FuncBody(
        paramLists = this.paramLists,
        retType = this.retType.toInt(),
        body = this.body.toInt(),
        always = this.always,
        captureKind = this.captureKind
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.FuncBody.paramLists: List<FuncParamList>
    get() = (0 until this.paramListsLength).mapNotNull { this.paramLists(it)?.parser() }

// 添加FuncParamList的parser方法
fun org.cangnova.cangjie.metadata.PackageFormat.FuncParamList.parser(): FuncParamList {
    return FuncParamList(
        params = this.params,
        desugars = this.desugars
    )
}

val org.cangnova.cangjie.metadata.PackageFormat.FuncParamList.params: List<Int>
    get() = (0 until this.paramsLength).mapNotNull { this.params(it).toInt() }

val org.cangnova.cangjie.metadata.PackageFormat.FuncParamList.desugars: List<Int>
    get() = (0 until this.desugarsLength).mapNotNull { this.desugars(it).toInt() }

fun org.cangnova.cangjie.metadata.PackageFormat.MemberValue.parserValue(): ConstValue? {
    return when (this.valueType) {
        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int8Value -> {
            val int8Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int8Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int8Value
            ConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt8Value -> {
            val uint8Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt8Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt8Value
            ConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int16Value -> {
            val int16Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int16Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int16Value
            ConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt16Value -> {
            val uint16Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt16Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt16Value
            ConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int32Value -> {
            val int32Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int32Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int32Value
            ConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt32Value -> {
            val uint32Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt32Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt32Value
            ConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int64Value -> {
            val int64Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Int64Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Int64Value
            ConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt64Value -> {
            val uint64Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.UInt64Value()) as? org.cangnova.cangjie.metadata.PackageFormat.UInt64Value
            ConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float32Value -> {
            val float32Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Float32Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Float32Value
            ConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float64Value -> {
            val float64Value =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.Float64Value()) as? org.cangnova.cangjie.metadata.PackageFormat.Float64Value
            ConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.ArrayValue -> {
            val arrayValue =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.ArrayValue()) as? org.cangnova.cangjie.metadata.PackageFormat.ArrayValue
            ConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.StringValue -> {
            ConstValue.StringValue(this.value(this).toString())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.CompositeValue -> {
            val compositeValue =
                this.value(org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex()) as? org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex
            ConstValue.CompositeValue(CompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> ConstValue.None
    }

}

fun org.cangnova.cangjie.metadata.PackageFormat.Pattern.parserValue(index: Int): ConstValue? {
    return when (this.valuesType(index)) {
        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int8Value -> {
            val int8Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.Int8Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int8Value
            ConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt8Value -> {
            val uint8Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.UInt8Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt8Value
            ConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int16Value -> {
            val int16Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.Int16Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int16Value
            ConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt16Value -> {
            val uint16Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.UInt16Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt16Value
            ConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int32Value -> {
            val int32Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.Int32Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int32Value
            ConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt32Value -> {
            val uint32Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.UInt32Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt32Value
            ConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int64Value -> {
            val int64Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.Int64Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int64Value
            ConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt64Value -> {
            val uint64Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.UInt64Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt64Value
            ConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float32Value -> {
            val float32Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.Float32Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Float32Value
            ConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float64Value -> {
            val float64Value = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.Float64Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Float64Value
            ConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.ArrayValue -> {
            val arrayValue = this.values(
                org.cangnova.cangjie.metadata.PackageFormat.ArrayValue(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.ArrayValue
            ConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.StringValue -> {
            ConstValue.StringValue(this.values(this, index).toString())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.CompositeValue -> {
            val compositeValue =
                this.values(
                    org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex(),
                    index
                ) as? org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex
            ConstValue.CompositeValue(CompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> ConstValue.None
    }

}

fun org.cangnova.cangjie.metadata.PackageFormat.ArrayValue.parserValue(index: Int): ConstValue? {
    return when (this.valType(index)) {
        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int8Value -> {
            val int8Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.Int8Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int8Value
            ConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt8Value -> {
            val uint8Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.UInt8Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt8Value
            ConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int16Value -> {
            val int16Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.Int16Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int16Value
            ConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt16Value -> {
            val uint16Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.UInt16Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt16Value
            ConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int32Value -> {
            val int32Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.Int32Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int32Value
            ConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt32Value -> {
            val uint32Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.UInt32Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt32Value
            ConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Int64Value -> {
            val int64Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.Int64Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Int64Value
            ConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.UInt64Value -> {
            val uint64Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.UInt64Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.UInt64Value
            ConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float32Value -> {
            val float32Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.Float32Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Float32Value
            ConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.Float64Value -> {
            val float64Value = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.Float64Value(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.Float64Value
            ConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.ArrayValue -> {
            val arrayValue = this.val_(
                org.cangnova.cangjie.metadata.PackageFormat.ArrayValue(),
                index
            ) as? org.cangnova.cangjie.metadata.PackageFormat.ArrayValue
            ConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.StringValue -> {
            ConstValue.StringValue(this.val_(this, index).toString())
        }

        org.cangnova.cangjie.metadata.PackageFormat.ConstValue.CompositeValue -> {
            val compositeValue =
                this.val_(
                    org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex(),
                    index
                ) as? org.cangnova.cangjie.metadata.PackageFormat.CompositeValueIndex
            ConstValue.CompositeValue(CompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> ConstValue.None
    }

}

val org.cangnova.cangjie.metadata.PackageFormat.ArrayValue.vals: List<ConstValue>
    get() = (0 until this.val_Length).mapNotNull { parserValue(it) }