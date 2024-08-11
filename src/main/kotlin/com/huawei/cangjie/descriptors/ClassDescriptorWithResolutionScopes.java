package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ClassDescriptorWithResolutionScopes extends ClassDescriptor{
    @NotNull
    LexicalScope getScopeForMemberDeclarationResolution();
    @NotNull
    @ReadOnly
    Collection<CallableMemberDescriptor> getDeclaredCallableMembers();

}
