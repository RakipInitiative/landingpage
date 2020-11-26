<#-- @ftlvariable name="data" type="com.example.IndexData" -->
<html>
<head>

    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>${representation.title1}</title>

    <link rel="stylesheet" href="${representation.resourcesFolder}/lib/jquery-ui.min.css">
    <link rel="stylesheet" href="${representation.resourcesFolder}/lib/bootstrap-3.4.1-dist/css/bootstrap.css">
    <link rel="stylesheet" href="${representation.resourcesFolder}/styles.css">
    <link rel="stylesheet" href="${representation.resourcesFolder}/lib/jssimulator.css">
</head>
<body>

    <script src="${representation.resourcesFolder}/lib/jquery.3.4.1.min.js"></script>
    <script src="${representation.resourcesFolder}/lib/jquery-ui.min.js"></script>
    <script src="${representation.resourcesFolder}/lib/bootstrap-3.4.1-dist/js/bootstrap.js"></script>
    <script src="${representation.resourcesFolder}/lib/editor_data.js"></script>
    <script src="${representation.resourcesFolder}/lib/handlers.js"></script>
    <script src="${representation.resourcesFolder}/lib/globalVars.js"></script>
    <script src="${representation.resourcesFolder}/lib/jssimulator.js"></script>

    <div class="container-fluid">

        <div id="Navbar">

            <div className="topnav" id="myTopnav" style="background-color: ${representation.mainColor};">
                <h1>${representation.title1}</h1>

                <!-- Menu link -->
                <a href="javascript:void(0)" style="font-size:36px;" className="icon" id="MenuIcon">
                    <i style="font-size:26px;" class="material-icons">menu</i>
                </a>

                <!-- Hard coded link -->
                <a class="Nav" href=${representation.link1} target="_blank">${representation.linkName1}</a>
            </div>

            <div id="mySidenav" class="sidenav">
                <a href="javascript:void(0)" class="closebtn">&times;</a> <!-- Close button -->
                <a class="Nav" href=${representation.link1} target="_blank">${representation.linkName1}</a>
            </div>

            <div id="searchBar">
                <div>
                    <input id="filter-search" class="form-control" type="search" placeholder="Search" aria-label="Search">
                    <span id="clear" class="glyphicon glyphicon-remove-circle"></span>
                    <div id="numberModels"></div>
                </div>
            </div>
        </div>

        <!-- TODO: Add contents to this description paragraph -->
        <p></p>

        <div id="MainTable">
            <table id="TableElement" class="sortable table table-sm table-responsive-xl">
                <thead>
                    <th class="actives" id="col1" scope="col" data-sort="name">Model Name</th>
                    <th class="actives hideColumn" id="col2" scope="col" data-sort="name">ModelID</th>
                    <th class="actives" id="colS" data-sort="name">
                        <span id="col3">Software</span><br/>
                        <span>
                            <select id="soft" class="crit">
                                <option selected="selected">Select</option>
                                <#list representation.metadata.uniqueSoftware as software>
                                <option value="${software}">${software}</option>
                                </#list>
                            </select><button id="clearSoft" title="reset" class="glyphicon glyphicon-remove"></button>
                        </span>
                    </th>
                    <th class="actives" id="colE" data-sort="name">
                        <span id="col4">Environment</span><br/>
                        <span>
                            <select id="env" class="crit">
                                <option selected="selected">Select</option>
                                <#list representation.metadata.uniqueEnvironments as environment>
                                <option value="${environment}">${environment}</option>
                                </#list>
                            </select><button id="clearEnv" title="reset" class="glyphicon glyphicon-remove"></button>
                        </span>
                    </th>
                    <th class="actives" id="colH" data-sort="name">
                        <span id="col5">Hazard</span><br/>
                        <span>
                            <select id="haz" class="crit">
                                <option selected="selected">Select</option>
                                <#list representation.metadata.uniqueHazards as hazard>
                                <option value="${hazard}">${hazard}</option>
                                </#list>
                            </select><button id="clearHaz" title="reset" class="glyphicon glyphicon-remove"></button>
                        </span>
                    </th>
                    <th class="actives" id="colT" data-sort="name">
                        <span id="col8">Type</span><br/>
                        <span>
                            <select id="type" class="crit">
                                <option selected="selected">Select</option>
                                <#list representation.metadata.uniqueTypes as type>
                                <option value="${type}">${type}</option>
                                </#list>
                            </select><button id="clearType" title="reset" class="glyphicon glyphicon-remove"></button>
                        </span>
                    </th>
                    <th class="actives" id="col6" scope="col" data-sort="name">Execution Time </th>
                    <th class="actives" id="col7" scope="col" data-sort="name">Upload Date </th>
                    <th id="cright">Details</th>
                </thead>
                <tbody id="rows">
                    <#list representation.metadata.views as modelview>
                    <tr id="${modelview?index}">
                        <td>${modelview.modelName}</td>
                        <td class="hideColumn">${modelview.modelId}</td>
                        <td class="softCol columnS">${modelview.software}</td>
                        <td class="envCol columnS">
                            <#list modelview.environment as item>${item} </#list>
                        </td>
                        <td class="hazCol columnS">
                            <#list modelview.hazard as item>${item} </#list>
                        </td>
                        <td class="typeCol columnS">${modelview.modelType}</td>
                        <td>${modelview.durationTime}</td>
                        <td>${modelview.uploadTime}</td>
                        <td>
                            <button type="button" class="btn btn-primary detailsButton" id="opener${modelview?index}">Details</button>
                            <br><br>
                            <a class="btn btn-primary downloadButton" href="${modelview.downloadUrl}" download>Download</a>
                            <div id="wrapper${modelview?index}"></div>
                            <br><br>
                            <button type="button" class="btn btn-primary executeButton" id="executor${modelview?index}"
                                data-toggle="modal">Execute</button>
                        </td>

                        <script>
                        $("#opener${modelview?index}").click(event => buildDialogWindow(${modelview?index}));
                         $("#executor${modelview?index}").click((event) => buildSimulatorWindow(event));
                        </script>
                    </tr>
                    </#list>
                </tbody>
            </table> <!-- #TableElement -->
        </div> <!-- #MainTable -->

        <!-- details dialog -->
        <div id="detailsDialog" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                        <h4 class="modal-title">Modal title</h4>
                    </div>
                    <div class="modal-body">
                        <nav class="navbar navbar-default">
                            <div class="navbar-collapse collapse">
                                <ul class="nav navbar-nav" id="viewTab"></ul>
                            </div>
                        </nav>
                        <div class="tab-content" id="viewContent"></div>
                    </div> <!-- .modal-body -->
                </div> <!-- .modal-content -->
            </div> <!-- .modal-dialog -->`;
        </div> <!-- modal -->



    <script>
        let BACKEND_URL = "${representation.endpoint}";

        // table head style
        $("table.sortable thead th").css("background-color", "${representation.mainColor}");

        // numberModels div style
        $("#numberModels, #filter-search").css("color", "${representation.mainColor}");

        // Hidden sidenav
        $(".sidenav").css("background-color", "${representation.mainColor}");

        // Selects style
        $("#soft, #env, #haz, #type").css("color", "${representation.mainColor}");

        // Buttons style
        $(".topnav a.Nav").css("background-color", "${representation.mainColor}")

        $(".detailsButton, .downloadButton").css("background-color", "${representation.buttonColor}");

        $("#clear").css("color", "${representation.mainColor}");

        $(".glyphicon-remove").css("color", "${representation.hoverColor}");

        // table head:hover
        $("th.actives.ascending, th.actives.descending, table.sortable th.actives").hover((mouse) => {
            $(this).css("background-color", mouse.type === "mouseenter" ?
                "${representation.hoverColor}" : "${representation.mainColor}")
        });

        $(".sidenav a.Nav").hover((mouse) => {
            $(this).css("background-color", mouse.type === "mouseenter" ?
                    "${representation.hoverColor}" : "${representation.mainColor}");
        });

        $(".sidenav .closebtn").hover((mouse) => {
            $(this).css("color", mouse.type === "mouseenter" ?
                "${representation.hoverColor}" : "white");
        });

        $(".topnav a.Nav").hover((mouse) => {
            $(this).css("background-color", mouse.type === "mouseenter" ?
                    "${representation.hoverColor}" : "${representation.mainColor}");
        });

        $("#MenuIcon").click(() => document.getElementById("mySidenav").style.width = "250px");
        $('.closebtn').click(() => document.getElementById("mySidenav").style.width = "0");

        // Scrolling: detect a scroll event on the tbody
        $('tbody').scroll((event) => {
            $('thead').css("left", -$("tbody").scrollLeft()); //fix the thead relative to the body scrolling
            $('thead th:nth-child(2)').css("left", $("tbody").scrollLeft()); //fix the first cell of the header
            $('tbody td:nth-child(2)').css("left", $("tbody").scrollLeft()); //fix the first column of tdbody
        });

    // Populate cache
        $(".table tbody tr").each(function() {
            let rawText = getText(this);
            let formattedText = rawText ? rawText.trim().toLowerCase() : "";

            // Add an object to the cache array
            _cache.push({
                element: this,
                text: formattedText
            });
        });

        // If browser does not support the input event, then use the keyup event
        let search = $("#filter-search"); // Get the input element
        // search metadata backend
        search.on('keyup', function (e) {
            if (e.key === 'Enter' || e.keyCode === 13) {
                let query = (this.value == undefined || this.value == "") ? "%20"
                : this.value.trim().toLowerCase(); // Get the query
                filter(query);
            }
        });


        //*******************Sort*******************//
        let compare = {                           // Declare compare object
            name: function(a, b) {                  // Add a method called name
                a = a.replace(/^the /i, '');          // Remove The from start of parameter
                b = b.replace(/^the /i, '');          // Remove The from start of parameter
                
                if (a < b) {                          // If value a is less than value b
                    return -1;                          // Return -1
                } else {                              // Otherwise
                    return a > b ? 1 : 0;               // If a is greater than b return 1 OR
                }                                     // if they are the same return 0
            }, duration: function(a, b) {              // Add a method called duration
                a = a.split(':');                     // Split the time at the colon
                b = b.split(':');                     // Split the time at the colon

                a = Number(a[0]) * 60 + Number(a[1]); // Convert the time to seconds
                b = Number(b[0]) * 60 + Number(b[1]); // Convert the time to seconds

                return a - b;                         // Return a minus b
            }, date: function(a, b) {                  // Add a method called date
                a = new Date(a);                      // New Date object to hold the date
                b = new Date(b);                      // New Date object to hold the date
          
                return a - b;                         // Return a minus b
            }
        };

        function sortColumn(idName, column) {
            let table = $(".sortable"); // This sortable table
            let tbody = table.find("tbody"); // Store table body
            let rows = tbody.find("tr").toArray(); // Store array containing rows
            let header = $(idName); // Get the header
            let order = header.data("sort"); // Get data-sort attribute
    
            // If selected item has ascending or descending class, reverse contents
            if (header.is('.ascending') || header.is('.descending')) {  
                header.toggleClass('ascending descending');    // Toggle to other class
                tbody.append(rows.reverse());                // Reverse the array
            } else {                                        // Otherwise perform a sort                            
                header.addClass('ascending');                // Add class to header
                // Remove asc or desc from all other headers
                header.siblings().removeClass('ascending descending'); 
                if (compare.hasOwnProperty(order)) {  // If compare object has method
                    rows.sort(function(a, b) {               // Call sort() on rows array
                        a = $(a).find('td').eq(column).text().toLowerCase(); // Get text of column in row a
                        b = $(b).find('td').eq(column).text().toLowerCase(); // Get text of column in row b
                        return compare[order](a, b);           // Call compare method
                    });
                    tbody.append(rows);
                }
            }
        }
  
        function sortSpan(idName, column){
            let table = $(".sortable");              // This sortable table
            let tbody = table.find('tbody');        // Store table body
            let rows = tbody.find('tr').toArray();   // Store array containing rows
            let header = $(idName).parents('th');                  // Get the header
            let order = header.data('sort');       // Get value of data-sort attribute
      
            // If selected item has ascending or descending class, reverse contents
            if (header.is('.ascending') || header.is('.descending')) {  
                header.toggleClass('ascending descending');    // Toggle to other class
                tbody.append(rows.reverse());                // Reverse the array
            } else {                                        // Otherwise perform a sort                            
                header.addClass('ascending');                // Add class to header
                // Remove asc or desc from all other headers
                header.siblings().removeClass('ascending descending'); 
                if (compare.hasOwnProperty(order)) {  // If compare object has method
                    console.log(column);
                    rows.sort(function(a, b) {               // Call sort() on rows array
                        a = $(a).find('td').eq(column).text().toLowerCase(); // Get text of column in row a
                        b = $(b).find('td').eq(column).text().toLowerCase(); // Get text of column in row b
                        return compare[order](a, b);           // Call compare method
                    });
                    tbody.append(rows);
                }
            }
        };

        // Filter by different software, environment & hazard values
        $('#soft, #env, #haz, #type').on('change', filterByCol);

        // Clear the search bar input
        $("#clear").click(() => {
            $('#rows tr').show();
            $("#filter-search").val("Search");
            $("#numberModels").fadeOut();

            // Clear selects
            document.getElementById("soft").value = "Select";
            document.getElementById("env").value = "Select";
            document.getElementById("haz").value = "Select";
            document.getElementById("type").value = "Select";
        });

        // Clear the selects of the different filters on button press
        $("#clearSoft").click(() => {
            document.getElementById("soft").value = "Select";
            filterByCol();
        });

        $("#clearEnv").click(() => {
            document.getElementById("env").value = "Select";
            filterByCol();
        });

        $("#clearHaz").click(() => {
            document.getElementById("haz").value = "Select";
            filterByCol();
        });
    
        $("#clearType").click(() => {
            document.getElementById("type").value = "Select";
            filterByCol();
        });

        // Sort columns
        $("#col1").click(() => sortColumn("#col1", 1));
        $("#col2").click(() => sortColumn("#col2", 2));
        $("#col3").click(() => sortColumn("#col3", 3));
        $("#col4").click(() => sortColumn("#col4", 4));
        $("#col5").click(() => sortSpan("#col5", 5));
        $("#col8").click(() => sortColumn("#col8", 8));
        $("#col6").click(() => sortColumn("#col6", 6));
        $("#col7").click(() => sortColumn("#col7", 7));
        

        // Details buttons events
        async function buildDialogWindow(modelIndex) {
            let metadata = await fetch(BACKEND_URL + "metadata/" + modelIndex)
                .then(response => response.json());
            
            let image = await fetch(BACKEND_URL + "image/" + metadata.generalInformation.identifier)
                .then(response => response.blob())
                .then(imageBlob => URL.createObjectURL(imageBlob));
            
            // Update .modal-title
            if (metadata.generalInformation && metadata.generalInformation.name) {
                $(".modal-title").text(metadata.generalInformation.name);
            }

            // Get appropiate metadata handler for the model type.
            let handler;
            if (metadata.modelType === "genericModel") {
                handler = new GenericModel(metadata, image);
            } else if (metadata.modelType === "dataModel") {
                handler = new DataModel(metadata, image);
            } else if (metadata.modelType === "predictiveModel") {
                handler = new PredictiveModel(metadata, image);
            } else if (metadata.modelType === "otherModel") {
                handler = new OtherModel(metadata, image);
            } else if (metadata.modelType === "toxicologicalModel") {
                handler = new ToxicologicalModel(metadata, image);
            } else if (metadata.modelType === "doseResponseModel") {
                handler = new DoseResponseModel(metadata, image);
            } else if (metadata.modelType === "exposureModel") {
                handler = new ExposureModel(metadata, image);
            } else if (metadata.modelType === "processModel") {
                handler = new ProcessModel(metadata, image);
            } else if (metadata.modelType === "consumptionModel") {
                handler = new ConsumptionModel(metadata, image);
            } else if (metadata.modelType === "healthModel") {
                handler = new HealthModel(metadata, image);
            } else if (metadata.modelType === "riskModel") {
                handler = new RiskModel(metadata, image);
            } else if (metadata.modelType === "qraModel") {
                handler = new QraModel(metadata, image);
            } else {
                handler = new GenericModel(metadata, image);
            }

            document.getElementById("viewTab").innerHTML = handler.menus;
  
            // Add tab panels
            let viewContent = document.getElementById("viewContent");
            viewContent.innerHTML = ""; // First remove old tabs
  
            // Add new tabs from handler
            Object.entries(handler.panels).forEach(([key, value]) => {
                // Create a tab from the panel (value)
                let tabPanel = document.createElement("div");
                tabPanel.setAttribute("role", "tabpanel");
                tabPanel.className = "tab-pane";
                tabPanel.id = key;
                tabPanel.innerHTML = value;
    
                viewContent.appendChild(tabPanel); // Add new tabPanel
            });
  
            // Set the first tab (general information) as active
            document.getElementById("generalInformation").classList.add("active");
    
            $("#simulatorDialog").modal("hide");
            $("#detailsDialog").modal("show");
        }

        <#noparse>


        /**
         * Populate the options of a select.
         * 
         * @param {string} selectId Id of a select
         * @param {array} options Array of possible values
         */
        function populateSelectById(selectId, options) {
            let select = document.getElementById(selectId);
            populateSelect(select, options);
        }

        /**
         * Populate the options of a select.
         *
         * @param {element} select DOM element
         * @param {array} options Array of possible values
         */
        function populateSelect(select, options) {
            if (options.size) {
                options.forEach(entry => select.innerHTML += `<option value="${entry}">${entry}</option>`);
            }
        }

        /**
         * Add tokens without duplicates and with the first letter capitalized to an existing set.
         */
        function addUniformElements(uniformedElement, targetSet) {
            uniformedElement.map(token => token.charAt(0).toUpperCase() + token.slice(1)).forEach(token => targetSet.add(token));
        }

        // Multiple filtering for every columns
        function filterByCol() {
            let filt = "";
            let rows = $("#rows tr");

            // TODO: For performance select1, select2, select3 and select4 should be retrieved with document.getElementById(id).value
            // TODO: For readability select1, select2, select3 and select4 should take more expressive names like softwareSelectValue.
            let select1 = $("#soft").val();
            let select2 = $("#env").val();
            let select3 = $("#haz").val();
            let select4 = $("#type").val();

            rows.hide();

            let numberModelsDiv = document.getElementById("numberModels");

            if (select1 == "Select" && select2 == "Select" && select3 == "Select" && select4 == "Select") {
                rows.show();
                numberModelsDiv.innerHTML = `Your search return ${rows.length} models`;
            } else if (select2 == "Select") {
                if (select1 != "Select" && select3 == "Select") {
                    filt = $(`#MainTable td.softCol:contains("${select1}")`).parent();
                } else if (select1 != "Select" && select3 != "Select") {
                    filt1 = rows.filter($(`#MainTable td.softCol:contains("${select1}")`).parent());
                    let selRows = rows.filter(filt1);
                    filt = selRows.filter($(`#MainTable td.hazCol:contains("${select3}")`).parent().show());
                    rows.hide();
                } else if (select1 == "Select" && select3 != "Select") {
                    filt = $(`#MainTable td.hazCol:contains("${select3}")`).parent();
                } else{
                    filt = ""
                }
            } else if (select1 == "Select") {
                if (select2 != "Select" && select3 == "Select") {
                    filt = `:contains("${select2}")`;
                } else if (select2 != "Select" && select3 != "Select"){
                    filt = `:contains("${select2}"):contains("${select3}")`;
                } else if (select2 == "Select" && select3 != "Select"){
                    filt = $(`#MainTable td.hazCol:contains("${select3}")`).parent().show();
                } else {
                    filt = "";
                }
            } else if (select3 == "Select") {
                if(select1 != "Select" && select2 != "Select"){
                    filt1 = rows.filter($(`#MainTable td.softCol:contains("${select1}")`).parent());
                    var selRows=rows.filter(filt1);
                    filt = selRows.filter($(`#MainTable td.envCol:contains("${select2}")`).parent().show());
                    rows.hide();
                } else {
                    filt="";
                }
            } else {
                filt = `:contains("${select1}"):contains("${select2}"):contains("${select3}"):contains("${select4}"`;
            }

            rows.filter(filt).show();
            let searchResult = rows.filter(filt);
            numberModelsDiv.innerHTML = `Your search returned ${searchResult.length} models`;

            // Get new sets for the filtered rows
            let softwareSet = new Set();
            let environmentSet = new Set();
            let hazardSet = new Set();
            let modelTypeSet = new Set();

            for (let i = 0; i < searchResult.length; i++) {
                let currentMatchCells = searchResult[i].getElementsByTagName("td");

                let software = searchResult[i].getElementsByTagName("td")[2].innerText;
                let environment = searchResult[i].getElementsByTagName("td")[3].innerText;
                let hazard = searchResult[i].getElementsByTagName("td")[4].innerText;
                let modelType = searchResult[i].getElementsByTagName("td")[5].innerText;

                // Split some entries joined with commas
                addUniformElements(software.split(/[,|]/), softwareSet);
                addUniformElements(environment.split(/[,|]/), environmentSet);
                addUniformElements(hazard.split(/[,|]/), hazardSet);
                addUniformElements(modelType.split(/[,|]/), modelTypeSet);
            }

            // Clear filters and populated them with the filtered results
            /* TODO: For performance, the selects could be queried at the beginning of this function when the variables are queried.
             This way the values would not need to be queried again.
             */ 
            let softwareSelect = document.getElementById("soft");
            populateSelect(softwareSelect, softwareSet);
            softwareSelect.value = select1;

            let environmentSelect = document.getElementById("env");
            populateSelect(environmentSelect, environmentSet);
            environmentSelect.value = select2;

            let hazardSelect = document.getElementById("haz");
            populateSelect(hazardSelect, hazardSet);
            hazardSelect.value = select3;

            let modelTypeSelect = document.getElementById("type");
            populateSelect(modelTypeSelect, modelTypeSet);
            modelTypeSelect.value = select4;

            // If no filters, restore the selects and numberModelsDiv
            if (filt == "") {

                // TODO: Restore original unique times from representation template variable
                // populateSelect(softwareSelect, _softwareSet);
                // populateSelect(environmentSelect, _environmentSet);
                // populateSelect(hazardSelect, _hazardSet);
                // populateSelect(modelTypeSelect, _modelTypeSet);
                // numberModelsDiv.innerHTML = " ";
            }
        }

        // Content elements for Searchfunction
        function getText(element) {
            let text;

            if (element.outerText) {
                text = element.outerText.trim();
            } else if (element.innerText) {
                text = element.innerText.trim();
            } else {
                text = "";
            }

            if (element.childNodes) {
                element.childNodes.forEach(child => text += getText(child));
            }

            return text;
        }
        async function searchFullMetadata(query){
            const rep = await fetch(BACKEND_URL + "search/" + query);
            return await rep.json();

        }
        async function filter(query) {

            let searchResults = await searchFullMetadata(query);//JSON.parse(await searchFullMetadata(query));

        //    // TODO: what is p???
            _cache.forEach(function(p) { // For each entry (<tr>) in cache pass image
                p.element.style.display = searchResults.includes(parseInt(p.element.id)) ? "" : "none"; // Show/Hide
                let numberOfVisibleRows = $("tbody tr:visible").length;
                document.getElementById("numberModels").innerHTML = `Your search returned ${numberOfVisibleRows} models`;
            })
        }
        </#noparse>

    </script>

</body>
</html>