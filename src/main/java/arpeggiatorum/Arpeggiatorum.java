package arpeggiatorum;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Arpeggiatorum extends Application {
    /**
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Arpeggiatorum v" + Arpeggiator.version);
        primaryStage.initStyle(StageStyle.UNIFIED);
        primaryStage.setFullScreen(true);

//        VBox vBox = new VBox(new Label("A JavaFX Label"));
//        Scene scene = new Scene(vBox);
//
        primaryStage.setOnCloseRequest((event) -> {
            System.out.println("Closing Stage");
        });

        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
            System.out.println("Key pressed: " + event.toString());

            switch (event.getCode()) {
                case KeyCode.ESCAPE: {
                    primaryStage.close();
                    break;
                }
                default: {
                    System.out.println("Unrecognized key");
                }
            }
        });


//        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
