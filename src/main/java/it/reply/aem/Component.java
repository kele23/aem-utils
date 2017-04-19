package it.reply.aem;


import org.apache.commons.io.FileUtils;

import javax.annotation.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by m.scala on 13-Apr-17.
 * Component
 */
class Component {

    private static final String CONTENT_XML = ".content.xml";
    private static final String CQ_DIALOG_XML = "_cq_dialog/.content.xml";

    private static final String SLY_CQ_COMPONENT_EXPRESSION = "jcr:primaryType=([\"'])cq:Component\\1";
    private static final String SLY_I18N_EXPRESSION = "\\$\\{[^\"'\\}]*([\"'])(.+)\\1[^\\}]*@[^\\}]*i18n[^\\}]*\\}"; // ${"text" @ i18n} (2:text)
    private static final String SLY_INCLUDE_EXPRESSION = "data-sly-include=([\"'])(.+)\\1"; //data-sly-include="test" => (2:test)
    private static final String DIALOG_LABEL_EXPRESSION = "fieldLabel=([\"'])(.+)\\1"; //fieldLabel="ciao" => (2:ciao)
    private static final String DIALOG_DESCRIPTION_EXPRESSION = "fieldDescription=([\"'])(.+)\\1"; //fieldDescription="ciao" => (2:ciao)
    private static final String DIALOG_JCR_TITLE_EXPRESSION = "jcr:title=([\"'])(.+)\\1"; //jcr:title="titolo" => (2:titolo)
    private static final String DIALOG_INCLUDE_EXPRESSION = "sling:resourceType=([\"'])granite\\/ui\\/components\\/foundation\\/include\\1\\s*path=([\"'])(.+)\\2"; // => (3:test)

    private static final Pattern SLY_I18N_PATTERN = Pattern.compile(SLY_I18N_EXPRESSION);
    private static final Pattern SLY_INCLUDE_PATTERN = Pattern.compile(SLY_INCLUDE_EXPRESSION);
    private static final Pattern SLY_CQ_COMPONENT_PATTERN = Pattern.compile(SLY_CQ_COMPONENT_EXPRESSION);
    private static final Pattern DIALOG_LABEL_PATTERN = Pattern.compile(DIALOG_LABEL_EXPRESSION);
    private static final Pattern DIALOG_DESCRIPTION_PATTERN = Pattern.compile(DIALOG_DESCRIPTION_EXPRESSION);
    private static final Pattern DIALOG_JCR_TITLE_PATTERN = Pattern.compile(DIALOG_JCR_TITLE_EXPRESSION);
    private static final Pattern DIALOG_INCLUDE_PATTERN = Pattern.compile(DIALOG_INCLUDE_EXPRESSION);

    /******** CLASSIC DIALOG INCLUDE ******* FIXME
     * <marginConfiguration
     *      jcr:primaryType="cq:Widget"
     *      path="/apps/reoco/components/structure/page/fragments-dlg/tab_panel-margin.infinity.json"
     *      xtype="cqinclude"/>
     ********************************/

    /******** TOUCH UI DIALOG INCLUDE ******* OK!!!
     * <advanced
     *      jcr:primaryType="nt:unstructured"
     *      sling:resourceType="granite/ui/components/foundation/include"
     *      path="/apps/credem/components/structure/pages/page/tabs/tab_advanced"/>
     ********************************/

    private File directory;
    private String componentName;
    private List<String> i18nKeys;

    /**
     * Create a component
     * @param directory The directory of the component
     */
    private Component(File directory){
        this.directory = directory;
        componentName = directory.getName();
    }

    /**
     * Return the component name
     * @return The component name
     */
    String getComponentName(){
        return componentName;
    }

    /**
     * Return the i18nKeys founded in the component
     * @return i18keys
     */
    List<String> getI18nKeys(boolean withDialog){
        List<String> i18nKeys = new ArrayList<>();

        List<File> htmlFiles = getCompFiles();
        if(htmlFiles != null) {
            for (File f : htmlFiles) {
                List<String> keys = getFileI18nKeys(f,withDialog);
                if (keys != null) {
                    i18nKeys.addAll(keys);
                }
            }
        }

        return i18nKeys;
    }

    /**
     * Get I18N Keys by File
     * @param file The file where to search
     * @return A List of I18N Keys
     */
    @Nullable
    private List<String> getFileI18nKeys(File file, boolean withDialog){
        String content;
        try {
            content = FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e){
            return null;
        }
        List<String> keys = new ArrayList<>();
        Matcher matcher;


        //SIGHTLY I18N
        matcher = SLY_I18N_PATTERN.matcher(content);
        while(matcher.find()){
            String value = matcher.group(2);
            keys.add(value);
        }
        if(withDialog) {
            //JCR_TITLE
            matcher = DIALOG_JCR_TITLE_PATTERN.matcher(content);
            while (matcher.find()) {
                String value = matcher.group(2);
                keys.add(value);
            }
            //DIALOG fieldLabel
            matcher = DIALOG_LABEL_PATTERN.matcher(content);
            while (matcher.find()) {
                String value = matcher.group(2);
                keys.add(value);
            }
            //DIALOG fieldDescription
            matcher = DIALOG_DESCRIPTION_PATTERN.matcher(content);
            while (matcher.find()) {
                String value = matcher.group(2);
                keys.add(value);
            }
        }

        return keys;
    }

    /**
     * Get the component list files
     * @return A List of files
     */
    @Nullable
    private List<File> getCompFiles(){

        File[] files = directory.listFiles();
        if(files == null)
            return null;

        Set<File> filesSet = new HashSet<>();
        filesSet.addAll(Arrays.asList(files));

        //add _cq_dialog file
        File cq_dialog = new File(directory,CQ_DIALOG_XML);
        if(cq_dialog.exists()){
            filesSet.add(cq_dialog);
        }

        for(File f : files){
            try {
                getCompFilesByInclude(f, filesSet);
            }catch(Exception ignored){}
        }

        List<File> fileList = new ArrayList<>();
        fileList.addAll(filesSet);
        return fileList;
    }

    /**
     * Search recursively for included files (data-sly-include="file")
     * @param f The current file
     * @param files The files set
     * @throws IOException File exists but is unreadable
     */
    private void getCompFilesByInclude(File f, Set<File> files) throws IOException {
        if(f.exists()){
            String content = FileUtils.readFileToString(f,"UTF-8");

            Matcher matcher;
            //SIGHTLY INCLUDE
            matcher = SLY_INCLUDE_PATTERN.matcher(content);
            while(matcher.find()){
                String value = matcher.group(2); //group 2
                File inclusion = new File(directory, value);
                if(inclusion.exists() && !files.contains(inclusion)){
                    files.add(inclusion);
                    getCompFilesByInclude(inclusion, files);
                }
            }

            //DIALOG INCLUDE
            matcher = DIALOG_INCLUDE_PATTERN.matcher(content);
            while(matcher.find()){
                String value = matcher.group(3); //group 3
                File inclusion = new File(directory, value);
                if(inclusion.exists() && !files.contains(inclusion)){
                    files.add(inclusion);
                    getCompFilesByInclude(inclusion, files);
                }
            }

        }
    }

////////////////////////////////////////////////////////////////////////////STATIC METHODS
    /**
     * Search for components in the project directory
     * @param directory The directory where searching for components
     * @return A list of components
     */
    static List<Component> searchForComponents(File directory) {
        List<Component> retList = new ArrayList<Component>();
        File[] currentFiles = directory.listFiles();
        if(currentFiles == null)
            return retList;
        for (File f : currentFiles) {
            //Search for sub-components
            if(f.isDirectory()){
                retList.addAll(searchForComponents(f));
                if(Component.isComponent(f)){
                    retList.add(new Component(f));
                }
            }
        }
        return retList;
    }

    /**
     * Controls if a directory is a component
     * @param directory The directory
     * @return True if the directory is a component
     */
    private static boolean isComponent(File directory){
        if(directory.exists() && directory.isDirectory()){
            File content = new File(directory,CONTENT_XML);
            if(content.exists()){
                try {
                    String contentString = FileUtils.readFileToString(content,"UTF-8");
                    Matcher matcher = SLY_CQ_COMPONENT_PATTERN.matcher(contentString);
                    if(matcher.find()){
                        return true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
