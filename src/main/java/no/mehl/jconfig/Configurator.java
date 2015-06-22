package no.mehl.jconfig;

import com.google.gson.Gson;
import no.mehl.jconfig.pojo.Config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Configurator implements ConfigChangeListener {

    private static final String DEFAULT_ENVIRONMENT = "local";

    private Config config;
    private List<ConfigChangeListener> configListeners;
    private ScheduledExecutorService pool;

    private Configurator() {
        configListeners = new ArrayList<ConfigChangeListener>();
        pool = new ScheduledThreadPoolExecutor(1);
    }

    public String getStringProperty(String key) {
        return parseTypedProperty(getDefaultEnvironmentValue(key), String.class);
    }

    public String getStringProperty(String environment, String key) {
        return parseTypedProperty(getEnvironmentValue(environment, key), String.class);
    }

    private Object getDefaultEnvironmentValue(String key) {
        return getEnvironmentValue(DEFAULT_ENVIRONMENT, key);
    }

    private Object getEnvironmentValue(String environment, String key) {
        if (config == null) {
            throw new ConfiguratorException("No config loaded, unable to get value");
        }
        Map<String, Object> env = config.get(environment);
        if (env == null) {
            throw new ConfiguratorException(String.format("Environment %s does not exist", environment));
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

        private Configurator configurator = new Configurator();

        public ConfiguratorBuilder withConfig(Config config) {
            configurator.config = config;
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
            configurator.pool.scheduleAtFixedRate(new ConfigWatcher(directory, file, configurator), interval, interval, unit);
            return this;
        }



        public Configurator build() {
            return configurator;
        }

    }

    public ExecutorService getPool() {
        return this.pool;
    }

}
