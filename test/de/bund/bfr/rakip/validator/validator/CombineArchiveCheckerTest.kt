package de.bund.bfr.rakip.validator.de.bund.bfr.rakip.validator

import de.bund.bfr.rakip.validator.CombineArchiveChecker
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class CombineArchiveCheckerTest {

    @Test
    fun testValidFile() {
        val file = File("testresources/toymodel.fskx")
        val checkResult = CombineArchiveChecker().check(file)
        assertTrue { checkResult.error.isEmpty() }
        assertTrue { checkResult.warnings.isEmpty() }
    }

    @Test
    fun testInvalidFile() {
        val file = File("testresources/fake.fskx")
        val checkResult = CombineArchiveChecker().check(file)
        assertTrue { checkResult.error.isNotEmpty() }
        assertTrue { checkResult.warnings.isEmpty() }
    }
}
