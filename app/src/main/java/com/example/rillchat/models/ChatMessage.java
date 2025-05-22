package com.example.rillchat.models;

import java.util.Date;


public class ChatMessage {
    public String senderId, receiverId, message, dateTime;
    public Date dateObject;
    public String conversionId, conversionName, conversionImage;
    public String caption;

    private boolean isFromAI = false;

    public boolean isFromAI() {
        return isFromAI;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
        this.isFromAI = "AI_BOT".equals(senderId); // auto-detect AI
    }
    public void setFromAI(boolean fromAI) {
        isFromAI = fromAI;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}