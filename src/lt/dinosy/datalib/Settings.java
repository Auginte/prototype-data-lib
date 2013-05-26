package lt.dinosy.datalib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * Class for storing global settings
 *
 * @author Aurelijus Banelis
 */
public class Settings {

    private String settingsFile;
    private Properties properties = new Properties();
    public String currentProject;
    public String currentCacheDirecotry;
    private static String settingsDirectory = System.getProperty("user.home") + "/.dinosy";
    private static Settings settingsInstance;

    private Settings() {
        settingsFile = settingsDirectory + "/settings.ini";
        if (!new File(settingsFile).exists()) {
            saveDefaultSettings(settingsFile);
        }
    }

    private void saveDefaultSettings(String settingsPath) {
        currentCacheDirecotry = new File(settingsPath).getParent() + "/rawData";
        System.out.println("directory = " + currentCacheDirecotry);
        File cacheDirectory = new File(currentCacheDirecotry);
        if (!cacheDirectory.isDirectory()) {
            cacheDirectory.mkdirs();
        }
        currentProject = "Nenurodytas";
        String data = "currentPorject=" + currentProject + "\n"
                + "currentCacheDirecotry=" + currentCacheDirecotry;
        filePutContents(settingsPath, data);
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

    /**
     * webData.xml
     */
    public String getWebDataFile() {
        String file = settingsDirectory + "/webData.xml";
        if (new File(file).exists()) {
            filePutContents(file, "");
        }
        return file;
    }

    /**
     * internetCache
     */
    public String getInternetCache() {
        String file = settingsDirectory + "/internetCache";
        if (new File(file).exists()) {
            filePutContents(file, "");
        }
        return file;
    }

    /**
     * internetCache
     */
    public String getClipboardFile() {
        String file = settingsDirectory + "/clipboard.ini";
        if (new File(file).exists()) {
            filePutContents(file, "");
        }
        return file;
    }

    private void filePutContents(String file, String data) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(data);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, "Error writing file" + file, ex);
        }
    }

    public String getCacheDirecotry() {
        return currentCacheDirecotry;
    }

    public String getPdfExtractor() {
        //FIXME: noraml settings
        String program = "/usr/bin/pdftk";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getPdfAnalyser() {
        //FIXME: noraml settings
        String program = "/usr/bin/identify";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getSketchingProgram() {
        //FIXME: noraml settings
        String program = "/usr/bin/mypaint";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getPaintingProgram() {
        //FIXME: noraml settings
        String program = "/usr/bin/kolourpaint";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getBrowserProgram() {
        //FIXME: noraml settings
        String program = "/usr/bin/firefox";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getTextEditorProgram() {
        //FIXME: noraml settings
        String program = "/usr/bin/geany";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getPdfViewer() {
        //FIXME: noraml settings
        String program = "/usr/bin/okular";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getFileBrowser() {
        //FIXME: noraml settings
        String program = "/usr/bin/nautilus";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getScreenCaptureProgram() {
        //FIXME: noraml settings
        String program = "/usr/bin/import";
        if (new File(program).exists()) {
            return program;
        } else {
            return null;
        }
    }

    public String getOkularSettingsDirectory() {
        //FIXME: noraml settings
        return System.getProperty("user.home") + "/.kde/share/apps/okular/";
    }

    private void loadVariables() {
        currentProject = properties.getProperty("currentProject");
        currentCacheDirecotry = properties.getProperty("currentCacheDirecotry");
    }

    private void storeVariables() {
        properties.setProperty("currentProject", currentProject);
        properties.setProperty("currentCacheDirecotry", currentCacheDirecotry);
    }

    public File getCurrentCacheDirecotry() {
        File direcotry = new File(getInstance().currentCacheDirecotry);
        if (!direcotry.isDirectory()) {
            direcotry = new File(System.getProperty("user.home"));
        }
        return direcotry;
    }

    public File getDateCacheDirecotry() {
        File directory = new File(getCurrentCacheDirecotry(), formadDate(new Date()));
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }
        return directory;
    }

    private static String formadDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static Settings getInstance() {
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

    public String getNameSpace() {
        return "http://aurelijus.banelis.lt/dinosy";
    }

    public InputStream getXsd() {
        return getClass().getResourceAsStream("dinosy.xsd");
    }

    public ZipEntry getZipDataName() {
        return new ZipEntry("dinosy.xml");
    }

    public ZipEntry getZipDataDirectory() {
        return new ZipEntry("data/");
    }

    public ZipEntry getZipSourceDirectory() {
        return new ZipEntry("source/");
    }

    public String getSupportedVersion() {
        return "1.1.3";
    }
}
