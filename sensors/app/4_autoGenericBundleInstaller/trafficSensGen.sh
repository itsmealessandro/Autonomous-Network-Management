echo "-------------------"
echo "generating sensor traffic"

echo "generating jar ..."

cd ./sensCreator/traffic-sens/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/traffic-sens-1.jar

echo "moving jar"

cp -r "./sensCreator/traffic-sens/target/traffic-sens-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
