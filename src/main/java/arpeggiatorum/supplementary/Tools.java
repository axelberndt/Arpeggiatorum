package arpeggiatorum.supplementary;

import arpeggiatorum.gui.ArpeggiatorumGUI;
import arpeggiatorum.gui.LogGUIController;
import com.jsyn.data.Spectrum;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Tools {
    /**
     * compute the energy density distribution of a complex spectrum
     *
     * @param spectrum
     * @return
     */
    public static double[] computeEnergyDensity(Spectrum spectrum) {
        double[] realPart = spectrum.getReal();
        double[] imaginaryPart = spectrum.getImaginary();
        int numBins = Math.min(realPart.length, imaginaryPart.length);

        // Calculate the energy density as the square of the absolute values of the complex spectrum
        double[] energyDensity = new double[numBins];
        for (int i = 0; i < numBins; i++) {
            double realValue = realPart[i];
            double imaginaryValue = imaginaryPart[i];
            energyDensity[i] = Math.pow(Math.hypot(realValue, imaginaryValue), 2);
        }

        return energyDensity;
    }

    /**
     * this is a helper method for the maintainer thread
     *
     * @param milliseconds
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Logger logger = Logger.getLogger(ArpeggiatorumGUI.getInstance().getClass().getName());
            logger.log(Level.SEVERE, "Failed to sleep.", e);
            LogGUIController.logBuffer.append(e.getMessage());
        }
    }

    /**
     * a little helper method to get an overview over all available audio devices and their input/output channels
     */
    public static void printAudioDevices() {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getDeviceCount();

        LogGUIController.logBuffer.append("\nID\tdevice name (Input/Output channels)\n--\t-----------------------------------------------------------\r\n");
        for (int i = 0; i < numDevices; ++i) {
            LogGUIController.logBuffer.append(i + "\t" + audioManager.getDeviceName(i)
                    + " (" + audioManager.getMaxInputChannels(i)
                    + "/" + audioManager.getMaxOutputChannels(i) + ")");
            if (i == audioManager.getDefaultInputDeviceID())
                LogGUIController.logBuffer.append("\t[Default Input device]\n");
            else if (i == audioManager.getDefaultOutputDeviceID())
                LogGUIController.logBuffer.append("\t[Default Output device]\n");
            else
                LogGUIController.logBuffer.append("\n");

        }
        LogGUIController.logBuffer.append("\n");
    }

    /**
     * this method searches the available devices for the specified device name and returns its ID number
     *
     * @param deviceName
     * @return
     */
    public static int getDeviceID(String deviceName) {
        if (deviceName == null)
            return AudioDeviceManager.USE_DEFAULT_DEVICE;

        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getDeviceCount();

        for (int i = 0; i < numDevices; ++i)
            if (audioManager.getDeviceName(i).equals(deviceName))
                return i;

        return AudioDeviceManager.USE_DEFAULT_DEVICE;
    }

    /**
     * get the ID of the requested device or null if the device is unavailable
     *
     * @param deviceName
     * @return
     */
    public static Integer isDeviceAvailable(String deviceName) {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getDeviceCount();

        for (int i = 0; i < numDevices; ++i)
            if (audioManager.getDeviceName(i).equals(deviceName))
                return i;

        return null;
    }

    public static double[] toDoubleArray(float[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        double[] ret = new double[n];
        for (int i = 0; i < n; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float) arr[i];
        }
        return ret;
    }
}
