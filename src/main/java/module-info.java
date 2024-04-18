module arpeggiatorum {
    requires java.desktop;
    requires java.logging;

    requires jsyn;
    requires TarsosDSP.core;

    //requires meico;
    //requires java.string.similarity;
    //requires jcip.annotations;

    requires org.controlsfx.controls;

    requires javafx.fxml;
    //requires javafx.base;
    //requires javafx.controls;
    //requires javafx.swing;
    //requires javafx.graphics;

    opens arpeggiatorum.gui to javafx.graphics, javafx.fxml;
    exports arpeggiatorum.gui to javafx.graphics, javafx.fxml;
}
