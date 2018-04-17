package jasmine.experiment;

import microsim.engine.SimulationEngine;
import microsim.engine.MultiRun;
import microsim.gui.shell.MultiRunFrame;

import jasmine.model.MacroModel;

public class MacroMultiRun extends MultiRun {

	// Experimental design usually involves one of the following:
	// (a) Running the simulation a given number of times, without changing the values of the parameters (but changing the random number seed)
	// (b) Spanning over the values of the parameters, keeping the random number seed fixed
	// In the example, the simulation is repeated a number of times equal to numberOfRepeatedRuns for each population size of agents, 	// specified by the parameter maxNumberOfAgents.

	public static boolean executeWithGui 			= true;

	// Define the parameters that specify the experiment, and assign an initial value (used in the first simulation)
	private Long counter 							= 1L;

	// Define maximum values for the experiment (used in the last simulation)
	private static Integer numberOfRepeatedRuns		= 20;		//Set default number of repeated runs
	private static Double endTime 					= 600.;

	//Set the absolute maximum number of runs when using the MultiRun GUI.  The series of simulations will stop when this
	//value is reached when using the MultiRun GUI.  Ensure that this is large enough to cover all necessary simulation runs
	//to prevent premature termination of experiment.
	private static Integer maxNumberOfRuns 			= (int) numberOfRepeatedRuns;
	
	public static void main(String[] args) {

		batchModeArgumentParsing(args);	//Used to pass arguments to the main class via the command line if the user wants to run in 'batch mode' outside of Eclipse IDE

		SimulationEngine engine = SimulationEngine.getInstance();

		MacroMultiRun experimentBuilder = new MacroMultiRun();
		engine.setExperimentBuilder(experimentBuilder);
		engine.setup();

		if (executeWithGui)
			new MultiRunFrame(experimentBuilder, "KS MultiRun", maxNumberOfRuns);
		else
			experimentBuilder.start();
	}

	@Override
	public void buildExperiment(SimulationEngine engine) {

		MacroModel model = new MacroModel();
		engine.addSimulationManager(model);

		MacroCollector collector = new MacroCollector(model);
		engine.addSimulationManager(collector);
		
		model.setCollector(collector); 

		//No need to add observer if running in batch mode

		// Overwrite the default values of the parameters of the simulation
		// model.setNumberOfCFirms(numberOfAgents);
		model.setEndTime(endTime);
	}

	@Override
	public boolean nextModel() {
		// Update the values of the parameters for the next experiment
		//if(counter > numberOfRepeatedRuns) {
		//	numberOfAgents *= 10;			//Increase the number of agents by a factor of 10 for the next experiment
		//	counter = 1L;					//Reset counter
		// }
		
		counter++;
		
		// Define the continuation condition
		if(counter <= numberOfRepeatedRuns) {		//Stop when the numberOfAgents goes above maxNumberOfAgents
			return true;
		}
		else return false;
	}

	@Override
	public String setupRunLabel() {
		// return numberOfAgents.toString() + " agents, count: " + counter.toString();
		return "count: " + counter.toString();
	}


	//MultiRun is designed for batch mode, so can overwrite default values by passing the values
	//to the main class as command line arguments
	//E.g. To specify the number of repeated runs to equal 10, add the following string in
	//the command line when executing the java program:
	// -n 10
	//if, for example, the model is to be run 10 times.
	public static void batchModeArgumentParsing(String[] args) {

		for (int i = 0; i < args.length; i++) {

			if (args[i].equals("-n")){			//Set the number of repeated runs in the experiment as a command line argument

				try {
					numberOfRepeatedRuns = Integer.parseInt(args[i + 1]);
				} catch (NumberFormatException e) {
					System.err.println("Argument " + args[i + 1] + " must be an integer reflecting the number of repeated simulations to run.");
					System.exit(1);
				}
				
				i++;
			}/* 
			else if (args[i].equals("-a")){			//Set the maximum number of agents in the experiment as a command line argument
				
				try {
				maxNumberOfAgents = Integer.parseInt(args[i + 1]);
				} catch (NumberFormatException e) {
					System.err.println("Argument " + args[i + 1] + " must be an integer reflecting the maximum number of agents.");
					System.exit(1);
				}
				
				i++;
			} */
			else if (args[i].equals("-g")){			//Toggle the MultiRun Gui on / off by passing the string '-g true' (on) or '-g false' (off) as a command line argument
				executeWithGui = Boolean.parseBoolean(args[i + 1]);
				i++;
			}
		}
	}

}
