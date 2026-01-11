package ru.itmo.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WsEventPublisher {

    private final SimpMessagingTemplate template;

    public WsEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public <T> void sendChange(String entity, ChangeAction action, Long id, T dto) {
        ChangeEvent<T> event = new ChangeEvent<>(entity, action, id, dto);

        String topic = switch (entity) {
            case "City" -> "/topic/cities";
            case "Human" -> "/topic/humans";
            case "Coordinates" -> "/topic/coordinates";
            default -> "/topic/changes";
        };

        template.convertAndSend(topic, event);
    }
}