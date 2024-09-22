package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode


abstract class CjMatchCasePattern(node:ASTNode): CjExpressionImpl(node)
class CjBindingPattern(node:ASTNode): CjMatchCasePattern(node)

class CjTypePattern(node:ASTNode ): CjMatchCasePattern(node)
class CjTuplePattern(node:ASTNode): CjMatchCasePattern(node)

class CjEnumPattern(node:ASTNode): CjMatchCasePattern(node)
class CjWildcardPattern(node:ASTNode): CjMatchCasePattern(node)

class CjConstantPattern(node:ASTNode): CjMatchCasePattern(node)
