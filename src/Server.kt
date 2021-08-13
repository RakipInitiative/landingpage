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
    val views: MutableList<ModelView>,
    val uniqueEnvironments: MutableSet<String>,
    val uniqueHazards: MutableSet<String>,
    val uniqueSoftware: MutableSet<String>,
    val uniqueTypes: MutableSet<String>
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
    val imgFiles: MutableList<File>
    val rawMetadata: List<String>
    val processedMetadata: ProcessedMetadata?
    val parsedMetadata: List<JsonNode>
    val fskweb_modelFiles: MutableList<File>
    val fskweb_filesFolder: String?
    val fskweb_rawMetadata: MutableList<String>
    val fskweb_processedMetadata: ProcessedMetadata?
    val fskweb_parsedMetadata: MutableList<JsonNode>
    val rakipweb_modelFiles: MutableList<File>
    val rakipweb_filesFolder: String?
    val rakipweb_rawMetadata: MutableList<String>
    val rakipweb_processedMetadata: ProcessedMetadata?
    val rakipweb_parsedMetadata: MutableList<JsonNode>
    val baseUrl: String?
    val context: String

    if (testing) {
        modelFiles = emptyList()
        filesFolder = null
        imgFiles = mutableListOf()
        rawMetadata = emptyList()
        processedMetadata = null
        parsedMetadata = emptyList()
        fskweb_modelFiles = mutableListOf()
        fskweb_filesFolder = null
        fskweb_rawMetadata = mutableListOf()
        fskweb_processedMetadata = null
        fskweb_parsedMetadata = mutableListOf()
        rakipweb_modelFiles = mutableListOf()
        rakipweb_filesFolder = null
        rakipweb_rawMetadata = mutableListOf()
        rakipweb_processedMetadata = null
        rakipweb_parsedMetadata = mutableListOf()
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
        imgFiles = File(appConfiguration.getProperty("plot_folder")).walk().filter { it.isFile && it.extension == "svg" }.toMutableList()

        // Metadata
        rawMetadata = loadRawMetadata(modelFiles)
        parsedMetadata = rawMetadata.map { MAPPER.readTree(it) }

        // FSK_Web

        // Model files
        fskweb_filesFolder = appConfiguration.getProperty("fskweb_model_folder")
        fskweb_modelFiles = File(fskweb_filesFolder).walk().filter { it.isFile && it.extension == "fskx" && it.length() > 1000 }.toMutableList()
        fskweb_modelFiles.sort()

        // Metadata
        fskweb_rawMetadata = loadRawMetadata(fskweb_modelFiles).toMutableList()
        fskweb_parsedMetadata = fskweb_rawMetadata.map { MAPPER.readTree(it) }.toMutableList()


        // RAKIP-Web


        // Model files
        rakipweb_filesFolder = appConfiguration.getProperty("rakipweb_model_folder")
        rakipweb_modelFiles = File(rakipweb_filesFolder).walk().filter { it.isFile && it.extension == "fskx" && it.length() > 1000}.toMutableList()
        rakipweb_modelFiles.sort()

        // Metadata
        rakipweb_rawMetadata = loadRawMetadata(rakipweb_modelFiles).toMutableList()
        rakipweb_parsedMetadata = rakipweb_rawMetadata.map { MAPPER.readTree(it) }.toMutableList()




        baseUrl = appConfiguration.getProperty("base_url")

        context = appConfiguration.getProperty("context")?.let { "$it/" } ?: ""

        processedMetadata = processMetadata(rawMetadata, executionTimes, uploadTimes, baseUrl)
        fskweb_processedMetadata = processMetadata(fskweb_rawMetadata, executionTimes, uploadTimes, baseUrl)
        rakipweb_processedMetadata = processMetadata(rakipweb_rawMetadata, executionTimes, uploadTimes, baseUrl)
    }

    val representation = object {
        val endpoint = baseUrl ?: ""
        val resourcesFolder = context
        val rakip_endpoint = baseUrl ?: ""
        val rakip_resourcesFolder = context
        val fskweb_endpoint = baseUrl ?: ""
        val fskweb_resourcesFolder = context
    }

    /** Helper function for retrieving execution and upload times. */
    fun getView(index: Int) = processedMetadata?.let { it.views[index] }
    fun getViewFskWeb(index: Int) = fskweb_processedMetadata?.let { it.views[index] }
    fun getViewRakipWeb(index: Int) = rakipweb_processedMetadata?.let { it.views[index] }

    routing {
        get("/") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/RAKIP-Model-Repository") {
            call.respond(FreeMarkerContent("rakipweb.ftl", mapOf("representation" to representation), ""))
            //call.respondText("coming soon")
            //call.respondRedirect("/landingpage")
            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/RAKIP-Web-Model-Repository") {
            call.respondRedirect("/landingpage/RAKIP-Model-Repository")
            //call.respondText("coming soon")
            //call.respondRedirect("/landingpage")
            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/FSK-Web-Model-Repository") {
            call.respond(FreeMarkerContent("curated.ftl", mapOf("representation" to representation), ""))
            //call.respondText("coming soon")
            //call.respondRedirect("/landingpage")
            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/FSK-Model-Repository") {
            call.respondRedirect("/landingpage/FSK-Web-Model-Repository")
            //call.respondText("coming soon")
            //call.respondRedirect("/landingpage")
            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/disclaimer") {
            call.respond(FreeMarkerContent("disclaimer.ftl", mapOf("representation" to representation), ""))
            //call.respondText("coming soon")

            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/masthead") {
            call.respond(FreeMarkerContent("masthead.ftl", mapOf("representation" to representation), ""))
            //call.respondText("coming soon")

            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/dataProtectionDeclaration") {
            call.respond(FreeMarkerContent("dataProtectionDeclaration.ftl", mapOf("representation" to representation), ""))
            //call.respondText("coming soon")

            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/dataprotectionnotice") {
            call.respond(FreeMarkerContent("dataprotectionnotice.ftl", mapOf("representation" to representation), ""))
            //call.respondText("coming soon")

            //call.respond(FreeMarkerContent("index.ftl", mapOf("representation" to representation), ""))
        }
        get("/RAKIP-Web/disclaimer") {
            call.respondRedirect("/landingpage/disclaimer")
        }
        get("/RAKIP-Web/masthead") {
            call.respondRedirect("/landingpage/masthead")
        }
        get("/RAKIP-Web/dataProtectionDeclaration") {
            call.respondRedirect("/landingpage/dataProtectionDeclaration")

        }
        get("/RAKIP-Web/dataprotectionnotice") {
            call.respondRedirect("/landingpage/dataprotectionnotice")
        }
        get("/FSK-Web/disclaimer") {
            call.respondRedirect("/landingpage/disclaimer")
        }
        get("/FSK-Web/masthead") {
            call.respondRedirect("/landingpage/masthead")
        }
        get("/FSK-Web/dataProtectionDeclaration") {
            call.respondRedirect("/landingpage/dataProtectionDeclaration")

        }
        get("/FSK-Web-/dataprotectionnotice") {
            call.respondRedirect("/landingpage/dataprotectionnotice")
        }

        get("/FSK-Web-Model-Repository/disclaimer") {
            call.respondRedirect("/landingpage/disclaimer")
        }
        get("/FSK-Web-Model-Repository/masthead") {
            call.respondRedirect("/landingpage/masthead")
        }
        get("/FSK-Web-Model-Repository/dataProtectionDeclaration") {
            call.respondRedirect("/landingpage/dataProtectionDeclaration")

        }
        get("/FSK-Web-Model-Repository/dataprotectionnotice") {
            call.respondRedirect("/landingpage/dataprotectionnotice")
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

        get("/FSK-Web-Model-Repository/download/{i}") {
            var curated_modelFiles: MutableList<File> = mutableListOf()
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = curated_modelFiles[it]
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

        get("/FSK-Web-Model-Repository/metadata") {
            call.respond(emptyList<JsonNode>())
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

        get("/FSK-Web-Model-Repository/metadata/{i}") {
            var curated_parsedMetadata = mutableListOf<JsonNode>()
            call.parameters["i"]?.toInt()?.let {
                call.respond(curated_parsedMetadata[it])
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
                    val imgFile = imgFiles.first { it.nameWithoutExtension.replace("/","").replace(":","") == imageId.replace("/","").replace(":","") }
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
                    val imgFile = imgFiles.first { it.nameWithoutExtension.replace("/","").replace(":","") == imageId.replace("/","").replace(":","") }
                    call.response.header("Content-Disposition", "inline")
                    call.respondText(imgFile.readText())
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("/FSK-Web-ModelRepository/image/{id}") {
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
        get("/modelscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    var language = parsedMetadata[it]["generalInformation"]["languageWrittenIn"].asText();
                    var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
                    val model = readModelScript(modelFile, uri)
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
                    var language = fskweb_parsedMetadata[it]["generalInformation"]["languageWrittenIn"].asText();
                    var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
                    val model = readModelScript(modelFile, uri)
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
                    var language = rakipweb_parsedMetadata[it]["generalInformation"]["languageWrittenIn"].asText();
                    var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
                    val model = readModelScript(modelFile, uri)
                    call.respondText(model)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/FSK-Web-Model-Repository/modelscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    call.respondText("")
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/visualizationscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    var language = parsedMetadata[it]["generalInformation"]["languageWrittenIn"].asText();
                    var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
                    val visualizationScript = readVisualizationScript(modelFile, uri)
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
                    var language = fskweb_parsedMetadata[it]["generalInformation"]["languageWrittenIn"].asText();
                    var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
                    val visualizationScript = readVisualizationScript(modelFile, uri)
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
                    var language = rakipweb_parsedMetadata[it]["generalInformation"]["languageWrittenIn"].asText();
                    var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
                    val visualizationScript = readVisualizationScript(modelFile, uri)
                    call.respondText(visualizationScript)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/readme/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = modelFiles[it]
                    var uri = FSKML.getURIS(1, 0, 12)["plain"]!!
                    val readme = readReadMe(modelFile, uri)
                    call.respondText(readme)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/FSK-Web/readme/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = fskweb_modelFiles[it]
                    var uri = FSKML.getURIS(1, 0, 12)["plain"]!!
                    val readme = readReadMe(modelFile, uri)
                    call.respondText(readme)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/RAKIP-Web/readme/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    val modelFile = rakipweb_modelFiles[it]
                    var uri = FSKML.getURIS(1, 0, 12)["plain"]!!
                    val readme = readReadMe(modelFile, uri)
                    call.respondText(readme)
                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        get("/FSK-Web-Model-Repository/visualizationscript/{i}") {
            call.parameters["i"]?.toInt()?.let {
                try {
                    call.respondText("")
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
        post("/FSK-Web-Model-Repository/execute/{i}") {
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

        get("/FSK-Web-Model-Repository/search/{term}") {
            val matchingModelIndexes = mutableListOf<Int>()
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

        get("/FSK-Web-Model-Repository/simulations/{i}") {
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

        get("/executionTime") {
            try{
                val appConfiguration = loadConfiguration()
                // Times
                val timesFile = appConfiguration.getProperty("times_csv")

                val temporaryUploadTimes = mutableMapOf<String, String>()
                val temporaryExecutionTimes = mutableMapOf<String, String>()
                File(timesFile).readLines().forEach {
                    val tokens = it.split(",")
                    val mId = tokens[0]
                    temporaryUploadTimes[mId] = tokens[2]
                    temporaryExecutionTimes[mId] = tokens[1]
                }
                val executionTimes = temporaryExecutionTimes.toMap()
                val uploadTimes = temporaryUploadTimes.toMap()
                call.respond(executionTimes)
            } catch (err: NullPointerException) {
                call.respond(HttpStatusCode.NotFound)
            }

        }
        get("/FSK-Web/executionTime") {
            call.respondRedirect("/landingpage/executionTime")
        }
        get("/RAKIP-Web/executionTime") {
            call.respondRedirect("/landingpage/executionTime")
        }
        get("/FSK-Web-Model-Repository/executionTime") {
            call.respondRedirect("/landingpage/executionTime")
        }
        get("/uploadDate") {
            try{
                val appConfiguration = loadConfiguration()
                // Times
                val timesFile = appConfiguration.getProperty("times_csv")

                val temporaryUploadTimes = mutableMapOf<String, String>()
                val temporaryExecutionTimes = mutableMapOf<String, String>()
                File(timesFile).readLines().forEach {
                    val tokens = it.split(",")
                    val mId = tokens[0]
                    temporaryUploadTimes[mId] = tokens[2]
                    temporaryExecutionTimes[mId] = tokens[1]
                }
                val executionTimes = temporaryExecutionTimes.toMap()
                val uploadTimes = temporaryUploadTimes.toMap()
                call.respond(uploadTimes)
            } catch (err: NullPointerException) {
                call.respond(HttpStatusCode.NotFound)
            }

        }
        get("/FSK-Web/uploadDate") {
            call.respondRedirect("/landingpage/uploadDate")
        }
        get("/RAKIP-Web/uploadDate") {
            call.respondRedirect("/landingpage/uploadDate")
        }
        get("/FSK-Web-Model-Repository/uploadDate") {
            call.respondRedirect("/landingpage/uploadDate")
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

        get("/FSK-Web-Model-Repository/executionTime/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getViewFskWeb(index)
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

        get("/FSK-Web-Model-Repository/uploadDate/{i}") {
            call.parameters["i"]?.toInt()?.let { index ->
                val view = getViewFskWeb(index)
                view?.let { call.respond(it.uploadTime) }
            }
        }

        // endpoint to add image to the service
        post("/addImage/{id}") {
            call.parameters["id"]?.let { imageId ->
                try {
                    val appConfiguration = loadConfiguration()
                    imgFiles.add(File(appConfiguration.getProperty("plot_folder") + "/" + imageId + ".svg"))
                    call.respond(HttpStatusCode.Accepted)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // endpoint to add model ID to the service (FSK-Web)
        post("/FSK-Web/addModel/{id}") {
            call.parameters["id"]?.let { modelId ->
                try {
                    val appConfiguration = loadConfiguration()
                    val folder = appConfiguration.getProperty("fskweb_model_folder")
                    fskweb_modelFiles.add(File(folder + "/" + modelId + ".fskx"))

                    // Metadata
                    var new_model = loadRawMetadata(listOf(File(folder + "/" + modelId + ".fskx"))).toMutableList()
                    fskweb_rawMetadata.addAll(new_model)
                    fskweb_parsedMetadata.addAll(new_model.map { MAPPER.readTree(it) }.toMutableList())


                    // Times
                    val timesFile = appConfiguration.getProperty("times_csv")

                    val temporaryUploadTimes = mutableMapOf<String, String>()
                    val temporaryExecutionTimes = mutableMapOf<String, String>()
                    File(timesFile).readLines().forEach {
                        val tokens = it.split(",")
                        val mId = tokens[0]
                        temporaryUploadTimes[mId] = tokens[2]
                        temporaryExecutionTimes[mId] = tokens[1]
                    }
                    val executionTimes = temporaryExecutionTimes.toMap()
                    val uploadTimes = temporaryUploadTimes.toMap()

                    addProcessMetadata(fskweb_rawMetadata, executionTimes, uploadTimes, baseUrl, fskweb_processedMetadata)
                    //fskweb_parsedMetadata.add(new_model.map { MAPPER.readTree(it) }.toMutableList())

                    call.respond(HttpStatusCode.Accepted)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // endpoint to add model ID to the service (FSK-Web)
        post("/RAKIP-Web/addModel/{id}") {
            call.parameters["id"]?.let { modelId ->
                try {
                    val appConfiguration = loadConfiguration()
                    val folder = appConfiguration.getProperty("rakipweb_model_folder")
                    rakipweb_modelFiles.add(File(folder + "/" + modelId + ".fskx"))

                    // Metadata
                    var new_model = loadRawMetadata(listOf(File(folder + "/" + modelId + ".fskx"))).toMutableList()
                    rakipweb_rawMetadata.addAll(new_model)
                    rakipweb_parsedMetadata.addAll(new_model.map { MAPPER.readTree(it) })

                    // Times
                    val timesFile = appConfiguration.getProperty("times_csv")

                    val temporaryUploadTimes = mutableMapOf<String, String>()
                    val temporaryExecutionTimes = mutableMapOf<String, String>()
                    File(timesFile).readLines().forEach {
                        val tokens = it.split(",")
                        val mId = tokens[0]
                        temporaryUploadTimes[mId] = tokens[2]
                        temporaryExecutionTimes[mId] = tokens[1]
                    }
                    val executionTimes = temporaryExecutionTimes.toMap()
                    val uploadTimes = temporaryUploadTimes.toMap()

                    addProcessMetadata(rakipweb_rawMetadata, executionTimes, uploadTimes, baseUrl, rakipweb_processedMetadata)
                    call.respond(HttpStatusCode.Accepted)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // endpoint to add model ID to the service (FSK-Web)
        post("/FSK-Web/updateRepository") {
            try {
                val appConfiguration = loadConfiguration()
                val folder = appConfiguration.getProperty("fskweb_model_folder")

                // clear metadata
                fskweb_modelFiles.clear()
                fskweb_rawMetadata.clear()
                fskweb_parsedMetadata.clear()

                // reload folder content
                // Model files

                fskweb_modelFiles.addAll(File(fskweb_filesFolder).walk().filter { it.isFile && it.extension == "fskx" && it.length() > 1000}.toMutableList())
                fskweb_modelFiles.sort()

                // Metadata
                fskweb_rawMetadata.addAll(loadRawMetadata(fskweb_modelFiles).toMutableList())
                fskweb_parsedMetadata.addAll(fskweb_rawMetadata.map { MAPPER.readTree(it) }.toMutableList())

                // Times
                val timesFile = appConfiguration.getProperty("times_csv")

                val temporaryUploadTimes = mutableMapOf<String, String>()
                val temporaryExecutionTimes = mutableMapOf<String, String>()
                File(timesFile).readLines().forEach {
                    val tokens = it.split(",")
                    val mId = tokens[0]
                    temporaryUploadTimes[mId] = tokens[2]
                    temporaryExecutionTimes[mId] = tokens[1]
                }
                val executionTimes = temporaryExecutionTimes.toMap()
                val uploadTimes = temporaryUploadTimes.toMap()

                addProcessMetadata(fskweb_rawMetadata, executionTimes, uploadTimes, baseUrl, fskweb_processedMetadata)
                call.respond(HttpStatusCode.Accepted)

            } catch (err: IndexOutOfBoundsException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        // endpoint to add model ID to the service (FSK-Web)
        post("/RAKIP-Web/updateRepository") {
            try {
                val appConfiguration = loadConfiguration()
                val folder = appConfiguration.getProperty("rakipweb_model_folder")

                // clear metadata
                rakipweb_modelFiles.clear()
                rakipweb_rawMetadata.clear()
                rakipweb_parsedMetadata.clear()

                // reload folder content
                // Model files

                rakipweb_modelFiles.addAll(File(rakipweb_filesFolder).walk().filter { it.isFile && it.extension == "fskx" && it.length() > 1000}.toMutableList())
                rakipweb_modelFiles.sort()

                // Metadata
                rakipweb_rawMetadata.addAll(loadRawMetadata(rakipweb_modelFiles).toMutableList())
                rakipweb_parsedMetadata.addAll(rakipweb_rawMetadata.map { MAPPER.readTree(it) }.toMutableList())

                // Times
                val timesFile = appConfiguration.getProperty("times_csv")

                val temporaryUploadTimes = mutableMapOf<String, String>()
                val temporaryExecutionTimes = mutableMapOf<String, String>()
                File(timesFile).readLines().forEach {
                    val tokens = it.split(",")
                    val mId = tokens[0]
                    temporaryUploadTimes[mId] = tokens[2]
                    temporaryExecutionTimes[mId] = tokens[1]
                }
                val executionTimes = temporaryExecutionTimes.toMap()
                val uploadTimes = temporaryUploadTimes.toMap()

                addProcessMetadata(rakipweb_rawMetadata, executionTimes, uploadTimes, baseUrl, rakipweb_processedMetadata)
                call.respond(HttpStatusCode.Accepted)

            } catch (err: IndexOutOfBoundsException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        // endpoint to remove modelf from FSK-Web repository
        post("/FSK-Web/removeModel/{id}") {
            call.parameters["id"]?.let { modelId ->
                try {
                    val appConfiguration = loadConfiguration()
                    val folder = appConfiguration.getProperty("fskweb_model_folder")
                    
                    fskweb_modelFiles.remove(File(folder + "/" + modelId + ".fskx"))

                    // Metadata
                    var new_model = loadRawMetadata(listOf(File(folder + "/" + modelId + ".fskx"))).toMutableList()
                    fskweb_rawMetadata.removeAll(new_model)
                    fskweb_parsedMetadata.removeAll(new_model.map { MAPPER.readTree(it) }.toMutableList())



                    // Times
                    val timesFile = appConfiguration.getProperty("times_csv")

                    val temporaryUploadTimes = mutableMapOf<String, String>()
                    val temporaryExecutionTimes = mutableMapOf<String, String>()
                    File(timesFile).readLines().forEach {
                        val tokens = it.split(",")
                        val mId = tokens[0]
                        if(mId != modelId){
                            temporaryUploadTimes[mId] = tokens[2]
                            temporaryExecutionTimes[mId] = tokens[1]    
                        }
                    }
                    val executionTimes = temporaryExecutionTimes.toMap()
                    val uploadTimes = temporaryUploadTimes.toMap()

                    addProcessMetadata(fskweb_rawMetadata, executionTimes, uploadTimes, baseUrl, fskweb_processedMetadata)

                    //deletion not possible on server, using knime node
                    //File(folder + "/" + modelId + ".fskx").delete()
                    // delete image file
                    imgFiles.remove(File(appConfiguration.getProperty("plot_folder") + "/" + modelId + ".svg"))
                    //File(appConfiguration.getProperty("plot_folder") + "/" + modelId + ".svg").delete()
                    call.respond(HttpStatusCode.Accepted)

                } catch (err: IndexOutOfBoundsException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        // endpoint to remove modelf from RAKIP-Web repository
        post("/RAKIP-Web/removeModel/{id}") {
            call.parameters["id"]?.let { modelId ->
                try {
                    val appConfiguration = loadConfiguration()
                    val folder = appConfiguration.getProperty("rakipweb_model_folder")

                    rakipweb_modelFiles.remove(File(folder + "/" + modelId + ".fskx"))

                    // Metadata
                    var new_model = loadRawMetadata(listOf(File(folder + "/" + modelId + ".fskx"))).toMutableList()
                    rakipweb_rawMetadata.removeAll(new_model)
                    rakipweb_parsedMetadata.removeAll(new_model.map { MAPPER.readTree(it) }.toMutableList())



                    // Times
                    val timesFile = appConfiguration.getProperty("times_csv")

                    val temporaryUploadTimes = mutableMapOf<String, String>()
                    val temporaryExecutionTimes = mutableMapOf<String, String>()
                    File(timesFile).readLines().forEach {
                        val tokens = it.split(",")
                        val mId = tokens[0]
                        if(mId != modelId){
                            temporaryUploadTimes[mId] = tokens[2]
                            temporaryExecutionTimes[mId] = tokens[1]
                        }
                    }
                    val executionTimes = temporaryExecutionTimes.toMap()
                    val uploadTimes = temporaryUploadTimes.toMap()

                    addProcessMetadata(rakipweb_rawMetadata, executionTimes, uploadTimes, baseUrl, rakipweb_processedMetadata)

                    //deletion not possible on server, using knime node
                    //File(folder + "/" + modelId + ".fskx").delete()
                    // delete image file
                    imgFiles.remove(File(appConfiguration.getProperty("plot_folder") + "/" + modelId + ".svg"))
                    //File(appConfiguration.getProperty("plot_folder") + "/" + modelId + ".svg").delete()


                    call.respond(HttpStatusCode.Accepted)

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
            software = generalInformation["software"]?.asText()?:"R",
            environment,
            hazard,
            modelType = metadataTree["modelType"].asText(),
            durationTime = executionTimes[modelId] ?: "", // Get durationTime from executionTimes dictionary
            uploadTime = uploadTimes[modelId] ?: "", // Get upload time from uploadTimes dictionary
            downloadUrl = baseUrl?.let { "$it/download/$index" } ?: ""
        )
    }.toMutableList()

    return ProcessedMetadata(
        modelViews,
        uniqueEnvironments = modelViews.flatMap { it.environment }.toMutableSet(),
        uniqueHazards = modelViews.flatMap { it.hazard }.toMutableSet(),
        uniqueSoftware = modelViews.map { it.software }.toMutableSet(),
        uniqueTypes = modelViews.map { it.modelType }.toMutableSet()
    )
}
private fun addProcessMetadata(
        rawMetadata: List<String>,
        executionTimes: Map<String, String>,
        uploadTimes: Map<String, String>,
        baseUrl: String?,
        processedMetadata: ProcessedMetadata?
) {

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
                software = generalInformation["software"]?.asText()?:"R",
                environment,
                hazard,
                modelType = metadataTree["modelType"].asText(),
                durationTime = executionTimes[modelId] ?: "", // Get durationTime from executionTimes dictionary
                uploadTime = uploadTimes[modelId] ?: "", // Get upload time from uploadTimes dictionary
                downloadUrl = baseUrl?.let { "$it/download/$index" } ?: ""
        )
    }.toMutableList()
    processedMetadata?.views?.clear()
    processedMetadata?.uniqueEnvironments?.clear()
    processedMetadata?.uniqueHazards?.clear()
    processedMetadata?.uniqueSoftware?.clear()
    processedMetadata?.uniqueTypes?.clear()
    processedMetadata?.views?.addAll(modelViews)
    processedMetadata?.uniqueEnvironments?.addAll(modelViews.flatMap { it.environment })
    processedMetadata?.uniqueHazards?.addAll(modelViews.flatMap { it.hazard })
    processedMetadata?.uniqueSoftware?.addAll(modelViews.map { it.software })
    processedMetadata?.uniqueTypes?.addAll(modelViews.map { it.modelType })

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

private fun readModelScript(modelFile: File, uri:URI): String {

    return CombineArchive(modelFile).use {
        it.getEntriesWithFormat(uri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == FskMetaDataObject.ResourceType.modelScript
        }.loadTextEntry()
    }
}

private fun readVisualizationScript(modelFile: File,uri:URI): String {

    return CombineArchive(modelFile).use {
        it.getEntriesWithFormat(uri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == FskMetaDataObject.ResourceType.visualizationScript
        }.loadTextEntry()
    }
}
private fun readReadMe(modelFile: File,uri:URI): String {

    return CombineArchive(modelFile).use {
        it.getEntriesWithFormat(uri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == FskMetaDataObject.ResourceType.readme
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