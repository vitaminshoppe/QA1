#!/bin/ksh
#
#su - shivraj.ugale -c /home/OMSAutomationScripts/Missing_Credit_Card_Expiry_Date.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database

RUN_SCRIPT=PROD


if [ $RUN_SCRIPT == "PROD" ] 
then    	
	USER=sterling_reader01
	PWD=xpruhjJ3Px
	DatabaseName=OMS_PROD
	HOST_NAME=lin-vsi-omspdb3
	PORT=1881
	SERVICE_SID="(SERVICE_NAME=OMSPROD)"	
	#st3rling0ms, xpruhjJ3Px, lin-vsi-omspdb3, 1881, OMSPROD
else

	ORACLE_HOME=/u01/app/oracle/product/11.2.0/
	LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
	TNS_ADMIN=$ORACLE_HOME/network/admin
	PATH=$ORACLE_HOME/bin:$PATH:.

    USER=sterling
	PWD=st34rl1ng0ms
	DatabaseName=omstest
	HOST_NAME=10.3.53.74
	PORT=1581
	SERVICE_SID="(SID=omstest)"	
fi

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

ARCDIR=$LOGDIR/arch
LOGFILE1=$LOGDIR/VSI_CANCEL_ITEMS.txt
LOGFILE2=$LOGDIR/VSI_CANCEL_ORDERS.txt
#LOGFILE2=$LOGDIR/CCMissingExpiryDate2.txt
TO_ADDRS="oms@vitaminshoppe.com"
#TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set linesize 3000
set trimspool on
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-YY HH12:MI:SS PM';

SELECT TRIM(YOL.ITEM_ID) AS ITEM_ID, SUM(YORS.STATUS_QUANTITY) AS CANCEL_QTY
FROM STERLING.YFS_ORDER_AUDIT YOA, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOA.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS 
AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YS.STATUS LIKE '9000%'
AND YOL.LINE_TYPE = 'SHIP_TO_HOME'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 60,'YYYYMMDD')
AND YOA.ORDER_AUDIT_KEY > TO_CHAR(SYSDATE - 0.042,'YYYYMMDD')
AND YOA.CREATETS > SYSDATE - '0.041667'
AND YOA.REASON_CODE = 'VSI_CANCEL'
AND YOA.MODIFYUSERID = 'VSIScheduleOrder'
GROUP BY YOL.ITEM_ID
ORDER BY CANCEL_QTY DESC;

spool off
exit 0
END

order_msg1=$(cat $LOGFILE1)


sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set linesize 3000
set trimspool on
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
spool $LOGFILE2
ALTER SESSION SET nls_date_format = 'DD-MON-YY HH12:MI:SS PM';

SELECT TRIM(YOH.CREATETS) AS ORDER_DATE, TRIM(YOA.CREATETS) AS CANCEL_DATE,
YOH.ORDER_NO, TRIM(YOL.ITEM_ID) AS ITEM_ID, TRIM(YORS.STATUS_QUANTITY) AS CANCEL_QTY, (YOL.UNIT_PRICE * YORS.STATUS_QUANTITY) AS CANCELLED_AMT, 
YOH.CUSTOMER_EMAILID, TRIM(YOH.ENTERPRISE_KEY) AS ENTP_KEY
FROM STERLING.YFS_ORDER_AUDIT YOA, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOA.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS 
AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT' AND YORS.STATUS_QUANTITY > '0'
AND YORS.STATUS = '9000'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 60,'YYYYMMDD')
AND YOA.ORDER_AUDIT_KEY > TO_CHAR(SYSDATE - 0.042,'YYYYMMDD')
AND YOA.CREATETS > SYSDATE - '0.041667'
AND YOL.LINE_TYPE = 'SHIP_TO_HOME'
AND YOA.REASON_CODE = 'VSI_CANCEL'
ORDER BY YOH.ORDER_DATE DESC;


spool off
exit 0
END

order_msg2=$(cat $LOGFILE2)

cancelCount1HR=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"

SELECT COUNT(DISTINCT(YOH.ORDER_HEADER_KEY)) AS CANCEL_1HR
FROM STERLING.YFS_ORDER_AUDIT YOA, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOA.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS 
AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT' AND YORS.STATUS_QUANTITY > '0'
AND YORS.STATUS = '9000'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 60,'YYYYMMDD')
AND YOA.ORDER_AUDIT_KEY > TO_CHAR(SYSDATE - 0.042,'YYYYMMDD')
AND YOA.CREATETS > SYSDATE - '0.041667'
AND YOL.LINE_TYPE = 'SHIP_TO_HOME'
AND YOA.REASON_CODE = 'VSI_CANCEL';


exit 0
END
)

cancelCount1DAY=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"

SELECT COUNT(DISTINCT(YOH.ORDER_HEADER_KEY)) AS CANCEL_1DAY
FROM STERLING.YFS_ORDER_AUDIT YOA, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOA.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS 
AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT' AND YORS.STATUS_QUANTITY > '0'
AND YORS.STATUS = '9000'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 60,'YYYYMMDD')
AND YOA.ORDER_AUDIT_KEY > TO_CHAR(SYSDATE - 0.042,'YYYYMMDD')
AND YOL.LINE_TYPE = 'SHIP_TO_HOME'
AND YOA.REASON_CODE = 'VSI_CANCEL';


exit 0
END
)


createCount1HR=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"

SELECT COUNT(YOH.ORDER_HEADER_KEY) AS CREATE_1HR
FROM STERLING.YFS_ORDER_HEADER YOH WHERE YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 0.5,'YYYYMMDD')
AND YOH.CREATETS > SYSDATE - '0.041667';

exit 0
END
)

createCount1DAY=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"

SELECT COUNT(YOH.ORDER_HEADER_KEY) AS CREATE_1DAY
FROM STERLING.YFS_ORDER_HEADER YOH 
WHERE YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 0.001,'YYYYMMDD');

exit 0
END
)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
echo "${DT}_${TM}"|mailx -s "Error running VSI CANCEL report" $TO_ADDRS
#elif [ $cancelCount1HR -eq 0 ]
#then echo -e "No orders cancelled in last 1 hour due to VSI_CANCEL.\n\n 1) Total Unique Order Count for today : $createCount1DAY \n 2) Total Unique Order Count added in the last hour: $createCount1HR \n\n 3) Total Unique Order Cancelled for ${DT} : $cancelCount1DAY \n 4) Total Unique Order Cancelled in the last hour: $cancelCount1HR" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders cancelled due to VSI_CANCEL in last 1 hr" -a /home/OMSAutomationScripts/VSI_CANCEL_ORDERS.txt $TO_ADDRS

elif [ $cancelCount1HR -gt 10 ] || [ $cancelCount1DAY -gt 20 ]
then
if [ $cancelCount1HR -gt 10 ]
then
#REPORTDATE=`date +%Y_%m_%d`
echo -e "Hello,\n\nSummary of VSI cancellations during scheduling provided below. Attached text file includes order data.\n\n 1) Total Unique Order Count for today : $createCount1DAY \n 2) Total Unique Order Count added in the last hour: $createCount1HR \n\n 3) Total Unique Order Cancelled for ${DT} : $cancelCount1DAY \n 4) Total Unique Order Cancelled in the last hour: $cancelCount1HR\n\n\n*******Please stop Scheduling agent and  analyze cancellation reason if #4 is 30 and above.*******\n\n\nItem & Quantity combination for VSI CANCEL provided below:\n\n$order_msg1" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders cancelled due to VSI_CANCEL in last 1 hr: $cancelCount1HR" -a /home/OMSAutomationScripts/VSI_CANCEL_ORDERS.txt $TO_ADDRS

fi

if [ $cancelCount1DAY -gt 20 ]
then
#REPORTDATE=`date +%Y_%m_%d`
echo -e "Hello,\n\nSummary of VSI cancellations during scheduling provided below. Attached text file includes order data.\n\n 1) Total Unique Order Count for today : $createCount1DAY \n 2) Total Unique Order Count added in the last hour: $createCount1HR \n\n 3) Total Unique Order Cancelled for ${DT} : $cancelCount1DAY \n 4) Total Unique Order Cancelled in the last hour: $cancelCount1HR\n\n\n*******Please stop Scheduling agent and  analyze cancellation reason if #4 is 30 and above.*******\n\n\nItem & Quantity combination for VSI CANCEL provided below:\n\n$order_msg1" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders cancelled due to VSI_CANCEL in last 1 hr: $cancelCount1HR" -a /home/OMSAutomationScripts/VSI_CANCEL_ORDERS.txt $TO_ADDRS

fi
fi
