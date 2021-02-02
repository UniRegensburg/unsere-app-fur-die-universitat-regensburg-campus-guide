# Hinweise zum Einsatz von Detekt
**TL;DR: lokal (am besten vor jedem Commit) auf der Kommandozeile im Projekt-Verzeichnis die Befehle "./gradlew detektAutoFormat" und danach "./gradlew detekt" ausführen** (Hinweis: in Windows nur "gradlew" statt "./gradlew")


Detekt ist (neben [Ktlint](https://github.com/pinterest/ktlint)) eines der beiden beliebtesten Tools zur statischen Code-Analyse für Kotlin-Code, d.h. dieser wird von Detekt auf Verstößte (Format und Style) gegen offizielle [Kotlin-Coding-Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) und den offiziellen [Styleguide](https://developer.android.com/kotlin/style-guide) überprüft.
Mit den CLI - und Formatting - Dependencies in der app/build.gradle ist Detekt zudem in der Lage einfache Formatierungsverstöße automatisch zu beheben (dazu wird intern Ktlint benutzt). Komplexere Fehler (wie z.B. MagicNumbers) müssen manuell korrigiert werden.

## Was mache ich jetzt damit??
Durch das Gradle-Plugin wurden einige Gradle-Tasks hinzugefügt. Diese können auf der Kommandozeile oder direkt in Android Studio im Terminal ausgeführt werden mithilfe des Gradle-Wrappers: gradlew *Task*
Besonders nützlich sind dabei die folgenden Tasks:
* **detektGenerateConfig** - erzeugt eine detekt.yml - Konfigurationsdatei unter ./config/detekt/, in der alle Standard-Regeln enthalten sind (sollte als Erstes ausgeführt werden; hier schon geschehen!)
* **detekt** - führt Detekt aus und prüft die Einhaltung der Regeln in der Konfigurationsdatei; generiert zusätzlich zu den Kommandozeilen-Output im Ordner ./app/build/reports/detekt/ noch eine txt, xml und html - Version der gefundenen Verstöße. Gerade die html-Version ist deutlich übersichtlicher und sehr hilfreich.

Dazu kommt noch ein eigener Task (in der app/build.gradle definiert, dort befinden sich im Übrigen auch die Konfigurationen für den normalen Detekt-Task):
* **detektAutoFormat** - führt ebenfalls Detekt aus, versucht dabei aber auch gleich Formatierungsverstöße automatisch zu verbessern, wo möglich

## Android Studio - Plugin
Es existiert auch ein offizielles [Plugin](https://plugins.jetbrains.com/plugin/10761-detekt) für Android Studio,
das unter Settings > Plugins > Marketplace installiert werden kann. Das Plugin muss, nachdem es installiert wurde,
**zusätzlich noch unter Settings >  Tools > detekt aktiviert werden**, dort kann auch der Pfad zur lokalen
detekt.yml-Confi-Datei gesetzt werden. Das Plugin fügt außerdem in der Toolbar oben unter "Refactor" noch den sehr
hilfreichen **Befehl "AutoCorrect by Detekt Rules"** hinzu, mit dem automatisch die gesamte Codebase anhand der aktuellen
Detekt-Regeln formatiert werden kann.

# Der normale Workflow sollte letztendlich in etwa so aussehen:
- (Optional) Während der Entwicklung können Verstöße mit dem Android-Studio - Plugin direkt erkannt und verbessert werden
- Bevor committed wird, werden auf der Kommandozeile im Projekt-Verzeichnis die Befehle "./gradlew detektAutoFormat" und danach "./gradlew detekt" ausgeführt (oder nur gradlew je nach Betriebssystem, s. oben)
    * der erste Befehl versucht Formatierungsfehler automatisch zu beheben, da dieser aber selbst bei erfolgreichem Korrigieren nur die (vorher) gefundenen Fehler zurückgibt, ist der zweite Aufruf nötig, um zu überprüfen, ob es noch Probleme gibt, die manuell behoben werden müssen
- Falls Fehler aufgetreten sind, die nicht automatisch behoben werden konnten, müssen diese manuell verbessert werden, danach kann committed werden.

Zur Sicherheit wird im pre-commit-hook dieser Workflow ebenfalls nochmal durchgeführt, falls vorher etwas vergessen wurde ;)
Code, der committed wurde, enthält also keinerlei Verstöße gegen die Detekt-Regeln mehr.

# Troubleshooting

## Ich habe 12 Methoden in meiner Klasse, detekt erlaubt aber nur 11 ...
Regeln können (falls nötig und sinnvoll), direkt in der detekt.yml - Datei angepasst werden (vielleicht ist die 12. Methode aber auch gar nicht nötig).

## java.lang.IndexOutOfBoundsException: Wrong offset: ... Should be in range: ...
Der Fehler tritt hin und wieder bei der AutoFormatierung an manchen Stellen auf (auch im Issue-Tracker des Github-Repos, leider scheint keinem so wirklich klar zu sein, woran das liegt...) und kann meiner Erfahrung nach nur durch manuelles Korrigieren der entsprechenden Stelle behoben werden.

##### Beispiel:
Fehler tritt auf:

```Kotlin
override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
): View? {
    ...
}
```

Fehler tritt nicht mehr auf:
```Kotlin
override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View? {
    ...
}
```

Fehler tritt ebenfalls nicht mehr auf (aber vermutlich Verstoß, weil Zeile zu lang):
```Kotlin
override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    ...
}
```
