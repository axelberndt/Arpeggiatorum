/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                             c
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*  
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*  
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
* 
*/

/*
*      _______                       _____   _____ _____  
*     |__   __|                     |  __ \ / ____|  __ \ 
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/ 
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |     
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|     
*                                                         
* -----------------------------------------------------------
*
*  TarsosDSP is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*  https://github.com/JorenSix/TarsosDSP
*  http://tarsos.0110.be/releases/TarsosDSP/
* 
*/
/* 
 * Copyright (c) 2006, Karl Helgason
 * 
 * 2007/1/8 modified by p.j.leonard
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package arpeggiatorum.microphoneControl;

import java.util.Arrays;

import com.jsyn.data.HammingWindow;
import com.jsyn.data.SpectralWindow;
import com.jsyn.data.Spectrum;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.ports.UnitSpectralOutputPort;
import com.softsynth.math.FourierMath;

import com.jsyn.unitgen.*;

//Copy of the SpectralFFT to generate a stub.

/**
 * Periodically transform the input signal using an FFT. Output complete spectra.
 * 
 * @author Phil Burk (C) 2013 Mobileer Inc
 * @version 016
 * @see SpectralIFFT
 * @see Spectrum
 * @see SpectralFilter
 */

/**
 * Implementation of the Constant Q Transform.<br>
 * References:
 * <p>
 * Judith C. Brown, <a
 * href="http://www.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf">
 * Calculation of a constant Q spectral transform</a>, J. Acoust. Soc. Am.,
 * 89(1): 425-434, 1991.
 * </p>
 * <p>
 * Judith C. Brown and Miller S. Puckette, <a
 * href="http://www.wellesley.edu/Physics/brown/pubs/effalgV92P2698-P2701.pdf"
 * >An efficient algorithm for the calculation of a constant Q transform</a>, J.
 * Acoust. Soc. Am., Vol. 92, No. 5, November 1992
 * </p>
 * <p>
 * Benjamin Blankertz, <a href=
 * "http://wwwmath1.uni-muenster.de/logik/org/staff/blankertz/constQ/constQ.pdf"
 * >The Constant Q Transform</a>
 * </p>
 * 
 * 
 * @author Joren Six
 * @author Karl Helgason
 * @author P.J Leonard
 */
public class CQT extends UnitGenerator {
    public UnitInputPort input;
    /**
     * Provides complete complex spectra when the FFT completes.
     */
    public UnitSpectralOutputPort output;
    private double[] buffer;
    private int cursor;
    private SpectralWindow window = RectangularWindow.getInstance();
    private int sizeLog2;
    private int offset;
    private boolean running;

    /**
     * The minimum frequency, in Hertz. The Constant-Q factors are calculated
     * starting from this frequency.
     */
    private final double minimumFrequency;

    /**
     * The maximum frequency in Hertz.
     */
    private final double maximumFreqency;

    /**
     * The length of the underlying FFT.
     */
    private int fftLength;

    /**
     * Lists the start of each frequency bin, in Hertz.
     */
    private final double[] frequencies;

    private final double[][] qKernel;

    private final int[][] qKernel_indexes;

    /**
     * The array with constant q coefficients. If you for
     * example are interested in coefficients between 256 and 1024 Hz
     * (2^8 and 2^10 Hz) and you requested 12 bins per octave, you
     * will need 12 bins/octave * 2 octaves * 2 entries/bin = 48
     * places in the output buffer. The coefficient needs two entries
     * in the output buffer since they are complex numbers.
     */
    private final double[] coefficients;

    /**
     * The output buffer with constant q magnitudes. If you for example are
     * interested in coefficients between 256 and 1024 Hz (2^8 and 2^10 Hz) and
     * you requested 12 bins per octave, you will need 12 bins/octave * 2
     * octaves = 24 places in the output buffer.
     */
    private final double[] magnitudes;

    /**
     * The number of bins per octave.
     */
    private int binsPerOctave;

    /**
     * The underlying FFT object.
     */
    private SpectralFFT fft;

    /* Define Unit Ports used by connect() and set(). */
    public CQT() {
        this(Spectrum.DEFAULT_SIZE_LOG_2, 44100, 86.12, 5601.68, 12, 0.001f, 1.0f);
    }

    /**
     * @param sizeLog2 for example, pass 10 to get a 1024 bin FFT
     */
    public CQT(int sizeLog2) {
        this(sizeLog2, 44100, 44100.0f/sizeLog2, 5601.68, 12, 0.001f, 1.0f);
    }

    public CQT(int sizeLog2, double sampleRate, double minFreq, double maxFreq, double binsPerOctave, double threshold,
            double spread) {
        addPort(input = new UnitInputPort("Input"));
        addPort(output = new UnitSpectralOutputPort("Output", 1 << sizeLog2));
        setSizeLog2(sizeLog2);

        this.minimumFrequency = minFreq;
        this.maximumFreqency = maxFreq;
        this.binsPerOctave = (int) binsPerOctave;

        // Calculate Constant Q
        double q = 1.0 / (Math.pow(2, 1.0 / binsPerOctave) - 1.0) / spread;

        // Calculate number of output bins
        int numberOfBins = (int) Math.ceil(binsPerOctave * Math.log(maximumFreqency / minimumFrequency) / Math.log(2));

        // Initialize the coefficients array (complex number so 2 x number of bins)
        coefficients = new double[numberOfBins * 2];

        // Initialize the magnitudes array
        magnitudes = new double[numberOfBins];

        // Calculate the minimum length of the FFT to support the minimum
        // frequency
        double calc_fftlen = (double) Math.ceil(q * sampleRate / minimumFrequency);

        // No need to use power of 2 FFT length.
        fftLength = (int) calc_fftlen;

        // System.out.println(fftLength);
        // The FFT length needs to be a power of two for performance reasons:
        fftLength = (int) Math.pow(2, Math.ceil(Math.log(calc_fftlen) / Math.log(2)));

        // Create FFT object
        fft = new SpectralFFT(fftLength);
        qKernel = new double[numberOfBins][];
        qKernel_indexes = new int[numberOfBins][];
        frequencies = new double[numberOfBins];

        // Calculate Constant Q kernels
        double[] temp = new double[fftLength * 2];
        double[] ctemp = new double[fftLength * 2];
        int[] cindexes = new int[fftLength];
        for (int i = 0; i < numberOfBins; i++) {
            double[] sKernel = temp;
            // Calculate the frequency of current bin
            frequencies[i] = (double) (minimumFrequency * Math.pow(2, i / binsPerOctave));

            // Calculate length of window
            int len = (int) Math.min(Math.ceil(q * sampleRate / frequencies[i]), fftLength);

            for (int j = 0; j < len; j++) {

                double cqtwindow = -.5 * Math.cos(2. * Math.PI * (double) j / (double) len) + .5;
                ; // Hanning Window
                  // double window = -.46*Math.cos(2.*Math.PI*(double)j/(double)len)+.54; //
                  // Hamming Window

                  cqtwindow /= len;

                // Calculate kernel
                double x = 2 * Math.PI * q * (double) j / (double) len;
                sKernel[j * 2] = (double) (cqtwindow * Math.cos(x));
                sKernel[j * 2 + 1] = (double) (cqtwindow * Math.sin(x));
            }
            for (int j = len * 2; j < fftLength * 2; j++) {
                sKernel[j] = 0;
            }

            // Perform FFT on kernel
            // This needs to happen once:
            // fft.complexForwardTransform(sKernel);
            Spectrum spectrum = new Spectrum(sKernel.length);
            Arrays.fill(spectrum.getImaginary(), 0.0);
            System.arraycopy(sKernel,0,spectrum.getReal(),0,sKernel.length);
            FourierMath.fft(sKernel.length, spectrum.getReal(), spectrum.getImaginary());

            // Remove all zeros from kernel to improve performance
            double[] cKernel = ctemp;

            int k = 0;
            for (int j = 0, j2 = sKernel.length - 2; j < sKernel.length / 2; j += 2, j2 -= 2) {
                double absval = Math.sqrt(sKernel[j] * sKernel[j] + sKernel[j + 1] * sKernel[j + 1]);
                absval += Math.sqrt(sKernel[j2] * sKernel[j2] + sKernel[j2 + 1] * sKernel[j2 + 1]);
                if (absval > threshold) {
                    cindexes[k] = j;
                    cKernel[2 * k] = sKernel[j] + sKernel[j2];
                    cKernel[2 * k + 1] = sKernel[j + 1] + sKernel[j2 + 1];
                    k++;
                }
            }

            sKernel = new double[k * 2];
            int[] indexes = new int[k];

            for (int j = 0; j < k * 2; j++)
                sKernel[j] = cKernel[j];
            for (int j = 0; j < k; j++)
                indexes[j] = cindexes[j];

            // Normalize fft output
            for (int j = 0; j < sKernel.length; j++)
                sKernel[j] /= fftLength;

            // Perform complex conjugate on sKernel
            for (int j = 1; j < sKernel.length; j += 2)
                sKernel[j] = -sKernel[j];

            for (int j = 0; j < sKernel.length; j++)
                sKernel[j] = -sKernel[j];

            qKernel_indexes[i] = indexes;
            qKernel[i] = sKernel;
        }
    }

    /**
     * Please do not change the size of the FFT while JSyn is running.
     * 
     * @param sizeLog2 for example, pass 9 to get a 512 bin FFT
     */
    public void setSizeLog2(int sizeLog2) {
        this.sizeLog2 = sizeLog2;
        output.setSize(1 << sizeLog2);
        buffer = output.getSpectrum().getReal();
        cursor = 0;
    }

    public int getSizeLog2() {
        return sizeLog2;
    }

    @Override
    public void generate(int start, int limit) {
        if (!running) {
            int mask = (1 << sizeLog2) - 1;
            if (((getSynthesisEngine().getFrameCount() - offset) & mask) == 0) {
                running = true;
                cursor = 0;
            }
        }
        // Don't use "else" because "running" may have changed in above block.
        if (running) {
            double[] inputs = input.getValues();
            for (int i = start; i < limit; i++) {
                buffer[cursor] = inputs[i] * window.get(cursor);
                ++cursor;
                // When it is full, do the FFT.
                if (cursor == buffer.length) {
                    // FourierMath.fft(buffer.length, spectrum.getReal(), spectrum.getImaginary());
                    // CQT Main loop
                    Spectrum spectrum = output.getSpectrum();
                    Arrays.fill(spectrum.getImaginary(), 0.0);
                    calculateMagnitudes(buffer);
                    System.arraycopy(buffer,0,spectrum.getReal(),0,buffer.length/2);
                    output.advance();
                    cursor = 0;

                }
            }
        }
    }

    /**
     * Take an input buffer with audio and calculate the constant Q
     * coefficients.
     * 
     * @param inputBuffer
     *                    The input buffer with audio.
     * 
     * 
     */
    public void calculate(double[] inputBuffer) {
        //Single FFT of the inputBuffer
        //fft.forwardTransform(inputBuffer);
        //Problems with buffer size?
        Spectrum spectrum = new Spectrum((int)Math.pow(2,sizeLog2));
        Arrays.fill(spectrum.getImaginary(), 0.0);
        System.arraycopy(inputBuffer,0,spectrum.getReal(),0,inputBuffer.length);
        FourierMath.fft(inputBuffer.length, spectrum.getReal(), spectrum.getImaginary());

        for (int i = 0; i < qKernel.length; i++) {
            double[] kernel = qKernel[i];
            int[] indexes = qKernel_indexes[i];
            double t_r = 0;
            double t_i = 0;
            for (int j = 0, l = 0; j < kernel.length; j += 2, l++) {
                int jj = indexes[l];
                double b_r = inputBuffer[jj];
                double b_i = inputBuffer[jj + 1];
                double k_r = kernel[j];
                double k_i = kernel[j + 1];
                // COMPLEX: T += B * K
                t_r += b_r * k_r - b_i * k_i;
                t_i += b_r * k_i + b_i * k_r;
            }
            coefficients[i * 2] = t_r;
            coefficients[i * 2 + 1] = t_i;
        }
    }

    /**
     * Take an input buffer with audio and calculate the constant Q magnitudes.
     * 
     * @param inputBuffer The input buffer with audio.
     */
    public void calculateMagnitudes(double[] inputBuffer) {
        calculate(inputBuffer);
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = (double) Math.sqrt(
                    coefficients[i * 2] * coefficients[i * 2] + coefficients[i * 2 + 1] * coefficients[i * 2 + 1]);
        }
    }

    public SpectralWindow getWindow() {
        return window;
    }

    /**
     * Multiply input data by this window before doing the FFT. The default is a
     * RectangularWindow.
     */
    public void setWindow(SpectralWindow window) {
        this.window = window;
    }

    /**
     * The FFT will be performed on a frame that is a multiple of the size plus this
     * offset.
     * 
     * @param offset
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

}
