package jasmine.model;

import jasmine.algorithms.MapSorting;
import jasmine.data.Parameters;
import jasmine.enums.DebtRepayment;
import microsim.data.db.PanelEntityKey;
import microsim.event.Order;
import microsim.event.SystemEventType;
import microsim.statistics.IDoubleSource;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

@Entity
public class Bank extends Agent implements IDoubleSource {

	private final static Logger log = Logger.getLogger(Bank.class);
	
	@Id
	private PanelEntityKey key;

	@Transient
	static long bankIdCounter = 0l;

	// ---------------------------------------------------------------------
	// Variables
	// ---------------------------------------------------------------------

	//TODO: Make these fields private!!!
	
	public double debt;

	public double equity;

	public double creditSupply;

	public double totalCreditRemaining; // variable used in the credit allocation process 

	public double reserves;

	public double cash;

	public double deposits;

	public double monetaryBase;

	public double profit;

	public double badDebt;

	public double depositRevenues;

	public double debtInterest;
	
	@Transient
	Map<CFirm, Double> creditMap; // map that surveys the firms with positive credit demand, and their net-worth-to-sale ratio
	
	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
		Update,
		CreditAllocation,
		Accounting;
	}

	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case Update:
			update();
			break;
		
		case CreditAllocation:
			creditAllocation();
			break;
			
		case Accounting:
			accounting();
			break;
		}
	}
	
	// ---------------------------------------------------------------------
	// ISource
	// ---------------------------------------------------------------------
	
	public enum Variables {
		MonetaryBase,
		CreditSupply,
		LogCreditSupply,
		LogBadDebt,
		LogDebt;
	}
	
	@Override
	public double getDoubleValue(Enum<?> variableDouble) {
		switch((Variables) variableDouble){
		
		case MonetaryBase:
			return monetaryBase;
			
		case CreditSupply:
			return creditSupply;
			
		case LogCreditSupply:
			if(creditSupply > 1)
				return Math.log(creditSupply);
			else 
				return 0.;
			
		case LogBadDebt:
//			return badDebt;
			if(badDebt > 0)
				return Math.log(badDebt);
			else return Double.NaN;
			
		case LogDebt:
//			return debt;
			if(debt > 0)
				return Math.log(debt);
			else return Double.NaN;
			
		default: 
			throw new IllegalArgumentException("Unsupported variable"); 
		
		}
	}

	// ---------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------
	
	//Default constructor, used to export data to database.
	public Bank() {
		super();
	}
	
	/* Note that the boolean argument newBank is a dummy variable used only to distinguish this constructor from 
	 * the default constructor, which is used by the database export process (in which we do not want to increment 
	 * the id counter, otherwise it will increment the counter without actually creating new banks in the model).
	 * The actual value of the boolean newBank is ignored. 
	 */
	public Bank(boolean newBank){
		super();
		
		this.key = new PanelEntityKey(bankIdCounter++);
				
		this.creditMap 						= new LinkedHashMap<>();
		
		// Note: only the liquid assets of consumption-good firms are taken into account.
		this.monetaryBase 						= Parameters.getNetLiquidAssets_cFirms() * ((double) model.getNumberOfCFirms());
		this.reserves 							= monetaryBase;
		this.equity 							= monetaryBase;
		this.cash 								= monetaryBase;
		
		this.deposits 							= 0;
		this.creditSupply 						= 0;	
		this.debt = this.badDebt 				= 0;
		
	}
	
	
//	// ---------------------------------------------------------------------
//	// Destructor
//	// ---------------------------------------------------------------------
//	
//	public void kill(){
//		
//		super.kill();
//		this.creditQueue 						= null;
//		
//	}
	
	// ---------------------------------------------------------------------
	// Own methods
	// ---------------------------------------------------------------------

	void update(){
		
		this.creditMap.clear();
		
		// In Dosi et al. implementation, only deposits of consumption-good firms are taken into account to compute the total level of credit 
		this.monetaryBase 						= 0;
		for(CFirm cFirm : model.getCFirms())
			this.monetaryBase 					+= cFirm.liquidAsset[0];  
		
		// Comparison with Dosi et al. (2013): there, MTC = multiplier * deposits. Here, identical, but express the multiplier in a different
		// fashion (reserve requirement rate = 1 / (1 + multiplier)). Thus, if multiplier = 2, then requirement rate = 1/3.
		this.deposits 							= monetaryBase / model.getReserveRequirementRate();
		this.reserves 							= monetaryBase;
		this.creditSupply 						= (1 - model.getReserveRequirementRate()) * deposits;
		this.totalCreditRemaining 				= creditSupply;
		
		this.depositRevenues					= 0;
		this.debtInterest						= 0;
		
	}
	
	void creditAllocation(){
		/* The bank compares the aggregate credit demand to its credit supply. If the aggregate credit demand is bigger, 
		the bank has to rank firms according to their net-worth-to-sale ratio. It then distributes the credit until it runs out of funds. 
		
		NOTE: there is a difference between the myopic version of the model, and the pseudo rational one. 
			1. Myoic: firms have access to a line of credit, and thus can decide how much to consume out of the loan that
			the bank offers them.  
 			2. Pseudo-rational firms do not have access to a line of credit. Once the loan is granted, the firm uses it.  */
		
		// Compute the aggregate credit demand
		collector.aggregateCreditDemand 		= 0;
		for(CFirm cFirm : creditMap.keySet()){
			collector.aggregateCreditDemand 	+= cFirm.creditDemand;
		}
		
		log.debug("The aggregate credit demand is " + collector.aggregateCreditDemand);
		
		if(collector.aggregateCreditDemand <= creditSupply){
			log.debug("Agg. demand < agg. supply");
			
			// Each firm receives its credit demand
			for(CFirm cFirm : creditMap.keySet()){
//				if(model.myopicDebtRepayment){
				if(model.debtRepayment.equals(DebtRepayment.Myopic)){					
					cFirm.loan 					= cFirm.creditDemand;
					cFirm.myopicExpendituresUpdate();
				} else {
					cFirm.loan 					= cFirm.creditDemand;
					cFirm.debt[1] 				+= cFirm.loan;
				}
			}
		} 
		else {
			// Sort the consumption-good firms by their net worth to sale ratio 
			creditMap = (Map<CFirm, Double>) MapSorting.sortByValueDescending(creditMap);
			log.debug("Agg. demand > agg. supply. Initial credit remaining: " + totalCreditRemaining);
			
			for(CFirm cFirm : creditMap.keySet()){
				
				if(totalCreditRemaining < 0){
					totalCreditRemaining 		= 0;
					log.error("Total credit remaining < 0");
				}
				
//				if(model.myopicDebtRepayment){
				if(model.debtRepayment.equals(DebtRepayment.Myopic)){
					if(cFirm.creditDemand <= totalCreditRemaining){
						cFirm.loan 				= cFirm.creditDemand;
					} else {
						cFirm.loan 				= totalCreditRemaining;
					}
					cFirm.myopicExpendituresUpdate();
				} else {
					if(cFirm.creditDemand <= totalCreditRemaining && totalCreditRemaining > 0){
						
						cFirm.loan 				= cFirm.creditDemand;
						cFirm.debt[1] 			+= cFirm.creditDemand;
						this.debt 				+= cFirm.creditDemand;
						
						log.debug("CFirm " + cFirm.getKey().getId() + " gets a loan of " + cFirm.loan);
						
						// the remaining credit supply is reduced accordingly 
						totalCreditRemaining 	-= cFirm.creditDemand;
						
					} else {
						
						cFirm.loan 				= totalCreditRemaining;
						cFirm.debt[1] 			+= totalCreditRemaining; 
						this.debt 				+= totalCreditRemaining; 
						
						log.debug("CFirm " + cFirm.getKey().getId() + " gets a loan of " + cFirm.loan);
						
						// the credit supply reaches 0 --> exit the loop
						totalCreditRemaining 	= 0;
					}
				}
			}
		}
	}
	
	void accounting(){
		
//		if(!model.myopicDebtRepayment){
		if(model.debtRepayment.equals(DebtRepayment.Psuedo_Rational)){
			this.debt 							= 0;
			this.badDebt 						= 0;
			for(CFirm cFirm : model.getCFirms()){
				this.debt 						+= cFirm.debt[1];
				this.badDebt 					+= cFirm.badDebt;
			}
			if(this.badDebt > 0.) {
				System.out.println("Bank.BadDebt = " + this.badDebt);
			}
		} 
		
		
		// If the economy has no debt, the bank is not doing any activity. Its profit is nil.
		if(this.debtInterest == 0)
			this.profit 						= 0;
		else
			this.profit 						= debtInterest - depositRevenues;
			// this.profit = debtRevenues - depositInterest + MacroModel.rCb * (cash + reserves);
		
		
		// The government taxes the bank's profit 
		if(profit > 0){
			collector.govRevenues 				+= model.taxRate * profit;
			this.cash 							-= model.taxRate * profit;
		}
		this.cash 								+= profit;
		
		// And the bank computes its new level of equity. FIXME: why equity = and not equity +=, especially given that
		// we specified an initial value
		this.equity 							= cash + reserves - badDebt;
		
		log.debug("BANK ACCOUNTING: Before the potential intervention of the government, we have " + 
					"\n returns on debt " + debtInterest +
					"\n interest on firm's deposits " + depositRevenues +
					"\n profit " + profit +
					"\n cash " + cash +
					"\n equity " + equity);
		
		if(equity < 0){ // FIXME: should not the bank first pay with its own fund the bad debt, and only afterwards
			// the government steps in ?
			collector.bankDifficulty			= true;
			// The bank has difficulty to cover its loss with its own liquidity. The government steps in, 
			// first to cover the bad debt 
			this.equity 						+= badDebt;
			collector.govSpending 				+= badDebt;
			this.badDebt 						= 0;
			
//			// POTENTIAL NEW VERSION
//			 this.badDebt						-= reserves;
//			 this.reserves						= 0;
//			 if(cash > 0)
//			 		this.badDebt				-= cash;
//			 this.cash							= 0;
//			 collector.govSpending				+= badDebt;
//			 this.badDebt						= 0;
			 
						
			// If the cash of the bank is also negative, the government pays for it as well 
			if(cash < 0){
				this.equity 					+= - cash;
				collector.govSpending 			+= cash;
				this.cash 						= 0;
			}
			// If these actions are not sufficient, the bank cannot be saved. The economy stops. 
			if(equity < 0){
				log.fatal("Equity < 0 after gov. intervention. System stops");
				model.getEngine().getEventQueue().scheduleSystem(model.getEngine().getTime(), Order.AFTER_ALL.getOrdering(), 0., model.getEngine(), SystemEventType.End);
			}
		} else {
			// Otherwise the bank could fully cover its losses with its cash and reserves 
			// FIXME: should not update equity accordingly ? Or cash? 
			// NOTE: in Mason code, does not reset bad debt like this... Keep it positive, which is why it grows continuously
			// until it burst and the government has to step in
			this.badDebt 						= 0;		//XXX: In the charts, this makes the Bank's bad debt always 0!  Is this in the wrong place?  Because if it is not reset to 0, the chart gets a monotonically increasing amount of bad debt.  However, my (Ross) own bad debt chart that sums up the cFirms bad debt at each time-step is not monotonically increasing.  Perhaps this is because cFirm bad debt disappears if(when) the cFirm exits, whereas it doesn't disappear from the Bank's accounts, so just accumulates.  Which is the correct way?
//			// POTENTIAL NEW VERSION
//			 if(reserves > badDebt){
//			 	this.reserves					-= badDebt;
//			 } else {
//			 	this.cash						-= badDebt - reserves;
//			 	this.reserves					= 0;
//			 }
//			 this.badDebt						= 0;

			collector.bankDifficulty			= 0;
		}
		
	}

	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public PanelEntityKey getKey() {
		return key;
	}
	
	
}