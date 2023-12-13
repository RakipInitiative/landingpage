
var _endpoint = _endpoint || window.location.origin + '/landingpage/DB/'; //'https://knime.bfr.berlin/backend/';//http://localhost:8080/' //'https://knime.bfr.berlin/landingpage/';
var _token = _token
var landingpage_home = window.location.origin + "/landingpage/"
var _endpoints 	= {
	metadata		: _endpoint + 'metadata/',
	image			: _endpoint + 'image/',
	download		: _endpoint + 'download/',
	uploadDate		: _endpoint + 'uploadDate/',
	executionTime	: _endpoint + 'executionTime/',
	simulations		: _endpoint + 'simulations/',
	execution 		: _endpoint + 'execute/',
	search 			: _endpoint + 'search/',
	filter 			: _endpoint + 'filter',
	metadata		: _endpoint + 'metadata/',
    modelScript     : _endpoint + "modelscript/",
    visScript       : _endpoint + "visualizationscript/",
    readMe          : _endpoint + "readme/"
};

var _appVars = {
	header			: {
		brand			: {
			logo			: 'assets/img/KIDA.png', // false
			title			: 'KIDA Model Repository' // false or ''
		},
		nav				: [

                          {
                             "title": "KIDA Home",
                             "href": landingpage_home + "KIDA-Model-Repository/"
                           },
                           {
                             "title": "KIDA BMEL",
                             "href": "https://kida-bmel.de/"
                           },
                           {
                             "title": "FSK-Lab Software",
                             "href": "https://foodrisklabs.bfr.bund.de/fsk-lab/"
                           },
                           {
                             "title": "Masthead",
                             "href": landingpage_home + "masthead"
                           },
                           {
                             "title": "Data Protection Declaration",
                             "href": landingpage_home + "dataProtectionDeclaration"
                           }
                          ]
	},
	mainTable 			: {
		rowActions 		: [
			{
				type 			: 'modal',
				idPrefix 		: 'mtActionDetails_',
				icon			: 'icon-eye',
				title 			: 'Details',
				target			: '#mtModalDetails'
			},
			{
				type 			: 'link',
				idPrefix 		: 'mtActionDownload_',
				icon			: 'icon-download',
				title 			: 'Download',
				on 				: {
					click 			: ( O, $action, rowIndex, rowData ) => {
					    var identifier = rowData["modelMetadata"]["generalInformation"]["identifier"];
						window.open( _endpoints.download + identifier + _token, '_blank' );
					}
				}
			},
			{
				type 			: 'link',
				idPrefix 		: 'mtActionSim_',
				icon			: 'icon-play',
				title 			: 'Simulation',
				on 				: {
					click 			: ( O, $action, rowIndex, rowData ) => {
					    var identifier = rowData["modelMetadata"]["generalInformation"]["identifier"];
					    var repository = window.location.pathname.split("/").pop();
						window.open( window.location.origin + '/knime/webportal/space/KIDA/Model_Execution?exec' + "&pm:file_ID="+identifier  , '_blank' );
					}
				}
			},
			{
                type 			: 'modal',
                idPrefix 		: 'mtUnavailable_',
                icon			: 'icon-minus-circle',
                title 			: 'Not available for download'
            }
//			{
//				type 			: 'modal',
//				idPrefix 		: 'mtActionSim_',
//				icon			: 'icon-play',
//				title 			: 'Simulation',
//				target			: '#mtModalSim'
//			}
		],
		cols 			: [
			{
				id 			: 'colModel',
				label		: 'Model',
				field 		: 'modelName',
				classes 	: {
					th 			: 'td-label min-200',
					td 			: 'td-label min-200 td-model'
				},
				sortable 	: true, // sortable
			},
			{
				id 			: 'colSoftware',
				label		: 'Software',
				field 		: 'software',
				classes 	: {
					th 			: null,
					td 			: 'td-soft'
				},
				sortable 	: true, // sortable
				facet 		: {
					tooltip 			: true,
					select2 			: true,
					select2SingleRow 	: true,
					placeholder 		: 'Software',
					maxSelectable 		: 1
				}
			},
			{
				id 			: 'colEnvironment',
				label		: 'Description',
				field 		: 'description',
				classes 	: {
					th 			: 'min-300',
					td 			: 'td-env min-300'
				},
				sortable 	: true, // sortable
				facet 		: {
					tooltip 			: true,
					select2 			: true,
					select2SingleRow 	: true,
					placeholder 		: 'description',
					maxSelectable 		: 1
				},
				collapsable	: true, // data-toggle-td
				formatter 	: '_list' // _formatter subroutine
			},
			{
				id 			: 'colType',
				label		: 'Tags',
				field 		: 'modelType',
				classes 	: {
					th 			: null,
					td 			: 'td-type'
				},
				sortable 	: true, // sortable
				facet 		: {
					tooltip 			: true,
					select2 			: true,
					select2SingleRow 	: true,
					placeholder 		: 'Type',
					maxSelectable 		: 1
				}
			},
			{
				id 			: 'colUploadDate',
				label		: 'Upload Date',
				field 		: 'uploadDate',
				classes 	: {
					th 			: null,
					td 			: null
				},
				sortable 	: true, // sortable
				switchable 	: true, // data-switchable
				formatter 	: '_uploadDate' // _formatter subroutine
			}
		],
		on 				: {			
			afterInit 		: ( O ) => {
				_log( 'on > afterInit', 'hook' ); // example hook output
				_log( O );
			},
			create			: ( O ) => {
				// create details modal
				O._modalDetails = new APPModalMTDetails( {
					data 		: O._metadata,
					id 			: 'mtModalDetails',
					classes 	: 'modal-details',
					type 		: 'mtDetails'
				}, O._$container );
				_log( O._modalDetails );

				// create simulations modal
				O._modalSim = new APPModalMTSimulations( {
					data 		: O._metadata,
					id 			: 'mtModalSim',
					classes 	: 'modal-sim',
					type 		: 'mtSim',
					on 			: {
						simRunModelView : ( O, modelId, simulation )=> {
							_log( 'on > simRunModelView', 'hook' ); // example hook output
							_log( O );
							_log( modelId );
							_log( simulation );
						}
					}
				}, O._$container );
				_log( O._modalSim );
			},
			afterPopulate 	: ( O, tableData ) => {
				_log( 'on > afterPopulate', 'hook' ); // example hook output
				_log( O );
				_log( tableData );
			},
			selectRow 		: ( O, rowIndex, rowData ) => {
				_log( 'on > selectRow', 'hook' ); // example hook output
				_log( O );
				_log( rowIndex );
				_log( rowData );
			},
			deselectRow 	: ( O, rowIndex, rowData ) => {
				_log( 'on > deselectRow', 'hook' ); // example hook output
				_log( O );
				_log( rowIndex );
				_log( rowData );
			},
			updateFilter 	: ( O, filtered ) => {
				_log( 'on > updateFilter', 'hook' ); // example hook output
				_log( O );
				_log( filtered );
			}
		}
	}
};


/**
 * app
 * initialization of main app
 */

$( document ).ready( () => { 
	_debug = true;
	_app = new APPLandingpage( _appVars, $( '.landingpage' ) );
} );