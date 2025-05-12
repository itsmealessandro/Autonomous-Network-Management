echo "preparing dir for compilation"

rm ./target/*

echo "compiling  ..."

javac -cp ./lib/org.eclipse.osgi_3.21.0.v20240717-2103.jar src/Activator.java src/BundleUpdaterUtil.java -d target

echo "compiled:"

ls ./target/

echo "-----------"

echo "preparing jar ..."

rm ./osgiJars/act.jar

echo "creating jar..."

jar cfm ./osgiJars/act.jar mfDir/MANIFEST.MF -C target .

echo "jar created"
