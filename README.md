# Landing page
Full stack version of the landing page.
- Backend written in Kotlin with Ktor at src/main/kotlin/server.kt
- Frontend written with FreeMarker at src/main/resources/templates/index.ftl

The application can be tested with the main method in server.kt and listens at port 8080. It has the endpoints:
- */* serves the index page
- */download/{i}* downloads the model file at index *i*
- */metadata/{i}* returns the metadata of the model at index *i*
- */image/{i}* returns the svg image of the model at index *i*. The image is returned as an SVG string.

## Configuration
The application requires a configuration file named *landingpage.properties* with settings needed for execution. This file can be either located at the user folder or at `CATALINA_HOME` (KNIME Server). In case both locations have the file, the file at the user folder takes precedence.

### How to find CATALINA_HOME
The environment variable `CATALINA_HOME` holds the path to the base directory of a Catalina environment. This can be checked when running the KNIME Server at the beginning. For example when running the startup script of the KNIME server or doing catalina run:

```
Using CATALINA_BASE:   /Applications/KNIME Server/apache-tomee-plus-7.0.5
Using CATALINA_HOME:   /Applications/KNIME Server/apache-tomee-plus-7.0.5
Using CATALINA_TMPDIR: /Applications/KNIME Server/apache-tomee-plus-7.0.5/temp
Using JRE_HOME:        /Library/Java/JavaVirtualMachines/jdk1.8.0_271.jdk/Contents/Home
Using CLASSPATH:       /Applications/KNIME Server/apache-tomee-plus-7.0.5/bin/bootstrap.jar:/Applications/KNIME Server/apache-tomee-plus-7.0.5/bin/tomcat-juli.jar
Tomcat started.
```

### Contents of the file

The *landingpage.properties* is a simple Java properties file with the following keys:
* `model_folder`: Folder with FSKX models.
* `plot_folder`: Folder with SVG model plots.
* `model_time_date.csv`: CSV file describing the execution and upload times of the models in the model folder. See more details at [model_time_date.csv](#model_time_datecsv).
* `base_url`: Url to application.
* `context`: Path of the application if deployed under an application container. For example for https://knime.bfr.berlin/landingpage the context is `landingpage`. This can be omitted for local applications not running in a container.

Example landingpage.properties file:
```
model_folder=/Users/jdoe/my_models/models
plot_folder=/Users/jdoe/my_models/plots
times_csv=/Users/jdoe/my_models/model_time_date.csv
base_url=http://localhost:8080/
context=landingpage
```

### model_time_date.csv
This CSV contains only three columns: model id, execution time in seconds and upload date. The model id is the id of the model in the metadata (generalInformation -> identifier). Example:
```
toyModel,5s,2018-11-08 | 08:08
```

## References
-	IntelliJ IDEA https://www.jetbrains.com/idea/ (Community edition)
-	Kotlin language for backend https://kotlinlang.org/docs/reference/
-	REST library used https://ktor.io/
-	Frontend with Freemarker templates. Guide https://ktor.io/docs/website.html
-	Deployment of Ktor app with Tomcat https://ktor.io/docs/war.html
