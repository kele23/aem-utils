package it.reply.aem;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Mojo(name = "generate-i18n", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateI18n extends AbstractMojo {

    /////////////////////////////////////////////////////////////////FILES - DIRECTORIES

    /**
     * The components directory
     */
    @Parameter(required = true)
    private File components = null;

    /**
     * The i18n directory
     */
    @Parameter(required = true)
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

    private static final Pattern PATTERN_I18N =
            Pattern.compile("\\$\\{[^\"']*((?<![\\\\])['\"])((?:.(?!(?<![\\\\])\\1))*.?)\\1[^@]+@" + ".*i18n.*}");

    public void execute() throws MojoExecutionException {

        if (i18n == null || !i18n.exists() || !i18n.isDirectory()) {
            throw new MojoExecutionException("Can't find the i18n directory.");
        }

        if (components == null || !components.exists() || !components.isDirectory()) {
            throw new MojoExecutionException("Components directory does not exist.");
        }

        //Get Languages
        List<Language> langs = new ArrayList<>();
        boolean defaultLang = true;
        for (String lang : languages) {
            try {
                File langFile = new File(i18n, lang.concat(".xml"));
                langs.add(new Language(lang, langFile, overwriteFile, defaultLang));
                defaultLang = false;
            } catch (ParserConfigurationException ignored) {
            }
        }

        //get all i18n texts
        try {

            List<Path> files = Files.walk(Paths.get(components.getPath()))
                    .filter(Files::isRegularFile)
                    .filter(path -> "html".equals(FilenameUtils.getExtension(path.normalize().toString())))
                    .collect(Collectors.toList());

            int total = 0;

            for (Path filePath : files) {
                File file = filePath.toFile();

                String fileContent = IOUtils.toString(new FileInputStream(file), "UTF-8");
                Matcher matcher = PATTERN_I18N.matcher(fileContent);
                List<String> i18nKeys = new ArrayList<>();
                while (matcher.find()) {
                    String i18nText = matcher.group(2);
                    i18nText = StringEscapeUtils.unescapeJava(i18nText);
                    i18nKeys.add(i18nText);
                }

                int size = i18nKeys.size();
                total += size;
                String fileRelativePath = file.getPath().substring(components.getPath().length());
                if (size > 0)
                    getLog().info("File " + fileRelativePath + " => N# " + size + " keys");
                else
                    getLog().warn("File " + fileRelativePath + " => N# " + size + " keys");

                for (Language lang : langs) {
                    lang.addI18nKeys(i18nKeys, overwriteKey, withMessage);
                }
            }

            getLog().info("Founded  => N# " + total + " keys");

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Commit languages
        for (Language lang : langs) {
            try {
                lang.commit();
            } catch (IOException e) {
                getLog().error(lang.getLanguage() + " => Can't write language file");
            }
        }

    }
}
