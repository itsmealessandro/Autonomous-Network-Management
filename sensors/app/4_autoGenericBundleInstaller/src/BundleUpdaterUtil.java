import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class BundleUpdaterUtil {
  private BundleContext context;

  public BundleUpdaterUtil(BundleContext context) {
    this.context = context;
  }

  public boolean isJarRemoved(String location) {

    if (location.startsWith("file:///" + Activator.FOLDER)) {
      File f = new File(location.replaceFirst("file:///", ""));
      if (!(f.exists()))
        return true;
    }
    return false;
  }

  public boolean isJarInstalled(String location) {
    Bundle[] bundles = context.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      if (bundles[i].getLocation().equals("file:///" + location)) {
        return true;
      }
    }
    return false;
  }

  /*
   * return a list of jar files in the directory
   */
  public ArrayList<String> getJarsFromLocation(String location) {
    /*
     * BUG: debug
     * try {
     * FileWriter fWriter = new FileWriter("debug.txt");
     * fWriter.write("the loc is: " + location);
     * fWriter.close();
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * System.out.println("location is: " + location);
     * 
     * end debug
     */
    ArrayList<String> fileLocations = new ArrayList<String>();
    File dir = new File(location);

    /*
     * debug
     * System.out.println("Controllo directory: " + location);
     * System.out.println("exists: " + dir.exists());
     * System.out.println("isDirectory: " + dir.isDirectory());
     * System.out.println("canRead: " + dir.canRead());
     */

    String[] jarFiles = dir.list(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        if (name.toLowerCase().endsWith(".jar")) {
          return true;
        } else {
          return false;
        }
      }
    });
    for (int i = 0; i < jarFiles.length; i++) {
      fileLocations.add(location + "/" + jarFiles[i]);
    }

    return fileLocations;
  }

  public Bundle getBundleFromJarLocation(String location) {
    Bundle[] bundles = context.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      if (bundles[i].getLocation().equals("file:///" + location)) {
        return bundles[i];
      }
    }
    return null;
  }

  public void updateBundlesFromLocation(String location) throws BundleException {
    ArrayList<String> jarLocations = this.getJarsFromLocation(location);

    Iterator<String> jarLocationsIt = jarLocations.iterator();
    while (jarLocationsIt.hasNext()) {
      String jarLocation = jarLocationsIt.next();

      if (this.isJarInstalled(jarLocation)) {
        Bundle b = this.getBundleFromJarLocation(jarLocation);
        if (b != null) {

          final int ACTIVE_STATE = 5;
          if (b.getState() != ACTIVE_STATE) {
            b.start();
          } else {

            System.out.println("Updating the bundle from the JAR " + jarLocation);
            b.update();
            // NOTE: If the bundle is not active, start it;
            System.out.println("the state of the bundle is:" + b.getState());
            // WARNING: this number represents ACTIVE state
          }
        }

      } else {
        try {
          System.out.println("Found the new Jar file " + jarLocation + ". Trying to install it....");
          Bundle newBundle = context.installBundle("file:///" + jarLocation);
          System.out.println("Jar file " + jarLocation + " installed.");
        } catch (BundleException e) {
          System.out.println("Error installing the bundle. Maybe it's already installed.");
          // TODO: handle exception
        }
      }
    }
  }

  public void removeBundlesFromRemovedJars(String location) {

    Bundle[] bundles = context.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      if (this.isJarRemoved(bundles[i].getLocation())) {
        System.out.println(
            "The Jar file " + bundles[i].getLocation() + " is no longer available. Uninstalling the bundle...");
        try {
          bundles[i].uninstall();
        } catch (BundleException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

  }

}
