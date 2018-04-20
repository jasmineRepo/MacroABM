package jasmine.model;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jasmine.algorithms.*;
import jasmine.data.Parameters;
import jasmine.enums.DebtManagement;
import jasmine.enums.DebtRepayment;
import jasmine.object.*;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

@Entity
public class CFirm extends Firm {

	private final static Logger log = Logger.getLogger(CFirm.class);
	
	@Id
	private PanelEntityKey key;

	static long cFirmIdCounter = 0l;
	
	// here are all the variables specific to the c-forms
	@Transient
	BalanceSheetCFirm balanceSheet;
	
	@Transient
	Set<KFirm> potentialKfirmsSet; // set of KFirm offers received by the firm.  These are the potential capital machine suppliers that the cFirms can use.  Was brochureLists. 
	
	@Transient
	Map<Machine, Integer> machineAgeMap; // map which associates an age to each machine of the firm's machine set 
	
	@Transient
	Map<Machine, Integer> machineQuantityMap; // map which gives the number of machines associated with the type of machine in the firm's machine set 
	
	@Transient
	Map<Machine, Integer> machinesToBeScrappedMap; /* map surveying the machines that c-firms initially want to scrap, either because they are too old, or
	 because they are not sufficiently productive. NOTE: such map is needed because it is possible that, eventually, c-firms will not scrap some of the machines
	 if it was not able to invest in machines to replace them.  */

	@Transient
	KFirm supplier;

	protected double productivity; // prod stands for productivity, was prod. 
	
	@Transient
	protected double[] markUpRate; // mark up, was mu.

	protected double unfilledDemand;

	protected double capitalStock; // capital stock, was k.

	protected double competitiveness; // competitiveness, was e.

	protected double investment; // investment, was inv.

	@Column(name = "Expansionary_Investment")
	protected double investmentExpansionary; // expansionary investment, was invExp. 

	protected double investmentSubstitutionary; // substitutionary investment, was invSub.

	@Transient
	protected double[] inventories; /* inventories, was n. An array is needed to compute the difference between past inventories and present one, 
	in order to compute the gdp, see equation (11) in Dosi et al. (2013) */

	protected double stockFinalGood;

	protected double grossOperatingSurplus; // gross operating surplus, was gos.

	protected double marketShareTemp; /* was fTemp.  Variable used temporary to stock the market share of c-firms while allocating
	the total stock of consumption among the different firms. Cf. MacroModel class, consumptionAllocation method. */
	
	// desired variables

	protected double expectedDemand; // expected demand, was eD.

	protected double desiredProduction; // desired production, was dQ.

	protected double desiredProductionStar; // desired production updated after checking that production plans are feasible, w/out knowing the loan the firm will receive, was dQStar.

	protected double optimalProduction; // `optimal` production after adjusting and receiving the loan, was qStar.

	protected double desiredInvestmentExpansionary; // desired expansionary investment; the two other following variables follow the same notation as for production (see dQStar and qStar), was dInvExpStar.

	protected double desiredInvestmentExpansionaryStar;	//dInvExpStar.

	protected double investmentExpansionaryStar;	//invExpStar.

	protected double desiredInvestmentSubstitutionary; // desired substitutionary investment, was dInvSub. 

	protected double desiredInvestmentSubstitionaryStar; 	//was dInvSubStar.

	protected double investmentSubstitionaryStar;	//was invSubStar.

	protected double desiredInventories; // desired inventories, was dN.

	protected double desiredCapitalStock; // desired capital, was dK.

	protected double maxPossibleCapitalStock; // TODO: can remove it, and put it back to a local variable after checking, was kTop.
	
	// financial variables

	protected double creditDemand; 

	protected double netWorthToSalesRatio; 

	protected double maxPossibleLoan; // borrowing capacity of the firm

	protected double loan; // loan received by the firm 

	protected double loanForProductionAndInvestment; // part of the loan used to fund production & investment, was loadProd.

	protected double loanForDebtRepayment; // part of the loan used to fund debt repayments, was loanDebt.

	protected double liquidAssetRemainingAfterProductionAndInvestment; // amount of liquid asset remaining after paying for production & investment, was liquidAssetPrime.

	protected double badDebt; // amount of debt the firm is not able to repay

	protected double externalFunding; // amount of resources coming from the bank in a given period, was externalFund.

	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
		Update,
		ChooseSupplier,
		InitialExpenditures,
		APrioriAdjustments,
		ExpendituresUpdate,
		InvestmentOrder,
		LaborDemand,
		MachineScrapping, 
		Production, 
		Accounting;
	}

	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case Update:
			update();
			break;
			
		case ChooseSupplier:
			chooseSupplier();
			break;
			
		case InitialExpenditures:
			// Adaptative expectations
			this.expectedDemand 					= demand[0]; 
			// Compute the desired level of production and investment, regardless of the firms' capital and financial resources
			initialProductionExpenditures();
			initialInvestmentExpenditures();
			// The production function is of the Leontieff form. Thus the firm cannot produce more than the capital he possesses
			if(desiredProduction > capitalStock){
				this.desiredProduction 				= capitalStock;
			}
			this.desiredProductionStar 				= desiredProduction; 
			break;
			
		case APrioriAdjustments:
			/* A complete description of the difference between the two types of adjustments is provided in the code documentation.
			In the myopic adjustments case, firms have access to a credit line. As such, firms do not take in consideration 
			that they have to repay a share of their debt at the end of the period. 
			In the pseudo rational adjustments case, firms do not have access to the credit line. There, firms have to take into account
			their expected liquid asset at the end of the period. 
			 */
//			if(model.myopicDebtRepayment){
			if(model.debtRepayment.equals(DebtRepayment.Myopic)){
				aPrioriFeasibilityMyopic();
			} else {
				aPrioriFeasibilityPseudoRational();
				expectedPayment();
			}
			model.getBank().creditMap.put(this, netWorthToSalesRatio);
			break;
			
		case ExpendituresUpdate:
			/* In accordance with the difference in APrioriAdjustments, once they know their loan, c-firms adjust differently depending on whether they (potentially) have access to a line of credit
			 or whether they don't. In the latest case, they once more take into account their expected liquid asset at the end of the period as object function 
			 
		  	NOTE: this is because all expenditures funded through internal funds will reduce the firms' liquid assets, 	and therefore its end-of-the-period assets. Said differently, 
		  	liquidAsset[1] records all the current expenditures funded internally (accounting convention, does not matter per se) */
//			if(!model.myopicDebtRepayment)
			if(model.debtRepayment.equals(DebtRepayment.Psuedo_Rational))
				pseudoRationalExpendituresUpdate();
			break;
			
		case InvestmentOrder:
			investmentOrder();
			break;
			
		case LaborDemand:
			if(productivity > 0)
				this.laborDemand 		= productionQuantity / productivity;
			else
				log.fatal("prod = O for CFirm " + this.getKey().getId() + " with prod " + productivity);
			break;
			
		case MachineScrapping:
			machineScrapping();
			// clear the map that contains all the scrapping 	
			machinesToBeScrappedMap.clear();
			break;
		
		case Production:
			production();
			break;
			
		case Accounting:
			accounting();
			break;
		}
	}

	// ---------------------------------------------------------------------
	// Constructors and Initialization
	// ---------------------------------------------------------------------
	
	//Default constructor, used when exporting data to database.
	public CFirm() {
		super();
	}
	
	//Constructor for brand new CFirm (for copying existing firm, see copy constructor below)
	/* Note that the boolean argument newFirm is a dummy variable used only to distinguish this constructor from the 
	 * default constructor, which is used by the database export process (in which we do not want to increment the 
	 * id counter, otherwise it will increment the counter without actually creating new firms in the model).  The
	 * actual value of the boolean newFirm is ignored. 
	 */
	public CFirm(boolean newFirm){
		super(true);
		
		/* Introductory note: what kind of objects are important to initialize? 
		 			(a) arrays 
		 			(b) lists & maps 
		 			(c) references to other entities
		 			(d) initial conditions that are needed for the model to run 
		*/
		
		// Extend the Firm constructor (define the firms' debt level)
		
		this.key = new PanelEntityKey(cFirmIdCounter++);
		
		// --- Machines' variables ---
		// The initial supplier of the firm is defined in matching(), MacroModel.java. Identically for the initial set of machines.
		this.supplier 					= null;  
		this.machineAgeMap 						= new LinkedHashMap<>();
		this.machineQuantityMap 							= new LinkedHashMap<>();
		this.machinesToBeScrappedMap 				= new LinkedHashMap<>();
		// Initial amount of capital -- corresponds to k / Parameters.getDimK() machines
		this.capitalStock 							= Parameters.getInitialCapitalStock_cFirms();
		this.potentialKfirmsSet 				= new LinkedHashSet<>();
		
		// --- Financial variables ---
		this.liquidAsset 				= new double[]{Parameters.getNetLiquidAssets_cFirms(), Parameters.getNetLiquidAssets_cFirms()}; 
		this.badDebt 					= 0;  
		this.creditDemand 				= 0;
		
		// --- Productivity variables ---
		// Initial mark-up
		this.markUpRate 						= new double[]{model.markUpRate, model.markUpRate};  
		this.productivity 						= Parameters.getInitialProductivity(); 
		this.costToProduceGood 							= Parameters.getInitialWage() / productivity;
		this.priceOfGoodProduced 							= new double[]{ (1 + markUpRate[1]) * costToProduceGood, (1 + markUpRate[1]) * costToProduceGood}; 
		// When computing the competitiveness of consumption-good firms, we divide by the economy-mean unfilled demand.
		// Default unfilled demand cannot be set equal to 0 otherwise might divide sometimes by 0
		this.unfilledDemand 			= 1;  
		 
		// --- Production variables ---
		this.sales 						= new double[]{1, 1}; 
		this.productionQuantity 							= 0; 
		
		// --- Competitiveness variables ---
		// Initial competitiveness
		this.competitiveness 							= 1; 
		// Firms' market share. All c-firms are ex-ante identical, hence they all have identical market shares.
		this.marketShare 							= new double[]{1 / ((double) model.getNumberOfCFirms()), 
											1 / ((double)model.getNumberOfCFirms()), 1 / ((double)model.getNumberOfCFirms())}; 
		
		// --- Investment variables ---
		this.investmentExpansionary 					= 0; 
		this.investmentSubstitutionary = this.investment 			= Parameters.getMachineSizeInCapital_cFirms();  
		this.size 						= 0;  
		
		// --- Balance sheet object. Records the firm's expenditures and revenues. 
		this.balanceSheet 				= new BalanceSheetCFirm(this, model);
		balanceSheet.setDebt(debt[1]);
		balanceSheet.setDebtInterest(0);
		balanceSheet.setDebtRepayment(0);
		balanceSheet.setDepositRevenues(MacroModel.returnOnFirmsDeposits * liquidAsset[1]);
		
		double priceProducer 			=  (1 + Parameters.getFixedMarkUp_kFirms()) * Parameters.getInitialWage() / (Parameters.getInitialProductivity() * Parameters.getA_kFirms());
		balanceSheet.setInvExpenditure(investment * priceProducer);
		balanceSheet.setLiquidAsset(liquidAsset[1]);
		balanceSheet.setSales(sales[1]);
		balanceSheet.setWageBill(0);
		
	}
	
	// Constructor that creates a firm as copy of an already existing one
	public CFirm(CFirm copy){
		/* INTRODUCTORY NOTES: Dosi et al. (2013) models entry of new firms as random copy of current incumbent. This poses however several
		 problems:
		 		(a) It is hardly justifiable to declare as positive (or negative) some variables, even though the firm is a new entrant
		 		(e.g. unfilled demand positive, market shares etc.)
		 		(b) Modeling entry this way leads to stock-flow inconsistencies. E.g. positive amount of liquid asset: where does the money 
		 		come from? Or positive capital set: how did the firm buy those machines in the first place? 
		 Variables that are problematic are denoted with two stars, **
		 */
		
		super(true);
		
		this.key = new PanelEntityKey(cFirmIdCounter++);
		this.newEntrant 						= true;
		this.exit 								= false;		
		
		// --- Machines ---
		this.potentialKfirmsSet 						= new LinkedHashSet<KFirm>();
		/* Do not define a new supplier, as it would not consider the new capital-good firms. Instead, new entrants will pick 
		one in the supplier() method */ 
		this.supplier 							= null;
		this.capitalStock 									= copy.capitalStock; // **
		this.investment = this.investmentExpansionary = this.investmentSubstitutionary 	= 0;

		// Fill the machines set with machines that are similar to the ones of the incumbent 
		this.machineAgeMap 								= new LinkedHashMap<>();
		this.machineQuantityMap 									= new LinkedHashMap<>();
		this.machinesToBeScrappedMap 						= new LinkedHashMap<>();

		for(Map.Entry<Machine, Integer> entry : copy.machineQuantityMap.entrySet()){	// **
			Machine newMachine 					= entry.getKey().clone();
			machineQuantityMap.put(newMachine, entry.getValue());
			// Even though they are copied from already existing machines, the age of the machines are set equal to 0
			machineAgeMap.put(newMachine, 0);
		}
		
		// --- Productivity ---
		this.markUpRate 								= new double[]{copy.markUpRate[0], copy.markUpRate[1]};
		this.priceOfGoodProduced 									= new double[]{copy.priceOfGoodProduced[0], copy.priceOfGoodProduced[1]};
		this.costToProduceGood 									= copy.costToProduceGood;
		this.unfilledDemand 					= copy.unfilledDemand; // **

		// --- Production --- 
		this.expectedDemand 								= copy.expectedDemand;
		this.demand 							= new double[]{copy.demand[0], copy.demand[1]};
		this.productionQuantity 									= copy.productionQuantity;
		this.sales 								= new double[]{copy.sales[0], copy.sales[1]};

		this.inventories 									= new double[]{0, 0};
		this.size 								= 0;

		// --- Competitiveness ---
		this.competitiveness 									= copy.competitiveness;
		this.marketShare 									= new double[]{copy.marketShare[0], copy.marketShare[1], copy.marketShare[2]}; // **
		
		// --- Financial variables --- 
		this.liquidAsset 						= new double[]{copy.liquidAsset[0], copy.liquidAsset[1]}; // **
		this.grossOperatingSurplus 								= copy.grossOperatingSurplus; // **
		this.balanceSheet 						= new BalanceSheetCFirm(this, model);
		this.externalFunding						= 0;

	}



	public void demandInitialization(){
		
		// The aggregate demand is shared uniformly across consumption-good firms since firms are ex-ante identical 
		double dem 						= collector.aggregateDemand / ((double)model.getNumberOfCFirms()); 
		this.demand 					= new double[]{dem, dem}; 
		this.productionQuantity							= dem;
		// Initial inventories defined according to equation (19) in Dosi et al. (2013) 
		this.inventories 							= new double[]{0, Parameters.getDesiredInventoriesProportionOfExpectedDemand_cFirms() * dem}; 
		this.stockFinalGood 			= productionQuantity + inventories[1]; 
		
		double sales0 					= dem * priceOfGoodProduced[1]; 
		this.sales 						= new double[]{sales0, sales0}; 
		// Gross operating surplus.
		this.grossOperatingSurplus						= sales0 - costToProduceGood * productionQuantity;
		
	}
	
	// ---------------------------------------------------------------------
	// Exit methods
	// ---------------------------------------------------------------------
	
	void survivalChecking(){
		// Identify whether the firm can remain in the market
		
		if(marketShare[2] > Parameters.getMarketShareThresholdForExit_cFirms() && liquidAsset[1] > 0){
			collector.cFirmsRemaining			+= 1;
			this.newEntrant 					= false;
		} else {
			this.exit							= true;
			collector.exit_cFirms 					+= 1;
			
			if(this.debt[1] > 0)
				collector.bankruptcyRate_cFirms 		+= 1;
			
			// TODO: this if else structure is mainly for checking purpose.
			if(liquidAsset[1] <= 0 && marketShare[2] > Parameters.getMarketShareThresholdForExit_cFirms())
				collector.exitLiquidityIssue_cFirms 		+= 1;
			else if(marketShare[2] <= Parameters.getMarketShareThresholdForExit_cFirms() && liquidAsset[1] > 0)
				collector.exitMarketShareIssue_cFirms		+= 1;
			else
				collector.exitAssetMarket_cFirms		+= 1;
		
		}
	}
	
	/* the kill() method removes all references of the firm to other objects. This helps to boost the performance of the simulations, 
	 by reducing the number of objects in the memory. */
	public void kill(){
		super.kill();
				
		this.balanceSheet.kill();
		this.balanceSheet 						= null;
		
		// --- Arrays ---
		this.markUpRate 								= null;
		this.inventories 									= null;
		
		// --- Entities ---
		this.supplier 							= null;
		for(Map.Entry<Machine, Integer> entry : machineAgeMap.entrySet()){
			entry.getKey().kill();
		}
		
		// --- Lists & Maps ---
		this.potentialKfirmsSet 						= null;
		this.machineAgeMap 								= null;
		this.machineQuantityMap 									= null;
		this.machinesToBeScrappedMap 						= null;
		
	}
	
	
	// ---------------------------------------------------------------------
	// Update methods
	// ---------------------------------------------------------------------
	
	public void update(){
		
		super.update();
		
		// Update temporal variables
		this.markUpRate[0] 								= markUpRate[1];
		this.inventories[0] 								= inventories[1];

		// Re-initialize some variables 
		this.loan 								= 0; 
		this.demand[1] 							= 0;
		this.externalFunding						= 0;
		
		// Re-compute total capital 
		this.capitalStock 									= 0;
		for(Map.Entry<Machine, Integer> entry : machineQuantityMap.entrySet()){
			// Update the unit labor cost of production, equation (13.5) in Dosi et al. (2013)
			double newCost						= collector.wage[1] / entry.getKey().getA()[1];
			entry.getKey().setCost(newCost); 
			// Equation (20.5) in Dosi et al. (2013)
			this.capitalStock 								+= entry.getValue() * Parameters.getMachineSizeInCapital_cFirms(); 
			// Update the age of each machine. 
			machineAgeMap.put(entry.getKey(), machineAgeMap.get(entry.getKey()) + 1);
		}
		
		// Update productivity
		this.productivity 								= 0;
		double numberOfMachine 					= capitalStock / Parameters.getMachineSizeInCapital_cFirms();
		if(numberOfMachine > 0){
			for(Map.Entry<Machine, Integer> entry : machineQuantityMap.entrySet()){
				// Equation (21.5) in Dosi et al. (2013)
				this.productivity 						+= entry.getKey().getA()[1] * entry.getValue() / numberOfMachine;
			}
		} else {
			// This should not happen, at least under reasonable parametrization.
			this.productivity							= 0;
			log.fatal("C-firm " + this.getKey().getId() + " has nil number of machines." );
		}
			
		// Update cost
		if(productivity > 0)
			// Equation (22) in Dosi et al. (2013)
			this.costToProduceGood								= collector.wage[1] / productivity;
		else {
			// This should not happen, at least under reasonable parametrization.
			this.costToProduceGood								= 10_000_000;
			log.fatal("C-firm " + this.getKey().getId() + " has nil productivity.");
		}
		
		// Update mark-up
		if(marketShare[0] > 0){
			// Equation (3) in Dosi et al. (2013)
			this.markUpRate[1] 							= markUpRate[0] * (1 + Parameters.getCoeffMarkUpRule_cFirms() * (marketShare[1] - marketShare[0]) / marketShare[0]); 
		} else{ 
			this.markUpRate[1] 							= markUpRate[0];
			log.error("f(t-2) = 0"); 
		}
		
		if(markUpRate[1] <= 0 || markUpRate[1] > 1)
			log.error("Mark up is either neg. or bigger than 1");
		
		// Update the price
		// Equation (22) in Dosi et al. (2013)
		this.priceOfGoodProduced[1] 								= (1 + markUpRate[1]) * costToProduceGood; 
		// Follow Dosi et al. code: ensure that prices are not negative
		if(priceOfGoodProduced[1] < Parameters.getMinPrice_kFirms()){
			this.priceOfGoodProduced[1] 							= Parameters.getMinPrice_kFirms();
			log.error("p < pmin");
		}
		
		// Update net-wealth to sale ratio
		if(sales[0] > 0){
			this.netWorthToSalesRatio 				= liquidAsset[0] / sales[0];
		} else {
			this.netWorthToSalesRatio 				= 0.;
		}
	}
	
	// ---------------------------------------------------------------------
	// Supplier methods
	// ---------------------------------------------------------------------
	
	void chooseSupplier(){
		// A consumption-good firm always need a supplier. If it does not have one, then it picks one at random
		if(supplier == null){
			int rnd 							= SimulationEngine.getRnd().nextInt(model.getNumberOfKFirms());
			KFirm newSupplier 					= model.getKFirms().get(rnd);
			this.supplier 						= newSupplier;
			//NOTE: no need to add it to the supplier's clients list. This is done at the end of the method 
		}
		
		// Consumption-good firms compare the brochures they have received and choose the supplier that is most competitive 
		KFirm oldSupplier 						= supplier;		
		// If condition that ensures that we will not iterate over an empty list (e.g. might be the case for some new entrants)
		if(!potentialKfirmsSet.isEmpty()){			//XXX: This check is unnecessary.
			for(KFirm potentialSupplier : potentialKfirmsSet){
				// Equation (17) in Dosi et al. (2013) measures a firm's competitiveness 
				double realPriceOldSupplier 	= oldSupplier.priceOfGoodProduced[1] + Parameters.getMachinePaybackPeriod_cFirms() * oldSupplier.machineProduced.getCost();
				double realPricePotentialSupplier = potentialSupplier.priceOfGoodProduced[1] + Parameters.getMachinePaybackPeriod_cFirms() * potentialSupplier.machineProduced.getCost();
				
				if(realPricePotentialSupplier < realPriceOldSupplier)
					oldSupplier 				= potentialSupplier;
			}
		}
		this.supplier 							= oldSupplier;
		// Notify the supplier that the consumption-good firm is a new client
		supplier.clients.add(this);
		potentialKfirmsSet.clear();
	}
	
	// ---------------------------------------------------------------------
	// Initial Expenditures methods
	// ---------------------------------------------------------------------
	
	void initialProductionExpenditures(){
		// Equation (19) in Dosi et al (2013)
		this.desiredInventories 								= Parameters.getDesiredInventoriesProportionOfExpectedDemand_cFirms() * expectedDemand; 
		if(this.liquidAsset[0] > 0){
			this.desiredProduction 							= expectedDemand + desiredInventories - inventories[0]; 
		} else {
			this.desiredProduction 							= 0;
		}
		/* If the expected demand is lower than the stock of capital, then the firm does not want to produce. When dQ = 0, follow
		Dosi et al. implementation: the firm is dead and will have to exit at the end of the period. In particular, it cannot sell
		its inventories. */
		if(desiredProduction < 0)
			this.desiredProduction 							= 0;
		
		/* The unit cost of production is an average of the firm's machine productivity (Equation (22)). If a firm does not need its 
		 entire set of machines to produce its desired quantity, it selects only its most productive ones.
		 At the aggregate, this represents on average a cost reduction of 5% */
		if(desiredProduction < capitalStock && desiredProduction > 0)
			machineSelection();
		
	}
	
	void machineSelection(){
		/* Selection of the most productive machines in order to produce in the most efficient way
		Rank the machine according to their productivity (from the most to the least productive) */
		Map<Machine, Double> productivitySet 	= new LinkedHashMap<>();
		for(Map.Entry<Machine, Integer> entry : machineQuantityMap.entrySet()){
			productivitySet.put(entry.getKey(), entry.getKey().getA()[1]);
		}
		Map<Machine, Double> productivityRanked = MapSorting.sortByValueDescending(productivitySet);
		
		/* Find the number of machines needed to meet the production target 
		i is used to compute (iteratively) the number of machines that still need to be added to the production set, while 
		numberMachinesNeeded is used in the formula of productivity (Equation 21.5 in Dosi et al., 2013) */ 
		int numberMachinesNeeded 				= (int) Math.ceil(desiredProduction / Parameters.getMachineSizeInCapital_cFirms());
		int i 									= numberMachinesNeeded;
		this.productivity							 	= 0;
		
		for(Map.Entry<Machine, Double> entry : productivityRanked.entrySet()){			
			if(i > 0){
				Machine m 						= entry.getKey();
				if(machineQuantityMap.get(m) > i){
					// There are (marginally) enough machines of type m to meet the production target. Stop here
					this.productivity 					+= m.getA()[1] * i / numberMachinesNeeded;
					i 							= 0;
				} else {
					// Machines ranked lower than the machines of type m are also (marginally) needed
					this.productivity 					+= m.getA()[1] * machineQuantityMap.get(m) / numberMachinesNeeded;
					i 							-= machineQuantityMap.get(m);
				}
			} else {
				break;
			}
		}
		this.costToProduceGood									= collector.wage[1] / productivity;
	}
	
	void initialInvestmentExpenditures(){
		/* INTRODUCTORY NOTES: investments need to be multiples of the capital dimension. We follow Dosi et al. implementation and
		round down investments when needed. 
		 Investments are made of two components: (a) expansionary investments and (b) substitutionary investments.
		(a) The difference between the desired level of capital and the actual level of capital (Equation (19.5) in Dosi et al., 2013) 
		
		Compute the desired amount of capital */
		if(SimulationEngine.getInstance().getTime() == 0.0){
			this.desiredCapitalStock 							= desiredProduction;
		}else {
			this.desiredCapitalStock 							= desiredProduction / Parameters.getDesiredCapacityUtilization_cFirms();
		}
		
		// There is an upper bound on the per period capital growth
		this.maxPossibleCapitalStock 								= capitalStock * (1 + Parameters.getMaxCapitalGrowthPerPeriod_cFirms());
		this.maxPossibleCapitalStock 								= Math.round(maxPossibleCapitalStock / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
		
		// Compute the desired expansionary investments
		if(desiredCapitalStock > capitalStock){
			if(desiredCapitalStock > maxPossibleCapitalStock)
				this.desiredInvestmentExpansionary 					= maxPossibleCapitalStock - capitalStock;
			else
				this.desiredInvestmentExpansionary 					= Math.floor((desiredCapitalStock - capitalStock) / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
		} else {
			this.desiredInvestmentExpansionary 						= 0;
		}
		this.desiredInvestmentExpansionaryStar 						= desiredInvestmentExpansionary;
		
		// (b) Substitutionary investments made of: (i) scrapping of machines that are too-old, (ii) scrapping of machines that are less productive than
		// the current vintage being produced
		
		this.desiredInvestmentSubstitutionary 							= 0;
		// Too-old machines 
		for(Map.Entry<Machine, Integer> entry : machineAgeMap.entrySet()){
			if(entry.getValue() > Parameters.getMaxAgeMachines_cFirms()){
				// Scrapping of these machines will actually take place only if the firm has the fund to replace them. So far, only store
				// them in the toBeScrapped map.
				this.machinesToBeScrappedMap.put(entry.getKey(), machineQuantityMap.get(entry.getKey()));
				this.desiredInvestmentSubstitutionary 					+= machineQuantityMap.get(entry.getKey()).doubleValue() * Parameters.getMachineSizeInCapital_cFirms();
			}
		}
				
		// Low productivity machines 
		for(Map.Entry<Machine, Integer> entry : machineQuantityMap.entrySet()){
			/* NOTE: requires the productivity of the current machine < the productivity of supplier's machine. Otherwise, 
			 the payback would be negative, and because b (the payback parameter) is positive, the firm would actually want to scrap 
			 this machine, even though its productivity is higher */
			if(entry.getKey().getA()[1] < supplier.getMachineProduced().getA()[1] && collector.wage[1] > 0){
				// Equation (21) in Dosi et al. (2013)
				double payback 					= supplier.priceOfGoodProduced[1] / ( collector.wage[1] / entry.getKey().getA()[1] - 
													collector.wage[1] / supplier.machineProduced.getA()[1] );

				if(payback <= Parameters.getMachinePaybackPeriod_cFirms() && !machinesToBeScrappedMap.containsKey(entry.getKey()) ){
					this.machinesToBeScrappedMap.put(entry.getKey(), entry.getValue());
					this.desiredInvestmentSubstitutionary 				+= entry.getValue().doubleValue() * Parameters.getMachineSizeInCapital_cFirms();
				}
			}
		}
		this.desiredInvestmentSubstitionaryStar = desiredInvestmentSubstitutionary;		
	}
	
	// ---------------------------------------------------------------------
	// A Priori Adjustments methods - INVESTMENT DECISIONS
	// ---------------------------------------------------------------------
	
		// ---------------------------------------------------------------------
		// Myopic / Access to line of credit 
		// ---------------------------------------------------------------------
	
	void aPrioriFeasibilityMyopic(){
		// Equation (5) in Dosi et al. (2013), using gross-operating surplus instead of past sales
		if(desiredProductionStar > 0 && grossOperatingSurplus > 0){
			this.maxPossibleLoan 						= model.loanToValueRatio * grossOperatingSurplus;
		} else {
			// Recall that firms with nil production are required to exit the economy
			this.maxPossibleLoan 						= 0;
		}
		
		// Local variables used iteratvely in the adjustment process
		double loanPrime 						= maxPossibleLoan;
		this.liquidAssetRemainingAfterProductionAndInvestment 					= liquidAsset[0];
		
		double costProduction 					= desiredProductionStar * costToProduceGood;
		double costInvExp 						= desiredInvestmentExpansionaryStar * supplier.getPriceOfGoodProducedNow() / Parameters.getMachineSizeInCapital_cFirms();
		double costInvSub 						= desiredInvestmentSubstitionaryStar * supplier.getPriceOfGoodProducedNow() / Parameters.getMachineSizeInCapital_cFirms();
		
		/* Firm have to check whether their current plans are achievable with their current resources. These are made of their internal
		funds and some potential loans, bounded above by their borrowing capacity.
		Firms have preferences: 
			(a) preferences for production over investment, 
			(b) preferences for internal funds over external ones  

		// The firm deducts from its liquid assets the cost of production */
		this.liquidAssetRemainingAfterProductionAndInvestment 					-= costProduction;
		if(liquidAssetRemainingAfterProductionAndInvestment < 0)
			this.liquidAssetRemainingAfterProductionAndInvestment 				= 0;
		
		// The firm verifies whether it can achieve its desired investments. If not, it scales down its plans 
		// Expansionary investments
		if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment)
			this.liquidAssetRemainingAfterProductionAndInvestment 				-= costInvExp;
		else if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			loanPrime 							-= costInvExp - liquidAssetRemainingAfterProductionAndInvestment;
			this.liquidAssetRemainingAfterProductionAndInvestment 				= 0;
		} else {
			if(liquidAssetRemainingAfterProductionAndInvestment > 0){
				this.desiredInvestmentExpansionaryStar 				= Math.floor((liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / supplier.priceOfGoodProduced[1]) * Parameters.getMachineSizeInCapital_cFirms();
			} else {
				this.desiredInvestmentExpansionaryStar 				= 0;
			}
			this.liquidAssetRemainingAfterProductionAndInvestment 				= 0;
			loanPrime 							= 0;
		}
		
		// Substitutionary investments
		if(costInvSub > liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			this.desiredInvestmentSubstitionaryStar 					= Math.floor((liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / supplier.priceOfGoodProduced[1]) * Parameters.getMachineSizeInCapital_cFirms();
		}  
		
	/*	Alternative way to code this adjustment process. Yields a much less volatile investment. 
	 
	 if(costProduction <= liquidAssetPrime)
			this.liquidAssetPrime 				-= costProduction;
		else if(costProduction <= liquidAssetPrime + loanPrime){
			loanPrime 							-= costProduction - liquidAssetPrime;
			this.liquidAssetPrime 				= 0;
		} else {
			this.dQStar 						= (liquidAssetPrime + loanPrime) / c;
			loanPrime 							= 0;
			this.liquidAssetPrime 				= 0;
		}
		
		if(costInvExp <= liquidAssetPrime)
			this.liquidAssetPrime 				-= costInvExp;
		else if(costInvExp <= liquidAssetPrime + loanPrime){
			loanPrime 							-= costInvExp - liquidAssetPrime;
			this.liquidAssetPrime 				= 0;
		} else {
			this.dInvExpStar 					= Math.floor((liquidAssetPrime + loanPrime) / supplier.getPPresent()) * 
													Parameters.getDimK();
			loanPrime 							= 0;
			this.liquidAssetPrime 				= 0;
		}
		
		if(costInvSub <= liquidAssetPrime)
			this.liquidAssetPrime 				-= costInvSub;
		else if(costInvSub <= liquidAssetPrime + loanPrime){
			loanPrime 							-= costInvSub - liquidAssetPrime;
			this.liquidAssetPrime 				= 0;
		} else {
			this.dInvSubStar 					= Math.floor((liquidAssetPrime + loanPrime) / supplier.getPPresent()) * 
													Parameters.getDimK();
			loanPrime 							= 0;
			this.liquidAssetPrime 				= 0;
		} */
		
		// Update the production & investment costs with the new quantity
		costProduction 							= costToProduceGood * desiredProductionStar;
		double costInvestment 					= supplier.priceOfGoodProduced[1] * (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar) / Parameters.getMachineSizeInCapital_cFirms();
				
		/* Line of credit assumption: firms include in their credit demands their entire debt, s.t. the firm is able to roll over its
		 debt -- unless it is credit constrained */
		if(debt[0] + costInvestment + costProduction <= liquidAsset[0])
			this.creditDemand 					= 0;
		else 
			this.creditDemand 					= debt[0] + costInvestment + costProduction - liquidAsset[0];
	}
	
		// ---------------------------------------------------------------------
		// Pseudo Rational / no line of credit 
		// ---------------------------------------------------------------------
	
	void aPrioriFeasibilityPseudoRational(){
		/* c-firms initially have to check whether or not their current plans are feasible with their current resources. These are made of their internal
		funds (liquidAsset[0]) and some loan, that is bounded above by their borrowing capacity (maxLoan). Firms have preferences which shape this adjustment: 
			(a) preferences for production over investment, 
			(b) preferences for internal funds over external ones 
		*/
		
		// equation (5) in Dosi et al. (2013). FIXME: why gos and not sales(t-1) ?
		if(desiredProductionStar > 0){
			this.maxPossibleLoan = model.loanToValueRatio * grossOperatingSurplus;
		} else {
			// follow Dosi et al. implementation:  "firms that did not succeed to produce do not invest"
			this.maxPossibleLoan = 0;
		}
		
		// loanPrime is a temporary variables used in the adjustment to not affect global variables 
		double loanPrime = maxPossibleLoan;
		this.liquidAssetRemainingAfterProductionAndInvestment = liquidAsset[0];
		
		double costProduction = desiredProductionStar * costToProduceGood;
		double costInvExp = desiredInvestmentExpansionaryStar * supplier.getPriceOfGoodProducedNow() / Parameters.getMachineSizeInCapital_cFirms();
		double costInvSub = desiredInvestmentSubstitionaryStar * supplier.getPriceOfGoodProducedNow() / Parameters.getMachineSizeInCapital_cFirms();
		
		log.debug("The resources of the CFirm " + this.getKey().getId() + 
					"\n maxLoan " + maxPossibleLoan + 
					"\n liquidAsset " + liquidAsset[0] + 
					"\n cost production " + costProduction + 
					"\n cost investment exp. " + costInvExp + 
					"\n cost investment sub. " + costInvSub);
		
		// c-firms check first if they achieve their desired production, and compute the corresponding amount of resources needed
		if(costProduction <= liquidAssetRemainingAfterProductionAndInvestment ){
			// then production can be entirely funded through internal funds
			this.liquidAssetRemainingAfterProductionAndInvestment -= costProduction;
		} else if(costProduction <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			// then production can be funded through internal funds & loan
			loanPrime -= costProduction - liquidAssetRemainingAfterProductionAndInvestment;
			this.liquidAssetRemainingAfterProductionAndInvestment = 0; 
		} else {
			// internal funds && loan were not sufficient to fund the entire production. The firm has to scale down its plan
			this.desiredProductionStar = (liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / costToProduceGood;
			this.liquidAssetRemainingAfterProductionAndInvestment = 0;
			loanPrime = 0;
		}
		
		// Follow the same if else structure for the two types of investment
		if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment){
			this.liquidAssetRemainingAfterProductionAndInvestment -= costInvExp;
		} else if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			loanPrime -= costInvExp - liquidAssetRemainingAfterProductionAndInvestment;
			this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		} else {
			// NOTE: here, slight difference: need to floor the level of investment as it has to be expressed in terms of machines
			this.desiredInvestmentExpansionaryStar = Math.floor((liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / supplier.getPriceOfGoodProducedNow()) * Parameters.getMachineSizeInCapital_cFirms();
			loanPrime = 0;
			this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		}
		
		if(costInvSub <= liquidAssetRemainingAfterProductionAndInvestment){
			this.liquidAssetRemainingAfterProductionAndInvestment -= costInvSub;
		} else if(costInvSub <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			loanPrime -= costInvSub - liquidAssetRemainingAfterProductionAndInvestment;
			this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		} else {
			// NOTE: here, slight difference: need to floor the level of investment as it has to be expressed in terms of machines
			this.desiredInvestmentSubstitionaryStar = Math.floor((liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / supplier.getPriceOfGoodProducedNow()) * Parameters.getMachineSizeInCapital_cFirms();
			loanPrime = 0;
			this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		}
		
		this.loanForProductionAndInvestment = maxPossibleLoan - loanPrime;
		
		log.debug("After the adjustments: " + 
					"\n production: " + desiredProductionStar + 
					"\n exp. inv.: " + desiredInvestmentExpansionaryStar + 
					"\n sub. inv: " + desiredInvestmentSubstitionaryStar + 
					"\n loan for production: " + loanForProductionAndInvestment + 
					"\n asset reamining: " + liquidAssetRemainingAfterProductionAndInvestment + 
					"\n loan remaining: " + loanPrime);
		
		
	}
	
	void expectedPayment(){
		// this method checks whether c-firms, given their feasible plans from the previous method, expect to be able to repay their due debt at the end of the period 
		
		// collection of parameters to make the closed form solutions look less awful 
		double param2 = Parameters.getDebtRepaymentSharePerPeriod_cFirms() + (1 - MacroModel.tax_rate) * MacroModel.interestRateOnDebt;
		double salesTemp = priceOfGoodProduced[1] * Math.min(demand[0], desiredProductionStar + inventories[0]);
		/* For clarification on the origin of salesTemp, see the code documentation pdf. Intuition: the firm cannot sell more than what it has produced (current production
		 + stock of inventories), nor more than what its demand is. 
		*/
		
		if(loanForProductionAndInvestment == 0){
			 // if c-firms has nil loan prod, it implies that prod. and inv. were funded only using internal funds
			
			// (a): does the firm expect to be able to repay its debt, without using the loan for debt repayments purposes?
			if(balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), 0, 0) > 0){
				// Yes. Hence the credit demand of the firm is nil
				
				this.loanForDebtRepayment = this.loanForProductionAndInvestment = this.creditDemand = 0;
				log.debug("CFirm " + this.getKey().getId() + " had pos. cash and expect to pay its debt without borrowing");
				
			} // (b): does the firm expect to be able to repay its debt, using the entire loan for debt repayments purposes?
			else if(balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), maxPossibleLoan, 0) > 0){
				/* Because the firm does not need accumulate loans if these are not to pay production, investment or debt repayments, the firm is looking
				 for the level of loanDebt such that the payment equation will be equal to 0. Because payment > 0 when loanDebt = maxLoan, and payment 
				 is monotonically increasing in loanDebt, this level of 'optimal' level of loan debt exists (mean value theorem).
				 */
				
				// closed-form solution for this optimal level
				this.loanForDebtRepayment = 1 / (1 - param2) * (param2 * debt[0] - (1 - MacroModel.tax_rate) * salesTemp - liquidAssetRemainingAfterProductionAndInvestment * (1 + MacroModel.returnOnFirmsDeposits)); 
				this.creditDemand = loanForDebtRepayment; // because loanProd = 0 de facto from the preferences of c-firms for internal funds and liquidAssetPrime > 0
				
				log.debug("CFirm " + this.getKey().getId() + " had pos. cash and need to borrow loan debt = " + loanForDebtRepayment + " to meet the condition. "
						+ "Payment should --> 0: " + balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), loanForDebtRepayment, 0));
				
			} /* (c): only remains the case where the firm expect to not be able to pay its debt at the end of the period. Thus it has to adjust its investment, and 
			 maybe production plans. These adjustments are undertaken in the APrioriAdjustments class */
			else {
				
				// payment is monotically increasing in loanDebt. Hence, setting loanDebt = maxLoan minimizes the required amount of adjustment needed
				this.loanForDebtRepayment = maxPossibleLoan;
				APrioriAdjustments.adjustmentsWithPositiveLiquidAsset(this);
				
			}
			
		} else {
			// else, the firm already plan to use part, or all, of the loan at its disposal. Once more, there is several scenarios to consider:
			
			// (a): conditional on planning to use all the loan for production and investment purposes, does the firm expect to be able to repay its debt? 
			if(loanForProductionAndInvestment == maxPossibleLoan){
				// recall than loanProd + loanDebt = maxLoan, by definition of the borrowing capacity of c-firms. Hence, loanDebt = 0
				this.loanForDebtRepayment = 0;
				
				if(balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), 0, loanForProductionAndInvestment) > 0 ){
					// yes the firm expect to be able to pay its debt at the end of the period: no adjustments are needed
					
					this.creditDemand = loanForProductionAndInvestment;
					log.debug("CFirm " + this.getKey().getId() + " used all its loan to pay for prod & inv and expect to meet the payment condition");
					
				} else {
					// no it does not, and in this case it cannot increase loan debt as loan prod is already equal to the maximal possible loan (recall loanProd + loanDebt = maxLoan)
					// therefore go straight into the adjustment process
					
					APrioriAdjustments.adjustmentsWithNilLiquidAsset(this);
					
				}
			} /* (b): not all the loan is planned to be used for production & inv. expenditures. Thus, if needed, the firm can increase its credit demand to 
			 meet the constraint (recall that the payment is monotonically increasing in loan debt) */
			else {
				// (i) does the firm expect to be able to repay its debt, not borrowing further? 
				if(balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), 0, loanForProductionAndInvestment) > 0 ){
					// yes it does
					
					this.loanForDebtRepayment = 0;
					this.creditDemand = loanForProductionAndInvestment; //NOTE: here, creditDemand < maxLoan
					log.debug("Not all the loan was used for prod. & inv. purposes and the firm does not need to borrow further");
				} // (ii) does the firm expect to be able to repay its debt, conditional on using all the remaining loan 
				else if(balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), maxPossibleLoan - loanForProductionAndInvestment, loanForProductionAndInvestment) > 0 ){
					/* yes it does. Once more, because payment is monotically increasing in payment, and because we know that at the maximal possible level for loanDebt (i.e. loanDebt = maxLoan - loanProd), 
					 the payment condition is positive, then by the mean value theorem there exists a level of loanDebt such that the payment will equal 0 */
					
					// closed-form expression
					this.loanForDebtRepayment = 1 / (1 - param2) * (param2 * (debt[0] + loanForProductionAndInvestment) - (1 - MacroModel.tax_rate) * salesTemp);
					this.creditDemand = loanForDebtRepayment + loanForProductionAndInvestment;
					log.debug("Not all the use was loan and there exists a closed-form solution for lDebt. lDebt = " + loanForDebtRepayment + " and payment should --> 0 " + 
					balanceSheet.payment(desiredProductionStar, (long) (desiredInvestmentExpansionaryStar + desiredInvestmentSubstitionaryStar), loanForDebtRepayment, loanForProductionAndInvestment));
					
				} else {
					/* (iii) final case: the firm does not expect to be able to repay its debt, even using the loan remaining for debt repayments. Hence need to adjust its production
					and investment plans. Recall: payment is monotically increasing in loanDebt. Hence, setting loanDebt = maxLoan minimizes the required amount of adjustment needed */
					
					this.loanForDebtRepayment = maxPossibleLoan - loanForProductionAndInvestment;
					APrioriAdjustments.adjustmentsWithNilLiquidAsset(this);
					
				}
			}
		}
		
		/* The bank, in the CreditAllocation event, will increase debt[1] of c-firms. Yet, because our closed-form expressions in the ExpendituresUpdate event are based on 
		the past debt + how the firm splits its loan between debt repayments and production / investment expenditures, need a place that store the past debt. This is debt[0] 
		NOTE: this does not affect the simulations, as debt[1] = debt[0] + loan */
		this.debt[1] = debt[0];
	}
	
	// ---------------------------------------------------------------------
	// Expenditures Update methods
	// ---------------------------------------------------------------------
	
		// ---------------------------------------------------------------------
		// Myopic / access to a line of credit 
		// ---------------------------------------------------------------------
	
	void myopicExpendituresUpdate(){
		/* In this version, consumption-good firms have access to a line of credit. They can thus 
		 		(a) consume only a part of the loan that the bank offers them
		 		(b) repay their debt (accumulation of line of credit) whenever they want
		 
		 Recall that credit demand = past debt + cost production + cost investment - liquid asset. 
		 Thus 
		 	(a) if credit demand = 0 	=> 	liquid asset > past debt + cost production + cost investment,
		 		and the firm can repay its entire (past) debt. 
		 	(b) if credit demand > 0, but 	liquid asset > cost production + cost investment, 	then the firm
		 		can repay a part of its debt, and reduce a bit its stock of debt.
		 	(c) if credit demand > 0 and liquid asset < cost production + cost investment, 	the firm accumulates
		 		more debt.
		 		
		 Difference between Mason and our version:
		 	(a) When credit demand = 0, our version allows the firm to repay entirely its debt
		 	(b) Firms try to reach intermediary level of investments in our version, which is not the case in the Mason one
		 */
		this.liquidAsset[1] 					= liquidAsset[0];
		
		if(creditDemand == 0){
			// The firm can achieve its original plans
			this.productionQuantity 								= desiredProductionStar;
			this.investmentExpansionary 						= desiredInvestmentExpansionaryStar;
			this.investmentSubstitutionary 						= desiredInvestmentSubstitionaryStar;
			
			// Difference between Mason and ours: because the firm has sufficient fund to repay its debt, it does so. 
			// This reduces its stock of liquid asset, as well as the bank's debt
//			if(model.mason){
			if(model.debtManagement.equals(DebtManagement.Dosi_Et_Al)){
				this.debt[1] 					= debt[0];
				// BEFORE: 
				// this.liquidAsset[1] -= c * q + supplier.getPPresent() * (invExp + invSub) / Parameters.getDimK();
				// Now: remains identical
			} else {
				this.liquidAsset[1] 			-= debt[0];
				model.getBank().debt			-= debt[0];
				this.debt[1]					= 0;
			}
		} else {
			if(loan == creditDemand){
				// The firm can achieve its original plans
				this.productionQuantity 						= desiredProductionStar;
				this.investmentExpansionary 				= desiredInvestmentExpansionaryStar;
				this.investmentSubstitutionary 				= desiredInvestmentSubstitionaryStar;
				
				// No difference between Mason and our version
				double costProduction		= costToProduceGood * productionQuantity;
				double costInvestment		= supplier.getPriceOfGoodProducedNow() * (investmentExpansionary + investmentSubstitutionary) / Parameters.getMachineSizeInCapital_cFirms();
				// External fund represents in the same time the component of the loan that is needed to fund 
				// production and investments, and the debt variation. External fund can be positive (increase in the 
				// stock of debt) or negative (reduction in the debt stock)
				this.externalFunding			= costProduction + costInvestment - liquidAsset[1];
				this.debt[1]				= debt[0] + externalFunding;
				model.getBank().totalCreditRemaining -= debt[1];
				// Update the bank's balance sheet accordingly
				model.getBank().debt		+= externalFunding;
				
				if(externalFunding < 0){
				 	// There is partial debt repayment, using personal liquid asset. Part of the liquid asset are thus
					// used here, and the remaining part is equal to the total expenditures 
					this.liquidAsset[1]		= costProduction + costInvestment;
					this.externalFunding		= 0;
				}
			} 
			else {
				
				collector.creditRationingRate_cFirms += 1;
				
				if(debt[0] + supplier.priceOfGoodProduced[1] * desiredInvestmentExpansionaryStar / Parameters.getMachineSizeInCapital_cFirms() + costToProduceGood * desiredProductionStar <= liquidAsset[1] + loan){
					
					// Can achieve optimal production and expansionary investment
					this.productionQuantity 							= desiredProductionStar;
					this.investmentExpansionary 					= desiredInvestmentExpansionaryStar;
					
					// Difference between Mason and our version: firm considers intermediary level of investment in our
					// version, whereas they directly set it to zero in the Mason one.
//					if(model.mason){
					if(model.debtManagement.equals(DebtManagement.Dosi_Et_Al)){
						// The firm directly sets its substitutionary investments to 0
						this.investmentSubstitutionary					= 0;	
						
						if(debt[0] + supplier.priceOfGoodProduced[1] * investmentExpansionary / Parameters.getMachineSizeInCapital_cFirms() + costToProduceGood * productionQuantity > liquidAsset[1]){
							// The firm still needs external resources. Note that this does not prevent the stock 
							// of debt to decrease -- if externalFund < 0
							this.externalFunding		= supplier.priceOfGoodProduced[1] * investmentExpansionary / Parameters.getMachineSizeInCapital_cFirms() + costToProduceGood * productionQuantity - liquidAsset[1];
							this.debt[1]			= debt[0] + externalFunding;
							model.getBank().debt 	+= externalFunding;
							
							if(externalFunding < 0){
								// There is partial debt repayment, using personal liquid asset. Part of the liquid asset are thus
								// used here, and the remaining part is equal to the total expenditures 
						 		this.liquidAsset[1]	= costToProduceGood * productionQuantity + supplier.priceOfGoodProduced[1] * investmentExpansionary / Parameters.getMachineSizeInCapital_cFirms();  
						 		this.externalFunding	= 0;
							}
						
						} else {
							// The firm does not need any external funds once its substitutionary investments are equal to 0
							this.externalFunding 		= 0;
							// It can repay its entire debt 
							this.liquidAsset[1] 	-= debt[0];
							model.getBank().debt 	-= debt[0];
							this.debt[1]			= 0;
						}
						model.getBank().totalCreditRemaining -= debt[1];
					} else {
						// The firm considers intermediary level of substitutionary investments
						
						this.investmentSubstitutionary					= Math.floor( (loan + liquidAsset[1] - 
														debt[0] - supplier.priceOfGoodProduced[1] * investmentExpansionary / Parameters.getMachineSizeInCapital_cFirms() - costToProduceGood * productionQuantity) / 
														supplier.getPriceOfGoodProducedNow() ) * Parameters.getMachineSizeInCapital_cFirms();

						double costProduction 		= productionQuantity * costToProduceGood;
						double costInvestment 		= ( investmentSubstitutionary + investmentExpansionary / Parameters.getMachineSizeInCapital_cFirms() ) * supplier.getPriceOfGoodProducedNow();
						this.externalFunding			= costProduction + costInvestment - liquidAsset[1];
						this.debt[1]				= debt[0] + externalFunding;
						model.getBank().totalCreditRemaining -= debt[1];
						model.getBank().debt		+= externalFunding;
							
						if(externalFunding < 0){
							// There is partial debt repayment, using personal liquid asset. Part of the liquid asset are thus
							// used here, and the remaining part is equal to the total expenditures 
					 		this.liquidAsset[1]		= costProduction + costInvestment;  
					 		this.externalFunding		= 0;
					 	} 
					}
				} else if(debt[0] + costToProduceGood * desiredProductionStar <= loan + liquidAsset[1]){
					// Overall, similar structure as the adjustment through substitutionary investments (cf. above)
					// Can achieve its initial production, and set substitutionary investments to zero
					this.productionQuantity 							= desiredProductionStar;
					this.investmentSubstitutionary 					= 0;
					
					// Difference between Mason and our version: firm considers intermediary level of investment in our
					// version, whereas they directly set it to zero in the Mason one.
//					if(model.mason){
					if(model.debtManagement.equals(DebtManagement.Dosi_Et_Al)){
						// The firm directly sets its substitutionary investments to 0
						this.investmentExpansionary					= 0;	
						
						if(debt[0] + costToProduceGood * productionQuantity > liquidAsset[1]){
							// The firm still needs external resources. Note that this does not prevent the stock 
							// of debt to decrease -- if externalFund < 0
							this.externalFunding		= costToProduceGood * productionQuantity - liquidAsset[1];
							this.debt[1]			= debt[0] + externalFunding;
							model.getBank().debt 	+= externalFunding;
							
							if(externalFunding < 0){
								// There is partial debt repayment, using personal liquid asset. Part of the liquid asset are thus
								// used here, and the remaining part is equal to the total expenditures 
						 		this.liquidAsset[1]	= costToProduceGood * productionQuantity;  
						 		this.externalFunding	= 0;
							}
						} else {
							// The firm does not need any external funds once its substitutionary investments are equal to 0
							this.externalFunding 		= 0;
							// It can repay its entire debt 
							this.liquidAsset[1] 	-= debt[0];
							model.getBank().debt 	-= debt[0];
							this.debt[1]			= 0;
						}
						model.getBank().totalCreditRemaining -= debt[1];
					} else {
						// The firm considers intermediary level of expansionary investments
						
						this.investmentExpansionary					= Math.floor( (loan + liquidAsset[1] - 
														debt[0] - costToProduceGood * productionQuantity) / 
														supplier.getPriceOfGoodProducedNow() ) * Parameters.getMachineSizeInCapital_cFirms();

						double costProduction 		= productionQuantity * costToProduceGood;
						double costInvestment 		= ( investmentExpansionary / Parameters.getMachineSizeInCapital_cFirms() ) * supplier.getPriceOfGoodProducedNow();
						this.externalFunding			= costProduction + costInvestment - liquidAsset[1];
						this.debt[1]				= debt[0] + externalFunding;
						model.getBank().totalCreditRemaining -= debt[1];
						model.getBank().debt		+= externalFunding;
							
						if(externalFunding < 0){
							// There is partial debt repayment, using personal liquid asset. Part of the liquid asset are thus
							// used here, and the remaining part is equal to the total expenditures 
					 		this.liquidAsset[1]		= costProduction + costInvestment;  
					 		this.externalFunding		= 0;
					 	} 
					}
				} else { 
					// The loan that the firm received is not sufficient to cover its entire production cost and the funding
					// of its past debt debt.
					// From here onward, no difference between Mason and our version.
					this.investmentSubstitutionary						= 0;
					this.investmentExpansionary						= 0;
					if(debt[0] <= liquidAsset[1] + loan){
						// Current resources are bigger than past debt
						// The firm produces up to what its resources can fund 
						this.productionQuantity 						= (loan + liquidAsset[1] - debt[0]) / costToProduceGood;
						// If it cannot produce at least one unit of final good, it exits (and does not leverage further)
						if(productionQuantity < 1){
							
							this.externalFunding		= 0;
							// The past debt exceeds the firm's resources. It accumulates bad debt
							if(debt[0] > liquidAsset[1]){
								this.debt[1] 		= debt[0] - liquidAsset[1];
								model.getBank().debt -= liquidAsset[1];
								this.liquidAsset[1]	= 0;
								this.badDebt		= debt[1];
								model.getBank().badDebt += debt[1];
							} else {
								model.getBank().debt-= debt[1];
								this.liquidAsset[1] -= debt[1];
								this.debt[1]		= 0;
								this.badDebt		= 0;
							}
							// The firm exits the economy
							this.productionQuantity					= 0;
							this.exit				= true;
							this.marketShare[0] = this.marketShare[1] = this.marketShare[2] = 0;
							
						} else {
							// The firm does not invest but can still reach a positive level of production
							this.externalFunding 		= costToProduceGood * productionQuantity - liquidAsset[1];
							this.debt[1]			= debt[0] + externalFunding;
							model.getBank().debt 	+= externalFunding;
							model.getBank().totalCreditRemaining -= debt[1];
							
							if(externalFunding < 0){
							 	this.liquidAsset[1] = costToProduceGood * productionQuantity;
							 	this.externalFunding = 0;
					 		} 
						}
						
					} else {
						// Previous debt cannot be reimbursed: the firm exits, without leveraging further,
						// but potentially accumulating a stock of bad debt						
						this.productionQuantity 					= 0;
						this.investmentExpansionary 			= 0; 
						this.investmentSubstitutionary 			= 0;
						
						this.externalFunding 		= 0;
						// The firm has a positive stock of debt that cannot be repaid, hence accumulates bad debt
						if(debt[0] > 0){
							this.debt[1]		= debt[0] - liquidAsset[1];
							model.getBank().debt -= liquidAsset[1];
							this.badDebt		= debt[1];
							model.getBank().badDebt += debt[1];
							this.liquidAsset[1]	= 0;
						} else {
							this.debt[1]		= 0;
							this.badDebt		= 0;
						}
						
						// The firm exits.
						this.marketShare[0] = this.marketShare[1] = this.marketShare[2] = 0;
						this.exit 				= true;
						
					}
				}
			}
		}
	}
	
		// ---------------------------------------------------------------------
		// Pseudo Rational / no line of credit 
		// ---------------------------------------------------------------------		
	
	 void pseudoRationalExpendituresUpdate(){
		/* C-firms have received their loans, and therefore know exactly how much they have to spend on production and investment. If the credit demand was nil (1),
		or if the loan is equal to credit demand (2), no further adjustments are needed and the desired quantities turn into actual quantities. When the firm has been
		credit rationed, i.e. loan < credit demand, then it has to undertake subsequent adjustments (3). 
		These a posteriori adjustments are very similar to the a priori adjustments. The only difference is that now, if the firm fails to adjust, it cannot deleverage
		as it already received the loan. 
		
		TODO: what we could do is to proceed as in Dosi et al. code, i.e. if the firm fails to adjust with the loan proposal, then it 
		refuses it to not being further leveraged. 
		 
		 */
		
		log.debug("\t\tCFirm " + this.getKey().getId() + " received a credit of " + loan);
		
		// (1)
		if(creditDemand == 0){
			this.productionQuantity = desiredProductionStar;
			this.investmentExpansionary = desiredInvestmentExpansionaryStar;
			this.investmentSubstitutionary = desiredInvestmentSubstitionaryStar;
			
			log.debug("It had a nil credit demand");
		
		} else {
			// (2)
			if(loan == creditDemand){
				
				this.productionQuantity = desiredProductionStar;
				this.investmentExpansionary = desiredInvestmentExpansionaryStar;
				this.investmentSubstitutionary = desiredInvestmentSubstitionaryStar;
				
				log.debug("Its credit demand was met");
			
			} // (3)
			else {
				/* In this case c-firms' plans will have to change. The easiest way for c-firms to find their new optimal is to restart the adjustment processes from sractch, 
				i.e. (a) to find which plans are achievable with their current resources (which is now know and fixed), (b) to find whether these plans yield expected liquid asset
				that are sufficient to pay the debt at the end of the period */
				
				this.loanForProductionAndInvestment = this.loanForDebtRepayment = 0;
				if(loan == 0){
					// the only resources available are the firms' internal liquid assets. The firm checks whether its original plan are achievable, given that it has a nil loan
					feasibilityNilLoan();
					// and then check if, conditional on this achievable plans, it expects its stock of liquid asset at the end of the period to be sufficient to pay its debt service
					if(balanceSheet.payment(optimalProduction, (long) (investmentExpansionaryStar + investmentSubstitionaryStar), 0, 0) >= 0){
						// Yes they do. Those plans become the actual production and investment of the firm
						this.productionQuantity = optimalProduction;
						this.investmentExpansionary = investmentExpansionaryStar;
						this.investmentSubstitutionary = investmentSubstitionaryStar;
						log.debug("Payment is expected to be positive, no need for further adjustment");
					} else {
						// As in the a priori case, the firm has to modify its production and investment expenditures until it expects to be able to pay its debt
						APosterioriAdjustments.adjustmentWithNilLoan(this);
					}
				} else {
					/* only one firm / period will be concerned by this case, where it has a positvie loan yet smaller than its credit demand. Besides that it has more resources 
				 	than in the above case, the structure is identical */
					feasibilityPositiveLoan();
					if(balanceSheet.payment(optimalProduction, (long) (investmentExpansionaryStar + investmentSubstitionaryStar), loanForDebtRepayment, loanForProductionAndInvestment) >= 0){
						// TODO: note, if the firm has a right to refuse the loan that is offered to it, then should change a bit structure to see whether the firm 
						// can make it without any loanDebt, or if there exists an interior solution.
						this.productionQuantity = optimalProduction;
						this.investmentExpansionary = investmentExpansionaryStar;
						this.investmentSubstitutionary = investmentSubstitionaryStar;
						log.debug("Payment is expected to be positive, no need for further adjustment");
					} else {
						APosterioriAdjustments.adjustmentWithPositiveLoan(this);
					}
				}	
			}
		}
	} 
	
	void feasibilityNilLoan(){
		/* As said above, c-firms retake their initial desired plans (the one derived in initialProductionExpenditures and initialInvestmentExpenditures) and check
		 whether they are achievable, given than their resources is only made of their internal funds.
		 
		 NOTE: instead of dQStar and dInv...Star, we are using qStar and qInv...Star, as this variables are no longer desired -- as in the firm knows at the end of the 
		 adjustment how much it will produce and how much it will invest, which was not the case in the a priori adjustments, as the firm did not know the loan it would
		 receive
		 */
		
		this.optimalProduction = desiredProduction;
		this.investmentExpansionaryStar = desiredInvestmentExpansionary;
		this.investmentSubstitionaryStar = desiredInvestmentSubstitutionary;
		
		double costProduction = costToProduceGood * optimalProduction;
		double costInvExp = supplier.priceOfGoodProduced[1] * investmentExpansionaryStar / Parameters.getMachineSizeInCapital_cFirms();
		double costInvSub = supplier.priceOfGoodProduced[1] * investmentSubstitionaryStar / Parameters.getMachineSizeInCapital_cFirms();
		
		this.liquidAssetRemainingAfterProductionAndInvestment = liquidAsset[0];
		
		/* As for a priori adjustments, the firm has a preference for production over inv. exp., and for inv. exp. over inv. sub. Hence start to see whether their production 
		plan is achievable */
		 if(costProduction <= liquidAssetRemainingAfterProductionAndInvestment)
			 // Yes it is, and the firm reduces its remaining internal fund accordinly
			 this.liquidAssetRemainingAfterProductionAndInvestment -= costProduction;
		 else {
			 // No it is not, s.t. the firm uses its entire internal stock of asset to fund production, and has to scale down its plan
			 this.optimalProduction = liquidAssetRemainingAfterProductionAndInvestment / costToProduceGood;
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		 }
		 
		 // Then, if it has any asset lefts, the firm checks for inv. exp. 
		 if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment)
			 this.liquidAssetRemainingAfterProductionAndInvestment -= costInvExp;
		 else {
			 this.investmentExpansionaryStar = Math.floor(liquidAssetRemainingAfterProductionAndInvestment / supplier.priceOfGoodProduced[1]) * Parameters.getMachineSizeInCapital_cFirms();
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		 }
		 
		 // and finally finishes with sub. inv.
		 if(costInvSub <= liquidAssetRemainingAfterProductionAndInvestment)
			 this.liquidAssetRemainingAfterProductionAndInvestment -= costInvSub;
		 else {
			 this.investmentSubstitionaryStar = Math.floor(liquidAssetRemainingAfterProductionAndInvestment / supplier.priceOfGoodProduced[1]) * Parameters.getMachineSizeInCapital_cFirms();
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		 }
		 
		 log.debug("With nil loan, can achieve the following prod. and inv. :" +
					"\n qStar " + optimalProduction +
					"\n invExStar " + investmentExpansionaryStar +
					"\n invSubStar " + investmentSubstitionaryStar +
					"\n asset remaining " + liquidAssetRemainingAfterProductionAndInvestment);
		
	}
	
	void feasibilityPositiveLoan(){
		// NOTE: this method is exactly identical to the aPrioriFeasibilityPseudoRational() method, except that the firm bases its decision on loan, instead of maxLoan
		
		// re-initialize the variables to their original optimal levels
		this.optimalProduction = desiredProduction;
		this.investmentExpansionaryStar = desiredInvestmentExpansionary;
		this.investmentSubstitionaryStar = desiredInvestmentSubstitutionary;
		
		double costProduction = costToProduceGood * optimalProduction;
		double costInvExp = supplier.priceOfGoodProduced[1] * investmentExpansionaryStar / Parameters.getMachineSizeInCapital_cFirms();
		double costInvSub = supplier.priceOfGoodProduced[1] * investmentSubstitionaryStar / Parameters.getMachineSizeInCapital_cFirms();
		
		this.liquidAssetRemainingAfterProductionAndInvestment = liquidAsset[0];
		double loanPrime = loan;
		
		/* As for a priori adjustments, the firm has a preference for production over inv. exp., and for inv. exp. over inv. sub. Hence start to see whether their production 
		plan is achievable */
		 if(costProduction <= liquidAssetRemainingAfterProductionAndInvestment)
			 // Yes it is, and the firm reduces its remaining internal fund accordinly
			 this.liquidAssetRemainingAfterProductionAndInvestment -= costProduction;
		 else if(costProduction <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			 loanPrime -= costProduction - liquidAssetRemainingAfterProductionAndInvestment;
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		 } else {
			 // No it is not, s.t. the firm uses its entire internal stock of asset to fund production, and has to scale down its plan
			 this.optimalProduction = (liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / costToProduceGood;
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
			 loanPrime = 0;
		 }
		 
		 // Then, if it has any asset lefts, the firm checks for inv. exp. 
		 if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment)
			 this.liquidAssetRemainingAfterProductionAndInvestment -= costInvExp;
		 else if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			 loanPrime -= costInvExp - liquidAssetRemainingAfterProductionAndInvestment;
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		 } else {
			 this.investmentExpansionaryStar = Math.floor((liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / supplier.priceOfGoodProduced[1]) * Parameters.getMachineSizeInCapital_cFirms();
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
			 loanPrime = 0;
		 }
		 
		 // and finally finishes with sub. inv.
		 if(costInvSub <= liquidAssetRemainingAfterProductionAndInvestment)
			 this.liquidAssetRemainingAfterProductionAndInvestment -= costInvSub;
		 else if(costInvExp <= liquidAssetRemainingAfterProductionAndInvestment + loanPrime){
			 loanPrime -= costInvSub - liquidAssetRemainingAfterProductionAndInvestment;
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
		 } else {
			 this.investmentSubstitionaryStar = Math.floor((liquidAssetRemainingAfterProductionAndInvestment + loanPrime) / supplier.priceOfGoodProduced[1]) * Parameters.getMachineSizeInCapital_cFirms();
			 this.liquidAssetRemainingAfterProductionAndInvestment = 0;
			 loanPrime = 0;
		 }
		 
		 this.loanForProductionAndInvestment = loan - loanPrime;
		 this.loanForDebtRepayment = loanPrime;
		 
		 log.debug("With positive loan, can achieve the following prod. and inv. :" +
					"\n qStar " + optimalProduction +
					"\n invExStar " + investmentExpansionaryStar +
					"\n invSubStar " + investmentSubstitionaryStar +
					"\n asset remaining " + liquidAssetRemainingAfterProductionAndInvestment + 
					"\n loan prod " + loanForProductionAndInvestment + 
					"\n loan debt " + loanForDebtRepayment);
		
	}
	
	// ---------------------------------------------------------------------
	// Other methods
	// ---------------------------------------------------------------------
	
	void investmentOrder(){
		// c-firms compute their total investment and send their orders to their supplier
		
		this.investment = investmentExpansionary + investmentSubstitutionary;
		
		if(investment > 0){
			long numberOfMachines 				= (long) (investment / Parameters.getMachineSizeInCapital_cFirms());
			supplier.bookOrder.put(this, numberOfMachines);
		}
	}
	
	public void laborRationing(double ratio){
		// The firm is labor rationed. It reduces its labor demand, and its production accordingly 
				
		this.laborDemand 						*= ratio;
		this.productionQuantity 									*= ratio;
		
	}
	
	// ---------------------------------------------------------------------
	// Capital market methods
	// ---------------------------------------------------------------------
	
	void machineScrapping(){
		/* The sources of scrapping are twofold; 
		 	1. machines that are too old,
		 	2. machines that are less productive than the current vintage of machine. 
		 In both cases, a firm will decide to actually scrap a particular vintage if it has resources to replace it */
		
		// Number of machines that can be replaced
		int numberOfReplacement 				= (int) (investmentSubstitutionary / Parameters.getMachineSizeInCapital_cFirms());
		
		// Rank the machines, from the most to the least costly. The first machine will be the one with the highest cost 
		Map<Machine, Double> machineCost 		= new LinkedHashMap<>();
		for(Machine machine : machinesToBeScrappedMap.keySet())
			machineCost.put(machine, machine.getCost());
		Map<Machine, Double> machineCostRanked 	= MapSorting.sortByValueDescending(machineCost);
		
		// Remove first machines that are too old
		for (Iterator<Map.Entry<Machine, Double>> iterator = machineCostRanked.entrySet().iterator(); iterator.hasNext();) {
			if(numberOfReplacement > 0){
				
				Map.Entry<Machine, Double> entry = iterator.next();
			    if(machineAgeMap.get(entry.getKey()) > Parameters.getMaxAgeMachines_cFirms()){
					
					Machine machine 			= entry.getKey();
					// If a firm cannot afford the replacement, it keeps the machines in their capital stock
					if(machineQuantityMap.get(machine) > numberOfReplacement){
						machineQuantityMap.put(machine, machineQuantityMap.get(machine) - numberOfReplacement);
						numberOfReplacement 	= 0;
					} else {
						numberOfReplacement 	-= machinesToBeScrappedMap.get(machine);
						machineQuantityMap.remove(machine);
						machineAgeMap.remove(machine);
						iterator.remove();
						machine.kill();
					}	
				}  
			} else 
				break;
		}
		
		// Remove the machines that are not sufficiently productive
		for(Iterator<Map.Entry<Machine, Double>> iterator = machineCostRanked.entrySet().iterator(); iterator.hasNext();){
			if(numberOfReplacement > 0){
				Map.Entry<Machine, Double> entry = iterator.next();
				Machine machine 				= entry.getKey();
				// If a firm cannot afford the replacement, it keeps the machines in their capital stock
				if(machineQuantityMap.get(machine) > numberOfReplacement){
					machineQuantityMap.put(machine, machineQuantityMap.get(machine) - numberOfReplacement);					
					numberOfReplacement 		= 0;
				} else {
					numberOfReplacement 		-= machinesToBeScrappedMap.get(machine);
					
					machineQuantityMap.remove(machine);
					machineAgeMap.remove(machine);
					iterator.remove();					
					machine.kill();
				}
			} else {
				break;
			}
		}
	}
	
	void machinePayment(){
		// The firm pays its supplier.
		
		if(investment > 0){
			
			double costInvestment				= investment * supplier.priceOfGoodProduced[1] / Parameters.getMachineSizeInCapital_cFirms();
			
//			if(model.myopicDebtRepayment){
			if(model.debtRepayment.equals(DebtRepayment.Myopic)){
				// The payment can be funded through internal funds and / or external funds
				if(externalFunding > costInvestment){
					this.externalFunding			-= costInvestment;
				} else {
					this.liquidAsset[1]			-= costInvestment - externalFunding;
					this.externalFunding			= 0;
				}
				supplier.sales[1] 				+= costInvestment;
			} else {
				// The payment can be funded through internal funds and / or external funds
				if(loanForProductionAndInvestment > 0){
					if(loanForProductionAndInvestment > costInvestment){
						this.loanForProductionAndInvestment 			-= costInvestment;
					} else {
						this.liquidAsset[0] 	-= costInvestment - loanForProductionAndInvestment;
						this.loanForProductionAndInvestment 			= 0;
					}
				} else {
					this.liquidAsset[0] 		-= costInvestment;					
				}
				supplier.sales[1] 				+= costInvestment;
			}
		} else {
			log.error("Machine payment method while the firm has nil investment.");
		}
	}
	
	// ---------------------------------------------------------------------
	// Good market methods
	// ---------------------------------------------------------------------
	
	void production(){
		/* The firm produces a quantity q of goods that it adds to its inventories. As for investment, the 
		financial structure of the firm determines the source of the expenditures. 
		NOTE: some external resources may remain after paying for production, due to labor rationing. 
		In the myopicDebtRepayment version, the firm is assumed to have access to a credit line. Thus
		it repays / gives back this amount to the bank */
		this.stockFinalGood 					= productionQuantity + inventories[0];
		double costProduction 					= productionQuantity * costToProduceGood;
		
//		if(model.myopicDebtRepayment){
		if(model.debtRepayment.equals(DebtRepayment.Myopic)){
			// The payment can be funded through internal funds and / or external funds
			if(externalFunding > costProduction){
				this.externalFunding				-= costProduction;
			} else {
				this.liquidAsset[1]				-= costProduction - externalFunding;
				this.externalFunding				= 0;
			}
			
			// If remains some external fund, gives it back to the bank
			if(externalFunding > 0){
				this.debt[1]					-= externalFunding;
				model.getBank().debt			-= externalFunding;
				this.externalFunding				= 0;
			}
		} else {
			// The payment can be funded through internal funds and / or external funds
			if(loanForProductionAndInvestment > 0){
				if(loanForProductionAndInvestment > costProduction){					
					this.loanForProductionAndInvestment 				-= costProduction;					
				} else {					
					this.liquidAsset[0] 		-= costProduction - loanForProductionAndInvestment;
					this.loanForProductionAndInvestment 				= 0;					
				}
			} else {				
				this.liquidAsset[0] 			-= costProduction;				
			}
		}	
	}
	
	public void demand(int i){
		// Demand allocation, launched by consumptionAllocation() in KSModel.java
		if(marketShareTemp > 0){
			double demand 						= collector.realConsumption * marketShareTemp;
			if(i == 1)
				this.demand[1]	 				= demand;
			
			if(demand <= stockFinalGood){
				// The firm can match the consumers' demand
				if(i > 1)
					// Increase the firm's demand by the corresponding amount
					this.demand[1] 				+= demand;
				else if (i == 1){
					// Unfilled demand is nil 
					this.unfilledDemand 		= 1;		//XXX: Why is this 1 and not demand?  It seems it is to prevent divide by 0 errors.
				}
				
				// The firm's stock of final good and the real consumption decrease accordingly
				this.stockFinalGood 			-= demand;
				model.consumptionTemp 			-= demand;
			} else {
				// The firm cannot match the consumers' demand
				if(i > 1)
					this.demand[1] 				+= stockFinalGood;
				else if (i == 1)
					this.unfilledDemand 		= 1 + demand - stockFinalGood;	//XXX Why the '1 + '?  It seems it is to prevent divide by 0 errors.
				
				// The stock of real consumption decreases accordingly
				model.consumptionTemp 			-= stockFinalGood;
				// The firm has no more stock and exit the demand allocation process
				this.stockFinalGood 			= 0;
				this.marketShareTemp 						= 0;
			}
		}
	}
	
	// ---------------------------------------------------------------------
	// Accounting methods
	// ---------------------------------------------------------------------
	
	void accounting(){
		this.sales[1] 							= priceOfGoodProduced[1] * Math.min(productionQuantity + inventories[0], demand[1]);
		this.size 								= Math.min(productionQuantity + inventories[0], demand[1]);
		this.grossOperatingSurplus 				= sales[1] - costToProduceGood * productionQuantity;
		
		double debtInterest 					= debt[1] * MacroModel.interestRateOnDebt;
		model.getBank().debtInterest			+= debtInterest;
		double depositRevenue 					= liquidAsset[0] * MacroModel.returnOnFirmsDeposits;
		model.getBank().depositRevenues			+= depositRevenue;
		
		this.profit 							= grossOperatingSurplus + depositRevenue - debtInterest;
		this.inventories[1] 					= Math.max(0, stockFinalGood);
		double diffN 							= inventories[1] - inventories[0];
		collector.diffTotalInventories 			+= diffN;
		
		double debtRepaid 						= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
		
//		if(model.myopicDebtRepayment){
		if(model.debtRepayment.equals(DebtRepayment.Myopic)){
			if(profit > 0){
				collector.govRevenues 			+= profit * model.taxRate;
				this.liquidAsset[1]				+= (1 - model.taxRate ) * profit + productionQuantity;
			} else {
				this.liquidAsset[1]				+= profit + productionQuantity;
			}
			
			if(debt[1] > 0 && !exit){
				if(liquidAsset[1] > debtRepaid){
					model.getBank().debt		-= debtRepaid;
					this.liquidAsset[1]			-= debtRepaid;
					this.debt[1]				-= debtRepaid;
				} else {
					this.debt[1]				-= liquidAsset[1];
					model.getBank().debt		-= liquidAsset[1];
					this.badDebt				= debt[1];
					model.getBank().badDebt		+= badDebt;
					this.liquidAsset[1]			= 0;
					this.exit					= true;
				}
			}
		} else {
			this.liquidAsset[1] 				= liquidAsset[0];
			this.debt[0]						= debt[1];
			
			assetPseudoRational();
		}	
	}
	
	
	void assetPseudoRational(){
		
		// Update the amount of liquid asset. 1st version (on which the closed-form expressions are based) 
		double debtInterest = debt[1] * MacroModel.interestRateOnDebt;
		double depositRevenue = liquidAsset[0] * MacroModel.returnOnFirmsDeposits;
		this.liquidAsset[1] += (1 - model.taxRate) * (sales[1] + depositRevenue - debtInterest);
		/* Note that in this version, the total cost of production is not taken into account in the tax, while it is in the
		accounting method() --> not consistent. The reason is that we already decreased it from liquidAsset[1], when c-firms 
		paid for production, in the same way as investments. 
		Second version might therefore be: 
		this.liquidAsset[1] += (1 - model.taxRate) * (sales[1] + depositRevenue - debtInterest) + model.taxRate * c * q;
		*/
	
		// Recall that loanProd can be positive if the firms have been labor rationed 
		if(loanForProductionAndInvestment > 0)
			this.loanForDebtRepayment += loanForProductionAndInvestment;
		
		log.debug("Before debt repayment, we have " + 
				"\n liquid asset " + liquidAsset[1] + 
				"\n loan debt " + loanForDebtRepayment + 
				"\n debt " + debt[1]);
		
		// Then c-firms repay their debt, if any. Note that they can use their remaining loan debt to do so
		if(liquidAsset[1] >= - Parameters.getErrorThreshold()){
			if(loanForDebtRepayment > 0){
				if(loanForDebtRepayment >= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1]){
					// using only the remaining laon is sufficient for the firm to pay their due debt
					this.debt[1] -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
					this.loanForDebtRepayment -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
					model.getBank().debt -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
				} else if(loanForDebtRepayment + liquidAsset[1] >= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1]){
					// using both the remaining loan and the firm's stock of liquid asset is sufficient for it to pay back its debt
					this.debt[1] -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
					this.liquidAsset[1] -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1] - loanForDebtRepayment;
					this.loanForDebtRepayment = 0;
					model.getBank().debt -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
				} else {
					// the firm cannot pay back its debt --> has to exit the market, and do so with positive debt					
					this.exit = true;
				}	
			} else {
				if(liquidAsset[1] >= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1]){
					// using the firm's stock of liquid asset is sufficient for it to pay back its debt 
					this.debt[1] -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
					this.liquidAsset[1] -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
					model.getBank().debt -= Parameters.getDebtRepaymentSharePerPeriod_cFirms() * debt[1];
				} else {
					// the firm cannot pay back its debt --> has to exit the market, and do so with positive debt
					this.exit = true;
				}	
			}
		} else {
			this.exit = true;
		}	
	
		log.debug("After debt repayment, we have " + 
				"\n liquid asset " + liquidAsset[1] + 
				"\n loan debt " + loanForDebtRepayment + 
				"\n debt " + debt[1] + 
				"\n exit " + exit);
		
		/* Need to do this outside the previous loop, because firms could be pushed out of the market for two reasons: incapacity to honor
		their debt repayment (previous loop), too-low share (earlier in the code). Yet the second case they could also have some remaining debt */
		if(exit && debt[1] > 0)
			debtExit();
		
		// Assumption: if c-firms have some loans left at the end of the period, they are authorized to give them back to the bank 
		if(loanForDebtRepayment > 0 && !exit){
			this.debt[1] -= loanForDebtRepayment;
			model.getBank().debt -=loanForDebtRepayment;
			this.loanForDebtRepayment = 0;
		}	
	}
	
	void debtExit(){
		log.debug("Debt exit procedure with positive debt. Information: " + 
					"\n loanDebt " + loanForDebtRepayment + 
					"\n debt " + debt[1] + 
					"\n assets " + liquidAsset[1] + 
					"\n market share " + marketShare[2]);
		
		// C-firms try to reduce their debt through all the possible mean: (a) the remaining loan, (b) stock of liquid asset 
		if(loanForDebtRepayment > 0){
			this.debt[1] -= loanForDebtRepayment;
			model.getBank().debt -= loanForDebtRepayment;
			this.loanForDebtRepayment = 0;
		}
		
		if(liquidAsset[1] > 0){
			if(liquidAsset[1] > debt[1]){
				// This can for instance be the case for c-firms that exit due to too-low market share. They could be potentially able to repay they entire debt
				this.liquidAsset[1] -= debt[1];
				model.getBank().debt -= debt[1];
				this.debt[1] = 0;
			} else {
				this.debt[1] -= liquidAsset[1];
				model.getBank().debt -= liquidAsset[1];
				this.liquidAsset[1] = 0;
			}
		}
		
		log.debug("After using all the resources, its (bad) debt becomes " + debt[1]); 
		
		if(debt[1] > 0){
			model.getBank().badDebt += debt[1];
		} 
	}
		
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public BalanceSheetCFirm getBalanceSheet() {
		return balanceSheet;
	}

	public void setBalanceSheet(BalanceSheetCFirm balanceSheet) {
		this.balanceSheet = balanceSheet;
	}

	public Set<KFirm> getPotentialKfirmsSet() {
		return potentialKfirmsSet;
	}

	public void setPotentialKfirmsSet(Set<KFirm> potentialKfirmsSet) {
		this.potentialKfirmsSet = potentialKfirmsSet;
	}

	public Map<Machine, Integer> getMachineAgeMap() {
		return machineAgeMap;
	}

	public void setMachineAgeMap(Map<Machine, Integer> machineAgeMap) {
		this.machineAgeMap = machineAgeMap;
	}

	public Map<Machine, Integer> getMachineQuantityMap() {
		return machineQuantityMap;
	}

	public void setMachineQuantityMap(Map<Machine, Integer> machineQuantityMap) {
		this.machineQuantityMap = machineQuantityMap;
	}

	public KFirm getSupplier() {
		return supplier;
	}

	public void setSupplier(KFirm supplier) {
		this.supplier = supplier;
	}

	public double[] getMarkUpRate() {
		return markUpRate;
	}

	public void setMarkUpRate(double[] markUpRate) {
		this.markUpRate = markUpRate;
	}

	public double getUnfilledDemand() {
		return unfilledDemand;
	}

	public void setUnfilledDemand(double unfilledDemand) {
		this.unfilledDemand = unfilledDemand;
	}

	public double getCapitalStock() {
		return capitalStock;
	}

	public void setCapitalStock(double capitalStock) {
		this.capitalStock = capitalStock;
	}

	public double getCompetitivess() {
		return competitiveness;
	}

	public void setCompetitiveness(double competitiveness) {
		this.competitiveness = competitiveness;
	}

	public double getInvestment() {
		return investment;
	}

	public void setInvestment(double investment) {
		this.investment = investment;
	}

	public double getInvestmentExpansionary() {
		return investmentExpansionary;
	}

	public void setInvestmentExpansionary(double investmentExpansionary) {
		this.investmentExpansionary = investmentExpansionary;
	}

	public double getInvestmentSubstitutionary() {
		return investmentSubstitutionary;
	}

	public void setInvestmentSubstitutionary(double investmentSubstitutionary) {
		this.investmentSubstitutionary = investmentSubstitutionary;
	}

	public double getCreditDemand() {
		return creditDemand;
	}

	public void setCreditDemand(double creditDemand) {
		this.creditDemand = creditDemand;
	}

	public double[] getInventories() {
		return inventories;
	}

	public void setInventories(double[] inventories) {
		this.inventories = inventories;
	}

	public double getExpectedDemand() {
		return expectedDemand;
	}

	public void setExpectedDemand(double expectedDemand) {
		this.expectedDemand = expectedDemand;
	}

	public double getdQ() {
		return desiredProduction;
	}

	public void setdQ(double desiredProduction) {
		this.desiredProduction = desiredProduction;
	}

	public double getdInvestmentExpansionary() {
		return desiredInvestmentExpansionary;
	}

	public void setdInvestmentExpansionary(double dInvestmentExpansionary) {
		this.desiredInvestmentExpansionary = dInvestmentExpansionary;
	}

	public double getdInvestmentSubstitutionary() {
		return desiredInvestmentSubstitutionary;
	}

	public void setdInvestmentSubstitutionary(double desiredInvestmentSubstitutionary) {
		this.desiredInvestmentSubstitutionary = desiredInvestmentSubstitutionary;
	}

	public double getDesiredInventories() {
		return desiredInventories;
	}

	public void setDesiredInventories(double desiredInventories) {
		this.desiredInventories = desiredInventories;
	}

	public double getProductionStar() {
		return desiredProductionStar;
	}

	public void setDesiredProductionStar(double desiredProductionStar) {
		this.desiredProductionStar = desiredProductionStar;
	}

	public double getOptimalProduction() {
		return optimalProduction;
	}

	public void setOptimalProduction(double optimalProduction) {
		this.optimalProduction = optimalProduction;
	}

	public double getdInvestmentExpansionaryStar() {
		return desiredInvestmentExpansionaryStar;
	}

	public void setDesiredInvestmentExpansionaryStar(double desiredInvestmentExpansionaryStar) {
		this.desiredInvestmentExpansionaryStar = desiredInvestmentExpansionaryStar;
	}

	public double getInvestmentExpansionaryStar() {
		return investmentExpansionaryStar;
	}

	public void setInvestmentExpansionaryStar(double investmentExpansionaryStar) {
		this.investmentExpansionaryStar = investmentExpansionaryStar;
	}

	public double getDesiredInvestmentSubstitionaryStar() {
		return desiredInvestmentSubstitionaryStar;
	}

	public void setDesiredInvestmentSubstitionaryStar(double desiredInvestmentSubstitionaryStar) {
		this.desiredInvestmentSubstitionaryStar = desiredInvestmentSubstitionaryStar;
	}

	public double getInvestmentSubstitionaryStar() {
		return investmentSubstitionaryStar;
	}

	public void setInvestmentSubstitionaryStar(double investmentSubstitionaryStar) {
		this.investmentSubstitionaryStar = investmentSubstitionaryStar;
	}

	public double getLoanForProductionAndInvestment() {
		return loanForProductionAndInvestment;
	}

	public void setLoanForProductionAndInvestment(double loanForProductionAndInvestment) {
		this.loanForProductionAndInvestment = loanForProductionAndInvestment;
	}

	public double getLoanForDebtRepayment() {
		return loanForDebtRepayment;
	}

	public void setLoanForDebtRepayment(double loanForDebtRepayment) {
		this.loanForDebtRepayment = loanForDebtRepayment;
	}

	public double getLiquidAssetRemainingAfterProductionAndInvestment() {
		return liquidAssetRemainingAfterProductionAndInvestment;
	}

	public double getMaxPossibleLoan() {
		return maxPossibleLoan;
	}

	public double getLoan() {
		return loan;
	}

	public void setLoan(double loan) {
		this.loan = loan;
	}

	public double getMarketShareTemp() {
		return marketShareTemp;
	}

	public void setMarketShareTemp(double marketShareTemp) {
		this.marketShareTemp = marketShareTemp;
	}

	public double getDesiredCapitalStock() {
		return desiredCapitalStock;
	}

	public void setDesiredCapitalStock(double desiredCapitalStock) {
		this.desiredCapitalStock = desiredCapitalStock;
	}

	public double getMaxPossibleCapitalStock() {
		return maxPossibleCapitalStock;
	}

	public void setMaxPossibleCapitalStock(double maxPossibleCapitalStock) {
		this.maxPossibleCapitalStock = maxPossibleCapitalStock;
	}

	public double getProductivity() {
		return productivity;
	}

	public void setProductivity(double productivity) {
		this.productivity = productivity;
	}
	
	public double getBadDebt(){
		return badDebt;
	}

	public double getNetWorthToSalesRatio() {
		return netWorthToSalesRatio;
	}

	public PanelEntityKey getKey() {
		return key;
	}

}