package de.bund.bfr.landingpage

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import de.unirostock.sems.cbarchive.ArchiveEntry
import de.unirostock.sems.cbarchive.CombineArchive
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.net.URI
import de.bund.bfr.fskml.FSKML
import de.bund.bfr.fskml.FskMetaDataObject
import org.jdom.Text
import org.jlibsedml.ChangeAttribute
import org.jlibsedml.Libsedml
import org.jlibsedml.SEDMLTags
import org.jlibsedml.SedML
import org.renjin.script.RenjinScriptEngine
import org.renjin.script.RenjinScriptEngineFactory
import java.util.*
import kotlin.collections.LinkedHashMap

val MAPPER = ObjectMapper()

val appConfiguration = loadConfiguration()

data class ModelView(
    val modelName: String,
    val modelId: String,
    val software: String,
    val environment: Set<String>,
    val hazard: Set<String>,
    val modelType: String,
    val durationTime: String,
    val uploadTime: String,
    val downloadUrl: String
)

data class ProcessedMetadata(
    val views: List<ModelView>,
    val uniqueEnvironments: Set<String>,
    val uniqueHazards: Set<String>,
    val uniqueSoftware: Set<String>,
    val uniqueTypes: Set<String>
)

fun Application.module() {
    install(DefaultHeaders)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
        }
    }
    install(CORS) {
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        anyHost()
    }

    val uploadTimes = mutableMapOf<String, String>()
    val executionTimes = mutableMapOf<String, String>()
    val timesFile = appConfiguration.getProperty("times_csv")
    File(timesFile).readLines().forEach {
        val tokens = it.split(",")
        val modelId = tokens[0]
        val modelUploadTime = tokens[2]
        val modelExecutionTime = tokens[1]

        uploadTimes[modelId] = modelUploadTime
        executionTimes[modelId] = modelExecutionTime
    }

    val filesFolder = appConfiguration.getProperty("model_folder")
    val modelFiles = File(filesFolder).walk().filter { it.isFile && it.extension == "fskx" }.toList()

    val imgFiles = File(appConfiguration.getProperty("plot_folder")).walk().filter { it.isFile }.toList()

    val rawMetadata = loadRawMetadata(modelFiles)
    val processedMetadata = processMetadata(rawMetadata, executionTimes, uploadTimes)

    val parsedMetadata = rawMetadata.map { MAPPER.readTree(it) }

    val representation = object {
        val endpoint = appConfiguration.getProperty("base_url")
        val resourcesFolder = if(appConfiguration.getProperty("context") != null) {
            "${appConfiguration.getProperty("context")}/"
        } else {
            ""
        }
    }

    routing {
        get("/") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }

        get("/download/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {

                    var modelFile = modelFiles[it]
                    // for the demo: if model is from FSK-Web (starts with 2020) then only download toymodel
                    if(modelFile.name.startsWith("2020") || modelFile.name.startsWith("showcase")
                        || modelFile.name.startsWith("gropin")){
                        modelFile = File(appConfiguration.getProperty("model_folder"), "toymodel.fskx")
                    }
                    call.response.header("Content-Disposition", "attachment; filename=${modelFile.name}")
                    call.respondFile(modelFile)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("/metadata/{i}") {
            call.parameters["i"]?.toInt()?.let {
                call.respond(parsedMetadata[it])
            }
        }

        // Returns image where id is the model id, e.g. /image/YE2017
        get("/image/{id}") {
            call.parameters["id"]?.let { imageId ->
                try {
                    val imgFile = imgFiles.first { it.nameWithoutExtension == imageId }
                    call.response.header("Content-Disposition", "inline")
                    call.respondText(imgFile.readText())
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("/modelscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    val model = readModelScript(modelFile)
                    call.respondText(model)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/visualizationscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    val visualizationScript = readVisualizationScript(modelFile)
                    call.respondText(visualizationScript)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("/execute/{i}") {
            // TODO: execute model at index i

            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    val model = readModel(modelFile)

                    if (call.parameters.contains("simulationIndex")) {
                        val simIndex = call.parameters["simulationIndex"]?.toInt() ?: 0
                        call.respondText(model.run(simIndex))
                    } else {
                        call.respondText(model.run(0))
                    }



                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

        }
        get("/search/{term}") {
            val matchingModelIndexes = mutableListOf<Int>()
            call.parameters["term"]?.let { term ->
                rawMetadata.forEachIndexed { index, modelMetadata ->
                    if (modelMetadata.contains(term, ignoreCase = true)) {
                        matchingModelIndexes.add(index)
                    }
                }
            }

            call.respond(matchingModelIndexes)
        }

        get("/simulations/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]

                    // Load metadata
                    val metadata = CombineArchive(modelFile).use { it.loadMetadata() }
                    val parameter = metadata["modelMath"]["parameter"]

                    val simulations = readSimulations(modelFile, parameter)
                    call.respond(simulations)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        static("/assets") {
            resources("assets")
        }
    }
}

private fun loadRawMetadata(modelFiles: List<File>): List<String> {
    val uri = URI("https://www.iana.org/assignments/media-types/application/json")

    val metadata = mutableListOf<String>()

    modelFiles.forEach { file ->
        CombineArchive(file).use { archive ->
            // Find entry named "metaData.json", read and add metadata
            archive.getEntriesWithFormat(uri).find { it.entityPath.endsWith("metaData.json") }?.let { entry ->
                // Extract metadaEntry to temp file
                val tempMetadataFile = File.createTempFile("metadata", ".json")
                entry.extractFile(tempMetadataFile)
                metadata.add(tempMetadataFile.readText(Charsets.UTF_8))
                tempMetadataFile.delete()
            }
        }
    }

    return metadata
}

private fun processMetadata(
    rawMetadata: List<String>,
    executionTimes: Map<String, String>,
    uploadTimes: Map<String, String>
): ProcessedMetadata {

    val modelViews = rawMetadata.mapIndexed { index, rawModel ->
        val metadataTree = MAPPER.readTree(rawModel)
        val generalInformation = metadataTree["generalInformation"]
        val scope = metadataTree["scope"]

        val modelId = generalInformation["identifier"].asText()

        // RAKIP 1.0.3 workaround. 1.0.4 has name properties. If not found, try to get 1.0.3 productName.
        val environment = if (scope.has("product")) {
            scope["product"].map { if (it.has("name")) it["name"] else it["productName"] }.map { it.asText() }
                .toSet()
        } else {
            emptySet()
        }

        // RAKIP 1.0.3 workaround. 1.0.4 has name properties. If not found, try to get 1.0.3 hazardName.
        val hazard = if (scope.has("hazard")) {
            scope["hazard"].map { if (it.has("name")) it["name"] else it["hazardName"] }.map { it.asText() }.toSet()
        } else {
            emptySet()
        }

        ModelView(
            modelName = generalInformation["name"].asText(),
            modelId,
            software = generalInformation["software"].asText(),
            environment,
            hazard,
            modelType = metadataTree["modelType"].asText(),
            durationTime = executionTimes[modelId] ?: "", // Get durationTime from executionTimes dictionary
            uploadTime = uploadTimes[modelId] ?: "", // Get upload time from uploadTimes dictionary
            downloadUrl = "${appConfiguration.getProperty("base_url")}/download/$index"
        )
    }

    return ProcessedMetadata(
        modelViews,
        uniqueEnvironments = modelViews.flatMap { it.environment }.toSet(),
        uniqueHazards = modelViews.flatMap { it.hazard }.toSet(),
        uniqueSoftware = modelViews.map { it.software }.toSet(),
        uniqueTypes = modelViews.map { it.modelType }.toSet()
    )
}

data class FskModel(
    val modelScript: String,
    val visualizationScript: String?,
    val simulations: List<FskSimulation>,
    val selectedSimulationIndex: Int,
    val workingDirectory: File? = null,
    val metadata: JsonNode
)

data class FskSimulation(val name: String, val parameters: LinkedHashMap<String, String>)


fun readModel(modelFile: File): FskModel {

    val URIS = FSKML.getURIS(1, 0, 12)

    val sedmlUri: URI = URIS["sedml"]!!
    val rUri = URIS["r"]!!

    var modelScript = ""
    var visualizationScript = ""
    var selectedSimulationIndex = 0
    var simulations = emptyList<FskSimulation>()
    var workingDirectory: File? = null
    var metadata: JsonNode

    CombineArchive(modelFile).use {

        // Load JSON metadata
        metadata = it.loadMetadata()

        // Load model and visualization scripts
        it.getEntriesWithFormat(rUri).filter { entry -> entry.descriptions.isNotEmpty() }
            .forEach { entry ->
                val firstDescription = entry.descriptions[0]
                val metadataObject = FskMetaDataObject(firstDescription)
                val resourceType = metadataObject.resourceType

                if (resourceType == FskMetaDataObject.ResourceType.modelScript) {
                    modelScript = entry.loadTextEntry()
                } else if (resourceType == FskMetaDataObject.ResourceType.visualizationScript) {
                    visualizationScript = entry.loadTextEntry()
                }
            }

        // Get SEDML document
        it.getEntriesWithFormat(sedmlUri).first().let { sedmlEntry ->

            val tempFile = createTempFile()
            sedmlEntry.extractFile(tempFile)

            // Read SEDML and delete temporary file
            val sedml = Libsedml.readDocument(tempFile).sedMLModel
            tempFile.delete()

            // Get selected simulation index
            selectedSimulationIndex = sedml.getSelectedSimulationIndex()
            simulations = sedml.getFskSimulations(metadata["modelMath"]["parameter"])
        }

        // Get resources into a working directory
        workingDirectory = it.processResources()
    }

    return FskModel(
        modelScript,
        visualizationScript,
        simulations,
        selectedSimulationIndex,
        workingDirectory,
        metadata
    )
}

private fun loadConfiguration(): Properties {

    val properties = Properties()

    val configFileInUserFolder = File(System.getProperty("user.home"), "landingpage.properties")

    if (configFileInUserFolder.exists()) {
        configFileInUserFolder.inputStream().use {
            properties.load(it)
        }
    } else {
        val catalinaFolder = System.getProperty("catalina.home")
        if (catalinaFolder != null && File(catalinaFolder, "landingpage.properties").exists()) {
            File(catalinaFolder, "landingpage.properties").inputStream().use {
                properties.load(it)
            }
        } else {
            error("Configuration file not found")
        }
    }

    return properties
}

private fun readModelScript(modelFile: File): String {
    val rUri = FSKML.getURIS(1, 0, 12)["r"]!!
    return CombineArchive(modelFile).use {
        it.getEntriesWithFormat(rUri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == FskMetaDataObject.ResourceType.modelScript
        }.loadTextEntry()
    }
}

private fun readVisualizationScript(modelFile: File): String {
    val rUri = FSKML.getURIS(1, 0, 12)["r"]!!
    return CombineArchive(modelFile).use {
        it.getEntriesWithFormat(rUri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == FskMetaDataObject.ResourceType.visualizationScript
        }.loadTextEntry()
    }
}

private fun readSimulations(modelFile: File, parameterMetadata: JsonNode): List<FskSimulation> {

    val sedmlUri: URI = FSKML.getURIS(1, 0, 12)["sedml"]!!

    return CombineArchive(modelFile).use {
        // Get SEDML document
        it.getEntriesWithFormat(sedmlUri).first().let { sedmlEntry ->

            val tempFile = createTempFile()
            sedmlEntry.extractFile(tempFile)

            // Read SEDML and delete temporary file
            val sedml = Libsedml.readDocument(tempFile).sedMLModel
            tempFile.delete()

            sedml
        }.getFskSimulations(parameterMetadata)
    }
}

/**
 * Process the resources in the CombineArchive and copy
 * them to a temporary working directory.
 *
 * @return generated working directory
 */
fun CombineArchive.processResources(): File? {
    val URIS = FSKML.getURIS(1, 0, 12)
    val textUri = URI.create("http://purl.org/NET/mediatypes/text-xplain")
    val csvUri: URI = URIS["csv"]!!
    val xlsxUri: URI = URIS["xlsx"]!!
    val rdataUri: URI = URIS["rdata"]!!

    val resourceEntries = mutableListOf<ArchiveEntry>()
    resourceEntries.addAll(getEntriesWithFormat(textUri))
    resourceEntries.addAll(getEntriesWithFormat(csvUri))
    resourceEntries.addAll(getEntriesWithFormat(xlsxUri))
    resourceEntries.addAll(getEntriesWithFormat(rdataUri))

    if (resourceEntries.isNotEmpty()) {
        val workingDirectory = createTempDir()

        resourceEntries.forEach { entry ->
            val targetFile = workingDirectory.resolve(entry.fileName)
            entry.extractFile(targetFile)
        }

        return workingDirectory
    }

    return null
}

fun CombineArchive.loadMetadata(): JsonNode {
    val jsonUri = FSKML.getURIS(1, 0, 12)["json"]!!
    return getEntriesWithFormat(jsonUri).first { entry -> entry.fileName == "metaData.json" }.let { entry ->
        val tempFile = createTempFile()
        try {
            entry.extractFile(tempFile)
            MAPPER.readTree(tempFile)
        } finally {
            tempFile.delete()
        }
    }
}

fun ArchiveEntry.loadTextEntry(): String {
    val tempFile = createTempFile()
    return try {
        extractFile(tempFile)
        tempFile.readText()
    } finally {
        tempFile.delete()
    }
}

/**
 * @return the selected simulation index if set, otherwise 0 (defaultSimulation).
 */
fun SedML.getSelectedSimulationIndex(): Int {
    return if (annotation.isNotEmpty()) {
        val indexAnnotation = annotation[0]
        val indexAnnotationText = indexAnnotation.annotationElement.content[0] as Text
        indexAnnotationText.text.toInt()
    } else {
        0
    }
}

fun SedML.getFskSimulations(parameterMetadata: JsonNode): List<FskSimulation> {

    // Filter out constant and output parameter metadata
    val inputParameterMetadata = parameterMetadata.filter {
        val classification = it.findValuesAsText("classification")[0]
        classification == "INPUT" || classification == "CONSTANT"
    }

    val simulations = mutableListOf<FskSimulation>()

    // For each SED-ML model create an FskSimulation
    models.forEach { model ->

        val newValues = model.listOfChanges.filter { change -> change.changeKind == SEDMLTags.CHANGE_ATTRIBUTE_KIND }
            .map { change -> change as ChangeAttribute }
            .map { change -> change.targetXPath.toString() to change.newValue!! }
            .toMap()

        val simulationName = model.id
        val orderedValues = LinkedHashMap<String, String>()
        inputParameterMetadata.forEach { param ->
            val paramId = param.findValuesAsText("id")[0]
            val paramValue = newValues[paramId]
            orderedValues[paramId] = paramValue!!
        }

        simulations.add(FskSimulation(simulationName, orderedValues))
    }

    return simulations
}

/**
 * Run model with the simulationIndex if provided or the selected simulation in model file (SED-ML).
 */
fun FskModel.run(simulationIndex: Int): String {

    val factory = RenjinScriptEngineFactory()
    val engine = factory.scriptEngine

    // TODO: 1. Resources
    workingDirectory?.let {
        val correctedPath = it.absolutePath.replace("\\", "/")
        engine.eval("setwd('$correctedPath')")
        println(engine.eval("getwd()"))
        println(engine.eval("list.files()"))

//        engine.eval("setwd('C:/Users/de/Documents')")
//        println(engine.eval("list.files('C:/Users/de/Documents')"))
    }

    // 2. Simulation script
    runSelectedSimulation(engine, simulationIndex)

    // 3. Model script
    engine.eval(modelScript)

    // 4. Visualization script
    engine.eval("options(device='png')")
    val temporaryImageFile = createTempFile("plot", ".svg")
    val command = "svg('${temporaryImageFile.absolutePath.replace("\\", "/")}')"
    engine.eval(command)

    // TODO: customized visualization script. Colours are not supported in Renjin
    //engine.eval("hist(result, breaks=50, main='PREVALENCE WITHIN A FLOCK AFTER HORIZ.TRANS.', xlab='Prevalence')")
    engine.eval(visualizationScript)
    engine.eval("dev.off()")

    // Delete temporary working directory
    workingDirectory?.delete()

    val svgPlot = temporaryImageFile.readText()
    temporaryImageFile.delete()

    return svgPlot
}

fun FskModel.runSelectedSimulation(engine: RenjinScriptEngine, simulationIndex: Int?) {

    val selectedSimulation = simulations[simulationIndex ?: selectedSimulationIndex]


    // Build parameters script
    val builder = StringBuilder()
    for ((parameterName, parameterValue) in selectedSimulation.parameters) {
        builder.append("$parameterName <- $parameterValue\n")
    }
    val parameterScript = builder.toString()

    engine.eval(parameterScript)

}

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1", module = Application::module).start(wait = true)
}