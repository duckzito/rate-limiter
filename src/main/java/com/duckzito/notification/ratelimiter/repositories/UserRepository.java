package com.duckzito.notification.ratelimiter.repositories;

import com.duckzito.notification.ratelimiter.domain.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByUsername(String username);

}
