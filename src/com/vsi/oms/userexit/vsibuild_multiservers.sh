#!/bin/sh
#set -x

       echo "Sending mail intimating start of build"
       echo -e "OMS Production Build has been started for jira $3\n\nBuild Command: ./vsibuild_multiservers.sh $1 $2 $3\n\nServers : lin-vsi-omspap1, lin-vsi-omspap2, lin-vsi-omspag1, lin-vsi-omspag2"|mailx -r "oms_build@vitaminshoppe.com" -s "OMS Production Build has been started"  oms@vitaminshoppe.com operators@vitaminshoppe.com esbsupportteam@vitaminshoppe.com dcs@vitaminshoppe.com omsdba@vitaminshoppe.com sdteam@vitaminshoppe.com
#oms@vitaminshoppe.com Joseph.Gargano@vitaminshoppe.com Sagar.Sapkota@vitaminshoppe.com


	validate_input () {

        echo  "validate input"
        if [ $# -ne 2 ]
        then
            echo "Usage:  $1 URL"
            echo "$2   :build/cdtbuild "
            echo "Example:  $1 http://10.75.19.61/ster/BR3 $2 cdtbuild"
            exit 1
        fi
        }
        build () {

        rm -rf  /opt/sterling/VSI_Build_Workspace
        mkdir  /opt/sterling/VSI_Build_Workspace

        mkdir /opt/sterling/VSI_Build_Workspace/VSI_BR1
        mkdir /opt/sterling/VSI_Build_Workspace/VSI_BR1/dev
        mkdir /opt/sterling/VSI_Build_Workspace/VSI_BR1/dev_sterling
        mkdir /opt/sterling/VSI_Build_Workspace/VSI_BR1/dev_archive


        svn checkout $1 /opt/sterling/VSI_Build_Workspace/VSI_BR1/dev   | tee build.log
        if [ "$2" == "cdtbuild" ]
        then
        echo 'Invoking CDT build'
                 #/Sterling/bin/sci_ant.sh -Dcdt.apply=true -f /opt/sterling/build/vsibuild_som.xml | tee -a build.log
                 /Sterling/Migration/apache-ant-1.7.1/bin/ant -Dcdt.apply=true -f /opt/sterling/build/vsibuild_som.xml | tee -a build.log

        else
        echo  'Invoking build with no CDT'
                 #/Sterling/bin/sci_ant.sh  -f /opt/sterling/build/vsibuild_som.xml | tee -a build.log
               /Sterling/Migration/apache-ant-1.7.1/bin/ant  -f /opt/sterling/build/vsibuild_som.xml | tee -a build.log

        fi

	}

shutdown_server () {
    echo "inside shutdown_server fn "   $1 $2
    ssh $1    <<REMOTE
        if [ "$2" = "APP" ] ; then
          echo "Shutting down Jboss "
          /etc/init.d/jboss stop > /dev/null
#/usr/jboss-eap-6.4.0/bin/jboss-cli.sh --connect --command=:shutdown > /dev/null

        else
         /opt/sterling/build/killAllservers.sh java > /dev/null
         # for i in `ps -ef | grep -v grep | grep invokear | awk -F"-inv" '{print $2}' | sort -n | uniq | awk '{print $2}'`
         # do
           # PID=`ps -ef | grep java |grep -v hyperic|grep -v grep | grep -v _wrap| grep "okeargs $i"|awk '{print $2}'`
            #echo "Shutting down $i $PID"
            #kill -9 $PID
          #done
        fi 
REMOTE
}

shutdown_server_local () {
    echo "inside shutdown_server fn "   $1 $2
   
          echo "Shutting down Jboss "
          /etc/init.d/jboss stop > /dev/null
       /opt/sterling/build/startservers.sh > /dev/null
#/usr/jboss-eap-6.4.0/bin/jboss-cli.sh --connect --command=:shutdown > /dev/null

        
}


start_server ()  {
   echo "Starting server " $1 $2
   ssh $1  <<REMOTE
   if  [ "$2" = "APP" ] ; then
       /Sterling/bin/sci_ant.sh -f /opt/sterling/build/vsibuild_som.xml  deploy
       /etc/init.d/jboss start > /dev/null 
#nohup /usr/jboss-eap-6.4.0/bin/standalone.sh -b=0.0.0.0 -bmanagement=0.0.0.0 > /dev/null &
#          /opt/sterling/build/startservers.sh > /dev/null

   else   
       /opt/sterling/build/startservers.sh
   fi
REMOTE

}

start_server_local ()  {
   echo "Starting server " $1 $2
   
      /etc/init.d/jboss start > /dev/null    
  
#nohup /usr/jboss-eap-6.4.0/bin/standalone.sh -b=0.0.0.0 -bmanagement=0.0.0.0 > /dev/null &

}

copy_buildfiles () {

     echo "Copy build files "  $1
    #rsync -r /Sterling/UpdatesDirectory  $1:$2 // removed as part of SOS-29
     rsync -r /Sterling/jar $1:$2
     rsync -r /Sterling/rcp $1:$2
     #rsync -r /Sterling/rcpdrop $1:$2 // removed as part of SOS-29
     rsync -r /Sterling/MQBinding $1:$2
	 rsync -r /Sterling/resources $1:$2
     #rysnc -r /Sterling/rcpextn $1:$2 // removed as part of SOS-29
     rsync -r /Sterling/extensions $1:$2 
      rsync -r /Sterling/properties $1:$2
     rsync -r /Sterling/external_deployments  $1:$2
}

# IP1, IP2 App box, IP3, IP4 agent box
IP1=10.3.51.160
IP2=10.3.51.161
IP3=10.3.51.162
IP4=10.3.51.163
DEST=/Sterling

  validate_input $1 $2
  shutdown_server_local $IP1 APP
 shutdown_server $IP2 APP
  shutdown_server $IP3 AGENT
 shutdown_server $IP4 AGENT

  build $1  $2

 sleep 30
 #copy_buildfiles $IP1 $DEST
 copy_buildfiles $IP2 $DEST
 copy_buildfiles $IP3 $DEST
 copy_buildfiles $IP4 $DEST

  start_server_local $IP1 APP
  start_server $IP2 APP
  echo "Sleeping for 30 seconds"  
   sleep 30

start_server $IP3 AGENT
start_server $IP4 AGENT

sleep 5m

curl -Is  http://10.3.51.160:8080/smcfs/console/login.jsp | head -1 | grep "HTTP/1.1 200 OK" &> /dev/null
if [ $? == 0 ];
then
        curl http://10.3.51.160:8080/smcfs/console/login.jsp &> /dev/null | grep "Yantra_7x"

                if [ $? == 0 ]; then
                        echo -e "OMS Production Build has failed with Yantra_7x error.\n\nPlease analyse build logs."|mailx -r "oms_build@vitaminshoppe.com" -s "OMS Production Build has been failed" oms@vitaminshoppe.com operators@vitaminshoppe.com esbsupportteam@vitaminshoppe.com dcs@vitaminshoppe.com omsdba@vitaminshoppe.com sdteam@vitaminshoppe.com

                else
                        echo -e "OMS Production Build has been completed.\n\nPlease move all the corresponding jira to QA status."|mailx -r "oms_build@vitaminshoppe.com" -s "OMS Production Build has been completed" oms@vitaminshoppe.com operators@vitaminshoppe.com esbsupportteam@vitaminshoppe.com dcs@vitaminshoppe.com omsdba@vitaminshoppe.com sdteam@vitaminshoppe.com
                fi
else
        echo -e "OMS Production Build has failed.\n\nPlease analyse build logs."|mailx -r "oms_build@vitaminshoppe.com" -s "OMS Production Build has been failed" oms@vitaminshoppe.com operators@vitaminshoppe.com esbsupportteam@vitaminshoppe.com dcs@vitaminshoppe.com omsdba@vitaminshoppe.com sdteam@vitaminshoppe.com
fi


#echo -e "OMS Production Build has been completed."|mailx -r "oms_build@vitaminshoppe.com" -s "OMS Production Build has been completed"   ActRetirement@vitaminshoppe.com oms@vitaminshoppe.com
# oms@vitaminshoppe.com  Joseph.Gargano@vitaminshoppe.com Sagar.Sapkota@vitaminshoppe.com

