package com.ozandemiralp.orbit_tracker.config;

import jakarta.annotation.PostConstruct;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class OrekitConfig {

    @Value("${orekit.data.path:src/main/resources/orekit-data-main.orekit-data}")
    private String orekitDataPath;

    @PostConstruct
    public void init() {
        File orekitData = new File(orekitDataPath);
        if (!orekitData.exists()) {
            throw new RuntimeException("Orekit data not found at " + orekitDataPath);
        }
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
    }
}