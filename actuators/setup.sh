#!/bin/bash

echo "hello from setup.sh"

cd ./app/4_autoGenericBundleInstaller/osgiJars/

#echo "i'm in: $(pwd)"
#echo "----------------------------"
#
#ls -la
#echo "----------------------------"

chmod +x org.eclipse.equinox.console_1.4.800.v20240513-1104.jar

#ls -la
echo "----------------------------"
echo "lunching the console"

java -jar org.eclipse.osgi_3.21.0.v20240717-2103.jar -console
