package arpeggiatorum.microphoneControl;

import java.awt.*;
import javax.swing.JPanel;

public class CQTHistogram extends JPanel {

    private double[] binCounts;
    private double[] frequencies;
    public double max;
    private final int WIDTH = 1080;
    private final int HEIGHT = 500;
    private final int BORDER = 10;

    public CQTHistogram(double[] binCounts, double[] frequencies) {
        this.binCounts = binCounts;
        this.frequencies = frequencies;
        max = 1.0f;

    }

    public void updateBins(double[] binCounts) {
        this.binCounts = binCounts;
    }

    public void updateBins(double[] binCounts, int lowIndex) {
        for (int i = 0; i < binCounts.length; i++) {
            this.binCounts[lowIndex + i] = binCounts[i];
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Font currentFont = g.getFont();
        Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.75F);

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(newFont);
        g2d.clearRect(0, 0, getWidth(), getHeight());
        int barWidth = getWidth() / binCounts.length;
        g2d.setColor(Color.RED);
        g2d.drawLine(0, (int) (getHeight() + BORDER) / 2, getWidth(), (int) (getHeight() + BORDER) / 2);
        for (int i = 0; i < binCounts.length; i++) {
            int barHeight = (int) ((binCounts[i] / max) * (getHeight() - BORDER));
            Rectangle rect = new Rectangle(i * barWidth, getHeight() - barHeight, barWidth, barHeight);
            if (barHeight >(getHeight() - BORDER) /2)
                g2d.setColor(Color.YELLOW);
            else
                g2d.setColor(Color.GREEN);
            g2d.fill(rect);
            g2d.setColor(Color.BLACK);
            g2d.draw(rect);
            g2d.setColor(Color.BLUE);
            g2d.drawString(String.format("%.0f", frequencies[i]), i * barWidth, getHeight() - barHeight);
        }
        g2d.dispose();
    }

}
