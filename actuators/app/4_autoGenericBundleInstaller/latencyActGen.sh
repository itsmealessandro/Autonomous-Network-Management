echo "-------------------"
echo "generating act latency"

echo "generating jar ..."

cd ./actCreator/latency-act/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/latency-act-1.jar

echo "moving jar"

cp -r "./actCreator/latency-act/target/latency-act-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
