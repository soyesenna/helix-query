package com.soyesenna.helixquery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Test Embeddable record for Address.
 */
@Embeddable
public record Address(
    @Column(name = "street")
    String street,

    @Column(name = "city")
    String city,

    @Column(name = "zip_code")
    String zipCode,

    @Column(name = "country")
    String country
) {}
