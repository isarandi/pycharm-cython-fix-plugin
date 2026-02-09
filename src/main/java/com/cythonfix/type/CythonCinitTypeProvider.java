package com.cythonfix.type;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.python.pro.cython.psi.CythonClass;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.jetbrains.python.psi.types.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Provides constructor parameter info from __cinit__ for cdef classes that lack __init__.
 *
 * In Cython, __cinit__ is the C-level constructor. When a cdef class has __cinit__ but no
 * __init__, PyCharm's built-in resolution (which only looks for __init__/__new__) reports
 * "unexpected parameter" for constructor calls. This type provider intercepts the call
 * resolution and provides __cinit__'s parameters instead.
 */
public class CythonCinitTypeProvider extends PyTypeProviderBase {

    @Override
    @Nullable
    public Ref<@Nullable PyCallableType> prepareCalleeTypeForCall(
            @Nullable PyType type,
            @NotNull PyCallExpression call,
            @NotNull TypeEvalContext context) {

        if (!(type instanceof PyClassType classType)) return null;
        if (!classType.isDefinition()) return null;
        if (!(classType.getPyClass() instanceof CythonClass cythonClass)) return null;

        // Only intervene if the class has no __init__ (own or inherited)
        PyFunction init = cythonClass.findMethodByName("__init__", true, context);
        if (init != null) return null;

        // Look for __cinit__ (own or inherited)
        PyFunction cinit = cythonClass.findMethodByName("__cinit__", true, context);
        if (cinit == null) return null;

        PyFunctionType cinitFunctionType = PyFunctionTypeImpl.create(cinit, context);
        PyCallableType callableType = new CinitCallableType(cinitFunctionType, classType);
        return Ref.create(callableType);
    }

    /**
     * Wraps __cinit__'s function type to delegate parameter resolution to __cinit__
     * while returning the class instance type (not None) as the call result.
     */
    private static class CinitCallableType implements PyCallableType {
        private final PyFunctionType myDelegate;
        private final PyClassType myClassType;

        CinitCallableType(PyFunctionType delegate, PyClassType classType) {
            myDelegate = delegate;
            myClassType = classType;
        }

        @Override
        @Nullable
        public PyType getReturnType(@NotNull TypeEvalContext context) {
            return myClassType.toInstance();
        }

        @Override
        @Nullable
        public PyType getCallType(@NotNull TypeEvalContext context, @NotNull PyCallSiteExpression callSite) {
            return myClassType.toInstance();
        }

        @Override
        @Nullable
        public List<PyCallableParameter> getParameters(@NotNull TypeEvalContext context) {
            return myDelegate.getParameters(context);
        }

        @Override
        @Nullable
        public PyCallable getCallable() {
            return myDelegate.getCallable();
        }

        @Override
        public int getImplicitOffset() {
            return myDelegate.getImplicitOffset();
        }

        @Override
        public List<? extends RatedResolveResult> resolveMember(
                @NotNull String name, @Nullable PyExpression location,
                @NotNull AccessDirection direction, @NotNull PyResolveContext resolveContext) {
            return Collections.emptyList();
        }

        @Override
        public Object[] getCompletionVariants(String prefix, PsiElement location, ProcessingContext context) {
            return new Object[0];
        }

        @Override
        @Nullable
        public String getName() {
            return myClassType.getName();
        }

        @Override
        public boolean isBuiltin() {
            return false;
        }

        @Override
        public void assertValid(String message) {
        }
    }
}
