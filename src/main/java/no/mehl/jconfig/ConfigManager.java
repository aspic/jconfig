package no.mehl.jconfig;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import no.mehl.jconfig.pojo.Config;
import no.mehl.jconfig.updater.FileUpdater;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConfigManager implements ConfigChangeListener {

    private static final String DEFAULT_CATEGORY = "local";

    private Config config;
    private List<ConfigChangeListener> configListeners;
    private ScheduledExecutorService pool;

    private static final Type ARRAY_STRING = new TypeToken<List<String[]>>() {}.getType();

    private ConfigManager() {
        configListeners = new ArrayList<ConfigChangeListener>();
        pool = new ScheduledThreadPoolExecutor(1);
    }

    public String getStringProperty(String key) {
        return getStringProperty(DEFAULT_CATEGORY, key);
    }

    public String getStringProperty(String category, String key) {
        return parseTypedProperty(getCategoryValue(category, key), String.class);
    }

    public List<String> getStringListProperty(String key) {
        return getStringListProperty(DEFAULT_CATEGORY, key);
    }

    public List<String> getStringListProperty(String category, String key) {
        List l = parseTypedProperty(getCategoryValue(category, key), List.class);
        List<String> strings = new ArrayList<String>();
        for (Object item : l) {
            if (item instanceof String) {
                strings.add((String) item);
            } else {
                throw new ConfiguratorException(String.format("List in \"%s\" for key \"%s\" does contain contain a non String item=%s", category, key, item));
            }
        }
        return strings;
    }

    private Object getCategoryValue(String category, String key) {
        if (config == null) {
            throw new ConfiguratorException("No config loaded, unable to get value");
        }
        Map<String, Object> env = config.get(category);
        if (env == null) {
            throw new ConfiguratorException(String.format("Category %s does not exist", category));
        }
        return env.get(key);
    }

    private <T> T parseTypedProperty(Object value, Class<T> clazz) {
        if (value == null) {
            throw new ConfiguratorException("Value was null");
        }
        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            throw new ConfiguratorException(String.format("Unable to cast value=%s of type %s", value, value.getClass()));
        }
    }

    public void addConfigChangedListener(ConfigChangeListener listener) {
        configListeners.add(listener);
    }

    @Override
    public void configChanged(Config newConfig) {
        this.config = newConfig;
        for (ConfigChangeListener ccl : configListeners) {
            ccl.configChanged(newConfig);
        }
    }

    public static class ConfiguratorBuilder {

        private ConfigManager configManager = new ConfigManager();

        public ConfiguratorBuilder withConfig(Config config) {
            configManager.config = config;
            return this;
        }

        public ConfiguratorBuilder withJsonConfig(String json) {
            return withConfig(new Gson().fromJson(json, Config.class));
        }

        public ConfiguratorBuilder withFileConfig(String path) {
            try {
                String config = new String(Files.readAllBytes(
                        Paths.get(getClass().getClassLoader()
                                .getResource(path)
                                .toURI())));
                return withJsonConfig(config);
            } catch (IOException e) {
                throw new ConfiguratorException(String.format("Unable to read config for path=%s", path), e);
            } catch (URISyntaxException e) {
                throw new ConfiguratorException(String.format("Invalid URI for path=%s", path), e);
            }
        }

        public ConfiguratorBuilder withConfigWatcher(String directory, String file, long interval, TimeUnit unit) {
            configManager.pool.scheduleAtFixedRate(new FileUpdater(directory, file, configManager), interval, interval, unit);
            return this;
        }



        public ConfigManager build() {
            return configManager;
        }

    }

    public ExecutorService getPool() {
        return this.pool;
    }

}
