package iuh.fit.goat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix để Spring map các message riêng theo user
        registry.setUserDestinationPrefix("/user");

        // Prefix khi FE muốn gửi message lên BE (MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")                    // URL kết nối: ws://localhost:8080/ws
                .setAllowedOriginPatterns("*")         // hoặc cụ thể: "http://localhost:3000"
                .withSockJS();                         // fallback cho browser cũ + hỗ trợ heartbeat
    }

}