import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.bund.bfr.landingpage.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notLike
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.security.MessageDigest
import java.util.ArrayList
import java.io.FileInputStream
import java.io.InputStream


val MAPPER = ObjectMapper()
val appConfiguration = loadConfiguration()

val basePath: String = appConfiguration.getProperty("base_path") //"/home/thschuel/KNIME_TESTING_ENV/test_build/workspace/"
val repositoryName: String =appConfiguration.getProperty("repository_name") //"RepositoryDB"
fun Application.databaseRoutes() {
    //************************

    //Database.connect("jdbc:h2:file:/home/thschuel/databases/H2_first;AUTO_SERVER=TRUE", driver = "org.h2.Driver")

    //transaction { SchemaUtils.create(RESULTS) }
    Database.connect("jdbc:h2:${basePath}${repositoryName}/repository_db.h2;AUTO_SERVER=TRUE", driver = "org.h2.Driver",  password = "curation")
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
        call.parameters["id"]?.let { mId ->
            try {
                handleDownloadRequest(mId)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/image/{id}") {
        call.parameters["id"]?.let { mId ->
            try {
                handleImageRequest(mId)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/modelscript/{id}") {
        call.parameters["id"]?.let { mId ->
            try {
                handleGetPropertyByIdRequest(mId,MODELS.mModelScript)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/visualizationscript/{id}") {
        call.parameters["id"]?.let { mId ->
            try {
                handleGetPropertyByIdRequest(mId,MODELS.mVisualizationScript)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/readme/{id}") {
        call.parameters["id"]?.let { mId ->
            try {
                handleGetPropertyByIdRequest(mId,MODELS.mReadme)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    get("/DB/simulations/{id}") {
        call.parameters["id"]?.let { mId ->
            try {
                handleGetPropertyByIdRequest(mId,MODELS.mSimulations)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    get("/DB/search/{term}") {
        call.parameters["term"]?.let { term ->
            try {
                handleSearchRequest(term)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    // Search for Zenodo DOI : Sandbox.zenodo
    get("/DB/search/10.5072/{term}") {
        call.parameters["term"]?.let { term ->
            try {
                handleSearchRequest(term)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    // Search for Zenodo DOI : Zenodo.org
    get("/DB/search/10.5281/{term}") {
        call.parameters["term"]?.let { term ->
            try {
                handleSearchRequest(term)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    // endpoint to run models with a posted simulation in the body(JSON)
    post("/DB/execute/{id}") {
        //val sim = call.receive<FskSimulation>()
        call.parameters["id"]?.let { mId ->
            try {
                handleExecutionRequest(mId)
            } catch (err: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}




private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetMetadataRequest() {
    var allOutcomes: List<JsonNode> = ArrayList()

    val parameters = getParameters(call)
    transaction {
        allOutcomes = MODELS.slice(MODELS.mMetadata)
            .select{whereFilter(repository = parameters.first, status = parameters.second, filter = parameters.third)}
            .map { resultRow -> resultRow[MODELS.mMetadata]}.map{MAPPER.readTree(it) }
    }.apply {
        call.respond(allOutcomes)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetDateTimeRequest(property: Column<String?>) {
    val result = mutableMapOf<String, String>()
    val parameters = getParameters(call)
    transaction {

        MODELS.slice(MODELS.mId, property)
            .select{whereFilter(repository = parameters.first, status = parameters.second, filter = parameters.third)}
            .map {it[MODELS.mId] +"," + it[property]}.forEach {
            val tokens = it.split(",")
            val mId = tokens[0]
            result[mId] = if (tokens[1] != "null") tokens[1] else ""
        }
    }.apply {
        call.respond(result)
    }
}
private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetPropertyByIdRequest(mId: String,
                                                                                        property: Column<String?>) {
    var result = String()
    val parameters = getParameters(call)
    transaction {
        result = MODELS.slice(property)
            .select{whereFilter(mId= mId, repository = parameters.first, status = parameters.second, filter = parameters.third)}
            .map { resultRow -> resultRow[property]}.firstOrNull().toString()
    }.apply {
        if(result == "null") {
            call.respond("")
        } else {
            call.respond(result)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleDownloadRequest(mId : String) {
    var filePath: String = basePath
    val parameters = getParameters(call)
    transaction {
        val result = MODELS.slice(MODELS.mFilePath)
            .select{whereFilter(mId= mId, repository = parameters.first, status = parameters.second, filter = parameters.third)}
            .map { resultRow -> resultRow[MODELS.mFilePath]}.first()
        result?.let {
            filePath +=  result.toString()
        }
    }.apply {
        call.response.header("Content-Disposition", "attachment; filename=${"$mId.fskx"}")
        call.respondFile(File(filePath))
    }
}

// image
private suspend fun PipelineContext<Unit, ApplicationCall>.handleImageRequest(mId : String) {
    var filePath: String = basePath
    val parameters = getParameters(call)
    var svg = "<svg version=\"1.1\" baseProfile=\"full\" width=\"300\" height=\"200\"\n" +
            "        xmlns=\"http://www.w3.org/2000/svg\"></svg>"
    transaction {
        val result = MODELS.slice(MODELS.mPlotPath)
            .select{whereFilter(mId= mId, repository = parameters.first, status = parameters.second, filter = parameters.third)}
            .map { resultRow -> resultRow[MODELS.mPlotPath]}.first()
        result?.let {
            filePath += result.toString()
            svg = File(filePath).readText()
        }
    }.apply {
        call.response.header("Content-Disposition", "inline")
        call.respondText(svg)
    }
}

// Deprecated since scripts are stored in DB
/*private suspend fun PipelineContext<Unit, ApplicationCall>.handleScriptRequest(mId : String,
                                                                               scriptResource: FskMetaDataObject.ResourceType) {
    var filePath: String = basePath;
    var script = String()
    val parameters = getParameters(call);
    transaction {
        val result = MODELS.slice(MODELS.mFilePath).select{whereFilter(mId,parameters.first,parameters.second)}
            .map { resultRow -> resultRow[MODELS.mFilePath]}.firstOrNull()
        result?.let{
            filePath += result
            MODELS.slice(MODELS.mMetadata).select{whereFilter(mId,parameters.first,parameters.second)}
                    .map { resultRow -> resultRow[MODELS.mMetadata]}.map{MAPPER.readTree(it) }.firstOrNull()?.let{metadata ->
                        var language = metadata?.get("generalInformation")?.get("languageWrittenIn")?.asText()?.let { it } ?: "r";

                        // URI model & visualization script
                        var uri = if (language.startsWith("py",ignoreCase = true)) FSKML.getURIS(1, 0, 12)["py"]!! else FSKML.getURIS(1, 0, 12)["r"]!!

                        // URI readme
                        if(scriptResource.equals(FskMetaDataObject.ResourceType.readme)){
                            uri = FSKML.getURIS(1, 0, 12)["plain"]!!
                        }
                        script = readScript(File(filePath), uri, scriptResource)

                    }
        }

    }.apply {

        call.respondText(script)
    }
}*/
// Execute model with Simulation (POST); RENJIN ONLY!
private suspend fun PipelineContext<Unit, ApplicationCall>.handleExecutionRequest(mId : String) {
    var filePath: String = basePath


    //var script = String()
    val parameters = getParameters(call)
    if(parameters.first == Repository.RENJIN){
        val sim = call.receive<FskSimulation>()
        transaction {
            val result = MODELS.slice(MODELS.mFilePath)
                .select{whereFilter(mId= mId, repository = parameters.first, status = parameters.second, filter = parameters.third)}
                .map { resultRow -> resultRow[MODELS.mFilePath]}.firstOrNull()
            result?.let{
                filePath += result



            }

        }.apply {
            // Calculate CHECKSUM to see if model file in DB is still valid
            //val check = createChecksum(filePath)?.fold("", { str, it -> str + "%02x".format(it) })
            val model = readModel(File(filePath))
            call.respondText(model.run(sim))
        }
    }

}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleSearchRequest(term: String) {
    var metadataList: List<String>
    val matchingModelIndexes = mutableListOf<Int>()
    val parameters = getParameters(call)

    transaction {
         metadataList = MODELS
                .select{
                    whereFilter(repository=parameters.first,status=parameters.second, filter = parameters.third)
                }
                .map { resultRow -> resultRow[MODELS.mMetadata].toString() +
                        "repo:" + resultRow[MODELS.mRepository].toString() + "_" +
                        "status:" + resultRow[MODELS.mStatus].toString() + "_" +
                        "doi:" + resultRow[MODELS.mDoi].toString() + "_" +
                        "conceptDoi:" + resultRow[MODELS.mConceptDoi].toString() + "_"}

        metadataList.forEachIndexed { index, modelMetadata ->
            if(term.contains("repo:")){
                val t = modelMetadata.substring(modelMetadata.indexOf("repo:[") + 6
                    ,modelMetadata.lastIndexOf("]")).toLowerCase().split(',')
                if (t.contains(term.substring(5)))
                    matchingModelIndexes.add(index)
            } else {
                if (modelMetadata.contains(term, ignoreCase = true)) {
                    matchingModelIndexes.add(index)
                }
            }
        }
    }.apply {
        call.respond(matchingModelIndexes)
    }
}
/*private fun readScript(modelFile: File, uri: URI, scriptResource : FskMetaDataObject.ResourceType): String {
    var x = 22;
    var result = CombineArchive(modelFile).use {
        it.getEntriesWithFormat(uri).filter { entry -> entry.descriptions.isNotEmpty() }.first { entry ->
            val firstDescription = entry.descriptions[0]
            val metadataObject = FskMetaDataObject(firstDescription)
            metadataObject.resourceType == scriptResource
        }.loadTextEntry()
    }
    return result;
}*/

private fun getParameters(call: ApplicationCall):Triple<Repository,Status,String>{

    val rep = Repository.fromSymbol(call.request.queryParameters["repository"].toString()) ?:Repository.FSK_WEB
    val status = Status.fromSymbol(call.request.queryParameters["status"].toString()) ?:Status.CURATED
    val filter = call.request.queryParameters["filter"] ?: ""
    return Triple(rep,status,filter)
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
    val mVisualizationScript = varchar("VisualizationScript", 200000).nullable()
    val mModelScript = varchar("ModelScript", 200000).nullable()
    val mReadme = varchar("Readme", 200000).nullable()
    val mSimulations = varchar("Simulations", 200000).nullable()
    val mExecutionTime = varchar("ExecutionTime", 255).nullable()
    val mUploadDate = varchar("UploadDate", 255).nullable()
    val mUploadedBy = varchar("UploadedBy", 255).nullable()
    val mChecksum  = varchar("Checksum", 255).nullable()

}

enum class Repository (val rep: String){
    RAKIP_WEB ("RAKIP-Web"),
    FSK_WEB("FSK-Web"),
    RENJIN("Renjin"),
    TRASHBIN("Trash-Bin"),
    ANY("Any");
    companion object {
        private val mapping = values().associateBy(Repository::rep)
        fun fromSymbol(rep: String) = mapping[rep]
    }
}

enum class Status(val key: String){
    UNCURATED("Uncurated"),
    CURATED("Curated"),
    ANY("Any");
    companion object {
        private val mapping = values().associateBy(Status::key)
        fun fromSymbol(key: String) = mapping[key]
    }
}
// FILTER BY REPOSITORY AND STATUS
private fun whereFilter(mId: String = "",
                        repository: Repository=Repository.FSK_WEB,
                        status: Status=Status.CURATED,
                        filter: String = ""): Op<Boolean>{
    var predicates : Op<Boolean> = MODELS.mId.isNotNull()

    if(filter.isNotEmpty())
        predicates = predicates.and(MODELS.mName like "%$filter%")
    if(repository != Repository.ANY)
        predicates = predicates.and(MODELS.mRepository like "%" + repository.rep + "%")
    if(repository == Repository.ANY)
        predicates = predicates.and(MODELS.mRepository notLike "%" + Repository.TRASHBIN.rep + "%" )
    if(status != Status.ANY)
        predicates = predicates.and(MODELS.mStatus eq status.key)

    if(mId.isNotEmpty())
        predicates = predicates.and(MODELS.mId eq mId)

    return predicates
}

// CHECKSUM FOR MODEL FILES
@Throws(Exception::class)
fun createChecksum(filename: String?): ByteArray? {
    val complete = MessageDigest.getInstance("SHA-256")
    filename?.let {
        val fis: InputStream = FileInputStream(it)
        val buffer = ByteArray(1024)

        var numRead: Int
        do {
            numRead = fis.read(buffer)
            if (numRead > 0) {
                complete.update(buffer, 0, numRead)
            }
        } while (numRead != -1)
        fis.close()
    }
    return complete.digest()
}