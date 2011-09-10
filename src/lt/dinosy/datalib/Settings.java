package lt.dinosy.datalib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for storing global settings
 * 
 * @author Aurelijus Banelis
 */
public class Settings {
    private String settingsFile;
    private Properties properties = new Properties();
    public String currentPorject;
    public String currentCacheDirecotry;
    
    private static Settings settingsInstance;
    
    public Settings() {
        settingsFile = System.getProperty("user.home") + "/.dinosy/settings.ini";
    }
    
    public void load() throws IOException {
        if ((new File(settingsFile)).exists()) {
            properties.load(new FileInputStream(settingsFile));
            loadVariables();
        }
    }
    
    public void store() throws IOException {
        storeVariables();
        properties.store(new FileOutputStream(settingsFile), null);
    }
    
    private void loadVariables() {
        currentPorject = properties.getProperty("currentPorject");
        currentCacheDirecotry = properties.getProperty("currentCacheDirecotry");
//        for (String string : properties.stringPropertyNames()) {
//            System.out.println(">" + string + "<");
//        }
    }
    
    private void storeVariables() {
        properties.setProperty("currentPorject", currentPorject);
        properties.setProperty("currentCacheDirecotry", currentCacheDirecotry);
    }
    
    public static File getCurrentCacheDirecotry() {
        File direcotry = new File(getInstance().currentCacheDirecotry);
        if (!direcotry.isDirectory()) {
            direcotry = new File(System.getProperty("user.home"));
        }
        return direcotry;
    }
    
    private static Settings getInstance() {
        if (settingsInstance == null) {
            settingsInstance = new Settings();
            try {
                settingsInstance.load();
            } catch (IOException ex) {
                Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, "Can not open settings for global use", ex);
            }
        }
        return settingsInstance;
    }
}
