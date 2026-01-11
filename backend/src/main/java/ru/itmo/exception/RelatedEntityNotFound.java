package ru.itmo.exception;

public class RelatedEntityNotFound extends RuntimeException {
    private final String entity;
    private final Long id;

    public RelatedEntityNotFound(String entity, Long id) {
        super(entity + " not found: " + id);
        this.entity = entity;
        this.id = id;
    }

    public String getEntity() {
        return entity;
    }

    public Long getId() {
        return id;
    }
}