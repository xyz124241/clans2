# NeptunClans

Clan-Plugin für Paper. Befehle: `/clan`, `/clan create|invite|accept|leave|delete`.

## JAR bauen

**Variante A: GitHub (ohne Programme installieren)**
1. Auf github.com ein neues, privates Repository anlegen.
2. Den kompletten Inhalt dieses Ordners hochladen (auch den Ordner `.github`).
3. Im Reiter "Actions" den Lauf "Build" abwarten (ca. 1 Minute).
4. Beim fertigen Lauf unter "Artifacts" `NeptunClans` herunterladen. Darin liegt `NeptunClans.jar`.

**Variante B: lokal**
JDK 21 und Maven installieren, im Ordner `mvn package` ausführen. Die JAR liegt dann in `target/NeptunClans.jar`.

Falls der Build mit "Could not find artifact paper-api" abbricht: in `pom.xml` den Wert `paper.version`
auf eine existierende Paper-Version ändern (Beispiel: `1.21.8-R0.1-SNAPSHOT`).

## Installieren
1. `NeptunClans.jar` in den Ordner `plugins`, Server neu starten.
2. PlaceholderAPI muss installiert sein.
3. In `plugins/TAB/groups.yml` unter `_DEFAULT_:`:
   ```yaml
   tabprefix: "%clan_tag%%luckperms-prefix%"
   tagprefix: "%clan_tag%%luckperms-prefix%"
   ```
   Danach `/tab reload`.
4. Limits pro LuckPerms-Gruppe stehen in `plugins/NeptunClans/config.yml`.

## Platzhalter
`%clan_tag%`, `%clan_name%`, `%clan_rank%`, `%clan_members%`
