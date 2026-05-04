# PasteBin_V2
Program podobny do pastebin. Zamiast wklejać tekst, będziemy wspierać dowolnego typu pliki zorganizowane w foldery. Użytkownik ma możliwość wrzucenia na naszą stronę kilku plików lub folderów, a następnie uzyskania krótkiego linku

# Backend
1. Kontrakt znajduje się w `backend/src/main/java/com/pdwww/pasteit/resources/statis/openapi.yaml`. Jest to opis każdego endpointu, udostępnionego przez backend.
2. Żeby uruchomić aplikację, należy uruchomić kontener dockera. Wystarczy, że w terminalu wpiszesz `cd backend && docker compose up` (lub odpowiednik na Windowsach). Uruchomi się serwis, który jest dostępny pod adresem `localhost:8080`. Możesz też zobaczyć ładne UI z endpointami: `localhost:8080/scalar.html`. 
3. Docker kontener jest bardzo bezpieczny - możesz nie przejmować się, że serwis zniszczy jakieś twoje pliki lub zrobi coś złego.
