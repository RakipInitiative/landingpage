# Landing page
Full stack version of the landing page.
- Backend written in Kotlin with Ktor at src/main/kotlin/server.kt
- Frontend written with FreeMarker at src/main/resources/templates/index.ftl

The application can be tested with the main method in server.kt and listens at port 8080. It has the endpoints:
- */* serves the index page
- */download/{i}* downloads the model file at index *i*
- */metadata/{i}* returns the metadata of the model at index *i*
- */image/{i}* returns the svg image of the model at index *i*. The image is returned as an SVG string.

## References
-	IntelliJ IDEA https://www.jetbrains.com/idea/ (Community edition)
-	Kotlin language for backend https://kotlinlang.org/docs/reference/
-	REST library used https://ktor.io/
-	Frontend with Freemarker templates. Guide https://ktor.io/docs/website.html
-	Deployment of Ktor app with Tomcat https://ktor.io/docs/war.html
