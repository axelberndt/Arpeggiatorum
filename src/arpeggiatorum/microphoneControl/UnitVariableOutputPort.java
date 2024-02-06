package arpeggiatorum.microphoneControl;

import com.jsyn.ports.ConnectableInput;
import com.jsyn.ports.ConnectableOutput;
import com.jsyn.ports.UnitPort;

public class UnitVariableOutputPort extends UnitPort implements ConnectableOutput{

	private double[] data;
	private boolean available;

	public boolean isAvailable() {
		return available;
	}
	public void advance() {
		available = true;
	}
	public UnitVariableOutputPort() {
		this("Output");
	}

	public UnitVariableOutputPort(int size) {
		this("Output", size);
	}
	public UnitVariableOutputPort(String name){
		super(name);
		data= new double[8];
	}
	public UnitVariableOutputPort(String name, int size) {
		super(name);
		data= new double[size];
	}
	public double[] getData() {
		return data;
	}

	/**
	 * @param connectableInput
	 */
	@Override
	public void connect(ConnectableInput connectableInput){
		connectableInput.connect(this);
	}

	/**
	 * @param connectableInput
	 */
	@Override
	public void disconnect(ConnectableInput connectableInput){
		connectableInput.disconnect(this);

	}
}
