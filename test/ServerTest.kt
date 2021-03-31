import de.bund.bfr.landingpage.module
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ServerTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {

            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
            }
        }
    }
}