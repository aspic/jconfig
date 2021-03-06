package no.mehl.jconfig;

import com.google.gson.Gson;
import no.mehl.jconfig.pojo.Config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities to read different types of files/paths
 */
public class ConfigParser {

    public Config parseJson(String json) {
        try {
            return new Gson().fromJson(json, Config.class);
        } catch (Exception e) {
            throw new ConfigException("Unable to parse json", e);
        }
    }

    public Config parseFilePath(String path) {
        try {
            return parseFile(Paths.get(getClass().getClassLoader().getResource(path).toURI()));
        } catch (URISyntaxException e) {
            throw new ConfigException(String.format("Invalid URI for path=%s", path), e);
        }
    }

    public Config parseFile(Path path) {
        try {
            return parseJson(new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            throw new ConfigException(String.format("Unable to read config for path=%s", path), e);
        }
    }
}
