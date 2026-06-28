let milliseconds = 0;
let timer = null;

// Pobranie elementów z HTML
const stopwatchDisplay = document.getElementById('stopwatch-display');
const startButton = document.getElementById('start-button');
const pauseButton = document.getElementById('pause-button');
const resetButton = document.getElementById('reset-button');

// Funkcja zmieniająca cyfry na format 00:00.00 (Minuty:Sekundy.Setne)
function formatTime() {
    let minutes = Math.floor(milliseconds / 60000);
    let seconds = Math.floor((milliseconds % 60000) / 1000);
    let hundredths = Math.floor((milliseconds % 1000) / 10);

    // Dodawanie zera z przodu, jeśli liczba jest jednocyfrowa
    let minutesStr = minutes < 10 ? '0' + minutes : minutes;
    let secondsStr = seconds < 10 ? '0' + seconds : seconds;
    let hundredthsStr = hundredths < 10 ? '0' + hundredths : hundredths;

    stopwatchDisplay.innerText = `${minutesStr}:${secondsStr}.${hundredthsStr}`;
}

// Obsługa przycisku START
startButton.addEventListener('click', () => {
    if (timer !== null) return; // Zabezpieczenie przed podwójnym odpaleniem

    let startTime = Date.now() - milliseconds;

    timer = setInterval(() => {
        milliseconds = Date.now() - startTime;
        formatTime();
    }, 10); // Aktualizacja co 10 milisekund

    // Zarządzanie stanem przycisków
    startButton.disabled = true;
    pauseButton.disabled = false;
    resetButton.disabled = false;
});

// Obsługa przycisku PAUZA
pauseButton.addEventListener('click', () => {
    clearInterval(timer);
    timer = null;

    startButton.disabled = false;
    pauseButton.disabled = true;
});

// Obsługa przycisku RESET
resetButton.addEventListener('click', () => {
    clearInterval(timer);
    timer = null;
    milliseconds = 0;
    formatTime();

    startButton.disabled = false;
    pauseButton.disabled = true;
    resetButton.disabled = true;
});