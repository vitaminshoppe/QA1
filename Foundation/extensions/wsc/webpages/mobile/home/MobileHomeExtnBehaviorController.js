


scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/text","scbase/loader!extn/mobile/home/MobileHomeExtn","scbase/loader!sc/plat/dojo/controller/ExtnServerDataController"]
 , function(			 
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojotext
			 ,
			    _extnMobileHomeExtn
			 ,
			    _scExtnServerDataController
){

return _dojodeclare("extn.mobile.home.MobileHomeExtnBehaviorController", 
				[_scExtnServerDataController], {

			
			 screenId : 			'extn.mobile.home.MobileHomeExtn'

			
			
			
			
			
						,

			
			
			 mashupRefs : 	[
	 		{
		 mashupRefId : 			'pack_getShipmentListCount'
,
		 mashupId : 			'mobileHomeReadyForPacking_getShipmentListCount'
,
		 extnType : 			'MODIFY'

	}
,
	 		{
		 mashupRefId : 			'extn_shipPackages_getShipmentListCount'
,
		 mashupId : 			'confirmShipment_getShipmentContainerListCount'
,
		 extnType : 			'ADD'

	}

	]

}
);
});

