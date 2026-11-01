package com.fundtransfer.support;

public class Customer {

    public final String firstName;
    public final String lastName;
    public final String street;
    public final String city;
    public final String state;
    public final String zipCode;
    public final String phone;
    public final String ssn;
    public final String username;
    public final String password;

    public Customer(String firstName, String lastName, String street, String city, String state,
                    String zipCode, String phone, String ssn, String username, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.phone = phone;
        this.ssn = ssn;
        this.username = username;
        this.password = password;
    }
}
