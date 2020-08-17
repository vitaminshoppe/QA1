#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/OMS-2016.sh
Test Update
ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

TO_ADDRS_ON_FAILURE="OMS@vitaminshoppe.com"
FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
TO_ADDRS="OMS@vitaminshoppe.com,operators@vitaminshoppe.com"

#RUN_SCRIPT (Value should be QA or PROD)

RUN_SCRIPT=PROD


if [ $RUN_SCRIPT == "PROD" ] 
then    	
	USER=sterling_reader01
	PWD=xpruhjJ3Px2
	DatabaseName=OMS_PROD
	HOST_NAME=lin-vsi-omspdb3
	PORT=1881
	SERVICE_SID="(SERVICE_NAME=OMSPROD)"	
	#st3rling0ms, xpruhjJ3Px, lin-vsi-omspdb3, 1881, OMSPROD
	
	
	TO_ADDRS_ON_FAILURE="OMS@vitaminshoppe.com"
	FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
	TO_ADDRS="OMS@vitaminshoppe.com,operators@vitaminshoppe.com"

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
	
	TO_ADDRS_ON_FAILURE="Yadavendra.Singh@vitaminshoppe.com"
	FROM_ADDRS="Yadavendra.Singh@vitaminshoppe.com"
	TO_ADDRS="Yadavendra.Singh@vitaminshoppe.com"
fi

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"



export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts
ARCDIR=$LOGDIR/Arch

LOGFILE=$LOGDIR/support5/OMS-2016/vsi.com_inventory.txt
LOGFILE2=$LOGDIR/OMS-2016.sh


export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS FROM_ADDRS TO_ADDRS_ON_FAILURE LOGFILE


orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(1) from sterling.yfs_inventory_supply where  shipnode_key in ('9004','9005') and supply_type='ONHAND' and inventory_item_key in 
(select inventory_item_key from sterling.yfs_inventory_item where organization_code='VSI.com') ;
exit 0
END
)


order_msg=$(cat $LOGFILE)


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
select invs.shipnode_key,invi.item_id,invs.quantity from
STERLING.yfs_inventory_item invi,STERLING.yfs_inventory_supply invs where
invi.inventory_item_key=invs.inventory_item_key and invi.organization_code='VSI.com' and
invs.supply_type='ONHAND' and invs.shipnode_key in ('9004','9005') GROUP BY invs.shipnode_key,invi.item_id,invs.quantity;
exit 0
END

#order_msg1=$(cat $LOGFILE2)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE

elif [ $orderCount1 -gt 0 ]
then
echo -e "Hello,\n\n Below are the details of VSI.com organization inventory for 9004 and 9005 DC\n\n  $order_msg\n\n\n***************" | mailx -r $FROM_ADDRS -s "VSI.com inventory for 9004 and 9005 DC: $orderCount1" -a $LOGFILE $TO_ADDRS

fi
