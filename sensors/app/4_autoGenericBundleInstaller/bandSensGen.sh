echo "-------------------"
echo "generating sensor bandwith"

echo "generating jar ..."

cd ./sensCreator/bandwidth-sens/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/bandwith-sens-1.jar

echo "moving jar"

cp -r "./sensCreator/bandwidth-sens/target/bandwith-sens-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
