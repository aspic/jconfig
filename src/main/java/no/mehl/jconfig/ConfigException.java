package no.mehl.jconfig;

public class ConfigException extends RuntimeException {
    public ConfigException(String s) {
        super(s);
    }

    public ConfigException(String s, Throwable t) {
        super(s, t);
    }

}
