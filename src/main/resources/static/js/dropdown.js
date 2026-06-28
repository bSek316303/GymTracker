document.addEventListener("DOMContentLoaded", () => {
    const dropdown = document.querySelector(".custom-dropdown");
    if (!dropdown) return; // Zabezpieczenie przed błędami, jeśli plik ładuje się na podstronie bez dropdownu

    const trigger = document.getElementById("dropdownTrigger");
    const title = dropdown.querySelector(".dropdown-title");
    const hiddenInput = document.getElementById("selected-exercise");
    const tiles = dropdown.querySelectorAll(".exercise-tile");
    const mainHeader = document.querySelector(".exercise-name");

    // Otwieranie / Zamykanie panelu
    trigger.addEventListener("click", () => {
        dropdown.classList.toggle("active");
    });

    // Obsługa kafelków
    tiles.forEach(tile => {
        tile.addEventListener("click", () => {
            tiles.forEach(t => t.classList.remove("is-selected")); // używamy klasy is-selected z components.css
            tile.classList.add("is-selected");
            
            const exerciseValue = tile.getAttribute("data-value");
            const exerciseName = tile.querySelector(".tile-name").textContent;
            
            hiddenInput.value = exerciseValue;
            title.textContent = exerciseName;
            title.style.color = "#ccff00"; 
            
            dropdown.classList.remove("active");

            if (mainHeader) {
                mainHeader.textContent = exerciseName;
            }
        });
    });

    // Zamknięcie po kliknięciu obok
    document.addEventListener("click", (e) => {
        if (!dropdown.contains(e.target)) {
            dropdown.classList.remove("active");
        }
    });
});