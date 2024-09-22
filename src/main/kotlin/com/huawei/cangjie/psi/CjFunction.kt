package com.huawei.cangjie.psi;


public interface CjFunction extends CjDeclarationWithBody, CjCallableDeclaration {
    boolean isLocal();
    default boolean isStatic(){
        return false;
    }
    default boolean isOperator() {
        return false;
    }
}

