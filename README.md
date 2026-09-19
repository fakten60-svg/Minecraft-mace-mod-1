# Aerial Mace Automation

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.141.6%2B1.21.11-blue)](https://fabricmc.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Client-seitige Fabric-Mod für **Minecraft Java Edition 1.21.11**, die eine automatisierte
Aerial-Mace-Sequenz ausführt, sobald sich der eigene Spieler **ungefähr 3 Blöcke über einem
anderen Spieler** befindet.

> ⚠️ **Fair-Play-Hinweis:** Automatisierte Abläufe verstoßen gegen die Serverregeln vieler
> Multiplayer-Server. Diese Mod ist für Testumgebungen und eigene Server gedacht. Die Nutzung
> erfolgt auf eigene Verantwortung.

---

## Funktionsweise

Sind alle Startbedingungen erfüllt, läuft folgende Sequenz ab:

```
IDLE
 ↓  (Ziel erkannt: Spieler ~3 Blöcke unterhalb, in Reichweite)
TARGET_FOUND
 ↓  Initial Delay:      zufällig 100–120 ms
EQUIP_CHESTPLATE        (Item in der Hand via Vanilla-Inventarklick als Brustplatte ausrüsten)
 ↓  Equip → Mace Delay: zufällig 70–80 ms
SWITCH_TO_MACE          (Hotbar-Slot mit einer Mace, wie ein normaler Hotbar-Wechsel)
 ↓  Mace → Attack Delay: zufällig 67–90 ms
ATTACK                  (normaler Angriff: attackEntity + Swing, wie ein echter Linksklick)
 ↓
IDLE
```

### Startbedingungen

Die Sequenz startet nur, wenn **alle** Bedingungen erfüllt sind:

- Der Client ist in einer Welt (Singleplayer oder Server) verbunden.
- Der eigene Spieler lebt und ist nicht im Zuschauermodus.
- Ein anderer Spieler wurde als Ziel erkannt.
- Die vertikale Differenz `playerY - targetY` liegt im konfigurierbaren Fenster
  (Standard: **2,85 – 3,25 Blöcke**, also „≈ 3 Blöcke“ mit Toleranz).
- Das Ziel liegt innerhalb der konfigurierbaren Reichweite
  (Standard: max. **5,5 Blöcke** gesamt / **4,5 Blöcke** horizontal).
- Es läuft gerade keine Sequenz.

### Abbruchbedingungen

Die State Machine prüft vor **jedem** Übergang die Voraussetzungen neu und bricht sauber ab
(Hotbar-Slot wird zurückgesetzt), wenn:

- das Mod deaktiviert wird (Taste **M**),
- der Spieler stirbt, in den Zuschauermodus wechselt oder die Welt verlässt,
- das Ziel verschwindet, stirbt oder das Höhen-/Distanzfenster verlässt,
- keine Mace in der Hotbar liegt,
- das getragene Handitem keine Brustplatte ist,
- die Sequenz nach 5 Sekunden noch nicht abgeschlossen ist (Timeout).

### „Wie normale Spielereingaben“

Die Mod nutzt bewusst ausschließlich vorhandene Client-Mechanismen:

| Aktion | Mechanismus |
| --- | --- |
| Chestplate ausrüsten | `ClientPlayerInteractionManager.clickSlot(...)` – dieselben Pakete wie ein Shift-Klick im Inventar |
| Mace anwählen | `PlayerInventory.setSelectedSlot(...)` + `UpdateSelectedSlotC2SPacket` – identisch zum Scrollrad |
| Angriff | `interactionManager.attackEntity(...)` + `player.swingHand(...)` – identisch zu einem echten Linksklick |

Keine direkten Server-Zustandsänderungen, keine gepanzerten Interna – nur der reguläre
Interaktionspfad des Clients.

---

## Installation (für Spieler)

1. [Fabric Loader](https://fabricmc.net/use/installer/) für 1.21.11 installieren.
2. [Fabric API](https://modrinth.com/mod/fabric-api) herunterladen und in den `mods`-Ordner legen.
3. Die Mod-JAR aus den [Releases](../../releases) (oder selbst gebaut: `build/libs/`) in den
   `mods`-Ordner legen.
4. Minecraft mit dem Fabric-Profil für 1.21.11 starten.

## Selbst bauen

Voraussetzungen: **JDK 21**

```bash
./gradlew build
```

Die fertige Mod liegt danach unter `build/libs/aerial-mace-automation-<version>.jar`
(die `-sources.jar` ist nur für Entwickler).

## Konfiguration

Die Datei `config/aerialmace.json` wird beim ersten Start automatisch erzeugt:

```json
{
  "enabled": true,
  "requireSneaking": false,
  "targetHeightMin": 2.85,
  "targetHeightMax": 3.25,
  "maxTargetDistance": 5.5,
  "maxHorizontalDistance": 4.5,
  "initialDelayMin": 100,
  "initialDelayMax": 120,
  "equipToMaceDelayMin": 70,
  "equipToMaceDelayMax": 80,
  "maceToAttackDelayMin": 67,
  "maceToAttackDelayMax": 90,
  "overlayMessages": true
}
```

| Schlüssel | Bedeutung |
| --- | --- |
| `enabled` | Master-Schalter (auch in-game per Taste **M** umschaltbar) |
| `requireSneaking` | Sequenz nur starten, während gesneakt wird |
| `targetHeightMin/Max` | Toleranzfenster für `playerY - targetY` (Standard ≈ 3 Blöcke) |
| `maxTargetDistance` | maximale Gesamt-Distanz zum Ziel |
| `maxHorizontalDistance` | maximale horizontale Distanz zum Ziel |
| `initialDelayMin/Max` | Delay vor dem Ausrüsten (ms, inklusive) |
| `equipToMaceDelayMin/Max` | Delay zwischen Ausrüsten und Mace-Wechsel (ms, inklusive) |
| `maceToAttackDelayMin/Max` | Delay zwischen Mace-Wechsel und Angriff (ms, inklusive) |
| `overlayMessages` | Statusmeldungen über der Hotbar anzeigen |

Kaputte oder fehlte Werte werden beim Start automatisch auf zulässige Bereiche korrigiert.

## Steuerung & ClickGUI

| Taste | Aktion |
| --- | --- |
| **Right Shift** (Standard, änderbar) | ClickGUI öffnen/schließen |
| **ESC** | ClickGUI schließen |

### ClickGUI

Die GUI öffnet sich mit **Right Shift** und zeigt vier Kategorien als frei bewegliche
Panels: **Combat, Visuals, Movement, Misc**. Die Module werden automatisch aus dem
ModuleManager geladen.

- **Linksklick** auf ein Modul: ON/OFF (animiert)
- **Rechtsklick** auf ein Modul: Settings ein-/ausklappen
- **Random-Delays** (Initial/Equip/Attack) werden als **Range-Bar mit zwei Handles**
  dargestellt – beide Punkte sind einzeln verschiebbar, Min < Max wird erzwungen, und die
  Combat-Logik übernimmt die Werte sofort.
- **Keybind-Zeile** in jedem Modul: Klick → „Press a key…“, Taste drücken zum Zuweisen,
  **ESC** setzt zurück auf **NONE**. Module haben standardmäßig **keinen** Keybind.
- **Client-Settings-Panel**: GUI-Keybind, GUI-Scale, Animation Speed, Blur, Click Sounds,
  Theme (Dark/Midnight/Mono), Color Picker für Accent/Background/Panel/Text-Farben sowie
  Reset-Aktionen (Module Settings, Keybinds, Theme, GUI Layout).
- Panels lassen sich per Drag & Drop verschieben (Header), scrollen bei Überlauf,
  und Positionen/Einstellungen werden persistent gespeichert (`config/aerialmace-gui.json`).

Die GUI schreibt ausschließlich in die bestehende `config/aerialmace.json` (Modul-Sidebar
`MaceSwitch` ↔ Combat-Logik) — es gibt keine parallelen GUI-Werte.

## Technik

- **Minecraft:** 1.21.11
- **Fabric Loader:** ≥ 0.19.5
- **Fabric API:** 0.141.6+1.21.11
- **Mappings:** Yarn 1.21.11+build.6
- **Java:** 21
- **Loom:** 1.17.21

### Architektur

```
de.aerialmace
├── AerialMaceClient          # Entrypoint: Config, Keybinding, Tick-Hook
├── config.ModConfig          # JSON-Config (config/aerialmace.json)
├── sequence.SequenceStateMachine  # Nicht-blockierende State Machine (Client-Tick)
├── target.TargetSelector     # Zielerkennung + Re-Validierung
└── util
    ├── Delays                # randomDelay(min, max) – inklusive, nicht-blockierend
    └── InventoryUtils        # Vanilla-Inventarklicks, Mace-Suche, Hotbar-Wechsel
```

Die Sequenz ist vollständig **nicht-blockierend** implementiert: Delays werden als Deadline
gespeichert und im Client-Tick geprüft – es gibt nirgends `Thread.sleep()`. Aktionen laufen im
selben Tick, in dem die Delay-Deadline abläuft, damit das konfigurierte Timing exakt bleibt.

## Lizenz

[MIT](LICENSE)
