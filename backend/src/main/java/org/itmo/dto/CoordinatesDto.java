package org.itmo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import org.itmo.domain.Coordinates;

public class CoordinatesDto {

    @Null(message = "Id of Coordinates must be omitted on create or update")
    private Long id;

    @Max(value = 460, message = "X must be less than 460")
    @NotNull(message = "x must be not null")
    private Float x;

    @NotNull(message = "Y must be not null")
    private Float y;

    public CoordinatesDto() {}

    public CoordinatesDto(Long id, Float x, Float y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public static CoordinatesDto fromEntity(Coordinates e) {
        if (e == null) return null;
        CoordinatesDto dto = new CoordinatesDto();
        dto.setId(e.getId());
        dto.setX(e.getX());
        dto.setY(e.getY());
        return dto;
    }

    public Coordinates toNewEntity(){
        Coordinates coordinates = new Coordinates();
        applyToEntity(coordinates);
        return coordinates;
    }

    public void applyToEntity(Coordinates e) {
        e.setX(this.x);
        e.setY(this.y);
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }
}
