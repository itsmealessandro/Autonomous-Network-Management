echo "#############################################################"
echo "#############################################################"
echo "#############################################################"
echo "Local Activator"
echo "#############################################################"
echo "#############################################################"
echo "#############################################################"

echo "preparing dir for compilation"

rm ./target/*

echo "compiling  ..."

javac -cp ./lib/org.eclipse.osgi_3.21.0.v20240717-2103.jar src/ActivatorLocal.java src/BundleUpdaterUtilLocal.java -d target

echo "compiled:"

ls ./target/

echo "-----------"

echo "removing jar ..."

rm ./osgiJars/actLocal.jar

echo "creating jar..."

jar cfm ./osgiJars/actLocal.jar mfDirLocal/MANIFEST.MF -C target .

echo "jar created"
