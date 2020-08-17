#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/Total no. of orders created for last 1 hour.sh

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

LOGFILE=$LOGDIR/support4/OMS-1928/marketplaceordersresults.txt
#LOGFILE2=$LOGDIR/Total no. of orders created for last 1 hour




TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com operators@vitaminshoppe.com"
FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com operators@vitaminshoppe.com"

#TO_ADDRS_ON_FAILURE="Yadavendra.Singh@vitaminshoppe.com"
#FROM_ADDRS="Yadavendra.Singh@vitaminshoppe.com"
#TO_ADDRS="Yadavendra.Singh@vitaminshoppe.com"


export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS FROM_ADDRS TO_ADDRS_ON_FAILURE LOGFILE


orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(*) from STERLING.yfs_order_header
where createts > (sysdate-1/24) and  extn_dtc_order='Y'  and   DRAFT_ORDER_FLAG  = 'N' and DOCUMENT_TYPE='0001' and entry_type='WEB' and order_type='WEB' and ENTERPRISE_KEY='VSI.com' ;
exit 0
END
)

orderCount2=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(DISTINCT YOH.Order_no) from sterling.yfs_order_header yoh , sterling.yfs_order_line yol
WHERE
yoh.createts > (sysdate-1/24)  and yoh.order_header_key=yol.order_header_key and yoh.entry_type='WEB' and yoh.order_type='WEB' and yol.LINE_TYPE in ('PICK_IN_STORE')
and yoh.DRAFT_ORDER_FLAG  = 'N' and yoh.DOCUMENT_TYPE='0001' ;
exit 0
END
)

orderCount3=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(DISTINCT YOH.Order_no) from sterling.yfs_order_header yoh , sterling.yfs_order_line yol
WHERE
yoh.createts > (sysdate-1/24)  and yoh.order_header_key=yol.order_header_key and yoh.order_type='POS' and yol.LINE_TYPE in ('SHIP_TO_HOME') and yoh.DRAFT_ORDER_FLAG  = 'N' and yoh.DOCUMENT_TYPE='0001' ;
exit 0
END
)

orderCount4=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(DISTINCT YOH.Order_no) from sterling.yfs_order_header yoh , sterling.yfs_order_line yol
WHERE
yoh.createts > (sysdate-1/24)  and yoh.order_header_key=yol.order_header_key and yoh.order_type='POS' and yol.LINE_TYPE in ('PICK_IN_STORE') and yoh.DRAFT_ORDER_FLAG  = 'N' and yoh.DOCUMENT_TYPE='0001' ;
exit 0
END
)

order_msg=$(cat $LOGFILE)

orderCount6=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select Count(*) from sterling.yfs_order_header WHERE createts > (sysdate-1/24) and ENTERPRISE_KEY='ADP' and DRAFT_ORDER_FLAG  = 'N' and DOCUMENT_TYPE='0001';
exit 0
END
)


sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 10000
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
spool $LOGFILE
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS';
select TO_CHAR(createts,'YYYY-MM-DD') As Order_createts,ENTERPRISE_KEY ,TO_CHAR(ORDER_DATE,'YYYY-MM-DD') As Order_date,  count(*) as orderscount  from sterling.yfs_order_header where
createts > (sysdate-1/24) and order_type='Marketplace' GROUP BY TO_CHAR(createts,'YYYY-MM-DD') ,ENTERPRISE_KEY,TO_CHAR(ORDER_DATE,'YYYY-MM-DD');
exit 0
END



#order_msg1=$(cat $LOGFILE2)

 

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`

echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE

else
REPORTDATE=`date +%Y-%m-%d`
IHRBACKTIME=`date -d '1 hour ago' +%H:%M:%S`
NOWTIME=`date +%H:%M:%S`

echo $IHRBACKTIME
echo $NOWTIME
echo -e "Hello,\n\nTotal no. of orders created in OMS in last 1 hour (between $IHRBACKTIME to $NOWTIME) on $REPORTDATE . \n\n 1) Total WEB VSI.com STH order : $orderCount1\n 2) Total WEB BOPUS orders : $orderCount2\n 3) Total POS STH : $orderCount3\n 4) Total POS pick in store orders : $orderCount4\n 5) Total ADP orders created : $orderCount6\n\n 6) Total Marketplace Orders:.\n\n$order_msg\n\n\n***************" | mailx -r $FROM_ADDRS -s "Total no. of orders created in OMS for last 1 hour on $REPORTDATE" -a $LOGFILE $TO_ADDRS

fi
