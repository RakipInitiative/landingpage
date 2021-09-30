import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.util.ArrayList



val MAPPER = ObjectMapper()


fun Application.databaseRoutes() {
    //************************

    //Database.connect("jdbc:h2:file:/home/thschuel/databases/H2_first;AUTO_SERVER=TRUE", driver = "org.h2.Driver")

    //transaction { SchemaUtils.create(RESULTS) }
    Database.connect("jdbc:h2:file:/home/thschuel/databases/repository_db2.h2;AUTO_SERVER=TRUE", driver = "org.h2.Driver",  password = "curation")

    transaction { SchemaUtils.create(RESULTS) }
    transaction { SchemaUtils.create(MODELS) }


    //*************************
    routing{
        handleGetRequests()
    }


}

fun Route.handleGetRequests(){
    val secureRandom = SecureRandom()
    val booleanRandomiser = { secureRandom.nextBoolean() }
    get("/flip") {
        handleFlipRequest(booleanRandomiser)
    }

    get("/outcomes") {
        handleGetOutcomesRequest()
    }
    get("/modelnames") {
        handleGetModelNamesRequest()
    }
    get("/metadataDB") {
        handleGetMetadataRequest()
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetOutcomesRequest() {
    var allOutcomes: List<Coin> = ArrayList()
    transaction {
        allOutcomes = RESULTS.selectAll().map { resultRow -> Coin(Face.valueOf(resultRow[RESULTS.face].toString())) }
    }.apply {
        call.respond(allOutcomes)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleFlipRequest(booleanRandomiser: () -> Boolean) {
    val result = booleanRandomiser.invoke()
    val faceValue: Face = if (result) Face.HEADS else Face.TAILS
    run {
        transaction {
            RESULTS.insert { it[face] = faceValue.name }
        }
    }.apply {
        call.respond(Coin(faceValue))
    }
}

data class Coin(var face: Face)

enum class Face { HEADS, TAILS }

object RESULTS : org.jetbrains.exposed.sql.Table() {
    val face = varchar("face", 5)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetModelNamesRequest() {
    var allOutcomes: List<String> = ArrayList()
    transaction {
        allOutcomes = MODELS.slice(MODELS.mName).selectAll().map { resultRow -> resultRow[MODELS.mName].toString() }
    }.apply {
        call.respond(allOutcomes)
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