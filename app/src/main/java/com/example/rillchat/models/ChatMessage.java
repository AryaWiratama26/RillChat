package com.example.rillchat.models;

import java.util.Date;


public class ChatMessage {
    public String senderId, receiverId, message, dateTime;
    public Date dateObject;
    public String conversionId, conversionName, conversionImage;

    private boolean isFromAI = false;

    public boolean isFromAI() {
        return isFromAI;
    }
    public void setFromAI(boolean fromAI) {
        isFromAI = fromAI;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
    
}