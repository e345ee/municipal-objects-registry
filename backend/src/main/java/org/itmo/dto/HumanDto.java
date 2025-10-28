package org.itmo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import org.itmo.domain.Human;

public class HumanDto {

    @Null(message = "Id of Human must be omitted on create or update")
    private Long id;

    @NotNull(message = "Height must be specified")
    @Positive(message = "Height must be greater than 0")
    private Float height;

    public HumanDto() {
    }

    public HumanDto(Long id, Float height) {
        this.id = id;
        this.height = height;
    }

    public static HumanDto fromEntity(Human e) {if (e == null) return null;
        HumanDto dto = new HumanDto();
        dto.setId(e.getId());
        dto.setHeight(e.getHeight());
        return dto;
    }

    public Human toNewEntity() {
        Human e = new Human();
        applyToEntity(e);
        return e;
    }

    public void applyToEntity(Human e) {
        e.setHeight(this.height);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }
}