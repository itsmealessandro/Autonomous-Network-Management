import java.io.File;
import java.io.FileWriter; // Puoi rimuoverlo se non più usato per il debug.txt
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger; // Importa il Logger

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class BundleUpdaterUtilLocal {
  // Definisci un logger per la classe
  private static final Logger LOGGER = Logger.getLogger(BundleUpdaterUtilLocal.class.getName());

  private BundleContext context;
  // Assumi che ActivatorLocal.FOLDER sia accessibile, altrimenti passalo nel
  // costruttore
  // private String activatorFolder;

  public BundleUpdaterUtilLocal(BundleContext context) {
    this.context = context;
    // this.activatorFolder = ActivatorLocal.FOLDER; // Se ActivatorLocal.FOLDER non
    // è statico/pubblico
  }

  /**
   * Controlla se un JAR associato a una location di bundle è stato rimosso dal
   * filesystem.
   * 
   * @param bundleLocation La location del bundle come registrata nell'OSGi (es.
   *                       "file:///path/to/jar").
   * @return true se il file JAR non esiste più, false altrimenti.
   */
  public boolean isJarRemoved(String bundleLocation) {
    // Verifica se la location inizia con il prefisso file:/// e il tuo folder
    // specifico
    // È importante che ActivatorLocal.FOLDER sia il percorso corretto e che il
    // confronto sia robusto.
    if (bundleLocation.startsWith("file:///")) {
      // Rimuovi il prefisso "file:///" per ottenere il percorso filesystem
      String filePath = bundleLocation.substring("file:///".length());
      File f = new File(filePath);

      // Log per debug: mostra il percorso che stai controllando
      LOGGER.fine("Checking if JAR file exists: " + filePath + " -> " + f.exists());

      if (!f.exists()) {
        LOGGER.info("JAR file " + filePath + " is no longer found on disk.");
        return true;
      }
    } else {
      LOGGER.warning(
          "Bundle location " + bundleLocation + " does not start with 'file:///', skipping 'isJarRemoved' check.");
    }
    return false;
  }

  /**
   * Controlla se un JAR specifico (dato il suo percorso sul filesystem) è già
   * installato come bundle.
   * 
   * @param jarLocation Il percorso del JAR sul filesystem (es. "path/to/my.jar").
   * @return true se il bundle corrispondente è già installato, false altrimenti.
   */
  public boolean isJarInstalled(String jarLocation) {
    Bundle[] bundles = context.getBundles();
    // Normalizza la location per il confronto, dato che OSGi potrebbe usare
    // "file:///"
    String normalizedJarLocation = "file:///" + jarLocation.replace("\\", "/"); // Assicura slash corretti per URI

    for (Bundle bundle : bundles) {
      // Log per debug: mostra i confronti delle location
      LOGGER.fine("Comparing installed bundle location '" + bundle.getLocation() + "' with target JAR '"
          + normalizedJarLocation + "'");
      if (bundle.getLocation().equals(normalizedJarLocation)) {
        LOGGER.info("JAR " + jarLocation + " is already installed as bundle " + bundle.getSymbolicName() + " (ID: "
            + bundle.getBundleId() + ").");
        return true;
      }
    }
    LOGGER.fine("JAR " + jarLocation + " is not currently installed.");
    return false;
  }

  /**
   * Restituisce una lista di percorsi assoluti dei file JAR presenti nella
   * directory specificata.
   * 
   * @param location Il percorso della directory (es. "../jar2installLocal").
   * @return Un ArrayList di Stringhe, contenente i percorsi completi dei file
   *         JAR.
   */
  public ArrayList<String> getJarsFromLocation(String location) {
    ArrayList<String> fileLocations = new ArrayList<>();
    File dir = new File(location);

    // --- DEBUG AVANZATO PER LA DIRECTORY ---
    // Rimuovi System.out.println e il FileWriter per usare il logger
    LOGGER.info("Checking directory: " + location);
    LOGGER.fine(String.format("Directory check details: exists=%b, isDirectory=%b, canRead=%b",
        dir.exists(), dir.isDirectory(), dir.canRead()));

    if (!dir.exists()) {
      LOGGER.severe("Directory '" + location + "' does not exist. Cannot retrieve JARs.");
      return fileLocations; // Ritorna lista vuota se la directory non esiste
    }
    if (!dir.isDirectory()) {
      LOGGER.severe("Path '" + location + "' is not a directory. Cannot retrieve JARs.");
      return fileLocations; // Ritorna lista vuota se non è una directory
    }
    if (!dir.canRead()) {
      LOGGER.severe("Cannot read from directory '" + location + "'. Check permissions.");
      return fileLocations; // Ritorna lista vuota se non hai permessi di lettura
    }

    String[] jarFileNames = dir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        boolean isJar = name.toLowerCase().endsWith(".jar");
        // LOGGER.finest("Checking file: " + name + " - is JAR: " + isJar); // Troppo
        // verboso per FINE, usa FINEST
        return isJar;
      }
    });

    if (jarFileNames == null || jarFileNames.length == 0) {
      LOGGER.info("No JAR files found in directory: " + location);
      return fileLocations;
    }

    for (String jarName : jarFileNames) {
      // Usa il percorso assoluto per evitare problemi con i percorsi relativi
      String fullPath = new File(dir, jarName).getAbsolutePath();
      fileLocations.add(fullPath);
      LOGGER.fine("Found JAR file: " + fullPath);
    }

    return fileLocations;
  }

  /**
   * Ottiene un bundle installato data la sua posizione sul filesystem.
   * 
   * @param jarLocation Il percorso del JAR sul filesystem (es. "path/to/my.jar").
   * @return L'oggetto Bundle se trovato, null altrimenti.
   */
  public Bundle getBundleFromJarLocation(String jarLocation) {
    Bundle[] bundles = context.getBundles();
    String normalizedJarLocation = "file:///" + jarLocation.replace("\\", "/"); // Normalizza per URI

    for (Bundle bundle : bundles) {
      if (bundle.getLocation().equals(normalizedJarLocation)) {
        return bundle;
      }
    }
    return null;
  }

  /**
   * Aggiorna i bundle installati o installa nuovi bundle da una directory
   * specificata.
   * 
   * @param location Il percorso della directory contenente i file JAR.
   * @throws BundleException Se si verifica un errore critico durante
   *                         l'installazione/aggiornamento di un bundle.
   */
  public void updateBundlesFromLocation(String location) { // Rimosso 'throws BundleException' perché le eccezioni sono
                                                           // gestite internamente
    LOGGER.info("##################################################################");
    LOGGER.info("##################################################################");
    LOGGER.info("##################################################################");
    LOGGER.info("##################################################################");
    LOGGER.info("Starting update/installation process for bundles in: " + location);

    ArrayList<String> jarLocations = this.getJarsFromLocation(location);

    if (jarLocations.isEmpty()) {
      LOGGER.info("No JAR files found to process in " + location + ". Skipping update.");
      return;
    }

    for (String jarLocation : jarLocations) {
      LOGGER.info("Processing JAR: " + jarLocation);

      // Ottimizzazione: è più efficiente cercare il bundle per symbolic name/version
      // ma per ora manteniamo il controllo per location per coerenza con le tue
      // funzioni esistenti
      if (this.isJarInstalled(jarLocation)) {
        Bundle b = this.getBundleFromJarLocation(jarLocation);
        if (b != null) {
          LOGGER.info("Bundle '" + b.getSymbolicName() + "' (ID: " + b.getBundleId() + ") from '" + jarLocation
              + "' is already installed.");

          // Usiamo le costanti Bundle.X per una maggiore chiarezza e robustezza
          if (b.getState() != Bundle.ACTIVE) {
            LOGGER.info("Bundle '" + b.getSymbolicName() + "' is not active. Attempting to start it...");
            try {
              b.start();
              LOGGER.info("Bundle '" + b.getSymbolicName() + "' started successfully. Current state: " + b.getState());
            } catch (BundleException e) {
              LOGGER.log(Level.SEVERE, "Failed to start bundle '" + b.getSymbolicName() + "' (ID: " + b.getBundleId()
                  + ") from '" + jarLocation + "'.", e);
            }
          } else {
            LOGGER.info("Bundle '" + b.getSymbolicName() + "' is already active. Attempting to update it...");
            try {
              b.update();
              LOGGER.info("Bundle '" + b.getSymbolicName() + "' updated successfully. Current state: " + b.getState());
            } catch (BundleException e) {
              LOGGER.log(Level.SEVERE, "Failed to update bundle '" + b.getSymbolicName() + "' (ID: " + b.getBundleId()
                  + ") from '" + jarLocation + "'.", e);
            }
          }
        } else {
          // Questa situazione dovrebbe essere prevenuta da isJarInstalled()
          LOGGER.warning("Inconsistency: isJarInstalled returned true for " + jarLocation
              + " but getBundleFromJarLocation returned null. This should not happen.");
        }
      } else {
        LOGGER.info("JAR " + jarLocation + " is not installed. Attempting to install it now...");
        try {
          // Costruisci l'URI corretto per l'installazione
          String installUri = "file:///" + jarLocation.replace("\\", "/");
          Bundle newBundle = context.installBundle(installUri);
          LOGGER.info("Bundle '" + newBundle.getSymbolicName() + "' (ID: " + newBundle.getBundleId()
              + ") installed successfully from " + jarLocation + ".");

          // Una volta installato, prova ad avviarlo. Spesso i bundle devono essere
          // esplicitamente avviati.
          if (newBundle.getState() != Bundle.ACTIVE) {
            LOGGER.info("Bundle '" + newBundle.getSymbolicName()
                + "' is not active after installation. Attempting to start it...");
            try {
              newBundle.start();
              LOGGER.info("Bundle '" + newBundle.getSymbolicName() + "' started successfully.");
            } catch (BundleException e) {
              LOGGER.log(Level.SEVERE,
                  "Failed to start newly installed bundle '" + newBundle.getSymbolicName() + "' (ID: "
                      + newBundle.getBundleId() + ") from '" + jarLocation + "'. Check its dependencies or Activator.",
                  e);
            }
          }

        } catch (BundleException e) {
          // *** ESTREMAMENTE IMPORTANTE: LOGGARE LA STACK TRACE COMPLETA ***
          LOGGER.log(Level.SEVERE, "Failed to install bundle from '" + jarLocation
              + "'. This usually indicates a missing dependency or a malformed bundle. See stack trace for details.",
              e);
        } catch (SecurityException e) {
          LOGGER.log(Level.SEVERE, "Security permissions denied while attempting to install bundle from '" + jarLocation
              + "'. Check Java security policy.", e);
        }
      }
    }
    LOGGER.info("Finished update/installation process for bundles in: " + location);
  }

  /**
   * Rimuove i bundle dall'ambiente OSGi i cui file JAR non esistono più sul
   * filesystem.
   * 
   * @param location La directory da cui i JAR venivano monitorati.
   */
  public void removeBundlesFromRemovedJars(String location) {
    LOGGER.info("Checking for removed JARs and uninstalling corresponding bundles for location: " + location);

    Bundle[] bundles = context.getBundles();
    for (Bundle bundle : bundles) {
      // Controlla solo i bundle che sono stati installati da una location che inizi
      // con il tuo folder
      // Questo evita di disinstallare bundle di sistema o altri bundle non gestiti da
      // questo updater
      if (bundle.getLocation().startsWith("file:///" + ActivatorLocal.FOLDER)
          && this.isJarRemoved(bundle.getLocation())) {
        LOGGER.warning("The JAR file for bundle '" + bundle.getSymbolicName() + "' (ID: " + bundle.getBundleId()
            + ") located at '" + bundle.getLocation() + "' is no longer available. Attempting to uninstall bundle...");
        try {
          bundle.uninstall();
          LOGGER.info(
              "Bundle '" + bundle.getSymbolicName() + "' (ID: " + bundle.getBundleId() + ") uninstalled successfully.");
        } catch (BundleException e) {
          LOGGER.log(Level.SEVERE, "Error uninstalling bundle '" + bundle.getSymbolicName() + "' (ID: "
              + bundle.getBundleId() + ") from '" + bundle.getLocation() + "'.", e);
        }
      }
    }
    LOGGER.info("Completed check for removed JARs and bundle uninstallation.");
  }
}
