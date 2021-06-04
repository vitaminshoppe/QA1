scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/utils/ModelUtils","scbase/loader!sc/plat/dojo/utils/ScreenUtils","scbase/loader!extn/components/shipment/summary/ShipmentSummaryExtnUI","scbase/loader!wsc/components/shipment/common/utils/ShipmentUtils", "scbase/loader!ias/utils/ContextUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils"]
,
function(			 
			    _dojodeclare
			 ,_scBaseUtils,
			   _scModelUtils,
				_scScreenUtils,
			    _extnShipmentSummaryExtnUI
			,
				_wscShipmentUtils
			,
				_iasContextUtils
			,
				_scWidgetUtils
){ 
	return _dojodeclare("extn.components.shipment.summary.ShipmentSummaryExtn", [_extnShipmentSummaryExtnUI],{
	Paymethod: function(event, bEvent, ctrl, args)
		{
			var paymentModel = null;
            paymentModel = _scScreenUtils.getModel(this, "getShipmentDetailsMore_output");
            var shipmentLine = null;
            shipmentLine = _scModelUtils.getModelObjectFromPath("Shipment.ShipmentLines.ShipmentLine", paymentModel); 
           var Paymethod = "";
           for (var length = 0; length < _scBaseUtils.getAttributeCount(shipmentLine); length++) 
                {
					var paymentMethod = null;
					paymentMethod =  _scModelUtils.getModelObjectFromPath("Order.PaymentMethods.PaymentMethod", shipmentLine[length]); 
					for(var i = 0; i < _scBaseUtils.getAttributeCount(paymentMethod); i++)
					{
						var paymentValues = "";
						var paymentType = _scModelUtils.getStringValueFromPath("PaymentType", paymentMethod[i]);
						if (paymentType === "CREDIT_CARD")
						{
						var paymentCardType =_scModelUtils.getStringValueFromPath("CreditCardType", paymentMethod[i]);
						var cardNo = _scModelUtils.getStringValueFromPath("DisplayCreditCardNo", paymentMethod[i]);
						paymentValues = paymentCardType + " - " + cardNo;						
						}  
						else if (paymentType === "PAYPAL")
						{
						var payref3 = _scModelUtils.getStringValueFromPath("PaymentReference3", paymentMethod[i]);
						paymentValues = paymentType + " - " + payref3;
						 }
						 else if (paymentType === "GIFT_CARD")
						{
						var svcNo = _scModelUtils.getStringValueFromPath("DisplaySvcNo", paymentMethod[i]);
						paymentValues = paymentType + " - " + svcNo;
						}
						else if (paymentType === "VOUCHERS")
						{
						var payref1 = _scModelUtils.getStringValueFromPath("PaymentReference1", paymentMethod[i]);
						paymentValues = paymentType + " - " + payref1;
						}					  
					   Paymethod = Paymethod + paymentValues + "  ";
					}
					return Paymethod;                
				}
		},
		
		initializeScreen: function(
        event, bEvent, ctrl, args) {
            var shipmentData = null;
            shipmentData = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
            _wscShipmentUtils.showNextTask(
            this, shipmentData, "Shipment.ShipNode.ShipNode", "lnkBRP", "lnkBRP", "", "lnkPack", "lnkPack", "lnkStartCustomerPickup");
            if (
            _iasContextUtils.isMobileContainer()) {
                this.fireSetWizardDescEvent(
                shipmentData);
                _scWidgetUtils.hideWidget(
                this, "img_TimeRmnClockWeb", false);
            } else {
                this.updateScreenTitle(
                this, shipmentData);
                this.initializeSLAwidget(
                shipmentData);
            }
            status = _scModelUtils.getStringValueFromPath("Shipment.Status.Status", shipmentData);
            if(_scBaseUtils.equals(status, "1100.70.06.20"))
            {
               _scWidgetUtils.setLabel(this,"lnkBRP","Continue Pick Pack",false);     
            }
            
            if (!(
            _wscShipmentUtils.showSLA(
            status))) {
                _scWidgetUtils.hideWidget(
                this, "img_TimeRmnClockWeb", true);
            }
            _wscShipmentUtils.showHideHoldLocation(
            this, shipmentData, "lblHoldLocation");
            this.handleStoreAddressPanel(
            shipmentData);
            var deliveryMethod = null;
            deliveryMethod = _scModelUtils.getStringValueFromPath("Shipment.DeliveryMethod", shipmentData);
            if (
            _scBaseUtils.equals(
            deliveryMethod, "PICK")) {
                this.initializeScreenPickupInStore(
                shipmentData);
            } else if (
            _scBaseUtils.equals(
            deliveryMethod, "SHP")) {
                this.initializeScreenShipFromStore(
                shipmentData);
            }
        }
});
});