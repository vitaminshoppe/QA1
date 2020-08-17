#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/Missing_Credit_Card_Expiry_Date.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.


#Change user, pswd and db to point to target database

USER=sterling_reader01
PWD=xpruhjJ3Px
DatabaseName=OMS_PROD

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

ARCDIR=$LOGDIR/Arch
LOGFILE1=$LOGDIR/BOPS_MonitorOrderResults.txt
LOGFILE2=$LOGDIR/DTC_MonitorOrderResults_Remorse.txt
LOGFILE3=$LOGDIR/DTC_MonitorOrderResults_Fraud.txt
LOGFILE4=$LOGDIR/DTC_MonitorOrderResults_AwaitPayInfo.txt
LOGFILE5=$LOGDIR/DTC_MonitorOrderResults_Other.txt
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
BOPS_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"
DTC_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"
FRAUD_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com navichal@vitaminshoppe.com Lauren.Alfant@vitaminshoppe.com sjordan@vitaminshoppe.com Amanda.Alfonso@vitaminshoppe.com rlora@vitaminshoppe.com 
DSoneji@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"
#DTC_ADDRS="irfan.uddin@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD BOPS_ADDRS DTC_ADDRS FRAUD_ADDRS TO_ADDRS_ON_FAILURE

orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS 
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -120,'YYYYMMDD')
AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOL.LINE_TYPE IN ('PICK_IN_STORE','SHIP_TO_STORE')
AND YORS.STATUS < '1500'
AND YORS.STATUS NOT IN ('1300','1310','1400')
AND SYSDATE - YORS.STATUS_DATE > '0.010';
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YOH.ORDER_NO) AS ORDER_NO, 
CASE 
WHEN TRIM(YS.DESCRIPTION) = 'Procurement Transfer Order Created' THEN 'ProcTOCreated' 
WHEN TRIM(YS.DESCRIPTION) = 'JDA STS Acknowledged' THEN 'JDA STS Ack'
ELSE TRIM(YS.DESCRIPTION) END AS "STATUS",TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS,STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -120,'YYYYMMDD')
AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOL.LINE_TYPE IN ('PICK_IN_STORE','SHIP_TO_STORE')
AND YORS.STATUS < '1500'
AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOHT.STATUS ='1100' AND YOHT.HOLD_TYPE not in ('VSI_FRD_REVIEW_HOLD')
AND SYSDATE - YORS.STATUS_DATE > '0.010'
ORDER BY YOH.ORDER_DATE ASC;
spool off
exit 0
END

order_msg1=$(cat $LOGFILE1)


orderCount2=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE +120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOHT.STATUS = '1100' AND YOHT.HOLD_TYPE = 'VSI_REMORSE_HOLD'
AND SYSDATE - YORS.STATUS_DATE > '0.010';
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE2
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT TRIM(YOH.ORDER_NO) AS ORDER_NO, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YS.DESCRIPTION) AS STATUS, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS, YOHT.HOLD_TYPE
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOHT.STATUS = '1100' AND YOHT.HOLD_TYPE = 'VSI_REMORSE_HOLD'
AND SYSDATE - YORS.STATUS_DATE > '0.010'
ORDER BY YOH.ORDER_DATE ASC;
spool off
exit 0
END

order_msg2=$(cat $LOGFILE2)


orderCount3=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOHT.STATUS = '1100' AND YOHT.HOLD_TYPE IN ('VSI_FRD_REVIEW_HOLD','VSI_FRAUD_HOLD')
AND SYSDATE - YORS.STATUS_DATE > '0.060';
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE3
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT DISTINCT(TRIM(YOH.ORDER_NO)) AS ORDER_NO, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YS.DESCRIPTION) AS STATUS, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS, YOHT.HOLD_TYPE
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE - 120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOHT.STATUS = '1100' AND YOHT.HOLD_TYPE IN ('VSI_FRD_REVIEW_HOLD','VSI_FRAUD_HOLD')
AND SYSDATE - YORS.STATUS_DATE > '0.060'
ORDER BY TRIM(YOH.ORDER_DATE) ASC;
spool off
exit 0
END

order_msg3=$(cat $LOGFILE3)


orderCount5=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -10,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
--AND YOHT.STATUS <> '1300' 
AND YOHT.HOLD_TYPE NOT IN ('VSI_FRD_REVIEW_HOLD','VSI_REMORSE_HOLD')
--AND YOH.PAYMENT_STATUS <> 'AWAIT_PAY_INFO'
AND YOHT.ORDER_HEADER_KEY  IN (select ot.ORDER_HEADER_KEY from  STERLING.YFS_ORDER_HOLD_TYPE ot where ot.ORDER_HEADER_KEY=YOHT.ORDER_HEADER_KEY
AND ot.STATUS='1100'
GROUP BY ot.ORDER_HEADER_KEY
HAVING COUNT(ot.ORDER_HEADER_KEY)=1
)
AND SYSDATE - YORS.STATUS_DATE > '0.010';
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE5
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT TRIM(YOH.ORDER_NO) AS ORDER_NO, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YS.DESCRIPTION) AS STATUS, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS, YOHT.HOLD_TYPE
FROM STERLING.YFS_ORDER_HEADER YOH,STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HOLD_TYPE YOHT
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY
AND YOH.ORDER_HEADER_KEY = YOHT.ORDER_HEADER_KEY 
AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -10,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' 
AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOHT.HOLD_TYPE NOT IN ('VSI_FRD_REVIEW_HOLD','VSI_REMORSE_HOLD')
AND YOHT.ORDER_HEADER_KEY  IN (select ot.ORDER_HEADER_KEY from  STERLING.YFS_ORDER_HOLD_TYPE ot where ot.ORDER_HEADER_KEY=YOHT.ORDER_HEADER_KEY
AND ot.STATUS='1100'
GROUP BY ot.ORDER_HEADER_KEY
HAVING COUNT(ot.ORDER_HEADER_KEY)=1)
AND SYSDATE - YORS.STATUS_DATE > '0.010'
ORDER BY YOH.ORDER_DATE ASC;
spool off
exit 0
END

order_msg5=$(cat $LOGFILE5)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount1 -gt 0 ]

then
echo -e "BOPS Orders from OMS PROD.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\n$order_msg1"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "BOPS Orders stuck in Created status. Count:$orderCount1" $BOPS_ADDRS
fi


if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount2 -gt 0 ]

then
echo -e "DTC Orders from OMS PROD stuck in Created status with Remorse Hold.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:Orders_Stuck_Created_Status.sh \nCriteria: Orders older than 80 mins and upto 120 days.\n\n$order_msg2"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "DTC Orders stuck in Created status with Remorse Hold. Count:$orderCount2" $DTC_ADDRS
fi


if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount3 -eq 0 ]
then
echo -e "No DTC orders are stuck in Created status with Fraud Hold.\n\nScript Name:Orders_Stuck_Created_Status.sh \nCriteria: Orders older than 80 mins and upto 120 days" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No DTC orders are stuck in Created status with Fraud Hold.Count:$orderCount3" $FRAUD_ADDRS
elif [ $orderCount3 -gt 0 ]
then
echo -e "DTC Orders from OMS PROD stuck in Created status with Fraud Hold.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:Orders_Stuck_Created_Status.sh \nCriteria: Orders older than 80 mins and upto 120 days.\n\n$order_msg3"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "DTC Orders stuck in Created status with Fraud Hold. Count:$orderCount3" $FRAUD_ADDRS
fi


if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount5 -gt 0 ]
then
echo -e "DTC Orders from OMS PROD stuck in Created status.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:Orders_Stuck_Created_Status.sh \nCriteria: Orders older than 80 mins and upto 10 days.\n\n$order_msg5"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "DTC Orders stuck in Created status. Count:$orderCount5" $DTC_ADDRS
fi


orderCount4=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOH.PAYMENT_STATUS = 'AWAIT_PAY_INFO'
AND SYSDATE - YORS.STATUS_DATE > '0.010';
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE4
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT DISTINCT(TRIM(YOH.ORDER_NO)) AS ORDER_NO, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YS.DESCRIPTION) AS STATUS, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS, YOH.PAYMENT_STATUS
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOH.PAYMENT_STATUS = 'AWAIT_PAY_INFO'
AND SYSDATE - YORS.STATUS_DATE > '0.010'
ORDER BY TRIM(YOH.ORDER_DATE) ASC;
spool off
exit 0
END

order_msg4=$(cat $LOGFILE4)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount4 -gt 0 ]

then
echo -e "DTC Orders from OMS PROD stuck in Created status with Awaiting Payment Info.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:Orders_Stuck_Created_Status.sh \nCriteria: Orders older than 80 mins and upto 120 days.\n\n$order_msg4"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "DTC Orders stuck in Created status with Awaiting Payment Info. Count:$orderCount4" $AZ_ADDRS
fi