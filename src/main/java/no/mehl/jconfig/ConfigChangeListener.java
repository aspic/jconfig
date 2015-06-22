package no.mehl.jconfig;

import no.mehl.jconfig.pojo.Config;

public interface ConfigChangeListener {
    public void configChanged(Config newConfig);
}
