package org.itmo.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity @Table(name="city")
public class City {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank private String name;

    @NotNull
    @Column(name = "CLIMATE", columnDefinition = "climate")
    @Convert(converter = ClimatePgEnumConverter.class)
    private Climate climate;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public Climate getClimate(){return climate;} public void setClimate(Climate climate){this.climate=climate;}
}
