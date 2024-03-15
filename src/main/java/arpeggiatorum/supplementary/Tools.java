package arpeggiatorum.supplementary;

import arpeggiatorum.gui.GUI;
import com.jsyn.data.Spectrum;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.devices.AudioDeviceManager;

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
            GUI.updateLogGUI(e.getMessage());
        }
    }

    /**
     * a little helper method to get an overview over all available audio devices and their input/output channels
     */
    public static void printAudioDevices() {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager(false);
        int numDevices = audioManager.getDeviceCount();

        GUI.updateLogGUI("\nID\tdevice name (Input/Output channels)\n--\t-----------------------------------------------------------\r\n");
        for (int i = 0; i < numDevices; ++i) {
            GUI.updateLogGUI(i + "\t" + audioManager.getDeviceName(i)
                    + " (" + audioManager.getMaxInputChannels(i)
                    + "/" + audioManager.getMaxOutputChannels(i) + ")");
            if (i == audioManager.getDefaultInputDeviceID())
                GUI.updateLogGUI("\t[Default Input device]\n");
            else if (i == audioManager.getDefaultOutputDeviceID())
                GUI.updateLogGUI("\t[Default Output device]\n");
            else
                GUI.updateLogGUI("\n");

        }
        GUI.updateLogGUI("\n");
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
}
