import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class Activator implements BundleActivator {

  protected static long INTERVAL = 5000;
  protected static String FOLDER = "/home/alessandro/Desktop/Università/magistrale/1_anno/as/Autonomous-Network-Management/osgiTest/1_Osgi/4_autoGenericBundleInstaller/jar2install";
  private static BundleContext context;
  private static BundleUpdaterUtil bundleUpdaterUtil;
  private final Thread thread = new Thread(new BundleUpdater());

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {

    Activator.context = bundleContext;
    bundleUpdaterUtil = new BundleUpdaterUtil(Activator.context);
    thread.start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    Activator.context = null;
    thread.interrupt();
    System.out.println("Bundle is stopping." + this.getClass().getName());
  }

  private class BundleUpdater implements Runnable {

    public void run() {
      try {
        String location = FOLDER;

        while (!Thread.currentThread().isInterrupted()) {
          Thread.sleep(INTERVAL);
          bundleUpdaterUtil.updateBundlesFromLocation(location);
          bundleUpdaterUtil.removeBundlesFromRemovedJars(location);
        }

      } catch (InterruptedException e) {
        System.out.println("I'm going now.");
      } catch (BundleException e) {
        System.out.println("Error updating bundle.");
        e.printStackTrace();
      }

    }
  }
}
