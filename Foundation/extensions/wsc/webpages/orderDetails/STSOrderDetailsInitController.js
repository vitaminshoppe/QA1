scDefine([
	"scbase/loader!dojo/_base/declare",
	"scbase/loader!sc/plat/dojo/controller/ScreenController"],
function(			 
	_dojodeclare,
    scScreenController
){
	return _dojodeclare(
		"extn.orderDetails.HomeExtnInitController", [scScreenController], {
		 screenId : 'extn.orderDetails.STSOrderDetails'
	});
});



