package com.huawei.cangjie.resolve.lazy.data

import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.psi.CjClass
import com.huawei.cangjie.psi.CjTypeParameterList
import com.huawei.cangjie.psi.CjTypeStatement

class CjClassInfo<T : CjTypeStatement>(
    element: T,
    override val classKind: ClassKind = ClassKind.CLASS
) : CjTypeStatementInfo<T>(element) {
    override val typeParameterList: CjTypeParameterList?
        get() = element.typeParameterList


}
//public class CjClassInfo extends CjClassOrObjectInfo<CjClass> {
//    private final ClassKind kind;
//
//    protected CjClassInfo(@NotNull CjClass classOrObject) {
//        super(classOrObject);
//        if (element instanceof CjEnumEntry) {
//            this.kind = ClassKind.ENUM_ENTRY;
//        }
//        else if (element.isInterface()) {
//            this.kind = ClassKind.INTERFACE;
//        }
//        else if (element.isEnum()) {
//            this.kind = ClassKind.ENUM_CLASS;
//        }
//        else if (element.isAnnotation()) {
//            this.kind = ClassKind.ANNOTATION_CLASS;
//        }
//        else {
//            this.kind = ClassKind.CLASS;
//        }
//    }
//
//    @Nullable
//    @Override
//    public CjTypeParameterList getTypeParameterList() {
//        return element.getTypeParameterList();
//    }
//
//    @NotNull
//    @Override
//    public ClassKind getClassKind() {
//        return kind;
//    }
//}
