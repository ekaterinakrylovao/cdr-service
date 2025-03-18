package com.example.cdrservice.entity;

import jakarta.persistence.*;

/**
 * Сущность, представляющая абонента.
 * Хранит номер абонента (MSISDN).
 */
@Entity
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String msisdn;

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }
}
