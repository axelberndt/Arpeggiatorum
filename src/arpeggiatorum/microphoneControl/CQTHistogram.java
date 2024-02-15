package arpeggiatorum.microphoneControl;


import arpeggiatorum.supplementary.MovingAverage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.swing.JPanel;

public class CQTHistogram extends JPanel{

	private double[] binCounts;
	private double[] frequencies;
	public double max;
	private final int WIDTH = 800;
	private final int HEIGHT = 800;
	private final int BORDER = 20;
	private MovingAverage mva = new MovingAverage(.99f);

	public CQTHistogram(double[] binCounts, double[] frequencies){
		this.binCounts = binCounts;
		this.frequencies = frequencies;
		max = 0.25f;
	}

	public void updateBins(double[] binCounts){
		this.binCounts = binCounts;
	}

	public void updateBins(double[] binCounts, int lowIndex){
		for (int i = 0; i < binCounts.length; i++){
			this.binCounts[lowIndex + i] = binCounts[i];
		}
	}

	@Override
	public Dimension getPreferredSize(){
		return new Dimension(WIDTH, HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		int barWidth = WIDTH / binCounts.length;
		//max=mva.average(Arrays.stream(binCounts).max().getAsDouble());
		g2d.setColor(Color.RED);
		g2d.drawLine(0,(int)(getHeight()+BORDER)/2,WIDTH, (int)(getHeight()+BORDER)/2);
		for (int i = 0; i < binCounts.length; i++){
//			if (binCounts[i]>max) {
//				max = binCounts[i];
//				System.out.println("New max: "+ max);
//			}
			int barHeight = (int) ((binCounts[i] / max) * (getHeight() - BORDER));
			Rectangle rect = new Rectangle(i * barWidth, getHeight() - barHeight, barWidth, barHeight);
			g2d.setColor(Color.GREEN);
			g2d.fill(rect);
			g2d.setColor(Color.BLACK);
			g2d.draw(rect);
			g2d.setColor(Color.BLUE);
			g2d.drawString(String.format("%.0fHz", frequencies[i]), i * barWidth, getHeight() - barHeight);

		}
		g2d.dispose();
	}
}
