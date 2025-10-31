package ru.itmo.websocet;

public class ChangeEvent<T> {
    private String entity;
    private ChangeAction action;
    private Long id;
    private T data;

    public ChangeEvent(String entity,   ChangeAction action, Long id, T data) {
        this.entity = entity;
        this.action = action;
        this.id = id;
        this.data = data;
    }


    public String getEntity() { return entity; }
    public ChangeAction getAction() { return action; }
    public Long getId() { return id; }
    public T getData() { return data; }
}