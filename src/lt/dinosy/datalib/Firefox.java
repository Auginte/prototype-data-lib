package lt.dinosy.datalib;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aurelijus Banelis
 */
public class Firefox {
    private Type type;
    private String url;
    private String title;
    private String xpath;
    private String data;
    //FIXME: use over configurations
    public static String webDataFile = System.getProperty("user.home") + "/.dinosy/webData.xml";
    public static String webDataCache = System.getProperty("user.home") + "/.dinosy/internetCache";
    private static final int READ_SIZE = 1024;

    public Firefox(Type type, String url, String title, String xpath, String data) {
        this.type = type;
        this.url = url;
        this.title = title;
        this.xpath = xpath;
        this.data = data;
    }

    public void appendData() {
        Controller controller = new Controller();
        List<Data> dataList = new ArrayList<Data>(1);
        Source source = new Source.Internet(new Date(), url, xpath, title, null);
        try {
            if (type == Type.image) {
                dataList.add(new Data.Image(data, generateURL(data), source));
                cacheImage();
                cachePage();
            } else {
                dataList.add(new Data.Plain(data, source));
                cachePage();
            }
            controller.append(dataList, webDataFile);
        } catch (Exception ex) {
            Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error appending web data file", ex);
        }
    }

    private void cacheImage() {
        Thread save = new Thread() {
            @Override
            public void run() {
                try {
                    String destination = webDataCache + "/" + generateURL(data);
                    download(data, destination);
                } catch (Exception ex) {
                    Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error caching image: " + data,  ex);
                }
            }
        };
        save.start();
    }

    private void cachePage() {
        Thread save = new Thread() {
            @Override
            public void run() {
                try {
                    String destination = webDataCache + "/" + generateURL(url) + "-" + System.currentTimeMillis() + ".html";
                    download(url, destination);
                } catch (Exception ex) {
                    Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error caching web page: " + url,  ex);
                }
            }
        };
        save.start();
    }

    public static void download(String address, String to) {
        FileOutputStream fos = null;
        try {
            URL addressUrl = new URL(address);
            ReadableByteChannel rbc = Channels.newChannel(addressUrl.openStream());
            fos = new FileOutputStream(to);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
        } catch (IOException ex) {
            Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error downloading: " + address + " to " + to, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error downloading: " + address + " to " + to, ex);
            }
        }
    }

    private String generateURL(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, "UTF-8").replace("%", "_");
    }

    public static enum Type {
        image,
        html
    }
}
