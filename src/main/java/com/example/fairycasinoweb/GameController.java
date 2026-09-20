package com.example.fairycasinoweb;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {

    private static final String COOKIE_NAME = "playerId";

    @Autowired
    private GameService gameService;

    // ============ Получить состояние игрока ============

    @GetMapping("/state")
    public Map<String, Object> getState(HttpServletRequest request, HttpServletResponse response) {
        Player player = getOrCreatePlayer(request, response);
        return playerToMap(player);
    }

    // ============ Крутить ============

    @PostMapping("/spin")
    public Map<String, Object> spin(HttpServletRequest request, HttpServletResponse response) {
        Player player = getOrCreatePlayer(request, response);
        GameService.SpinResult result = gameService.spin(player);

        Map<String, Object> map = playerToMap(player);
        map.put("success", result.success);
        map.put("message", result.message);
        map.put("fairyIndex", result.fairyIndex);
        map.put("prize", result.prize);
        map.put("duplicate", result.duplicate);
        return map;
    }

    // ============ Купить билет ============

    @PostMapping("/buy")
    public Map<String, Object> buy(HttpServletRequest request, HttpServletResponse response) {
        Player player = getOrCreatePlayer(request, response);
        boolean success = gameService.buyTicket(player);

        Map<String, Object> map = playerToMap(player);
        map.put("success", success);
        if (!success) {
            map.put("message", "Не хватает искр или уже максимум билетов");
        } else {
            map.put("message", "OK");
        }
        return map;
    }

    // ============ Вспомогательные ============

    private Player getOrCreatePlayer(HttpServletRequest request, HttpServletResponse response) {
        String playerId = null;

        // Ищем cookie с ID игрока
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    playerId = cookie.getValue();
                    break;
                }
            }
        }

        // Создаём нового игрока, если cookie нет
        boolean isNew = (playerId == null || playerId.isBlank());
        Player player = gameService.getOrCreatePlayer(playerId);

        if (isNew) {
            Cookie cookie = new Cookie(COOKIE_NAME, player.id);
            cookie.setMaxAge(60 * 60 * 24 * 365); // 1 год
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        return player;
    }

    private Map<String, Object> playerToMap(Player player) {
        Map<String, Object> map = new HashMap<>();
        map.put("tickets", player.tickets);
        map.put("sparks", player.sparks);
        map.put("fairyCounts", player.fairyCounts);
        map.put("history", player.history);
        return map;
    }
}