# Arpeggiatorum

![Java compatibility 21+](https://img.shields.io/badge/java-21%2B-blue.svg)

Authors: [Axel Berndt](https://github.com/axelberndt) ([Paderborn University](https://www.muwi-detmold-paderborn.de/personen/professorinnen-und-professoren/prof-dr-ing-axel-berndt), Detmold), [Davide Andrea Mauro](https://github.com/murivan) ([Paderborn University](https://kreativ.institute/), Detmold) <br>

A software arpeggiator to drive any MIDI instrument.

Work in Progress!

Use Gradle script "jlink" to produce a usable, self-contained, output.

Windows:
Compile with JavaFX version 16 if you want to have TouchScreen support. Touch support has been bugged since JavaFX 17 and has not been resolved as of 21.0.2
For ASIO support copy the provided dlls in the binary folder.

Mac:
Compile with JavaFX version 21.0.2
