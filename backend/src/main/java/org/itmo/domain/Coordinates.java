package org.itmo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;

@Entity @Table(name = "coordinates")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Max(value = 460, message = "X code cannot exceed 460")
    @Column(nullable = false)
    private float x;

    @Column(nullable = false)
    private Float y;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }
}
