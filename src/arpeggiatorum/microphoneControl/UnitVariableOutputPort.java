package arpeggiatorum.microphoneControl;

import com.jsyn.ports.ConnectableInput;
import com.jsyn.ports.ConnectableOutput;
import com.jsyn.ports.UnitPort;

public class UnitVariableOutputPort extends UnitPort implements ConnectableOutput{
	private double[] data;
	private boolean available;

	public UnitVariableOutputPort(){
		this("Output");
	}

	public UnitVariableOutputPort(int size){
		this("Output", size);
	}

	public UnitVariableOutputPort(String name){
		super(name);
		data = new double[8];
	}

	public UnitVariableOutputPort(String name, int size){
		super(name);
		data = new double[size];
	}

	public double[] getData(){
		return data;
	}

	public void advance(){
		available = true;
	}

	@Override
	public void connect(ConnectableInput other){
		other.connect(this);
	}

	@Override
	public void disconnect(ConnectableInput other){
		other.disconnect(this);
	}

	public boolean isAvailable(){
		return available;
	}

}
