package com.duckzito.notification.ratelimiter.config.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


//TODO: this should apply to certain endpoints only
@Slf4j
@Component
public class RateLimitFilter implements WebFilter {

    //TODO: Replace this with a cache
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket createIPRateLimitBucket()  {
        Bandwidth limit = Bandwidth.simple(10, Duration.ofMinutes(1));

        return Bucket4j.builder().addLimit(limit).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .map(ip -> {
                    if (!buckets.containsKey(ip)) {
                        log.info("Creating new bucket for ip: {}", ip);
                        buckets.put(ip, createIPRateLimitBucket());
                    }
                    return ip;
                })
                .doOnNext( ip -> log.info("Available IP tokens left: {}", buckets.get(ip).getAvailableTokens()))
                .map(ip -> buckets.get(ip).tryConsumeAndReturnRemaining(1))
                .filter( consumed -> consumed.getRemainingTokens() == 0)
                .map(remaining -> this.updateExceeded(exchange, remaining.getRemainingTokens(), remaining.getNanosToWaitForRefill()))
                .thenEmpty(chain.filter(exchange));


    }

    private Mono<Void> updateExceeded(ServerWebExchange exchange, Long remaining, Long waitToRefill) {
        exchange.getResponse().setStatusCode(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", remaining.toString());
        exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", String.valueOf(TimeUnit.NANOSECONDS.toSeconds(waitToRefill)));

        return Mono.empty();
    }
}
