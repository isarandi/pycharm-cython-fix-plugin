package com.cythonfix.psi;

import com.intellij.lang.ASTNode;
import com.intellij.python.pro.cython.psi.CythonNamedParameter;
import com.jetbrains.python.psi.stubs.PyNamedParameterStub;

/**
 * A CythonNamedParameter that returns a unique synthetic name for unnamed parameters,
 * preventing false "duplicate parameter name" errors in cdef extern declarations
 * like: void f(qhT *, char *, char *) nogil
 */
public class FixedCythonNamedParameter extends CythonNamedParameter {
    public FixedCythonNamedParameter(ASTNode node) {
        super(node);
    }

    public FixedCythonNamedParameter(PyNamedParameterStub stub) {
        super(stub);
    }

    @Override
    public String getName() {
        String name = super.getName();
        if (name == null) {
            return "__unnamed_" + getTextOffset();
        }
        return name;
    }
}