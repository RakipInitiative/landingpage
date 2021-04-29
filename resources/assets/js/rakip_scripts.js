
var _endpoint = _endpoint || 'https://knime.bfr.berlin/landingpage/'; //'https://knime.bfr.berlin/backend/';//http://localhost:8080/' //'https://knime.bfr.berlin/landingpage/';
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
};

var _appVars = {
	header			: {
		brand			: {
			logo			: 'assets/img/RAKIP_logo.jpg', // false
			title			: 'RAKIP Model Repository' // false or ''
		},
		nav				: [

			{
				title		: 'RAKIP Model Repository (login)',
				href		: 'https://knime.bfr.berlin/knime/webportal/space/RAKIP-Web/7._FSK_Repository_Model_Runner?exec'
			},
			{
                title		: 'Joining Models',
            	href		: 'https://knime.bfr.berlin/knime/webportal/space/RAKIP-Web/joining/'
            },
			{
                title		: 'Other Services',
            	href		: 'https://knime.bfr.berlin/knime/webportal/space/RAKIP-Web/'
            },
			{
				title		: 'Contact',
				href		: 'https://foodrisklabs.bfr.bund.de/contact_rakip/'
			},
			{
				title		: 'Imprint',
				href		: 'https://foodrisklabs.bfr.bund.de/legal-notice/'
			},
			{
				title		: 'Privacy Policy',
				href		: 'https://foodrisklabs.bfr.bund.de/disclaimer_en/'
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
						window.open( _endpoints.download + rowIndex, '_blank' );
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
						window.open( 'https://knime.bfr.berlin/knime/webportal/space/RAKIP-Web/executeModelFromRepository?exec' + "&pm:file_ID="+identifier + "&pm:repository="+repository , '_blank' );
					}
				}
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
				label		: 'Environment',
				field 		: 'environment',
				classes 	: {
					th 			: 'min-300',
					td 			: 'td-env min-300'
				},
				sortable 	: true, // sortable
				facet 		: {
					tooltip 			: true,
					select2 			: true,
					select2SingleRow 	: true,
					placeholder 		: 'Environment',
					maxSelectable 		: 1
				},
				collapsable	: true, // data-toggle-td
				formatter 	: '_list' // _formatter subroutine
			},
			{
				id 			: 'colHazard',
				label		: 'Hazard',
				field 		: 'hazard',
				classes 	: {
					th 			: null,
					td 			: 'td-haz'
				},
				sortable 	: true, // sortable
				facet 		: {
					tooltip 			: true,
					select2 			: true,
					select2SingleRow 	: true,
					placeholder 		: 'Hazard',
					maxSelectable 		: 1
				}
			},
			{
				id 			: 'colType',
				label		: 'Type',
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
				id 			: 'colExecTime',
				label		: 'Execution Time',
				field 		: 'executionTime',
				classes 	: {
					th 			: null,
					td 			: null
				},
				sortable 	: true, // sortable
				sorter 		: '_execution', // _sorter subroutine
				switchable 	: true, // data-switchable
				formatter 	: '_execution' // _formatter subroutine
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