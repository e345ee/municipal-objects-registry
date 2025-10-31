package ru.itmo.page;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;


public class CityPageRequest {

    private Integer page = 0;
    private Integer size = 20;

    private List<String> sort = new ArrayList<>(List.of("id,asc"));


    private String sortBy;
    private String dir = "asc";

    private Long id;
    private String name;
    private String climate;
    private String government;
    private Long population;
    private Integer telephoneCode;
    private Boolean capital;
    private Long area;
    private Long metersAboveSeaLevel;


    private Long coordinatesId;
    private Long governorId;
    private Boolean governorIdIsNull;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate establishmentDate;


    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public List<String> getSort() { return sort; }
    public void setSort(List<String> sort) { this.sort = sort; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClimate() { return climate; }
    public void setClimate(String climate) { this.climate = climate; }

    public String getGovernment() { return government; }
    public void setGovernment(String government) { this.government = government; }

    public Long getPopulation() { return population; }
    public void setPopulation(Long population) { this.population = population; }

    public Integer getTelephoneCode() { return telephoneCode; }
    public void setTelephoneCode(Integer telephoneCode) { this.telephoneCode = telephoneCode; }

    public Boolean getCapital() { return capital; }
    public void setCapital(Boolean capital) { this.capital = capital; }

    public Long getArea() { return area; }
    public void setArea(Long area) { this.area = area; }

    public Long getMetersAboveSeaLevel() { return metersAboveSeaLevel; }
    public void setMetersAboveSeaLevel(Long metersAboveSeaLevel) { this.metersAboveSeaLevel = metersAboveSeaLevel; }

    public Long getCoordinatesId() { return coordinatesId; }
    public void setCoordinatesId(Long coordinatesId) { this.coordinatesId = coordinatesId; }

    public Long getGovernorId() { return governorId; }
    public void setGovernorId(Long governorId) { this.governorId = governorId; }

    public Boolean getGovernorIdIsNull() { return governorIdIsNull; }
    public void setGovernorIdIsNull(Boolean governorIdIsNull) { this.governorIdIsNull = governorIdIsNull; }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }

    public LocalDate getEstablishmentDate() { return establishmentDate; }
    public void setEstablishmentDate(LocalDate establishmentDate) { this.establishmentDate = establishmentDate; }
}
