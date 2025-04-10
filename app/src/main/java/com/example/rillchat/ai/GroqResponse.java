package com.example.rillchat.ai;

import java.util.List;

public class GroqResponse {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;

    public static class Choice {
        public Message message;
        public int index;
        public Object logprobs;
        public String finish_reason;
    }

    public static class Message {
        public String role;
        public String content;
    }
}
