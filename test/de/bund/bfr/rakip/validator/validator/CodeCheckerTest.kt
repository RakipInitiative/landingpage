package de.bund.bfr.rakip.validator.de.bund.bfr.rakip.validator

import de.bund.bfr.rakip.validator.CodeChecker
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class CodeCheckerTest {

    @Test
    fun testValidFile() {
        val file = File("testresources/toymodel.fskx")
        val checkResult = CodeChecker().check(file)

        assertTrue(checkResult.error.isEmpty())
        assertTrue(checkResult.warnings.isEmpty())
    }

    @Test
    fun testBrokenModel() {
        val file = File("testresources/broken_model.fskx")
        val checkResult = CodeChecker().check(file)

        assertTrue(checkResult.error.isNotEmpty())
        assertTrue(checkResult.warnings.isEmpty())
    }

    @Test
    fun testBrokenVisualization() {
        val file = File("testresources/broken_visualization.fskx")
        val checkResult = CodeChecker().check(file)

        assertTrue(checkResult.error.isNotEmpty())
        assertTrue(checkResult.warnings.isEmpty())
    }
}
