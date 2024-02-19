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

        // Berechne die Energiedichte als Quadrat der absoluten Werte des komplexen Spektrums
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
            //e.printStackTrace();
            GUI.logMessages.append(e.getMessage());
        }
    }

    /**
     * a little helper method to get an overview over all available audio devices and their input/output channels
     */
    public static void printAudioDevices() {
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager();
        int numDevices = audioManager.getDeviceCount();

        GUI.logMessages.append("\nID\tdevice name (input/output channels)\n--\t-----------------------------------------------------------\r\n");
        // System.out.println("\nID\tdevice name (input/output channels)\n--\t-----------------------------------------------------------");
        for (int i = 0; i < numDevices; ++i) {
            //  System.out.print(i + "\t" + audioManager.getDeviceName(i)
            //           + " (" + audioManager.getMaxInputChannels(i)
            //           + "/" + audioManager.getMaxOutputChannels(i) + ")");
            GUI.logMessages.append(i + "\t" + audioManager.getDeviceName(i)
                    + " (" + audioManager.getMaxInputChannels(i)
                    + "/" + audioManager.getMaxOutputChannels(i) + ")");
            if (i == audioManager.getDefaultInputDeviceID())
                // System.out.println("\t[default input device]");
                GUI.logMessages.append("\t[default input device]\n");
            else if (i == audioManager.getDefaultOutputDeviceID())
                // System.out.println("\t[default output device]");
                GUI.logMessages.append("\t[default output device]\n");
            else
                // System.out.print("\n");
                GUI.logMessages.append("\n");

        }
        // System.out.print("\n");
        GUI.logMessages.append("\n");
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

        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager();
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
        AudioDeviceManager audioManager = AudioDeviceFactory.createAudioDeviceManager();
        int numDevices = audioManager.getDeviceCount();

        for (int i = 0; i < numDevices; ++i)
            if (audioManager.getDeviceName(i).equals(deviceName))
                return i;

        return null;
    }
}
