


scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/text","scbase/loader!extn/components/shipment/backroompick/BackroomPickProductScanExtn","scbase/loader!sc/plat/dojo/controller/ExtnServerDataController"]
 , function(			 
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojotext
			 ,
			    _extnBackroomPickProductScanExtn
			 ,
			    _scExtnServerDataController
){

return _dojodeclare("extn.components.shipment.backroompick.BackroomPickProductScanExtnBehaviorController", 
				[_scExtnServerDataController], {

			
			 screenId : 			'extn.components.shipment.backroompick.BackroomPickProductScanExtn'

			
			
			
			
			
						,

			
			
			 mashupRefs : 	[
	 		{
		 mashupRefId : 			'updateShipmentQuantityForPickAllLine'
,
		 mashupId : 			'backroomPick_updateShipmentQuantity'
,
		 extnType : 			'MODIFY'

	}

	]

}
);
});

