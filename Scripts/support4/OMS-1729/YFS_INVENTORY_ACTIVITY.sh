#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/Count in YFS_INVENTORY_ACTIVITY table.sh

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

#LOGFILE2=$LOGDIR/Count in YFS_INVENTORY_ACTIVITY table.sh

TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"

FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"

FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS FROM_ADDRS TO_ADDRS_ON_FAILURE 


orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
select count(distinct inventory_activity_key) from sterling.yfs_inventory_activity;
exit 0
END
)

echo $orderCount1


if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount1 -gt 2000 ]

then
echo -e "Hello,\n\n Number of records YFS_INVENTORY_ACTIVITY table is $orderCount1.\n\n Please monitor the error percentage of BOPS inventory monitor of all 3 apps.  \n\n\n***************" | mailx -r $FROM_ADDRS -s "Number of records in  YFS_INVENTORY_ACTIVITY table" $TO_ADDRS
fi


