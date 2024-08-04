package com.linqingying.cangjie.doc.parser;


import com.linqingying.cangjie.doc.psi.impl.CDocName;
import com.linqingying.cangjie.doc.psi.impl.CDocSection;
import com.linqingying.cangjie.doc.psi.impl.CDocTag;

public class CDocElementTypes {
    public static final CDocElementType CDOC_SECTION = new CDocElementType("CDOC_SECTION", CDocSection.class);
    public static final CDocElementType CDOC_TAG = new CDocElementType("CDOC_TAG", CDocTag.class);
    public static final CDocElementType CDOC_NAME = new CDocElementType("CDOC_NAME", CDocName.class);
}
