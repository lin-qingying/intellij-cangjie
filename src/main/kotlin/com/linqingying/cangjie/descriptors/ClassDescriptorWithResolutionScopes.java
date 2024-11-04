package com.linqingying.cangjie.descriptors;

import com.linqingying.cangjie.psi.CjTypeStatement;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import com.linqingying.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ClassDescriptorWithResolutionScopes extends ClassDescriptor{
    @NotNull
    LexicalScope getScopeForMemberDeclarationResolution();
    @NotNull
    @ReadOnly
    Collection<CallableMemberDescriptor> getDeclaredCallableMembers();
    @NotNull
    LexicalScope getScopeForInitializerResolution();
    @NotNull
    LexicalScope getScopeForClassHeaderResolution();
    @NotNull
    LexicalScope getScopeForConstructorHeaderResolution();





}
