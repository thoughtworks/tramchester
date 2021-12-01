#!/bin/bash

# AWS LINUX VERSION #################
# shellcheck disable=SC2155

logger Nessus: Begin setup of nessus agent

export META_DATA=http://169.254.169.254/latest/meta-data


if [ "$NESSUS_LINKING_KEY" == '' ]; then
        logger 'NESSUS_LINKING_KEY missing'
        exit;
fi

# from meta-data
export AWS_REGION=`curl $META_DATA/placement/region`
export INSTANCE_ID=`curl $META_DATA/instance-id`
export ACCOUNT_ID=`curl $META_DATA/identity-credentials/ec2/info | jq .AccountId | sed 's/"//g'`

# from tags
export INSTALLER=`aws ec2 describe-tags --region=$AWS_REGION --filters "Name=resource-id, Values=$INSTANCE_ID" "Name=key, Values=NESSUS_INSTALLER" | jq .Tags[0].Value | sed 's/"//g'`

logger Nessus: installing from $INSTALLER
cd ~ec2-user || (logger Could not cd to ec2-user && exit)
wget -nv $INSTALLER -O agent.rpm
rpm --install agent.rpm

#  - First, start Nessus Agent by typing /sbin/service nessusagent start
# - To link this agent, use the '/opt/nessus_agent/sbin/nessuscli agent' command.
#   Type '/opt/nessus_agent/sbin/nessuscli agent help' for more info.

export GROUP="AWS-"$ACCOUNT_ID

logger Nessus: link for $INSTANCE_ID and $ACCOUNT_ID with $NESSUS_LINKING_KEY
/opt/nessus_agent/sbin/nessuscli agent link --key=$NESSUS_LINKING_KEY --name=$INSTANCE_ID --groups=$GROUP --cloud

logger Nessus: start
/sbin/service nessusagent start

logger Nessus: started