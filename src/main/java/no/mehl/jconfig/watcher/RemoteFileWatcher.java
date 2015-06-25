package no.mehl.jconfig.watcher;

import no.mehl.jconfig.listener.ConfigChangeListener;
import no.mehl.jconfig.ConfigException;
import no.mehl.jconfig.ConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Synchronizes config with a web service
 */
public class RemoteFileWatcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFileWatcher.class);

    private URL url;
    private Optional<String> cachedHash = Optional.empty();
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
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));

            while ((readLine = in.readLine()) != null) {
                out.write(readLine);
            }
            out.flush();
            out.close();

            String jsonContent = new String(Files.readAllBytes(tempPath));
            Optional<String> md5Hash = getMD5Hash(jsonContent);
            logger.debug("Created md5hash={} for content={}", md5Hash, jsonContent);
            if (!cachedHash.isPresent() || (md5Hash.isPresent() && !cachedHash.get().equals(md5Hash.get()))) {
                listener.configChanged(parser.parseJson(jsonContent));
            }
            cachedHash = md5Hash;
        } catch (IOException e) {
            logger.error("Unable to read remote config file, config will be unchanged.", e);
        } catch (Exception e) {
            logger.error("Error when reading content", e);
        }
    }

    private Optional<String> getMD5Hash(String content) {
        try {
            return Optional.ofNullable((new HexBinaryAdapter()).marshal(MessageDigest.getInstance("MD5").digest(content.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to create hash of string", e);
        }
        return Optional.empty();
    }
}
