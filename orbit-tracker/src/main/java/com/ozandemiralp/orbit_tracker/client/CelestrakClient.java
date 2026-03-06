package com.ozandemiralp.orbit_tracker.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CelestrakClient {

    private final WebClient webClient;

    public CelestrakClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getTleDataByGroup(String group) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/NORAD/elements/gp.php")
                        .queryParam("GROUP", group)
                        .queryParam("FORMAT", "tle")
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }}
