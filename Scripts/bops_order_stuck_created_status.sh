#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/bops_order_stuck_created_status.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

MOBILE_PUSH_LIB=/home/OMSAutomationScripts/mobilepush/


#RUN_SCRIPT (Value should be QA or PROD)

RUN_SCRIPT=PROD


if [ $RUN_SCRIPT == "PROD" ] 
then    	
	USER=sterling_reader01
	PWD=xpruhjJ3Px
	DatabaseName=OMS_PROD
	HOST_NAME=lin-vsi-omspdb3
	PORT=1881
	SERVICE_SID="(SERVICE_NAME=OMSPROD)"	
	#st3rling0ms, xpruhjJ3Px, lin-vsi-omspdb3, 1881, OMSPROD_SVC
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


#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts


ARCDIR=$LOGDIR/Arch
LOGFILE1=$LOGDIR/BOPS_MonitorOrderResults.txt

#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
BOPS_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"

TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"
BOPS_ADDRS="oms@vitaminshoppe.com"
#BOPS_ADDRS="Yadavendra.Singh@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD BOPS_ADDRS TO_ADDRS_ON_FAILURE

orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
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



mydate=$(date '+%d/%m/%Y %H:%M:%S')

#orderCount1=12

orderCount1=$(echo $orderCount1 | sed -e 's/\r//g')


if [ $orderCount1 -gt 10 ]
then
	echo Hey that\'s a large number.
	#echo "BOPUS Orders stuck in created status".
	#echo "Count is $mycount at $mydate".
	
	sh $MOBILE_PUSH_LIB'vsisupport_ibmpush.sh' "no. of BOPUS Orders stuck in created status" "Count is $orderCount1 at $mydate"
else
	echo "Count is below threshold"
fi


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