# ExplURe

## Überblick
*ExplURe* ist eine mobile Android-Anwendung zur Erstellung und Nutzung interaktiver
Entdeckungstouren am Campus der Universität Regensburg. NutzerInnen können über die App Routen aus POIs erstellen, die durch Text-, Bild-, Audio- oder Videoinformationen beschrieben werden. Routen können durch Begleitinformationen thematisch beschrieben und von anderen NutzerInnen recherchiert und gestartet werden. Dabei führt die Anwendung die NutzerInnen entlang der Route über den Campus und erlaubt das Kommentieren der Strecken bzw. Orte und das Teilen eigener Eindrücke.  

## Screenshots
TODO

## Getting Started
TODO Link zu Google Play Store, wenn veröffentlicht

## Technische Umsetzung
Die App ist in Kotlin geschrieben, als Single-Activity-Architektur aufgebaut und folgt dem [MVVM-Pattern](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) sowie aktuellen [*Best practices*](https://developer.android.com/jetpack/guide) für die Android - Entwicklung.
Zur Darstellung der Karte wird [Mapbox](https://www.mapbox.com/) verwendet.
Für das Backend wird [Google Firebase](https://firebase.google.com/?hl=en) verwendet, sowohl als Datenbank (Cloud Firestore und Cloud Storage) als auch für die Nutzer-Authentifizierung.

Um eine hohe Code-Qualität im Team zu gewährleisten, werden [Detekt](https://detekt.github.io/detekt/) zur statischen Code-Analyse und der Einhaltung offizieller Style Guidelines (auch als lokaler pre-commit-hook implementiert) sowie Unit- und Instrumented-Tests eingesetzt. Detekt sowie die Tests sind ebenfalls als [Github Actions](./.github/workflows) in den Continuous Integration - Workflow integriert.

### Verwendete Komponenten und Bibliotheken:
* [Android Jetpack](https://developer.android.com/jetpack) - Komponenten:
    - ViewBinding
    - Navigation
    - RecyclerView
    - Viewmodel
    - LiveData

##### Third Party - Bibliotheken:
* [Firebase Android SDK](https://firebase.google.com/docs/android/setup?hl=en)
* [Mapbox Android SDK](https://docs.mapbox.com/android/maps/guides/)
* [Detekt](https://github.com/detekt/detekt)
* [Timber](https://github.com/JakeWharton/timber)
* [LeakCanary](https://square.github.io/leakcanary/)
* [Koin](https://insert-koin.io/)

## Hinweise zur Dokumentation

* Allgemeine Regelungen und Guidelines bei der Entwicklung sowie eine Anleitung zum Erstellen eines APK-Releases befinden sich im [Wiki des Repositories](https://github.com/UniRegensburg/unsere-app-fur-die-universitat-regensburg-campus-guide/wiki). Dort ist ebenfalls ein [Template](https://github.com/UniRegensburg/unsere-app-fur-die-universitat-regensburg-campus-guide/wiki/Checkliste---Pull-Request-Review) für die Erstellung einheitlicher Reviews zu finden. 
* Die Verwendung der Git-Hooks ist im [githooks-Unterordner](./githooks) genauer beschrieben.
* Hinweise zur Vewendung von Detekt und Troubleshooting dazu, befinden sich in der [Hinweise_zu_Detekt - Datei](./Hinweise_zu_Detekt.md).
* Hinweise zur Verwendung von Mapbox und den Mapbox API - Keys sind in der [Hinweise_zu_Mapbox - Datei](./Hinweise_zu_Mapbox.md).
