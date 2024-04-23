module arpeggiatorum {
    requires java.desktop;
    requires java.logging;

    requires jsyn;
    requires TarsosDSP.core;

    requires org.controlsfx.controls;

    requires eu.hansolo.fx.touchslider;
    requires eu.hansolo.regulators;

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
}
