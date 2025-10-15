package org.itmo.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "city")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Positive(message = "Area must be greater than 0")
    private int area;

    @NotNull(message = "Population cannot be null")
    @Positive(message = "Population must be greater than 0")
    private Long population;

    private boolean capital;

    private Integer metersAboveSeaLevel;

    @Positive(message = "Telephone code must be greater than 0")
    @Max(value = 100000, message = "Telephone code cannot exceed 100000")
    private Integer telephoneCode;

    @NotNull(message = "Climate type must be specified")
    @Column(name = "CLIMATE", columnDefinition = "climate")
    @Convert(converter = ClimatePgEnumConverter.class)
    private Climate climate;

    @Column(name = "GOVERNMENT", columnDefinition = "government")
    @Convert(converter = GovernmentPgEnumConverter.class)
    private Government government;

    public Long getId() {
        return id;
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public Government getGovernment() {
        return government;
    }

    public void setGovernment(Government government) {
        this.government = government;
    }

    public Integer getTelephoneCode() {
        return telephoneCode;
    }

    public void setTelephoneCode(Integer telephoneCode) {
        this.telephoneCode = telephoneCode;
    }

    public Integer getMetersAboveSeaLevel() {
        return metersAboveSeaLevel;
    }

    public void setMetersAboveSeaLevel(Integer metersAboveSeaLevel) {
        this.metersAboveSeaLevel = metersAboveSeaLevel;
    }

    public boolean isCapital() {
        return capital;
    }

    public void setCapital(boolean capital) {
        this.capital = capital;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
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

    public Climate getClimate() {
        return climate;
    }

    public void setClimate(Climate climate) {
        this.climate = climate;
    }
}
