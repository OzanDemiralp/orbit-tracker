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

    public Mono<String> getIssTle(){
        return this.webClient.get()
                .uri("/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle")
                .retrieve()
                .bodyToMono(String.class);
    }
}
