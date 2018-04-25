package jasmine.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jasmine.algorithms.MapSorting;
import jasmine.data.Parameters;
import jasmine.object.*;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

@Entity
public class KFirm extends Firm {
	
	private final static Logger log = Logger.getLogger(KFirm.class);
	
	@Id
	private PanelEntityKey key;

	static long kFirmIdCounter = 0l;
	
	@Transient
	BalanceSheetKFirm balanceSheet;
	@Transient
	Set<CFirm> clients; // list of clients the KFirm has
	@Transient
	Map<CFirm, Long> bookOrder; // collects the order of the firm's clients
	@Transient
	Machine machineProduced;

	double laborDemandForProduction; // labor demand only for production purpose. Note: need to separate labour demand because when labor rationing occurs, it
	// affects only the labor demand for the production of machines, as the R&D labor demand has already taken place at the beginning of the period

	// R&D activity
	double laborDemandForRd; // labor demand only for R&D purpose 

	@Transient
	protected double[] firmProductivity; // prod stands for productivity, was prod.

	protected double rdExpenditure; // r&d expenditures, was rd.

	protected double aInn; // machine's prod. resulting from innovation, was aInn.

	protected double aIm; // machine's prod. resulting from imitation, was aIm.

	protected double bInn; // firm's prod. resulting from innovation, was bInn.

	protected double bIm; // firm's prod. resulting from imitation, was bIm.

	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
		Update,
		Research,
		Brochure,
		LaborDemand,
		MachineProduction,
		Accounting;
	}

	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case Update:
			update();
			break;
		
		case Research:
			research();
			break;
			
		case Brochure:
			brochure();
			break;
			
		case LaborDemand:
			laborDemand();
			break;
			
		case MachineProduction:
			machineProduction();
			break;
			
		case Accounting:
			accounting();
			break;
		}
	}

	
	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------
		
	//Default constructor, used when exporting data to database.
	public KFirm() {
		super();
	}
	
	//Constructor for brand new KFirm (for copying existing firm, see copy constructor below)
	/* Note that the boolean argument newFirm is a dummy variable used only to distinguish this constructor from 
	 * the default constructor, which is used by the database export process (in which we do not want to increment 
	 * the id counter, otherwise it will increment the counter without actually creating new firms in the model).
	 * The actual value of the boolean newFirm is ignored. 
	 */
	public KFirm(boolean newFirm){
		super(true);
		// Extend the Firm constructor (define the firms' debt level)		
		
		this.key = new PanelEntityKey(kFirmIdCounter++);
		
		// --- Customers --- 
		// Initial clients are defined in matching(), MacroModel.java
		this.clients 					= new LinkedHashSet<CFirm>();
		this.bookOrder 					= new LinkedHashMap<>();
		
		// --- Machine produced ---
		Machine machine 				= new Machine(this); 
		machine.setMachineProductivity(new double[]{Parameters.getInitialProductivity(), Parameters.getInitialProductivity()}); 
		machine.setCost(Parameters.getInitialWage() / Parameters.getInitialProductivity()); 
		machine.setVintage(0); 
		this.machineProduced 			= machine; 
		
		// --- Productivity ---
		this.firmProductivity 						= new double[]{Parameters.getInitialProductivity(), Parameters.getInitialProductivity()}; 
		this.costToProduceGood 							= Parameters.getInitialWage() / (firmProductivity[1] * Parameters.getA_kFirms()); 
		this.priceOfGoodProduced 							= new double[]{ (1 + Parameters.getFixedMarkUp_kFirms()) * costToProduceGood, (1 + Parameters.getFixedMarkUp_kFirms()) * costToProduceGood}; 
		
		// --- Financial variables --- 
		this.liquidAsset = new double[]{Parameters.getNetLiquidAssets_kFirms(), Parameters.getNetLiquidAssets_kFirms()}; 
		
		// --- Competitiveness ---
		this.marketShare 							= new double[]{1 / ((double) model.getNumberOfKFirms()), 
											1 / ((double) model.getNumberOfKFirms()), 1 / ((double) model.getNumberOfKFirms())}; 
		// --- Production ---
		this.productionQuantity 							= 0;  
		this.size = 0; 
		// Initial sales determine the amount of initial investment in R&D
		CFirm aFirm 					= model.getCFirms().get(0);
		double sales0 					= ( ( ( aFirm.investment / Parameters.getMachineSizeInCapital_cFirms() ) * ( (double) model.getNumberOfCFirms() ) ) /
												( (double) model.getNumberOfKFirms() ) ) * priceOfGoodProduced[1];
		this.sales 						= new double[]{sales0, sales0}; 
		this.demand 					= new double[]{sales0 / priceOfGoodProduced[1], sales0 / priceOfGoodProduced[1]};
				
		// --- R&D ---
		this.rdExpenditure 						= Parameters.getFractionPastSalesInvestedInRandD_kFirms() * sales0; 
		this.laborDemandForRd 				= rdExpenditure / Parameters.getInitialWage();
		
		// --- Balance sheet object. Records the firm's expenditures and revenues. 
		this.balanceSheet = new BalanceSheetKFirm(this, model);
		balanceSheet.setDepositRevenues(liquidAsset[1] * MacroModel.returnOnFirmsDeposits);
		balanceSheet.setLiquidAsset(liquidAsset[1]);
		balanceSheet.setSales(sales0);
		balanceSheet.setWageBill(0);
		
	}

	
	// Constructor that creates a firm as copy of an already existing one
	public KFirm(KFirm copy){
		/* Unlike the creation of new consumption-good firms, less variables need to be copied for capital-good firms. The number of
		 inconsistencies is thus reduced. The problems are only:
		 		(a) New entrant needs a positive level of sales to have positive R&D in the first period
		 		(b) The amount of liquid asset the new entrant possesses is still copied from the incumbent firm
		 */
		
		super(true);
		this.key = new PanelEntityKey(kFirmIdCounter++);
		
		this.exit 						= false;
		this.newEntrant 				= true;
			
		// --- Customers ---
		this.clients 					= new LinkedHashSet<CFirm>();
		this.bookOrder 					= new LinkedHashMap<>();
		
		// --- Machines --- 
		this.rdExpenditure 						= copy.rdExpenditure;
		this.machineProduced 			= new Machine(this);
		this.machineProduced.setMachineProductivity(copy.machineProduced.getMachineProductivity().clone());
		this.machineProduced.setCost(copy.machineProduced.getCost());
		
		// --- Productivity ---
		this.firmProductivity 						= new double[]{copy.firmProductivity[0], copy.firmProductivity[1]};
		this.priceOfGoodProduced 							= new double[]{copy.priceOfGoodProduced[0], copy.priceOfGoodProduced[1]};
		this.costToProduceGood 							= copy.costToProduceGood;
		this.marketShare 							= new double[]{0, 0, 0};
		
		// --- Production --- 
		this.demand 					= new double[]{0, 0};
		this.size 						= 0;
		double sales0 					= priceOfGoodProduced[1] * model.getNumberOfCFirms() / model.getNumberOfKFirms();
		this.sales 						= new double[]{sales0, sales0};
		
		// --- Financial variables --- 
		this.liquidAsset 				= new double[]{copy.liquidAsset[0], copy.liquidAsset[1]}; // **
		this.balanceSheet 				= new BalanceSheetKFirm(this, model);
		
	}

	
	
	// ---------------------------------------------------------------------
	// Exit methods
	// ---------------------------------------------------------------------
	
	void survivalChecking(){
		// Identify whether the firm can survive 
		if(liquidAsset[1] > 0 && clients.size() > 0){
			collector.kFirmsRemaining	+= 1;
			this.newEntrant 			= false;
		} else {
			this.exit 					= true;
			collector.exit_kFirms 			+= 1;
		}
	}
	
	/* the kill() method removes all references of the firm to other objects. This helps to boost the performance of the simulations, 
	 by reducing the number of objects in the memory. */
	public void kill(){
		
		super.kill();
				
		this.balanceSheet.kill();
		this.balanceSheet 				= null;
		
		// --- Entities ---
		this.machineProduced.kill();
		this.machineProduced 			= null;
		
		// --- Lists & Maps ---
		this.clients 					= null;
		this.bookOrder 					= null;
		
		
	}
	
	
	
	// ---------------------------------------------------------------------
	// Update methods
	// ---------------------------------------------------------------------

	public Machine factory(){
		
		Machine machineSold 			= machineProduced.clone();
		return machineSold;
		
	}
	
	public void update(){
		
		super.update();
		// Set the firm's sales equal to 0 because they are increased incrementally from the CFirm class 
		this.sales[1] 					= 0;
		this.machineProduced.update(); // update a(t) = a(t-1)
		this.firmProductivity[0] 					= firmProductivity[1];
		
	}
	
	// ---------------------------------------------------------------------
	// Research methods
	// ---------------------------------------------------------------------
	
	void research(){
		
		/* Define low level of productivity before the innovation & imitation process, s.t. even if they fail, these variables
		will still be defined -- and we will not divide by 0 afterwards. */
		this.aIm 						= 0.00001;
		this.aInn 						= 0.00001;
		this.bIm 						= 0.00001;
		this.bInn 						= 0.00001;
		
		// Compute the amount of resources spent on R&D activities 
		if(sales[0] > 0){
			// Equation (14) in Dosi et al. (2013)
			this.rdExpenditure 					= Parameters.getFractionPastSalesInvestedInRandD_kFirms() * sales[0]; 
		} // else: remains identical
		this.liquidAsset[1] 			-= rdExpenditure; 
		
		// These resources are used to pay workers
		//TODO: potentially to remove balanceSheet.increaseWageBill(rd);
		this.laborDemandForRd 				= rdExpenditure / collector.wage[1];
		this.laborDemand 				+= laborDemandForRd;
				
		// This labor demand is divided between innovation and imitation activities. Equation (14.5) in Dosi et al. (2013)
		double rdInnovation 			= laborDemandForRd * Parameters.getShareInnovationInRandD_kFirms();
		double rdImitation 				= laborDemandForRd * (1 - Parameters.getShareInnovationInRandD_kFirms());
				
		// The firm launches then its innovation and imitation process
		if(rdInnovation > 0)
			innovation(rdInnovation);
		if(rdImitation > 0)
			imitation(rdImitation);
				
		// Compare the attractiveness of the innovation and imitation outcome with the current machine. 
		// Equation (17) in Dosi et al. (2013)
		double realPriceInn 			= (1 + Parameters.getFixedMarkUp_kFirms()) * collector.wage[1] / (bInn * Parameters.getA_kFirms()) + 
											Parameters.getMachinePaybackPeriod_cFirms() * collector.wage[1] / aInn;
		double realPriceIm 				= (1 + Parameters.getFixedMarkUp_kFirms()) * collector.wage[1] / (bIm * Parameters.getA_kFirms()) + 
											Parameters.getMachinePaybackPeriod_cFirms() * collector.wage[1] / aIm;
		double realCurrentPrice 		= (1 + Parameters.getFixedMarkUp_kFirms()) * collector.wage[1] / (firmProductivity[0] * Parameters.getA_kFirms()) + 
											Parameters.getMachinePaybackPeriod_cFirms() * collector.wage[1] / machineProduced.getMachineProductivity()[0];
		
		// Choose the machine with the lowest "real price"
		if( realPriceIm < realCurrentPrice ){
			this.machineProduced.setMachineProductivity(new double[]{machineProduced.getMachineProductivity()[0], aIm});
			this.firmProductivity[1] 				= bIm;
		}
		
		if( realPriceInn < realCurrentPrice ){
			this.machineProduced.setMachineProductivity(new double[]{machineProduced.getMachineProductivity()[0], aInn});
			this.firmProductivity[1] 				= bInn;
			// if the new machine is the result of an innovation, then it is considered as a new vintage 
			this.machineProduced.setVintage(machineProduced.getVintage() + 1);			
		} 
		
		// Update the machine's labor productivity 
		if(machineProduced.getMachineProductivity()[1] > 0)
			// Equation (13.5) in Dosi et al. (2013)
			machineProduced.setCost(collector.wage[1] / machineProduced.getMachineProductivity()[1]); 
		else 
			log.error("Machine's production <= 0");
		
		// Update the firm's cost of production
		if(firmProductivity[1] > 0){
			// Equation (12) in Dosi et al. (2013)
			this.costToProduceGood 						= collector.wage[1] / (Parameters.getA_kFirms() * firmProductivity[1]); // eq. (12) in Dosi et al. (2013)
		} else{
			// This should not happen theoretically.
			this.firmProductivity[1] 				= 0.00001; 
			log.error("KFirm's prod <= 0");
		}
		
		// Update the firm's price. Equation (13) in Dosi et al. (2013)
		this.priceOfGoodProduced[1] 						= (1 + Parameters.getFixedMarkUp_kFirms()) * costToProduceGood; 
		if(priceOfGoodProduced[1] < Parameters.getMinPrice_kFirms())
			this.priceOfGoodProduced[1] 					= Parameters.getMinPrice_kFirms();
				
	}
	
	void innovation(double rdInnovation){
		
		// Equation (15) in Dosi et al. (2013)
		double paramBernoulli 					= 1 - Math.exp( - Parameters.getZeta1_Innovation_kFirms() * rdInnovation);
		if(paramBernoulli == 1)
			paramBernoulli 						= 0.99999; 
	
		// The firm draws whether it can access the innovation phase  
		BinomialDistribution berDistribution 	= new BinomialDistribution((RandomGenerator) SimulationEngine.getRnd(), 1, paramBernoulli);
		if(berDistribution.sample() == 1){ 
			// The potential discovery is a random sample from a beta distribution with parameters alpha1 and beta1
			BetaDistribution betaDistribution 	= new BetaDistribution((RandomGenerator) SimulationEngine.getRnd(), Parameters.getAlpha1_Innovation_kFirms(), Parameters.getBeta1_Innovation_kFirms(), BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
			
			// Machine productivity. Then scale the random variable s.t. it belongs to the appropriate support 
			double innovation 					= betaDistribution.sample();
			innovation 							= Parameters.getX1lower_Innovation_kFirms() + innovation * 
													( Parameters.getX1upper_Innovation_kFirms() - Parameters.getX1lower_Innovation_kFirms() );
			
			// Equation (15.5) in Dosi et al. (2013)
			this.aInn 							= machineProduced.getMachineProductivity()[0] * (1 + innovation);
			
			// Firm's productivity. Then scale the random variable s.t. it belongs to the appropriate support 
			innovation 							= betaDistribution.sample();
			innovation 							= Parameters.getX1lower_Innovation_kFirms() + innovation * 
													( Parameters.getX1upper_Innovation_kFirms() - Parameters.getX1lower_Innovation_kFirms() );
			
			// Equation (15.5) in Dosi et al. (2013)
			this.bInn 							= firmProductivity[0] * (1 + innovation);
		} 
	}
	
	void imitation(double rdImitation){
		
		// Equation (16) in Dosi et al. (2013)
		double paramBernoulli 					= 1 - Math.exp( - Parameters.getZeta2_Imitation_kFirms() * rdImitation);
		if(paramBernoulli == 1)
			paramBernoulli 						= 0.99999; 
		
		// The firm draws whether it can access the imitation phase  
		BinomialDistribution berDistribution = new BinomialDistribution((RandomGenerator) SimulationEngine.getRnd(), 1, paramBernoulli);
		if(berDistribution.sample() == 1){
			/* INTRODUCTORY NOTES: the weights, and the final imitation, are constructed based on (t-1) machines. Otherwise, some firms
			could imitate the outcome of other firms that had already done their imitation process etc. */
			
			// List attributing a weight to each capital-good firm, based on its Euclidian distance with this capital-good firm
			List<Pair<KFirm, Double>> weightList = new LinkedList<>(); 
			double sum 							= 0;
			
			for(KFirm kFirm : model.getKFirms()){
				
				double prodOther 				= kFirm.firmProductivity[0];
				double prodMachineOther 		= kFirm.machineProduced.getMachineProductivity()[0];
				// Compute the Euclidian distance 
				double weight 					= Math.sqrt( (firmProductivity[0] - prodOther) * (firmProductivity[0] - prodOther) + 
													(machineProduced.getMachineProductivity()[0] - prodMachineOther) * 
													(machineProduced.getMachineProductivity()[0] - prodMachineOther) );
				// Inverse the Euclidian distance
				if(weight > 0)
					weight 						= 1 / weight;
				else 
					weight 						= 0;
				
				weightList.add(new Pair<KFirm, Double>(kFirm, weight));
				sum 							+= weight; 
			}
			
			if(sum > 0){
				EnumeratedDistribution<KFirm> kFirmDistribution = new EnumeratedDistribution<>((RandomGenerator) SimulationEngine.getRnd(), weightList);
				KFirm firmSelected 				= kFirmDistribution.sample(); // pick randomly a firm from the list 
				this.aIm 						= firmSelected.machineProduced.getMachineProductivity()[0];
				this.bIm 						= firmSelected.firmProductivity[0];
			} // else, there is absolutely no differences between firms. Firms cannot imitate.
		}
	}
	
	// ---------------------------------------------------------------------
	// Brochure methods
	// ---------------------------------------------------------------------
	
	void brochure(){
		// Capital-good firms send brochure to their current clients. New entrants have potential clients in their clients list.
		for(CFirm cFirm : clients){
			cFirm.potentialKfirmsSet.add(this);
		}
		
		// Capital-good firms can also reach out to new clients, randomly chosen 
		int numberOfNewBrochures 				= (int) Math.round(clients.size() * Parameters.getShareNewClientsPerPeriod_kFirms());
		
		if(numberOfNewBrochures == 0){
			// Following Dosi et al. implementation, capital-good firms can always reach at least one new clients
			numberOfNewBrochures 				= 1; 
		}
		
		// The firm's number of potential clients is bounded above by the total number of consumption-good firms
		if(numberOfNewBrochures + clients.size() > model.getNumberOfCFirms()){
			numberOfNewBrochures 				= model.getNumberOfCFirms() - clients.size();
		}
		
		if(numberOfNewBrochures < 0){
			log.error("numberOfNewBrochures is neg.: " + numberOfNewBrochures);
			numberOfNewBrochures 				= 0;
		}
		
		// Create a list storing all potential clients so that a firm will not send twice a brochure to the same potential clients
		List<CFirm> listPotentialClients 		= new LinkedList<>();
		
		// Send the brochures 
		while(numberOfNewBrochures > 0){
			int rnd 							= SimulationEngine.getRnd().nextInt(model.getNumberOfCFirms());
			CFirm potentialClient 				= model.getCFirms().get(rnd);
			// The potential client cannot be an actual client, nor a potential client already in the list 
			if(!clients.contains(potentialClient) && (listPotentialClients.isEmpty() || !listPotentialClients.contains(potentialClient))){
				potentialClient.potentialKfirmsSet.add(this);
				listPotentialClients.add(potentialClient);
				numberOfNewBrochures			-= 1;
			}
		}
		
		// The list of actual clients is cleared and will be filled incrementally from the CFirm class
		clients.clear();
	}
	
	// ---------------------------------------------------------------------
	// Labor market methods
	// ---------------------------------------------------------------------
	
	void laborDemand(){
		
		// k-firms compute their demand by iterating over their books of commands 
		this.demand[1] 							= 0;
		
		for(Map.Entry<CFirm, Long> entry : bookOrder.entrySet()){
			this.demand[1] 						+= entry.getValue();
		}
		// Capital-good firms produce on demand; thus production = demand
		this.productionQuantity 									= this.demand[1];
		
		// Labor demand for the production of machines is added to the R&D labor demand
		if(firmProductivity[1] > 0)
			this.laborDemandForProduction 				= productionQuantity / (firmProductivity[1] * Parameters.getA_kFirms());
		else {
			log.fatal("ERROR: prod of KFirm < 0 when computing labor demand");
		}
		this.laborDemand 						+= laborDemandForProduction;
		
	}
	
	public void laborRationing(double ratio){
		/* The firm is labor rationed. It reduces its labor demand, and its production accordingly 
		 Because the production of the firm is equal to the investments of its clients, the latter have to be reduced
		
		NOTE: because investments are rounded down, the sum of the new investments may be smaller than what
		can actually be produced by the capital-good firm, i.e. there are "leftovers". 
		Personal assumption: in this situation, the firm ranks its clients from the biggest one to the smallest one 
		in terms of their order, and "distribute those leftovers" until they become nil.  */
				
		double pastQ		 					= productionQuantity;
		
		if(pastQ > 0){
			
			// Update production and labor demand with the labor ratio
			this.productionQuantity 								= Math.floor(productionQuantity * ratio); 
			this.laborDemandForProduction 				= productionQuantity / (firmProductivity[1] * Parameters.getA_kFirms());
			this.laborDemand 					= laborDemandForProduction + laborDemandForRd;
			
			// Compute the quantity sold when rounding down the rationed investments of each clients
			double totalRationedDemand 			= 0;
			for(Map.Entry<CFirm, Long> entry : bookOrder.entrySet()){
				CFirm client 					= entry.getKey();
				totalRationedDemand 			+= Math.floor( ( client.investment / Parameters.getMachineSizeInCapital_cFirms() ) * productionQuantity / pastQ );
			}
			
			// Compute the difference between the production and the rounded down demand
			double remainingQuantity 			= productionQuantity - totalRationedDemand;
			if(remainingQuantity < 0)
				log.error("Remaining qty is negative: " + remainingQuantity);
			
			if(remainingQuantity == 0){
				// Each clients receives the same proportional reduction in its investments
				
				for(Map.Entry<CFirm, Long> entry : bookOrder.entrySet()){
					
					CFirm client 				= entry.getKey();
					// Compute the new level of investment 
					client.investment 					= Math.floor( (client.investment / Parameters.getMachineSizeInCapital_cFirms()) * productionQuantity / pastQ) * 
												Parameters.getMachineSizeInCapital_cFirms();
					
					/* Consumption-good firms prefer expansionary over substitutionary investments. If the new level 
					of investment is lower than the level of expansionary investment, then the entire investments 
					expenditures are devoted to expansionary ones */
					if(client.investment < client.investmentExpansionary)
						client.investmentExpansionary 			= client.investment;
					client.investmentSubstitutionary 				= client.investment - client.investmentExpansionary;
					
					// Update the investments in the firm's book
					long numberOfMachine 		= (long) client.investment / (long) Parameters.getMachineSizeInCapital_cFirms();
					entry.setValue(numberOfMachine);
				}
			} else {
				/* If the firm has leftover to distribute, it
					(a) first applies the same proportional reduction to all its clients
					(b) distributes the leftovers, starting from its biggest clients */
				
				// Rank the clients 
				bookOrder 						= (LinkedHashMap<CFirm, Long>) MapSorting.sortByValueDescending(bookOrder); 
				
				// Apply the same proportional reduction to all clients + distribute the leftovers 
				for(Map.Entry<CFirm, Long> entry : bookOrder.entrySet()){
					
					CFirm client 				= entry.getKey();
					// Compute the new level of investment 
					double invRounded 			= Math.floor( (client.investment / Parameters.getMachineSizeInCapital_cFirms()) * productionQuantity / pastQ) * Parameters.getMachineSizeInCapital_cFirms();
					// Compute the difference with its initial level of investment. De facto diffInv is also a 
					// multiple of DimK
					double diffInv 				= client.investment - invRounded;
					
					// Apply the proportional ratio
					client.investment 					= invRounded;
					// Distribute the leftovers, if any
					if(remainingQuantity > 0){
						if(diffInv > remainingQuantity * Parameters.getMachineSizeInCapital_cFirms()){
							client.investment 			+= remainingQuantity * Parameters.getMachineSizeInCapital_cFirms();
							remainingQuantity 	= 0;
						} else {
							remainingQuantity 	-= diffInv / Parameters.getMachineSizeInCapital_cFirms();
							client.investment 			+= diffInv;
						}
					}
					
					/* Consumption-good firms prefer expansionary over substitutionary investments. If the new level 
					of investment is lower than the level of expansionary investment, then the entire investments 
					expenditures are devoted to expansionary ones */
					if(client.investment < client.investmentExpansionary)
						client.investmentExpansionary 			= client.investment;
					client.investmentSubstitutionary 				= client.investment - client.investmentExpansionary;
					
					// Update the value in the k-firms' books
					long numberOfMachine 		= (long) (client.investment / Parameters.getMachineSizeInCapital_cFirms());
					entry.setValue(numberOfMachine);
				}
			}			
		}
	}
	
	// ---------------------------------------------------------------------
	// Capital market methods
	// ---------------------------------------------------------------------
	
	void machineProduction(){
		// The firm produces the machines and delivers them to its clients, that pay it in exchange 
		if(!bookOrder.isEmpty()){		//XXX: Isn't this an unnecessary check?
			for(Map.Entry<CFirm, Long> entry : bookOrder.entrySet()){
				
				machineDelivery(entry.getKey());
				entry.getKey().machinePayment();
				
			}
		} 
		// The delivery is finished. The firm clears its books
		bookOrder.clear();
	}
	
	void machineDelivery(CFirm client){
		
//		int numberOfMachine 					= (int) client.inv / (int) Parameters.getDimK();
		int numberOfMachine 					= (int) (client.investment / Parameters.getMachineSizeInCapital_cFirms());
		Machine machineDelivered 				= factory();
		
		// Delivers the machine
		client.machineAgeMap.put(machineDelivered, 0);
		client.machineQuantityMap.put(machineDelivered, numberOfMachine);
		
	}
	
	// ---------------------------------------------------------------------
	// Accounting methods
	// ---------------------------------------------------------------------
	
	void accounting(){
		/* The firm computes its profit. 
		NOTE: because the monetary base is made only of the consumption-good firms, the capital-good firms do not get
		a return on their deposit. */ 
		this.profit 							= sales[1] - costToProduceGood * productionQuantity - rdExpenditure;
		this.liquidAsset[1] 					= liquidAsset[0];	
		
		if(profit > 0){
			collector.govRevenues 				+= profit * model.taxRate; 
			this.liquidAsset[1] 				-= profit * model.taxRate;
		}

		/* NOTE: R&D expenditures were already deduced from the liquid asset of the firm in the R&D method. 
		Add it there in order to not count it twice */ 
		this.liquidAsset[1] 					+= profit + rdExpenditure;
		
		/* NOTE: In Dosi et al. implementation of their model, capital-good firms cannot die from financiary motives */
		if(liquidAsset[1] <= 0){
			this.liquidAsset[1] 				= 1;
		}
		
		this.size 								= productionQuantity;
	}
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------


	public BalanceSheetKFirm getBalanceSheet() {
		return balanceSheet;
	}

	public void setBalanceSheet(BalanceSheetKFirm balanceSheet) {
		this.balanceSheet = balanceSheet;
	}

	public Set<CFirm> getClients() {
		return clients;
	}

	public void setClients(Set<CFirm> clients) {
		this.clients = clients;
	}

	public Machine getMachineProduced() {
		return machineProduced;
	}
	
	public double getMachineProductivity(){
		return machineProduced.getMachineProductivity()[1];
	}

	public void setMachineProduced(Machine machineProduced) {
		this.machineProduced = machineProduced;
	}

	public double getRdExpenditure() {
		return rdExpenditure;
	}

	public void setRdExpenditure(double rdExpenditure) {
		this.rdExpenditure = rdExpenditure;
	}

	public double getLaborDemandForRd() {
		return laborDemandForRd;
	}
	
	public double getLaborDemandForProduction() {
		return laborDemandForProduction;
	}

	public double[] getFirmProductivity() {
		return firmProductivity;
	}
	
	public double getFirmProductivityNow() {
		return firmProductivity[1];
	}

	public void setFirmProductivity(double[] productivity) {
		this.firmProductivity = productivity;
	}

	public PanelEntityKey getKey() {
		return key;
	}

}