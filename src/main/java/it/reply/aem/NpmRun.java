package it.reply.aem;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;

/**
 * Created by m.scala on 15-Jun-17.
 *
 */
@Mojo( name = "npm-run", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class NpmRun extends AbstractMojo {

    /**
     * The Npm Build project directory
     */
    @Parameter( required = true )
    private File npmDir = null;

    /**
     * The Clientlib directory
     */
    @Parameter( required = true )
    private File clientlibDir = null;

    /**
     * The folder path (relative to npmDir) where are compiled elements
     */
    @Parameter
    private String distName = "dist";

    /**
     * Command to launch [default = build]
     */
    @Parameter
    private String command = "build";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if ( npmDir == null || !npmDir.exists() || !npmDir.isDirectory() )
        {
            throw new MojoExecutionException( "Npm project directory does not exist." );
        }

        if ( clientlibDir == null || !clientlibDir.exists() || !clientlibDir.isDirectory() )
        {
            throw new MojoExecutionException( "Clientlib directory does not exist." );
        }

        Runtime rt = Runtime.getRuntime();
        String npm = isWindows() ? "npm.cmd" : "npm";
        try {
            Process pr = rt.exec(npm + " run " + command,null, npmDir);

            StreamGobbler errorGobbler = new StreamGobbler(pr.getErrorStream(), getLog(), LogType.ERROR);
            StreamGobbler outputGobbler = new StreamGobbler(pr.getInputStream(), getLog(), LogType.INFO);

            errorGobbler.start();
            outputGobbler.start();
            pr.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        File distDir = new File(npmDir,distName);
        if ( !distDir.exists() || !distDir.isDirectory() )
        {
            throw new MojoExecutionException( "Dist directory does not exist." );
        }

        getLog().info("Copying dist directory");
        try {
            FileUtils.copyDirectory(distDir,clientlibDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File jsTxt = new File(clientlibDir, "js.txt");
        File cssTxt = new File(clientlibDir, "css.txt");
        try {
            FileWriter jsWriter = new FileWriter(jsTxt);
            FileWriter cssWriter = new FileWriter(cssTxt);

            String names[] = clientlibDir.list();
            if(names != null){
                for (String name : names) {
                    if(name.matches(".*\\.js$")){
                        jsWriter.append(name).append("\n");
                    }else if(name.matches(".*\\.css$")){
                        cssWriter.append(name).append("\n");
                    }
                }
            }

            jsWriter.flush();
            jsWriter.close();

            cssWriter.flush();
            cssWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        getLog().info("Completed");

    }

    enum LogType {
        ERROR,
        INFO,
        WARNING
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private class StreamGobbler extends Thread {
        private InputStream is;
        private Log log;
        private LogType logType;

        StreamGobbler(InputStream is, Log log, LogType type) {
            this.is = is;
            this.log = log;
            this.logType = type;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ( (line = br.readLine()) != null) {
                    switch (logType){
                        case INFO:{
                            log.info(line);
                            break;
                        }
                        case ERROR:{
                            log.error(line);
                            break;
                        }
                        case WARNING:{
                            log.warn(line);
                            break;
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}