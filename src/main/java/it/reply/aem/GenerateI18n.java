package it.reply.aem;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Mojo( name = "generate-i18n", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class GenerateI18n extends AbstractMojo {

    /////////////////////////////////////////////////////////////////FILES - DIRECTORIES
    private static final String I18N_DIR = "i18n";
    private static final String COMPONENTS_DIR = "components";

    /**
     * The components directory
     */
    @Parameter( required = true )
    private File components = null;

    /**
     * The i18n directory
     */
    @Parameter( required = true )
    private File i18n = null;

    /**
     * Languages to use, first language is the code default.
     */
    @Parameter
    private String[] languages = {"en"};

    /**
     * Overwrites key if is already defined in the i18n file.
     */
    @Parameter
    private boolean overwriteKey = false;

    /**
     * Overwrites file if is already defined, It deletes the old keys.
     */
    @Parameter
    private boolean overwriteFile = false;

    /**
     * Writes the message for not default language.
     */
    @Parameter
    private boolean withMessage = true;


    public void execute() throws MojoExecutionException
    {

        if ( i18n == null || !i18n.exists() || !i18n.isDirectory() )
        {
            throw new MojoExecutionException( "Can't find the i18n directory." );
        }

        if ( components == null || !components.exists() || !components.isDirectory() )
        {
            throw new MojoExecutionException( "Components directory does not exist." );
        }

        //Get Components
        List<Component> cmps = Component.searchForComponents(components);
        //Get Languages
        List<Language> langs = new ArrayList<>();
        for(String lang : languages){
            try{
                File langFile = new File(i18n,lang.concat(".xml"));
                langs.add(new Language(lang, langFile, overwriteFile));
            }catch (ParserConfigurationException ignored) {}
        }

        //Add i18n Keys to Every Language
        for(Component cmp : cmps){
            List<String> i18nKeys = cmp.getI18nKeys();
            if(i18nKeys == null || i18nKeys.isEmpty()){
                getLog().warn(cmp.getComponentName() + " => No i18n Keys");
            }else {
                getLog().info(cmp.getComponentName() + " => " + i18nKeys.size() + " keys");
            }
            for(Language lang : langs) {
                lang.addI18nKeys(i18nKeys, overwriteKey);
            }
        }

        //Write Out Languages
        boolean firstLang = true;
        for(Language lang : langs){
            try {
                lang.commit(withMessage || firstLang);
            } catch (IOException e) {
                getLog().warn("Can't write "+ lang.getLanguage()+ " file");
            }
            firstLang = false;
        }

    }
}
