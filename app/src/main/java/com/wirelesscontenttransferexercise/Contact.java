package com.wirelesscontenttransferexercise;

import org.json.JSONException;
import org.json.JSONObject;

public class Contact {
    public String name;
    public String phoneNumber;
    public String image;


    public Contact(JSONObject jsonObject) {
        this.name = jsonObject.optString("name","");
        this.phoneNumber = jsonObject.optString("phone_number","");
        this.image=jsonObject.optString("image","");
    }

    public Contact(String name, String phoneNumber,String image) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.image=image;
    }

    public String getName() {
        return name;
    }



    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getImage() {
        return image;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", getName());
            obj.put("phone_number", getPhoneNumber());
            obj.put("image",getImage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}