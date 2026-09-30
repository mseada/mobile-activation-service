package com.telecom.activation.stub;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnStubsEnabled
@RequiredArgsConstructor
@RequestMapping("/stubs")
public class StubInspectionController {

    private final StubRegistry registry;

    @GetMapping("/invocations")
    public List<StubInvocation> invocations() {
        return registry.invocations();
    }

    @GetMapping("/kafka/events")
    public List<PublishedEvent> kafkaEvents() {
        return registry.events();
    }

    @DeleteMapping
    public ResponseEntity<Void> reset() {
        registry.clear();
        return ResponseEntity.noContent().build();
    }
}
