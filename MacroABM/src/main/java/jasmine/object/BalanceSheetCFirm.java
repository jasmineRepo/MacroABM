package jasmine.object;

import org.apache.log4j.Logger;

import jasmine.data.Parameters;
import jasmine.model.CFirm;
import jasmine.model.MacroModel;

public class BalanceSheetCFirm {
	
	private final static Logger log = Logger.getLogger(BalanceSheetCFirm.class);
	
	// the balance sheet can be considered as an accountant, or simply as an object, that collects all the financial information of a firm, and perform
	// the computation related to it 
	
	private CFirm cFirm;
	
	// Stocks
	private double debt;	
	private double liquidAsset;
	
	// Flows
		// in
	private double sales;
	private double depositRevenues;
	
		// out
	private double wageBill;
	private double invExpenditure;
	private double debtInterest;
	private double debtRepayment;
	
	MacroModel model;
	
	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------
	
	public BalanceSheetCFirm(){
		
	}
	
	public BalanceSheetCFirm(CFirm cFirm, MacroModel model){
		
		this.cFirm = cFirm;
		this.model = model;
		
	}
	
	// ---------------------------------------------------------------------
	// Methods
	// ---------------------------------------------------------------------
	
	public double payment(double q, long inv, double lD, double lP){
		
		double sales = cFirm.getPriceOfGoodProducedNow() * Math.min(q + cFirm.getInventories()[0], cFirm.getDemand()[0]);
		double costProd = cFirm.getCostToProduceGood() * q;
		double costInv = inv * cFirm.getSupplier().getPriceOfGoodProducedNow() / Parameters.getMachineSizeInCapital_cFirms();
		
		double liqAssetRemain = Math.max(0, cFirm.getLiquidAsset()[0] - costProd - costInv);
		
		double payment = (1 - model.getTaxRate()) * (sales  + MacroModel.getrDepo() * liqAssetRemain - MacroModel.getrDebt() * (debt + lD + lP)) + 
				liqAssetRemain + lD - Parameters.getDebtRepaymentSharePerPeriod_cFirms() * (debt + lP + lD);
		
		log.info("Payment information: payment: " + payment);
		
		return payment;
	}
	
	public void profit(){
		
	}
	
	public void kill(){
		
		this.cFirm = null;
		
	}
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public CFirm getcFirm() {
		return cFirm;
	}

	public void setcFirm(CFirm cFirm) {
		this.cFirm = cFirm;
	}

	public double getDebt() {
		return debt;
	}

	public void setDebt(double debt) {
		this.debt = debt;
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

	public double getInvExpenditure() {
		return invExpenditure;
	}

	public void setInvExpenditure(double invExpenditure) {
		this.invExpenditure = invExpenditure;
	}

	public double getDebtInterest() {
		return debtInterest;
	}

	public void setDebtInterest(double debtInterest) {
		this.debtInterest = debtInterest;
	}

	public double getDebtRepayment() {
		return debtRepayment;
	}

	public void setDebtRepayment(double debtRepayment) {
		this.debtRepayment = debtRepayment;
	}
	
}
