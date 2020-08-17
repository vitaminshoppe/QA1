#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/ftc_cancel_orders.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.


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

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"



export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts
ARCDIR=$LOGDIR/Arch

LOGFILE2=$LOGDIR/FTC_Cancel_MonitorOrderResults_Orders.txt

FTC_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"

TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"
FTC_ADDRS="oms@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD FTC_ADDRS TO_ADDRS_ON_FAILURE

orderCount2=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
from STERLING.yfs_inbox inx, sterling.yfs_order_header yoh, STERLING.yfs_order_line yol,sterling.yfs_order_date yod  where inx.flow_name='VSIFTCCancelOrderLinesMonitor' 
and yoh.order_header_key=inx.order_header_key
and inx.order_line_key=yol.order_line_key
and yod.order_line_key=inx.order_line_key
and yod.date_type_id='YCD_FTC_CANCEL_DATE'
and inx.inbox_key > TO_CHAR(SYSDATE,'YYYYMMDD');
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
spool $LOGFILE2
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
select inx.createts as alertrasiedDate, yoh.order_no, yoh.order_date,yol.line_type,yol.item_id,yod.actual_date as ftc_cancel_date
from STERLING.yfs_inbox inx, sterling.yfs_order_header yoh, STERLING.yfs_order_line yol,sterling.yfs_order_date yod  where inx.flow_name='VSIFTCCancelOrderLinesMonitor' 
and yoh.order_header_key=inx.order_header_key
and inx.order_line_key=yol.order_line_key
and yod.order_line_key=inx.order_line_key
and yod.date_type_id='YCD_FTC_CANCEL_DATE'
and inx.inbox_key > TO_CHAR(SYSDATE,'YYYYMMDD')
ORDER by inx.createts ASC;
spool off
exit 0
END

order_msg2=$(cat $LOGFILE2)



if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount2 -eq 0 ]
then
echo -e "No DTC orders are stuck in Created status with Fraud Hold.\n\nScript Name:ftc_cancel_orders.sh \nCriteria: Orders older than 80 mins and upto 120 days" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No FTC Cancel orders will cancel in next to 2 days.Count:$orderCount2" $FTC_ADDRS
elif [ $orderCount2 -gt 0 ]
then
echo -e "Orders from OMS PROD which will get cancel in next two days for FTC cancel.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:ftc_cancel_orders.sh \nCriteria: Orders older than 80 mins and upto 120 days.\n\n$order_msg2"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders will get FTC cancel in next two days. Count:$orderCount2" $FTC_ADDRS
fi


