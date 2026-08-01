package com.distribuidora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "distribuidora.stock")
public class StockProperties {

    private int defaultLowThreshold = 20;

    public int getDefaultLowThreshold() {
        return defaultLowThreshold;
    }

    public void setDefaultLowThreshold(int defaultLowThreshold) {
        this.defaultLowThreshold = defaultLowThreshold;
    }
}
