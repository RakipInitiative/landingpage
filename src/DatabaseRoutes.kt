import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.bund.bfr.fskml.FSKML
import de.bund.bfr.landingpage.readModelScript
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.security.SecureRandom
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
    get("/DB/modelscript/{id}") {
        call.parameters["id"]?.toString()?.let {mId ->
            try {

                handleScriptRequest(mId)

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
        allOutcomes = MODELS.slice(property).select {MODELS.mId eq mId}.map { resultRow -> resultRow[property]}.firstOrNull().toString()
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
        filePath += MODELS.slice(MODELS.mFilePath).select {MODELS.mId eq mId}.map { resultRow -> resultRow[MODELS.mFilePath]}.first().toString()
    }.apply {
        call.response.header("Content-Disposition", "attachment; filename=${mId + ".fskx"}")
        call.respondFile(File(filePath))
    }
}

// Script & Readme Requests
private suspend fun PipelineContext<Unit, ApplicationCall>.handleScriptRequest(mId : String) {
    var filePath: String = "/home/thschuel/KNIME_TESTING_ENV/test_build/workspace/";
    var script = String()
    transaction {
        filePath += MODELS.slice(MODELS.mFilePath).select {MODELS.mId eq mId}.map { resultRow -> resultRow[MODELS.mFilePath]}.first().toString()
        val metadata = MODELS.slice(MODELS.mMetadata).select {MODELS.mId eq mId}.map { resultRow -> resultRow[MODELS.mMetadata]}.map{MAPPER.readTree(it) }.first()

        var language = metadata["generalInformation"]["languageWrittenIn"].asText();
        var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!
        script = readModelScript(File(filePath), uri)

    }.apply {

        call.respondText(script)
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