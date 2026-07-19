---IN PROGRESS -> NIE WSZYSTKIE FUNKCJE ZOSTAŁY ZAIMPLEMENTOWANE---

GymProgressTracker

GymProgressTracker to zaawansowana aplikacja do precyzyjnego śledzenia postępów treningowych. Została zaprojektowana z myślą o osobach, które chcą świadomie zarządzać swoim planem, kontrolować progresję z tygodnia na tydzień i błyskawicznie reagować na potencjalną stagnację.


🚀 Główne funkcjonalności
Zarządzanie użytkownikami: Każdy użytkownik posiada własne konto (email + hasło), do którego przypisany jest jeden aktualny plan treningowy oraz nielimitowana historia sesji treningowych.

Inteligentny system planów: Aplikacja automatycznie przypomina o konieczności zmiany planu treningowego (zalecane co 2-4 miesiące). Drobne korekty (np. zmiana liczby serii) posiadają opcję "nie aktualizuj daty", zapobiegając niepotrzebnemu resetowaniu licznika czasu trwania planu.

Elastyczna struktura tygodniowa: Możliwość zaplanowania od 1 do 7 jednostek treningowych w tygodniu, w pełni dostosowanych do harmonogramu użytkownika.

Wielowymiarowe parametry ćwiczeń: Wsparcie dla różnych stylów treningu poprzez możliwość wyboru parametru docelowego: Powtórzenia (np. wyciskanie sztangi), Czas (np. deska), Dystans (np. bieganie, pchanie sań) lub Kalorie (sesje cardio).

Szczegółowe metryki i RIR: System pozwala na notowanie ciężaru, czasu przerwy (Rest Time) oraz RIR (Reps in Reserve – zapas powtórzeń). Pola te są opcjonalne (nullable), co pozwala na elastyczność w dni o słabszej dyspozycji.


🏋️‍♂️ Realizacja treningu i progresja
Intuicyjny interfejs treningowy: Wyraźny podział na sekcje tygodniowe oraz czytelne, wizualne odróżnienie treningów ukończonych od zaplanowanych, co znacznie poprawia doświadczenie użytkownika (UX).

Trening w locie: Podczas realizacji jednostki treningowej użytkownik ma dostęp do interaktywnego formularza, w którym na bieżąco uzupełnia wyniki dla poszczególnych ćwiczeń i serii.

Kontekst historyczny: Rozpoczynając trening z co najmniej drugiego tygodnia planu, aplikacja wyświetla przy każdym ćwiczeniu dokładne wyniki z poprzedniego tygodnia.

Wykrywanie stagnacji: Precyzyjne śledzenie danych (np. ten sam ciężar i liczba powtórzeń, ale skrócony czas przerwy) pozwala na dostrzeżenie mikro-progresu lub szybkiej identyfikacji stagnacji, bez tracenia tygodni na analizy.


🛠 Technologie
Projekt został stworzony przy użyciu następujących technologii i narzędzi:

Backend: Java 17, Spring Boot, Spring Web, Spring Security, Spring Data JPA (Hibernate)

Szablony (Frontend): Thymeleaf

Frontend: HTML5, CSS3, JavaScript (Vanilla)

Baza danych: MariaDB (zarządzana przez XAMPP)

Narzędzia: Maven, Lombok, IntelliJ IDEA


⚙️ Jak uruchomić projekt lokalnie
Aby uruchomić aplikację w środowisku lokalnym, wykonaj poniższe kroki:

1. Wymagania wstępne
Java 17+ (lub nowsza wersja)

Maven (opcjonalnie, projekt posiada Maven Wrapper)

XAMPP (do obsługi bazy danych MariaDB)

IDE (opcjonalnie, np. IntelliJ IDEA)

2. Pobranie projektu
Sklonuj repozytorium do wybranego folderu:

Bash
git clone https://github.com/bSek316303/GymTracker.git
cd GymProgressTracker
3. Konfiguracja bazy danych (MariaDB)
Uruchom XAMPP Control Panel.

Włącz usługi Apache oraz MySQL.

Kliknij przycisk Admin przy usłudze MySQL – otworzy się przeglądarka z phpMyAdmin.

Utwórz nową bazę danych o nazwie gymtracker:

Kliknij zakładkę "New" (Nowa).

Wpisz gymtracker w polu nazwy bazy danych i kliknij "Create".

Zaimportuj strukturę tabel:

Wybierz stworzoną bazę gymtracker.

Wejdź w zakładkę SQL.

Przekopiuj zawartość db.sql.txt w miejsce na zapytania SQL.

4. Uruchomienie aplikacji
Opcja A: Przez IntelliJ IDEA (zalecane dla programistów)
Otwórz projekt w IntelliJ (File -> Open -> wybierz folder projektu).

Poczekaj, aż Maven pobierze wszystkie zależności (zobaczysz pasek postępu w prawym dolnym rogu).

Otwórz klasę główną aplikacji (z adnotacją @SpringBootApplication).

Kliknij zielony przycisk Run (strzałka obok metody main).

Opcja B: Przez terminal (bez IDE)
W głównym folderze projektu użyj wbudowanego narzędzia Maven Wrapper:

Windows:

Bash
mvnw.cmd spring-boot:run

Linux / macOS:

Bash
./mvnw spring-boot:run

5. Dostęp do aplikacji
Po poprawnym uruchomieniu, aplikacja będzie dostępna pod adresem:
http://localhost:8080
