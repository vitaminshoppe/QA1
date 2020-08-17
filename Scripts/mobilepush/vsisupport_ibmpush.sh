#!/bin/bash


EMAIL_TO=oms@vitaminshoppe.com
EMAIL_FROM=oms_auto_notification@vitaminshoppe.com


message_title="DTC Orders stuck in created status"
message_payload="Count is 13 at 11AM"

#message_title=""
#message_payload=""
#if [ "$#" -ge 2 ]; then
# message_tag=$1
# message_title=$2
# message_payload=$3
#fi


if [ "$#" -ge 1 ]; then
 message_title=$1
 message_payload=$2
fi

message_tag="vitaminshoppe"

#INPUT_JSON='{"message": {"alert": "'"$message_title"'"},"settings": {"gcm": {"payload": {"desc": "'"$message_payload"'"}}}}'
INPUT_JSON='{"message": {"alert": "'"$message_title"'"},"settings": {"gcm": {"payload": {"desc": "'"$message_payload"'"}}},"target": {	"tagNames": ["'"$message_tag"'"]}}'



echo "$INPUT_JSON"

############ GET ACCESS_TOKEN FOR IBM PUSH NOTIFICATION ##############

#export CURL_CA_BUNDLE=/OMS_install/ibm_vsi_automation/certificates/wildcardbluemixnet.crt

#cd  /usr/bin
CURL_PATH="/usr/bin/curl"
#CURL_PATH="curl"

access_token_response=$($CURL_PATH --silent -k -X POST --header "Content-Type: application/x-www-form-urlencoded" --header "Accept: application/json" --data-urlencode "grant_type=urn:ibm:params:oauth:grant-type:apikey"   --data-urlencode "apikey=O_PrS8oalO66SZMV63tY4hoAT8JJk3I2E_5ICjz1H0ue"   "https://iam.bluemix.net/identity/token" | python -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

echo $access_token_response

############ SEND IBM PUSH NOTIFICATION TO MENTIONED APPID ##############

#export CURL_CA_BUNDLE=/OMS_install/ibm_vsi_automation/certificates/us-southimfpushcloudibmcom.crt

$CURL_PATH -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Bearer $access_token_response" --header 'Accept-Language: en-US' -d "$INPUT_JSON" 'https://imfpush.us-east.bluemix.net/imfpush/v1/apps/2a5bbc99-cf76-4a47-96b1-2b56e9a1cba9/messages'


EMAIL_SUBJECT="$message_title"
EMAIL_BODY="$message_payload"

############ SEND EMAIL NOTIFICATION ##############

echo "--sending email-started--"
echo  "$EMAIL_BODY"
echo  "$EMAIL_SUBJECT"
#echo  "$EMAIL_BODY" | mailx -r "$EMAIL_FROM" -s "$EMAIL_SUBJECT" "$EMAIL_TO"
echo "--sending email-end--"