package ru.itmo.dto;

import java.util.List;

public class ImportResultDto {
    private int created;
    private List<Long> cityIds;

    public ImportResultDto() {}

    public ImportResultDto(int created, List<Long> cityIds) {
        this.created = created;
        this.cityIds = cityIds;
    }

    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }

    public List<Long> getCityIds() { return cityIds; }
    public void setCityIds(List<Long> cityIds) { this.cityIds = cityIds; }
}
