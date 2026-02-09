package com.cythonfix.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.jetbrains.python.psi.PyNamedParameter;
import com.jetbrains.python.psi.impl.stubs.PyNamedParameterElementType;
import com.jetbrains.python.psi.stubs.PyNamedParameterStub;
import org.jetbrains.annotations.NotNull;

/**
 * Stub element type for unnamed Cython parameters. Extends
 * PyNamedParameterElementType so the PSI infrastructure treats
 * these nodes as proper parameter stubs.
 */
public class FixedCythonParameterElementType extends PyNamedParameterElementType {
    public static final FixedCythonParameterElementType INSTANCE = new FixedCythonParameterElementType();

    public FixedCythonParameterElementType() {
        super("CYTHON_UNNAMED_PARAMETER");
    }

    @Override
    @NotNull
    public PsiElement createElement(@NotNull ASTNode node) {
        return new FixedCythonNamedParameter(node);
    }

    @Override
    @NotNull
    public PyNamedParameter createPsi(@NotNull PyNamedParameterStub stub) {
        return new FixedCythonNamedParameter(stub);
    }

    @Override
    @NotNull
    protected IStubElementType getStubElementType() {
        return INSTANCE;
    }
}