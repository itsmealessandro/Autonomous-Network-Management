import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ActivatorLocal implements BundleActivator {

  protected static long INTERVAL = 5000;
  // NOTE: This should change based on .env
  protected static String FOLDER = "../jar2installLocal";
  private static BundleContext context;
  private static BundleUpdaterUtilLocal bundleUpdaterUtilLocal;
  private final Thread thread = new Thread(new BundleUpdater());

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {

    ActivatorLocal.context = bundleContext;
    bundleUpdaterUtilLocal = new BundleUpdaterUtilLocal(ActivatorLocal.context);
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
    ActivatorLocal.context = null;
    thread.interrupt();
    System.out.println("Bundle is stopping." + this.getClass().getName());
  }

  private class BundleUpdater implements Runnable {

    public void run() {
      try {
        String location = FOLDER;

        while (!Thread.currentThread().isInterrupted()) {
          Thread.sleep(INTERVAL);
          bundleUpdaterUtilLocal.updateBundlesFromLocation(location);
          bundleUpdaterUtilLocal.removeBundlesFromRemovedJars(location);
        }

      } catch (InterruptedException e) {
        System.out.println("I'm going now.");
        e.printStackTrace();
      }

    }
  }
}
