import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class testOsgi3 implements BundleActivator, Runnable {

  private final Thread thread = new Thread(this);

  @Override
  public void start(BundleContext bc) {
    System.out.println("Bundle has started, I'm test 3");
    thread.start();
  }

  @Override
  public void stop(BundleContext context) {
    System.out.println("Bundle is stopping." + this.getClass().getName());
  }

  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(5000);

        System.out.println("I'm running, I'm test 3");
      } catch (InterruptedException e) {
        System.out.println("something exploded");
      }
    }

  }
}
