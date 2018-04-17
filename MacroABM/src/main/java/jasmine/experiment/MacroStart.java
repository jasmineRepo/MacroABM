package jasmine.experiment;

import microsim.engine.ExperimentBuilder;
import microsim.engine.SimulationEngine;
import microsim.gui.shell.MicrosimShell;
import jasmine.model.MacroModel;
import jasmine.experiment.MacroCollector;
import jasmine.experiment.MacroObserver;

//*******************************************************************
//
//	Standard JAS-mine project Start class: run this class with Java
//	to run this simulation.  Other than that, there is nothing of
//	interest for the user here.
//
//*******************************************************************
public class MacroStart implements ExperimentBuilder {

	public static void main(String[] args) {
		boolean showGui = true;

		SimulationEngine engine = SimulationEngine.getInstance();
		MicrosimShell gui = null;
		if (showGui) {
			gui = new MicrosimShell(engine);
			gui.setVisible(true);
		}

		MacroStart experimentBuilder = new MacroStart();
		engine.setExperimentBuilder(experimentBuilder);

		engine.setup();
	}

	public void buildExperiment(SimulationEngine engine) {
		MacroModel model = new MacroModel();
		MacroCollector collector = new MacroCollector(model);
		model.setCollector(collector); 
		MacroObserver observer = new MacroObserver(model, collector);

		engine.addSimulationManager(model);
		engine.addSimulationManager(collector);
		engine.addSimulationManager(observer);	
	}
	
}
