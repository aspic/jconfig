package no.mehl.jconfig;

import com.google.gson.reflect.TypeToken;
import no.mehl.jconfig.listener.ConfigChangeListener;
import no.mehl.jconfig.listener.ConfigManagerListener;
import no.mehl.jconfig.pojo.Config;
import no.mehl.jconfig.watcher.FileWatcher;
import no.mehl.jconfig.watcher.RemoteFileWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class encapsulates a {@link no.mehl.jconfig.pojo.Config} and provides means for getting
 * values and listening for config changes.
 */
public class ConfigManager implements ConfigChangeListener {

    private static final String DEFAULT_CATEGORY = "local";

    private Optional<Config> config = Optional.empty();
    private List<ConfigManagerListener> configListeners;
    private ScheduledExecutorService pool;

    private ConfigManager() {
        configListeners = new ArrayList<>();
        pool = new ScheduledThreadPoolExecutor(1);
    }

    public String getStringProperty(String key) {
        return getStringProperty(DEFAULT_CATEGORY, key);
    }

    public String getStringProperty(String category, String key) {
        return parseTypedProperty(getCategoryValue(category, key), String.class);
    }

    public String getIntProperty(String key) {
        return getStringProperty(DEFAULT_CATEGORY, key);
    }

    public int getIntProperty(String category, String key) {
        return parseTypedProperty(getCategoryValue(category, key), Double.class).intValue();
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
                throw new ConfigException(String.format("List in \"%s\" for key \"%s\" does contain contain a non String item=%s", category, key, item));
            }
        }
        return strings;
    }

    private Object getCategoryValue(String category, String key) {
        if (!config.isPresent()) {
            throw new ConfigException("No config loaded, unable to get value");
        }
        Map<String, Object> env = config.get().get(category);
        if (env == null) {
            throw new ConfigException(String.format("Category %s does not exist", category));
        }
        return env.get(key);
    }

    private <T> T parseTypedProperty(Object value, Class<T> clazz) {
        if (value == null) {
            throw new ConfigException("Value was null");
        }
        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            throw new ConfigException(String.format("Unable to cast value=%s of type %s", value, value.getClass()));
        }
    }

    public void addConfigChangedListener(ConfigManagerListener listener) {
        configListeners.add(listener);
    }

    @Override
    public void configChanged(Config newConfig) {
        this.config = Optional.ofNullable(newConfig);
        for (ConfigManagerListener ccl : configListeners) {
            ccl.configChanged(this);
        }
    }

    public static class ConfiguratorBuilder {

        private ConfigManager configManager = new ConfigManager();
        private ConfigParser parser = new ConfigParser();

        public ConfiguratorBuilder withConfig(Config config) {
            configManager.config = Optional.ofNullable(config);
            return this;
        }

        public ConfiguratorBuilder withJson(String json) {
            configManager.config = Optional.ofNullable(parser.parseJson(json));
            return this;
        }

        public ConfiguratorBuilder withResources(String resourcePath) {
            configManager.config = Optional.ofNullable(parser.parseFilePath(resourcePath));
            return this;
        }

        public ConfiguratorBuilder withFileWatcher(String directory, String file, long interval, TimeUnit unit) {
            configManager.pool.scheduleAtFixedRate(new FileWatcher(directory, file, configManager), 0, interval, unit);
            return this;
        }

        public ConfiguratorBuilder withRemoteFileWatcher(String url, long interval, TimeUnit unit) {
            configManager.pool.scheduleAtFixedRate(new RemoteFileWatcher(url, configManager), 0, interval, unit);
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
