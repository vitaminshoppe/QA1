#nohup /opt/sterling/build/startIntegrationServer.sh VSIRTAMDeltaSyncAgent "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSIOrderCreate "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIKountCallSTHOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSICheckoutRiskified "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIScheduleSTHOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIReleaseSTHOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIMarketPlaceOrderCreate "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/agentserver_mod.sh VSIScheduleOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIScheduleBOPUSOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIResolveFraudHold "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIPaymentSuccessServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSISendRelease "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/agentserver_mod.sh VSIResolveTOBHold "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIResolveRemorseHold "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIPaymentCollection "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIPaymentExecution "-Xms1024m -Xmx2048m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIScheduleBackOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIADPPaymentCollection "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/startIntegrationServer.sh VSIRTAMDeltaSyncAgent "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSIOrderCreate "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSIScheduleBOPUSOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIReleaseOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSISendRelease "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIAuditPurge "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSICancelDraftOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/startIntegrationServer.sh VSICreateCustomer "-Xms512m -Xmx1024m" > /dev/null 2>&2 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIOrderMonitor  "-Xms256m -Xmx512m" > /dev/null 2>&1 & 

#nohup /opt/sterling/build/agentserver_mod.sh VSICloseOrder "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIReleaseAckFromWMSIntegServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIShipConfirmFromWMSIntegServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIShipConfirmInternalServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIResolveAlertServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

sleep 10;

#nohup /opt/sterling/build/startIntegrationServer.sh VSIMCLFullSyncAgent "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIStatusUpdates "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSICreateShipment "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSICancelFromJDA "-Xms256m -Xmx512m" > /dev/null 2>&1 & 

sleep 10;
#nohup /opt/sterling/build/startIntegrationServer.sh VSIMCLDeltaSyncAgent "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIWavedFromWMSIntegServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSICancelFromES "-Xms256m -Xmx512m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSISendInvoiceOnSuccess "-Xms512m -Xmx1024m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSIIssueGiftCard "-Xms256m -Xmx512m" > /dev/null 2>&1 &

sleep 10;

nohup /opt/sterling/build/startIntegrationServer.sh VSIPublishInvoice "-Xms256m -Xmx512m" > /dev/null 2>&1 & 

nohup /opt/sterling/build/startIntegrationServer.sh VSISendVGCActivation "-Xms256m -Xmx512m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIOGCInvoice "-Xms256m -Xmx512m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIReturnOrderCreateInvoice "-Xms256m -Xmx512m" > /dev/null 2>&1 &


sleep 10;

nohup /opt/sterling/build/agentserver_mod.sh VSIShipmentInvoiceServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSICRMCustomerSync "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSISyncInventory "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSISyncInventory "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/startIntegrationServer.sh adjustInventory "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh adjustInventory "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh adjustInventory "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSISTSUpdate "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/agentserver_mod.sh VSIRTAMAgent "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIItemBasedAllocation "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/agentserver_mod.sh VSIReturnOrderPaymentCollection "-Xms256m -Xmx512m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIAckFromJDA "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/startIntegrationServer.sh VSIARCreateUpdate "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIReleaseAckWMSWholesaleIntegServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIShipCnWholesaleFromWMSIntegServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/agentserver_mod.sh VSIReturnOrderPaymentExecution "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIOrderPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIShipmentPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIInboxPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/agentserver_mod.sh VSIReprocessPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIAuditPurge "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSITransferOrdPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIPersonInfoPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIInventoryAuditPurge "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSIConsolidateInventory "-Xms256m -Xmx512m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/agentserver_mod.sh VSICatalogIndex "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIInitialAdjustInventory "-Xms256m -Xmx512m" > /dev/null 2>&1 &

sleep 10;

#nohup /opt/sterling/build/startIntegrationServer.sh VSIComFullSyncAgent "-Xms256m -Xmx512m" > /dev/null 2>&1 & 

#nohup /opt/sterling/build/startIntegrationServer.sh VSIPriceSync "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIManageOrganization "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIManageCalendar  "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIUserLoadServer  "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh ManageItemServer  "-Xms256m -Xmx512m" > /dev/null 2>&1 & 

#nohup /opt/sterling/build/startIntegrationServer.sh VSICommJunStlmnt "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIDataMigrationLoadIntegServer "-XX:PermSize=512m -XX:MaxPermSize=1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIPrintPickPack "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIPrintCustomerReceipt "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIStoreUserLoad "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSISFSShipJDAServer "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSISFSConfirmShpToATG "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIPrintPackShipLabel "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSISOMReverseAllocation "-Xms512m -Xmx1024m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIMixedOrderFraudCheck "-Xms512m -Xmx1024m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIMixedCartJDAAllocation "-Xms512m -Xmx1024m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIWavedWMSWholesaleIntegServer "-Xms512m -Xmx1024m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &

nohup /opt/sterling/build/startIntegrationServer.sh VSIWholesaleOrderCreate "-Xms256m -Xmx512m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIReleaseAckFromWMSWholesaleIntegServer "-Xms256m -Xmx512m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIShipCnWholesaleFromWMSIntegServer "-Xms256m -Xmx512m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &

#nohup /opt/sterling/build/startIntegrationServer.sh VSIResolveInvHold "-Xms256m -Xmx512m -XX:PermSize=128m -XX:MaxPermSize=256m" > /dev/null 2>&1 &