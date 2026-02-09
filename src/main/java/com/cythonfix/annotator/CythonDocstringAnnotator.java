package com.cythonfix.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.python.pro.cython.CythonLanguageDialect;
import com.jetbrains.python.highlighting.PyHighlighter;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyStatementList;
import com.jetbrains.python.ast.PyAstStringLiteralExpression;
import com.jetbrains.python.ast.docstring.DocStringUtilCore;
import org.jetbrains.annotations.NotNull;

/**
 * Applies docstring highlighting to string literals in Cython function/class bodies.
 *
 * The JFlex lexer only enters "pending docstring" state after `def`/`class` keywords.
 * Since `cdef`/`cpdef` are plain identifiers to the lexer, their docstrings are tokenized
 * as regular strings. This annotator corrects that by applying PY_DOC_COMMENT coloring.
 */
public class CythonDocstringAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PyFunction) && !(element instanceof PyClass)) return;
        if (!CythonLanguageDialect.isInsideCythonFile(element)) return;

        PyStatementList statementList;
        if (element instanceof PyFunction fn) {
            statementList = fn.getStatementList();
        } else {
            statementList = ((PyClass) element).getStatementList();
        }

        if (statementList == null) return;

        PyAstStringLiteralExpression docString = DocStringUtilCore.findDocStringExpression(statementList);
        if (docString == null) return;

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(docString)
                .textAttributes(PyHighlighter.PY_DOC_COMMENT)
                .create();
    }
}