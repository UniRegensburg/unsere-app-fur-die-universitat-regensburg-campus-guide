### Informationen zu den GitHooks

Normalerweise befinden sich githooks im lokalen .git-Ordner des geklonten Repositories. Da dieser aber nicht mit in die Versionsverwaltung gehört, musste ein eigener Ordner für die Hooks angelegt werden. Damit Git die Hooks trotzdem finden kann, **muss die lokale Git-Configuration angepasst werden**, indem der neue Pfad zu den githooks gesetzt wird. Dafür muss **im Wurzel-Verzeichnis** des Repositories der Befehl 
```sh
git config --local core.hookspath githooks
```
ausgeführt werden. Danach werden alle hier (korrekt benannten) Hooks beim Commit ausgeführt (egal ob über Kommandozeile, Android Studio oder eine Git GUI wie Github Desktop).

### Sonstiges
Für Github Desktop muss in Windows 10 die JAVA_HOME - Umgebungsvariable gesetzt sein.

