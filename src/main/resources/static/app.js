const fairies = [
    { name: "Лейла",  prize: 5,   emoji: "💧", color: "#ff9ec7" },
    { name: "Текна",  prize: 15,  emoji: "🔧", color: "#6ee7ff" },
    { name: "Муза",   prize: 20,  emoji: "🎵", color: "#a78bfa" },
    { name: "Флора",  prize: 30,  emoji: "🌸", color: "#86efac" },
    { name: "Стелла", prize: 50,  emoji: "☀️", color: "#fde047" },
    { name: "Блум",   prize: 100, emoji: "🔥", color: "#f87171" }
];
const chances = [35, 25, 18, 12, 7, 3];

const MAX_TICKETS = 10;
const TICKET_INTERVAL_MS = 5 * 60 * 1000; // 5 минут

let tickets = 3;
let sparks = 0;
let lastTicketTime = Date.now();

// Загружаем сохранение
const saved = localStorage.getItem("fairyCasino");
if (saved) {
    try {
        const data = JSON.parse(saved);
        tickets = data.tickets;
        sparks = data.sparks;
        if (data.lastTicketTime) {
            lastTicketTime = data.lastTicketTime;
        }
    } catch (e) {
        console.log("Не удалось загрузить сохранение");
    }
}

// Начисляем билеты за прошедшее время (пока страница была закрыта)
function accrueTickets() {
    const now = Date.now();
    const elapsed = now - lastTicketTime;
    const earned = Math.floor(elapsed / TICKET_INTERVAL_MS);

    if (earned > 0 && tickets < MAX_TICKETS) {
        const before = tickets;
        tickets = Math.min(tickets + earned, MAX_TICKETS);
        if (tickets > before) {
            lastTicketTime = now;
            updateStats();
            save();
        }
    }
}

function save() {
    localStorage.setItem("fairyCasino", JSON.stringify({
        tickets: tickets,
        sparks: sparks,
        lastTicketTime: lastTicketTime
    }));
}

function updateStats() {
    document.getElementById("stats").textContent =
        "Билетов: " + tickets + " | Искр: " + sparks;
}

function pickFairy() {
    const roll = Math.random() * 100;
    let sum = 0;
    for (let i = 0; i < fairies.length; i++) {
        sum += chances[i];
        if (roll < sum) return i;
    }
    return fairies.length - 1;
}

function spin() {
    const button = document.getElementById("spin-btn");

    if (tickets <= 0) {
        document.getElementById("result-name").textContent = "Билетов нет!";
        document.getElementById("result-prize").textContent =
            "Подожди 5 минут — накопится новый";
        return;
    }

    button.disabled = true;

    const circle = document.getElementById("fairy-circle");
    circle.textContent = "🎰";
    circle.style.background = "rgba(255, 255, 255, 0.3)";
    document.getElementById("result-name").textContent = "Крутим...";
    document.getElementById("result-prize").textContent = "";

    circle.classList.add("spinning");

    setTimeout(function() {
        circle.classList.remove("spinning");

        tickets--;
        const index = pickFairy();
        const fairy = fairies[index];
        sparks += fairy.prize;

        // Если билетов не было давно — обновим время отсчёта
        if (tickets === 0) {
            lastTicketTime = Date.now();
        }

        circle.textContent = fairy.emoji;
        circle.style.background = fairy.color;

        document.getElementById("result-name").textContent = "Выпала: " + fairy.name + "!";
        document.getElementById("result-prize").textContent = "+" + fairy.prize + " искр";

        updateStats();
        save();
        button.disabled = false;
    }, 500);
}

// Начисляем билеты за прошедшее время при загрузке
accrueTickets();
updateStats();

// И потом каждые 5 секунд проверяем — не пора ли выдать новый билет
setInterval(function() {
    accrueTickets();
}, 5000);