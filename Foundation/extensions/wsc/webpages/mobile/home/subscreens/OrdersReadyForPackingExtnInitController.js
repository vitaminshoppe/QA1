


scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/text","scbase/loader!extn/mobile/home/subscreens/OrdersReadyForPackingExtn","scbase/loader!sc/plat/dojo/controller/ExtnScreenController"]
 , function(			 
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojotext
			 ,
			    _extnOrdersReadyForPackingExtn
			 ,
			    _scExtnScreenController
){

return _dojodeclare("extn.mobile.home.subscreens.OrdersReadyForPackingExtnInitController", 
				[_scExtnScreenController], {

			
			 screenId : 			'extn.mobile.home.subscreens.OrdersReadyForPackingExtn'

			
			
			
			
			
						,

			
			
			 mashupRefs : 	[
	 		{
		 sourceBindingOptions : 			''
,
		 sequence : 			''
,
		 mashupId : 			'readyForPacking_getShipmentListInit'
,
		 sourceNamespace : 			''
,
		 mashupRefId : 			'getShipmentList'
,
		 extnType : 			'MODIFY'
,
		 callSequence : 			''

	}

	]

}
);
});

