package lt.dinosy.datalib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Class for storing global settings
 * 
 * @author Aurelijus Banelis
 */
public class Settings {
    private String settingsFile;
    private Properties properties = new Properties();
    
    public String currentPorject;
    
    public Settings() {
        settingsFile = System.getProperty("user.home") + "/.dinosy/settings.ini";
    }
    
    public void load() throws IOException {
        if ((new File(settingsFile)).exists()) {
            properties.load(new FileInputStream(settingsFile));
            loadVariables();
        }
    }
    
    private void loadVariables() {
        currentPorject = properties.getProperty("currentPorject");
//        for (String string : properties.stringPropertyNames()) {
//            System.out.println(">" + string + "<");
//        }
    }
    
    public void store() throws IOException {
        storeVariables();
        properties.store(new FileOutputStream(settingsFile), null);
    }
    
    private void storeVariables() {
        properties.setProperty("currentPorject", currentPorject);
    }
}
