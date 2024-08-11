package com.huawei.cangjie.resolve.constants;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.psi.CjConstantExpression;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.types.CangJieType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * 常量检查
 */
public class CompileTimeConstantChecker {
    private final ResolutionContext<?> context;
    private final ModuleDescriptor module;
    private final CangJieBuiltIns builtIns;
    private final boolean checkOnlyErrorsThatDependOnExpectedType;
    private final BindingTrace trace;
    public CompileTimeConstantChecker(
            @NotNull ResolutionContext<?> context,
            @NotNull ModuleDescriptor module,
            boolean checkOnlyErrorsThatDependOnExpectedType
    ) {
        this.context = context;
        this.module = module;
        this.builtIns = module.getBuiltIns();
        this.checkOnlyErrorsThatDependOnExpectedType = checkOnlyErrorsThatDependOnExpectedType;
        this.trace = context.trace;
    }

    // return true if there is an error
    public boolean checkConstantExpressionType(
            @Nullable ConstantValue<?> compileTimeConstant,
            @NotNull CjConstantExpression expression,
            @NotNull CangJieType expectedType
    ) {
        IElementType elementType = expression.getNode().getElementType();

//        if (elementType == CjNodeTypes.INTEGER_CONSTANT) {
//            return checkIntegerValue(compileTimeConstant, expectedType, expression);
//        }
//        else if (elementType == CjNodeTypes.FLOAT_CONSTANT) {
//            return checkFloatValue(compileTimeConstant, expectedType, expression);
//        }
//        else if (elementType == CjNodeTypes.BOOLEAN_CONSTANT) {
//            return checkBooleanValue(expectedType, expression);
//        }
//        else if (elementType == CjNodeTypes.CHARACTER_CONSTANT) {
//            return checkCharValue(compileTimeConstant, expectedType, expression);
//        }
//        else if (elementType == CjNodeTypes.NULL) {
//            return checkNullValue(expectedType, expression);
//        }
        return false;
    }
}
