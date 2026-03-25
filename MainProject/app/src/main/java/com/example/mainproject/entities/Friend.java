package com.example.mainproject.entities;

public class Friend {
    public String uid;
    public String name;
    public String status;

    public Friend(){}

    public Friend(String userID, String name, String status){
        this.uid = userID;
        this.name = name;
        this.status = status;
    }
}
