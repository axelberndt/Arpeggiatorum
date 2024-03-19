package arpeggiatorum;

import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


public class ArpeggiatorumGUI extends Application {
    private static Arpeggiatorum arpeggiatorum;
    /**
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ArpeggiatorumGUI.class.getResource("ArpeggiatorumGUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        //GUI Definition
        primaryStage.setTitle("ArpeggiatorumGUI v" + Arpeggiator.version);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setFullScreen(true);

        primaryStage.setOnCloseRequest((event) -> {
            Arpeggiatorum.getInstance().SaveNClose();
        });

        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
            switch (event.getCode()) {
                case KeyCode.Q: {
                    Arpeggiatorum.getInstance().SaveNClose();
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
