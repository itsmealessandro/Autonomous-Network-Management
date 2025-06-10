echo "-------------------"
echo "generating act suspicious_activity"

echo "generating jar ..."

cd ./actCreator/suspiciousactivity-act/
mvn clean verify
cd ../../

echo "removing old jar ..."

rm ./jar2install/suspiciousactivity-act-1.jar

echo "moving jar"

cp -r "./actCreator/suspiciousactivity-act/target/suspiciousactivity-act-1.jar" "./jar2install/"

echo "jar moved in jar2install:"

cd ./jar2install/

ls -la
