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

package org.cangnova.cangjie.metadata.model.fb.parser


import com.intellij.util.text.SemVer
import org.cangnova.cangjie.metadata.PackageFormat.*
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.model.fb.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

fun ByteBuffer.toFbPackage(): FbPackage {

    val packageFb = Package.getRootAsPackage(this)
    return packageFb.parser()


}

fun InputStream.toFbPackage(): FbPackage {
    return ByteBuffer.wrap(this.readBytes()).toFbPackage()
}

fun ByteArrayInputStream.toFbPackage(): FbPackage {

    return ByteBuffer.wrap(this.readAllBytes()).toFbPackage()
}

fun Package.getFullPkgName(): String {
    return fullPkgName ?: ""

}

fun Package.getpkgDepInfo(): String {
    return try {
        return this.pkgDepInfo ?: ""
    } catch (e: Exception) {
        ""
    }
}

fun Package.parser(): FbPackage {
//由于Package最低支持1.0.0 ，所以少于1.0.0的不解析

    val version = SemVer.parseFromText(this.version ?: "0.0.0")


    if (version == null || version < SemVer.parseFromText("1.0.0")) {
        return FbPackage(
            cjcVersion = "0.0.0",
            cjoVersion = BuiltInsBinaryVersion.INSTANCE,
            fullPkgName = "",
            pkgDepInfo = "",
            imports = emptyList(),
            files = emptyList(),
            fileImports = emptyList(),
            types = emptyList(),
            exprs = emptyList(),
            decls = emptyList(),
            values = emptyList(),
            moduleName = ""
        )
    }

    return FbPackage(
        cjcVersion = this.version ?: "0.0.0",
        cjoVersion = this.cjoVersion?.toBinaryVersion() ?: BuiltInsBinaryVersion.INSTANCE,
        fullPkgName = this.getFullPkgName(),
        pkgDepInfo = this.getpkgDepInfo(),
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

val Package.fileImports: List<FbImports>
    get() = (0 until this.allFileImportsLength).mapNotNull { this.allFileImports(it)?.parser() }

fun Imports.parser(): FbImports {
    return FbImports(
        importSpecs = this.importSpecs
    )
}

val Imports.importSpecs: List<FbImportSpec>
    get() = (0 until this.importSpecsLength).mapNotNull { this.importSpecs(it)?.parser() }

fun ImportSpec.parser(): FbImportSpec {
    return FbImportSpec(
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        prefixPaths = this.prefixPaths,
        identifier = this.identifier ?: "",
        asIdentifier = this.asIdentifier ?: "",
        reExport = FbAccessModifier.fromUByte(this.reExport)

    )
}

val ImportSpec.prefixPaths: List<String>
    get() = (0 until this.prefixPathsLength).mapNotNull { this.prefixPaths(it) }

fun Position.parser(): FbPosition {
    return FbPosition(
        file = this.file.toInt(),
        line = this.line,
        column = this.column,
        pkgId = this.pkgId.toInt(),
        ignore = this.ignore,

        )
}

val Expr.operands: List<UInt>
    get() {
        return (0 until this.operandsLength).mapNotNull { this.operands(it) }
    }

fun Expr.parser(): FbExpr {
    return FbExpr(
        kind = FbExprKind.fromUShort(this.kind),
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        mapExpr = this.mapExpr,
        operands = this.operands,
        type = this.type,
        overflowPolicy = FbOverflowPolicy.fromUByte(this.overflowPolicy),
        info = this.info
    )
}

fun Decl.parser(): FbDecl {
    return FbDecl(
        kind = FbDeclKind.fromUShort(this.kind),
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

val Decl.info: FbDeclInfo
    get() {
        return when (this.infoType) {
            DeclInfo.ClassInfo -> (this.info(ClassInfo()) as? ClassInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.InterfaceInfo -> (this.info(InterfaceInfo()) as? InterfaceInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.StructInfo -> (this.info(StructInfo()) as? StructInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.EnumInfo -> (this.info(EnumInfo()) as? EnumInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.ExtendInfo -> (this.info(ExtendInfo()) as? ExtendInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.PropInfo -> (this.info(PropInfo()) as? PropInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.VarInfo -> (this.info(VarInfo()) as? VarInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.VarWithPatternInfo -> (this.info(VarWithPatternInfo()) as? VarWithPatternInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.ParamInfo -> (this.info(ParamInfo()) as? ParamInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.FuncInfo -> (this.info(FuncInfo()) as? FuncInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.BuiltInInfo -> (this.info(BuiltInInfo()) as? BuiltInInfo)?.parser()
                ?: FbDeclInfo.None

            DeclInfo.AliasInfo -> (this.info(AliasInfo()) as? AliasInfo)?.parser()
                ?: FbDeclInfo.None

            else -> FbDeclInfo.None
        }
    }


fun ClassInfo.parser(): FbDeclInfo {
    return FbDeclInfo.ClassInfo(
        inheritedTypes = this.inheritedTypes,
        body = this.bodys,
        adInfo = this.adInfo?.parser(),
        isAnno = this.isAnno,
        annoTargets = this.annoTargets,
        runtimeVisible = this.runtimeVisible,
        annoTargets2 = this.annoTargets2
    )

}

fun InterfaceInfo.parser(): FbDeclInfo.InterfaceInfo {
    return FbDeclInfo.InterfaceInfo(

        inheritedTypes = this.inheritedTypes,
        body = this.bodys

    )
}

fun StructInfo.parser(): FbDeclInfo.StructInfo {
    return FbDeclInfo.StructInfo(

        inheritedTypes = this.inheritedTypes,
        body = this.bodys,
        adInfo = this.adInfo?.parser() ?: FbAutoDiffInfo(false, false, "", emptyList(), emptyList(), 0u)
    )

}

fun EnumInfo.parser(): FbDeclInfo.EnumInfo {
    return FbDeclInfo.EnumInfo(
        inheritedTypes = this.inheritedTypes,
        body = this.bodys,
        adInfo = this.adInfo?.parser() ?: FbAutoDiffInfo(false, false, "", emptyList(), emptyList(), 0u),
        hasArguments = this.hasArguments,
        nonExhaustive = this.nonExhaustive,
        ellipsisPos = this.ellipsisPos?.parser() ?: FbPosition(0, 0, 0, 0, false)
    )

}

fun ExtendInfo.parser(): FbDeclInfo.ExtendInfo {
    return FbDeclInfo.ExtendInfo(
        inheritedTypes = this.inheritedTypes,
        body = this.bodys
    )

}

fun PropInfo.parser(): FbDeclInfo.PropInfo {
    return FbDeclInfo.PropInfo(
        isConst = this.isConst,
        isMutable = this.isMutable,
        setters = this.setters,
        getters = this.getters

    )
}

val PropInfo.setters: List<UInt>
    get() {
        return (0 until this.settersLength).mapNotNull { this.setters(it) }
    }
val PropInfo.getters: List<UInt>
    get() {
        return (0 until this.gettersLength).mapNotNull { this.getters(it) }
    }

fun VarInfo.parser(): FbDeclInfo.VarInfo {
    return FbDeclInfo.VarInfo(
        isVar = this.isVar,
        isConst = this.isConst,
        isMemberParam = this.isMemberParam,
        initializer = this.initializer.toInt(),
        value = this.parserValue()

    )
}

fun VarInfo.parserValue(): FbConstValue {
    return when (this.valueType) {
        ConstValue.Int8Value -> {
            val int8Value =
                this.value(Int8Value()) as? Int8Value
            FbConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        ConstValue.UInt8Value -> {
            val uint8Value =
                this.value(UInt8Value()) as? UInt8Value
            FbConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        ConstValue.Int16Value -> {
            val int16Value =
                this.value(Int16Value()) as? Int16Value
            FbConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        ConstValue.UInt16Value -> {
            val uint16Value =
                this.value(UInt16Value()) as? UInt16Value
            FbConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        ConstValue.Int32Value -> {
            val int32Value =
                this.value(Int32Value()) as? Int32Value
            FbConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        ConstValue.UInt32Value -> {
            val uint32Value =
                this.value(UInt32Value()) as? UInt32Value
            FbConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        ConstValue.Int64Value -> {
            val int64Value =
                this.value(Int64Value()) as? Int64Value
            FbConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        ConstValue.UInt64Value -> {
            val uint64Value =
                this.value(UInt64Value()) as? UInt64Value
            FbConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        ConstValue.Float32Value -> {
            val float32Value =
                this.value(Float32Value()) as? Float32Value
            FbConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        ConstValue.Float64Value -> {
            val float64Value =
                this.value(Float64Value()) as? Float64Value
            FbConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        ConstValue.ArrayValue -> {
            val arrayValue =
                this.value(ArrayValue()) as? ArrayValue
            FbConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        ConstValue.StringValue -> {
            FbConstValue.StringValue(this.value(this).toString())
        }

        ConstValue.CompositeValue -> {
            val compositeValue =
                this.value(CompositeValueIndex()) as? CompositeValueIndex
            FbConstValue.FbCompositeValue(FbCompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> FbConstValue.None
    }
}

fun VarWithPatternInfo.parser(): FbDeclInfo.VarWithPatternInfo {
    return FbDeclInfo.VarWithPatternInfo(

        isVar = this.isVar,
        isConst = this.isConst,
        irrefutablePattern = this.irrefutablePattern?.parser() ?: FbPattern(
            kind = FbPatternKind.InvalidPattern,
            begin = FbPosition(0, 0, 0, 0, false),
            end = FbPosition(0, 0, 0, 0, false)
        ),
        initializer = this.initializer.toInt()

    )
}

fun ParamInfo.parser(): FbDeclInfo.ParamInfo {
    return FbDeclInfo.ParamInfo(
        isNamedParam = this.isNamedParam,
        isMemberParam = this.isMemberParam,
        defaultVal = this.defaultVal.toInt()

    )
}

fun FuncInfo.parser(): FbDeclInfo.FuncInfo {
    return FbDeclInfo.FuncInfo(

        funcBody = this.funcBody?.parser() ?: FbFuncBody(emptyList(), 0, 0, false, 0u),
        overflowPolicy = FbOverflowPolicy.fromUByte(this.overflowPolicy),
        op = FbOperatorKind.fromUByte(this.op),
        adInfo = this.adInfo?.parser() ?: FbAutoDiffInfo(false, false, "", emptyList(), emptyList(), 0u),
        isConst = this.isConst,
        isInline = this.isInline,
        isFastNative = this.isFastNative

    )
}

fun BuiltInInfo.parser(): FbDeclInfo.BuiltInInfo {
    return FbDeclInfo.BuiltInInfo(

        builtInType = FbBuiltInType.fromUByte(this.builtInType)
    )

}

fun AliasInfo.parser(): FbDeclInfo.AliasInfo {
    return FbDeclInfo.AliasInfo(

        aliasedTy = this.aliasedTy.toInt()

    )
}

fun AutoDiffInfo.parser(): FbAutoDiffInfo {
    return FbAutoDiffInfo(
        isDiff = this.isDiff,
        isAdj = this.isAdj,
        primal = this.primal,
        excepts = this.excepts,
        includes = this.includes,
        stage = this.stage
    )
}

val AutoDiffInfo.includes: List<String>
    get() {
        return (0 until this.includesLength).mapNotNull { this.includes(it) }
    }
val AutoDiffInfo.excepts: List<String>
    get() {
        return (0 until this.exceptsLength).mapNotNull { this.excepts(it) }
    }
val InterfaceInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val StructInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val ClassInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val StructInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val InterfaceInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val ClassInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val EnumInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val EnumInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }
val ExtendInfo.bodys: List<Int>
    get() {
        return (0 until this.bodyLength).mapNotNull { this.body(it).toInt() }
    }
val ExtendInfo.inheritedTypes: List<Int>
    get() {
        return (0 until this.inheritedTypesLength).mapNotNull { this.inheritedTypes(it).toInt() }
    }

fun DeclHash.parser(): FbDeclHash {
    return FbDeclHash(
        instVar = this.instVar,
        virt = this.virt,
        sig = this.sig,
        srcUse = this.srcUse,
        bodyHash = this.bodyHash
    )
}

fun Anno.parser(): FbAnno {
    return FbAnno(
        kind = FbAnnoKind.fromUShort(this.kind),
        identifier = this.identifier,
        args = this.args
    )
}

val Anno.args: List<FbAnnoArg>
    get() {
        return (0 until this.argsLength).mapNotNull { this.args(it)?.parser() }
    }

fun AnnoArg.parser(): FbAnnoArg {
    return FbAnnoArg(
        name = this.name ?: "",
        expr = this.expr
    )
}

val Decl.annotations: List<FbAnno>
    get() = (0 until this.annotationsLength).mapNotNull { this.annotations(it)?.parser() }
val Decl.attributes: List<ULong>
    get() = (0 until this.attributesLength).mapNotNull { this.attributes(it) }

fun SemaTy.parser(): FbSemaTy {
    return FbSemaTy(
        kind = FbTypeKind.fromUShort(this.kind),
        typeArgs = this.typeArgs,
        info = this.info
    )
}

val SemaTy.info: FbSemaTyInfo
    get() {
        val a = this.info(FuncTyInfo())
        return when (this.infoType) {
            SemaTyInfo.FuncTyInfo -> (this.info(FuncTyInfo()) as? FuncTyInfo)?.parser()
                ?: FbSemaTyInfo.None

            SemaTyInfo.CompositeTyInfo -> (this.info(CompositeTyInfo()) as? CompositeTyInfo)
                ?.parser() ?: FbSemaTyInfo.None

            SemaTyInfo.GenericTyInfo -> (this.info(GenericTyInfo()) as? GenericTyInfo)?.parser()
                ?: FbSemaTyInfo.None

            SemaTyInfo.ArrayTyInfo -> (this.info(ArrayTyInfo()) as? ArrayTyInfo)?.parser()
                ?: FbSemaTyInfo.None


            else -> FbSemaTyInfo.None
        }

    }

val Expr.info: FbExprInfo
    get() {


        return when (this.infoType) {
            ExprInfo.CallInfo -> (this.info(CallInfo()) as? CallInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.UnaryInfo -> (this.info(UnaryInfo()) as? UnaryInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.BinaryInfo -> (this.info(BinaryInfo()) as? BinaryInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.IncOrDecInfo -> (this.info(IncOrDecInfo()) as? IncOrDecInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.LitConstInfo -> (this.info(LitConstInfo()) as? LitConstInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.ReferenceInfo -> (this.info(ReferenceInfo()) as? ReferenceInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.LambdaInfo -> (this.info(LambdaInfo()) as? LambdaInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.AssignInfo -> (this.info(AssignInfo()) as? AssignInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.ArrayInfo -> (this.info(ArrayInfo()) as? ArrayInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.JumpInfo -> (this.info(JumpInfo()) as? JumpInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.FuncArgInfo -> (this.info(FuncArgInfo()) as? FuncArgInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.SubscriptInfo -> (this.info(SubscriptInfo()) as? SubscriptInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.MatchInfo -> (this.info(MatchInfo()) as? MatchInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.BlockInfo -> (this.info(BlockInfo()) as? BlockInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.TryInfo -> (this.info(TryInfo()) as? TryInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.LetPatternDestructorInfo -> (this.info(LetPatternDestructorInfo()) as? LetPatternDestructorInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.ForInInfo -> (this.info(ForInInfo()) as? ForInInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.MatchCaseInfo -> (this.info(MatchCaseInfo()) as? MatchCaseInfo)?.parser()
                ?: FbExprInfo.None

            ExprInfo.SpawnInfo -> (this.info(SpawnInfo()) as? SpawnInfo)?.parser()
                ?: FbExprInfo.None

            else -> FbExprInfo.None
        }


    }

fun CallInfo.parser(): FbExprInfo.Call {
    return FbExprInfo.Call(
        FbCallInfo(
            hasSideEffect = this.hasSideEffect,
            callKind = FbCallKind.fromUByte(this.callKind),
        )
    )
}


fun ArrayTyInfo.parser(): FbSemaTyInfo.FbArrayTyInfo {
    return FbSemaTyInfo.FbArrayTyInfo(

        dimsOrSize = this.dimsOrSize


    )
}

fun GenericTyInfo.parser(): FbSemaTyInfo.FbGenericTyInfo {
    return FbSemaTyInfo.FbGenericTyInfo(

        declPtr = this.declPtr?.parser(),
        upperBounds = this.upperBounds

    )

}

val GenericTyInfo.upperBounds: List<Int>
    get() {
        return (0 until this.upperBoundsLength).mapNotNull { this.upperBounds(it).toInt() }
    }

fun FuncTyInfo.parser(): FbSemaTyInfo.FbFuncTyInfo {
    return FbSemaTyInfo.FbFuncTyInfo(

        retType = this.retType.toInt(),
        isC = this.isC,
        hasVariableLenArg = this.hasVariableLenArg,

        )
}

fun Generic.parser(): FbGeneric {
    return FbGeneric(
        typeParameters = this.typeParameters,
        constraints = this.constraints
    )
}

fun Constraint.parser(): FbConstraint {
    return FbConstraint(
        begin = this.begin?.parser(),
        end = this.end?.parser(),
        type = this.type,
        uppers = this.uppers
    )
}

val Constraint.uppers: List<Int>
    get() {

        return (0 until this.uppersLength).mapNotNull { this.uppers(it).toInt() }
    }
val Generic.constraints: List<FbConstraint>
    get() {
        return (0 until this.constraintsLength).mapNotNull { this.constraints(it)?.parser() }
    }
val Generic.typeParameters: List<Int>
    get() = (0 until this.typeParametersLength).mapNotNull { this.typeParameters(it).toInt() }

fun FullId.parser(): FbFullId {
    return FbFullId(
        pkgId = this.pkgId,
        decl = try {
            this.decl ?: ""
        } catch (e: IndexOutOfBoundsException) {
            ""
        },
        index = this.index.toInt()
    )
}

fun CompositeTyInfo.parser(): FbSemaTyInfo.FbCompositeTyInfo {
    return FbSemaTyInfo.FbCompositeTyInfo(

        declPtr = this.declPtr?.parser(),
        isThisTy = this.isThisTy

    )
}

val Package.decls: List<FbDecl>
    get() = (0 until this.allDeclsLength).mapNotNull { this.allDecls(it)?.parser() }

val SemaTy.typeArgs: List<Int>
    get() = (0 until this.typeArgsLength).mapNotNull { this.typeArgs(it).toInt() }

val Package.types: List<FbSemaTy>
    get() = (0 until this.allTypesLength).mapNotNull { this.allTypes(it)?.parser() }
val Package.exprs: List<FbExpr>
    get() = (0 until this.allExprsLength).mapNotNull { this.allExprs(it)?.parser() }
val Package.values: List<FbCompositeValue>
    get() = (0 until this.allValuesLength).mapNotNull {


        this.allValues(it)?.parser()

    }
val CompositeValue.fields: List<FbMemberValue>
    get() {
        return (0 until this.fieldsLength).mapNotNull { this.fields(it)?.parser() }


    }

fun MemberValue.parser(): FbMemberValue {
    return FbMemberValue(
        field = this.field ?: "",
        type = this.type,
        value = this.parserValue()
    )
}

fun CompositeValue.parser(): FbCompositeValue {
    return FbCompositeValue(
        type = this.type,
        fields = this.fields
    )
}

val Package.files: List<String>
    get() {
        return try {
            (0 until this.allFilesLength).mapNotNull { this.allFiles(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

val Package.imports: List<String>
    get() = (0 until this.importsLength).mapNotNull { this.imports(it) }

fun CjoVersion.toBinaryVersion(): BuiltInsBinaryVersion {
    return BuiltInsBinaryVersion(
        this.majorNum.toInt(),
        this.minorNum.toInt(),
        this.patchNum.toInt(),

        )
}


fun UnaryInfo.parser(): FbExprInfo.Unary {
    return FbExprInfo.Unary(
        FbUnaryInfo(
            op = FbOperatorKind.fromUByte(this.op)
        )
    )
}

fun BinaryInfo.parser(): FbExprInfo.Binary {
    return FbExprInfo.Binary(
        FbBinaryInfo(
            op = FbOperatorKind.fromUByte(this.op)
        )
    )
}

fun IncOrDecInfo.parser(): FbExprInfo.IncOrDec {
    return FbExprInfo.IncOrDec(
        FbIncOrDecInfo(
            op = FbOperatorKind.fromUByte(this.op)
        )
    )
}

fun LitConstInfo.parser(): FbExprInfo.LitConst {
    return FbExprInfo.LitConst(
        FbLitConstInfo(
            strValue = this.strValue ?: "",
            constKind = FbLitConstKind.fromUByte(this.constKind),
            strKind = FbStringKind.fromUByte(this.strKind)
        )
    )
}

fun ReferenceInfo.parser(): FbExprInfo.Reference {
    return FbExprInfo.Reference(
        FbReferenceInfo(
            reference = this.reference ?: "",
            target = this.target?.parser() ?: FbFullId(0, "", 0),
            instTys = this.instTys,
            matchedParentTy = this.matchedParentTy
        )
    )
}

val ReferenceInfo.instTys: List<UInt>
    get() = (0 until this.instTysLength).mapNotNull { this.instTys(it) }

fun LambdaInfo.parser(): FbExprInfo.Lambda {
    return FbExprInfo.Lambda(
        FbLambdaInfo(
            funcBody = this.funcBody.parser(),
            supportMock = this.supportMock
        )
    )
}

fun AssignInfo.parser(): FbExprInfo.Assign {
    return FbExprInfo.Assign(
        FbAssignInfo(
            isCompound = this.isCompound,
            op = FbOperatorKind.fromUByte(this.op)
        )
    )
}

fun ArrayInfo.parser(): FbExprInfo.Array {
    return FbExprInfo.Array(
        FbArrayInfo(
            initFunc = this.initFunc?.parser() ?: FbFullId(0, "", 0),
            isValueArray = this.isValueArray
        )
    )
}

fun JumpInfo.parser(): FbExprInfo.Jump {
    return FbExprInfo.Jump(
        FbJumpInfo(
            isBreak = this.isBreak
        )
    )
}

fun FuncArgInfo.parser(): FbExprInfo.FuncArg {
    return FbExprInfo.FuncArg(
        FbFuncArgInfo(
            withInout = this.withInout,
            isDefaultVal = this.isDefaultVal
        )
    )
}

fun SubscriptInfo.parser(): FbExprInfo.Subscript {
    return FbExprInfo.Subscript(
        FbSubscriptInfo(
            isTupleAccess = this.isTupleAccess
        )
    )
}

fun MatchInfo.parser(): FbExprInfo.Match {
    return FbExprInfo.Match(
        FbMatchInfo(
            matchMode = this.matchMode
        )
    )
}

fun BlockInfo.parser(): FbExprInfo.Block {
    return FbExprInfo.Block(
        FbBlockInfo(
            isExpr = this.isExpr
        )
    )
}

val BlockInfo.isExpr: List<Boolean>
    get() = (0 until this.isExprLength).mapNotNull { this.isExpr(it) }

fun TryInfo.parser(): FbExprInfo.Try {
    return FbExprInfo.Try(
        FbTryInfo(
            resources = this.resources,
            patterns = this.patterns
        )
    )
}

val TryInfo.resources: List<FbFullId>
    get() = (0 until this.resourcesLength).mapNotNull { this.resources(it)?.parser() }

val TryInfo.patterns: List<FbPattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

fun LetPatternDestructorInfo.parser(): FbExprInfo.LetPatternDestructor {
    return FbExprInfo.LetPatternDestructor(
        FbLetPatternDestructorInfo(
            patterns = this.patterns
        )
    )
}

val LetPatternDestructorInfo.patterns: List<FbPattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

fun ForInInfo.parser(): FbExprInfo.ForIn {
    return FbExprInfo.ForIn(
        FbForInInfo(
            pattern = this.pattern?.parser() ?: FbPattern(
                kind = FbPatternKind.InvalidPattern,
                begin = FbPosition(0, 0, 0, 0, false),
                end = FbPosition(0, 0, 0, 0, false)
            ),
            forInKind = FbForInKind.fromUByte(this.forInKind)
        )
    )
}

fun MatchCaseInfo.parser(): FbExprInfo.MatchCase {
    return FbExprInfo.MatchCase(
        FbMatchCaseInfo(
            patterns = this.patterns
        )
    )
}

val MatchCaseInfo.patterns: List<FbPattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

fun SpawnInfo.parser(): FbExprInfo.Spawn {
    return FbExprInfo.Spawn(
        FbSpawnInfo(
            future = this.future?.parser() ?: FbFullId(0, "", 0)
        )
    )
}

// 添加Pattern的parser方法
fun Pattern.parser(): FbPattern {
    return FbPattern(
        kind = FbPatternKind.fromByte(this.kind),
        begin = this.begin?.parser() ?: FbPosition(0, 0, 0, 0, false),
        end = this.end?.parser() ?: FbPosition(0, 0, 0, 0, false),
        patterns = this.patterns,
        types = this.types,
        exprs = this.exprs,
        values = this.values,
        matchBeforeRuntime = this.matchBeforeRuntime,
        needRuntimeTypeCheck = this.needRuntimeTypeCheck
    )
}

val Pattern.patterns: List<FbPattern>
    get() = (0 until this.patternsLength).mapNotNull { this.patterns(it)?.parser() }

val Pattern.types: List<Int>
    get() = (0 until this.typesLength).mapNotNull { this.types(it).toInt() }

val Pattern.exprs: List<Int>
    get() = (0 until this.exprsLength).mapNotNull { this.exprs(it).toInt() }

val Pattern.values: List<FbConstValue>
    get() = (0 until this.valuesLength).mapNotNull {
        this.parserValue(it)
    }

// 添加FuncBody的parser方法
fun FuncBody.parser(): FbFuncBody {
    return FbFuncBody(
        paramLists = this.paramLists,
        retType = this.retType.toInt(),
        body = this.body.toInt(),
        always = this.always,
        captureKind = this.captureKind
    )
}

val FuncBody.paramLists: List<FbFuncParamList>
    get() = (0 until this.paramListsLength).mapNotNull { this.paramLists(it)?.parser() }

// 添加FuncParamList的parser方法
fun FuncParamList.parser(): FbFuncParamList {
    return FbFuncParamList(
        params = this.params,
        desugars = this.desugars
    )
}

val FuncParamList.params: List<Int>
    get() = (0 until this.paramsLength).mapNotNull { this.params(it).toInt() }

val FuncParamList.desugars: List<Int>
    get() = (0 until this.desugarsLength).mapNotNull { this.desugars(it).toInt() }

fun MemberValue.parserValue(): FbConstValue? {
    return when (this.valueType) {
        ConstValue.Int8Value -> {
            val int8Value =
                this.value(Int8Value()) as? Int8Value
            FbConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        ConstValue.UInt8Value -> {
            val uint8Value =
                this.value(UInt8Value()) as? UInt8Value
            FbConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        ConstValue.Int16Value -> {
            val int16Value =
                this.value(Int16Value()) as? Int16Value
            FbConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        ConstValue.UInt16Value -> {
            val uint16Value =
                this.value(UInt16Value()) as? UInt16Value
            FbConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        ConstValue.Int32Value -> {
            val int32Value =
                this.value(Int32Value()) as? Int32Value
            FbConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        ConstValue.UInt32Value -> {
            val uint32Value =
                this.value(UInt32Value()) as? UInt32Value
            FbConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        ConstValue.Int64Value -> {
            val int64Value =
                this.value(Int64Value()) as? Int64Value
            FbConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        ConstValue.UInt64Value -> {
            val uint64Value =
                this.value(UInt64Value()) as? UInt64Value
            FbConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        ConstValue.Float32Value -> {
            val float32Value =
                this.value(Float32Value()) as? Float32Value
            FbConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        ConstValue.Float64Value -> {
            val float64Value =
                this.value(Float64Value()) as? Float64Value
            FbConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        ConstValue.ArrayValue -> {
            val arrayValue =
                this.value(ArrayValue()) as? ArrayValue
            FbConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        ConstValue.StringValue -> {
            FbConstValue.StringValue(this.value(this).toString())
        }

        ConstValue.CompositeValue -> {
            val compositeValue =
                this.value(CompositeValueIndex()) as? CompositeValueIndex
            FbConstValue.FbCompositeValue(FbCompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> FbConstValue.None
    }

}

fun Pattern.parserValue(index: Int): FbConstValue? {
    return when (this.valuesType(index)) {
        ConstValue.Int8Value -> {
            val int8Value = this.values(
                Int8Value(),
                index
            ) as? Int8Value
            FbConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        ConstValue.UInt8Value -> {
            val uint8Value = this.values(
                UInt8Value(),
                index
            ) as? UInt8Value
            FbConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        ConstValue.Int16Value -> {
            val int16Value = this.values(
                Int16Value(),
                index
            ) as? Int16Value
            FbConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        ConstValue.UInt16Value -> {
            val uint16Value = this.values(
                UInt16Value(),
                index
            ) as? UInt16Value
            FbConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        ConstValue.Int32Value -> {
            val int32Value = this.values(
                Int32Value(),
                index
            ) as? Int32Value
            FbConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        ConstValue.UInt32Value -> {
            val uint32Value = this.values(
                UInt32Value(),
                index
            ) as? UInt32Value
            FbConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        ConstValue.Int64Value -> {
            val int64Value = this.values(
                Int64Value(),
                index
            ) as? Int64Value
            FbConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        ConstValue.UInt64Value -> {
            val uint64Value = this.values(
                UInt64Value(),
                index
            ) as? UInt64Value
            FbConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        ConstValue.Float32Value -> {
            val float32Value = this.values(
                Float32Value(),
                index
            ) as? Float32Value
            FbConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        ConstValue.Float64Value -> {
            val float64Value = this.values(
                Float64Value(),
                index
            ) as? Float64Value
            FbConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        ConstValue.ArrayValue -> {
            val arrayValue = this.values(
                ArrayValue(),
                index
            ) as? ArrayValue
            FbConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        ConstValue.StringValue -> {
            FbConstValue.StringValue(this.values(this, index).toString())
        }

        ConstValue.CompositeValue -> {
            val compositeValue =
                this.values(
                    CompositeValueIndex(),
                    index
                ) as? CompositeValueIndex
            FbConstValue.FbCompositeValue(FbCompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> FbConstValue.None
    }

}

fun ArrayValue.parserValue(index: Int): FbConstValue? {
    return when (this.valType(index)) {
        ConstValue.Int8Value -> {
            val int8Value = this.val_(
                Int8Value(),
                index
            ) as? Int8Value
            FbConstValue.Int8Value(int8Value?.val_ ?: 0)
        }

        ConstValue.UInt8Value -> {
            val uint8Value = this.val_(
                UInt8Value(),
                index
            ) as? UInt8Value
            FbConstValue.UInt8Value(uint8Value?.val_ ?: 0u)
        }

        ConstValue.Int16Value -> {
            val int16Value = this.val_(
                Int16Value(),
                index
            ) as? Int16Value
            FbConstValue.Int16Value(int16Value?.val_ ?: 0)
        }

        ConstValue.UInt16Value -> {
            val uint16Value = this.val_(
                UInt16Value(),
                index
            ) as? UInt16Value
            FbConstValue.UInt16Value(uint16Value?.val_ ?: 0u)
        }

        ConstValue.Int32Value -> {
            val int32Value = this.val_(
                Int32Value(),
                index
            ) as? Int32Value
            FbConstValue.Int32Value(int32Value?.val_ ?: 0)
        }

        ConstValue.UInt32Value -> {
            val uint32Value = this.val_(
                UInt32Value(),
                index
            ) as? UInt32Value
            FbConstValue.UInt32Value(uint32Value?.val_ ?: 0u)
        }

        ConstValue.Int64Value -> {
            val int64Value = this.val_(
                Int64Value(),
                index
            ) as? Int64Value
            FbConstValue.Int64Value(int64Value?.val_ ?: 0L)
        }

        ConstValue.UInt64Value -> {
            val uint64Value = this.val_(
                UInt64Value(),
                index
            ) as? UInt64Value
            FbConstValue.UInt64Value(uint64Value?.val_ ?: 0uL)
        }

        ConstValue.Float32Value -> {
            val float32Value = this.val_(
                Float32Value(),
                index
            ) as? Float32Value
            FbConstValue.Float32Value(float32Value?.val_ ?: 0.0f)
        }

        ConstValue.Float64Value -> {
            val float64Value = this.val_(
                Float64Value(),
                index
            ) as? Float64Value
            FbConstValue.Float64Value(float64Value?.val_ ?: 0.0)
        }

        ConstValue.ArrayValue -> {
            val arrayValue = this.val_(
                ArrayValue(),
                index
            ) as? ArrayValue
            FbConstValue.ArrayValue(arrayValue?.vals ?: emptyList())
        }

        ConstValue.StringValue -> {
            FbConstValue.StringValue(this.val_(this, index).toString())
        }

        ConstValue.CompositeValue -> {
            val compositeValue =
                this.val_(
                    CompositeValueIndex(),
                    index
                ) as? CompositeValueIndex
            FbConstValue.FbCompositeValue(FbCompositeValueIndex(compositeValue?.idx ?: 0u))
        }

        else -> FbConstValue.None
    }

}

val ArrayValue.vals: List<FbConstValue>
    get() = (0 until this.val_Length).mapNotNull { parserValue(it) }