package com.cythonfix.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.python.pro.cython.CythonLanguageDialect;
import com.intellij.python.pro.cython.psi.CythonIncludeStatement;
import com.intellij.python.pro.cython.psi.CythonNamedElement;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyQualifiedExpression;
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves Cython-specific names (ctypedef, cdef struct, ctypedef fused, etc.) from included
 * .pxi files.
 *
 * PyCharm's built-in include resolution delegates to PyFile.multiResolveName(), which uses
 * ExportedNameCache. That cache takes the stub path for files not open in the editor, and
 * since CythonTypedefStatement has no stub type, it's invisible. This provider fills the gap
 * by walking included files' AST children to find CythonNamedElement instances.
 *
 * This is a non-overriding provider, so it only fires when normal resolution found nothing.
 */
public class CythonIncludeResolveProvider implements PyReferenceResolveProvider {

    @Override
    @NotNull
    public List<RatedResolveResult> resolveName(
            @NotNull PyQualifiedExpression element,
            @NotNull TypeEvalContext context) {

        if (element.isQualified()) return Collections.emptyList();
        if (!CythonLanguageDialect.isInsideCythonFile(element)) return Collections.emptyList();

        String name = element.getReferencedName();
        if (name == null) return Collections.emptyList();

        if (!(element.getContainingFile() instanceof PyFile pyFile)) return Collections.emptyList();

        List<RatedResolveResult> results = new ArrayList<>();
        resolveInIncludes(pyFile, name, results, new HashSet<>());
        return results;
    }

    private static void resolveInIncludes(
            PyFile file, String name, List<RatedResolveResult> results, Set<PyFile> visited) {
        if (!visited.add(file)) return;

        for (PsiElement child : file.getChildren()) {
            if (child instanceof CythonIncludeStatement include) {
                PsiElement resolved = include.getReference().resolve();
                if (resolved instanceof PyFile includedFile) {
                    findCythonNamedElement(includedFile, name, results);
                    // Handle transitive includes
                    resolveInIncludes(includedFile, name, results, visited);
                }
            }
        }
    }

    private static void findCythonNamedElement(
            PyFile file, String name, List<RatedResolveResult> results) {
        for (PsiElement child : file.getChildren()) {
            if (child instanceof CythonNamedElement named && name.equals(named.getName())) {
                results.add(new RatedResolveResult(RatedResolveResult.RATE_NORMAL, child));
            }
        }
    }
}
