# Hinweise zum Mapbox Android SDK:

Für das Laden und Anzeigen einer Karte in der Android - App sowie für die Navigation werden zwei API-Token benötigt: ein öffentliches Access Token (beginnt immer mit "pk.") und ein geheimes Token (beginnt immer mit "sh.") mit dem zusätzlichen "Downloads:Read"-Zugrifssrecht (vgl. [Installations-Anweisung in der Dokumentation](https://docs.mapbox.com/android/maps/guides/install/)). Letzteres ist speziell für die Navigation nötig. Das Anzeigen der Karte funktioniert bereits mit dem öffentlichen API-Key.
Um die Tokens zu erzeugen, wird ein (kostenloser) Mapbox-Account benötigt ([hier](https://account.mapbox.com/)).
Damit diese API-Schlüssel nicht mit in das Github Repository gelangen, können diese beispielsweise in der gradle.properties - Datei im Gradle-Ordner des Nutzerverzeichnisses hinterlegt und beim Gradle-Build-Prozess mit eingelesen werden. Eine genauere Erklärung hierzu sowie eine Alternative finden sich [hier](https://docs.mapbox.com/help/troubleshooting/private-access-token-android-and-ios/). Die entsprechende gradle.properties-Datei, in der die Keys hinterlegt werden können, befindet sich normalerweise im Ordner ```<USER_HOME>/.gradle/```. Wenn die Datei noch nicht vorhanden ist, kann einfach eine neue gradle.properties - Datei im ```.gradle``` - Ordner angelegt werden.
Das benötigte Format, damit die Keys richtig eingelesen werden können, sieht folgendermaßen aus:

```
MAPBOX_DOWNLOADS_TOKEN=hier_eigenes_Secret_Token_einfügen
MAPBOX_ACCESS_TOKEN=hier_eigenes_Public_Token_einfügen
```

Sobald die eigenen Token auf diese Weise in der Datei hinterlegt wurden, sollte die Karte in der App angezeigt werden und eine Navigation möglich sein.
