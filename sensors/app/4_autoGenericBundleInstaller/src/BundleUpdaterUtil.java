import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class BundleUpdaterUtil {
  private final BundleContext context;

  public BundleUpdaterUtil(BundleContext context) {
    this.context = context;
  }

  /**
   * Checks if the jar file corresponding to the given location has been removed
   * (does not exist).
   */
  public boolean isJarRemoved(String location) {
    if (location != null && location.startsWith("file:///" + Activator.FOLDER)) {
      File f = new File(location.replaceFirst("file:///", ""));
      boolean removed = !f.exists();
      if (removed) {
        System.out.println("[DEBUG] Jar file removed from disk: " + location);
      }
      return removed;
    }
    return false;
  }

  /**
   * Checks if the jar file at the given location is already installed as an OSGi
   * bundle.
   */
  public boolean isJarInstalled(String location) {
    for (Bundle bundle : context.getBundles()) {
      if (("file:///" + location).equals(bundle.getLocation())) {
        System.out.println("[DEBUG] Jar file already installed as bundle: " + location);
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a list of absolute jar file paths inside the given directory
   * location.
   */
  public ArrayList<String> getJarsFromLocation(String location) {
    ArrayList<String> fileLocations = new ArrayList<>();
    File dir = new File(location);

    if (!dir.exists()) {
      System.out.println("[ERROR] Directory does not exist: " + location);
      return fileLocations;
    }
    if (!dir.isDirectory()) {
      System.out.println("[ERROR] Path is not a directory: " + location);
      return fileLocations;
    }
    if (!dir.canRead()) {
      System.out.println("[ERROR] Cannot read directory: " + location);
      return fileLocations;
    }

    String[] jarFiles = dir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".jar");
      }
    });

    if (jarFiles == null) {
      System.out.println("[ERROR] Failed to list files in directory: " + location);
      return fileLocations;
    }

    for (String jarFile : jarFiles) {
      String fullPath = location + "/" + jarFile;
      fileLocations.add(fullPath);
      System.out.println("[DEBUG] Found jar file: " + fullPath);
    }
    return fileLocations;
  }

  /**
   * Retrieves the OSGi bundle that corresponds to the given jar location.
   */
  public Bundle getBundleFromJarLocation(String location) {
    for (Bundle bundle : context.getBundles()) {
      if (("file:///" + location).equals(bundle.getLocation())) {
        return bundle;
      }
    }
    return null;
  }

  /**
   * Updates existing bundles or installs new bundles from jar files found in the
   * given directory.
   */
  public void updateBundlesFromLocation(String location) throws BundleException {
    ArrayList<String> jarLocations = getJarsFromLocation(location);

    Iterator<String> jarLocationsIt = jarLocations.iterator();
    while (jarLocationsIt.hasNext()) {
      String jarLocation = jarLocationsIt.next();

      if (isJarInstalled(jarLocation)) {
        Bundle b = getBundleFromJarLocation(jarLocation);
        if (b != null) {
          final int ACTIVE_STATE = Bundle.ACTIVE;
          if (b.getState() != ACTIVE_STATE) {
            System.out.println("[INFO] Starting inactive bundle from JAR: " + jarLocation);
            b.start();
          } else {
            System.out.println("[INFO] Updating active bundle from JAR: " + jarLocation);
            b.update();
            System.out.println("[DEBUG] Bundle state after update: " + b.getState());
          }
        } else {
          System.out.println("[WARN] Bundle object not found for installed jar: " + jarLocation);
        }
      } else {
        try {
          System.out.println("[INFO] New jar detected: " + jarLocation + ". Installing bundle...");
          Bundle newBundle = context.installBundle("file:///" + jarLocation);
          System.out.println("[INFO] Successfully installed bundle from: " + jarLocation);
          // Optional: start bundle right after installation
          newBundle.start();
          System.out.println("[INFO] Started new bundle with ID: " + newBundle.getBundleId());
        } catch (BundleException e) {
          System.out.println("[ERROR] Failed to install bundle from: " + jarLocation);
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Uninstalls bundles whose jar files are no longer present on disk.
   */
  public void removeBundlesFromRemovedJars(String location) {
    Bundle[] bundles = context.getBundles();

    for (Bundle bundle : bundles) {
      if (isJarRemoved(bundle.getLocation())) {
        System.out.println("[INFO] Jar file no longer exists: " + bundle.getLocation() + ". Uninstalling bundle ID: "
            + bundle.getBundleId());
        try {
          bundle.uninstall();
          System.out.println("[INFO] Successfully uninstalled bundle ID: " + bundle.getBundleId());
        } catch (BundleException e) {
          System.out.println("[ERROR] Failed to uninstall bundle ID: " + bundle.getBundleId());
          e.printStackTrace();
        }
      }
    }
  }
}
