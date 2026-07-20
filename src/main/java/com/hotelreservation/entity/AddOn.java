package com.hotelreservation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "addons")
public class AddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addon_id")
    private Long addonID;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, length = 50)
    private PricingModel pricingModel;

    public AddOn() {
    }

    public AddOn(String name, double price, PricingModel pricingModel) {
        this.name = name;
        this.price = price;
        this.pricingModel = pricingModel;
    }

    public Long getAddonID() {
        return addonID;
    }

    public void setAddonID(Long addonID) {
        this.addonID = addonID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public PricingModel getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(PricingModel pricingModel) {
        this.pricingModel = pricingModel;
    }
}
