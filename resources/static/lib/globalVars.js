// Regexp to match SId from SBML
var _idRegexp = /^[A-Za-z_^s]\w*$/;
// simulations of the currently selected model
let _simulations = [];
let _selectedSimulationIndex = 0;

let _parameters = [];



// These sets are used with the th-filters
let _softwareSet = new Set();
let _environmentSet = new Set();
let _hazardSet = new Set();
let _modelTypeSet = new Set();


let _cache = [];

let _representation = {
    title1: "Landing Page",
    link1: "https://knime.bfr.berlin/knime/",
    linkName1: "Web Repository (authentication required)",
    mainColor: "rgb(55,96,146)",
    buttonColor: "rgb(83,121,166)",
    hoverColor: "rgb(130,162,200)",
    title2: "FSK-Web",
    metadata: {}

};
