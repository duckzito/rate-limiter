package com.duckzito.notification.ratelimiter.controllers;

import com.duckzito.notification.ratelimiter.domain.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/notification", produces = "application/json")
@RequiredArgsConstructor
public class NotificationController {

    @PostMapping("/send")
    public Mono<String> send(@RequestBody Message message) {
        return Mono.just(message.getValue())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

}
