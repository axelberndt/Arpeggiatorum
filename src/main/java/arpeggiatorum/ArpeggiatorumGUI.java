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
    private static volatile ArpeggiatorumGUI instance;

    //Singleton pattern
    public static synchronized ArpeggiatorumGUI getInstance() {
        if (instance == null) {
            synchronized (ArpeggiatorumGUI.class) {
                if (instance == null) {
                    instance = new ArpeggiatorumGUI();
                }
            }
        }
        return instance;
    }

    @Override
    public void init() throws Exception {
        super.init();
        synchronized (ArpeggiatorumGUI.class) {
            if (instance != null) throw new UnsupportedOperationException(
                    getClass() + " is singleton but constructor called more than once");
            instance = this;
        }
    }

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
            Arpeggiatorum.SaveNClose(this);
        });


        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
            switch (event.getCode()) {
                case KeyCode.Q: {
                    Arpeggiatorum.SaveNClose(this);
                    break;
                }
                case KeyCode.L: {
                    //Open Log Menu
                    Arpeggiatorum.LoadLog(this);
                    break;
                }
                default: {
                    LogGUIController.logBuffer.append("Unrecognized key.");
                }
            }
        });

        Arpeggiatorum.LoadConfig(this);
    }

    public static void main(String[] args) {
        arpeggiatorum = new Arpeggiatorum();
        Application.launch(args);
    }
}
