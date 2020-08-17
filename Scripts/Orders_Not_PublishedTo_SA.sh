#!/bin/ksh
#
#su - shivraj.ugale -c /home/OMSAutomationScripts/Missing_Credit_Card_Expiry_Date.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database
USER=sterling_reader01
PWD=xpruhjJ3Px
DatabaseName=OMS_PROD

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

ARCDIR=$LOGDIR/Arch
LOGFILE1=$LOGDIR/SAPendingOrders.txt
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
TO_ADDRS="oms@vitaminshoppe.com SalesAuditEmailGroup@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com Arsalan.Aziz@vitaminshoppe.com Allison.Morris@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE = "oms@vitaminshoppe.com omsdba@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com"
#TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE

orderCount=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
SELECT COUNT(DISTINCT(ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_INVOICE
WHERE INVOICE_TYPE = 'SHIPMENT'
AND MODIFYTS BETWEEN SYSDATE - 120 AND SYSDATE - .071
AND STATUS = '00';
exit 0
END
)

TOT_PENING_AMT=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
SELECT (SUM(YOI.TOTAL_AMOUNT) - (SUM(YOI.OTHER_CHARGES + YOI.TAX))) AS TOT_PENING_AMT
FROM STERLING.YFS_ORDER_INVOICE YOI
WHERE INVOICE_TYPE = 'SHIPMENT'
AND YOI.MODIFYTS > TRUNC(SYSDATE-120,'DD') AND YOI.MODIFYTS < SYSDATE - .071 --TRUNC(SYSDATE,'DD') --AND MODIFYTS BETWEEN SYSDATE - 120 AND SYSDATE - .071
AND STATUS = '00';
exit 0
END
)

#SELECT (SUM(YOI.TOTAL_AMOUNT - YOI.TOTAL_TAX) - (SUM(YOI.OTHER_CHARGES))) AS TOT_AMT
#SAMPLE ORDER WITH SHIPPING & SALES TAX: WO16228672

DAY_PENDING_AMT=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
SELECT (SUM(YOI.TOTAL_AMOUNT) - (SUM(YOI.OTHER_CHARGES + YOI.TAX))) AS DAY_PENDING_AMT
FROM STERLING.YFS_ORDER_INVOICE YOI
WHERE INVOICE_TYPE = 'SHIPMENT'
AND YOI.MODIFYTS > TRUNC(SYSDATE-1,'DD') AND YOI.MODIFYTS < SYSDATE - .071 --TRUNC(SYSDATE,'DD')
AND STATUS = '00';
exit 0
END
)

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
spool $LOGFILE1
SELECT TRIM(YOI.ORDER_NO) AS ORDER_NO, YS.EXTN_TRANSACTION_NO AS TRANSACTION_NO, YOI.MODIFYTS
FROM STERLING.YFS_ORDER_INVOICE YOI,  STERLING.YFS_SHIPMENT YS
WHERE YOI.INVOICE_TYPE = 'SHIPMENT' 
AND YS.SHIPMENT_KEY = YOI.SHIPMENT_KEY
AND YOI.MODIFYTS BETWEEN SYSDATE - 120 AND SYSDATE - .071
AND YOI.STATUS = '00'
ORDER BY YOI.MODIFYTS ASC;
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount -eq 0 ]
then
echo -e "This morning’s query displayed no orders waiting to be sent to SA." | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders pending for SA.Count:$orderCount" $TO_ADDRS
elif [ $orderCount -gt 0 ]
then
echo -e "Orders from OMS PROD.\n\nPlease check why orders are not sent to SALES AUDIT. All the below orders have STATUS = '00'. \n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders pending to be published to SA. Count:$orderCount / DTC Total: $ $TOT_PENING_AMT / DTC Yest: $ $DAY_PENDING_AMT                                         " $TO_ADDRS
fi
#elif [ $orderCount -le 5 ]
#then
#echo -e "OMS Support,\n\nOrders from OMS PROD.\n\nPlease look into them at the earliest.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders stuck in Created status. Count:$orderCount" oms@vitaminshoppe.com