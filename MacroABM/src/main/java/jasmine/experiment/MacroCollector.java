package jasmine.experiment;

import java.util.Map;

import microsim.annotation.GUIparameter;
import microsim.data.DataExport;
import microsim.engine.AbstractSimulationCollectorManager;
import microsim.engine.SimulationEngine;
import microsim.engine.SimulationManager;
import microsim.event.EventGroup;
import microsim.event.EventListener;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.IIntSource;
import microsim.statistics.functions.MaxArrayFunction;
import microsim.statistics.functions.MeanArrayFunction;
//import microsim.statistics.functions.MeanVarianceArrayFunction;
import microsim.statistics.functions.SumArrayFunction;

import org.apache.log4j.Logger;

import jasmine.data.Parameters;
import jasmine.model.*;
import jasmine.object.Machine;

public class MacroCollector extends AbstractSimulationCollectorManager implements EventListener, IDoubleSource, IIntSource {

	private final static Logger log = Logger.getLogger(MacroCollector.class);

	@GUIparameter(description = "Number of periods during which data won't be recorded")
	Integer initialTimestepsToIgnore 						= 0;//100; was burnIn.
	@GUIparameter(description = "Toggle to export snapshot to .csv files")
	boolean exportToCSV 				= true;				//If true, data will be recorded to .csv files in the output directory
	@GUIparameter(description = "Toggle to export snapshot to output database")
	boolean exportToDatabase 			= false;		//If true, data will be recorded in the output database in the output directory
	@GUIparameter(description = "Toggle to export commercial bank data")
	boolean exportBankData 			= true;
	@GUIparameter(description = "Toggle to export individual data on firms")
	boolean exportFirmsData 			= true;
	@GUIparameter(description = "Set the time between snapshots to be exported to the database and/or .csv files")
	Double timestepsBetweenSnapshots 	= 1.;

	//DataExport objects to handle exporting data to database and/or .csv files
	// private DataExport exportAgentsCreated;
	private DataExport exportStatistics;
	private DataExport exportCFirms;
	private DataExport exportKFirms;
	private DataExport exportBank;
	
	// MacroStatistics will store the macro statistics in it, to finally export them in .csv files
	private MacroStatistics statistics;
	
//	public int flag;		//XXX: This field doesn't seem to be updated with any value other than 0 at the beginning, so I will comment out.
	public int bankDifficulty;
	
	// Here are put together all the macro variables 
	
	// Labor market
	public double[] wage;
	public double unemployment;
	public double rationingRatio_Labor;	//was rationingRatio.
	
	public double laborDemand; // total labor demand 
	public double laborDemandUsedForProduction; // labor demand used for production (i.e. all but R&D), was laborDemandProd.
	public double laborDemand_cFirms; // labor demand c-sector, was laborDemandCons.
	public double laborDemand_kFirms; // labor demand k-sector, was laborDemandCapital.
	public CrossSection.Double csLaborDemand_cFirms;	//was csLaborDemandCons.
	public SumArrayFunction fSumLaborDemand_cFirms;		//was fSumLaborDemandCons.
	public CrossSection.Double csLaborDemandForProduction_kFirms;	//was csLaborDemandCapital.
	public SumArrayFunction fSumLaborDemandForProduction_kFirms;		//was fSumLaborDemandCapital.
	
	public double laborDemandForRandD; // labor demand only for R&D purpose, was laborDemandRd.
	public CrossSection.Double csLaborDemandForRd;	//was csLaborDemandRd.
	public SumArrayFunction fSumLaborDemandForRd;	//was fSumLaborDemandRd.
	
	public double[] unemploymentRate; // unemployment rate, was uRate.
	public double diffUnemploymentRate; // diff. in unemployment rate, was diffU.
	public double diffCPI; // diff. in cpi, was diffCpi.
	public double diffProductivity; // diff. in productivity, was diffProd. 
	public double diffWage; // diff. in wage
	
	public double diffLogWage;
	public double diffLogCPI;	//was diffLogCpi.
	
	// Goods market
	public double initialAggregateDemand;
//	public double aggregateDemand;
	public double[] consumption;
	public double realConsumption;
	public double unfilledConsumption; // consumption (demand) not able to be fulfilled in t by CFirms that will come back in (t+1)
	// the two following variables are mainly used for the computation of the c-firms' competitiveness and market share 
	public double[] cpi; // consumer price index (price of cFirms)
	
	public double production_cFirms; // real production in the consumption sector, was productionCons.
	public double productionNominal_cFirms; // nominal production in the consumption sector, was productionNomCons.
	public CrossSection.Double csTotalProduction_cFirms; // total production, consumption sector, was csTotalProdCons.
	public SumArrayFunction fSumTotalProduction_cFirms; //was fSumTotalProdCons
	
	public double demand_cFirms;	//was demandCons
	public CrossSection.Double csDemand_cFirms;	//was csDemandCons
	public SumArrayFunction fSumDemand_cFirms;	//was fSumDemandCons
	
	public double meanPrice_cFirms;	//was meanPrice
	public CrossSection.Double csPastPrice_cFirms;		//was csPastPriceCons
	public CrossSection.Double csPresentPrice_cFirms;	//was csPresentPriceCons
	public MeanArrayFunction fMeanPastPrice_cFirms;	//fMeanPastPriceCons
	public MeanArrayFunction fMeanPresentPrice_cFirms;	//fMeanPresentPriceCons
	
	public double meanUnfilledDemand;
	public CrossSection.Double csUnfilledDemand;
	public MeanArrayFunction fMeanUnfilledDemand; 
	
	// Capital market
	public double production_kFirms;	//was productionCapital.
	public double productionNominal_kFirms; // nominal production in the capital goods sector, was productionNomCapital.
	public CrossSection.Double csTotalProduction_kFirms; // total production, capital goods sector, was csTotalProdCapital.
	public SumArrayFunction fSumTotalProduction_kFirms; //was fSumTotalProdCapital.
	public double[] ppi; // production price index (price of KFirms)
	
	// Macroeconomic variables 
	public double[] averageLaborProductivity; // mean productivity in consumption-sector, was averageLaborProd.
	public double output;
	public double[] gdp;
	public double gdpLog;
	public double gdpNominal; // gdp nominal, was gdpNom.
	public double gdpGrowth; 
	public double totalFactorProductivity; // total factor productivity, was tfp. 
	public double totalInventories; // total inventories, was nTot; TODO: can remove
	public double diffTotalInventories_cFirms; // difference in inventories, was diffN. 
	public double diffTotalInventoriesNominal_cFirms; // difference in inventories, nominal, was diffNNom.
	
	// C-firms
	public double[] meanCompetitiveness_cFirms; // mean competitiveness in consummption-sector, was meanComp. 
	public int exit_cFirms; // total exit in the C-sector, was exit2.
	public int exitLiquidityIssue_cFirms; // exit in the C-sector due to liquidity issue, was exit2AssetOnly. 
	public int exitMarketShareIssue_cFirms; // exit in the C-sector due to lack of market share, was exit2MarketOnly.
	public int exitAssetMarket_cFirms;	//was exit2AssetMarket.		//XXX: Ross - not sure what this represents - check!
	public double[] totalMarketShare_cFirms; // total market share; used to normalize market share, was fTot. 
	public double herfindahlMeasure_cFirms; // Herfindahl measure for cons sector, was hCons.
	public int cFirmsRemaining; 
	public double totalLiquidAssets_cFirms;	//was totLiquidAssetCons.
	public CrossSection.Double csLiquidAssets_cFirms;	//was csLiquidAssetCons.
	public SumArrayFunction fSumLiquidAssets_cFirms;	//wasfSumLiquidAssetCons.
	public double meanLiquidityToSalesRatio_cFirms;
	public double totalBadDebt;
	
	public double meanCostWeightedByMarketShare_cFirms;	//Cost weighted by market share, was MeanCostCons.
	
	public double meanProductivityWeightedByMarketShare_cFirms; // mean productivity when taking account the importance of the firm, defined by its market share, was meanMarketShareProdC. 
	public double meanLogProductivity_cFirms;		//was meanProdLogC.
	
	public double investmentExpansionaryTotal_cFirms;		//was invExpTotal.
	public double investmentSubstitutionaryTotal_cFirms;		//was invSubTotal.
	public double[] investmentTotal_cFirms;		//was invTotal.
	public CrossSection.Double csInvestmentExpansionary;	//was csInvExp.
	public CrossSection.Double csInvestmentSubstitutionary;	//was csInvSub.
	public SumArrayFunction fSumInvestmentExpansionary;		//wasvfSumInvExp.
	public SumArrayFunction fSumInvestmentSubstitutionary;		//was fSumInvSub.
	
	public CrossSection.Double csBadDebt_cFirms;		//was csBadDebt
	public SumArrayFunction fSumBadDebt_cFirms;			//was fSumBadDebt
	public double bankruptcyRate_cFirms;				//was bankruptcyRate
	public CrossSection.Double csLiquidityToSalesRatio_cFirms;	//was csLiquiditySale
	public MeanArrayFunction fMeanLiquidityToSalesRatio_cFirms;	//wasfMeanLiquiditySale

	public MeanArrayFunction fMeanLiquidAssets_cFirms;
	public SumArrayFunction fSumLiquidityToSalesRatio_cFirms;
	
	public double capitalStock_cFirms; //was capitalStock. // TODO: remove
	public double capitalStockDesired_cFirms; //was capitalStockDesired. // TODO: remove
	public double capitalStockTopLimit_cFirms; //was capitalStockTop.	// TODO: remove
	public double avgAgeMachines_cFirms; //was avgAgeMachine. // TODO: can remove
	public double markUpTot_cFirms; //was markUpTot. // TODO: can remove	//XXX: Is this a total or an average?  It is used in the charts and labelled as an average!
	public double creditRationingRate_cFirms; //was creditRationingRate. // TODO: to remove
	public double sumDesiredProduction_cFirms; //was sumDesiredProd. // TODO: to remove
	public double desiredExpansionaryInvestmentTotal_cFirms;		//was dInvExpTotal.
	public double desiredExpansionaryInvestmentTotalStar_cFirms;	//was dInvExpTotalStar.
	
	public double profit_cFirms;	//was profitCons
	public CrossSection.Double csProfit_cFirms;	//was csProfitCons
	public SumArrayFunction fSumProfit_cFirms;		//was fSumProfitCons
	
	public double grossOperatingSurplus_cFirms;
	public CrossSection.Double csGrossOperatingSurplus_cFirms;	//was csProfitCons
	public SumArrayFunction fSumGrossOperatingSurplus_cFirms;		//was fSumProfitCons
	
	// K-firms
	public int exit_kFirms; // exit in the K-sector, was exit1
	public double herfindahlMeasure_kFirms; // Herfindahl measure for capital sector, was hCapital
	public int kFirmsRemaining;
	public double rdExpenditures_kFirms;	//was rdExpenditures
	public CrossSection.Double csRdExpenditures_kFirms;	//was csRdExpenditures
	public SumArrayFunction fSumRdExpenditures_kFirms;		//was fSumRdExpenditures
	
	public double topProductivity_kFirms; // top prod. of the k-firms, was topProdK
	public double meanProductivityWeightedByMarketShare_kFirms; // mean productivity of k-firms taking into account the k-firms' market share, was meanMarketShareProdK
	public double meanLogProductivity_kFirms; // mean productivity of k-firms, logged, was meanProdLogK
	public CrossSection.Double csProductivity_kFirms;	//was csProdK
	public MaxArrayFunction fMaxProductivity_kFirms;	//was fMaxProdK
	
	public double profit_kFirms;	//was profitCapital
	public CrossSection.Double csProfit_kFirms;		//was csProfitCapital
	public SumArrayFunction fSumProfit_kFirms;		//was fSumProfitCapital
	public double totalLiquidAssets_kFirms;			//was totLiquidAssetCapital
	public CrossSection.Double csLiquidAsset_kFirms;	//was csLiquidAssetCapital
	public SumArrayFunction fSumLiquidAsset_kFirms;		//was fSumLiquidAssetCapital
	
	public int maxClientsPerFirm_kFirms; // TODO: can remove, was maxClient
	
	// Technological progress
	public double topMachineProductivity; // top prod. of machines used in the c-sector. was topProdMachine
	public double meanMachineProductivityWeightedByMarketShare; // mean productivity of k-firms' machines taking into account the k-firms' market share, was meanMarketShareProdMachine
	public CrossSection.Double csMachineProductivity;	//was csProdMachine
	public MaxArrayFunction fMaxMachineProductivity;	//was fMaxProdMachine
	
	// Financial variables
	public double aggregateCreditDemand;
	public double averageCreditDemandToCreditSupplyRatio;		//was averageCreditDemandCreditSupplyRatio
	
	// Government
	public double govSpending; // government spending
	public double govRevenues; // government revenues 
	public double govBalance;	//govRevenues - govSpending
	public double govStock;		//sum of govBalance over time.
	
	public double govBalanceToGdp; // ratio of the gov's deficit to gdp, was govDefGdp
	public double govStockToGdp; //ratio of the gov's debt to gdp, was govDebtGdp
	public double govSpendingToGdp; // ratio of the gov's spending to gdp, was govSpendingGdp
	public double govRevenuesToGdp;
	
	MacroModel model;

	//ROSS: New variables to double check accounting identities	
	private double aggConsumption;

	private double Ycin;

	private double Yproduction;

	private double consumptionToYcin;

	private double investmentToYcin;

	private double changeInInventoriesValueToYcin;

	private double productionNominalKFirmsToYproduction;

	private double productionNominalCFirmsToYproduction;

	private double grossInvestment;

	private double gdpEx;

	private double consumptionToGDPex;

	private double investmentToGDPex;

	private double gdpInc;

	private double wagesToGDPinc;

	private double profitsToGDPinc;

	private double interestToGDPinc;
	
	private double depreciationToGDPinc;

	private double gdpIncWithoutDep;

	private double wagesToGDPincWithoutDep;

	private double interestToGDPincWithoutDep;

	private double profitsToGDPincWithoutDep;

	private double earnings;

//	private MeanVarianceArrayFunction fMeanVarianceLiquidityToSalesRatio_cFirms;
	
	
	// ---------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------
	
	public MacroCollector(SimulationManager manager) {

		super(manager);
		this.model = (MacroModel) getManager();
				
	}

	// ---------------------------------------------------------------------
	// Manager methods
	// ---------------------------------------------------------------------

	@Override
	public void buildObjects() {
		
		// Initialize the statistics object
		statistics 						= new MacroStatistics();
		
		// Create the export objects 
		
		exportStatistics 				= new DataExport(statistics, exportToDatabase, exportToCSV);
		if(exportFirmsData){
			exportCFirms				= new DataExport(model.getCFirms(), exportToDatabase, exportToCSV);
			exportKFirms 				= new DataExport(model.getKFirms(), exportToDatabase, exportToCSV);			
		}
		if(exportBankData) {
			exportBank 				= new DataExport(model.getBank(), exportToDatabase, exportToCSV);
		}
		
		// Create all the cross sectional objects
		
		csProductivity_kFirms 						= new CrossSection.Double(model.getKFirms(), KFirm.class, "getFirmProductivityNow", true);
		csMachineProductivity 					= new CrossSection.Double(model.getKFirms(), KFirm.class, "getMachineProductivity", true);
		csLaborDemand_cFirms				= new CrossSection.Double(model.getCFirms(), Firm.class, "getLaborDemand", true);
		csLaborDemandForProduction_kFirms			= new CrossSection.Double(model.getKFirms(), KFirm.class, "getLaborDemandForProduction", true);
		csLaborDemandForRd 				= new CrossSection.Double(model.getKFirms(), KFirm.class, "getLaborDemandForRd", true);
		csPastPrice_cFirms 				= new CrossSection.Double(model.getCFirms(), Firm.class, "getPriceOfGoodProducedPrevious", true);
		csPresentPrice_cFirms 				= new CrossSection.Double(model.getCFirms(), Firm.class, "getPriceOfGoodProducedNow", true);
		csUnfilledDemand 				= new CrossSection.Double(model.getCFirms(), CFirm.class, "getUnfilledDemand", true);
		csTotalProduction_cFirms 				= new CrossSection.Double(model.getCFirms(), Firm.class, "getProductionQuantity", true);
		csTotalProduction_kFirms			 	= new CrossSection.Double(model.getKFirms(), Firm.class, "getProductionQuantity", true);
		
		csInvestmentExpansionary 						= new CrossSection.Double(model.getCFirms(), CFirm.class, "getInvestmentExpansionary", true);
		csInvestmentSubstitutionary 						= new CrossSection.Double(model.getCFirms(), CFirm.class, "getInvestmentSubstitutionary", true);
		csDemand_cFirms 					= new CrossSection.Double(model.getCFirms(), Firm.class, "getDemandNow", true);
		csProfit_cFirms 					= new CrossSection.Double(model.getCFirms(), Firm.class, "getProfit", true);
		csGrossOperatingSurplus_cFirms 					= new CrossSection.Double(model.getCFirms(), CFirm.class, "getGrossOperatingSurplus", true);
		csProfit_kFirms 				= new CrossSection.Double(model.getKFirms(), Firm.class, "getProfit", true);
		csLiquidAsset_kFirms 			= new CrossSection.Double(model.getKFirms(), Firm.class, "getLiquidAssetNow", true);
		csLiquidAssets_cFirms 				= new CrossSection.Double(model.getCFirms(), Firm.class, "getLiquidAssetNow", true);
				
		csRdExpenditures_kFirms 				= new CrossSection.Double(model.getKFirms(), KFirm.class, "getRdExpenditure", true);
		csBadDebt_cFirms						= new CrossSection.Double(model.getCFirms(), CFirm.class, "getBadDebt", true);
		csLiquidityToSalesRatio_cFirms					= new CrossSection.Double(model.getCFirms(), CFirm.class, "getNetWorthToSalesRatio", true);
//		csLiquidityToSalesRatio_cFirms					= new CrossSection.Double(model.getCFirms(), CFirm.DoubleVariables.NetWorthToSales);
		 
		// Create all the objects computing functions on the previous cross-sections (max, mean, sum etc.)
		
		fMaxProductivity_kFirms 						= new MaxArrayFunction.Double(csProductivity_kFirms);
		fMaxMachineProductivity 				= new MaxArrayFunction.Double(csMachineProductivity);
		
		fMeanPastPrice_cFirms 				= new MeanArrayFunction(csPastPrice_cFirms);
		fMeanPresentPrice_cFirms 			= new MeanArrayFunction(csPresentPrice_cFirms);
		fMeanUnfilledDemand 			= new MeanArrayFunction(csUnfilledDemand);
		
		
		fMeanLiquidAssets_cFirms	= new MeanArrayFunction(csLiquidAssets_cFirms);
		fSumLiquidityToSalesRatio_cFirms 	= new SumArrayFunction.Double(csLiquidityToSalesRatio_cFirms);
		fMeanLiquidityToSalesRatio_cFirms				= new MeanArrayFunction(csLiquidityToSalesRatio_cFirms);
//		fMeanVarianceLiquidityToSalesRatio_cFirms		= new MeanVarianceArrayFunction(csLiquidityToSalesRatio_cFirms);
		
		fSumLaborDemand_cFirms 				= new SumArrayFunction.Double(csLaborDemand_cFirms);
		fSumLaborDemandForProduction_kFirms = new SumArrayFunction.Double(csLaborDemandForProduction_kFirms);
		fSumLaborDemandForRd 				= new SumArrayFunction.Double(csLaborDemandForRd);
		fSumTotalProduction_cFirms 			= new SumArrayFunction.Double(csTotalProduction_cFirms);
		fSumTotalProduction_kFirms 			= new SumArrayFunction.Double(csTotalProduction_kFirms);
		fSumInvestmentExpansionary 			= new SumArrayFunction.Double(csInvestmentExpansionary);
		fSumInvestmentSubstitutionary 		= new SumArrayFunction.Double(csInvestmentSubstitutionary);
		fSumDemand_cFirms 					= new SumArrayFunction.Double(csDemand_cFirms);
		fSumProfit_cFirms 					= new SumArrayFunction.Double(csProfit_cFirms);
		fSumGrossOperatingSurplus_cFirms	= new SumArrayFunction.Double(csGrossOperatingSurplus_cFirms);
		fSumProfit_kFirms 					= new SumArrayFunction.Double(csProfit_kFirms);
		fSumLiquidAsset_kFirms 				= new SumArrayFunction.Double(csLiquidAsset_kFirms);
		fSumLiquidAssets_cFirms 			= new SumArrayFunction.Double(csLiquidAssets_cFirms);
		fSumRdExpenditures_kFirms 			= new SumArrayFunction.Double(csRdExpenditures_kFirms);
		fSumBadDebt_cFirms					= new SumArrayFunction.Double(csBadDebt_cFirms);
		
		fSumLaborDemand_cFirms.setCheckingTime(false);
		fSumLaborDemandForProduction_kFirms.setCheckingTime(false);
		fSumLaborDemandForRd.setCheckingTime(false);
		
	}

	@Override
	public void buildSchedule() {
	
		EventGroup collectorEventGroup = new EventGroup();

		collectorEventGroup.addEvent(this, Processes.DumpStatistics);
		if(exportBankData) {
			collectorEventGroup.addEvent(this, Processes.DumpBank);
		}
		if(exportFirmsData){
			// export data on firms only if wiling to obtain individual information on them 
			collectorEventGroup.addEvent(this, Processes.DumpCFirms);
			collectorEventGroup.addEvent(this, Processes.DumpKFirms);
		}		
		
		// Note: store information only after the burn-in phase 
		getEngine().getEventQueue().scheduleRepeat(collectorEventGroup, initialTimestepsToIgnore, Parameters.COLLECTOR_ORDERING, timestepsBetweenSnapshots);

	}

	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
		Update,
		TechFrontier,
		AggregateComputation,
		DumpBank,
		DumpInStatistics,
		DumpStatistics,
		DumpCFirms,
		DumpKFirms;
	}

	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case Update:
			update();
			break;
		
		case TechFrontier:
			techFrontier();
			break;
			
		case AggregateComputation:
			aggregateComputation();
			break;

		case DumpBank:
			try{
				exportBank.export();
			} catch(Exception e){
				log.error(e.getMessage());
			}
			break;

		case DumpInStatistics:
			dumpInStatistics();
			break;
			
		// events to export the data to either the database or the .csv files
		case DumpStatistics:
			try{
				exportStatistics.export();
			} catch(Exception e){
				log.error(e.getMessage());
			}
			break;
		
		case DumpCFirms:
			try{
				exportCFirms.export();
			} catch(Exception e){
				log.error(e.getMessage());
			}
			break;
		
		case DumpKFirms:
			try{
				exportKFirms.export();
			} catch(Exception e){
				log.error(e.getMessage());
			}
			break;

		}
		
	}
	
	// ---------------------------------------------------------------------
	// ISource methods
	// ---------------------------------------------------------------------
	
	// These methods mainly allow to transform the aggregate variables computed here in time series, and then to plot them in the observer
	public enum Variables {
		InterestRate,
		UnemploymentRatePercent,
		LogGDP,
		LogTotalInvestment,
//		ConsumptionLog,
		LogUnfilledConsumption,
		LogRealConsumption,
		ConsumptionToGDPpercent,
		InvestmentToGDPpercent,
		GdpGrowth,
		TotalExpansionaryInvestment_cFirms,
		TotalSubstitutionaryInvestment_cFirms,
		LogTotalExpansionaryInvestment_cFirms,
		LogTotalSubsitionaryInvestment_cFirms,
		ConsumerInflationPercent_cFirms,
		ProducerInflationPercent_kFirms,
		meanLogProductivity_kFirms,
		meanLogProductivity_cFirms,
		LowInv,
		HighInv,
		Exit_kFirms,
		Exit_cFirms,
		ExitLiquidityIssue_cFirms,
		ExitMarketShareIssue_cFirms,
		ExitAssetMarket_cFirms,
		GovBalanceToGDPpercent,
		GovStockToGDPpercent,
		GovSpendingToGDPpercent,
		GovRevenuesToGDPpercent,
		LogGovSpending,
		LogCreditDemand, 
		LogMeanProductivityWeightedByMarketShare_kFirms,
		LogMeanMachineProductivityWeightedByMarketShare,
		// TODO: can remove afterwards, only to understand better the model
		LogProduction_cFirms,
		TotalMarkUp_cFirms,
		ProfitToGDPpercent_cFirms,
		LogProfit_cFirms,
		LogProfit_kFirms,
		Profit_kFirms,
		ProfitToGDPpercent_kFirms,
		FirmAndBankProfitsToGDPpercent,
		ConsumptionPlusFirmAndBankProfitsToGDPpercent,
		ChangeInInventoriesPlusConsumptionPlusFirmAndBankProfitsToGDPpercent,
		WagesPlusUnemploymentBenefitsToGDPpercent,
		MaxClientsPerFirm_kFirms,
		TotalInventories,
		LogDiffInventories_cFirms,
		LogDiffInventoriesNominal_cFirms,
		DiffTotalInventoriesToGDPpercent,
		AverageAgeMachine_cFirms,
		LogRandDexpenditures_kFirms,
		LogConsumption,
		LogConsumptionReal,
		TotalLiquidAssets_cFirms,
		LogTotalLiquidAssets_kFirms,
		CostCapital,
		SalesCapital,
		CostSalesCapital,
		CPI,
		MeanCostWeightedByMarketShare_cFirms,
		CreditRationingRate_cFirms,
		LogTotalDesiredProduction_cFirms,
		LogCapitalStock_cFirms,
		LogDesiredCapitalStock_cFirms,
		LogCapitalStockTopLimit,
		LogDesiredExpansionaryInvestmentTotal_cFirms,
		LogDesiredExpansionaryInvestmentTotalStar_cFirms,
		LogProduction_kFirms,
		LogProductionNominal_kFirms,
		LogProductionNominal_cFirms,
		
		ActualExpansionaryInvestmentToGDPpercent_cFirms,
		DesiredExpansionaryInvestmentToGDPpercent_cFirms,
		DesiredExpansionaryInvestmentStarToGDPpercent_cFirms,
		ActualSubsitionaryInvestmentToGDPpercent_cFirms,

		LaborDemand,
		LogTopProductivity_kFirms,
		LogTopMachineProductivity,
		LaborRationingRatio,
		LogBadDebt,
		LogWage,
		Wage,
//		Flag,
		Empty,
		ExitPercent_kFirms,
		ExitPercent_cFirms,
		ExitPercentLiquidityIssue_cFirms,
		ExitPercentMarketShareIssue_cFirms,
		ExitPercentAssetMarket_cFirms,
		
		//ROSS: New variables to double check accounting identities investigating equation 11 of Dosi et al. (2013)
		LogYcin,
		LogYproduction,
		YcinToYproduction,
		ConsumptionToYcinPercent,
		InvestmentToYcinPercent,
		ChangeInInventoriesValueToYcinPercent,
		ProductionNominalKFirmsToYproductionPercent,
		ProductionNominalCFirmsToYproductionPercent,

		LogGdpEx,
		ConsumptionToGDPexPercent,
		InvestmentToGDPexPercent,
		LogGdpInc,
		WagesToGDPincPercent,
		ProfitsToGDPincPercent,
		InterestToGDPincPercent,
		DepreciationToGDPincPercent,
		LogGdpIncWithoutDep,
		WagesToGDPincWithoutDepPercent,
		InterestToGDPincWithoutDepPercent,
		ProfitsToGDPincWithoutDepPercent,
		GdpExToGdpInc,
		GdpExToGdpIncWithoutDep,

		
		
		LogAggregateDemand, 
		LogEarnings,

	}
 	
	@Override
	public double getDoubleValue(Enum<?> variableDouble) {
		switch((Variables) variableDouble){
		
		// TODO: can remove up to end
		case LogProduction_cFirms:
			if(production_cFirms > 0)
				return Math.log(production_cFirms);
			else 
				return Double.NaN;
		case LogRandDexpenditures_kFirms:
			if(rdExpenditures_kFirms > 0)
				return Math.log(rdExpenditures_kFirms);
			else 
				return Double.NaN;
		case AverageAgeMachine_cFirms:
			return this.avgAgeMachines_cFirms;
		case TotalInventories:
			return this.totalInventories;
		case DiffTotalInventoriesToGDPpercent:
			return 100. * diffTotalInventoriesNominal_cFirms / gdpNominal;
		case LogDiffInventoriesNominal_cFirms:
			if(diffTotalInventoriesNominal_cFirms > 0.)
				return Math.log(diffTotalInventoriesNominal_cFirms);
			else return Double.NaN;
		case LogDiffInventories_cFirms:
			if(diffTotalInventories_cFirms > 0.)
				return Math.log(diffTotalInventories_cFirms);
			else return Double.NaN;			
			
		case TotalMarkUp_cFirms:
			return this.markUpTot_cFirms;
		case LogProfit_cFirms:
			if(profit_cFirms > 0)
				return Math.log(profit_cFirms);
			else return Double.NaN;
		case LogProfit_kFirms:
			if(profit_kFirms > 0.)
				return Math.log(profit_kFirms);
			else return Double.NaN;
			
		case Profit_kFirms:
			return this.profit_kFirms;

		case ProfitToGDPpercent_kFirms:
			if(gdp[1] > 0.)
				return 100. * profit_kFirms / gdpNominal;
			else return Double.NaN;
			
		case ProfitToGDPpercent_cFirms:
			if(gdp[1] > 0.)
				return 100. * profit_cFirms / gdpNominal;
			else return Double.NaN;
			
		case FirmAndBankProfitsToGDPpercent:
			if(gdp[1] > 0.)
				return 100. * (profit_cFirms + profit_kFirms + model.getBank().profit) / gdpNominal;
			else return Double.NaN;
			
		case ConsumptionPlusFirmAndBankProfitsToGDPpercent:
			if(gdp[1] > 0.)
				return 100. * (aggConsumption + profit_cFirms + profit_kFirms + model.getBank().profit) / gdpNominal;
			else return Double.NaN;
			
		case ChangeInInventoriesPlusConsumptionPlusFirmAndBankProfitsToGDPpercent:
			if(gdp[1] > 0.)
				return 100. * (diffTotalInventoriesNominal_cFirms + aggConsumption + profit_cFirms + profit_kFirms + model.getBank().profit) / gdpNominal;
			else return Double.NaN;
			
		case WagesPlusUnemploymentBenefitsToGDPpercent:
			if(gdp[1] > 0.)
				return 100. * wage[1] * (1 + unemployment * model.getUnemploymentBenefitShare()) / gdpNominal;
			else return Double.NaN;
			
		case LogConsumption:
			if(aggConsumption > 0.)
//				return Math.log(this.consumption[1]);
				return Math.log(this.aggConsumption);
			else 
				return Double.NaN;
		case LogConsumptionReal:
			if(realConsumption > 0.)
				return Math.log(this.realConsumption);
			else 
				return Double.NaN;
		case TotalLiquidAssets_cFirms:
				return this.totalLiquidAssets_cFirms;
				
		case LogTotalLiquidAssets_kFirms:
			if(totalLiquidAssets_kFirms > 0.)
				return Math.log(this.totalLiquidAssets_kFirms);
			else 
				return Double.NaN;
			
		case MeanCostWeightedByMarketShare_cFirms:
			return this.meanCostWeightedByMarketShare_cFirms;
		case CPI:
			return this.cpi[1];
		case CreditRationingRate_cFirms:
			return this.creditRationingRate_cFirms;
		case LogTotalDesiredProduction_cFirms:
			if(sumDesiredProduction_cFirms > 0)
				return Math.log(this.sumDesiredProduction_cFirms);
			else 
				return Double.NaN;
		
		case LogCapitalStock_cFirms:
			if(capitalStock_cFirms > 0)
				return Math.log(this.capitalStock_cFirms);
			else 
				return Double.NaN;
		case LogDesiredCapitalStock_cFirms:
			if(capitalStockDesired_cFirms > 0)
				return Math.log(this.capitalStockDesired_cFirms);
			else 
				return Double.NaN;
		case LogCapitalStockTopLimit:
			if(capitalStockTopLimit_cFirms > 0)
				return Math.log(this.capitalStockTopLimit_cFirms);
			else 
				return Double.NaN;
		case LogDesiredExpansionaryInvestmentTotal_cFirms:
			if(desiredExpansionaryInvestmentTotal_cFirms > 0)
				return Math.log(this.desiredExpansionaryInvestmentTotal_cFirms);
			else 
				return Double.NaN;
		case LogDesiredExpansionaryInvestmentTotalStar_cFirms:
			if(desiredExpansionaryInvestmentTotalStar_cFirms > 0)
				return Math.log(this.desiredExpansionaryInvestmentTotalStar_cFirms);
			else 
				return Double.NaN;
			
		case LogProduction_kFirms:
			if(production_kFirms > 0.)
				return Math.log(production_kFirms);
			else return Double.NaN;
			
		case LogProductionNominal_kFirms:
			if(productionNominal_kFirms > 0.)
				return Math.log(productionNominal_kFirms);
			else return Double.NaN;
		case LogProductionNominal_cFirms:
			if(productionNominal_cFirms > 0.)
				return Math.log(productionNominal_cFirms);
			else return Double.NaN;
			
		case ActualExpansionaryInvestmentToGDPpercent_cFirms:
			if(gdp[1] > 0)
				return 100. * investmentExpansionaryTotal_cFirms / gdpNominal;
			else return Double.NaN;
			
		case DesiredExpansionaryInvestmentToGDPpercent_cFirms:
			if(gdp[1] > 0)
				return 100. * desiredExpansionaryInvestmentTotal_cFirms / gdpNominal;
			else return Double.NaN;
			
		case DesiredExpansionaryInvestmentStarToGDPpercent_cFirms:
			if(gdp[1] > 0)
				return 100. * desiredExpansionaryInvestmentTotalStar_cFirms / gdpNominal;
			else return Double.NaN;
			
		case ActualSubsitionaryInvestmentToGDPpercent_cFirms:
			if(gdp[1] > 0)
				return 100. * investmentSubstitutionaryTotal_cFirms / gdpNominal;
			else return Double.NaN;
			
	
		case LaborDemand:
			return this.laborDemand;
		case LogTopMachineProductivity:			//Was called 'TopProdLabor', which is confusing as it appears to refer to the top machine productivity..
			if(topMachineProductivity > 0)
				return Math.log(this.topMachineProductivity);
			else return Double.NaN;
		case LogTopProductivity_kFirms:		//Was called 'TopProdMachine', which is confusing as it appears to refer to the productivity of the kFirms...
			if(topProductivity_kFirms > 0)
				return Math.log(topProductivity_kFirms);
			else return Double.NaN;
		case LogMeanProductivityWeightedByMarketShare_kFirms:
			if(meanProductivityWeightedByMarketShare_kFirms > 0)
				return Math.log(meanProductivityWeightedByMarketShare_kFirms);
			else return Double.NaN;
		case LogMeanMachineProductivityWeightedByMarketShare:
			if(meanMachineProductivityWeightedByMarketShare > 0)
				return Math.log(meanMachineProductivityWeightedByMarketShare);
			else return Double.NaN;

			
		// end
		
		case InterestRate:
			return model.getInterestRate();
			
		case UnemploymentRatePercent:
			return 100. * unemploymentRate[1];
			
		case LogGDP:
			return gdpLog;
			
		case ConsumptionToGDPpercent:
			return 100. * aggConsumption / gdpNominal;
//			return 100. * consumption[1] / gdpNominal;
//			return 100. * realConsumption / gdp[1];
			
		case LogRealConsumption:
			if(realConsumption > 0.)
				return Math.log(realConsumption);
			else return Double.NaN;
			
		case LogUnfilledConsumption:
			if(unfilledConsumption > 0.)
				return Math.log(unfilledConsumption);
			else return Double.NaN;
			
		case InvestmentToGDPpercent:
			return 100. * (investmentExpansionaryTotal_cFirms + investmentSubstitutionaryTotal_cFirms) / gdpNominal;
			
		case LogTotalInvestment:
			if(investmentExpansionaryTotal_cFirms + investmentSubstitutionaryTotal_cFirms > 0) {
				return Math.log(investmentExpansionaryTotal_cFirms + investmentSubstitutionaryTotal_cFirms);
			}
			else {
				return Double.NaN;
			}
//		case ConsumptionLog:
//			return Math.log(consumption[1]);
			
		
		case TotalExpansionaryInvestment_cFirms:
			return investmentExpansionaryTotal_cFirms;
		case TotalSubstitutionaryInvestment_cFirms:
			return investmentSubstitutionaryTotal_cFirms;
			
		case LogTotalExpansionaryInvestment_cFirms:
			if(investmentExpansionaryTotal_cFirms > 0)
				return Math.log(investmentExpansionaryTotal_cFirms);
			else 
				return Double.NaN;
		case LogTotalSubsitionaryInvestment_cFirms:
			if(investmentSubstitutionaryTotal_cFirms > 0)
				return Math.log(investmentSubstitutionaryTotal_cFirms);
			else 
				return Double.NaN;
			
		case meanLogProductivity_kFirms:
			return meanLogProductivity_kFirms;
		case meanLogProductivity_cFirms:
			return meanLogProductivity_cFirms;
		
		case GdpGrowth:
			return gdpGrowth;
			
		case ConsumerInflationPercent_cFirms:
			return 100. * (cpi[1] - cpi[0]) / cpi[0];
		case ProducerInflationPercent_kFirms:
			return 100. * (ppi[1] - ppi[0]) / ppi[0];
			
		case GovBalanceToGDPpercent:
			return 100. * govBalanceToGdp;
		case GovStockToGDPpercent:
			return 100. * govStockToGdp;
		case GovSpendingToGDPpercent:
			return 100. * govSpendingToGdp;
		case GovRevenuesToGDPpercent:
			return 100. * govRevenuesToGdp;
		case LogGovSpending:
			if(govSpending > 0.)
				return Math.log(govSpending);
			else return Double.NaN;
			
		case LogCreditDemand:
			if(aggregateCreditDemand > 1)
				return Math.log(aggregateCreditDemand);
			else
				return 0.;
			
		case LogWage:
			return Math.log(wage[1]);
		
		case Wage:
			return wage[1];
			
		case LogBadDebt:
//			return totalBadDebt;
			if(totalBadDebt > 0)
				return Math.log(totalBadDebt);
			else return Double.NaN;
			
		case LaborRationingRatio:
			return this.rationingRatio_Labor;
			
		case Empty:
			return Double.NaN;

			
		case ExitPercent_kFirms:
			return 100. * exit_kFirms / (double)model.getKFirms().size();
		case ExitPercent_cFirms:
			return 100. * exit_cFirms / (double)model.getCFirms().size();
		case ExitPercentLiquidityIssue_cFirms:
			return 100. * exitLiquidityIssue_cFirms / (double)model.getCFirms().size();
		case ExitPercentMarketShareIssue_cFirms:
			return 100. * exitMarketShareIssue_cFirms / (double)model.getCFirms().size();
		case ExitPercentAssetMarket_cFirms:
			return 100. * exitAssetMarket_cFirms / (double)model.getCFirms().size();

			
		case LogYcin:
			if(Ycin > 0.)
				return Math.log(Ycin);
			else return Double.NaN;
		case LogYproduction:
			if(Yproduction > 0.)
				return Math.log(Yproduction);
			else return Double.NaN;
		case YcinToYproduction:
			if(Yproduction > 0.)
				return Ycin / Yproduction;
			else return Double.NaN;
		case ConsumptionToYcinPercent:
			return 100. * consumptionToYcin;
		case InvestmentToYcinPercent:
			return 100. * investmentToYcin;
		case ChangeInInventoriesValueToYcinPercent:
			return 100. * changeInInventoriesValueToYcin;
		case ProductionNominalKFirmsToYproductionPercent:
			return 100. * productionNominalKFirmsToYproduction; 
		case ProductionNominalCFirmsToYproductionPercent:
			return 100. * productionNominalCFirmsToYproduction;
						
		case LogGdpEx:
			if(gdpEx > 0.)
				return Math.log(gdpEx);
			else return Double.NaN;
		case ConsumptionToGDPexPercent:
			return 100. * consumptionToGDPex;
		case InvestmentToGDPexPercent:
			return 100. * investmentToGDPex;
		case LogGdpInc:
			if(gdpInc > 0.)
				return Math.log(gdpInc);
			else return Double.NaN;
		case WagesToGDPincPercent:
			return 100. * wagesToGDPinc;
		case ProfitsToGDPincPercent:
			return 100. * profitsToGDPinc;
		case InterestToGDPincPercent:
			return 100. * interestToGDPinc;
		case DepreciationToGDPincPercent:
			return 100. * depreciationToGDPinc;
		case LogGdpIncWithoutDep:
			if(gdpIncWithoutDep > 0.)
				return Math.log(gdpIncWithoutDep);
			else return Double.NaN;
		case WagesToGDPincWithoutDepPercent:
			return 100. * wagesToGDPincWithoutDep;
		case InterestToGDPincWithoutDepPercent:
			return 100. * interestToGDPincWithoutDep;
		case ProfitsToGDPincWithoutDepPercent:
			return 100. * profitsToGDPincWithoutDep;
		case GdpExToGdpInc:
			if(gdpInc > 0.)
				return gdpEx / gdpInc;
			else return Double.NaN;
		case GdpExToGdpIncWithoutDep:
			if(gdpIncWithoutDep > 0.)
				return gdpEx / gdpIncWithoutDep;
			else return Double.NaN;
			
			
			
			
			
			
			
			
			
			
			
			
		case LogAggregateDemand:
			if(initialAggregateDemand > 0.)
				return Math.log(initialAggregateDemand);
			else return Double.NaN;
		case LogEarnings:
			if(earnings > 0.)
				return Math.log(earnings);
			else return Double.NaN;
			
			
		default: 
			throw new IllegalArgumentException("Unsupported variable"); 
		}
	}

	@Override
	public int getIntValue(Enum<?> variableInt) {
		switch((Variables) variableInt){
		
		// TODO: can remove this one
		case MaxClientsPerFirm_kFirms:
			return this.maxClientsPerFirm_kFirms;
			
		case Exit_kFirms:
			return exit_kFirms;
		case Exit_cFirms:
			return exit_cFirms;
		case ExitLiquidityIssue_cFirms:
			return exitLiquidityIssue_cFirms;
		case ExitMarketShareIssue_cFirms:
			return exitMarketShareIssue_cFirms;
		case ExitAssetMarket_cFirms:
			return exitAssetMarket_cFirms;
			
//		case Flag:
//			return flag;
		
		default: 
			throw new IllegalArgumentException("Unsupported variable"); 
		
		}
	}


	// ---------------------------------------------------------------------
	// Own methods
	// ---------------------------------------------------------------------

	public void macroInitialization(){
		// Introductory note: define the initial conditions of the economy 
		
		// --- Labor market ---
		this.unemploymentRate 					= new double[]{1, 1};
		this.wage 					= new double[]{Parameters.getInitialWage(),Parameters.getInitialWage()}; 
		// Recall that all consumption-good firms have initially identical productivity 
		this.averageLaborProductivity 		= new double[]{0, Parameters.getInitialProductivity()}; 
		
		// --- Capital market ---
		// Id. for capital-good firms' machines  
		this.topProductivity_kFirms 				= Parameters.getInitialProductivity(); 
		this.topMachineProductivity 		= Parameters.getInitialProductivity(); 
		
		this.investmentTotal_cFirms 				= new double[]{0, 0};
		
		// --- Consumption market ---
		// The initial aggregate demand corresponds to the steady-state aggregate demand. The reasoning is presented in the code documentation 
		KFirm aKFirm 				= model.getKFirms().get(0); 
		CFirm aCFirm 				= model.getCFirms().get(0); 
		this.initialAggregateDemand 		= ( ( wage[0] / ( aKFirm.getFirmProductivity()[1] * Parameters.getA_kFirms() ) + Parameters.getFractionPastSalesInvestedInRandD_kFirms() * aKFirm.getPriceOfGoodProducedNow() ) * 
										aCFirm.getInvestment() / Parameters.getMachineSizeInCapital_cFirms() * ((double) model.getNumberOfCFirms()) * (1 - model.getUnemploymentBenefitShare()) +
										model.getUnemploymentBenefitShare() * wage[0] * MacroModel.getLabourSupply() ) / 
										(aCFirm.getPriceOfGoodProducedNow() - ((1 - model.getUnemploymentBenefitShare()) * wage[0]) / aCFirm.getProductivity() ); 
		
		// --- Consumption-good firms ---
		// All firms are ex-ante identical. Thus, their cost is price wage / productivity, and they price with the same mark-up
		double initialCPrice 		= (1 + model.getMarkUpRate()) * wage[1] / averageLaborProductivity[1]; 
		this.cpi 					= new double[]{0, initialCPrice};
		this.exitLiquidityIssue_cFirms 		= 0; 
		this.exitMarketShareIssue_cFirms		= 0;
		this.exitAssetMarket_cFirms		= 0;
		this.exit_cFirms 					= 0;
		
		this.consumption 			= new double[]{0, 0};
		this.unfilledConsumption 		= 0;
		this.diffTotalInventories_cFirms 					= 0;
		
		// Recall: [0] = t-2, [1] = t-1, [0] = t????????
		this.totalMarketShare_cFirms 					= new double[]{0., 0., 0.}; 
		this.meanCompetitiveness_cFirms 				= new double[]{1, 1}; 
		
		// --- Capital-good firms ---
		// All firms are ex-ante identical. Thus, their cost is price wage / (productivity * a), and they price with the same mark-up
		double initialKPrice 		= (1 + Parameters.getFixedMarkUp_kFirms()) * wage[1] / ( averageLaborProductivity[1] * Parameters.getA_kFirms() ); 
		this.ppi 					= new double[]{0, initialKPrice}; 
		this.exit_kFirms 					= 0;
		
		// --- Government ---
		this.govStock 				= 0;
		this.govStockToGdp 			= 0; 
		this.govBalanceToGdp 				= 0; 
		
		// --- Financial sector --- 
		this.bankruptcyRate_cFirms 		= 0;
		
		this.gdp = new double[]{0, 0}; 
		
	}

	void update(){
		
		log.info("PERIOD " + SimulationEngine.getInstance().getTime());
				
		/* Update temporal variables, from (t) to (t-1), and set variables that are incrementally calculated to 0. 
		NOTE: the wage is updated at the end of the period, because it requires information from the current period.  */
		this.averageLaborProductivity[0] 		= averageLaborProductivity[1];
		this.meanCompetitiveness_cFirms[0]		= meanCompetitiveness_cFirms[1];
		this.cpi[0] 							= cpi[1];
		this.ppi[0] 							= ppi[1];
		this.unemploymentRate[0] 				= unemploymentRate[1];
		this.totalMarketShare_cFirms[0] 		= totalMarketShare_cFirms[1];
		this.totalMarketShare_cFirms[1] 		= totalMarketShare_cFirms[2];
		this.gdp[0] 							= gdp[1];
		this.investmentTotal_cFirms[0] 			= investmentTotal_cFirms[1];
		this.consumption[0] 					= consumption[1];
		
		this.laborDemandUsedForProduction 		= 0;
		this.laborDemandForRandD 				= 0;
		this.diffTotalInventories_cFirms 		= 0;
		this.govRevenues 						= 0;
		this.govSpending 						= 0;
		this.creditRationingRate_cFirms 		= 0;
		
//		this.flag								= 0;
		
	}
	
	public void techFrontier(){
		
		// Compute the technological frontier, defined as the maximal level of labor and capital-good firms' productivity 
		fMaxProductivity_kFirms.updateSource();
		fMaxMachineProductivity.updateSource();	
		this.topProductivity_kFirms 					= fMaxProductivity_kFirms.getDoubleValue(IDoubleSource.Variables.Default);
		this.topMachineProductivity 			= fMaxMachineProductivity.getDoubleValue(IDoubleSource.Variables.Default);
		
		// Compute the aggregate R&D labor and total expenditures 
		fSumLaborDemandForRd.updateSource();
		fSumRdExpenditures_kFirms.updateSource();
		this.laborDemandForRandD 				= fSumLaborDemandForRd.getDoubleValue(IDoubleSource.Variables.Default);
		// Note: laborDemand is computed in the laborMarket() method, MacroModel class
		this.rdExpenditures_kFirms 			= fSumRdExpenditures_kFirms.getDoubleValue(IDoubleSource.Variables.Default);
		
	}
	
	public void laborDemandProd(){
		// Update aggregate labor market's variable (the labor demand from both sectors)
		
		this.laborDemandUsedForProduction 			= 0;
		this.laborDemand_cFirms 			= 0;
		this.laborDemand_kFirms 		= 0;
		
		for(CFirm cFirm : model.getCFirms()){
			this.laborDemandUsedForProduction 		+= cFirm.getLaborDemand();
			this.laborDemand_cFirms 		+= cFirm.getLaborDemand();
		}
		
		for(KFirm kFirm : model.getkFirms()){
			this.laborDemandUsedForProduction 		+= kFirm.getLaborDemandForProduction();
			this.laborDemand_kFirms 	+= kFirm.getLaborDemandForProduction();
		}
		
		this.laborDemand_kFirms 		+= laborDemandForRandD;
				
	}
	
	public void competitivenessAggregate(){
		// Compute the mean price and unfilled demand
		
		fMeanPastPrice_cFirms.updateSource();
		fMeanPresentPrice_cFirms.updateSource();
		fMeanUnfilledDemand.updateSource();
		
		meanUnfilledDemand 				= fMeanUnfilledDemand.getDoubleValue(IDoubleSource.Variables.Default);
		double meanPastPrice			= fMeanPastPrice_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		double meanPresentPrice			= fMeanPresentPrice_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		/*FIXME: 
		(a) Dosi et al. implementation use the two-periods average for the mean price
		(b) They do not divide it by 2 */
		meanPrice_cFirms 						= ( meanPastPrice + meanPresentPrice ) / 2.;
		
	}
	
	public void marketShareNormalization(){
		// Normalize the market share of consumption-good firms such that they always sum up to one.
		this.totalMarketShare_cFirms[0] 					= 0;
		this.totalMarketShare_cFirms[1] 					= 0;
		this.totalMarketShare_cFirms[2] 					= 0;
		
		for(CFirm cFirm : model.getCFirms()){
			this.totalMarketShare_cFirms[0] 				+= cFirm.getMarketShare()[0];
			this.totalMarketShare_cFirms[1] 				+= cFirm.getMarketShare()[1];
			this.totalMarketShare_cFirms[2] 				+= cFirm.getMarketShare()[2];
		}
		
		if(totalMarketShare_cFirms[0] <= 0 || totalMarketShare_cFirms[1] <= 0 || totalMarketShare_cFirms[2] <= 0)
			log.error("fTot[i] <= 0)");
		
		for(CFirm cFirm : model.getCFirms()){
			double[] newF 				= new double[]{
										cFirm.getMarketShare()[0] / totalMarketShare_cFirms[0],
										cFirm.getMarketShare()[1] / totalMarketShare_cFirms[1],
										cFirm.getMarketShare()[2] / totalMarketShare_cFirms[2] };
			cFirm.setMarketShare(newF);
			cFirm.setMarketShareTemp(cFirm.getMarketShare()[2]); 
		}
	}
	
	public void aggregateConsumption(){
		// Compute aggregate consumption
		
		// Compute government spending 
		this.govSpending 			= 0;
		if(unemployment > 0)
			this.govSpending 			+= unemployment * wage[1] * model.getUnemploymentBenefitShare();		//Note that bailouts may be included in this measure during accounting() method calls.
		
		// Compute aggregate consumption
		// Add the past consumption non-matched by the consumption-good firms' production to the current aggregate consumption
		this.earnings					= (laborDemand * wage[1]) + (unemployment * wage[1] * model.getUnemploymentBenefitShare());
		this.consumption[1] 			= earnings + unfilledConsumption * (1 + model.getInterestRate());
		aggConsumption					= consumption[1];
		
		log.fatal("Consumption variables: " + 
					"\n Gov. spending " + govSpending + 
					"\n past unfilled consumption " + unfilledConsumption + 
					"\n total consumption " + consumption[1]);
		
	}
	
	public void priceIndices(){
		// Compute the price indices 
		
		// Compute total production in both sectors 
		fSumTotalProduction_kFirms.updateSource();
		fSumTotalProduction_cFirms.updateSource();
		production_cFirms 					= fSumTotalProduction_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		production_kFirms 				= fSumTotalProduction_kFirms.getDoubleValue(IDoubleSource.Variables.Default);
		
		log.debug("Production update: " + 
					"\n total prod capital " + production_kFirms + 
					"\n total prod cons " + production_cFirms);	
		
		// Update the capital-good firms' market shares 
		for(KFirm kFirm : model.getKFirms()){
			double[] newF;
			if(production_kFirms > 0){
				double marketShare 		= kFirm.getProductionQuantity() / production_kFirms;
				newF 					= new double[]{
										kFirm.getMarketShare()[0],
										kFirm.getMarketShare()[1],
										marketShare };
			} else {
				newF 					= new double[]{
										kFirm.getMarketShare()[0],
										kFirm.getMarketShare()[1],
										kFirm.getMarketShare()[1] };
			}
			
			kFirm.setMarketShare(newF);
		}
		
		// Compute the two price indexes 
		this.cpi[1] 					= 0;
		for(CFirm cFirm : model.getCFirms())
			this.cpi[1] 				+= cFirm.getPriceOfGoodProducedNow() * cFirm.getMarketShare()[2];
		
		this.ppi[1] 					= 0;
		for(KFirm kFirm : model.getKFirms()){
			if(production_kFirms > 0)
				this.ppi[1] 			+= kFirm.getPriceOfGoodProducedNow() * kFirm.getMarketShare()[2];
			else 
				this.ppi[1] 			+= kFirm.getPriceOfGoodProducedNow();
		}
		
		// If production capital is nil, assume that all firms have identical market shares 
		if(production_kFirms <= 0)
			this.ppi[1] 				/= (double) model.getNumberOfKFirms();
		
		log.debug("Price index variables: " +
					"\n cpi: (" + cpi[0] + ", " + cpi[1] + ") " +
					"\n ppi: (" + ppi[0] + ", " + ppi[1] + "). INFO on ppi computation:  " +
					"\n production capital: " + production_kFirms);
		
	}
	
	void aggregateComputation(){
		
		this.creditRationingRate_cFirms 		/= (model.getNumberOfCFirms() / 100.);		// Divide by 100 to create percentage.
		
		// CONSUMPTION-GOOD FIRMS
		// Investments
		
		fSumInvestmentExpansionary.updateSource();
		fSumInvestmentSubstitutionary.updateSource();
		fSumDemand_cFirms.updateSource();
		fSumProfit_cFirms.updateSource();
		fSumGrossOperatingSurplus_cFirms.updateSource();
		fSumLiquidAssets_cFirms.updateSource();
		
		// Investments is expressed in terms of machines
		this.investmentExpansionaryTotal_cFirms 				= fSumInvestmentExpansionary.getDoubleValue(IDoubleSource.Variables.Default);
		this.investmentExpansionaryTotal_cFirms		 		/= Parameters.getMachineSizeInCapital_cFirms();
		this.investmentSubstitutionaryTotal_cFirms 				= fSumInvestmentSubstitutionary.getDoubleValue(IDoubleSource.Variables.Default);
		this.investmentSubstitutionaryTotal_cFirms 				/= Parameters.getMachineSizeInCapital_cFirms();
		this.investmentTotal_cFirms[1] 				= investmentSubstitutionaryTotal_cFirms + investmentExpansionaryTotal_cFirms;
		
		// Total liquid asset
		this.totalLiquidAssets_cFirms 		= fSumLiquidAssets_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		
		// Total demand received by the firms 
		this.demand_cFirms 				= fSumDemand_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		
		// Total profit 
		this.profit_cFirms 				= fSumProfit_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		this.grossOperatingSurplus_cFirms 				= fSumGrossOperatingSurplus_cFirms.getDoubleValue(IDoubleSource.Variables.Default);
		
		// Productivity
		this.meanProductivityWeightedByMarketShare_cFirms 		= 0; // mean prod. with weight = market share
		this.meanLogProductivity_cFirms 				= 0; // mean prod. in log
		this.productionNominal_cFirms 			= 0; // nominal total production
		this.diffTotalInventoriesNominal_cFirms 					= 0; // nominal total variation of inventories 
		this.averageLaborProductivity[1] 		= 0; // measure of productivity taken into account in the wage equation
		this.herfindahlMeasure_cFirms 						= 0; // Herfindahl measure 
		this.meanCostWeightedByMarketShare_cFirms 				= 0; // mean cost, with weight = market share
		this.capitalStock_cFirms 				= 0; // capital stock
		this.totalBadDebt						= 0.;
		this.meanLiquidityToSalesRatio_cFirms 	= 0.;
		
		for(CFirm cFirm : model.getCFirms()){
			this.meanProductivityWeightedByMarketShare_cFirms 	+= cFirm.getProductivity() * cFirm.getMarketShare()[2];
			this.meanLogProductivity_cFirms 			+= Math.log(cFirm.getProductivity());
			this.productionNominal_cFirms 		+= cFirm.getProductionQuantity() * cFirm.getPriceOfGoodProducedNow();
			this.diffTotalInventoriesNominal_cFirms 				+= cFirm.getInventories()[1] * cFirm.getPriceOfGoodProducedNow() - cFirm.getInventories()[0] * cFirm.getPriceOfGoodProducedPrevious();
			this.averageLaborProductivity[1] 	+= cFirm.getLaborDemand() * cFirm.getProductivity() / laborDemandUsedForProduction;
			this.herfindahlMeasure_cFirms 					+= cFirm.getMarketShare()[2] * cFirm.getMarketShare()[2];
			this.meanCostWeightedByMarketShare_cFirms 			+= cFirm.getCostToProduceGood() * cFirm.getMarketShare()[2];
			this.capitalStock_cFirms 			+= cFirm.getCapitalStock();
			this.totalBadDebt					+= cFirm.getBadDebt();
			this.meanLiquidityToSalesRatio_cFirms 	+=	cFirm.getNetWorthToSalesRatio();
		}
		this.meanLogProductivity_cFirms 				/= (double) model.getNumberOfCFirms();
		this.herfindahlMeasure_cFirms 						= (herfindahlMeasure_cFirms - 1 / ((double) model.getNumberOfCFirms()) ) / 1 / 
										((double) model.getNumberOfCFirms());
		this.meanLiquidityToSalesRatio_cFirms 	/=	(double) model.getNumberOfCFirms();
		
		if(meanCostWeightedByMarketShare_cFirms > 0)
			// Equivalent to cpi / c - 1
			this.markUpTot_cFirms 				= (this.cpi[1] - meanCostWeightedByMarketShare_cFirms) / meanCostWeightedByMarketShare_cFirms; 
		else 
			System.err.println("ERROR: costTot <= 0");
		
		// CAPITAL-GOOD FIRMS
		
		// Total profit 
		fSumProfit_kFirms.updateSource();
		this.profit_kFirms 				= fSumProfit_kFirms.getDoubleValue(IDoubleSource.Variables.Default);
		// Total asset
		fSumLiquidAsset_kFirms.updateSource();
		this.totalLiquidAssets_kFirms 		= fSumLiquidAsset_kFirms.getDoubleValue(IDoubleSource.Variables.Default);

		// Productivity
		this.meanProductivityWeightedByMarketShare_kFirms 		= 0; // mean productivity amongst the capital-good firms, with weight = market share 
		this.meanMachineProductivityWeightedByMarketShare = 0; // mean productivity amongst the machine produced, with weight = market share 
		this.meanLogProductivity_kFirms 				= 0;
		this.herfindahlMeasure_kFirms 					= 0; // Herfindahl measure 
		this.maxClientsPerFirm_kFirms 					= 0; // maximal number of clients
		productionNominal_kFirms						= 0;
		
		for(KFirm kFirm : model.getKFirms()){
			if(production_kFirms > 0){
				productionNominal_kFirms 		+= kFirm.getProductionQuantity() * kFirm.getPriceOfGoodProducedNow();
				this.meanProductivityWeightedByMarketShare_kFirms += kFirm.getFirmProductivity()[1] * kFirm.getMarketShare()[2];
				this.meanMachineProductivityWeightedByMarketShare += kFirm.getMachineProduced().getMachineProductivity()[1] * kFirm.getMarketShare()[2];
			} else {
				this.meanProductivityWeightedByMarketShare_kFirms += kFirm.getFirmProductivity()[1];
				this.meanMachineProductivityWeightedByMarketShare += kFirm.getMachineProduced().getMachineProductivity()[1];
			}
			this.meanLogProductivity_kFirms 			+= Math.log(kFirm.getFirmProductivity()[1]);
			this.herfindahlMeasure_kFirms 				+= kFirm.getMarketShare()[2] * kFirm.getMarketShare()[2];
			// The average labor productivity takes also into account the capital good firms' productivity
			this.averageLaborProductivity[1] 	+= kFirm.getFirmProductivity()[1] * kFirm.getLaborDemandForProduction() / laborDemandUsedForProduction;
			
			if(kFirm.getClients().size() > maxClientsPerFirm_kFirms)
				this.maxClientsPerFirm_kFirms 			= kFirm.getClients().size();
		}
		this.herfindahlMeasure_kFirms 					= (herfindahlMeasure_kFirms - 1 / ( (double) model.getNumberOfKFirms()) ) / (1 / ( (double) model.getNumberOfKFirms()) );
		this.meanLogProductivity_kFirms 				/= (double) model.getNumberOfKFirms();
		
		// As for the price index, if the production of the capital is nil, assume that the firms have identical market share 
		if(production_kFirms <= 0)
			this.meanProductivityWeightedByMarketShare_kFirms 	/= (double) model.getNumberOfKFirms();
		
		// MACRO VARIABLES
		
		// Consumption 
		this.realConsumption 			= consumption[1] / cpi[1];
		this.realConsumption 			-= unfilledConsumption;
		// Production
		this.output 					= realConsumption + production_kFirms;
		// GDP; equation (11) in Dosi et al. (2013)
		this.gdp[1] 					= realConsumption + production_kFirms + diffTotalInventories_cFirms;		//Note that production_kFirms = investmentTotal_cFirms.
		this.gdpNominal					= productionNominal_kFirms + productionNominal_cFirms + diffTotalInventoriesNominal_cFirms;
		this.gdpLog 					= Math.log(gdp[1]);
				
		
		
		// GDP growth
		if(SimulationEngine.getInstance().getTime() > 0)
			this.gdpGrowth 				= Math.log(gdp[1]) - Math.log(gdp[0]);
		else 
			this.gdpGrowth 				= 0;
		//	Total factor productivity 
		if(laborDemand > 0)
			this.totalFactorProductivity 					= gdp[1] / laborDemand;
		else 
			this.totalFactorProductivity 					= gdp[1];
		// Credit activity
		this.averageCreditDemandToCreditSupplyRatio = aggregateCreditDemand / model.getBank().creditSupply;
		
		
		// GOVERNMENTS
		this.govBalance 				= govRevenues - govSpending;
		// If govStock < 0, it is the government's debt
		this.govStock 					+= govBalance; 
		this.govBalanceToGdp 			= govBalance / gdpNominal;
		this.govStockToGdp 				= govStock / gdpNominal;
		this.govSpendingToGdp 			= govSpending / gdpNominal;
		this.govRevenuesToGdp			= govRevenues / gdpNominal;
		
		// WAGE
		this.wage[0] 					= wage[1];
		
		log.debug("WAGE COMPUTATION. Wage(t-1) = " + wage[0] + " and inputs " + 
					"\n u : ( " + unemploymentRate[0] + ", " + unemploymentRate[1] + " )" +
					"\n cpi : ( " + cpi[0] + ", " + cpi[1] + " )" +
					"\n AB : ( " + averageLaborProductivity[0] + ", " + averageLaborProductivity[1] + " )");
		
		// Prevents the unemployment rate to be nil, which would make us divide by zero
		if(unemploymentRate[0] < Parameters.getNaturalLevelUnemployment())
			this.unemploymentRate[0] 				= Parameters.getNaturalLevelUnemployment();			//XXX: Should we also update the 'unemployment' field?  Or even the laborDemand field for consistency?  Or this only used for calculating wage inflation and avoiding divide by zero errors?
		
		// Inputs for equation (10) in Dosi et al. (2013))
		this.diffUnemploymentRate 						= (unemploymentRate[1] - unemploymentRate[0]) / unemploymentRate[0];
		this.diffCPI 					= (cpi[1] - cpi[0]) / cpi[0];
		this.diffProductivity 					= (averageLaborProductivity[1] - averageLaborProductivity[0]) / averageLaborProductivity[0];
		
		log.debug("Before putting upper/lower bound on var. wage, we have: " + 
				"\n diffU " + diffUnemploymentRate + 
				"\n diffCpi " + diffCPI + 
				"\n diffProd " + diffProductivity);
		
		// Put an upper bound on the growth rate of the three differences. For this, follow Dosi et al. implementation
		if(diffCPI > cpi[0] * Parameters.getMaxVariation_Wages())
			diffCPI 					= cpi[0] * Parameters.getMaxVariation_Wages();
		if(diffUnemploymentRate > unemploymentRate[0] * Parameters.getMaxVariation_Wages())
			diffUnemploymentRate 						= unemploymentRate[0] * Parameters.getMaxVariation_Wages();
		if(diffProductivity > averageLaborProductivity[0] * Parameters.getMaxVariation_Wages())
			diffProductivity			 		= averageLaborProductivity[0] * Parameters.getMaxVariation_Wages();
		
		// Put a lower bound on the growth rate of these three differences 
		if(diffCPI < - cpi[0] * Parameters.getMaxVariation_Wages())
			diffCPI 					= - cpi[0] * Parameters.getMaxVariation_Wages();
		if(diffUnemploymentRate < - unemploymentRate[0] * Parameters.getMaxVariation_Wages())
			diffUnemploymentRate 						= - unemploymentRate[0] * Parameters.getMaxVariation_Wages();
		if(diffProductivity < - averageLaborProductivity[0] * Parameters.getMaxVariation_Wages())
			diffProductivity 					= - averageLaborProductivity[0] * Parameters.getMaxVariation_Wages();
		
		this.diffWage 					= Parameters.getCoeffLaborProd_Wages() * diffProductivity + Parameters.getCoeffCPI_Wages() * diffCPI + 
										Parameters.getCoeffUnemployment_Wages() * diffUnemploymentRate;
		this.wage[1] 					= wage[0] * (1 + diffWage);
		
		log.debug("Final wage: " + wage[1]);
		
		this.diffLogWage 				= Math.log(wage[1]) - Math.log(wage[0]);
		this.diffLogCPI 				= Math.log(cpi[1]) - Math.log(cpi[0]);
		
		// TODO: to remove eventually
		this.avgAgeMachines_cFirms 				= 0;
		this.totalInventories 						= 0; 
		this.sumDesiredProduction_cFirms 			= 0;
		this.capitalStockDesired_cFirms 		= 0;
		this.capitalStockTopLimit_cFirms 			= 0;
		this.desiredExpansionaryInvestmentTotal_cFirms 				= 0;
		this.desiredExpansionaryInvestmentTotalStar_cFirms 			= 0;
		
		for(CFirm cFirm : model.getCFirms()){
			double avgAge 				= 0;
			double numbMachine 			= 0;
			for(Map.Entry<Machine, Integer> entry: cFirm.getMachineAgeMap().entrySet()){
				numbMachine 			+= cFirm.getMachineQuantityMap().get(entry.getKey());
				avgAge 					+= entry.getValue().doubleValue() * cFirm.getMachineQuantityMap().get(entry.getKey()).doubleValue();
			}
			avgAge 						/= numbMachine;
			this.avgAgeMachines_cFirms 			+= avgAge;
			this.totalInventories 					+= cFirm.getInventories()[1];
			this.sumDesiredProduction_cFirms 		+= cFirm.getdQ();
			this.capitalStockDesired_cFirms 	+= cFirm.getDesiredCapitalStock();
			this.capitalStockTopLimit_cFirms 		+= cFirm.getMaxPossibleCapitalStock();
			this.desiredExpansionaryInvestmentTotal_cFirms 			+= cFirm.getdInvestmentExpansionary();
			this.desiredExpansionaryInvestmentTotalStar_cFirms 		+= cFirm.getdInvestmentExpansionaryStar();
		}
		this.avgAgeMachines_cFirms 				/= model.getNumberOfCFirms();
		this.desiredExpansionaryInvestmentTotal_cFirms 				/= Parameters.getMachineSizeInCapital_cFirms();
		this.desiredExpansionaryInvestmentTotalStar_cFirms 			/= Parameters.getMachineSizeInCapital_cFirms();
		
		
		//Ross:  New variables to establish accounting identities
		
		//Production view of (nominal) GDP: I assume the equations in Dosi et al. (eq 11 in 2010 and just under eq 15 in 2013) are incorrect, in that production Q is not weighted by the cost of production.  In our case, we do weigh by costs to get monetary values.
		Yproduction = productionNominal_kFirms + productionNominal_cFirms;
		productionNominalKFirmsToYproduction = productionNominal_kFirms / Yproduction;
		productionNominalCFirmsToYproduction = productionNominal_cFirms / Yproduction;
				
		//Expenditure view of (nominal) GDP
		Ycin		= aggConsumption + investmentTotal_cFirms[1] + diffTotalInventoriesNominal_cFirms;
		consumptionToYcin = aggConsumption / Ycin;
		investmentToYcin = investmentTotal_cFirms[1] / Ycin;
		changeInInventoriesValueToYcin = diffTotalInventoriesNominal_cFirms / Ycin;
		

//		Expenditure approach of GDP:
//			GDP = consumption + gross investment, 
//		where investment includes investment in machines ("fixed investment") and investment in inventories. Note that gross investment and not net investment (net of depreciation) should be considered.
//		Note that banks here do not play any role, as they do not sell services to consumers nor they invest (in our simple economy).
		grossInvestment = investmentTotal_cFirms[1] + diffTotalInventories_cFirms;	//Note investmentTotal_cFirms = production_kFirms.
		gdpEx = realConsumption + grossInvestment;
		consumptionToGDPex = realConsumption / gdpEx;
		investmentToGDPex = grossInvestment / gdpEx;
		
		
//		Cost/income approach of GDP:		
//			GDP = wages + interests + profits (all before taxes) + depreciation
//		Depreciation is included because this is one of the "destinations" GDP could go to, i.e. covering for losses in capital. 
//		Profits are computed as 
//			profits = sales (to the market) - cost of production
//		where
//			costs = purchases of intermediate goods and services (null in our case) +  wages + interests - delta(inventories) + depreciation
//			delta(inventories) is measured as end-of-period - beginning-of-period. Note that a decrease in inventories is associated to an increase in costs, as goods are fictitiously "bought" from the deposit.
//		Substituting, we obtain 
//			GDP = wages + interests + sales - wages - interests + delta(inventories) - depreciation + depreciation
//			    = value of production
//			    = sales + delta(inventories)
		
		double profitsTotal = grossOperatingSurplus_cFirms + profit_kFirms + model.getBank().profit;		//XXX: Cannot use profit_cFirms, as this already includes interest on deposits - interest on loans.  Note that the negative of this term is actually the Bank's profit which we include here, because the return on deposits and loans is different.
		double wagesTotal = wage[1] * laborDemand;
		double interests = model.getBank().depositRevenues;						//XXX: Do we need to deduct interest on loans?
		double depreciation = investmentSubstitutionaryTotal_cFirms;			//XXX: Use cFirms' substitionary investment measure to calculate depreciation???  Is this correct?
		gdpInc = wagesTotal + interests + profitsTotal + depreciation;			//ROSS: Not sure how to handle depreciation.  Do we use substitionary investment of cFirms?
		wagesToGDPinc = wagesTotal / gdpInc;
		interestToGDPinc = interests / gdpInc;
		profitsToGDPinc = profitsTotal / gdpInc;
		depreciationToGDPinc = depreciation / gdpInc;

		//Exclude depreciation (as not sure if it is correct measurement - it is currently based on substitionary investment of cFirms)
		gdpIncWithoutDep = wagesTotal + interests + profitsTotal;
		wagesToGDPincWithoutDep = wagesTotal / gdpIncWithoutDep;
		interestToGDPincWithoutDep = interests / gdpIncWithoutDep;
		profitsToGDPincWithoutDep = profitsTotal / gdpIncWithoutDep;
				
				
				
	}
	
	void dumpInStatistics(){
		
		statistics.setConsumption(consumption[1]);
		statistics.setCPI(cpi[1]);
		statistics.setCreditDemand(aggregateCreditDemand);
		statistics.setExit_kFirms(exit_kFirms);
		statistics.setExit_cFirms(exit_cFirms);
		statistics.setGDP(gdp[1]);
		statistics.setGdpGrowth(gdpGrowth);
		statistics.setGovDebt(govStock);
		statistics.setGovRevenues(govRevenues);
		statistics.setGovSpending(govSpending);
		statistics.setHerfindahlMeasure_kFirms(herfindahlMeasure_kFirms);
		statistics.setHerfindahlMeasure_cFirms(herfindahlMeasure_cFirms);
		statistics.setInvestmentExpansionaryTotal(investmentExpansionaryTotal_cFirms);
		statistics.setInvestmentSubstitionaryTotal(investmentSubstitutionaryTotal_cFirms);
		statistics.setLaborDemand(laborDemand);
		statistics.setMeanMachineProductivityWeightedByMarketShare(meanMachineProductivityWeightedByMarketShare);
		statistics.setMeanProductivityWeightedByMarketShare_kFirms(meanProductivityWeightedByMarketShare_kFirms);
		statistics.setMeanProductivityWeightedByMarketShare_cFirms(meanProductivityWeightedByMarketShare_cFirms);
		statistics.setPPI(ppi[1]);
		statistics.setProduction_kFirms(production_kFirms);
		statistics.setProduction_cFirms(production_cFirms);
		statistics.setProfit_kFirms(profit_kFirms);
		statistics.setProfit_cFirms(profit_cFirms);
		statistics.setRealConsumption(realConsumption);
		statistics.setTotalFactorProductivity(totalFactorProductivity);
		statistics.setTopProductivity_kFirms(topProductivity_kFirms);
		statistics.setTopMachineProductivity(topMachineProductivity);
		statistics.setUmemploymentRate(unemploymentRate[1]);
		statistics.setWage(wage[1]);
		
		
		statistics.setTotalDebt(model.getBank().debt);
		statistics.setBadDebt(totalBadDebt);
		statistics.setCreditSupply(model.getBank().creditSupply);
		statistics.setBankruptcyRate(bankruptcyRate_cFirms / model.getNumberOfCFirms());
		statistics.setLiquidityToSalesRatio(meanLiquidityToSalesRatio_cFirms);
		
		
						
		double invGrowth 				= 0;
		if(investmentTotal_cFirms[0] != 0)
			invGrowth 					= (investmentTotal_cFirms[1] - investmentTotal_cFirms[0]) / investmentTotal_cFirms[0];
		statistics.setInvestmentGrowth(invGrowth);
		
		double consGrowth 				= 0;
		if(consumption[0] != 0)
			consGrowth 					= (consumption[1] - consumption[0]) / consumption[0];
		statistics.setConsumptionGrowth(consGrowth);
		
	}
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public boolean isExportToCSV() {
		return exportToCSV;
	}

	public void setExportToCSV(boolean exportToCSV) {
		this.exportToCSV = exportToCSV;
	}

	public boolean isExportToDatabase() {
		return exportToDatabase;
	}

	public void setExportToDatabase(boolean exportToDatabase) {
		this.exportToDatabase = exportToDatabase;
	}

	public Double getTimestepsBetweenSnapshots() {
		return timestepsBetweenSnapshots;
	}

	public void setTimestepsBetweenSnapshots(Double timestepsBetweenSnapshots) {
		this.timestepsBetweenSnapshots = timestepsBetweenSnapshots;
	}
	
	public double getLaborDemandUsedForProduction(){
		return laborDemandUsedForProduction;
	}

	public boolean isExportFirmsData() {
		return exportFirmsData;
	}

	public void setExportFirmsData(boolean exportFirmsData) {
		this.exportFirmsData = exportFirmsData;
	}

	public Integer getInitialTimestepsToIgnore() {
		return initialTimestepsToIgnore;
	}

	public void setInitialTimestepsToIgnore(Integer initialTimestepsToIgnore) {
		this.initialTimestepsToIgnore = initialTimestepsToIgnore;
	}

	public boolean isExportBankData() {
		return exportBankData;
	}

	public void setExportBankData(boolean exportBankData) {
		this.exportBankData = exportBankData;
	}

}