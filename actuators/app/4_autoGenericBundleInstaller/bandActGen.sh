echo "-------------------"
echo "generating act bandwith"

echo "generating jar ..."

cd ./actCreator/bandwidth-act/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/bandwith-act-1.jar

echo "moving jar"

cp -r "./actCreator/bandwidth-act/target/bandwith-act-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
