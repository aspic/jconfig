package no.mehl.jconfig.listener;

import no.mehl.jconfig.pojo.Config;

public interface ConfigChangeListener {
    public void configChanged(Config newConfig);
}
