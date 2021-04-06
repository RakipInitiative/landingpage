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
import de.unirostock.sems.cbarchive.CombineArchiveException
import io.ktor.request.*
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

fun Application.module(testing: Boolean = false) {
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

    val modelFiles: List<File>
    val filesFolder: String?
    val imgFiles: List<File>
    val rawMetadata: List<String>
    val processedMetadata: ProcessedMetadata?
    val parsedMetadata: List<JsonNode>
    val fskweb_modelFiles: List<File>
    val fskweb_filesFolder: String?
    val fskweb_imgFiles: List<File>
    val fskweb_rawMetadata: List<String>
    val fskweb_processedMetadata: ProcessedMetadata?
    val fskweb_parsedMetadata: List<JsonNode>
    val rakipweb_modelFiles: List<File>
    val rakipweb_filesFolder: String?
    val rakipweb_imgFiles: List<File>
    val rakipweb_rawMetadata: List<String>
    val rakipweb_processedMetadata: ProcessedMetadata?
    val rakipweb_parsedMetadata: List<JsonNode>
    val baseUrl: String?
    val context: String

    if (testing) {
        modelFiles = emptyList()
        filesFolder = null
        imgFiles = emptyList()
        rawMetadata = emptyList()
        processedMetadata = null
        parsedMetadata = emptyList()
        fskweb_modelFiles = emptyList()
        fskweb_filesFolder = null
        fskweb_imgFiles = emptyList()
        fskweb_rawMetadata = emptyList()
        fskweb_processedMetadata = null
        fskweb_parsedMetadata = emptyList()
        rakipweb_modelFiles = emptyList()
        rakipweb_filesFolder = null
        rakipweb_imgFiles = emptyList()
        rakipweb_rawMetadata = emptyList()
        rakipweb_processedMetadata = null
        rakipweb_parsedMetadata = emptyList()
        baseUrl = null
        context = ""
    } else {

        val appConfiguration = loadConfiguration()

        // Times
        val timesFile = appConfiguration.getProperty("times_csv")

        val temporaryUploadTimes = mutableMapOf<String, String>()
        val temporaryExecutionTimes = mutableMapOf<String, String>()
        File(timesFile).readLines().forEach {
            val tokens = it.split(",")
            val modelId = tokens[0]
            temporaryUploadTimes[modelId] = tokens[2]
            temporaryExecutionTimes[modelId] = tokens[1]
        }

        val uploadTimes = temporaryUploadTimes.toMap()
        val executionTimes = temporaryExecutionTimes.toMap()

        // Model files
        filesFolder = appConfiguration.getProperty("model_folder")
        modelFiles = File(filesFolder).walk().filter { it.isFile && it.extension == "fskx" }.toList()

        // Image files
        imgFiles = File(appConfiguration.getProperty("plot_folder")).walk().filter { it.isFile }.toList()

        // Metadata
        rawMetadata = loadRawMetadata(modelFiles)
        parsedMetadata = rawMetadata.map { MAPPER.readTree(it) }

        // FSK_Web
        // Times
        val fskweb_timesFile = appConfiguration.getProperty("fskweb_times_csv")

        File(fskweb_timesFile).readLines().forEach {
            val tokens = it.split(",")
            val modelId = tokens[0]
            temporaryUploadTimes[modelId] = tokens[2]
            temporaryExecutionTimes[modelId] = tokens[1]
        }

        val fskweb_uploadTimes = temporaryUploadTimes.toMap()
        val fskweb_executionTimes = temporaryExecutionTimes.toMap()

        // Model files
        fskweb_filesFolder = appConfiguration.getProperty("fskweb_model_folder")
        fskweb_modelFiles = File(fskweb_filesFolder).walk().filter { it.isFile && it.extension == "fskx" }.toList()

        // Image files
        fskweb_imgFiles = File(appConfiguration.getProperty("fskweb_plot_folder")).walk().filter { it.isFile }.toList()

        // Metadata
        fskweb_rawMetadata = loadRawMetadata(fskweb_modelFiles)
        fskweb_parsedMetadata = fskweb_rawMetadata.map { MAPPER.readTree(it) }


        // RAKIP-Web
        // Times
        val rakipweb_timesFile = appConfiguration.getProperty("rakipweb_times_csv")

        File(rakipweb_timesFile).readLines().forEach {
            val tokens = it.split(",")
            val modelId = tokens[0]
            temporaryUploadTimes[modelId] = tokens[2]
            temporaryExecutionTimes[modelId] = tokens[1]
        }

        val rakipweb_uploadTimes = temporaryUploadTimes.toMap()
        val rakipweb_executionTimes = temporaryExecutionTimes.toMap()

        // Model files
        rakipweb_filesFolder = appConfiguration.getProperty("rakipweb_model_folder")
        rakipweb_modelFiles = File(rakipweb_filesFolder).walk().filter { it.isFile && it.extension == "fskx" }.toList()

        // Image files
        rakipweb_imgFiles = File(appConfiguration.getProperty("rakipweb_plot_folder")).walk().filter { it.isFile }.toList()

        // Metadata
        rakipweb_rawMetadata = loadRawMetadata(rakipweb_modelFiles)
        rakipweb_parsedMetadata = rakipweb_rawMetadata.map { MAPPER.readTree(it) }




        baseUrl = appConfiguration.getProperty("base_url")

        context = appConfiguration.getProperty("context")?.let { "$it/" } ?: ""

        processedMetadata = processMetadata(rawMetadata, executionTimes, uploadTimes, baseUrl)
        fskweb_processedMetadata = processMetadata(fskweb_rawMetadata, fskweb_executionTimes, fskweb_uploadTimes, baseUrl)
        rakipweb_processedMetadata = processMetadata(rakipweb_rawMetadata, rakipweb_executionTimes, rakipweb_uploadTimes, baseUrl)
    }

    val representation = object {
        val endpoint = baseUrl ?: ""
        val resourcesFolder = context
    }

    /** Helper function for retrieving execution and upload times. */
    fun getView(index: Int) = processedMetadata?.let { it.views[index] }
    fun getViewFskWeb(index: Int) = fskweb_processedMetadata?.let { it.views[index] }
    fun getViewRakipWeb(index: Int) = rakipweb_processedMetadata?.let { it.views[index] }

    routing {
        get("/") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }


        get("/download/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    call.response.header("Content-Disposition", "attachment; filename=${modelFile.name}")
                    call.respondFile(modelFile)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // FSK-Web download
        get("/FSK-Web/download/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = fskweb_modelFiles[it]
                    call.response.header("Content-Disposition", "attachment; filename=${modelFile.name}")
                    call.respondFile(modelFile)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // FSK-Web download
        get("/RAKIP-Web/download/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = rakipweb_modelFiles[it]
                    call.response.header("Content-Disposition", "attachment; filename=${modelFile.name}")
                    call.respondFile(modelFile)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/download_dummy/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    filesFolder?.let {
                        val modelFile = File(it, "toymodel.fskx")
                        call.response.header("Content-Disposition", "attachment; filename=${modelFile.name}")
                        call.respondFile(modelFile)
                    }
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/metadata") {
            call.respond(parsedMetadata)
        }
        get("/FSK-Web/metadata") {
            call.respond(fskweb_parsedMetadata)
        }
        get("/RAKIP-Web/metadata") {
            call.respond(rakipweb_parsedMetadata)
        }
        get("/metadata/{i}") {
            call.parameters["i"]?.toInt()?.let {
                call.respond(parsedMetadata[it])
            }
        }

        get("/FSK-Web/metadata/{i}") {
            call.parameters["i"]?.toInt()?.let {
                call.respond(fskweb_parsedMetadata[it])
            }
        }
        get("/RAKIP-Web/metadata/{i}") {
            call.parameters["i"]?.toInt()?.let {
                call.respond(rakipweb_parsedMetadata[it])
            }
        }

        // Returns image where id is the model id, e.g. /image/YE2017
        get("/image/{id}") {
            call.parameters["id"]?.let { imageId ->
                try {
                    val imgFile = imgFiles.first { it.nameWithoutExtension.replace("/","").replace(":","") == imageId.replace("/","").replace(":","") }
                    call.response.header("Content-Disposition", "inline")
                    call.respondText(imgFile.readText())
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // Returns image where id is the model id, e.g. /image/YE2017
        get("/FSK-Web/image/{id}") {
            call.parameters["id"]?.let { imageId ->
                try {
                    val imgFile = fskweb_imgFiles.first { it.nameWithoutExtension.replace("/","").replace(":","") == imageId.replace("/","").replace(":","") }
                    call.response.header("Content-Disposition", "inline")
                    call.respondText(imgFile.readText())
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // Returns image where id is the model id, e.g. /image/YE2017
        get("/RAKIP-Web/image/{id}") {
            call.parameters["id"]?.let { imageId ->
                try {
                    val imgFile = rakipweb_imgFiles.first { it.nameWithoutExtension.replace("/","").replace(":","") == imageId.replace("/","").replace(":","") }
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
        get("/FSK-Web/modelscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = fskweb_modelFiles[it]
                    val model = readModelScript(modelFile)
                    call.respondText(model)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/RAKIP-Web/modelscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = rakipweb_modelFiles[it]
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
        get("/FSK-Web/visualizationscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = fskweb_modelFiles[it]
                    val visualizationScript = readVisualizationScript(modelFile)
                    call.respondText(visualizationScript)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/RAKIP-Web/visualizationscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = rakipweb_modelFiles[it]
                    val visualizationScript = readVisualizationScript(modelFile)
                    call.respondText(visualizationScript)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // endpoint to run a model with a simulationIndex given as a parameter
        get("/execute/{i}") {
            // TODO: execute model at index i

            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    val model = readModel(modelFile)

                    val simulation = model.simulations[call.parameters["simulationIndex"]?.toInt() ?: 0]
                    call.respondText(model.run(simulation))
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // endpoint to run models with a posted simulation in the body(JSON)
        post("/execute/{i}") {
            val sim = call.receive<FskSimulation>()
            //println("SERVER: Message from the client: $sim");
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    val model = readModel(modelFile)

                    call.respondText(model.run(sim))

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }


        // endpoint for FSK-Web doesn't allow execution
        get("/FSK-Web/execute/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val svg = "<svg version=\"1.1\" baseProfile=\"full\" width=\"300\" height=\"200\"\n" +
                            "        xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                    call.respondText(svg)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // endpoint for RAKIP-Web doesn't allow execution
        get("/RAKIP-Web/execute/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val svg = "<svg version=\"1.1\" baseProfile=\"full\" width=\"300\" height=\"200\"\n" +
                            "        xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                    call.respondText(svg)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        post("/FSK-Web/execute/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val svg = "<svg version=\"1.1\" baseProfile=\"full\" width=\"300\" height=\"200\"\n" +
                            "        xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                    call.respondText(svg)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        post("/RAKIP-Web/execute/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val svg = "<svg version=\"1.1\" baseProfile=\"full\" width=\"300\" height=\"200\"\n" +
                            "        xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                    call.respondText(svg)

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

        get("/FSK-Web/search/{term}") {
            val matchingModelIndexes = mutableListOf<Int>()
            call.parameters["term"]?.let { term ->
                fskweb_rawMetadata.forEachIndexed { index, modelMetadata ->
                    if (modelMetadata.contains(term, ignoreCase = true)) {
                        matchingModelIndexes.add(index)
                    }
                }
            }

            call.respond(matchingModelIndexes)
        }
        get("/RAKIP-Web/search/{term}") {
            val matchingModelIndexes = mutableListOf<Int>()
            call.parameters["term"]?.let { term ->
                rakipweb_rawMetadata.forEachIndexed { index, modelMetadata ->
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
        get("/FSK-Web/simulations/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = fskweb_modelFiles[it]

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
        get("/RAKIP-Web/simulations/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = rakipweb_modelFiles[it]

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
        // endpoint to get the time for executing a default simulation
        // i = index
        get("/executionTime/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getView(index)
                view?.let { call.respond(it.durationTime) }
            }
        }

        get("/FSK-Web/executionTime/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getViewFskWeb(index)
                view?.let { call.respond(it.durationTime) }
            }
        }
        get("/RAKIP-Web/executionTime/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getViewRakipWeb(index)
                view?.let { call.respond(it.durationTime) }
            }
        }

        // endpoint to get the upload Date
        // i = index
        get("/uploadDate/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getView(index)
                view?.let { call.respond(it.uploadTime) }
            }
        }
        get("/FSK-Web/uploadDate/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getViewFskWeb(index)
                view?.let { call.respond(it.uploadTime) }
            }
        }
        get("/RAKIP-Web/uploadDate/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getViewRakipWeb(index)
                view?.let { call.respond(it.uploadTime) }
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
        try{
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
        } catch (e: CombineArchiveException){
        }

    }

    return metadata
}

private fun processMetadata(
    rawMetadata: List<String>,
    executionTimes: Map<String, String>,
    uploadTimes: Map<String, String>,
    baseUrl: String?
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
            downloadUrl = baseUrl?.let { "$it/download/$index" } ?: ""
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
 * UPDATE: if a userDefinedSim was given with the POST endpoint, then run that simulation and ignore simulationIndex
 */
fun FskModel.run(simulation:FskSimulation): String {

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
    runSelectedSimulation(engine, simulation)

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


/**
 * This function runs a simulation from the model file.
 * The simulation was given by POST execute endpoint or taken from the simulations from the model file
 */
fun FskModel.runSelectedSimulation(engine: RenjinScriptEngine, userDefinedSim: FskSimulation) {

    // Build parameters script
    val builder = StringBuilder()
    for ((parameterName, parameterValue) in userDefinedSim.parameters) {
        builder.append("$parameterName <- $parameterValue\n")
    }
    val parameterScript = builder.toString()

    engine.eval(parameterScript)
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1", module = Application::module).start(wait = true)
}