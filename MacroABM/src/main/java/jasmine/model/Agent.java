package jasmine.model;

import jasmine.experiment.MacroCollector;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;

public abstract class Agent implements EventListener {

	// model is used mostly to access the policy parameters, as well as the static variables
	// collector is used to access the macro variables (e.g. current wage, aggregate demand etc.)
	protected MacroModel model;

	protected MacroCollector collector;

	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
	}

	public void onEvent(Enum<?> type) {
	}

	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------
	
	public Agent(){
		
		this.model = (MacroModel) SimulationEngine.getInstance().getManager(MacroModel.class.getCanonicalName());
		this.collector 		= model.getCollector();
		
	}

	
	// ---------------------------------------------------------------------
	// Destructor
	// ---------------------------------------------------------------------

	public void kill(){
		this.model 			= null;
		this.collector 		= null;
	}

	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public MacroModel getModel(){
		return this.model;
	}
	
	public MacroCollector getCollector(){
		return this.collector;
	}
	
}