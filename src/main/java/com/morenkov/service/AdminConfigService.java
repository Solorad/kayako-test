package com.morenkov.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.morenkov.ao.AdminConfigEntity;
import com.morenkov.dto.AdminConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Due to there are only three properties and they are very lightweight, PluginSettings are used here
 * instead of more generic ActiveObjects.
 *
 * @author emorenkov
 */
@Component
public class AdminConfigService {

    private static final Logger log = LoggerFactory.getLogger(AdminConfigService.class);
    public static final String DEFAULT_PREFIX = "com.kayako.";

    private final PluginSettings pluginSettings;

    @Autowired
    public AdminConfigService(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    /**
     * Saves value to plugin properties. Default plugin specific prefix is used here.
     *
     * @param name
     * @param value
     */
    public void setProperty(String name, String value) {
        pluginSettings.put(DEFAULT_PREFIX + name, value);
    }

    /**
     * Retrieve value from plugin properties.
     * @param name
     * @return
     */
    public String getProperty(String name) {
        return (String) pluginSettings.get(DEFAULT_PREFIX + name);
    }
}