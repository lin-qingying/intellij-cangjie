package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.name.ClassId;
import com.linqingying.cangjie.psi.CjProjectionKind;
import com.linqingying.cangjie.psi.CjUserType;
import com.linqingying.cangjie.psi.psiUtil.StubUtils;
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub;
import com.linqingying.cangjie.psi.stubs.impl.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



    public class CjUserTypeElementType extends CjStubElementType<CangJieUserTypeStub, CjUserType> {
    public CjUserTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjUserType.class, CangJieUserTypeStub.class);
    }

    @NotNull
    @Override
    public CangJieUserTypeStub createStub(@NotNull CjUserType psi, StubElement parentStub) {
        return new CangJieUserTypeStubImpl((StubElement<?>) parentStub, null,null);
    }

    @Override
    public @NotNull CjUserType createPsi(@NotNull CangJieUserTypeStub stub) {
        return new  CjUserType(stub);
    }

    @Override
    public @NotNull CjUserType createPsiFromAst(@NotNull ASTNode node) {
        return new  CjUserType(node);
    }

    @Override
    public void serialize(@NotNull CangJieUserTypeStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        serializeType(dataStream, ((CangJieUserTypeStubImpl) stub).getUpperBound());
    }

    private enum CangJieTypeBeanKind {
        CLASS, TYPE_PARAMETER, FLEXIBLE, NONE;

        static CangJieTypeBeanKind fromBean(@Nullable CangJieTypeBean typeBean) {
            if (typeBean == null) return NONE;
            if (typeBean instanceof CangJieTypeParameterTypeBean) return TYPE_PARAMETER;
            if (typeBean instanceof CangJieClassTypeBean) return CLASS;
            return FLEXIBLE;
        }
    }

    public static void serializeType(@NotNull StubOutputStream dataStream, @Nullable CangJieTypeBean type) throws IOException {
        dataStream.writeInt(CangJieTypeBeanKind.fromBean(type).ordinal());
        if (type instanceof CangJieClassTypeBean) {
            StubUtils.serializeClassId(dataStream, ((CangJieClassTypeBean) type).getClassId());
            dataStream.writeBoolean(type.getNullable());
            List<CangJieTypeArgumentBean> arguments = ((CangJieClassTypeBean) type).getArguments();
            dataStream.writeInt(arguments.size());
            for (CangJieTypeArgumentBean argument : arguments) {
                CjProjectionKind kind = argument.getProjectionKind();
                dataStream.writeInt(kind.ordinal());

                    serializeType(dataStream, argument.getType());

            }
        }
        else if (type instanceof CangJieTypeParameterTypeBean) {
            dataStream.writeName(((CangJieTypeParameterTypeBean) type).getTypeParameterName());
            dataStream.writeBoolean(type.getNullable());
            dataStream.writeBoolean(((CangJieTypeParameterTypeBean) type).getDefinitelyNotNull());
        }
        else if (type instanceof CangJieFlexibleTypeBean) {
            serializeType(dataStream, ((CangJieFlexibleTypeBean) type).getLowerBound());
            serializeType(dataStream, ((CangJieFlexibleTypeBean) type).getUpperBound());
        }
    }

    @NotNull
    @Override
    public CangJieUserTypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new CangJieUserTypeStubImpl((StubElement<?>) parentStub, deserializeType(dataStream),null);
    }

    @Nullable
    public static CangJieTypeBean deserializeType(@NotNull StubInputStream dataStream) throws IOException {
        CangJieTypeBeanKind typeKind = CangJieTypeBeanKind.values()[dataStream.readInt()];
        switch (typeKind) {
            case CLASS: {
                ClassId classId = Objects.requireNonNull(StubUtils.deserializeClassId(dataStream));
                boolean isNullable = dataStream.readBoolean();
                int count = dataStream.readInt();
                List<CangJieTypeArgumentBean> arguments = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int kind = dataStream.readInt();
                    CangJieTypeArgumentBean argument;

                        argument = new CangJieTypeArgumentBean(CjProjectionKind.values()[kind], deserializeType(dataStream));

                    arguments.add(argument);
                }
                return new CangJieClassTypeBean(classId, arguments, isNullable,null);
            }
            case TYPE_PARAMETER: {
                String typeParameterName = Objects.requireNonNull(dataStream.readNameString());
                boolean nullable = dataStream.readBoolean();
                boolean definitelyNotNull = dataStream.readBoolean();
                return new CangJieTypeParameterTypeBean(typeParameterName, nullable, definitelyNotNull);
            }
            case FLEXIBLE: {
                return new CangJieFlexibleTypeBean(Objects.requireNonNull(deserializeType(dataStream)),
                        Objects.requireNonNull(deserializeType(dataStream)));
            }
            case NONE:
                return null;
        }
        return null;
    }
}
