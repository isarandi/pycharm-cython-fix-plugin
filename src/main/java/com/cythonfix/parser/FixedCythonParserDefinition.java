package com.cythonfix.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.python.pro.cython.CythonLanguageDialect;
import com.intellij.python.pro.cython.parser.CythonLexer;
import com.intellij.python.pro.cython.psi.CythonFile;
import com.jetbrains.python.PythonParserDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Parser definition that uses our fixed parser.
 * Since CythonParserDefinition is final, we re-implement the interface.
 */
public class FixedCythonParserDefinition extends PythonParserDefinition {
    @Override
    @NotNull
    public Lexer createLexer(Project project) {
        return new CythonLexer();
    }

    @Override
    @NotNull
    public PsiParser createParser(Project project) {
        return new FixedCythonParser();
    }

    @Override
    @NotNull
    public IFileElementType getFileNodeType() {
        return CythonLanguageDialect.getInstance().getFileElementType();
    }

    @Override
    @NotNull
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new CythonFile(viewProvider);
    }
}
