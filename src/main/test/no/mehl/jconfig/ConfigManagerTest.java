package no.mehl.jconfig;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ConfigManagerTest {

    @Test(expected = ConfiguratorException.class)
    public void getValue_givenNoConfig_shouldThrowException() {
        buildStandard().getStringProperty("foo");
    }

    @Test(expected = ConfiguratorException.class)
    public void getValue_givenNoValue_shouldThrowException() {
        buildStandard().getStringProperty("foo");
    }

    @Test
    public void getValue_givenValueInCategory_shouldReturnValue() {
        ConfigManager configManager = buildJson("{\"dev\": {\"foo\": \"bar\"}}");
        assertEquals("bar", configManager.getStringProperty("dev", "foo"));
    }

    @Test
    public void getArrayValue_givenValue_shouldReturnArray() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [\"one\", \"two\"]}}");
        List<String> values = configManager.getStringListProperty("foo");
        assertEquals(2, values.size());
    }

    @Test(expected = ConfiguratorException.class)
    public void getListValue_givenNonStringList_shouldThrowException() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [1, \"two\"]}}");
        List<String> values = configManager.getStringListProperty("foo");
    }

    private ConfigManager buildStandard() {
        return new ConfigManager.ConfiguratorBuilder().build();
    }

    private ConfigManager buildJson(String json) {
        return new ConfigManager.ConfiguratorBuilder().withJsonConfig(json).build();
    }


}