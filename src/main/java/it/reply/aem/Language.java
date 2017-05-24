package it.reply.aem;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.maven.plugin.logging.Log;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by m.scala on 13-Apr-17.
 * Language
 */
class Language {

    private static final String JCR_ROOT = "jcr:root";
    private static final String SLING_KEY = "sling:key";
    private static final String SLING_MESSAGE = "sling:message";
    //(:4 => Key, :6 => Message)
    private static final String XML_LANGUAGE_NODE_EXPRESSION = "<.*\\s*jcr:mixinTypes=(['\"])\\[sling:Message]\\1\\s*jcr:primaryType=([\"'])nt:folder\\2\\s*sling:key=([\"'])([^\"']*)\\3\\s*sling:message=([\"'])([^\"']*)\\5\\/>";
    private static final Pattern XML_LANGUAGE_NODE_PATTERN = Pattern.compile(XML_LANGUAGE_NODE_EXPRESSION);

    private static final String XML_HEADER = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"\n" +
            "    jcr:language=\"%s\"\n" +
            "    jcr:mixinTypes=\"[mix:language]\"\n" +
            "    jcr:primaryType=\"nt:folder\">\n";
    private static final String XML_LANGUAGE_NODE = "" +
            "    <%s\n" +
            "        jcr:mixinTypes=\"[sling:Message]\"\n" +
            "        jcr:primaryType=\"nt:folder\"\n" +
            "        sling:key=\"%s\"\n" +
            "        sling:message=\"%s\"/>\n";
    private static final String XML_FOOTER = "</jcr:root>";

    private String language;
    private File langFile;
    private boolean defaultLang;
    private HashMap<String,String> map;

    Language(String language, File langFile, boolean overwriteFile, boolean defaultLang) throws ParserConfigurationException {
        this.language = language;
        this.langFile = langFile;
        this.defaultLang = defaultLang;
        map = new HashMap<>();

        //Load current file
        if(!overwriteFile){
            if(langFile.exists()){
                try {
                    String langcontent = FileUtils.readFileToString(langFile,"UTF-8");
                    Matcher matcher = XML_LANGUAGE_NODE_PATTERN.matcher(langcontent);
                    while(matcher.find()){
                        String key = matcher.group(4);
                        String message = matcher.group(6);
                        map.put(key,message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Return the key of this language
     * @return The language key
     */
    String getLanguage() {
        return language;
    }

    /**
     * Add i18n Keys to this language
     * @param i18nKeys The i18n keys
     * @param overwriteKey Overwrite the key if is already contained
     */
    void addI18nKeys(List<String> i18nKeys, boolean overwriteKey, boolean withMessage) {
        for(String key: i18nKeys){

            String esc = escape(key);

            if(overwriteKey){
                if(map.containsKey(esc)){
                    map.remove(esc);
                }
                if(withMessage || defaultLang) {
                    map.put(esc, esc);
                }else{
                    map.put(esc,"");
                }
            }else{
                if(!map.containsKey(esc)){
                    if(withMessage || defaultLang) {
                        map.put(esc, esc);
                    }else{
                        map.put(esc,"");
                    }
                }
            }
        }
    }

    /**
     * Write out this language to the file
     */
    void commit() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(XML_HEADER,language));
        int counter = 0;
        Set<String> nodeNames = new HashSet<>();
        for(String key : map.keySet()){
            String nodeName = key.replaceAll("[^\\p{ASCII}]", "").replaceAll("[\\s+-.,!@#$%^&*()\\[\\]{};\\\\/|<>:\"']", "");
            if( nodeName.matches("^[0-9].*") ){
                nodeName = "N" + nodeName;
            }
            if(nodeName.length() > 20){
                nodeName = nodeName.substring(0,20) + counter++;
            }
            if( nodeNames.contains(nodeName) ){
                nodeName += counter++;
            }
            nodeNames.add(nodeName);
            String message = map.get(key);
            builder.append(String.format(XML_LANGUAGE_NODE,nodeName,key,message));
        }
        builder.append(XML_FOOTER);
        FileUtils.writeStringToFile(langFile,builder.toString(),"UTF-8");
    }

    private String escape(String s){
        String unEscape = StringEscapeUtils.unescapeJava(s); //Remove \ escapes
        String xmlEscape = StringEscapeUtils.escapeXml(unEscape); //Escape for xml
        return StringEscapeUtils.escapeJava(xmlEscape); //Escape tab,newline ecc...
    }

}
