package ru.itmo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.persistence.annotations.FetchAttribute;
import org.eclipse.persistence.annotations.FetchGroup;
import org.eclipse.persistence.annotations.ReturnInsert;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "city")
@FetchGroup(
        name = "city.withRelations",
        attributes = {
                @FetchAttribute(name = "coordinates"),
                @FetchAttribute(name = "governor")
        }
)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @ReturnInsert(returnOnly = true)
    private LocalDate creationDate;

    @Positive(message = "Area must be > 0")
    @Column(name = "area", nullable = false)
    private int area;

    @NotNull(message = "Population cannot be null")
    @Positive(message = "Population must be > 0")
    @Column(name = "population", nullable = false)
    private Long population;

    @Temporal(TemporalType.DATE)
    @Column(name = "establishment_date")
    private Date establishmentDate;

    @Column(name = "capital", nullable = false)
    private boolean capital;

    @Column(name = "meters_above_sea_level")
    private Integer metersAboveSeaLevel;

    @Positive(message = "Telephone code must be > 0")
    @Max(value = 100000, message = "Telephone code must be ≤ 100000")
    @Column(name = "telephone_code")
    private Integer telephoneCode;

    @NotNull(message = "Climate must be specified")
    @Convert(converter = ClimatePgEnumConverter.class)
    @Column(name = "climate", columnDefinition = "climate", nullable = false)
    private Climate climate;

    @Convert(converter = GovernmentPgEnumConverter.class)
    @Column(name = "government", columnDefinition = "government")
    private Government government;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "governor_id")
    private Human governor;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.name = name;
    }

    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        this.coordinates = coordinates;
    }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }

    public Integer getArea() { return area; }
    public void setArea(Integer area) {
        if (area == null || area <= 0) {
            throw new IllegalArgumentException("Area must be > 0");
        }
        this.area = area;
    }

    public Long getPopulation() { return population; }
    public void setPopulation(Long population) {
        if (population == null || population <= 0) {
            throw new IllegalArgumentException("Population must be > 0 and not null");
        }
        this.population = population;
    }

    public Date getEstablishmentDate() { return establishmentDate; }
    public void setEstablishmentDate(Date establishmentDate) { this.establishmentDate = establishmentDate; }

    public Boolean isCapital() { return capital; }
    public void setCapital(Boolean capital) {
        this.capital = (capital != null && capital);
    }

    public Integer getMetersAboveSeaLevel() { return metersAboveSeaLevel; }
    public void setMetersAboveSeaLevel(Integer metersAboveSeaLevel) { this.metersAboveSeaLevel = metersAboveSeaLevel; }

    public Integer getTelephoneCode() { return telephoneCode; }
    public void setTelephoneCode(Integer telephoneCode) {
        if (telephoneCode != null) {
            if (telephoneCode <= 0 || telephoneCode > 100000) {
                throw new IllegalArgumentException("Telephone code must be > 0 and ≤ 100000");
            }
        }
        this.telephoneCode = telephoneCode;
    }

    public Climate getClimate() { return climate; }

    public void setClimate(Climate climate) {
        if (climate == null) {
            throw new IllegalArgumentException("Climate must be specified");
        }
        this.climate = climate;
    }

    public Government getGovernment() { return government; }
    public void setGovernment(Government government) { this.government = government; }

    public Human getGovernor() { return governor; }
    public void setGovernor(Human governor) { this.governor = governor; }
}
