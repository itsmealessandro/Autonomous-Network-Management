echo "-------------------"
echo "generating test 3"

echo "preparing dir for compilation"

rm ./target4test/target/*

echo "compiling  ..."

javac -cp ./lib/org.eclipse.osgi_3.21.0.v20240717-2103.jar src/testOsgi3.java -d target4test/target

echo "compiled:"

ls ./target4test/target/*

echo "-----------"

echo "preparing jar ..."

rm ./jar_storage/t3.jar

echo "creating jar..."

jar cfm jar_storage/t3.jar mf4test/3/MANIFEST.mf -C target4test/target/ testOsgi3.class

echo "jar created"
