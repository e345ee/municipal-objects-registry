package org.itmo.api;

import java.util.List;

public class CityPageRequest {

    private Integer page = 0;
    private Integer size = 20;


    private List<String> sort = List.of("id,asc");


    private String name;
    private String climate;
    private String government;

    private Long id;
    private Long coordinatesId;
    private Long governorId;

    private Boolean governorIdIsNull;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCoordinatesId() {
        return coordinatesId;
    }

    public void setCoordinatesId(Long coordinatesId) {
        this.coordinatesId = coordinatesId;
    }

    public Long getGovernorId() {
        return governorId;
    }

    public void setGovernorId(Long governorId) {
        this.governorId = governorId;
    }

    private String sortBy;

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    private String dir;

    private Long population;
    private Integer telephoneCode;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
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

    public String getGovernment() {
        return government;
    }

    public void setGovernment(String government) {
        this.government = government;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
    }

    public Integer getTelephoneCode() {
        return telephoneCode;
    }

    public void setTelephoneCode(Integer telephoneCode) {
        this.telephoneCode = telephoneCode;
    }

    public Boolean getGovernorIdIsNull() { return governorIdIsNull; }
    public void setGovernorIdIsNull(Boolean governorIdIsNull) { this.governorIdIsNull = governorIdIsNull; }
}