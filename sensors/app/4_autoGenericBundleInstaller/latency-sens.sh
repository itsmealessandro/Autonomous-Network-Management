echo "-------------------"
echo "generating sensor latency"

echo "generating jar ..."

cd ./sensCreator/latency-sens/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/latency-sens-1.jar

echo "moving jar"

cp -r "./sensCreator/latency-sens/target/latency-sens-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
