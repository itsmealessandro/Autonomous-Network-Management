echo "-------------------"
echo "generating sensor suspicious activity"

echo "generating jar ..."

cd ./sensCreator/suspiciousactivity-sens/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/suspiciousactivity-sens-1.jar

echo "moving jar"

cp -r "./sensCreator/suspiciousactivity-sens/target/suspiciousactivity-sens-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
