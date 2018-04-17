package jasmine.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import microsim.data.db.PanelEntityKey;

/* NOTE: to be able to export data into .csv files, the class from which the collector is exporting must contains
a PanelEntityKey; hence they also must be entities. It follows that we cannot export statistics directly from
the collector. Thus we are creating this class which simply collects the data we want to dump. 
 */

@Entity
public class MacroStatistics {

	@Id
	private PanelEntityKey key;
	
	// General macro variables 
//	@Column(name="GDP")
	private double gdp;
//	@Column(name="GDP_growth")
	private double gdpGrowth;
//	@Column(name="Consumption")
	private double consumption;
//	@Column(name="Consumption_Growth")
	private double consumptionGrowth;
//	@Column(name="Real_Consumption")
	private double realConsumption;
//	@Column(name="Total_Factor_Productivity")
	private double totalFactorProductivity;
//	@Column(name="Wage")
	private double wage;
//	@Column(name="Unemployment_Rate")
	private double unemploymentRate;
//	@Column(name="Labor_Demand")
	private double laborDemand;
//	@Column(name="CPI")
	private double cpi;
//	@Column(name="PPI")
	private double ppi;
	
	// C-firms aggregate variables
//	@Column(name="Production_cFirms")
	private double production_cFirms;
//	@Column(name="Profit_cFirms")
	private double profit_cFirms;
//	@Column(name="Exit_cFirms")
	private int exit_cFirms;
//	@Column(name="Herfindahl_cFirms")
	private double herfindahlMeasure_cFirms;
//	@Column(name="Mean_Productivity_cFirms")
	private double meanProductivityWeightedByMarketShare_cFirms;
//	@Column(name="Investment_Expansionary")
	private double investmentExpansionaryTotal;
//	@Column(name="Investment_Substitionary")
	private double investmentSubstitionaryTotal;
//	@Column(name="Investment_Growth")
	private double investmentGrowth;
//	@Column(name="Bad_Debt")
	private double badDebt;
//	@Column(name="Bankruptcy_Rate")
	private double bankruptcyRate;
//	@Column(name="Liquidity_Sales_Ratio")
	private double liquidityToSalesRatio;
	
	// K-firms aggregate variables
//	@Column(name="Production_kFirms")
	private double production_kFirms;
//	@Column(name="Profit_kFirms")
	private double profit_kFirms;
//	@Column(name="Exit_kFirms")
	private int exit_kFirms;
//	@Column(name="Herfindahl_kFirms")
	private double herfindahlMeasure_kFirms;
//	@Column(name="Mean_Productivity_kFirms")
	private double meanProductivityWeightedByMarketShare_kFirms;
	
	// R&D 
//	@Column(name="Mean_Machine_Productivity")
	private double meanMachineProductivityWeightedByMarketShare;
//	@Column(name="Top_Productivity_kFirms")
	private double topProductivity_kFirms;
//	@Column(name="Top_Machine_Productivity")
	private double topMachineProductivity;
	
	// Bank
//	@Column(name="Credit_Demand")
	private double creditDemand;
//	@Column(name="Credit_Supply")
	private double creditSupply;
//	@Column(name="Total_Debt")
	private double totalDebt;
	
	// Government
//	@Column(name="Gov_Spending")
	private double govSpending;
//	@Column(name="Gov_Revenues")
	private double govRevenues;
//	@Column(name="Gov_Debt")
	private double govDebt;
	
	
	// ---------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------
	
	public MacroStatistics() {
		
		key = new PanelEntityKey(0l);
		
	}
	
	// Getters and Setters 
	
	public double getGDP() {
		return gdp;
	}
	public void setGDP(double gdp) {
		this.gdp = gdp;
	}
	public double getConsumption() {
		return consumption;
	}
	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}
	public double getRealConsumption() {
		return realConsumption;
	}
	public void setRealConsumption(double realConsumption) {
		this.realConsumption = realConsumption;
	}
	public double getTotalFactorProductivity() {
		return totalFactorProductivity;
	}
	public void setTotalFactorProductivity(double totalFactorProductivity) {
		this.totalFactorProductivity = totalFactorProductivity;
	}
	public double getWage() {
		return wage;
	}
	public void setWage(double wage) {
		this.wage = wage;
	}
	public double getUnemploymentRate() {
		return unemploymentRate;
	}
	public void setUmemploymentRate(double unemploymentRate) {
		this.unemploymentRate = unemploymentRate;
	}
	public double getLaborDemand() {
		return laborDemand;
	}
	public void setLaborDemand(double laborDemand) {
		this.laborDemand = laborDemand;
	}
	public double getCPI() {
		return cpi;
	}
	public void setCPI(double cpi) {
		this.cpi = cpi;
	}
	public double getPPI() {
		return ppi;
	}
	public void setPPI(double ppi) {
		this.ppi = ppi;
	}
	public double getProduction_cFirms() {
		return production_cFirms;
	}
	public void setProduction_cFirms(double production_cFirms) {
		this.production_cFirms = production_cFirms;
	}
	public double getProfit_cFirms() {
		return profit_cFirms;
	}
	public void setProfit_cFirms(double profit_cFirms) {
		this.profit_cFirms = profit_cFirms;
	}
	public int getExit_cFirms() {
		return exit_cFirms;
	}
	public void setExit_cFirms(int exit_cFirms) {
		this.exit_cFirms = exit_cFirms;
	}
	public double getHerfindahlMeasure_cFirms() {
		return herfindahlMeasure_cFirms;
	}
	public void setHerfindahlMeasure_cFirms(double herfindahlMeasure_cFirms) {
		this.herfindahlMeasure_cFirms = herfindahlMeasure_cFirms;
	}
	public double getMeanProductivityWeightedByMarketShare_cFirms() {
		return meanProductivityWeightedByMarketShare_cFirms;
	}
	public void setMeanProductivityWeightedByMarketShare_cFirms(double meanProductivityWeightedByMarketShare_cFirms) {
		this.meanProductivityWeightedByMarketShare_cFirms = meanProductivityWeightedByMarketShare_cFirms;
	}
	public double getInvestmentExpansionaryTotal() {
		return investmentExpansionaryTotal;
	}
	public void setInvestmentExpansionaryTotal(double investmentExpansionaryTotal) {
		this.investmentExpansionaryTotal = investmentExpansionaryTotal;
	}
	public double getInvestmentSubstitionaryTotal() {
		return investmentSubstitionaryTotal;
	}
	public void setInvestmentSubstitionaryTotal(double investmentSubstitionaryTotal) {
		this.investmentSubstitionaryTotal = investmentSubstitionaryTotal;
	}
	public double getProduction_kFirms() {
		return production_kFirms;
	}
	public void setProduction_kFirms(double production_kFirms) {
		this.production_kFirms = production_kFirms;
	}
	public double getProfit_kFirms() {
		return profit_kFirms;
	}
	public void setProfit_kFirms(double profit_kFirms) {
		this.profit_kFirms = profit_kFirms;
	}
	public int getExit_kFirms() {
		return exit_kFirms;
	}
	public void setExit_kFirms(int exit_kFirms) {
		this.exit_kFirms = exit_kFirms;
	}
	public double getHerfindahlMeasure_kFirms() {
		return herfindahlMeasure_kFirms;
	}
	public void setHerfindahlMeasure_kFirms(double herfindahlMeasure_kFirms) {
		this.herfindahlMeasure_kFirms = herfindahlMeasure_kFirms;
	}
	public double getMeanProductivityWeightedByMarketShare_kFirms() {
		return meanProductivityWeightedByMarketShare_kFirms;
	}
	public void setMeanProductivityWeightedByMarketShare_kFirms(double meanProductivityWeightedByMarketShare_kFirms) {
		this.meanProductivityWeightedByMarketShare_kFirms = meanProductivityWeightedByMarketShare_kFirms;
	}
	public double getMeanMachineProductivityWeightedByMarketShare() {
		return meanMachineProductivityWeightedByMarketShare;
	}
	public void setMeanMachineProductivityWeightedByMarketShare(double meanMachineProductivityWeightedByMarketShare) {
		this.meanMachineProductivityWeightedByMarketShare = meanMachineProductivityWeightedByMarketShare;
	}
	public double getTopProductivity_kFirms() {
		return topProductivity_kFirms;
	}
	public void setTopProductivity_kFirms(double topProductivity_kFirms) {
		this.topProductivity_kFirms = topProductivity_kFirms;
	}
	public double getTopMachineProductivity() {
		return topMachineProductivity;
	}
	public void setTopMachineProductivity(double topMachineProductivity) {
		this.topMachineProductivity = topMachineProductivity;
	}
	public double getCreditDemand() {
		return creditDemand;
	}
	public void setCreditDemand(double creditDemand) {
		this.creditDemand = creditDemand;
	}
	public double getGovSpending() {
		return govSpending;
	}
	public void setGovSpending(double govSpending) {
		this.govSpending = govSpending;
	}
	public double getGovRevenues() {
		return govRevenues;
	}
	public void setGovRevenues(double govRevenues) {
		this.govRevenues = govRevenues;
	}
	public double getGovDebt() {
		return govDebt;
	}
	public void setGovDebt(double govDebt) {
		this.govDebt = govDebt;
	}
	public double getGdpGrowth() {
		return gdpGrowth;
	}
	public void setGdpGrowth(double gdpGrowth) {
		this.gdpGrowth = gdpGrowth;
	}
	public double getConsumptionGrowth() {
		return consumptionGrowth;
	}
	public void setConsumptionGrowth(double consumptionGrowth) {
		this.consumptionGrowth = consumptionGrowth;
	}
	public double getInvestmentGrowth() {
		return investmentGrowth;
	}
	public void setInvestmentGrowth(double investmentGrowth) {
		this.investmentGrowth = investmentGrowth;
	}
	public double getBadDebt() {
		return badDebt;
	}
	public void setBadDebt(double badDebt) {
		this.badDebt = badDebt;
	}
	public double getCreditSupply() {
		return creditSupply;
	}
	public void setCreditSupply(double creditSupply) {
		this.creditSupply = creditSupply;
	}
	public double getTotalDebt() {
		return totalDebt;
	}
	public void setTotalDebt(double totalDebt) {
		this.totalDebt = totalDebt;
	}
	public double getBankruptcyRate() {
		return bankruptcyRate;
	}
	public void setBankruptcyRate(double bankruptcyRate) {
		this.bankruptcyRate = bankruptcyRate;
	}
	public double getLiquidityToSalesRatio() {
		return liquidityToSalesRatio;
	}
	public void setLiquidityToSalesRatio(double liquidityToSalesRatio) {
		this.liquidityToSalesRatio = liquidityToSalesRatio;
	}
	
	
}
