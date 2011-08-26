package lt.dinosy.datalib;

import java.awt.Dimension;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import static lt.dinosy.datalib.Source.parseDate;

/**
 * importing information from Okular documents
 *
 * @author Aurelijus Banelis
 */
public class Okular {
    private static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private static XPathFactory xPathFactory = XPathFactory.newInstance();
    private File infoFile;
    private Document xmlDocument;
    public static String imageCacheDirectory = System.getProperty("user.home") + "/.dinosy/cache";
    private int innerId = 1;
    private String realDocument = null;
    private static final boolean printErrors = false;

    public Okular(File infoFile) {
        this.infoFile = infoFile;
    }

    public List<Data> exportData() {
        List<Data> result = new LinkedList<Data>();
        //TODO: fixme
        if (xmlDocument == null) {
            try {
                xmlDocument = getDocument(infoFile.getPath());
                realDocument = xmlDocument.getDocumentElement().getAttribute("url");
                NodeList nodes = getNodes("//base", xmlDocument);
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (nodes.item(i) instanceof Element) {
                        Element element = (Element) nodes.item(i);

                        int typeNumber = Integer.parseInt(getParent(element, "annotation").getAttribute("type"));
                        AnnotationType type = AnnotationType.get(typeNumber);
                        if (in(type, AnnotationType.AHighlight, AnnotationType.AStamp, AnnotationType.AGeom)) {
                            //TODO: (okular) local / global (dinosy) date conversation
                            Date date = parseDate(element.getAttribute("modifyDate"));

                            Element pageNode = getParent(element, "page");
                            int page = Integer.parseInt(pageNode.getAttribute("number")) + 1;

                            Element boundary = getChild(element, "boundary");
                            float l = Float.parseFloat(boundary.getAttribute("l"));
                            float r = Float.parseFloat(boundary.getAttribute("r"));
                            float b = Float.parseFloat(boundary.getAttribute("b"));
                            float t = Float.parseFloat(boundary.getAttribute("t"));

                            Source.Okular source = new Source.Okular(date, realDocument, page, l, r, b, t);

                            if (imageCacheDirectory != null) {
                                String imagePath = imageCacheDirectory + "/" + getName() + "-" + (innerId++) + "-p" + page + ".jpg";
                                result.add(new Data.Image(imagePath, source));
                                extractImage(imagePath, source);
                            } else {
                                result.add(new Data.Plain(getName() + " " + page, source));
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Okular.class.getName()).log(Level.SEVERE, "Error parsing Okular config file", ex);
            }
        }
        return result;
    }
    
    private String getName() {
        return infoFile.getName().substring(0, infoFile.getName().length() - 4);
    }
    
    private void extractImage(final String imagePath, final Source.Okular source) {
        final File imageFile = new File(imagePath);
        if (!imageFile.getParentFile().isDirectory()) {
            if (!imageFile.getParentFile().mkdirs()) {
                return;
            }
        }
        Thread extraction = new Thread() {
            @Override
            public void run() {
                int page = source.getPage();
//                String extractImage = "/usr/bin/pdftk \"" + source.getSource() + "\" cat " + page + " output \"" + imagePath + ".pdf\"";
                String pdfFile = imagePath + ".pdf";
                Okular.run(new String[] {"/usr/bin/pdftk", source.getSource(), "cat", String.valueOf(page), "output", pdfFile});
                Dimension size = getPageDimention(pdfFile);
                if (size != null) {
                    float height = size.height * 800 / size.width;
                    size.setSize(800, height);
                    int l = (int) (size.width * source.getPosition().l);
                    int t = (int) (size.height * source.getPosition().t);
                    int w = (int) (size.width * (source.getPosition().r - source.getPosition().l));
                    int h = (int) (size.height * (source.getPosition().b - source.getPosition().t));
                    String cropString = w + "x" + h + "+" + l + "+" + t;
                    Okular.run(new String[] {"/usr/bin/convert", "-density", "400", pdfFile, "-scale", "800x", "-crop", cropString, imagePath});
                } else {
                    Okular.run(new String[] {"/usr/bin/convert", "-density", "400", pdfFile, "-scale", "800x", imagePath});
                }
                (new File(pdfFile)).delete();
            }
        };
        extraction.start();
    }

    public static String run(String[] command)
    {
        String lastLine = "";
        try {
            Process proc = Runtime.getRuntime().exec(command);
            String line = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ( (line = br.readLine()) != null) {
                lastLine = line;
            }
            boolean errorWas = false;
            br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            line = null;
            while ( (line = br.readLine()) != null) {
                if (printErrors) {
                    if (!errorWas) {
                        System.err.println("Executting: " + implode(command, " "));
                        errorWas = true;
                    }
                    System.err.println(line);
                }
            }
            proc.waitFor();
        } catch (Throwable t) {}
        return lastLine;
    }

    public static Dimension getPageDimention(String pdfFile) {
        String lastLine = run(new String[] {"/usr/bin/identify", pdfFile});
        Matcher matcher = Pattern.compile(".+ PDF (\\d+)x(\\d+) .+").matcher(lastLine);
        if (matcher.find()) {
            return new Dimension(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        return null;
    }

    private static String implode(String[] array, String glue) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String string : array) {
            if (first) {
                first = false;
            } else {
                result.append(glue);
            }
            result.append(string);
        }
        return result.toString();
    }

    public String printAll() {
        try {
            return readFileAsString(infoFile);
        } catch(IOException ex) {
            return "ERROR: " + ex;
        }
    }

    public File getInfoFile() {
        return infoFile;
    }

    public String getRealDocument() {
        return realDocument;
    }

    
    /*
     * Static elements
     */

    private static String getDir() {
        return "/home/aurelijus/.kde/share/apps/okular/";
    }

    private static String readFileAsString(File file) throws java.io.IOException {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(file));
            f.read(buffer);
        } finally {
            if (f != null) try { f.close(); } catch (IOException ignored) { }
        }
        return new String(buffer);
}

    public static File[] getDocumentsData() {
        File settingsDirectory = new File(getDir() + "docdata");
        if (settingsDirectory.isDirectory()) {
            return settingsDirectory.listFiles();
        } else {
            return new File[0];
        }
    }

    public static List<Okular> parseFiles() {
        List<Okular> result = new LinkedList<Okular>();
        for (File file : getDocumentsData()) {
            Okular object = new Okular(file);
            result.add(object);
        }
        return result;
    }

    public static List<String> getBookmarks() {
        List<String> result = new LinkedList<String>();
        try {
            Document xmlDocument = getDocument(getDir() + "bookmarks.xml");
            NodeList resultList = getNodes("//bookmark", xmlDocument);
            for (int i= 0; i < resultList.getLength(); i++) {
                Element element = (Element) resultList.item(i);
                result.add(element.getAttribute("href"));
            }
        } catch (XPathExpressionException ex) {
            Logger.getLogger(Okular.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Okular.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Okular.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Okular.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    
    /*
     * XML utilities
     */
    
    private static Document getDocument(String file) throws ParserConfigurationException, SAXException, IOException {
        documentBuilderFactory.setValidating(false);
        InputSource inputStream = new org.xml.sax.InputSource();
        inputStream.setCharacterStream(new FileReader(file));
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(inputStream);
    }
    
    private static NodeList getNodes(String xpaht, Document document) throws XPathExpressionException {
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expression = xpath.compile(xpaht);
        return (NodeList) expression.evaluate(document, XPathConstants.NODESET);
    }
    
    private static Element getParent(Element child, String parentName) throws XPathExpressionException {
        return (Element) xPathFactory.newXPath().evaluate("ancestor::" + parentName, child, XPathConstants.NODE);
    }
    
    private static Element getChild(Element parent, String childName) throws XPathExpressionException {
        return (Element) xPathFactory.newXPath().evaluate("descendant::" + childName, parent, XPathConstants.NODE);
    }

    /*
     * Enumeration utilities
     */

    enum AnnotationType {
            AText(1),      ///< A textual annotation
            ALine(2),      ///< A line annotation
            AGeom(3),      ///< A geometrical annotation
            AHighlight(4), ///< A highlight annotation
            AStamp(5),     ///< A stamp annotation
            AInk(6),       ///< An ink annotation
            ACaret(8),     ///< A caret annotation
            AFileAttachment(9), ///< A file attachment annotation
            ASound(10),    ///< A sound annotation
            AMovie(11),    ///< A movie annotation
            A_BASE(0);      ///< The annotation base class

        private int type;

        private AnnotationType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public static AnnotationType get(int type) {
            for (AnnotationType annotationType : values()) {
                if (annotationType.getType() == type) {
                    return annotationType;
                }
            }
            return AnnotationType.A_BASE;
        }
    }

    private static boolean in(AnnotationType type, AnnotationType ... searchIn) {
        for (AnnotationType annotationType : searchIn) {
            if (type == annotationType) {
                return true;
            }
        }
        return false;
    }
}
