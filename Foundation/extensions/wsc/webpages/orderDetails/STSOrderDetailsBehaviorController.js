scDefine([
	"scbase/loader!dojo/_base/declare",
	"scbase/loader!sc/plat/dojo/controller/ServerDataController"], 
function(
	_dojodeclare,
	scServerDataController
){
	return _dojodeclare(
		"extn.orderDetails.GiftCardEmailBehaviorController", [scServerDataController], {
		screenId : 	'extn.orderDetails.STSOrderDetails'
	});
});

