
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/components/shipment/confirm/ConfirmShipmentContainerDetailExtnUI"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnConfirmShipmentContainerDetailExtnUI
){ 
	return _dojodeclare("extn.components.shipment.confirm.ConfirmShipmentContainerDetailExtn", [_extnConfirmShipmentContainerDetailExtnUI],{
	// custom code here
	
	extnFormatStatusDate:function(
        dataValue, screen, widget, namespace, modelObj, options) {
            var formattedOrderNo = null;
            formattedOrderNo = dataValue;
            return formattedOrderNo;
        }
});
});

