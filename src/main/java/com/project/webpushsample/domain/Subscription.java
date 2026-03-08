package com.project.webpushsample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Subscription {
    @Id @GeneratedValue private Long id;
    private String endpoint;
    private Long customerId;
    private String p256dh;
    private String auth;

    protected Subscription() {}

    public Subscription(String endpoint, String p256dh, String auth, Long customerId) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public String getAuth() {
        return auth;
    }
}
