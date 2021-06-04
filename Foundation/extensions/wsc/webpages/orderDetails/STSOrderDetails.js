scDefine(["dojo/text!./templates/STSOrderDetails.html", 
		  "scbase/loader!dojo/_base/declare",  
		  "scbase/loader!sc/plat/dojo/utils/BaseUtils", 
		  "scbase/loader!sc/plat/dojo/utils/ModelUtils",
		  "scbase/loader!sc/plat/dojo/widgets/Screen",
		  "scbase/loader!sc/plat/dojo/utils/ScreenUtils",
		  "scbase/loader!sc/plat/dojo/utils/WidgetUtils",
		  "scbase/loader!ias/utils/ContextUtils"
		  ], 
function( templateText,
		  _dojodeclare, 
		  _scBaseUtils, 
		  _scModelUtils,
		  _scScreen,
		  _scScreenUtils,
		  _scWidgetUtils,
		  _iasContextUtils
) {  

    return _dojodeclare("extn.orderDetails.STSOrderDetails", [_scScreen], {
        templateString: templateText,
        uId: "STSOrderDetails",
        packageName: "extn.orderDetails",
        className: "STSOrderDetails",
        title: "STSOrderDetails",
        screen_description: "Screen for displaying STS order details",
        namespaces: {
            targetBindingNamespaces: [
			{
                description: "Get Order Details Output is stored here",
				value: 'extn_getOrderDetailsNS_input'
				
            }
			],
            sourceBindingNamespaces: [{
					value: 'screenInput',
					description: "Input provided when the screen is loaded used to default values."
				},
			{
				value: 'extn_getOrderDetailsNS',
			}
			]
        },
        hotKeys: [],
        events: [],
        subscribers: {
            local: [{
                eventId: 'afterScreenInit',
                sequence: '30',
                handler: {
                    methodName: "initializeScreen"
                }
            }, {
				eventId: 'afterBehaviorMashupCall',
				sequence: '51',
				handler: {
					methodName: "extn_afterBehaviorMashupCall"
				}
			}, {
                eventId: 'Popup_btnClose_onClick',
                sequence: '30',
                description: '',
                listeningControlUId: 'Popup_btnClose',
                handler: {
                    methodName: "closePopUp",
                    description: ""
                }
            },]
        },
		closePopUp :function(){
			_scWidgetUtils.closePopup(this, "", false);

		}, 
		extn_afterBehaviorMashupCall: function(event, bEvent, ctrl, args){
			_scWidgetUtils.closePopup(this, "", false);

		},
		initializeScreen: function(event, bEvent, ctrl, args) {	
			var paramsScreen = _scModelUtils.getStringValueFromPath("screen.params.scEditorInput", args);
			var argsNew = args;
			var orderInput = {};
			orderInput.Order = {};
			orderInput.Order.OrderLines = {};
			orderInput.Order.OrderLines.OrderLine = {};
			orderInput.Order.OrderNo = paramsScreen.Order.OrderNo; 
			var orderLines = paramsScreen.Order.OrderLines.OrderLine;
			var arrOrderLines = _scBaseUtils.getNewArrayInstance();
			for(var orderLine in orderLines) 
			{
				var lineType = orderLines[orderLine].LineType;
				var currentStore = _iasContextUtils.getFromContext("CurrentStore");
				var shipNode = orderLines[orderLine].ShipNode;
				if((lineType == "SHIP_TO_STORE") && (_scBaseUtils.equals(currentStore, shipNode)))
				{
					
					var statusNumber = orderLines[orderLine].MaxLineStatus;
					if(statusNumber=="2160" || statusNumber=="2160.10" || statusNumber=="2160.20" || statusNumber=="1100")
					{
						orderLines[orderLine].Status = "Processing";
					}
					if(statusNumber=="2160.200")
					{
						orderLines[orderLine].Status = "In Transit to Store";
					}
                    _scBaseUtils.appendToArray(arrOrderLines, orderLines[orderLine]);
				}
			}
			_scModelUtils.addListToModelPath("Order.OrderLines.OrderLine", arrOrderLines, orderInput);
			_scScreenUtils.setModel(this, "extn_getOrderDetailsNS", orderInput , null);
		}



    });
});
