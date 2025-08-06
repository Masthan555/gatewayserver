package com.eazybytes.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class GatewayserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayserverApplication.class, args);
    }

    @Bean
    public RouteLocator eazyBankRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route(r -> r.path("/eazybank/accounts/**")
                        .filters(f -> f.rewritePath("/eazybank/accounts/(?<segment>.*)", "/${segment}")
                                .circuitBreaker(c -> c.setName("accountsCircuitBreaker")
                                        .setFallbackUri("forward:/contactSupport"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                                )
                                .requestRateLimiter(rateLimiterConfig -> rateLimiterConfig
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(getKeyResolver())
                                )
                        )
                        .uri("lb://ACCOUNTS")
                )
                .route(r -> r.path("/eazybank/loans/**")
                        .filters(f -> f.rewritePath("/eazybank/loans/(?<segment>.*)", "/${segment}")
                                .circuitBreaker(c -> c.setName("loansCircuitBreaker")
                                        .setFallbackUri("forward:/contactSupport"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                                )
                                .requestRateLimiter(rateLimiterConfig -> rateLimiterConfig
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(getKeyResolver())
                                ))
                        .uri("lb://LOANS"))
                .route(r -> r.path("/eazybank/cards/**")
                        .filters(f -> f.rewritePath("/eazybank/cards/(?<segment>.*)", "/${segment}")
                                .circuitBreaker(c -> c.setName("cardsCircuitBreaker")
                                        .setFallbackUri("forward:/contactSupport"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                                )
                                .requestRateLimiter(rateLimiterConfig -> rateLimiterConfig
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(getKeyResolver())
                                ))
                        .uri("lb://CARDS"))
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 1, 1);
    }

    @Bean
    KeyResolver getKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("user");
            String key = user != null ? user : "anonymous";
            return Mono.just(key);
        };
    }
}
