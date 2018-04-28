package com.test.session;

import javax.persistence.*;

/**
 * Created by akhaf on 13.04.2018.
 */
@Entity
@Table(name = "CONTACTS")
public class Message {

    @Id
    @Column(name = "ID")
    private String id;


    @Column(name = "FIRSTNAME")
    private String firstname;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @Override
    public String toString() {
        return id + " " +  firstname;
    }
}
