package com.example.fairycasinoweb;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final int MAX_TICKETS = 10;
    private static final int TICKET_PRICE = 50;
    private static final int DUPLICATE_BONUS = 10;
    private static final long TICKET_INTERVAL_MS = 5 * 60 * 1000; // 5 минут

    private static final String SAVE_FILE = "players.json";

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // ============ Загрузка / сохранение ============

    @PostConstruct
    public void init() {
        loadPlayers();
    }

    private void loadPlayers() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return;

        try {
            String json = Files.readString(Paths.get(SAVE_FILE));
            // Простейший парсинг: файл представляет собой список игроков, разделённых "###"
            // Каждый игрок — строки: id;tickets;sparks;count0,count1,...;lastTicketTime
            for (String block : json.split("###")) {
                if (block.isBlank()) continue;
                String[] lines = block.trim().split("\n");
                if (lines.length < 5) continue;

                Player p = new Player();
                p.id = lines[0];
                p.tickets = Integer.parseInt(lines[1]);
                p.sparks = Integer.parseInt(lines[2]);

                String[] counts = lines[3].split(",");
                p.fairyCounts = new int[counts.length];
                for (int i = 0; i < counts.length; i++) {
                    p.fairyCounts[i] = Integer.parseInt(counts[i]);
                }
                p.lastTicketTime = Long.parseLong(lines[4]);
                p.history = new ArrayList<>();
                players.put(p.id, p);
            }
            System.out.println("Загружено игроков: " + players.size());
        } catch (Exception e) {
            System.out.println("Ошибка загрузки игроков: " + e.getMessage());
        }
    }

    private void savePlayers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE))) {
            for (Player p : players.values()) {
                writer.write(p.id);
                writer.newLine();
                writer.write(String.valueOf(p.tickets));
                writer.newLine();
                writer.write(String.valueOf(p.sparks));
                writer.newLine();

                StringBuilder counts = new StringBuilder();
                for (int i = 0; i < p.fairyCounts.length; i++) {
                    if (i > 0) counts.append(",");
                    counts.append(p.fairyCounts[i]);
                }
                writer.write(counts.toString());
                writer.newLine();
                writer.write(String.valueOf(p.lastTicketTime));
                writer.newLine();
                writer.write("###");
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сохранения игроков: " + e.getMessage());
        }
    }

    // ============ Игроки ============

    public Player getOrCreatePlayer(String id) {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        Player player = players.computeIfAbsent(id, Player::new);
        accrueTickets(player);
        return player;
    }

    // ============ Билеты ============

    private void accrueTickets(Player player) {
        long now = System.currentTimeMillis();
        long elapsed = now - player.lastTicketTime;
        long intervals = elapsed / TICKET_INTERVAL_MS;

        if (intervals > 0 && player.tickets < MAX_TICKETS) {
            int before = player.tickets;
            player.tickets = Math.min(player.tickets + (int) intervals, MAX_TICKETS);
            player.lastTicketTime = now;
            if (player.tickets > before) {
                savePlayers();
            }
        }
    }

    // ============ Крутка ============

    public SpinResult spin(Player player) {
        accrueTickets(player);

        if (player.tickets <= 0) {
            return new SpinResult(false, "Билетов нет!", -1, 0, false);
        }

        player.tickets--;

        int index = pickFairy();
        boolean isDuplicate = player.fairyCounts[index] > 0;

        int prize = FAIRY_PRIZES[index];
        player.sparks += prize;
        player.fairyCounts[index]++;

        if (isDuplicate) {
            player.sparks += DUPLICATE_BONUS;
        }

        player.history.add(0, new Player.SpinRecord(index, prize, isDuplicate));
        while (player.history.size() > 5) {
            player.history.remove(player.history.size() - 1);
        }

        savePlayers();

        return new SpinResult(true, "OK", index, prize, isDuplicate);
    }

    private int pickFairy() {
        int roll = random.nextInt(100) + 1;
        if (roll <= 35) return 0;       // Лейла
        else if (roll <= 60) return 1;  // Текна
        else if (roll <= 78) return 2;  // Муза
        else if (roll <= 90) return 3;  // Флора
        else if (roll <= 97) return 4;  // Стелла
        else return 5;                  // Блум
    }

    // ============ Магазин ============

    public boolean buyTicket(Player player) {
        accrueTickets(player);

        if (player.tickets >= MAX_TICKETS) return false;
        if (player.sparks < TICKET_PRICE) return false;

        player.sparks -= TICKET_PRICE;
        player.tickets++;
        savePlayers();
        return true;
    }

    // ============ Данные фей ============

    public static final String[] FAIRY_NAMES  = {"Лейла", "Текна", "Муза", "Флора", "Стелла", "Блум"};
    public static final int[]    FAIRY_PRIZES = {5, 15, 20, 30, 50, 100};
    public static final String[] FAIRY_RARITIES = {"common", "common", "uncommon", "uncommon", "rare", "legendary"};

    // ============ Класс результата ============

    public static class SpinResult {
        public boolean success;
        public String message;
        public int fairyIndex;
        public int prize;
        public boolean duplicate;

        public SpinResult(boolean success, String message, int fairyIndex, int prize, boolean duplicate) {
            this.success = success;
            this.message = message;
            this.fairyIndex = fairyIndex;
            this.prize = prize;
            this.duplicate = duplicate;
        }
    }
}