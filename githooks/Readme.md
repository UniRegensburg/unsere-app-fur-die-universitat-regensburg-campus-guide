### Informationen zu den GitHooks

Githooks sind Programme mit festgelegten Namen (z.B. pre-commit oder commit-msg), die sich in den Git-Prozess an der enstrpechenden Stelle einklinken und es im Falle des pre-commit-hooks beispielsweise erlauben, bevor commited wird, bestimmte Aktionen durchzuführen (wie z.B. den Code auf Style-Violations mit Detekt oder Ktlint zu prüfen).
Normalerweise befinden sich githooks im lokalen .git/hooks/ - Ordner des geklonten Repositories (standardmäßig mit der ".sample"-Endung, da nur Platzhalter). Da dieser Ordner aber nicht mit in die Versionsverwaltung gehört, wurde ein eigener Ordner für die Hooks angelegt. Damit Git die Hooks trotzdem finden kann, **muss die Git-Configuration im Repository lokal angepasst werden**, indem der neue Pfad zu den githooks gesetzt wird. Dafür muss **im Wurzel-Verzeichnis** des Repositories der Befehl
```sh
git config --local core.hookspath githooks
```
ausgeführt werden. Danach werden alle hier (korrekt benannten) Hooks beim Commit ausgeführt (egal ob über Kommandozeile, Android Studio oder eine Git GUI wie Github Desktop).
Der Befehl tut nichts anderes als den Wert der hookspath - Variable in der lokalen ./git/config - Datei auf den eigenen Ordner zu ändern. Wenn dieser Schritt rückgängig gemacht werden soll, kann die Zeile *hookspath = githooks* in der config - Datei wieder gelöscht werden.

### Sonstiges
Für Github Desktop muss in Windows 10 die JAVA_HOME - Umgebungsvariable gesetzt sein.

