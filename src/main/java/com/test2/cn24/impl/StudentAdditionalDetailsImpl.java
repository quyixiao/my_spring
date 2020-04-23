package com.test2.cn24.impl;

import com.test2.cn24.StudentAdditionalDetails;

public class StudentAdditionalDetailsImpl implements StudentAdditionalDetails {







    private String city;
    private String country;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void showAdditionalDetails() {
        System.out.println(this.city);
        System.out.println(this.country);
    }
}