package com.ozandemiralp.orbit_tracker.config;

import jakarta.annotation.PostConstruct;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class OrekitConfig {
    @PostConstruct
    public void initOrekit() {
        File orekitData = new File("orbit-tracker/src/main/resources/orekit-data-main");
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        DataContext.getDefault()
                .getDataProvidersManager()
                .addProvider(new DirectoryCrawler(orekitData));
    }
}
