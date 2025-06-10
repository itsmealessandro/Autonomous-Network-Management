echo "-------------------"
echo "generating act traffic_flwo"

echo "generating jar ..."

cd ./actCreator/trafficflow-act/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/trafficflow-act-1.jar

echo "moving jar"

cp -r "./actCreator/trafficflow-act/target/trafficflow-act-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
