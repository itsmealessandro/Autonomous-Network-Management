import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class testOsgi implements BundleActivator {

  @Override
  public void start(BundleContext bc) {
    System.out.println("Bundle has started, I'm alive 2");
  }

  @Override
  public void stop(BundleContext context) {
    System.out.println("Bundle is stopping 2.");
  }
}
