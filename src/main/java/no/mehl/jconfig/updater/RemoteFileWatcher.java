package no.mehl.jconfig.updater;

import no.mehl.jconfig.ConfigChangeListener;
import no.mehl.jconfig.ConfigException;
import no.mehl.jconfig.ConfigParser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Synchronizes config with a web service
 */
public class RemoteFileWatcher implements Runnable {

    private URL url;
    private long cachedFileSize;
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
        int i;
        int sum = 0;
        try {
            URLConnection con = url.openConnection();
            Path tempPath = Files.createTempFile("remote-config", ".json");
            File tempFile = tempPath.toFile();
            BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile.getName()));
            while ((i = bis.read()) != -1) {
                bos.write(i);
                sum += i;
            }
            bos.flush();
            bis.close();

            if (sum != cachedFileSize) {
                listener.configChanged(parser.parseFile(tempPath));
                cachedFileSize = sum;
            }

        } catch (MalformedInputException malformedInputException) {
            malformedInputException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
