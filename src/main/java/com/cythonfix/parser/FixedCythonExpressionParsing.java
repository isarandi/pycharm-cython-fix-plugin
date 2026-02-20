package com.cythonfix.parser;

import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.python.pro.cython.parser.CythonExpressionParsing;
import com.intellij.python.pro.cython.psi.elementTypes.CythonElementTypes;
import com.jetbrains.python.PyParsingBundle;
import com.jetbrains.python.PyTokenTypes;

/**
 * Fixes sizeof() parsing to accept general expressions, not just type declarations.
 *
 * PyCharm's built-in parser only handles sizeof(type), e.g. sizeof(int), sizeof(MyStruct).
 * It fails on sizeof(expr) with chained member access like sizeof(self.ptr[0].info.field)
 * because the type declaration parser only allows one dot-access after subscripts.
 *
 * Fix: try type-based parsing first (preserving existing behavior), and if it doesn't
 * consume everything up to ')', roll back and parse as a general expression instead.
 */
public class FixedCythonExpressionParsing extends CythonExpressionParsing {

    public FixedCythonExpressionParsing(FixedCythonParsingContext context) {
        super(context);
    }

    @Override
    protected boolean parseUnaryExpression(boolean isTargetExpression) {
        if (atToken(PyTokenTypes.IDENTIFIER, TOK_SIZEOF)) {
            parseFixedSizeOfExpression();
            return true;
        }
        return super.parseUnaryExpression(isTargetExpression);
    }

    private void parseFixedSizeOfExpression() {
        SyntaxTreeBuilder.Marker sizeofMarker = myBuilder.mark();
        nextToken(); // consume "sizeof"
        checkMatches(PyTokenTypes.LPAR, PyParsingBundle.message("PARSE.expected.lpar"));

        // Try type-based parsing first (handles sizeof(int), sizeof(unsigned long), etc.)
        SyntaxTreeBuilder.Marker rollbackMarker = myBuilder.mark();
        FixedCythonDeclParsing declParser =
                ((FixedCythonParsingContext) getParsingContext()).getFixedDeclParser();
        declParser.parseBaseTypeDecl(false);
        declParser.parseNameDeclPublic(true, false, false);

        if (atToken(PyTokenTypes.RPAR)) {
            // Type parsing consumed everything up to ')' — use it
            rollbackMarker.drop();
        } else {
            // Type parsing didn't reach ')' — roll back and try as expression
            rollbackMarker.rollbackTo();
            parseExpressionOptional();
        }

        checkMatches(PyTokenTypes.RPAR, PyParsingBundle.message("PARSE.expected.rpar"));
        sizeofMarker.done(CythonElementTypes.SIZEOF_EXPRESSION);
    }
}
