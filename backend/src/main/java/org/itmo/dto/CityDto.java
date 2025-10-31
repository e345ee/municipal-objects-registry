package org.itmo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import org.itmo.domain.City;
import org.itmo.domain.Climate;
import org.itmo.domain.Government;

import java.time.LocalDate;
import java.util.Date;

public class CityDto {

    @JsonIgnore
    private boolean coordinatesIdSpecified;
    @JsonIgnore
    private boolean coordinatesSpecified;
    @JsonIgnore
    private boolean governorIdSpecified;
    @JsonIgnore
    private boolean governorSpecified;

    @Null(message = "Id must be omitted on create or update")
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

    private Long coordinatesId;
    private CoordinatesDto coordinates;

    private Long governorId;
    private HumanDto governor;

    public CityDto() {
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

    @Override
    public String toString() {
        return "CityDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", area=" + area +
                ", population=" + population +
                ", establishmentDate=" + establishmentDate +
                ", capital=" + capital +
                ", metersAboveSeaLevel=" + metersAboveSeaLevel +
                ", telephoneCode=" + telephoneCode +
                ", climate='" + climate + '\'' +
                ", government='" + government + '\'' +
                ", coordinatesId=" + coordinatesId +
                ", coordinates=" + coordinates +
                ", governorId=" + governorId +
                ", governor=" + governor +
                '}';
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

    public void setArea(Integer area) {
        this.area = area;
    }

    public Boolean getCapital() {
        return capital;
    }

    public void setCapital(Boolean capital) {
        this.capital = capital;
    }

    public void setCoordinatesId(Long coordinatesId) {
        this.coordinatesId = coordinatesId;
        this.coordinatesIdSpecified = true;
    }
    public Long getCoordinatesId() { return coordinatesId; }

    public void setCoordinates(CoordinatesDto coordinates) {
        this.coordinates = coordinates;
        this.coordinatesSpecified = true;
    }
    public CoordinatesDto getCoordinates() { return coordinates; }

    public void setGovernorId(Long governorId) {
        this.governorId = governorId;
        this.governorIdSpecified = true;
    }
    public Long getGovernorId() { return governorId; }

    public void setGovernor(HumanDto governor) {
        this.governor = governor;
        this.governorSpecified = true;
    }
    public HumanDto getGovernor() { return governor; }

    @JsonIgnore public boolean isCoordinatesSpecified() {
        return coordinatesIdSpecified || coordinatesSpecified;
    }
    @JsonIgnore public boolean isGovernorSpecified() {
        return governorIdSpecified || governorSpecified;
    }

    public static CityDto fromEntity(City c) {
        CityDto dto = new CityDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setCreationDate(c.getCreationDate());
        dto.setArea(c.getArea());
        dto.setPopulation(c.getPopulation());
        dto.setEstablishmentDate(c.getEstablishmentDate());
        dto.setCapital(c.isCapital());
        dto.setMetersAboveSeaLevel(c.getMetersAboveSeaLevel());
        dto.setTelephoneCode(c.getTelephoneCode());
        dto.setClimate(c.getClimate() != null ? c.getClimate().name() : null);
        dto.setGovernment(c.getGovernment() != null ? c.getGovernment().name() : null);

        if (c.getCoordinates() != null) {
            dto.setCoordinatesId(c.getCoordinates().getId());
            dto.setCoordinates(CoordinatesDto.fromEntity(c.getCoordinates()));
        }

        if (c.getGovernor() != null) {
            dto.setGovernorId(c.getGovernor().getId());
            dto.setGovernor(HumanDto.fromEntity(c.getGovernor()));
        }

        return dto;
    }
}
