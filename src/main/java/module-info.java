module arpeggiatorum {
    requires java.desktop;
    requires java.logging;

    requires transitive jsyn;
    requires transitive TarsosDSP.core;

    requires transitive org.controlsfx.controls;

    //requires transitive eu.hansolo.fx.touchslider;
    requires transitive eu.hansolo.regulators;

    // 3rd party
    requires transitive org.kordamp.iconli.core;
    requires transitive org.kordamp.ikonli.javafx;
    requires transitive org.kordamp.ikonli.fontawesome;
    requires transitive org.kordamp.ikonli.material;
    requires transitive org.kordamp.ikonli.materialdesign;
    requires transitive org.kordamp.ikonli.weathericons;

    requires javafx.fxml;
    //requires javafx.base;
    //requires javafx.controls;
    //requires javafx.swing;
    //requires javafx.graphics;

    opens arpeggiatorum.gui to javafx.graphics, javafx.fxml;
    exports arpeggiatorum.gui to javafx.graphics, javafx.fxml;
    exports arpeggiatorum.supplementary to javafx.fxml, javafx.graphics;
    opens arpeggiatorum.supplementary to javafx.fxml, javafx.graphics;
}
