echo "-------------------"
echo "generating sensor packet loss"

echo "generating jar ..."

cd ./sensCreator/packetloss-sens/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/packetloss-sens-1.jar

echo "moving jar"

cp -r "./sensCreator/packetloss-sens/target/packetloss-sens-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
