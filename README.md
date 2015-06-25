# JsonConfigSlurper thingy
Small configuration library for java

## Setup with Gradle

In build.gradle:

    // Adds repository
    maven{
        url 'https://github.com/aspic/java-builds/raw/master/'
    }
    
    // Enable dependency
    dependencies {
        compile 'no.mehl.jconfig:java-config:0.1'
    }
    
## Standard usage

```java
// Json properties
String json = "{ \"production\": { \"secretKey\": \"so secret\" } }";

// Creates config manager
ConfigManager manager = new ConfigManager.ConfigManagerBuilder().withJson(json).build();

// Returns the string for the production category
System.out.println(manager.getString("production", "secretKey")); // prints "so secret"
```
    
## Config watchers

### File watcher

The file watcher detects when an external config file is changed, and reads the new config into memory:
    
```java
// Watch the file "config" in the directory "/tmp"
ConfigManager configManager = new ConfigManager.ConfigManagerBuilder()
                .withFileWatcher("/tmp", "config", 2, TimeUnit.SECONDS) // polls every second
                .build();
```

### Remote file watcher

A remote file watcher downloads and parses a resource over HTTP:

```java
// Watches a json resource from the specified url
ConfigManager manager = new ConfigManager.ConfigManagerBuilder()
                .withRemoteFileWatcher("http://localhost:80/config, 1, TimeUnit.HOURS) // hourly
                .build();
```

## Config changes

Consumers can listen for config changes by adding a listener:

```java
ConfigManagerListener listener = newConfig -> {
    System.out.println("some config changed, reload a property!");
};
configManager.addConfigChangedListener(listener);
```


