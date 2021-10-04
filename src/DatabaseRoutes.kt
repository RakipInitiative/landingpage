import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.bund.bfr.fskml.FSKML
import de.bund.bfr.fskml.FskMetaDataObject
import de.bund.bfr.landingpage.loadTextEntry
import de.unirostock.sems.cbarchive.CombineArchive
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URI
import java.util.ArrayList



val MAPPER = ObjectMapper()


fun Application.databaseRoutes() {
    //************************

    //Database.connect("jdbc:h2:file:/home/thschuel/databases/H2_first;AUTO_SERVER=TRUE", driver = "org.h2.Driver")

    //transaction { SchemaUtils.create(RESULTS) }
    Database.connect("jdbc:h2:file:/home/thschuel/databases/repository_db2.h2;AUTO_SERVER=TRUE", driver = "org.h2.Driver",  password = "curation")
    transaction { SchemaUtils.create(MODELS) }


    //*************************
    routing{
        handleGetRequests()
    }


}

fun Route.handleGetRequests(){
    get("/DB/test/{id}") {
        try {
            call.parameters["id"]?.let { id ->
                val rep = call.request.queryParameters["repository"]
                val status = call.request.queryParameters["status"]


                call.respondText("$id:  '$rep' has the following message: ${Repository.FSK_WEB.rep}")
            }
        } catch (err: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }
    get("/DB/metadata") {
        handleGetMetadataRequest()
    }
    get("/DB/executionTime") {
        handleGetDateTimeRequest(MODELS.mExecutionTime)
    }
    get("/DB/uploadDate") {
        handleGetDateTimeRequest(MODELS.mUploadDate)
    }
    get("/DB/metadata/{id}") {
        call.parameters["id"]?.let { id ->
            handleGetPropertyByIdRequest(id, MODELS.mMetadata)
        }
    }
    get("/DB/doi/{id}") {
        call.parameters["id"]?.let { id ->
            handleGetPropertyByIdRequest(id, MODELS.mDoi)
        }
    }
    get("/DB/download/{id}") {
        call.parameters["id"]?.toString()?.let {mId ->
            try {
                handleDownloadRequest(mId)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/image/{id}") {
        call.parameters["id"]?.toString()?.let {mId ->
            try {
                handleImageRequest(mId)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/modelscript/{id}") {
        call.parameters["id"]?.toString()?.let {mId ->
            try {
                handleScriptRequest(mId, FskMetaDataObject.ResourceType.modelScript)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/visualizationscript/{id}") {
        call.parameters["id"]?.toString()?.let {mId ->
            try {
                handleScriptRequest(mId, FskMetaDataObject.ResourceType.visualizationScript)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/readme/{id}") {
        call.parameters["id"]?.toString()?.let {mId ->
            try {
                handleScriptRequest(mId, FskMetaDataObject.ResourceType.readme)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/search/{term}") {
        call.parameters["term"]?.toString()?.let {term ->
            try {
                handleSearchRequest(term)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}




private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetMetadataRequest() {
    var allOutcomes: List<JsonNode> = ArrayList()
    transaction {
        allOutcomes = MODELS.slice(MODELS.mMetadata).selectAll().map { resultRow -> resultRow[MODELS.mMetadata]}.map{MAPPER.readTree(it) }
    }.apply {
        call.respond(allOutcomes)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetDateTimeRequest(property: Column<String?>) {
    var result = mutableMapOf<String, String>()
    transaction {

        MODELS.slice(MODELS.mId, property).selectAll().map {it[MODELS.mId] +"," + it[property]}.forEach {
            val tokens = it.split(",")
            val mId = tokens[0]
            result[mId] = if (tokens[1] != "null") tokens[1] else ""
        }
    }.apply {
        call.respond(result)
    }
}
private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetPropertyByIdRequest(mId: String, property: Column<String?>) {
    var allOutcomes: String = String()
    transaction {
        allOutcomes = MODELS.slice(property).select {whereFilter(mId)}.map { resultRow -> resultRow[property]}.firstOrNull().toString()
    }.apply {
        if(allOutcomes.equals("null")) {
            call.respond("")
        } else {
            call.respond(allOutcomes)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDownloadRequest(mId : String) {
    var filePath: String = "/home/thschuel/KNIME_TESTING_ENV/test_build/workspace/";
    transaction {
        filePath += MODELS.slice(MODELS.mFilePath).select {whereFilter(mId)}.map { resultRow -> resultRow[MODELS.mFilePath]}.first().toString()
    }.apply {
        call.response.header("Content-Disposition", "attachment; filename=${mId + ".fskx"}")
        call.respondFile(File(filePath))
    }
}

// image
private suspend fun PipelineContext<Unit, ApplicationCall>.handleImageRequest(mId : String) {
    var filePath: String = "/home/thschuel/KNIME_TESTING_ENV/test_build/workspace/";
    transaction {
        filePath += MODELS.slice(MODELS.mPlotPath).select {whereFilter(mId)}.map { resultRow -> resultRow[MODELS.mPlotPath]}.first().toString()
    }.apply {
        call.response.header("Content-Disposition", "inline")
        call.respondText(File(filePath).readText())
    }
}

// Script & Readme Requests
private suspend fun PipelineContext<Unit, ApplicationCall>.handleScriptRequest(mId : String,
                                                                               scriptResource: FskMetaDataObject.ResourceType) {
    var filePath: String = "/home/thschuel/KNIME_TESTING_ENV/test_build/workspace/";
    var script = String()
    transaction {
        filePath += MODELS.slice(MODELS.mFilePath).select {whereFilter(mId)}.map { resultRow -> resultRow[MODELS.mFilePath]}.first().toString()
        val metadata = MODELS.slice(MODELS.mMetadata).select {whereFilter(mId)}.map { resultRow -> resultRow[MODELS.mMetadata]}.map{MAPPER.readTree(it) }.first()

        var language = metadata["generalInformation"]["languageWrittenIn"].asText();

        // URI model & visualization script
        var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!

        // URI readme
        if(scriptResource.equals(FskMetaDataObject.ResourceType.readme)){
            uri = FSKML.getURIS(1, 0, 12)["plain"]!!
        }
        script = readScript(File(filePath), uri, scriptResource)

    }.apply {

        call.respondText(script)
    }
}

private fun readScript(modelFile: File, uri: URI, scriptResource : FskMetaDataObject.ResourceType): String {
    var x = 22;
    var result = CombineArchive(modelFile).use {
        it.getEntriesWithFormat(uri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == scriptResource
        }.loadTextEntry()
    }
    return result;
}
private suspend fun PipelineContext<Unit, ApplicationCall>.handleSearchRequest(term: String) {
    var metadataList: List<String> = ArrayList()
    val matchingModelIndexes = mutableListOf<Int>()
    transaction {
        metadataList = MODELS.slice(MODELS.mMetadata).selectAll().map { resultRow -> resultRow[MODELS.mMetadata].toString()}
        metadataList.forEachIndexed { index, modelMetadata ->
            if (modelMetadata.contains(term, ignoreCase = true)) {
                matchingModelIndexes.add(index)
            }
        }
    }.apply {
        call.respond(matchingModelIndexes)
    }
}
object MODELS : org.jetbrains.exposed.sql.Table("Models") {
    val mId = varchar("UUID", 255).uniqueIndex()
    val mName = varchar("ModelName", 255).nullable()
    val mDoi = varchar("DOI", 255).nullable()
    val mConceptDoi = varchar("ConceptDOI", 255).nullable()
    val mStatus = varchar("Status", 255).nullable()
    val mRepository = varchar("Repository", 255).nullable()
    val mFilePath = varchar("FilePath", 255).nullable()
    val mPlotPath = varchar("PlotPath", 255).nullable()
    val mMetadata = varchar("Metadata", 200000).nullable()
    val mExecutionTime = varchar("ExecutionTime", 255).nullable()
    val mUploadDate = varchar("UploadDate", 255).nullable()
    val mUploadedBy = varchar("UploadedBy", 255).nullable()
}

enum class Repository (val rep: String){
    RAKIP_WEB ("RAKIP-Web"),
    FSK_WEB("FSK-Web"),
    RENJIN("Renjin"),
    TRASHBIN("Trash-Bin")
}

enum class Status(val status: String){
    UNCURATED("Uncurated"),
    CURATED("Curated")
}
// FILTER BY REPOSITORY AND STATUS
private fun whereFilter(mId: String,
                        repository: Repository=Repository.FSK_WEB,
                        status: Status=Status.UNCURATED): Op<Boolean>{

    return (MODELS.mId eq mId) and (MODELS.mRepository eq repository.rep);
}