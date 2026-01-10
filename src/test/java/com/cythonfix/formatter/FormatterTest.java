package com.cythonfix.formatter;

import com.intellij.lang.Language;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Tests for the Cython formatter fix.
 *
 * <p>Note: Full Cython formatting tests require PyCharm Professional with
 * the Cython module loaded. The test framework doesn't fully initialize
 * Cython language support, so tests verify basic functionality and skip
 * Cython-specific assertions when the language isn't available.
 *
 * <p>For complete testing, manually verify in PyCharm that:
 * <ul>
 *   <li>{@code <RLE *>} stays as-is (not {@code < RLE * >})</li>
 *   <li>{@code &var} stays as-is (not {@code & var})</li>
 *   <li>{@code float *x} stays as-is (not {@code float * x})</li>
 * </ul>
 */
public class FormatterTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    /**
     * Test that the plugin classes load without error.
     */
    public void testPluginLoads() {
        assertNotNull("FormattingModelBuilder should be loadable",
            FixedCythonFormattingModelBuilder.class);
    }

    /**
     * Test formatting with Cython constructs.
     * Skips Cython-specific assertions if the language isn't available.
     */
    public void testCythonFormatting() {
        String input =
            "ctypedef double *BB\n" +
            "ctypedef struct RLE:\n" +
            "    uint *cnts\n" +
            "\n" +
            "cdef void foo():\n" +
            "    cdef RLE r\n" +
            "    (<RLE *> &r).h = 1\n" +
            "    rleFrPoly(<RLE *> &Rs._R[i], <const double *> data, k, h, w)\n" +
            "    rleFree(&self.r)\n";

        PsiFile file = myFixture.configureByText("test.pyx", input);

        Language cythonLanguage = Language.findLanguageByID("Cython");
        String fileType = file.getFileType().getName();

        // Run formatter
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            CodeStyleManager.getInstance(getProject()).reformat(file);
        });

        String result = file.getText();

        // Only assert Cython-specific formatting if Cython is fully loaded
        if (cythonLanguage != null && "Cython".equals(fileType)) {
            // Type casts: no space inside angle brackets
            assertFalse("Cast should not have space after <",
                result.contains("< RLE"));
            assertFalse("Cast should not have space before >",
                result.contains("* >"));

            // Address-of: no space after &
            assertFalse("Address-of should not have space after &",
                result.contains("& r") || result.contains("& self") || result.contains("& Rs"));

            // Pointer declarations: proper spacing
            assertTrue("Pointer declaration should have proper spacing",
                result.contains("double *BB") || result.contains("double* BB"));
        } else {
            // Log skip reason for CI visibility
            System.out.println("Skipping Cython assertions: language=" + cythonLanguage +
                ", fileType=" + fileType);
            System.out.println("Full Cython testing requires PyCharm Professional.");
        }
    }
}
