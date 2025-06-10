echo "-------------------"
echo "generating act packetloss"

echo "generating jar ..."

cd ./actCreator/packetloss-act/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/packetloss-act-1.jar

echo "moving jar"

cp -r "./actCreator/packetloss-act/target/packetloss-act-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
