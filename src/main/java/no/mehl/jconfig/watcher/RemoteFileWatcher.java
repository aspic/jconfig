package no.mehl.jconfig.watcher;

import no.mehl.jconfig.listener.ConfigChangeListener;
import no.mehl.jconfig.ConfigException;
import no.mehl.jconfig.ConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Synchronizes config with a web service
 */
public class RemoteFileWatcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFileWatcher.class);

    private URL url;
    private long cachedFileBytes;
    private ConfigParser parser;
    private ConfigChangeListener listener;

    public RemoteFileWatcher(String endpoint, ConfigChangeListener listener) {
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new ConfigException(String.format("Malformed url=%s", endpoint), e);
        }
        this.listener = listener;
        this.parser = new ConfigParser();
    }

    @Override
    public void run() {
        BufferedReader in;
        String readLine;
        try {
            Path tempPath = Files.createTempFile("remote-config", ".json");
            File tempFile = tempPath.toFile();
            in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(tempFile) , "UTF-8"));

            while ((readLine = in.readLine()) != null) {
                out.write(readLine);
            }
            out.flush();
            out.close();

            long fileLength = tempFile.length();
            if (cachedFileBytes != fileLength) {
                listener.configChanged(parser.parseFile(tempPath));
                cachedFileBytes = fileLength;
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to read remote config file, config will be unchanged.", e);
        } catch (IOException e) {
            logger.error("Unable to read remote config file, config will be unchanged.", e);
        }
    }
}
