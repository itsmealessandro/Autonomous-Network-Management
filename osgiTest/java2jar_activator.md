# requirements
- Activator.java
- BundleUpdaterUtil.java


## compile them in a specific folder (e.g. target/)
```
javac -cp ./lib/org.eclipse.osgi_3.21.0.v20240717-2103.jar src/Activator.java src/BundleUpdaterUtil.java -d target

```
## create the jar
`jar cfm act.jar mfDir/MANIFEST.MF -C target .`

"-C" adds all the .class files in the folder

