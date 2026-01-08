package kingazm.app;

public class Manual {
    private static void printGameManual() {
        System.out.println("\n" +
            "╔════════════════════════════════════════════════════════════════════════════════╗\n" +
            "║                         PODRĘCZNIK GRY - OKRĘTY WOJENNE                        ║\n" +
            "╚════════════════════════════════════════════════════════════════════════════════╝\n\n" +

            "█ CEL GRY:\n" +
            "  Zatopić wszystkie statki przeciwnika poprzez celne strzały zanim on zatopi\n" +
            "  Twoje statki. Gra jest przeznaczona dla dwóch graczy.\n\n" +

            "█ ZASADY PODSTAWOWE:\n" +
            "  - Każdy gracz otrzymuje losową planszę 10x10 ze statkami\n" +
            "  - Statki mają różne długości (od 1 do 4 pól)\n" +
            "  - Gracze wykonują ruchy na zmianę\n" +
            "  - Celem jest zestrzelenie wszystkich statków przeciwnika\n" +
            "  - Gracz może oddać strzał na turę\n\n" +

            "█ ROZPOCZĘCIE GRY:\n" +
            "  1. Uruchom serwer na porcie (domyślnie na porcie 12345)\n" +
            "  2. Podłącz dwóch klientów:\n" +
            "     $ java kingazm.net.Client -host localhost -port 12345\n" +
            "  3. Serwer automatycznie sparuje graczy, gdy dojdzie drugi\n" +

            " SPOSÓB GRY:\n" +
            "  - Wpisz współrzędne w formacie: A1, B5, J10 itd.\n" +
            "  - Litera reprezentuje kolumnę (A-J), cyfra reprezentuje rząd (1-10)\n" +
            "  - Przykład: A1, C7, J10\n\n" +

            " SYMBOLE NA PLANSZY:\n" +
            "  # - Twój cały statek\n" +
            "  @ - Trafiony statek\n" +
            "  ~ - Pudło\n" +
            "  ? - Nieznana pole przeciwnika\n\n" +

            "█ KOMENDY:\n" +
            "  - Współrzędne (np. A1)  - Oddaj strzał na współrzędnych\n" +
            "  - q                     - Zamknij grę i rozłącz\n\n" +

            "█ BŁĘDY I KOMUNIKATY:\n" +
            "  - \"Nie twoja tura\"       - Czekaj na swoją turę\n" +
            "  - \"Nieprawidłowe współrzędne\" - Złe formatowanie (spróbuj A1-J10)\n" +
            "  - \"To pole już zostało zestrzelone\" - Już strzelałeś na to pole\n" +
            "  - \"Błąd komunikacji\"     - Utrata połączenia, gra zostaje przerwana\n\n" +

            "█ OPCJE SERWERA:\n" +
            "  - -port <numer>  - Zmień port (domyślnie 12345)\n" +
            "  - Przykład: java Server -port 9999\n\n" +
            "█ OPCJE KLIENTA:\n" +
            "  - -host <adres>      - Adres serwera (domyślnie localhost)\n" +
            "  - -port <numer>      - Port serwera (domyślnie 12345)\n" +
            "  - -retries <liczba>  - Ilość prób połączenia (domyślnie 5)\n" +
            "  - -delay <ms>        - Opóźnienie między próbami (domyślnie 1000ms)\n\n" +
            
            "\n\n");
    }

    public static void main(String[] args){
        printGameManual();
    }
}