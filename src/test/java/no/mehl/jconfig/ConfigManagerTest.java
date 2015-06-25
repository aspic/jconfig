package no.mehl.jconfig;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ConfigManagerTest {

    @Test(expected = ConfigException.class)
    public void getValue_givenNoConfig_shouldThrowException() {
        buildStandard().getString("foo");
    }

    @Test(expected = ConfigException.class)
    public void getValue_givenNoValue_shouldThrowException() {
        buildStandard().getString("foo");
    }

    @Test
    public void getValue_givenValueInCategory_shouldReturnValue() {
        ConfigManager configManager = buildJson("{\"dev\": {\"foo\": \"bar\"}}");
        assertEquals("bar", configManager.getString("dev", "foo"));
    }

    @Test
    public void getStringList_givenStringList_shouldStringList() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [\"one\", \"two\"]}}");
        List<String> values = configManager.getStringList("foo");
        assertEquals(2, values.size());
    }

    @Test(expected = ConfigException.class)
    public void getStringList_givenMixedList_shouldThrowException() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [1, \"two\"]}}");
        configManager.getStringList("foo");
    }

    @Test
    public void getDoubleList_givenDoubleList_shouldReturnDoubleList() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [1.0001, 10000]}}");
        List<Double> values = configManager.getDoubleList("foo");
        assertEquals(1.0001, values.get(0), 0.00000001);
        assertEquals(10000, values.get(1), 0.00000001);
    }

    @Test
    public void getIntList_givenIntList_shouldReturnIntList() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [1, 22123]}}");
        List<Integer> values = configManager.getIntList("foo");
        assertEquals(1, values.get(0), 0.00000001);
        assertEquals(22123, values.get(1), 0.00000001);
    }

    @Test(expected = ConfigException.class)
    public void getIntList_givenMixedList_shouldThrowException() {
        ConfigManager configManager = buildJson("{\"local\": {\"foo\": [1, 22123.01]}}");
        List<Integer> values = configManager.getIntList("foo");
        assertEquals(1, values.get(0), 0.00000001);
        assertEquals(22123, values.get(1), 0.00000001);
    }

    private ConfigManager buildStandard() {
        return new ConfigManager.ConfigManagerBuilder().build();
    }

    private ConfigManager buildJson(String json) {
        return new ConfigManager.ConfigManagerBuilder().withJson(json).build();
    }


}