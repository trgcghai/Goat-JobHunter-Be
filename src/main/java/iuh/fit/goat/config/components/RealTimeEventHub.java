package iuh.fit.goat.config.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.entity.EventHub;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class RealTimeEventHub {
    private final ObjectMapper objectMapper;
    private final Map<String, EventHub<?>> hubs = new HashMap<>();

    @SuppressWarnings("unchecked")
    private <T> EventHub<T> getHub(String hubName, Class<T> clazz) {
        return (EventHub<T>) this.hubs.computeIfAbsent(hubName, k -> new EventHub<T>());
    }

    @SuppressWarnings("unchecked")
    public <T> void push(String hubName, T event) {
        this.getHub(hubName, (Class<T>) event.getClass()).push(event);
    }

    public <T, R> Flux<ServerSentEvent<String>> stream(
            String hubName, Class<T> clazz, Predicate<T> filter, Function<T, R> converter
    ) {
        return this.getHub(hubName, clazz)
                .stream()
                .filter(filter)
                .flatMap(e -> {
                    try {
                        R dto = converter.apply(e);
                        String json = this.objectMapper.writeValueAsString(dto);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event(hubName + "-event")
                                .data(json)
                                .build()
                        );
                    } catch (Exception ex) {
                        return Flux.error(ex);
                    }
                })
                .onErrorResume(e -> Flux.empty());
    }

}
