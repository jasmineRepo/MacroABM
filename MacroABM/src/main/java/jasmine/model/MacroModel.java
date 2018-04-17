package jasmine.model;

/* FIXME INTRODUCTORY NOTES
 	(a) not done: machineScrapping()
 	(b) doubt: when a firm exits, should we set its liquid assets = 0? If do not, then counts in the current 
 	period statistics, even though the firm exits… (if so: modify in myopicDebtAdjustment(), marketCompetitiveness(), 
 	and accounting() )
 */


import microsim.engine.AbstractSimulationManager;
import microsim.engine.SimulationEngine;
import microsim.annotation.GUIparameter;
import microsim.event.EventGroup;
import microsim.event.EventListener;
import microsim.event.Order;
import microsim.event.SystemEvent;
import microsim.event.SystemEventType;

import jasmine.data.Parameters;
import jasmine.experiment.MacroCollector;
import jasmine.object.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import javax.persistence.Transient;

import org.apache.log4j.Logger;


public class MacroModel extends AbstractSimulationManager implements EventListener {

	private final static Logger log = Logger.getLogger(MacroModel.class);
	
	@Transient
	MacroCollector collector; 

	// Parameters changeable in the GUI 
	@GUIparameter(description = "Set the number of consumption-good firms")
	Integer numberOfCFirms 				= 200;
	@GUIparameter(description = "Set the number of capital-good firms")
	Integer numberOfKFirms 				= 50;
	@GUIparameter(description = "Set the length of the simulation")
	Double endTime 						= 500.;
	@GUIparameter(description = "Bank's reserve requirement rate") 
	Double reserveRequirementRate 		= 0.5;
	@GUIparameter(description = "Loan-to-value ratio") 
	Double loanToValueRatio 			= 2.;
	@GUIparameter(description = "Unemployment benefit defined as the proportion of market wage")
	Double unemploymentBenefitShare 	= 0.4;		//was minWage, XXX: Is this relabelling correct?
	@GUIparameter(description = "Initial mark up rate for consumption-good firms")
	Double markUpRate 						= 0.2;
	@GUIparameter(description = "Tax rate on firms' profit")
	Double taxRate 						= 0.1;
	@GUIparameter(description = "Economy's interest rate")
	Double interestRate 							= 0.025;
	@GUIparameter(description = "Determines how firms consider debt repayment")
	boolean myopicDebtRepayment 		= true;
	@GUIparameter(description = "Mason (if set to true) or Lhuillier (if set to false) version for debt management")
	boolean mason						= true;
	@GUIparameter(description = "Use a fixed random seed to start (pseudo) random number generator")
	boolean fixRandomSeed 				= true;
	@GUIparameter(description = "Seed of the (pseudo) random number generator if fixed")
	Long seedIfFixed 					= 1166517026l;
	

	// --- Define static variables for variables that are global to the economy & constant ---
	// return on firms' deposits 
	static double returnOnFirmsDeposits; 
	// interest rate on the debt 
	static double interestRateOnDebt; 
	// return on the bank's deposit at the central bank
	static double returnOnBankDepositsAtCentralBank; 
	// Tax rate. Need a static variable for static classes APrioriAdjustments and APosterioriAdjustments
	static double tax_rate; 
	// Labor supply (fixed and inelastic)
	static double laborSupply;
	
	// Variable used in the consumption allocation process. Use to determine the remaining amount of consumption to allocate (see consumptionAllocation())
	public double consumptionTemp; 
	
	// --- Define the entities of the model ---
	// Consumption-good firms
	private List<CFirm> cFirms;
	// Capital-good firms
	private List<KFirm> kFirms;
	// (Single) bank
	private Bank bank;
	
	// ---------------------------------------------------------------------
	// Manager methods
	// ---------------------------------------------------------------------
	
	public void buildObjects() {

		// Calibrate the model. 
		// If the simulations are thrown from the Estimation class, then only the parameters that are picked manually are loaded.
		if(!Parameters.ESTIMATION_OF_PARAMETERS)
			Parameters.initializationWithoutEstimation();
		else
			Parameters.initializationWithEstimation();
				
		/* Stop the simulation if the number of consumption-good firms is not a multiple of the number of capital-good firms.  This is to ensure that, 
		 * at the start of the simulation, all firms are set in an equivalent state, with the same (integer) number of consumption good firms being
		 * clients of the capital good firms.
		*/
		if(numberOfCFirms % numberOfKFirms != 0){
			throw new IllegalArgumentException("ERROR: The number of consumption-good firms needs to be a multiple of the number of capital-good firms!  Please adjust the number of firms in order to satisfy this.");
//			System.err.println("ERROR: The number of consumption-good firms needs to be a multiple of the number of capital-good firms!  Please adjust the number of firms in order to satisfy this.");
//			getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.End), getEngine().getTime(), Order.BEFORE_ALL.getOrdering());
		}
		
		// Set the seed of the Random Number Generating Function.
		if(fixRandomSeed)										//if fixed, the model will follow the same trajectory as other executions withe same random number seed.
			SimulationEngine.getRnd().setSeed(seedIfFixed);
		
		// Compute the static (constant) variables of the economyÒ
		returnOnFirmsDeposits 				= interestRate * (1 - Parameters.getCoeffMarkDownOnDepositRate_Bank());
		interestRateOnDebt 				= interestRate * (1 + Parameters.getCoeffMarkUpOnInterestRate_Bank());
		returnOnBankDepositsAtCentralBank 				= interestRate * (1 - Parameters.getCoeffMarkDownOnDepositRateAtCentralBank_Bank());
		tax_rate 				= taxRate;
		laborSupply			= Parameters.getLaborSupply();
		
		// --- Create the three main entities --- 
		// Consumption-good firms 
		cFirms = new LinkedList<CFirm>();
		for(int i = 0; i < numberOfCFirms; i++){
			CFirm newCFirm = new CFirm(true);
			cFirms.add(newCFirm);
		}
		
		// Capital-good firms
		kFirms = new LinkedList<KFirm>();
		for(int i = 0; i < numberOfKFirms; i++){
			KFirm newKFirm = new KFirm(true);
			kFirms.add(newKFirm);
		}
		
		// Match customers (consumption-good firms) and producers (capital-good firms) 
		matching();
		
		// The bank
		bank 				= new Bank(true);
		
		// Initialize the macro variables
		collector.macroInitialization();
		
		// Once all the previous variables are initialized, one can compute the initial sales, demand and inventories of consumption-good firms
		for(CFirm cFirm : cFirms){
			cFirm.demandInitialization();
		}
		
		/* FIXME: to potentially remove 
		for(KFirm kFirm : kFirms){
			
			kFirm.research();
			kFirm.machineProduced.setTao(1);
			// Because have to understand this as the accumulation of pre-starting economy research. No cost / spending in R&D
			kFirm.liquidAsset[1] = Parameters.getLiquidAssetCapital(); 
			//Because now there is heterogeneity in k-firms' price --> redefine the sales
			CFirm aCFirm = cFirms.get(0);
			double sales0  = ( ( (aCFirm.inv / Parameters.getDimK()) * numberOfCFirms ) / numberOfKFirms ) * kFirm.p[1];
			kFirm.sales[0] = kFirm.sales[1] = sales0; 
			
		}
		// collector.techFrontier(); */
				
		log.debug("Object created");
				
	}

	public void buildSchedule() {
		EventGroup eventGroup = new EventGroup();
		
		/* Overall schedule:
		 			(1) Exit and entry of consumption & capital-good firms
		 			(2) Update of firms, bank and macroeconomic variables 
		 			(3) Capital-good firms pursue their R&D activities 
		 			(4) Consumption-good firms choose their new supplier
		 			(5) Consumption-good firms plan how much to produce and invest, as well as how much to borrow
		 			(6) The bank allocates its supply of credit 
		 			(7) Consumption-good firms update their production and investment plans according to their resources. If their 
		 			investment is positive, they pass their order to their supplier
		 			(8) Firms hire workers to produce
		 			(9) Capital-good firms start producing. Consumption-good firms scrap the machines they can substitute**
		 			(10) Consumption-good firms produce and sell their goods to the consumers 
		 			(11) All entities realize their accounting
		 			(12) Statistics are computed
		 
		** Note: even if consumption-good firms scrap their machines (9) before producing (10), this does not affect, in the code,
		 the amount firm can actually produce in the current period.
		
		Exit & entry: c-firms with too-low market share & / or negative liquid assets exit the market and are replaced by random 
		copy of current incumbents. K-firms with no clients exit as well, and are replaced in the same fashion.
		Note: entry & exit take place at the beginning of the schedule so that exiting firms are recorded in the database before they die. 
		 */
		eventGroup.addEvent(this, Processes.Exit);
		eventGroup.addEvent(this, Processes.Entry);
		
		// Entities update their variables (e.g. set some of them equal to 0, or update their optimal prices, the credit supply)
		eventGroup.addEvent(collector, MacroCollector.Processes.Update);
		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.Update); 
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.Update); 
		eventGroup.addEvent(bank, Bank.Processes.Update); 
		
		// Capital-good firms undertake their R&D activity. The collector updates the aggregate variables  
		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.Research);
		eventGroup.addEvent(collector, MacroCollector.Processes.TechFrontier); 
		
		// Capital-good firms send brochures to consumption-good firms to promote their machines. Consumption-good firms choose their supplier
		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.Brochure);
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.ChooseSupplier);
		
		/* Consumption-good firms form their initial plans given their demand expectation. Initially, financial constraints are not taken 
		 into account. Then, they consider their borrowing capacity, which may lead to downward adjustments (a priori adjustments) */
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.InitialExpenditures);
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.APrioriAdjustments);
		
		/* The bank observes the aggregate credit demand. If it exceeds its credit supply, the economy is credit rationed. The bank then 
		 sorts firms depending on their net worth to sale ratio. 
		 Once firms have received their loan and know their actual resources, they update their production and investment plans.
		 With their level of investment known, consumption-good firms send their orders to their suppliers. */
		eventGroup.addEvent(bank, Bank.Processes.CreditAllocation);
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.ExpendituresUpdate);
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.InvestmentOrder);
		
		/* Hidden assumption in Dosi et al. (2013): the production function is of the Leontieff form. Thus, if the (aggregate) 
		 labor demand exceeds the labor supply, firms scale down their production plans. */
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.LaborDemand);
		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.LaborDemand);
		eventGroup.addEvent(this, Processes.LaborMarket);
		
		/* Capital market. 
		 		1. Once the actual level of investment is determined (i.e. the level of investment that can be funded and produced), 
		 		consumption-good firms pay their supplier and capital-good firms deliver the machines. 
		 		2. Consumption-good firms scrap the machines they are able to replace. */
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.MachineScrapping);		//Note that Hugo claims scrapping machines before production has no effect on the production in this time-step (see notes above). 
		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.MachineProduction);		//ROSS: Capital Machines should only be available at the end of the time-step in which they were ordered, according to the Dosi papers!
		
		/* Good market. 
				1. Consumption-good firms undertake their production process
				2. The competitiveness of each firm is determined
				3. The consumption allocation starts, determining the demand & the sales of consumption-good firms */
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.Production);		
//		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.MachineProduction);		//ROSS: Capital Machines should only be available at the end of the time-step in which they were ordered, so perhaps this should be placed here in the schedule, after the cFirms do their production.  Note that we put the production here before exit of firms to ensure kFirms provide all machines that were ordered by cFirms before their exit. 
		eventGroup.addEvent(this, Processes.GoodMarketCompetitiveness); 
		eventGroup.addEvent(this, Processes.ConsumptionAllocation); 
		
		// Firms in both sectors compute their profit, their new stock of liquid assets, and pay their debt, if any. 
		eventGroup.addCollectionEvent(kFirms, KFirm.Processes.Accounting); 
		eventGroup.addCollectionEvent(cFirms, CFirm.Processes.Accounting);
		eventGroup.addEvent(bank, Bank.Processes.Accounting);
		
		// Compute the macroeconomic variables. Store them in the MacroStatistics class to then export them in the .csv file
		eventGroup.addEvent(collector, MacroCollector.Processes.AggregateComputation);
		eventGroup.addEvent(collector, MacroCollector.Processes.DumpInStatistics);
		
		getEngine().getEventQueue().scheduleRepeat(eventGroup, 0., Parameters.MODEL_ORDERING, 1.);
		
		//For termination of simulation
//		getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.End), endTime, Order.AFTER_ALL.getOrdering());
		SystemEvent end = new SystemEvent(SimulationEngine.getInstance(), SystemEventType.End);
		getEngine().getEventQueue().scheduleOnce(end, endTime, Order.AFTER_ALL.getOrdering());

		
		log.fatal("Schedule created");
	}


	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
		Exit,
		Entry,
		LaborMarket,
		GoodMarketCompetitiveness,
		ConsumptionAllocation,
		End;
	}

	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case Exit:
			exit();
			break;
		case Entry:
			entry();
			break;
			
		case LaborMarket:
			laborMarket();
			break;
			
		case GoodMarketCompetitiveness:
			marketCompetitiveness();
			break;
			
		case ConsumptionAllocation:
			consumptionAllocation();
			break;

		case End:
			getEngine().end();
			break;

		}
	}
	
	
	// ---------------------------------------------------------------------
	// Initialization methods
	// ---------------------------------------------------------------------

	void matching(){
		// Temporary variables to perform the matching 
		int step 				= (int)((double)numberOfCFirms / (double)numberOfKFirms);
		int count 				= 0;
		
		// Each consumption-good firms has a unique supplier; each capital-good firms has # "step" clients
		for(KFirm kFirm : kFirms){
//			cont 				+= step;
			
			for(int i = 0; i < step; i++){
//				CFirm cFirm 	= cFirms.get((count - i - 1));		//Ross: This seems like a overly complicated, confusing way of picking a cFirm.  I replace with the line below and move incrementation of count to afterwards.
				CFirm cFirm 	= cFirms.get((count + i));
				cFirm.supplier 	= kFirm;
				kFirm.clients.add(cFirm);
			}
			count 				+= step;
			
		}
		
		// Fill the initial stock of machine for consumption-good firms. 
		KFirm producer 			= kFirms.get(0);
		
		for(CFirm cFirm : cFirms){
			// The firms' initial stock is populated of machines that are identical in terms of 
			// productivity, but that differs with respect to their age (s.t. substitutionary investments are initially positive).
			
			int numberMachine 	= ( (int) cFirm.capitalStock ) / ( (int) Parameters.getMachineSizeInCapital_cFirms() );
			int age 			= (int) Parameters.getMaxAgeMachines_cFirms() + 1;
			
			while(numberMachine > 0){
				Machine m 		= producer.factory();
				cFirm.machineQuantityMap.put(m, 1);
				cFirm.machineAgeMap.put(m, age);
				
				age--;
				if(age < 1)
					age 		= (int) Parameters.getMaxAgeMachines_cFirms() + 1;
				
				numberMachine--;	
			}
		}
		log.debug("Matching completed");
	}

	// ---------------------------------------------------------------------
	// Entry / exit methods
	// ---------------------------------------------------------------------
	
	void exit(){
		log.debug("Exit process");
		/* INTRODUCTORY NOTES: the exit and entry process follows the logic of object-oriented programming. When a firm exits,
		it is removed from the list of surviving firms, as well as all objects that are only linked to this firm. 
		
		Note on the structure of the method: when a capital-good firm looses all its clients because these latest exit the economy, 
		then we assume that this capital-good firm leaves the market as well. Thus, the consumption-good firm exit process needs to 
		be coded before the capital-good firm exit one. 
		
		Because the exit process takes place at the beginning of the period (see the rationale above), every time series that are linked
		to exit behaviors (e.g. exit, bankruptcy rate etc.) need to be shifted by -1 period such that they synchronize with the business 
		cycle. */		 
		
		// Identify which consumption-good firm exit 
		collector.cFirmsRemaining 					= 0;
		collector.exit_cFirms 							= 0;
		collector.exitLiquidityIssue_cFirms 					= 0;
		collector.exitMarketShareIssue_cFirms 					= 0;
		collector.exitAssetMarket_cFirms					= 0;
		collector.bankruptcyRate_cFirms					= 0;
	
		for(CFirm cFirm : cFirms)
			cFirm.survivalChecking();
		
		// If all consumption-good firms exit in a given period, then ends the simulation
		if(collector.cFirmsRemaining > 0){
			// Remove from list c-firms that exit
			for(Iterator<CFirm> it = cFirms.iterator(); it.hasNext();){
				
				CFirm cFirm 						= it.next();
				
				if(cFirm.exit){					
					// Remove the c-firm from the list of clients of its supplier, and remove the object's references 
					cFirm.supplier.clients.remove(cFirm);
					cFirm.kill();
//					cFirm = null;
					it.remove();
				}
			}
		} else {
			log.fatal("All consumption-good firms exit in the same period. Stop the simulation");
			getEngine().getEventQueue().scheduleSystem(getEngine().getTime(), Order.BEFORE_ALL.getOrdering(), 0., getEngine(), SystemEventType.End);
		}
		
		// Identify which capital-good firm exit
		collector.exit_kFirms 							= 0;

		for(KFirm kFirm : kFirms)
			kFirm.survivalChecking();
		
		// If all capital-good firms exit in a given period, then ends the simulation
		if(collector.kFirmsRemaining > 0){
			// Remove k-firm that exit
			for(Iterator<KFirm> it = kFirms.iterator(); it.hasNext();){
			
				KFirm kFirm = it.next();
				
				if(kFirm.exit){
					// Remove the k-firm from the list of k-firms, and remove the object's references 
					kFirm.kill();
//					kFirm = null;
					it.remove();
				}
			}
		} else {
			log.fatal("All k-firms are going to die in the same period --> stop the simulation");
			getEngine().getEventQueue().scheduleSystem(getEngine().getTime(), Order.BEFORE_ALL.getOrdering(), 0., getEngine(), SystemEventType.End);
		}
		
		log.fatal("Exit statistics: " +
					"\n number of exit 1 " + collector.exit_kFirms +
					"\n number of exit 2 " + collector.exit_cFirms +
					"\n\n asset " + collector.exitLiquidityIssue_cFirms + 
					"\n\n market " + collector.exitMarketShareIssue_cFirms + 
					"\n\n both " + collector.exitAssetMarket_cFirms); 
		
	}
	
	void entry(){
		log.debug("Entry process");
		/* INTRODUCTORY NOTES: new firms are coded as new objects. Their information is mostly copied from a firm already in the market, taken 
		 at random. 
		 * Once more, the other of entrance matters. c-firms, and do not define any supplier
		will be defined anyway in a random manner in the supplier() method, (2) k-firms, so that they can target all c-firms of the economy, including the 
		new entrants */
		
		// Consumption-good firms
		while(cFirms.size() < numberOfCFirms){
			// Randomly select an incumbent, that is not a new entrant 
			CFirm copy 						= null;
			while(copy == null){
				int rnd 					= SimulationEngine.getRnd().nextInt(cFirms.size());
				copy 						= cFirms.get(rnd);
				
				if(copy.newEntrant)
					copy 					= null;
			}
			
			// Create a new consumption-good firms as a copy of the incumbent
			CFirm newEntrant 				= new CFirm(copy);
			
			// Add the new entrant to the list of incumbents 
			cFirms.add(newEntrant);
		}
		log.debug("The final number of C-firms in the economy is " + cFirms.size()); 
		
		// Because the market shares are also copy from the incumbents, it is likely that the sum of market shares exceed 1. Thus normalize.
		collector.marketShareNormalization();
		
		// Capital-good firms
		while(kFirms.size() < numberOfKFirms){
			// Randomly select an incumbent, that is not a new entrant 
			KFirm copy 						= null;
			while(copy == null){
				int rnd 					= SimulationEngine.getRnd().nextInt(kFirms.size());
				copy 						= kFirms.get(rnd);
				if(copy.newEntrant)
					copy 					= null;
			}
			
			// Create a new capital-good firms as a copy of the incumbent
			KFirm newEntrant 				= new KFirm(copy);
			
			// The firm chooses then some potential clients, to which it will send its brochures at the beginning of the period 
			int numberClientsNewEntrant 	= (int)((double)numberOfCFirms / (double)numberOfKFirms);
			
			while(numberClientsNewEntrant > 0){
				int rnd 					= SimulationEngine.getRnd().nextInt(cFirms.size());
				CFirm potentialClient 		= cFirms.get(rnd);
				if(newEntrant.clients.isEmpty() || !newEntrant.clients.contains(potentialClient)){
					newEntrant.clients.add(potentialClient);
					numberClientsNewEntrant -= 1;
				}
			}			
			// Add the new entrant to the list of incumbents 
			kFirms.add(newEntrant);
		}
		log.fatal("The final number of K-firms in the economy is " + kFirms.size());
		
	}
	
	// ---------------------------------------------------------------------
	// Labor & good markets
	// ---------------------------------------------------------------------
	
	void laborMarket(){
		
		// The collector computes the aggregate labor demand
		collector.laborDemandProd();
		double laborDemandProd 				= collector.getLaborDemandProd();
		
		// Subtract from the total labor supply the R&D labor demand, already used  
		double laborSupplyRemaining 		= 0;
		if(laborSupply >= collector.laborDemandForRandD)
			laborSupplyRemaining 			= laborSupply - collector.laborDemandForRandD;
		else 
			System.err.println("ERROR: labor demand RD > labor supply");
		
		log.debug("The aggregate labor demand is " + laborDemandProd + " while the labor supply is " + laborSupplyRemaining);
		
		// Firms have to change their production plans if there is labor rationing 
		if(laborDemandProd > laborSupplyRemaining){
			// Consumption and capital-good firms scale down their production plans. There is no unemployment.
			// NOTE: labor rationing reduces also consumption-good firms' investments as it shifts down capital-good 
			// firm production 
			
			collector.rationingRatio_Labor	 	= laborSupplyRemaining / laborDemandProd;
			log.fatal("Labor rationing, with ratio " + collector.rationingRatio_Labor);
			
			for(CFirm cFirm : cFirms)
				cFirm.laborRationing(collector.rationingRatio_Labor);
			for(KFirm kFirm : kFirms)
				kFirm.laborRationing(collector.rationingRatio_Labor);
			
			collector.laborDemandProd();			
		}
		
		collector.laborDemand 				= collector.laborDemandUsedForProduction + collector.laborDemandForRandD;
		collector.unemployment 				= laborSupply - collector.laborDemand;
		collector.unemploymentRate[1] 					= collector.unemployment / laborSupply;
		
		log.fatal("LABOR MARKET: " +
				"\n labor demand " + collector.laborDemand + 
				"\n unemployment " + collector.unemployment + 
				"\n u. rate " + collector.unemploymentRate[1]);
	}
	
	void marketCompetitiveness(){
		
		/* Compute the mean price and the mean competitiveness to use in equation (24), Dosi et al. (2013) 
		NOTE: normalize the price and the competitiveness. */
		collector.competitivenessAggregate();
		log.fatal("Competitiveness aggregate variable: " + 
					"\n Mean price " + collector.meanPrice_cFirms + 
					"\n Mean unfilled demand " + collector.meanUnfilledDemand);
		
		// Normalize the market share, as some firms have already exited the economy (cf. myopicExpendituresUpdate)
		collector.marketShareNormalization();
		
		// Compute the firms' competitiveness & the mean competitiveness
		collector.meanCompetitiveness_cFirms[1] 				= 0;
		for(CFirm cFirm : cFirms){
			// Equation (24) in Dosi et al. (2013)
			cFirm.competitiveness 						= - Parameters.getCoeffPriceCompetitiveness_cFirms() * cFirm.priceOfGoodProduced[1] / collector.meanPrice_cFirms - 
											Parameters.getCoeffUnfilledDemandCompetitiveness_cFirms() * cFirm.unfilledDemand / collector.meanUnfilledDemand;
			// Equation (24.5) in Dosi et al. (2013)
			collector.meanCompetitiveness_cFirms[1] 			+= cFirm.competitiveness * cFirm.marketShare[1];
			
		}
		
		// Compute the new firms' market share. Firms with too-low market share exit
		// NOTE: careful, chi = -1 (even though in their calibration table in Dosi et al. (2013), chi = 1).
		for(CFirm cFirm : cFirms){
			cFirm.marketShare[2] 						= cFirm.marketShare[1] * (1 + Parameters.getChi_cFirms() * 
											( cFirm.competitiveness - collector.meanCompetitiveness_cFirms[1] ) / collector.meanCompetitiveness_cFirms[1] );
			
			if(cFirm.marketShare[2] < Parameters.getMarketShareThresholdForExit_cFirms()){
				
				cFirm.exit 					= true;
				cFirm.marketShare[0] 					= 0;
				cFirm.marketShare[1] 					= 0;
				cFirm.marketShare[2] 					= 0;
				
				// Update its financial variables
				if(cFirm.debt[1] > cFirm.liquidAsset[1]){
					if(cFirm.liquidAsset[1] > 0){
						cFirm.debt[1] 		-= cFirm.liquidAsset[1];
						bank.debt 			-= cFirm.liquidAsset[1];
						cFirm.liquidAsset[1]= 0;
					}
					cFirm.badDebt			= cFirm.debt[1];
					bank.badDebt 			+= cFirm.debt[1];
				} else {
					bank.debt				-= cFirm.debt[1];
					cFirm.liquidAsset[1] 	-= cFirm.debt[1];
					cFirm.debt[1]			= 0;
					cFirm.badDebt			= 0;
				}
			}
		}
		
		// Some new firms may have exited the market. Re-normalize the firms' market shares
		collector.marketShareNormalization();
	}
	
	void consumptionAllocation(){
		
		// 1. Compute aggregate consumption
		collector.aggregateConsumption();
		collector.priceIndexes();
		collector.realConsumption 			= collector.consumption[1] / collector.cpi[1];
		
		/* 2. Allocate total consumption across the different consumption-good firms
		It might be that once all firms received their corresponding demand (real consumption * market share), 
		the stock of consumption is still positive, while some firms still have some goods in their stocks. This is why
		we iterate over firms until either real consumption or the total stock of goods reach zero.
		
		NOTE: the unfilled demand of a firm is determined only in the first iteration */
		
		// Temporary variable used to stop the allocation the process  when all firms run out of goods  
		double fTotTemp 					= 0;
		for(CFirm cFirm : cFirms){
			fTotTemp 						+= cFirm.marketShare[2];
		}
		 
		this.consumptionTemp 				= 0; 
		
		// Temporary variable used to specify if it is the first round of the allocation process
		int i 								= 1;
		
		// Allocation process
		// NOTE: use collector.realConsumption >= 1 as a gross approximation of collector.realConsumption > 0 in order to
		// prevent the loop to run forever  
		while(fTotTemp > 0 && collector.realConsumption >= 1){
			this.consumptionTemp 			= collector.realConsumption;
						
			// Compute the respective demand for every firms
			for(CFirm cFirm : cFirms)
				cFirm.demand(i);
			
			// Compute whether there is still firms in the market
			fTotTemp 						= 0;
			for(CFirm cFirm : cFirms)
				fTotTemp 					+= cFirm.marketShareTemp;
			if(fTotTemp > 0){
				for(CFirm cFirm : cFirms)
					cFirm.marketShareTemp 			/= fTotTemp;
			}
			
			collector.realConsumption 		= consumptionTemp;
			i 								+= 1;
		}
		
		// The past consumption is the real consumption that had not been met in the allocation process
		collector.pastConsumption 			= collector.realConsumption;
		if(collector.pastConsumption < 0){
			collector.pastConsumption 		= 0;
			log.error("Past consumption < 0: " + collector.pastConsumption);
		}
		
	}
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public Double getEndTime() {
		return endTime;
	}

	public void setEndTime(Double endTime) {
		this.endTime = endTime;
	}

	public Integer getNumberOfCFirms() {
		return numberOfCFirms;
	}

	public void setNumberOfCFirms(Integer numberOfCFirms) {
		this.numberOfCFirms = numberOfCFirms;
	}

	public List<CFirm> getCFirms() {
		return cFirms;
	}

	public void setCFirms(List<CFirm> cFirms) {
		this.cFirms = cFirms;
	}
	
	public List<KFirm> getKFirms() {
		return kFirms;
	}

	public void setKFirms(List<KFirm> kFirms) {
		this.kFirms = kFirms;
	}
	
	public Integer getNumberOfKFirms() {
		return numberOfKFirms;
	}

	public void setNumberOfKFirms(Integer numberOfKFirms) {
		this.numberOfKFirms = numberOfKFirms;
	}

	public Double getReserveRequirementRate() {
		return reserveRequirementRate;
	}

	public void setReserveRequirementRate(Double creditMultiplier) {
		this.reserveRequirementRate = creditMultiplier;
	}

	public Double getLoanToValueRatio() {
		return loanToValueRatio;
	}

	public void setLoanToValueRatio(Double loanToValueRatio) {
		this.loanToValueRatio = loanToValueRatio;
	}

	public Double getUnemploymentBenefitShare() {
		return unemploymentBenefitShare;
	}

	public void setUnemploymentBenefitShare(Double unemploymentBenefitShare) {
		this.unemploymentBenefitShare = unemploymentBenefitShare;
	}

	public Double getTaxRate() {
		return taxRate;
	}

	public void setTaxRate(Double taxRate) {
		this.taxRate = taxRate;
	}

	public Double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(Double interestRate) {
		this.interestRate = interestRate;
	}

	public Long getSeedIfFixed() {
		return seedIfFixed;
	}

	public void setSeedIfFixed(Long seedIfFixed) {
		this.seedIfFixed = seedIfFixed;
	}

	public Double getMarkUpRate() {
		return markUpRate;
	}

	public void setMarkUpRate(Double markUpRate) {
		this.markUpRate = markUpRate;
	}

	public Bank getBank() {
		return bank;
	}

	public boolean isMyopicDebtRepayment() {
		return myopicDebtRepayment;
	}

	public void setMyopicDebtRepayment(boolean myopicDebtRepayment) {
		this.myopicDebtRepayment = myopicDebtRepayment;
	}

	public static double getrDepo() {
		return returnOnFirmsDeposits;
	}

	public static double getrDebt() {
		return interestRateOnDebt;
	}

	public static double getrCb() {
		return returnOnBankDepositsAtCentralBank;
	}

	public static double getLabourSupply() {
		return laborSupply;
	}

	public List<KFirm> getkFirms() {
		return kFirms;
	}

	public MacroCollector getCollector() {
		return collector;
	}

	public void setCollector(MacroCollector collector) {
		this.collector = collector;
	}

	public static double getTax() {
		return tax_rate;
	}

	public boolean isMason() {
		return mason;
	}

	public void setMason(boolean mason) {
		this.mason = mason;
	}

	public boolean isFixRandomSeed() {
		return fixRandomSeed;
	}

	public void setFixRandomSeed(boolean fixRandomSeed) {
		this.fixRandomSeed = fixRandomSeed;
	}

}