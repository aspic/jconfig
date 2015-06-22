package no.mehl.jconfig;

public class ConfiguratorException extends RuntimeException {
    public ConfiguratorException(String s) {
        super(s);
    }

    public ConfiguratorException(String s, Throwable t) {
        super(s, t);
    }

}
