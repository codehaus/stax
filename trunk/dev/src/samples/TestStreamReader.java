package samples;

import java.io.*;
import java.util.List;
import java.util.zip.*;

import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * Simple helper test class for checking how stream reader handles xml
 * documents.
 *
 * @author Tatu Saloranta
 */
public class TestStreamReader
    implements XMLStreamConstants
{
    protected TestStreamReader() {
    }

    protected XMLInputFactory getFactory()
    {
        // Stax ref. impl. is the default, no need to override?
        //System.setProperty("javax.xml.stream.XMLInputFactory", "com.bea.xml.stream.MXParser");

        XMLInputFactory f = XMLInputFactory.newInstance();
        System.out.println("Factory instance: "+f.getClass());

        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);

        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        //f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        //f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);

        //f.setProperty(XMLInputFactory.REPORTER, new TestReporter());
        //f.setProperty(XMLInputFactory.RESOLVER, new TestResolver1());

        return f;
    }

    protected int test(File file)
        throws Exception
    {
        XMLInputFactory f = getFactory();

        System.out.print("Coalesce: "+f.getProperty(XMLInputFactory.IS_COALESCING));
        System.out.println(", NS-aware: "+f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        System.out.print("Entity-expanding: "+f.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        System.out.println(", validating: "+f.getProperty(XMLInputFactory.IS_VALIDATING));

        int total = 0;
        XMLStreamReader sr;

        // Let's deal with gzipped files too?
        FileInputStream fis = new FileInputStream(file);
        if (file.getName().endsWith(".gz")) {
            System.out.println("[gzipped input file!]");
            sr = f.createXMLStreamReader(new GZIPInputStream(fis));
        } else {
            sr = f.createXMLStreamReader(file.toURL().toExternalForm(), fis);
        }

        int type = sr.getEventType();

        System.out.println("START: version = '"+sr.getVersion()
                           +"', xml-encoding = '"+sr.getCharacterEncodingScheme()
                           +"', input encoding = '"+sr.getEncoding()+"'");


        //while (sr.hasNext()) {
        while (type != END_DOCUMENT) {
            type = sr.next();
            total += type; // so it won't be optimized out...

            boolean hasName = sr.hasName();

            System.out.print("["+type+"]");

            // Uncomment for location info debugging:
	    /*
            Location li = sr.getLocation();
            System.out.println(" LOC: "+li);
	    */

            if (sr.hasText()) {
                String text = sr.getText();
                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length(); // to prevent dead code elimination
                }
                if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                    System.out.println(" Text("+text.length()+") = '"+text+"'.");
                    if (text.length() == 1) {
                        System.out.println(" [first char code: 0x"+Integer.toHexString(text.charAt(0))+"]");
                    }
                } else if (type == SPACE) {
                    System.out.print(" Ws = '"+text+"'.");
                    char c = (text.length() == 0) ? ' ': text.charAt(text.length()-1);
                    if (c != '\r' && c != '\n') {
                        System.out.println();
                    }
                } else if (type == DTD) {
                    System.out.println(" DTD:");
                    List entities = (List) sr.getProperty("javax.xml.stream.entities");
                    List notations = (List) sr.getProperty("javax.xml.stream.notations");
                    int entCount = (entities == null) ? -1 : entities.size();
                    int notCount = (notations == null) ? -1 : notations.size();
                    System.out.print("  ("+entCount+" entities, "+notCount
                                       +" notations), sysid ");
                    System.out.print(", declaration = <<");
                    System.out.print(text);
                    System.out.println(">>");
                } else if (type == ENTITY_REFERENCE) {
                    // entity ref
                    System.out.println(" Entity ref: &"+sr.getLocalName()+" -> '"+sr.getText()+"'.");
                    hasName = false; // to suppress further output
                } else { // PI?
                    ;
                }
            }

            if (type == PROCESSING_INSTRUCTION) {
                System.out.println(" PI target = '"+sr.getPITarget()+"'.");
                System.out.println(" PI data = '"+sr.getPIData()+"'.");
            } else if (type == START_ELEMENT) {
                String prefix = sr.getPrefix();
                System.out.print('<');
                if (prefix != null && prefix.length() > 0) {
                    System.out.print(prefix);
                    System.out.print(':');
                }
                System.out.print(sr.getLocalName());
                System.out.print(" {ns '");
                System.out.print(sr.getNamespaceURI());
                System.out.print("'}> ");
                int count = sr.getAttributeCount();
                int nsCount = sr.getNamespaceCount();
                System.out.println(" ["+nsCount+" ns, "+count+" attrs]");
                // debugging:
                for (int i = 0; i < nsCount; ++i) {
                    System.out.println(" ns#"+i+": '"+sr.getNamespacePrefix(i)
                                     +"' -> '"+sr.getNamespaceURI(i)
                                     +"'");
                }
                for (int i = 0; i < count; ++i) {
                    System.out.print(" attr#"+i+": "+sr.getAttributePrefix(i)
                                     +":"+sr.getAttributeLocalName(i)
                                     +" ("+sr.getAttributeNamespace(i)
                                     +") -> '"+sr.getAttributeValue(i)
                                     +"'");
                    System.out.println(sr.isAttributeSpecified(i) ?
                                       "[specified]" : "[Default]");
                }
            } else if (type == END_ELEMENT) {
                System.out.print("</");
                String prefix = sr.getPrefix();
                if (prefix != null && prefix.length() > 0) {
                    System.out.print(prefix);
                    System.out.print(':');
                }
                System.out.print(sr.getLocalName());
                System.out.print(" {ns '");
                System.out.print(sr.getNamespaceURI());
                System.out.print("'}> ");
                int nsCount = sr.getNamespaceCount();
                System.out.println(" ["+nsCount+" ns unbound]");
            } else if (type == START_DOCUMENT) { // only for multi-doc mode
                System.out.print("XML-DECL: version = '"+sr.getVersion()
                                 +"', xml-decl-encoding = '"+sr.getCharacterEncodingScheme()
                                 +"', app-encoding = '"+sr.getEncoding()
                                 +"', stand-alone set: "+sr.standaloneSet());
            }
        }
        return total;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+TestStreamReader.class+" [file]");
            System.exit(1);
        }

        try {
            int total = new TestStreamReader().test(new File(args[0]));
            System.out.println();
            System.out.println("Total: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }
}
