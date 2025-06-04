echo "-------------------"
echo "generating sensor"

echo "generating jar ..."

cd ./sensCreator/my-sens-creator/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/*

echo "moving jar"

cp -r "./sensCreator/my-sens-creator/target/my_sens-1.0-SNAPSHOT.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
