package arpeggiatorum.microphoneControl;


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
	private double max;
	private final int WIDTH = 800;
	private final int HEIGHT = 800;
	private final int BORDER = 20;
	private MovingAverage mva = new MovingAverage(.99f);

	public CQTHistogram(double[] binCounts, double[] frequencies){
		this.binCounts = binCounts;
		this.frequencies = frequencies;
		//max=mva.average(Arrays.stream(binCounts).max().getAsDouble());
		max=1.0f;
		//
		// System.out.println(max);
	}

	public void updateBins(double[] binCounts){
		this.binCounts = binCounts;
		//max=mva.average(Arrays.stream(binCounts).max().getAsDouble());
	}

	public void updateBins(double[] binCounts, int lowIndex){
		//this.binCounts = binCounts;
		for (int i = 0; i <binCounts.length ; i++){
			this.binCounts[lowIndex+i]=binCounts[i];
		}
		//max=mva.average(Arrays.stream(binCounts).max().getAsDouble());
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
		for (int i = 0; i < binCounts.length; i++){
			int barHeight = (int) ((binCounts[i] / max) * (getHeight() - BORDER));
			Rectangle rect = new Rectangle(i * barWidth, getHeight() - barHeight, barWidth, barHeight);
			g2d.setColor(Color.GREEN);
			g2d.fill(rect);
			g2d.setColor(Color.BLACK);
			g2d.draw(rect);
			g2d.setColor(Color.RED);
			g2d.drawString(String.format("%.0fHz", frequencies[i]), i * barWidth, getHeight() - barHeight);
		}
		g2d.dispose();

	}
}
