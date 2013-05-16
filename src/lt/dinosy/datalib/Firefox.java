package lt.dinosy.datalib;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.iharder.Base64;

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
    private String saved;
    public static String webDataFile = Settings.getInstance().getWebDataFile();
    public static String webDataCache = Settings.getInstance().getInternetCache();
    private static final int READ_SIZE = 1024;

    public Firefox(Type type, String url, String title, String xpath, String data, String saved) {
        this.type = type;
        this.url = url;
        this.title = title;
        this.xpath = xpath;
        this.data = data;
        this.saved = saved;
    }

    //TODO: overthink Serializable (not only for copy and paste)
    class SpecificSelection implements Transferable, Serializable {

        private String mime;
        private String data;
        private DataFlavor[] flavors;

        public SpecificSelection(String mime, String data) {
            this.mime = mime;
            this.data = data;
            flavors = new DataFlavor[]{new DataFlavor(String.class, mime)};
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor arg0) {
            for (DataFlavor dataFlavor : flavors) {
                if (dataFlavor.getMimeType().equalsIgnoreCase(arg0.getMimeType())) {
                    return true;
                }
            }
            return false;
        }

        public String getTransferData(DataFlavor arg0) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(arg0)) {
                return data;
            } else {
                throw new UnsupportedFlavorException(arg0);
            }
        }
    }

    public void toClipboard() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("url", url);
        map.put("title", title);
        map.put("xpath", xpath);
        map.put("data", Base64.encodeBytes(data.getBytes()));
        map.put("saved", saved);
        map.put("type", type.name());
        map.put("date", Source.parseDate(new Date()));
        PsiaudoClipboard.addToClipboard(map, "DiNoSy Firefox");
    }

    public void appendData() {
        Controller controller = new Controller();
        List<Data> dataList = new ArrayList<Data>(1);
        Source source = new Source.Internet(new Date(), url, xpath, title, saved, null);
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
                    Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error caching image: " + data, ex);
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
                    Logger.getLogger(Firefox.class.getName()).log(Level.SEVERE, "Error caching web page: " + url, ex);
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
