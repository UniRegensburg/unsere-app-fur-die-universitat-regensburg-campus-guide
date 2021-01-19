### Informationen zu den GitHooks

Normalerweise befinden sich githooks im lokalen .git-Ordner des geklonten Repositories. Da dieser aber nicht mit in die Versionsverwaltung gehört, musste ein eigener Ordner für die Hooks angelegt werden. Damit Git die Hooks trotzdem finden kann, **muss die lokale Git-Configuration angepasst werden**, indem der neue Pfad zu den githooks gesetzt wird. Dafür muss **im Wurzel-Verzeichnis** des Repositories der Befehl 
```sh
git config --local core.hookspath githooks
```
ausgeführt werden. Danach werden alle hier (korrekt benannten) Hooks beim Commit ausgeführt (egal ob über Kommandozeile, Android Studio oder eine Git GUI wie Github Desktop).

### Sonstiges
Github Desktop scheint manchmal Probleme mit den Githooks zu haben, vermutlich wäre es besser für Commits die Kommandozeile oder die IDE (z.B. Android Studio) zu verwenden. Für Desktop muss in Windows 10 auf jeden Fall die JAVA_HOME gesetzt sein.

