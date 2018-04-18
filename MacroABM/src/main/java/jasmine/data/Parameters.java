package jasmine.data;

import microsim.event.Order;

public class Parameters {
	
		//Scheduling
		public static final int MODEL_ORDERING = 0;
		public static final int COLLECTOR_ORDERING = Order.AFTER_ALL.getOrdering();		//Observer calls updates of cross-sectional objects and functions, so should be fired BEFORE the collector class!
		public static final int OBSERVER_ORDERING = Order.AFTER_ALL.getOrdering()-1;		//Observer calls updates of cross-sectional objects and functions, so should be fired BEFORE the collector class!
		
		// If true, run the estimation procedure. Otherwise, use the parameters presented in Dosi et al. 2013 (appendix)
		public static final boolean ESTIMATION_OF_PARAMETERS = false;


		//Simulation parameter
		private static double errorThreshold; // margin of error, was epsilon.
		private static int forgoneObservation; 
		
		// Initial parametrization
		private static double initialProductivity; // initial productivity of the two firm sectors, was a0.
		private static double initialWage; // initial wage, was w0.
		private static double netLiquidAssets_kFirms; // net wealth of (capital goods) k-firms at t = 0, was liquidAssetCapital.
		private static double netLiquidAssets_cFirms; // net wealth of (consumption goods) c-firms at t = 0, was liquidAssetConsumption.
		private static double initialCapitalStock_cFirms; // initial stock of capital of c-firms, was k0.
		private static double laborSupply; // labour supply = number of households = number of consumers, was ls.
			
		// K-FIRMS (Capital Goods producing Firms)
		private static double fixedMarkUp_kFirms; // fixed mark-up, price equation, was mu1.
		private static double fractionPastSalesInvestedInRandD_kFirms; // fraction of past sales invested in R&D ("R&D investment propensity" in Dosi et al. 2013), was nu.
		private static double shareInnovationInRandD_kFirms; // share of innovation in R&D expenditure ("R&D allocation to innovative search" in Dosi et al. 2013), was xi.
		private static double zeta1_innovation_kFirms; // parameter in Bernouilli distribution parameter for innovation ("Firm search capabilities" in Dosi et al. 2013), was zeta1.
		private static double zeta2_imitation_kFirms; // parameter in Bernouilli distribution parameter for imitation ("Firm search capabilities" in Dosi et al. 2013), was zeta2.
		private static double shareNewClientsPerPeriod_kFirms; // new clients per period as a share of current clients ("New-customer sample parameter" in Dosi et al. 2013), was gamma.
		
		private static double a_kFirms; //TODO: ask Mauro what this a is, was 'a'.
		private static double minPrice_kFirms; // min. price. TODO: is this really useful? I.e. is there a risk that price --> 0?, was pmin.

		// Parameters for the Beta distribution for innovation of Capital Goods producing Firms
		private static double alpha1_innovation_kFirms; // alpha parameter in Beta distribution for innovation, was alpha1. 
		private static double beta1_innovation_kFirms; // beta parameter in Beta distribution for innovation, was beta1.
		private static double x1lower_innovation_kFirms; // lower support of Beta distribution for innovation, was x1lower.
		private static double x1upper_innovation_kFirms; // upper support of Beta distribution for innovation, was x1upper.
		
		// C-FIRMS (Consumption Goods producing Firms)
		private static double machineSizeInCapital_cFirms; // size of a machine in terms of capital, was dimK.
		private static double maxCapitalGrowthPerPeriod_cFirms; // "in any give period firm capital growth rates cannot exceed a fixed maximum threshold", was maxKGrowth.
		private static double marketShareThresholdForExit_cFirms; // market share below which a firm exits, was exit.
		
		private static double desiredInventoriesProportionOfExpectedDemand_cFirms; // desired level of inventories as a share of expected demand, was iota. 
		private static double desiredCapacityUtilization_cFirms; // desired level of capacity utilization, was cud.
		private static double chi_cFirms; // parameter in firms' market share equation ("Replicator dynamics coefficient" in Dosi et al. 2013), was chi.

		private static double maxAgeMachines_cFirms; // maximal age of machines after which they have to be replaced ("'Physical' scrapping age" in Dosi et al. 2013), was eta. 
		private static double machinePaybackPeriod_cFirms; // payback parameter ("Payback period" in Dosi et al. 2013), was b.

		private static double coeffMarkUpRule_cFirms; // coefficient in the mark up rule ("Coefficient in the consumption-good firm mark-up rule" in Dosi et al. 2013), was v.
				
		private static double coeffPriceCompetitiveness_cFirms; // price relative importance in competitiveness equation ("Competitiveness weights" in Dosi et al. 2013), was omega1.
		private static double coeffUnfilledDemandCompetitiveness_cFirms; // unfilled demand relative importance in competitiveness equation ("Competitiveness weights" in Dosi et al. 2013), was omega2.
		
		private static double debtRepaymentSharePerPeriod_cFirms; // fraction of the debt they repay in each period, was repaymentShare.
		
		// BANKING
		
		private static double coeffMarkDownOnDepositRate_Bank; // markdown on deposit rate, was psiD.
		private static double coeffMarkUpOnInterestRate_Bank; // markup on interest rate, was psiU.
		private static double coeffMarkDownOnDepositRateAtCentralBank_Bank; // mark down on bank's deposit at the CB, was cbMd.
		
		// WAGE DYNAMICS
		private static double coeffLaborProd_Wages; // labor productivity parameter, was psi1.
		private static double coeffCPI_Wages; // inflation/cpi parameter, was psi2.
		private static double coeffUnemployment_Wages; // unemployment parameter, was psi3.
		private static double maxVariation_Wages; // max. variation of wage, ~ Dosi et al., was maxVarWage. 
		private static double naturalLevelUnemployment; // natural level of unemployment, was uStar.  This is a lower bound for the unemployment rate (to prevent divide by zero errors), which is used in the calculation of wage inflation.  The unemployment rate is reset to this level if it falls below it. 
	
	public static void initializationWithoutEstimation(){
		// initialization of the parameter when all parameters are picked up manually (~ Dosi et al. calibration)
		
		// simulation parameter
		forgoneObservation = 1;
		errorThreshold = 0.001;
		
		// initial parameters
		
		initialProductivity = 1;
		initialWage = 1;
		netLiquidAssets_kFirms = 1_000;			//Note, "1_000" means "1000", i.e. since Java 7, underscores can be used to separate digits to improve readability.  In practice, the reader can ignore underscores.
		netLiquidAssets_cFirms = 1_000;
		initialCapitalStock_cFirms = 800;
		laborSupply = 250_000;
		
		// k-firms parameters
		
		fixedMarkUp_kFirms = 0.04;
		fractionPastSalesInvestedInRandD_kFirms = 0.04;
		shareInnovationInRandD_kFirms = 0.5;
		zeta1_innovation_kFirms = 0.3;
		zeta2_imitation_kFirms = 0.3;
		shareNewClientsPerPeriod_kFirms = 0.5;
		
		alpha1_innovation_kFirms = 3;
		beta1_innovation_kFirms = 3;
		x1lower_innovation_kFirms = - 0.15;
		x1upper_innovation_kFirms = 0.15;
		
		a_kFirms = 0.1;
		// a = 1;
		minPrice_kFirms = 0.01;
		
		// c-firms parameters
		
		machineSizeInCapital_cFirms = 40;
		maxCapitalGrowthPerPeriod_cFirms = 0.5;
		desiredInventoriesProportionOfExpectedDemand_cFirms = 0.1;
		maxAgeMachines_cFirms = 19; //XXX: 20 in the paper, but Hugo left this as 19...?
		machinePaybackPeriod_cFirms = 120; //XXX: 3 in the paper, but Hugo left this as 120...?
		coeffMarkUpRule_cFirms = 0.01;
		desiredCapacityUtilization_cFirms = 0.75;
		coeffPriceCompetitiveness_cFirms = 1;
		coeffUnfilledDemandCompetitiveness_cFirms = 1;
		chi_cFirms = - 1;		//XXX: This is 1 in the paper, but Hugo left this as -1...?
		debtRepaymentSharePerPeriod_cFirms = 0.33;
		marketShareThresholdForExit_cFirms = 0.00001;
		
		// banking parameters
		
		coeffMarkDownOnDepositRate_Bank = 1;
		coeffMarkUpOnInterestRate_Bank = 0.5;
		coeffMarkDownOnDepositRateAtCentralBank_Bank = 0.9;
		
		// labor market parameters
		
		coeffLaborProd_Wages = 1;
		coeffCPI_Wages = 0;
		coeffUnemployment_Wages = 0;
		maxVariation_Wages = 1;
		naturalLevelUnemployment = 0.01;
		
	}
	
	public static void initializationWithEstimation() {
		// initialization when estimating the parameter with MCMC (see the Estimation class). The parameters calibrated here
		// are the one that are not estimated by the Bayesian process -- that is that are constant across the entire process 
		// so far, only removed b (b is the only parameter estimated using MCMC).
				
		// simulation parameter
		forgoneObservation = 1;
		errorThreshold = 0.001;
		
		// initial parameters
		
		initialProductivity = 1;
		initialWage = 1;
		netLiquidAssets_kFirms = 1_000;
		netLiquidAssets_cFirms = 1_000;
		initialCapitalStock_cFirms = 800;
		laborSupply = 250_000;
		
		// k-firms parameters
		
		fixedMarkUp_kFirms = 0.04;
		fractionPastSalesInvestedInRandD_kFirms = 0.04;
		shareInnovationInRandD_kFirms = 0.5;
		zeta1_innovation_kFirms = 0.3;
		zeta2_imitation_kFirms = 0.3;
		shareNewClientsPerPeriod_kFirms = 0.5;
		
		alpha1_innovation_kFirms = 3;
		beta1_innovation_kFirms = 3;
		x1lower_innovation_kFirms = - 0.15;
		x1upper_innovation_kFirms = 0.15;
		
		a_kFirms = 0.1;
		// a = 1;
		minPrice_kFirms = 0.01;
		
		// c-firms parameters
		
		machineSizeInCapital_cFirms = 40;
		maxCapitalGrowthPerPeriod_cFirms = 0.5;
		desiredInventoriesProportionOfExpectedDemand_cFirms = 0.1;
		maxAgeMachines_cFirms = 19;
		coeffMarkUpRule_cFirms = 0.01;
		desiredCapacityUtilization_cFirms = 0.75;
		coeffPriceCompetitiveness_cFirms = 1;
		coeffUnfilledDemandCompetitiveness_cFirms = 1;
		chi_cFirms = - 1;
		debtRepaymentSharePerPeriod_cFirms = 0.33;
		marketShareThresholdForExit_cFirms = 0.00001;
		
		// banking parameters
		
		coeffMarkDownOnDepositRate_Bank = 1;
		coeffMarkUpOnInterestRate_Bank = 0.5;
		coeffMarkDownOnDepositRateAtCentralBank_Bank = 0.9;
		
		// labor market parameters
		
		coeffLaborProd_Wages = 1;
		coeffCPI_Wages = 0;
		coeffUnemployment_Wages = 0;
		maxVariation_Wages = 1;
		naturalLevelUnemployment = 0.01;
		
		
		//TODO: Create a method here to assert the properties of the parameters.  For example, "[omega1 and omega2] are positive parameters" (Dosi et al. 2013).  But do we ever check?
		
	}

	public static double getErrorThreshold() {
		return errorThreshold;
	}

	public static double getInitialProductivity() {
		return initialProductivity;
	}

	public static double getInitialWage() {
		return initialWage;
	}

	public static double getNetLiquidAssets_kFirms() {
		return netLiquidAssets_kFirms;
	}

	public static double getNetLiquidAssets_cFirms() {
		return netLiquidAssets_cFirms;
	}

	public static double getInitialCapitalStock_cFirms() {
		return initialCapitalStock_cFirms;
	}

	public static double getLaborSupply() {
		return laborSupply;
	}

	public static double getMarketShareThresholdForExit_cFirms() {
		return marketShareThresholdForExit_cFirms;
	}

	public static double getFixedMarkUp_kFirms() {
		return fixedMarkUp_kFirms;
	}

	public static double getFractionPastSalesInvestedInRandD_kFirms() {
		return fractionPastSalesInvestedInRandD_kFirms;
	}

	public static double getShareInnovationInRandD_kFirms() {
		return shareInnovationInRandD_kFirms;
	}

	public static double getZeta1_Innovation_kFirms() {
		return zeta1_innovation_kFirms;
	}

	public static double getZeta2_Imitation_kFirms() {
		return zeta2_imitation_kFirms;
	}

	public static double getShareNewClientsPerPeriod_kFirms() {
		return shareNewClientsPerPeriod_kFirms;
	}

	public static double getAlpha1_Innovation_kFirms() {
		return alpha1_innovation_kFirms;
	}

	public static double getBeta1_Innovation_kFirms() {
		return beta1_innovation_kFirms;
	}

	public static double getX1lower_Innovation_kFirms() {
		return x1lower_innovation_kFirms;
	}

	public static double getX1upper_Innovation_kFirms() {
		return x1upper_innovation_kFirms;
	}

	public static double getMachineSizeInCapital_cFirms() {
		return machineSizeInCapital_cFirms;
	}

	public static double getMaxCapitalGrowthPerPeriod_cFirms() {
		return maxCapitalGrowthPerPeriod_cFirms;
	}

	public static double getDesiredInventoriesProportionOfExpectedDemand_cFirms() {
		return desiredInventoriesProportionOfExpectedDemand_cFirms;
	}

	public static double getDesiredCapacityUtilization_cFirms() {
		return desiredCapacityUtilization_cFirms;
	}

	public static double getChi_cFirms() {
		return chi_cFirms;
	}

	public static double getMaxAgeMachines_cFirms() {
		return maxAgeMachines_cFirms;
	}

	public static double getMachinePaybackPeriod_cFirms() {
		return machinePaybackPeriod_cFirms;
	}

	public static double getCoeffMarkUpRule_cFirms() {
		return coeffMarkUpRule_cFirms;
	}

	public static double getCoeffPriceCompetitiveness_cFirms() {
		return coeffPriceCompetitiveness_cFirms;
	}

	public static double getCoeffUnfilledDemandCompetitiveness_cFirms() {
		return coeffUnfilledDemandCompetitiveness_cFirms;
	}

	public static double getDebtRepaymentSharePerPeriod_cFirms() {
		return debtRepaymentSharePerPeriod_cFirms;
	}

	public static double getCoeffMarkDownOnDepositRate_Bank() {
		return coeffMarkDownOnDepositRate_Bank;
	}

	public static double getCoeffMarkUpOnInterestRate_Bank() {
		return coeffMarkUpOnInterestRate_Bank;
	}

	public static double getCoeffMarkDownOnDepositRateAtCentralBank_Bank() {
		return coeffMarkDownOnDepositRateAtCentralBank_Bank;
	}

	public static double getCoeffLaborProd_Wages() {
		return coeffLaborProd_Wages;
	}

	public static double getCoeffCPI_Wages() {
		return coeffCPI_Wages;
	}

	public static double getCoeffUnemployment_Wages() {
		return coeffUnemployment_Wages;
	}

	public static double getMaxVariation_Wages() {
		return maxVariation_Wages;
	}

	public static double getNaturalLevelUnemployment() {
		return naturalLevelUnemployment;
	}

	public static double getA_kFirms() {
		return a_kFirms;
	}
		
	public static double getMinPrice_kFirms() {
		return minPrice_kFirms;
	}

	public static int getForgoneObservation() {
		return forgoneObservation;
	}

	public static void setMachinePaybackPeriod_cFirms(double machinePaybackPeriod_cFirms) {
		Parameters.machinePaybackPeriod_cFirms = machinePaybackPeriod_cFirms;
	}

}
