echo "-------------------"
echo "generating sensor"

echo "generating jar ..."

cd ./sensCreator/my-sens-creator/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2installLocal/*

echo "moving jar"

cp -r "./sensCreator/my-sens-creator/target/my_sens-1.0-SNAPSHOT.jar" "./jar2installLocal/"

echo "jar moved in jar2installLocal:"

cd ./jar2installLocal/

ls -la
