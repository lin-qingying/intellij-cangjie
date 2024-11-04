package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor

interface BuiltInsPackageFragment : PackageFragmentDescriptor
{
    val isFallback: Boolean
}
