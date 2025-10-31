package ru.itmo.page;

import java.util.List;

public class CoordinatesPageRequest {

    private Integer page = 0;
    private Integer size = 20;


    private List<String> sort;


    private Long id;
    private Float x;
    private Float y;


    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public List<String> getSort() { return sort; }
    public void setSort(List<String> sort) { this.sort = sort; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Float getX() { return x; }
    public void setX(Float x) { this.x = x; }
    public Float getY() { return y; }
    public void setY(Float y) { this.y = y; }
}