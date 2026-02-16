package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.application.security.ActorContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(ApiPaths.TERMINAL_ME)
public class TerminalMeQueryController {

    @GetMapping("/status")
    public Map<String, String> status(ActorContext actorContext) {
        return Map.of("terminalUid", actorContext.actorRef(), "status", "ACTIVE");
    }

    @GetMapping("/config")
    public Map<String, String> config(ActorContext actorContext) {
        return Map.of("terminalUid", actorContext.actorRef(), "mode", "STANDARD");
    }

    @GetMapping("/health")
    public Map<String, String> health(ActorContext actorContext) {
        return Map.of("terminalUid", actorContext.actorRef(), "health", "UP");
    }
}
