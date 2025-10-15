package org.itmo.dto;

import jakarta.validation.constraints.Positive;
import org.itmo.domain.City;
import org.itmo.domain.Climate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CityDto {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Positive(message = "Area must be greater than 0")
    private int area;

    @NotNull(message = "Population cannot be null")
    @Positive(message = "Population must be greater than 0")
    private Long population;

    @NotNull(message = "Climate type must be specified")
    private String climate;

    public CityDto() {
    }

    public CityDto(Long id, String name, String climate) {
        this.id = id;
        this.name = name;
        this.climate = climate;
    }

    public static CityDto fromEntity(City e) {
        return new CityDto(e.getId(), e.getName(), e.getClimate().name());
    }

    public City toNewEntity() {
        City c = new City();
        c.setName(name);
        c.setClimate(Climate.valueOf(climate));
        return c;
    }

    public void applyToEntity(City c) {
        c.setName(name);
        c.setClimate(Climate.valueOf(climate));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClimate() {
        return climate;
    }

    public void setClimate(String climate) {
        this.climate = climate;
    }
}
