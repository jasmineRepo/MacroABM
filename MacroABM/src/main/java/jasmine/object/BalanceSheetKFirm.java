package jasmine.object;

import jasmine.model.KFirm;
import jasmine.model.MacroModel;

public class BalanceSheetKFirm {
	
	// the balance sheet can be considered as an accountant, or simply as an object, that collects all the financial information of a firm, and perform
	// the computation related to it 
	
	private KFirm kFirm;
	
	// stock
	private double liquidAsset;
	
	// flow
		// in
	private double sales;
	private double depositRevenues;
	
		// out
	private double wageBill;
	
	MacroModel model;
	
	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------
	
	public BalanceSheetKFirm(){
		
	}
	
	public BalanceSheetKFirm(KFirm kFirm, MacroModel model){
		
		this.kFirm = kFirm;
		this.model = model;
		
	}
	
	public void kill(){
		
		this.kFirm = null;
		
	}
	
	// ---------------------------------------------------------------------
	// Methods
	// ---------------------------------------------------------------------
	
	public void clearFlows(){
		// in every periods, the flows are reset equal to 0
		
		this.sales = 0;
		this.depositRevenues = 0;
		this.wageBill = 0;
		
	}
	
	public void profit(){
		
	}
	
	public void nextLiquidAsset(){
		
	}
	
	public void increaseWageBill(double increase){
		this.wageBill += increase;
	}
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public KFirm getkFirm() {
		return kFirm;
	}

	public void setkFirm(KFirm kFirm) {
		this.kFirm = kFirm;
	}

	public double getLiquidAsset() {
		return liquidAsset;
	}

	public void setLiquidAsset(double liquidAsset) {
		this.liquidAsset = liquidAsset;
	}

	public double getSales() {
		return sales;
	}

	public void setSales(double sales) {
		this.sales = sales;
	}

	public double getDepositRevenues() {
		return depositRevenues;
	}

	public void setDepositRevenues(double depositRevenues) {
		this.depositRevenues = depositRevenues;
	}

	public double getWageBill() {
		return wageBill;
	}


	public void setWageBill(double wageBill) {
		this.wageBill = wageBill;
	}
	
}
