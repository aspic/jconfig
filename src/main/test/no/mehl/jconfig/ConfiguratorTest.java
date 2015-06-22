package no.mehl.jconfig;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfiguratorTest {

    @Test(expected = ConfiguratorException.class)
    public void getValue_givenNoConfig_shouldThrowException() {
        buildStandard().getStringProperty("foo");
    }

    @Test(expected = ConfiguratorException.class)
    public void getValue_givenNoValue_shouldThrowException() {
        buildStandard().getStringProperty("foo");
    }

    @Test
    public void getValue_givenValueInEnvironment_shouldReturnValue() {
        Configurator configurator = buildJson("{\"dev\": {\"foo\": \"bar\"}}");
        assertEquals("bar", configurator.getStringProperty("dev", "foo"));
    }

    private Configurator buildStandard() {
        return new Configurator.ConfiguratorBuilder().build();
    }

    private Configurator buildJson(String json) {
        return new Configurator.ConfiguratorBuilder().withJsonConfig(json).build();
    }


}