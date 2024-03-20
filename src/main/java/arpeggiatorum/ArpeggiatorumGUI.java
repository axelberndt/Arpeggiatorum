package arpeggiatorum;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


public class ArpeggiatorumGUI extends Application {
    private static FXMLLoader fxmlLoader;
    private static Scene scene;
    private static Arpeggiatorum arpeggiatorum;

    public static ArpeggiatorumController controllerHandle;
    /**
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        fxmlLoader = new FXMLLoader(ArpeggiatorumGUI.class.getResource("ArpeggiatorumGUI.fxml"));
        scene = new Scene(fxmlLoader.load());

        controllerHandle = fxmlLoader.getController();
        //GUI Definition
        primaryStage.setTitle("ArpeggiatorumGUI v" + Arpeggiator.version);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setFullScreen(true);

        primaryStage.setOnCloseRequest((event) -> {
            Arpeggiatorum.getInstance().SaveNClose(this);
        });

        Arpeggiatorum.LoadConfig(this);
        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
            switch (event.getCode()) {
                case KeyCode.Q: {
                    Arpeggiatorum.getInstance().SaveNClose(this);
                    break;
                }
                case KeyCode.L: {
                    //Open Log Menu
                    break;
                }
                default: {
                    //  System.out.println("Unrecognized key");
                }
            }
        });


    }

    public static void main(String[] args) {
        arpeggiatorum=new Arpeggiatorum();
        Application.launch(args);
    }


}
