package jasmine.model;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class Firm extends Agent {

	// This class defines all variables that both types of firm have in common
	
	// financial variables
	@Transient
	protected double[] liquidAsset;
	@Transient
	protected double[] debt;

	protected double profit;
	
	// production-side variables
	@Transient
	protected double[] priceOfGoodProduced; // was p, p stands for price

	protected double costToProduceGood; // was c, c stands for cost 

	protected double productionQuantity; // was q, q stands for production

	protected double laborDemand;
	@Transient
	protected double[] demand; 
	@Transient
	protected double[] sales; 
	
	// info. on the firms
	protected double size; // the size of the economy is a function either of the firm's demand, or the firm's stock of goods, depending on which is the smallest 
	@Transient
	protected double[] marketShare; // market share, was f. 

	protected boolean exit; // exit = true when firms are exiting the economy 

	protected boolean newEntrant; //newEntrant = true when firm enters the economy for the first time


	// ---------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------
	
	public Firm() {
		super();
	}
	
	public Firm(boolean newFirm){
		super();
		
		// --- Define initial variables that are common to consumption and capital-good firms ---
		// firm starts off with no debt
		this.debt 				= new double[]{0, 0}; 
		this.exit 				= false;
		this.newEntrant 		= false;
		
	}

	
	// ---------------------------------------------------------------------
	// Destructor
	// ---------------------------------------------------------------------

	public void kill(){
		
		super.kill();
		
		this.liquidAsset 		= null;
		this.demand 			= null;
		this.sales 				= null;
		this.marketShare 					= null;
		this.priceOfGoodProduced 					= null;
		this.debt 				= null;
		// All the other firm's variables are primitives and do not need to removed manually
		
	}
	
	public void update(){
		
		// Update all temporal arrays at the firm level, from (t) to (t-1)
		this.liquidAsset[0] 	= liquidAsset[1];
		
		this.debt[0] 			= debt[1];
		this.demand[0] 			= demand[1];
		this.priceOfGoodProduced[0] 				= priceOfGoodProduced[1];
		this.sales[0] 			= sales[1];
		this.marketShare[0] 				= marketShare[1];
		this.marketShare[1] 				= marketShare[2];
		this.laborDemand		= 0;
		
	}

	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public double[] getLiquidAsset() {
		return liquidAsset;
	}
	
	public double getLiquidAssetNow(){
		return liquidAsset[1];
	}

	public void setLiquidAsset(double[] liquidAsset) {
		this.liquidAsset = liquidAsset;
	}

	public double[] getDebt() {
		return debt;
	}

	public void setDebt(double[] debt) {
		this.debt = debt;
	}

	public double getProfit() {
		return profit;
	}

	public void setProfit(double profit) {
		this.profit = profit;
	}

	public double getPriceOfGoodProducedNow(){
		return priceOfGoodProduced[1];
	}
	
	public double getPriceOfGoodProducedPrevious(){
		return priceOfGoodProduced[0];
	}
	
	public double[] getPriceOfGoodProduced() {
		return priceOfGoodProduced;
	}

	public void setPriceOfGoodProduced(double[] priceOfGoodProduced) {
		this.priceOfGoodProduced = priceOfGoodProduced;
	}

	public double getCostToProduceGood() {
		return costToProduceGood;
	}

	public void setCostToProduceGood(double costToProduceGood) {
		this.costToProduceGood = costToProduceGood;
	}

	public double getProductionQuantity() {
		return productionQuantity;
	}

	public void setProductionQuantity(double productionQuantity) {
		this.productionQuantity = productionQuantity;
	}

	public double getLaborDemand() {
		return laborDemand;
	}

	public void setLaborDemand(double laborDemand) {
		this.laborDemand = laborDemand;
	}

	public double[] getDemand() {
		return demand;
	}
	
	public double getDemandNow() {
		return demand[1];
	}

	public void setDemand(double[] demand) {
		this.demand = demand;
	}

	public double[] getSales() {
		return sales;
	}

	public void setSales(double[] sales) {
		this.sales = sales;
	}

	public double getSize() {
		return size;
	}

	public void setSize(double size) {
		this.size = size;
	}

	public double[] getMarketShare() {
		return marketShare;
	}

	public void setMarketShare(double[] marketShare) {
		this.marketShare = marketShare;
	}

	public boolean isExit() {
		return exit;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}
	
}