# Arpeggiatorum

![Java compatibility 21+](https://img.shields.io/badge/java-21%2B-blue.svg)

Authors: [Axel Berndt](https://github.com/axelberndt) ([Paderborn University](https://www.muwi-detmold-paderborn.de/personen/professorinnen-und-professoren/prof-dr-ing-axel-berndt), Detmold), [Davide Andrea Mauro](https://github.com/murivan) ([Paderborn University](https://kreativ.institute/), Detmold) <br>

A software arpeggiator to drive any MIDI instrument. It can be controlled with audio as input.

**Work in Progress!**

Use Gradle to compile and run the project. E.g. if using IntelliJ: gradle run.
Use Gradle script "jlink" to produce a usable, self-contained, output.

**Interface**:

Keyboard shortcuts:
- Esc/Q: Close current window/quit.

- L: Open Log Messages.

- P: Open Performance/Play mode. In Performance mode a small set of touch-control enabled elements are available.


**Windows**:

Compile with JavaFX version 16 if you want to have TouchScreen support. Touch support has been bugged since JavaFX 17 and has not been resolved as of 21.0.3 .

**Mac**:

Compile with JavaFX version 21.0.3
