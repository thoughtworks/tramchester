#!/bin/bash

logger Begin setup of tramchester server

export ENV=`ec2metadata --user-data| grep ENV | cut -d = -f 2-`
export BUILD=`ec2metadata --user-data| grep BUILD | cut -d = -f 2-`
export ARTIFACTSURL=`ec2metadata --user-data| grep ARTIFACTSURL | cut -d = -f 2-`

if [ "$BUILD" == '' ]; then
        echo 'BUILD missing'
        exit;
fi
if [ "$ENV" == '' ]; then
        echo 'ENV missing'
        exit;
fi
if [ "$ARTIFACTSURL" == '' ]; then
        echo 'ARTIFACTSURL missing'
        exit;
fi

logger Set up Web server Build: $BUILD Url: $ARTIFACTSURL Env: $ENV

# fetch and install the package
distUrl=$ARTIFACTSURL/$BUILD/tramchester-1.0.zip
dist=`basename $distUrl`

target=tramchester-1.0

cd ~ubuntu
mkdir -p server
cd server

logger Attempt to fetch files from $distUrl
wget $distUrl -O $dist
unzip $dist

# cloudwatch logs agent setup
logger Set up amazon cloudwatch logs agent
wget https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py
chmod +x ./awslogs-agent-setup.py
sed -i.orig "s/PREFIX/web_${ENV}_${BUILD}/" $target/cloudwatch_logs_web.conf
./awslogs-agent-setup.py -n -r eu-west-1 -c $target/cloudwatch_logs_web.conf

# fix ownership
chown -R ubuntu .

# start 
logger Start tramchester server
sudo -E -u ubuntu bash ./$target/bin/tramchester server &

# Get Cloud Formation call back URL from user data
callbackUrl=`ec2metadata --user-data | grep WAITURL | cut -d = -f 2-`

logger Web Server Cloud Formation callback URL: $callbackUrl

uuid=`uuid -F SIV`

logger callback UniqueId $uuid

# signal Cloud Formation that data import has finished
curl -X PUT -H 'Content-Type:' --data-binary '{"Status": "SUCCESS", "Reason": "Web Server started", "UniqueId": "$uuid", "Data": "$BUILD"}' $callbackUrl || logger Web Server callback failed

logger Started Web server for $BUILD and $ENV
logger Finish Web bootstrap script
