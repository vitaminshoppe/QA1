#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/Count of Orders Created/Processed from ATG,OMS,AZDC,VADC.sh

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

#LOGFILE2=$LOGDIR/Count of Orders Created/Processed from ATG,OMS,AZDC,VADC

TO_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"

TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"
TO_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD FTC_ADDRS TO_ADDRS_ON_FAILURE


orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(*) from STERLING.yfs_order_header 
where ORDER_HEADER_KEY > TO_CHAR(SYSDATE,'YYYYMMDD') and  extn_dtc_order='Y'  and 
order_type in  'WEB';
exit 0
END
)

orderCount2=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select  count(*) as orderscount from sterling.yfs_order_header where ORDER_HEADER_KEY > TO_CHAR(SYSDATE,'YYYYMMDD') and ENTERPRISE_KEY ='ADP'  and 
order_type in  'WEB';
exit 0
END
)

orderCount3=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(*) from STERLING.yfs_export where export_key > TO_CHAR(SYSDATE,'YYYYMMDD') and message like '%OrderNo="WO%'  and flow_name ='VSISendReleaseToWMS_DB' and message like '%ShipNode="9004"%';
exit 0
END
)

orderCount4=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select COUNT(*) from STERLING.yfs_export where export_key > TO_CHAR(SYSDATE,'YYYYMMDD') and message like '%OrderNo="WO%'  and flow_name ='VSISendReleaseToWMS_DB' and message like '%ShipNode="9005"%';
exit 0
END
)

#order_msg1=$(cat $LOGFILE2)



if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`

echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE

else
echo -e "Hello,\n\nSummary of Orders  flow from created(ATG) to released(DCs). \n\n 1) Total ATG Orders Created for today : $orderCount1 \n 2) Total ATG ADP Orders Created for today: $orderCount2 \n\n 3) Total ATG Orders released to 9004(VADC) for today  : $orderCount3 \n 4) Total ATG Orders released to 9005 (AZDC) for today: $orderCount4\n\n\n*******" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders created/processed ATG, OMS, AZDC, VADC" $TO_ADDRS

fi
