package com.example.rillchat.ai;

import java.util.List;

public class GroqRequest {
    public String model = "llama-3.3-70b-versatile";
    public List<Message> messages;
    public double temperature = 0.6;
    public int max_tokens = 1024;
    public double top_p = 0.95;
    public boolean stream = false;

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
