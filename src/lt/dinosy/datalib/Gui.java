package lt.dinosy.datalib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import lt.dinosy.datalib.Controller.BadVersionException;
import lt.dinosy.datalib.Representation.Element;
import org.xml.sax.SAXException;

/**
 * Graphical user interface interation
 *
 * @author Aurelijus Banelis
 */
public class Gui {
    public static void main(String[] args) {
        if (args.length >= 11 && args[0].equalsIgnoreCase("import")) {
            parseFirefoxImport(args);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("help")) {
            if (args[1].equalsIgnoreCase("import")) {
                System.out.println(helpImportFirefox());
            } else {
                System.out.println("Unknown command: " + args[1]);
                System.out.println(helpGlobal());
            }
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("okular")) {
            Controller controller = null;
            controller = open();
    //        show(controller);
            importOkularData();
        } else {
            System.out.println(helpGlobal());
        }
    }

    private static void parseFirefoxImport(String[] args) {
        Firefox.Type type = null;
        String url = null;
        String title = null;
        String xpath = null;
        String data = null;
        try {
            for (int i = 1; i < 11; i+=2) {
                if (args[i].equalsIgnoreCase("-type")) {
                    type = Firefox.Type.valueOf(args[i+1]);
                } else if (args[i].equalsIgnoreCase("-url")) {
                    url = URLDecoder.decode(args[i + 1], "UTF-8");
                } else if (args[i].equalsIgnoreCase("-title")) {
                    title = URLDecoder.decode(args[i + 1], "UTF-8");
                } else if (args[i].equalsIgnoreCase("-xpath")) {
                    xpath = URLDecoder.decode(args[i + 1], "UTF-8");
                } else if (args[i].equalsIgnoreCase("-data")) {
                    data = URLDecoder.decode(args[i + 1], "UTF-8");
                }
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, "UTF-8 encoding should be supported", ex);
        }
        if (type == null || url == null || title == null || xpath == null || data == null) {
            System.out.println(errorMessage(new String[] {"-type", "-url", "-title", "-xpath", "-data"}, type, url, title, xpath, data));
            System.out.println(helpImportFirefox());
        } else {
            Firefox firefox = new Firefox(type, url, title, xpath, data);
            firefox.appendData();
        }
    }

    private static String errorMessage(String[] names, Object ... given) {
        StringBuilder messages = new StringBuilder();
        for (int i= 0; i < names.length; i++) {
            if (given[i] == null) {
                messages.append(names[i]).append(" not given or invalid.").append("\n");
            }
        }
        return messages.toString();
    }

    private static String helpImportFirefox() {
        return "Usage: import -type <type> -url <url> -title <title> -xpath <xpath> -data <data>\n" +
               "\t<type>\timage or html\n" +
               "\t<url>\tx-www-form-urlencoded url of the web page\n" +
               "\t<title>\tx-www-form-urlencoded title of the web page\n" +
               "\t<xpath>\tx-www-form-urlencoded xpath query starting from body to selected part in document\n" +
               "\t<data>\tx-www-form-urlencoded data. If <type> image - url to image, if <type> html - selection html code\n";
    }

    private static String helpGlobal() {
        return "Usage: <command> <-parameter_name parameter_value>\n" +
               "\t<command>\n" +
               "\t\timport - imports data about selection in web page\n" +
               "\t\tokular - updates information about okular annotations\n" +
               "\t\thelp <command> - show aditional information about command\n\n" +
               "For more information go to http://aurelijus.banelis.lt/dinosy\n";
    }

    private static void importOkularData() {
        for (Okular okular : Okular.parseFiles()) {
            System.out.println("======== " + okular.getInfoFile().getName() + " ===============");
            for (Data data : okular.exportData()) {
                System.out.println(">" + data);
            }
        }
        System.out.println("=========================================");
        for (String string : Okular.getBookmarks()) {
            System.out.println(">" + string);
        }
    }

    private static Controller open() {
        Controller controller = new Controller();
        try {
            controller.openFile( "src/lt/dinosy/datalib/testdocument.xml");
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadVersionException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return controller;
    }

    private static void show(Controller controller) {
        Map<Integer, Data> data = controller.getData();
        for (Integer id : data.keySet()) {
            Data item = data.get(id);
            System.out.println(id + ": " + item.getData().toString());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            System.out.println("\t Source: " + item.getSource().getSource().toString() + " | " + dateFormat.format(item.getSource().getDate()));
            for (Data dataItem : item.getChilds()) {
                System.out.println("\t child: " + dataItem.getId() + ": " + dataItem.getData().getClass());
            }
            for (Relation relation : item.getRelations()) {
                System.out.println("\t relation: " + relation.getFrom().getId() + " -> " + relation.getTo().getId());
            }
            for (Representation representation : item.getRepresentations()) {
                if (representation instanceof Representation.Element) {
                    Representation.Element element = (Element) representation;
                    System.out.println("\t representation: " + element.getPosition().getX() + "x" + element.getPosition().getY() + "x" + element.getZ() + " Size" + element.getSize().getWidth() + "x" + element.getSize().getHeight());
                } else {
                    System.out.println("\t representation place holder " + representation.getDataId());
                }
            }
            if (item instanceof Data.Class) {
                Data.Class dataClass = (Data.Class) item;
                for (String string : dataClass.getAttributes()) {
                    System.out.println("\t a:" + string);
                }
                for (String string : dataClass.getMethods()) {
                    System.out.println("\t m:" + string);
                }
            }
        }
    }
}
