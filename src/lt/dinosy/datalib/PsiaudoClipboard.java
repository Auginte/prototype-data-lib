package lt.dinosy.datalib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aurelijus Banelis
 */
public class PsiaudoClipboard {

    private static String getFile() {
        return Settings.getInstance().getClipboardFile();
    }

    public static void addToClipboard(Map<String, String> data, String comment) {
        try {
            BufferedWriter writter = new BufferedWriter(new FileWriter(getFile(), true));
            writter.append("[" + getTime() + " " + comment + "]");
            writter.newLine();
            for (String key : data.keySet()) {
                writter.append(key + ": " + data.get(key).trim());
                writter.newLine();
            }
            writter.newLine();
            writter.close();
        } catch (IOException ex) {
            Logger.getLogger(PsiaudoClipboard.class.getName()).log(Level.SEVERE, "Cannot add to psiaudo clipboard", ex);
        }
    }

    public static List<Map<String, String>> getFromClipboard() {
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();
        BufferedReader reader = null;
        try {
            //TODO: locking
            reader = new BufferedReader(new FileReader(getFile()));
            String line = null;
            Map<String, String> clip = new HashMap<String, String>();
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0 && clip.size() > 0) {
                    result.add(clip);
                    clip = new HashMap<String, String>();
                } else if (!line.startsWith("[")) {
                    String[] data = line.split(": ", 2);
                    if (data.length == 2) {
                        clip.put(data[0], data[1]);
                    }
                }
            }
            reader.close();
            FileOutputStream eraser = new FileOutputStream(getFile());
            eraser.write(new byte[0]);
            eraser.close();
        } catch (IOException ex) {
            Logger.getLogger(PsiaudoClipboard.class.getName()).log(Level.SEVERE, "Cannot write to psiaudo clipboard", ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(PsiaudoClipboard.class.getName()).log(Level.SEVERE, "Error closing psiaudo clipboard", ex);
            }
        }
        return result;
    }

    protected static String getTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
