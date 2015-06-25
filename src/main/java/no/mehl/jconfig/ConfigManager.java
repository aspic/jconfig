package no.mehl.jconfig;

import no.mehl.jconfig.listener.ConfigChangeListener;
import no.mehl.jconfig.listener.ConfigManagerListener;
import no.mehl.jconfig.pojo.Config;
import no.mehl.jconfig.watcher.FileWatcher;
import no.mehl.jconfig.watcher.RemoteFileWatcher;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class encapsulates a {@link no.mehl.jconfig.pojo.Config} and provides means for getting
 * values and listening for config changes.
 */
public class ConfigManager implements ConfigChangeListener {

    private Optional<String> defaultCategory = Optional.of("local");

    private Optional<Config> config = Optional.empty();
    private List<ConfigManagerListener> configListeners;
    private ScheduledExecutorService pool;

    private ConfigManager() {
        configListeners = new ArrayList<>();
        pool = new ScheduledThreadPoolExecutor(1);
    }

    public String getString(String key) {
        return getString(defaultCategory.get(), key);
    }

    public String getString(String category, String key) {
        return parseTypedProperty(getCategoryValue(category, key), String.class);
    }

    public String getInt(String key) {
        return getString(defaultCategory.get(), key);
    }

    public int getInt(String category, String key) {
        return parseTypedProperty(getCategoryValue(category, key), Double.class).intValue();
    }

    public List<String> getStringList(String key) {
        return getStringList(defaultCategory.get(), key);
    }

    public List<String> getStringList(String category, String key) {
        try {
            List l = parseTypedProperty(getCategoryValue(category, key), List.class);
            return Arrays.asList(Arrays.copyOf(l.toArray(), l.size(), String[].class));
        } catch (ArrayStoreException e) {
            throw new ConfigException("Unable to create list of strings", e);
        }
    }

    public List<Integer> getIntList(String key) {
        return getIntList(defaultCategory.get(), key);
    }

    private List<Integer> getIntList(String category, String key) {
        List<Double> doubles = getDoubleList(category, key);
        return doubles.stream().filter(this::isInt).map(Double::intValue).collect(Collectors.toList());
    }

    public List<Double> getDoubleList(String key) {
        return getDoubleList(defaultCategory.get(), key);
    }

    private List<Double> getDoubleList(String category, String key) {
        try {
            List l = parseTypedProperty(getCategoryValue(category, key), List.class);
            return Arrays.asList(Arrays.copyOf(l.toArray(), l.size(), Double[].class));
        } catch (ArrayStoreException e) {
            e.printStackTrace();
            throw new ConfigException("Unable to create list of integers", e);
        }
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

    private boolean isInt(double d) {
        if(d == Math.floor(d) && !Double.isInfinite(d)) {
            return true;
        }
        throw new ConfigException(String.format("Value=%f is not an integer", d));
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

        public ConfiguratorBuilder withDefaultCategory(String category) {
            configManager.defaultCategory = Optional.of(category);
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
