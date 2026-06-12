package com.fundflow.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fund")
public class Fund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "vintage_year", nullable = false)
    private int vintageYear;

    @Column(name = "target_size", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetSize;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Fund() {
        // JPA
    }

    public Fund(String name, int vintageYear, BigDecimal targetSize, String currency) {
        this.name = name;
        this.vintageYear = vintageYear;
        this.targetSize = targetSize;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVintageYear() {
        return vintageYear;
    }

    public void setVintageYear(int vintageYear) {
        this.vintageYear = vintageYear;
    }

    public BigDecimal getTargetSize() {
        return targetSize;
    }

    public void setTargetSize(BigDecimal targetSize) {
        this.targetSize = targetSize;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
