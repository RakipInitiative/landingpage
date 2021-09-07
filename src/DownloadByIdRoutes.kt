import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import java.io.File

fun Application.downloadRoutes(modelFiles:List<File>) {
    routing{
        downloadByIdRoute(modelFiles)
    }


}

fun Route.downloadByIdRoute(modelFiles:List<File>){
    get("/downloadById/{id}") {
        call.parameters["id"]?.let { fileId ->
            try {
                val modelFile = modelFiles.first { it.nameWithoutExtension == fileId }
                call.response.header("Content-Disposition", "attachment; filename=${modelFile.name}")
                call.respondFile(modelFile)
            } catch (err: IndexOutOfBoundsException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

}