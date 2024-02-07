package arpeggiatorum.microphoneControl;

import com.jsyn.ports.ConnectableInput;
import com.jsyn.ports.ConnectableOutput;
import com.jsyn.ports.PortBlockPart;
import com.jsyn.ports.UnitPort;

public class UnitVariableInputPort extends UnitPort implements ConnectableInput{
	private UnitVariableOutputPort other;

	private double[] data;

	public UnitVariableInputPort(){
		this("Output");
	}

	public UnitVariableInputPort(String name){
		super(name);
	}

	public void setData(double[] data){
		this.data = data;
	}

	public double[] getData(){
		if (other == null){
			return data;
		} else{
			return other.getData();
		}
	}

	@Override
	public void connect(ConnectableOutput other){
		if (other instanceof UnitVariableOutputPort){
			this.other = (UnitVariableOutputPort) other;
		} else{
			throw new RuntimeException(
					"Can only connect UnitVariableOutputPort to UnitVariableInputPort!");
		}
	}

	@Override
	public void disconnect(ConnectableOutput other){
		if (this.other == other){
			this.other = null;
		}
	}

	@Override
	public PortBlockPart getPortBlockPart(){
		return null;
	}

	@Override
	public void pullData(long frameCount, int start, int limit){
		if (other != null){
			other.getUnitGenerator().pullData(frameCount, start, limit);
		}
	}

	public boolean isAvailable(){
		if (other != null){
			return other.isAvailable();
		} else{
			return (data != null);
		}
	}

}
