package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl;
import com.linqingying.cangjie.resolve.scopes.*;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FunctionDescriptorUtil {
    @NotNull
    public static LexicalScope getFunctionInnerScope(
            @NotNull LexicalScope outerScope,
            @NotNull FunctionDescriptor descriptor,
            @NotNull LocalRedeclarationChecker redeclarationChecker
    ) {

//        return outerScope;
        return new LexicalScopeImpl(
                outerScope, descriptor, true, descriptor.getExtensionReceiverParameter(),
                descriptor.getContextReceiverParameters(), LexicalScopeKind.FUNCTION_INNER_SCOPE, redeclarationChecker,
                handler -> {
//                    for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
//                        handler.addClassifierDescriptor(typeParameter);
//                    }
                    for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
                        if (valueParameterDescriptor instanceof ValueParameterDescriptorImpl.WithDestructuringDeclaration) {
                            List<VariableDescriptor> entries =
                                    ((ValueParameterDescriptorImpl.WithDestructuringDeclaration) valueParameterDescriptor)
                                            .getDestructuringVariables();
                            for (VariableDescriptor entry : entries) {
                                handler.addVariableDescriptor(entry);
                            }
                        } else {
                            handler.addVariableDescriptor(valueParameterDescriptor);
                        }
                    }
                    return Unit.INSTANCE;
                }
        );
    }

    @NotNull
    public static LexicalScope getFunctionInnerScope(
            @NotNull LexicalScope outerScope, @NotNull FunctionDescriptor descriptor,
            @NotNull BindingTrace trace, @NotNull OverloadChecker overloadChecker
    ) {
        return getFunctionInnerScope(outerScope, descriptor, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker));
    }
}
