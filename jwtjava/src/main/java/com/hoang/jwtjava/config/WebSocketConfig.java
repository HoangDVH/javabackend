package com.hoang.jwtjava.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final CorsProperties corsProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null)
                    return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                        && !"/user/queue/orders".equals(accessor.getDestination()))
                    throw new BadCredentialsException("Subscription destination not allowed");
                if (StompCommand.SEND.equals(accessor.getCommand()))
                    throw new BadCredentialsException("Client messages are not allowed");
                return message;
            }
        });
    }

    private JwtAuthenticationToken authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new BadCredentialsException("Missing WebSocket bearer token");

        Jwt jwt = jwtDecoder.decode(authorization.substring(7).trim());
        String scope = Optional.ofNullable(jwt.getClaimAsString("scope")).orElse("");
        List<SimpleGrantedAuthority> authorities = Arrays.stream(
                        scope.split("\\s+"))
                .filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        boolean sellerOrAdmin = authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SELLER")
                        || authority.getAuthority().equals("ROLE_ADMIN"));
        if (!sellerOrAdmin)
            throw new BadCredentialsException("Seller or admin role required");

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
