package org.itmo.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Date;


@Entity
@Table(name = "city")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "creation_date", nullable = false, updatable = false, insertable = false)
    private LocalDate creationDate;

    @Positive(message = "Area must be greater than 0")
    @Column(name = "area", nullable = false)
    private int area;

    @NotNull(message = "Population cannot be null")
    @Positive(message = "Population must be greater than 0")
    @Column(name = "population", nullable = false)
    private Long population;

    @Temporal(TemporalType.DATE)
    @Column(name = "establishment_date")
    private Date establishmentDate;

    @NotNull(message = "Population cannot be null")
    @Column(name = "capital", nullable = false)
    private boolean capital;

    @Column(name = "meters_above_sea_level")          // <- ключевая правка
    private Integer metersAboveSeaLevel;

    @Positive(message = "Telephone code must be greater than 0")
    @Max(value = 100000, message = "Telephone code cannot exceed 100000")
    @Column(name = "telephone_code", nullable = false) // <- ключевая правка
    private Integer telephoneCode;

    @NotNull(message = "Climate type must be specified")
    @Column(name = "climate", columnDefinition = "climate", nullable = false)
    @Convert(converter = ClimatePgEnumConverter.class)
    private Climate climate;

    @NotNull(message = "Government must be specified")
    @Column(name = "government", columnDefinition = "government", nullable = false)
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

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public Date getEstablishmentDate() {
        return establishmentDate;
    }

    public void setEstablishmentDate(Date establishmentDate) {
        this.establishmentDate = establishmentDate;
    }
}
