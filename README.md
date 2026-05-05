# PasteBin_V2
Program podobny do pastebin. Zamiast wklejać tekst, będziemy wspierać dowolnego typu pliki zorganizowane w foldery. Użytkownik ma możliwość wrzucenia na naszą stronę kilku plików lub folderów, a następnie uzyskania krótkiego linku

# Backend
1. Kontrakt znajduje się w `backend/src/main/java/com/pdwww/pasteit/resources/statis/openapi.yaml`. Jest to opis każdego endpointu, udostępnionego przez backend.
2. Żeby uruchomić aplikację, należy uruchomić kontener dockera - Wpisz `docker compose up --build`.
Uruchomi się serwis, który jest dostępny pod adresem `localhost:8080`. Możesz też zobaczyć ładne UI z endpointami: `localhost:8080/scalar.html`.
3. Kiedy będziesz pobierał nową wersję backendu - musisz usunać stare dane, ponieważ prawdopodobnie nie będą kompatybilne z nową wersją. Wpisz `docker compose down -v`. Potem możesz uruchomić jak w 2.
4. Docker kontener jest bardzo bezpieczny - możesz nie przejmować się, że serwis zniszczy jakieś twoje pliki lub zrobi coś złego.
