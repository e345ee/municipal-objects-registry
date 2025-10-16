package org.itmo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.itmo.domain.City;
import org.itmo.domain.Climate;
import org.itmo.domain.Government;
import java.time.LocalDate;
import java.util.Date;

public class CityDto {
    @Null(message = "Id must be omitted on create")
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Null(message = "Id must be omitted on create")
    private LocalDate creationDate;

    @NotNull(message = "Area must be specified")
    @Positive(message = "Area must be greater than 0")
    private Integer area;

    @NotNull(message = "Population cannot be null")
    @Positive(message = "Population must be greater than 0")
    private Long population;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date establishmentDate;

    @NotNull(message = "Capital must be specified")
    private Boolean capital;


    @NotNull(message = "MetersAboveSeaLevel must be specified")
    private Integer metersAboveSeaLevel;

    @NotNull(message = "Telephone code must be specified")
    @Positive(message = "Telephone code must be greater than 0")
    @Max(value = 100000, message = "Telephone code cannot exceed 100000")
    private Integer telephoneCode;

    @NotNull(message = "Climate must be specified")
    @Pattern(regexp = "RAIN_FOREST|HUMIDSUBTROPICAL|TUNDRA", message = "Invalid climate")
    private String climate;

    @Pattern(regexp = "DEMARCHY|KLEPTOCRACY|CORPORATOCRACY|PLUTOCRACY|THALASSOCRACY",
            message = "Invalid government")
    private String government;

    public CityDto() {
    }

    public CityDto(Long id, String name, int area, Long population, boolean capital, Integer metersAboveSeaLevel, Integer telephoneCode, String climate, String government, LocalDate creationDate, Date establishmentDate) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.population = population;
        this.capital = capital;
        this.metersAboveSeaLevel = metersAboveSeaLevel;
        this.telephoneCode = telephoneCode;
        this.climate = climate;
        this.government = government;
        this.creationDate = creationDate;
        this.establishmentDate = establishmentDate;
    }

    public static CityDto fromEntity(City e) {
        return new CityDto(e.getId(), e.getName(), e.getArea(), e.getPopulation(), e.isCapital(), e.getMetersAboveSeaLevel(), e.getTelephoneCode(), e.getClimate().name(), e.getGovernment().name(), e.getCreationDate(),        // <- из БД
                e.getEstablishmentDate()
        );
    }

    public City toNewEntity() {
        City c = new City();
        c.setName(name);
        c.setArea(area);
        c.setPopulation(population);
        c.setCapital(capital);
        c.setMetersAboveSeaLevel(metersAboveSeaLevel);
        c.setTelephoneCode(telephoneCode);
        c.setClimate(Climate.valueOf(climate));
        c.setGovernment(Government.valueOf(government));
        c.setEstablishmentDate(establishmentDate);
        return c;
    }


    public void applyToEntity(City c) {
        c.setName(name);
        c.setArea(area);
        c.setPopulation(population);
        c.setCapital(capital);
        c.setMetersAboveSeaLevel(metersAboveSeaLevel);
        c.setTelephoneCode(telephoneCode);
        c.setClimate(Climate.valueOf(climate));
        c.setGovernment(Government.valueOf(government));
        c.setEstablishmentDate(establishmentDate);
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
    }

    public boolean isCapital() {
        return capital;
    }

    public void setCapital(boolean capital) {
        this.capital = capital;
    }

    public Integer getMetersAboveSeaLevel() {
        return metersAboveSeaLevel;
    }

    public void setMetersAboveSeaLevel(Integer metersAboveSeaLevel) {
        this.metersAboveSeaLevel = metersAboveSeaLevel;
    }

    public Integer getTelephoneCode() {
        return telephoneCode;
    }

    public void setTelephoneCode(Integer telephoneCode) {
        this.telephoneCode = telephoneCode;
    }

    public String getGovernment() {
        return government;
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

    public void setGovernment(String government) {
        this.government = government;
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
