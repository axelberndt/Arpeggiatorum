package arpeggiatorum.gui;

import arpeggiatorum.Arpeggiator;
import arpeggiatorum.Arpeggiatorum;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


public class ArpeggiatorumGUI extends Application {
    public static ArpeggiatorumController controllerHandle;
    private static FXMLLoader fxmlLoader;
    private static Scene scene;
    private static Arpeggiatorum arpeggiatorum;
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

    public static void main(String[] args) {
        Application.launch(args);
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
        //IntelliJ fails to debug fullscreen apps (crashes when switching to IDE)
        //primaryStage.setFullScreen(true);
        primaryStage.setMaximized(true);

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
}