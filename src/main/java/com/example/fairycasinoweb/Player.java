package com.example.fairycasinoweb;

import java.util.ArrayList;
import java.util.List;

public class Player {
    public String id;
    public int tickets;
    public int sparks;
    public int[] fairyCounts;
    public long lastTicketTime;
    public List<SpinRecord> history;

    public Player() {
    }

    public Player(String id) {
        this.id = id;
        this.tickets = 3;
        this.sparks = 0;
        this.fairyCounts = new int[6];
        this.lastTicketTime = System.currentTimeMillis();
        this.history = new ArrayList<>();
    }

    public static class SpinRecord {
        public int fairyIndex;
        public int prize;
        public boolean duplicate;
        public long timestamp;

        public SpinRecord() {}

        public SpinRecord(int fairyIndex, int prize, boolean duplicate) {
            this.fairyIndex = fairyIndex;
            this.prize = prize;
            this.duplicate = duplicate;
            this.timestamp = System.currentTimeMillis();
        }
    }
}