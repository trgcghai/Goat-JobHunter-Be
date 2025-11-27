package iuh.fit.goat.entity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class EventHub<T> {
    private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void push(T event) {
        this.sink.tryEmitNext(event);
    }

    public Flux<T> stream() {
        return this.sink.asFlux();
    }
}
