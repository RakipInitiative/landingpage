package de.bund.bfr.rakip.validator

import de.unirostock.sems.cbarchive.CombineArchive
import de.unirostock.sems.cbarchive.CombineArchiveException
import java.io.File

import de.bund.bfr.fskml.FSKML
import de.bund.bfr.fskml.FskMetaDataObject
import de.unirostock.sems.cbarchive.ArchiveEntry

data class CheckResult(val error: String, val warnings: List<String>)
data class ValidationResult(val isValid: Boolean, val checks: List<CheckResult>)

interface Checker {
    fun check(file: File): CheckResult
}

class CombineArchiveChecker : Checker {

    /**
     * Check if the passed file is a valid CombineArchive.
     *
     * @return If valid a CheckResult with empty error and warnings. If invalid a CheckResult with an error and no
     * warnings.
     */
    override fun check(file: File): CheckResult {

        return try {
            CombineArchive(file).use { }
            CheckResult("", emptyList())
        } catch (err: CombineArchiveException) {
            CheckResult(err.message ?: "", emptyList())
        }
    }
}

class StructureChecker : Checker {

    /**
     * Checks if the passed file has the required files of a simple FSKX model.
     */
    override fun check(file: File): CheckResult {

        val result: CheckResult

        CombineArchive(file).use { archive ->
            // metaData.json is mandatory
            if (!archive.hasMetadata()) {
                result = CheckResult(error = "Missing metadata", warnings = emptyList())
            }

            // model script is mandatory
            else if (!archive.hasModelScript()) {
                result = CheckResult(error = "Missing model script", warnings = emptyList())
            }

            // simulation settings are mandatory
            else if (!archive.hasSimulations()) {
                result = CheckResult(error = "Missing simulation settings", warnings = emptyList())
            } else {
                result = CheckResult(error = "", warnings = emptyList())
            }
        }

        return result
    }

    private fun CombineArchive.hasMetadata(): Boolean {
        val jsonUri = FSKML.getURIS(1, 0, 12)["json"]!!
        return getEntriesWithFormat(jsonUri).any { entry -> entry.fileName == "metaData.json" }
    }

    private fun CombineArchive.hasModelScript(): Boolean {
        val rUri = FSKML.getURIS(1, 0, 12)["r"]
        return getEntriesWithFormat(rUri).filter { entry -> entry.descriptions.isNotEmpty() }
            .any { entry ->
                val metaDataObject = FskMetaDataObject(entry.descriptions[0])
                metaDataObject.resourceType == FskMetaDataObject.ResourceType.modelScript
            }
    }

    private fun CombineArchive.hasSimulations(): Boolean {
        val sedmlUri = FSKML.getURIS(1, 0, 12)["sedml"]
        return hasEntriesWithFormat(sedmlUri)
    }
}

class CodeChecker() : Checker {

    companion object {
        val blacklist = CodeChecker::class.java.getResource("/blacklist.txt").readText().lines().filter{it.isNotEmpty()}
    }

    override fun check(file: File): CheckResult {

        CombineArchive(file).use { archive ->

            // Read model and visualization scripts
            val rUri = FSKML.getURIS(1, 0, 12)["r"]
            var modelScript = ""
            var visualizationScript = ""

            archive.getEntriesWithFormat(rUri).filter { entry -> entry.descriptions.isNotEmpty() }
                .forEach { entry ->
                    val metaDataObject = FskMetaDataObject(entry.descriptions[0])
                    if (metaDataObject.resourceType == FskMetaDataObject.ResourceType.modelScript) {
                        // Read model script
                        modelScript = entry.loadTextEntry()
                    } else {
                        // Read visualization script
                        visualizationScript = entry.loadTextEntry()
                    }
                }

            if (modelScript.isNotEmpty()) {
                val modelCheck = validateScript(modelScript)
                if (modelCheck.error.isNotEmpty()) return modelCheck
            }

            if (visualizationScript.isNotEmpty()) {
                val visualizationCheck = validateScript(visualizationScript)
                if (visualizationCheck.error.isNotEmpty()) return visualizationCheck
            }

            return CheckResult("", emptyList())
        }
    }

    private fun validateScript(script: String): CheckResult {

        for (command: String in blacklist) {
            if (script.contains(command))
                return CheckResult("Command $command is not allowed", emptyList())
        }
        return CheckResult("", emptyList())
    }
}

private fun ArchiveEntry.loadTextEntry(): String {
    val tempFile = createTempFile()
    return try {
        extractFile(tempFile)
        tempFile.readText()
    } finally {
        tempFile.delete()
    }
}