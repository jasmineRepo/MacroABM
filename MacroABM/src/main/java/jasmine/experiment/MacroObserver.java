package jasmine.experiment;

import jasmine.data.Parameters;
import jasmine.model.Bank;
import jasmine.model.MacroModel;

import microsim.annotation.GUIparameter;
import microsim.engine.AbstractSimulationObserverManager;
import microsim.engine.SimulationCollectorManager;
import microsim.engine.SimulationManager;
import microsim.event.CommonEventType;
import microsim.event.EventGroup;
import microsim.event.EventListener;
import microsim.gui.GuiUtils;
import microsim.gui.plot.TimeSeriesSimulationPlotter;
import microsim.statistics.IDoubleSource;
import microsim.statistics.IIntSource;
import microsim.statistics.functions.MultiTraceFunction;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

public class MacroObserver extends AbstractSimulationObserverManager implements EventListener {
	
	@GUIparameter(description = "Set a regular time for any charts to update")
	Double chartUpdatePeriod = 1.;
	@GUIparameter(description = "Toggle to decide if GUI includes data visualization")
	Boolean charts = true;
	@GUIparameter(description = "Toggle to show charts helping to compare the model with Dosi et al.")
	Boolean dosiComparison = false;
	
	private final static Logger log = Logger.getLogger(MacroObserver.class);
	
//	private TimeSeriesSimulationPlotter inflations;
//	private TimeSeriesSimulationPlotter goodMarket;
//	private TimeSeriesSimulationPlotter capitalMarket;
//	private TimeSeriesSimulationPlotter exit;
//	private TimeSeriesSimulationPlotter government;
//	private TimeSeriesSimulationPlotter laborMarket;
//	private TimeSeriesSimulationPlotter profit;
//	private TimeSeriesSimulationPlotter flag;
//	
//	private TimeSeriesSimulationPlotter bankMarket;
//	private TimeSeriesSimulationPlotter debt;
//	private TimeSeriesSimulationPlotter rd;
//	private TimeSeriesSimulationPlotter clientAndAge;
//	private TimeSeriesSimulationPlotter inventories;
//	private TimeSeriesSimulationPlotter asset;
//	private TimeSeriesSimulationPlotter creditRationing;
//	private TimeSeriesSimulationPlotter capital;
//	
//	private TimeSeriesSimulationPlotter logOutputConsumptionInvestment;
//	private TimeSeriesSimulationPlotter logInvestments;
//	private TimeSeriesSimulationPlotter logProductivityMomentsC;
//	private TimeSeriesSimulationPlotter logProductivityMomentsK;
//	private TimeSeriesSimulationPlotter investmentLumpinessLow;
//	private TimeSeriesSimulationPlotter investmentLumpinessHigh;
	
	/////////////////// Ross additions ////////////////////////////////////////////////
	
	private Set<JInternalFrame> updateChartSet;			//Collection of graphical objects to be updated on schedule
	private Set<JComponent> tabSet;						//Collection of graphical objects to be displayed in a tabbed Frame
	
	//Boolean GUI parameter toggles to switch a particular chart on / off
	
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean acountingIdentities = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean consumptionComponents = false;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean gdpComponents = false;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean nominalGDPcomponents = false;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean nationalAccounts = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean gdpConsumptionInvestment = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean expenditureComponentsToGDP = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean creditActivity = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean creditRationing = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean debt = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean capitalStock = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean costPriceMarkup = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean investment = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean investmentToGDP = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean consumptionGoodsMarket = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean productivity = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean profit = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean profitToGDP = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean machines = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean governmentRatios = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean laborMarket = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean firmExitRates = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean assets = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean meanProductivity = true;
	@GUIparameter(description = "Toggle to turn chart on / off")
	private boolean inflation = true;
	
	

	public MacroObserver(SimulationManager manager, SimulationCollectorManager collectorManager) {
		super(manager, collectorManager);
	}

	// ---------------------------------------------------------------------
	// Manager methods
	// ---------------------------------------------------------------------

	@Override
	public void buildObjects() {
		
		final MacroCollector collector = (MacroCollector) getCollectorManager();
		final MacroModel model = (MacroModel) getManager();
		final Bank bank = (Bank) model.getBank();
		
		if(charts){
						
			updateChartSet = new LinkedHashSet<JInternalFrame>();	//Set of all charts needed to be scheduled for updating
			tabSet = new LinkedHashSet<JComponent>();		//Set of all JInternalFrames each having a tab.  Each tab frame will potentially contain more than one chart each.

			if(acountingIdentities) {
				
				Set<JInternalFrame> accountingIdentitiesPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter yPlot = new TimeSeriesSimulationPlotter("Y comparison", "Log"); 
				yPlot.addSeries("Y = C + I + N", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogYcin));
				yPlot.addSeries("Y = Sum of production values", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogYproduction));
				updateChartSet.add(yPlot);			//Add to set to be updated in buildSchedule method
				accountingIdentitiesPlots.add(yPlot); 							    			    			    
			    TimeSeriesSimulationPlotter yComparisonPlot = new TimeSeriesSimulationPlotter("Ycin / Yproduction", ""); 
				yComparisonPlot.addSeries("Ycin / Yproduction", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.YcinToYproduction));
				updateChartSet.add(yComparisonPlot);			//Add to set to be updated in buildSchedule method
				accountingIdentitiesPlots.add(yComparisonPlot); 							    			    			    
				TimeSeriesSimulationPlotter yCINcomponentsPlot = new TimeSeriesSimulationPlotter("Components of Ycin", "%"); 
				yCINcomponentsPlot.addSeries("Consumption / Ycin", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumptionToYcinPercent));
				yCINcomponentsPlot.addSeries("Investment / Ycin", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.InvestmentToYcinPercent));
				yCINcomponentsPlot.addSeries("Change in Inventories Values / Ycin", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ChangeInInventoriesValueToYcinPercent));
				updateChartSet.add(yCINcomponentsPlot);			//Add to set to be updated in buildSchedule method
				accountingIdentitiesPlots.add(yCINcomponentsPlot);
				TimeSeriesSimulationPlotter yProductioncomponentsPlot = new TimeSeriesSimulationPlotter("Components of Yproduction", "%"); 
				yProductioncomponentsPlot.addSeries("kFirms production / Yproduction", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProductionNominalKFirmsToYproductionPercent));
				yProductioncomponentsPlot.addSeries("cFirms production / Yproduction", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProductionNominalCFirmsToYproductionPercent));
				updateChartSet.add(yProductioncomponentsPlot);			//Add to set to be updated in buildSchedule method
				accountingIdentitiesPlots.add(yProductioncomponentsPlot); 							    			 
				tabSet.add(createScrollPaneFromPlots(accountingIdentitiesPlots, "Consumption Components", accountingIdentitiesPlots.size()));
				
			}
			
			

			if(consumptionComponents) {
				//CHECKS
				Set<JInternalFrame> consumptionPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter laborDemandPlot = new TimeSeriesSimulationPlotter("Labor Demand", ""); 
				laborDemandPlot.addSeries("Labor Demand", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LaborDemand));
				updateChartSet.add(laborDemandPlot);			//Add to set to be updated in buildSchedule method
				consumptionPlots.add(laborDemandPlot); 							    			    			    
			    TimeSeriesSimulationPlotter wagesPlot = new TimeSeriesSimulationPlotter("Wage", "Log"); 
				wagesPlot.addSeries("Wage", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogWage));
				updateChartSet.add(wagesPlot);			//Add to set to be updated in buildSchedule method
				consumptionPlots.add(wagesPlot); 							    			    			    
				TimeSeriesSimulationPlotter govSpendingPlot = new TimeSeriesSimulationPlotter("Gov Spending", "Log"); 
				govSpendingPlot.addSeries("Gov Spending", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogGovSpending));
				updateChartSet.add(govSpendingPlot);			//Add to set to be updated in buildSchedule method
				consumptionPlots.add(govSpendingPlot); 							    			 
				TimeSeriesSimulationPlotter pastConsumptionPlot = new TimeSeriesSimulationPlotter("Past Consumption", "Log"); 
				pastConsumptionPlot.addSeries("Past Consumption", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogPastConsumption));
				updateChartSet.add(pastConsumptionPlot);			//Add to set to be updated in buildSchedule method
				consumptionPlots.add(pastConsumptionPlot); 							    			 
				TimeSeriesSimulationPlotter interestRatePlot = new TimeSeriesSimulationPlotter("Interest Rate", ""); 
				interestRatePlot.addSeries("Interest Rate", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.InterestRate));
				updateChartSet.add(interestRatePlot);			//Add to set to be updated in buildSchedule method
				consumptionPlots.add(interestRatePlot);
				tabSet.add(createScrollPaneFromPlots(consumptionPlots, "Consumption Components", consumptionPlots.size()));
			}
			
			
			if(gdpComponents) {
				Set<JInternalFrame> gdpPlots = new LinkedHashSet<JInternalFrame>();
				TimeSeriesSimulationPlotter productionKFirmsPlot = new TimeSeriesSimulationPlotter("Production kFirms", "Log"); 
				productionKFirmsPlot.addSeries("Production (kFirms)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProduction_kFirms));
				updateChartSet.add(productionKFirmsPlot);			//Add to set to be updated in buildSchedule method
				gdpPlots.add(productionKFirmsPlot); 							   
			    TimeSeriesSimulationPlotter realConsumptionPlot = new TimeSeriesSimulationPlotter("Real Consumption", "Log"); 
				realConsumptionPlot.addSeries("Real Consumption", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogRealConsumption));
				updateChartSet.add(realConsumptionPlot);			//Add to set to be updated in buildSchedule method
				gdpPlots.add(realConsumptionPlot);
				TimeSeriesSimulationPlotter diffInventoriesPlot = new TimeSeriesSimulationPlotter("Diff in Inventories (cFirms)", "Log"); 
				diffInventoriesPlot.addSeries("Diff in Inventories", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDiffInventories_cFirms));
				updateChartSet.add(diffInventoriesPlot);			//Add to set to be updated in buildSchedule method
				gdpPlots.add(diffInventoriesPlot); 							    
				tabSet.add(createScrollPaneFromPlots(gdpPlots, "GDP components", gdpPlots.size()));
			}
			
			if(nominalGDPcomponents) {
				Set<JInternalFrame> gdpNominalPlots = new LinkedHashSet<JInternalFrame>();
				TimeSeriesSimulationPlotter productionNominalKFirmsPlot = new TimeSeriesSimulationPlotter("Nominal Production kFirms", "Log"); 
				productionNominalKFirmsPlot.addSeries("Nominal Production (kFirms)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProductionNominal_kFirms));
				updateChartSet.add(productionNominalKFirmsPlot);			//Add to set to be updated in buildSchedule method
				gdpNominalPlots.add(productionNominalKFirmsPlot);
				TimeSeriesSimulationPlotter productionNominalCFirmsPlot = new TimeSeriesSimulationPlotter("Nominal Production cFirms", "Log"); 
				productionNominalCFirmsPlot.addSeries("Nominal Production (cFirms)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProductionNominal_cFirms));
				updateChartSet.add(productionNominalCFirmsPlot);			//Add to set to be updated in buildSchedule method
				gdpNominalPlots.add(productionNominalCFirmsPlot); 							    
				TimeSeriesSimulationPlotter diffInventoriesNominalPlot = new TimeSeriesSimulationPlotter("Diff in Nominal Inventories (cFirms)", "Log"); 
				diffInventoriesNominalPlot.addSeries("Diff in Nominal Inventories", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDiffInventoriesNominal_cFirms));
				updateChartSet.add(diffInventoriesNominalPlot);			//Add to set to be updated in buildSchedule method
				gdpNominalPlots.add(diffInventoriesNominalPlot); 							    
				tabSet.add(createScrollPaneFromPlots(gdpNominalPlots, "Nominal GDP components", gdpNominalPlots.size()));
			}
			
			if(nationalAccounts) {
			    Set<JInternalFrame> accountingPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter expenditurePlot = new TimeSeriesSimulationPlotter("Expenditure", "%"); 
				expenditurePlot.addSeries("Consumption To GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumptionToGDPpercent));
				expenditurePlot.addSeries("Investment to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.InvestmentToGDPpercent));
				expenditurePlot.addSeries("Change in Inventories (Consumption Goods) to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.DiffTotalInventoriesToGDPpercent));
	//			expenditurePlot.addSeries("Government Spending to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovSpendingToGDPpercent));			
				updateChartSet.add(expenditurePlot);			//Add to set to be updated in buildSchedule method
				accountingPlots.add(expenditurePlot); 							    			    			    
				TimeSeriesSimulationPlotter incomePlot = new TimeSeriesSimulationPlotter("Income", "%");
				incomePlot.addSeries("Wages + Benefits to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.WagesPlusUnemploymentBenefitsToGDPpercent));
				incomePlot.addSeries("Profits to GDP (Firms + Bank)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.FirmAndBankProfitsToGDPpercent));
				updateChartSet.add(incomePlot);			//Add to set to be updated in buildSchedule method
				accountingPlots.add(incomePlot);
				
				TimeSeriesSimulationPlotter consumptionPlusProftsPlot = new TimeSeriesSimulationPlotter("(Consumption + Profits) / GDP", "%");
				consumptionPlusProftsPlot.addSeries("Consumption + Profits) / GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumptionPlusFirmAndBankProfitsToGDPpercent));
//				consumptionPlusProftsPlot.addSeries("Change in Inventories + Consumption + Profits) / GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ChangeInInventoriesPlusConsumptionPlusFirmAndBankProfitsToGDPpercent));
				updateChartSet.add(consumptionPlusProftsPlot);			//Add to set to be updated in buildSchedule method
				accountingPlots.add(consumptionPlusProftsPlot);
				
				tabSet.add(createScrollPaneFromPlots(accountingPlots, "National Accounts", accountingPlots.size()));
			}
			
			if(gdpConsumptionInvestment) {
			    //Create chart containing time-series' of log GDP, log consumption and log total investment
				TimeSeriesSimulationPlotter logOutputConsumptionInvestment = new TimeSeriesSimulationPlotter("GDP, Consumption, Investment", "Log");
				logOutputConsumptionInvestment.addSeries("GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogGDP));
				logOutputConsumptionInvestment.addSeries("Consumption", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogConsumption));
				logOutputConsumptionInvestment.addSeries("Investment", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalInvestment));
//				logOutputConsumptionInvestment.addSeries("Aggregate Demand", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogAggregateDemand));
				logOutputConsumptionInvestment.setName("GDP, Consumption, Investment");
				updateChartSet.add(logOutputConsumptionInvestment);			//Add to set to be updated in buildSchedule method
			    tabSet.add(logOutputConsumptionInvestment);
	//			GuiUtils.addWindow(logOutputConsumptionInvestment, 0, 0, 300, 250);
			}
		    
			if(expenditureComponentsToGDP) {
			    Set<JInternalFrame> outputComponentsPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter consumptionToGDPplot = new TimeSeriesSimulationPlotter("Consumption to GDP ratio", "%"); 
				consumptionToGDPplot.addSeries("Consumption to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumptionToGDPpercent)); 
				updateChartSet.add(consumptionToGDPplot);			//Add to set to be updated in buildSchedule method
				outputComponentsPlots.add(consumptionToGDPplot); 							    			    			    
			    TimeSeriesSimulationPlotter investmentToGDPplot = new TimeSeriesSimulationPlotter("Investment to GDP ratio", "%");
				investmentToGDPplot.addSeries("Investment to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.InvestmentToGDPpercent)); 
				updateChartSet.add(investmentToGDPplot);			//Add to set to be updated in buildSchedule method
				outputComponentsPlots.add(investmentToGDPplot);			
				TimeSeriesSimulationPlotter inventoriesToGDP = new TimeSeriesSimulationPlotter("Change in Inventories To GDP ratio", "%"); 
				inventoriesToGDP.addSeries("Consumption Good Firms", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.DiffTotalInventoriesToGDPpercent));
				outputComponentsPlots.add(inventoriesToGDP);
				updateChartSet.add(inventoriesToGDP);			//Add to set to be updated in buildSchedule method
				tabSet.add(createScrollPaneFromPlots(outputComponentsPlots, "Expenditure Components", outputComponentsPlots.size()));
			}		    
		    
		    
		    if(!dosiComparison){
				
			    if(creditActivity) {
				    //Create chart containing time-series' of credit supply and demand
					TimeSeriesSimulationPlotter bankMarket = new TimeSeriesSimulationPlotter("Credit Activity", "Log");
					bankMarket.addSeries("Credit Supply", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.LogCreditSupply));
					bankMarket.addSeries("Credit Demand", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogCreditDemand));
					bankMarket.setName("Credit activity");
					updateChartSet.add(bankMarket);			//Add to set to be updated in buildSchedule method
				    tabSet.add(bankMarket);
	//				GuiUtils.addWindow(bankMarket, 300, 0, 300, 250);
			    }
			    
			    if(creditRationing) {
					TimeSeriesSimulationPlotter creditRationing = new TimeSeriesSimulationPlotter("Credit rationing", "%");
					creditRationing.addSeries("% rationed", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.CreditRationingRate_cFirms));
					creditRationing.setName("Credit rationing");
					updateChartSet.add(creditRationing);			//Add to set to be updated in buildSchedule method
				    tabSet.add(creditRationing);
	//				GuiUtils.addWindow(creditRationing, 300, 0, 300, 250);
			    }
			    
			    if(debt) {
				    //Two charts under one tab, because data in charts have different scales
	//			    Set<JInternalFrame> debtPlots = new LinkedHashSet<JInternalFrame>();
					TimeSeriesSimulationPlotter debt = new TimeSeriesSimulationPlotter("Debt", "Log");
					debt.addSeries("Debt", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.LogDebt));
					debt.addSeries("cFirm sector Bad debt", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogBadDebt));
	//				debt.addSeries("Bank Bad debt (log)", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.LogBadDebt)); 
					debt.setName("Debt");
					updateChartSet.add(debt);			//Add to set to be updated in buildSchedule method
	//				debtPlots.add(debt);
				    tabSet.add(debt);
			    }
			    
////				GuiUtils.addWindow(debt, 600, 0, 300, 250);							    			    			    
//			    TimeSeriesSimulationPlotter badDebt = new TimeSeriesSimulationPlotter("Bad Debt", "");
//				badDebt.addSeries("Bad Debt (cFirm sector)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogBadDebt));
//				badDebt.addSeries("Bad debt (bank)", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.LogBadDebt));
//				updateChartSet.add(badDebt);			//Add to set to be updated in buildSchedule method
//				debtPlots.add(badDebt);
//				//The observer's createScrollPaneFromPlots() method arranges layout for multiple charts side-by-side
//				tabSet.add(createScrollPaneFromPlots(debtPlots, "Debt", debtPlots.size()));		//Add to set of charts to feature in tabbed pane


			    
			    
			    if(capitalStock) {
					TimeSeriesSimulationPlotter capital = new TimeSeriesSimulationPlotter("Capital Stock of Consumption Firms", "Log");
	//				capital.addSeries("", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Empty));					//XXX: Ross: Not sure what Hugo was hoping to achieve here
					capital.addSeries("Actual Stock", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogCapitalStock_cFirms));
					capital.addSeries("Desired Stock", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredCapitalStock_cFirms));
					capital.addSeries("Top Limit of Stock", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogCapitalStockTopLimit));
					capital.setName("Capital Stock");
					updateChartSet.add(capital);			//Add to set to be updated in buildSchedule method
				    tabSet.add(capital);
	//				GuiUtils.addWindow(capital, 600, 0, 300, 250);
			    }
			    
			    
			    if(costPriceMarkup) {
					TimeSeriesSimulationPlotter costPriceMarkUp = new TimeSeriesSimulationPlotter("Cost, Price & Mark-up Averages", "");		//was Labor & mu	//XXX: Why was this named 'Labor'?
	//				laborMarket.addSeries("", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Empty));				//XXX: Ross: Not sure what Hugo was hoping to achieve here
					costPriceMarkUp.addSeries("Avg Mark-up", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TotalMarkUp_cFirms));
					costPriceMarkUp.addSeries("CPI", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.CPI));
					costPriceMarkUp.addSeries("Avg Cost", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.MeanCostWeightedByMarketShare_cFirms));
					costPriceMarkUp.setName("Cost, Price & Mark-up");
					updateChartSet.add(costPriceMarkUp);			//Add to set to be updated in buildSchedule method
				    tabSet.add(costPriceMarkUp);					//Tab will be created for this chart
	//				GuiUtils.addWindow(laborMarket, 900, 0, 300, 250);
		    	}
		    
//				TimeSeriesSimulationPlotter capitalMarket = new TimeSeriesSimulationPlotter("Expansionary Investment", "Log");			//Was Capital Market
//				capitalMarket.addSeries("Actual (log).", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalExpansionaryInvestment_cFirms));
//				// capitalMarket.addSeries("Sub. inv.", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.InvestmentSub));
//				capitalMarket.addSeries("Desired (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredExpansionaryInvestmentTotal_cFirms));
//				capitalMarket.addSeries("Desired star (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredExpansionaryInvestmentTotalStar_cFirms));	//XXX: What does star mean???
//				capitalMarket.setName("Investment (expansionary)");
//				updateChartSet.add(capitalMarket);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(capitalMarket);					//Tab will be created for this chart
////				GuiUtils.addWindow(capitalMarket, 0, 250, 300, 250);

		    }
			    
		    if(investment) {
			    Set<JInternalFrame> investmentPlots = new LinkedHashSet<JInternalFrame>();
				TimeSeriesSimulationPlotter expansionaryInvestmentPlot = new TimeSeriesSimulationPlotter("Expansionary Investment", "Log");			//Was Capital Market
				expansionaryInvestmentPlot.addSeries("Actual", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalExpansionaryInvestment_cFirms));
				expansionaryInvestmentPlot.addSeries("Desired", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredExpansionaryInvestmentTotal_cFirms));
				expansionaryInvestmentPlot.addSeries("Desired Star", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredExpansionaryInvestmentTotalStar_cFirms));	//XXX: What does star mean???
	//				expansionaryInvestmentPlot.setName("Investment");
				updateChartSet.add(expansionaryInvestmentPlot);			//Add to set to be updated in buildSchedule method
			    investmentPlots.add(expansionaryInvestmentPlot);
			    TimeSeriesSimulationPlotter substitionaryInvestmentPlot = new TimeSeriesSimulationPlotter("Substitionary Investment", "Log");	
				substitionaryInvestmentPlot.addSeries("Substitionary investment", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalSubsitionaryInvestment_cFirms));
				updateChartSet.add(substitionaryInvestmentPlot);			//Add to set to be updated in buildSchedule method
				investmentPlots.add(substitionaryInvestmentPlot);
				tabSet.add(createScrollPaneFromPlots(investmentPlots, "Investment", investmentPlots.size()));
		    }

			if(!dosiComparison) {
				
				if(investmentToGDP) {
					//As a proportion of GDP
					Set<JInternalFrame> investmentToGDPPlots = new LinkedHashSet<JInternalFrame>();
					TimeSeriesSimulationPlotter expansionaryInvestmentToGDPplot = new TimeSeriesSimulationPlotter("Expansionary Investment to GDP", "%");
					expansionaryInvestmentToGDPplot.addSeries("Actual inv to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ActualExpansionaryInvestmentToGDPpercent_cFirms));
					expansionaryInvestmentToGDPplot.addSeries("Desired to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.DesiredExpansionaryInvestmentToGDPpercent_cFirms));
					expansionaryInvestmentToGDPplot.addSeries("Desired star to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.DesiredExpansionaryInvestmentStarToGDPpercent_cFirms));	//XXX: What does star mean???
	//				expansionaryInvestmentPlot.setName("Investment");
					updateChartSet.add(expansionaryInvestmentToGDPplot);			//Add to set to be updated in buildSchedule method
				    investmentToGDPPlots.add(expansionaryInvestmentToGDPplot);
				    TimeSeriesSimulationPlotter substitionaryInvestmentToGDPplot = new TimeSeriesSimulationPlotter("Substitionary Investment to GDP", "%");
					substitionaryInvestmentToGDPplot.addSeries("Substitionary investment to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ActualSubsitionaryInvestmentToGDPpercent_cFirms));
					updateChartSet.add(substitionaryInvestmentToGDPplot);			//Add to set to be updated in buildSchedule method
					investmentToGDPPlots.add(substitionaryInvestmentToGDPplot);
					tabSet.add(createScrollPaneFromPlots(investmentToGDPPlots, "Investment to GDP", investmentToGDPPlots.size()));
				}
				
			    if(consumptionGoodsMarket) {
					TimeSeriesSimulationPlotter goodMarket = new TimeSeriesSimulationPlotter("Goods Market (Consumption Firms)", "Log");
					goodMarket.addSeries("Production", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProduction_cFirms));
					goodMarket.addSeries("Consumption", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogConsumption));
					goodMarket.addSeries("Total Desired Production", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalDesiredProduction_cFirms));
					goodMarket.setName("Consumption Goods Market");
					updateChartSet.add(goodMarket);			//Add to set to be updated in buildSchedule method
				    tabSet.add(goodMarket);					//Tab will be created for this chart
	//				GuiUtils.addWindow(goodMarket, 300, 250, 300, 250);
			    }
			    
			    if(productivity) {
				    //XXX: Ross - I don't understand the labelling of these time-series!!!  Need to check
				    Set<JInternalFrame> productivityPlots = new LinkedHashSet<JInternalFrame>();
				    TimeSeriesSimulationPlotter machineProd = new TimeSeriesSimulationPlotter("Machine Productivity", "Log"); 
					// rd.addSeries("R&D expenditures", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.RdExpenditures));
	//				rdAll.addSeries("Avg Firm Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.MeanProductivityWeightedByMarketShare_kFirms));	//was labelled 'Mean K'.
					machineProd.addSeries("Avg Machine Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogMeanMachineProductivityWeightedByMarketShare));	//was labelled 'Mean Machine'. 
					machineProd.addSeries("Top Machine Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTopMachineProductivity));		//was labelled 'Top Prod Labor', which is confusing as it appears to refer to machine productivity.
	//				rdAll.addSeries("Top Machine Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TopProdMachine));	//was labelled 'Top Prod Capital'.
					updateChartSet.add(machineProd);			//Add to set to be updated in buildSchedule method
					productivityPlots.add(machineProd); 							    			    			    
				    TimeSeriesSimulationPlotter capitalProd = new TimeSeriesSimulationPlotter("Capital Goods Firm Productivity", "Log");
					capitalProd.addSeries("Avg Firm Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogMeanProductivityWeightedByMarketShare_kFirms));	//was labelled 'Mean K'.
					capitalProd.addSeries("Top Firm Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTopProductivity_kFirms));	//was labelled 'Top Prod Capital'. 
					updateChartSet.add(capitalProd);			//Add to set to be updated in buildSchedule method
					productivityPlots.add(capitalProd);
					tabSet.add(createScrollPaneFromPlots(productivityPlots, "Productivity", productivityPlots.size()));
			    }
				
			    if(profit) {
					Set<JInternalFrame> profitPlots = new LinkedHashSet<JInternalFrame>();
					TimeSeriesSimulationPlotter profit = new TimeSeriesSimulationPlotter("Profit: Consumption & Capital Goods Firms", "Log"); 
					profit.addSeries("Consumption Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProfit_cFirms));
					profit.addSeries("Capital Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProfit_kFirms));
					updateChartSet.add(profit);			//Add to set to be updated in buildSchedule method
					profitPlots.add(profit);				
					TimeSeriesSimulationPlotter capitalProfit = new TimeSeriesSimulationPlotter("Capital Goods Firm Profit", ""); 
					capitalProfit.addSeries("Capital Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Profit_kFirms));
					updateChartSet.add(capitalProfit);			//Add to set to be updated in buildSchedule method								
					profitPlots.add(capitalProfit);
				    tabSet.add(createScrollPaneFromPlots(profitPlots, "Profit", profitPlots.size()));					//Tab will be created for this chart
			    }

			    if(profitToGDP) {
					Set<JInternalFrame> profitToGDPplots = new LinkedHashSet<JInternalFrame>();
					TimeSeriesSimulationPlotter profitToGDP = new TimeSeriesSimulationPlotter("Total Profit to GDP: Consumption & Capital Goods Firms", "%"); 
					profitToGDP.addSeries("Consumption Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProfitToGDPpercent_cFirms));
					profitToGDP.addSeries("Capital Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProfitToGDPpercent_kFirms));
					updateChartSet.add(profitToGDP);			//Add to set to be updated in buildSchedule method
					profitToGDPplots.add(profitToGDP);				
					TimeSeriesSimulationPlotter capitalProfitToGDP = new TimeSeriesSimulationPlotter("Capital Goods Firm Profit to GDP", "%"); 
					capitalProfitToGDP.addSeries("Capital Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProfitToGDPpercent_kFirms));
					updateChartSet.add(capitalProfitToGDP);			//Add to set to be updated in buildSchedule method								
					profitToGDPplots.add(capitalProfitToGDP);
				    tabSet.add(createScrollPaneFromPlots(profitToGDPplots, "Profit to GDP", profitToGDPplots.size()));
			    }			    
			    
			    if(machines) {
					TimeSeriesSimulationPlotter clientAndAge = new TimeSeriesSimulationPlotter("Machines: Clients & Age", "#"); 
					clientAndAge.addSeries("Max Clients per Capital Firm", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.MaxClientsPerFirm_kFirms));
					clientAndAge.addSeries("Avg Age of Machines owned by Consumption Firms (time-steps)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.AverageAgeMachine_cFirms));
					clientAndAge.setName("Machines: Clients & Age");
					updateChartSet.add(clientAndAge);			//Add to set to be updated in buildSchedule method
				    tabSet.add(clientAndAge);					//Tab will be created for this chart
	//				GuiUtils.addWindow(clientAndAge, 0, 500, 300, 250);
			    }
			    
//				TimeSeriesSimulationPlotter inventories = new TimeSeriesSimulationPlotter("Inventories", ""); 
//				inventories.addSeries("Total Consumption Sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TotalInventories));
//				inventories.setName("Inventories");
//				updateChartSet.add(inventories);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(inventories);					//Tab will be created for this chart
////				GuiUtils.addWindow(inventories, 300, 500, 300, 250);

//				TimeSeriesSimulationPlotter inventoriesToGDP = new TimeSeriesSimulationPlotter("Change in Inventories To GDP ratio", "%"); 
//				inventoriesToGDP.addSeries("Consumption Good Firms", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.DiffTotalInventoriesToGDPpercent));
//				inventoriesToGDP.setName("Inventories To GDP");
//				updateChartSet.add(inventoriesToGDP);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(inventoriesToGDP);					//Tab will be created for this chart

			}
			
			if(governmentRatios) {
				TimeSeriesSimulationPlotter government = new TimeSeriesSimulationPlotter("Government Ratios", "%");
				government.addSeries("Balance (revenues - spending) to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovBalanceToGDPpercent));
				government.addSeries("Stock (accumulated Balance over time) to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovStockToGDPpercent));
				government.addSeries("Spending to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovSpendingToGDPpercent));
				government.addSeries("Revenues to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovRevenuesToGDPpercent));
				government.setName("Government Ratios");
				updateChartSet.add(government);			//Add to set to be updated in buildSchedule method
			    tabSet.add(government);					//Tab will be created for this chart
	//				GuiUtils.addWindow(government, 600, 500, 300, 250);
			}
			
			
		    if(!dosiComparison) {

		    	if(laborMarket) {
				    Set<JInternalFrame> laborMarketPlots = new LinkedHashSet<JInternalFrame>();			    
				    TimeSeriesSimulationPlotter laborMarketWage = new TimeSeriesSimulationPlotter("Market Wage", "Log");
					laborMarketWage.addSeries("Market Wage", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogWage));
					updateChartSet.add(laborMarketWage);			//Add to set to be updated in buildSchedule method
					laborMarketPlots.add(laborMarketWage);
				    TimeSeriesSimulationPlotter laborMarketUnemployment = new TimeSeriesSimulationPlotter("Unemployment Rate", "%");
					laborMarketUnemployment.addSeries("Unemployment Rate", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.UnemploymentRatePercent));
					updateChartSet.add(laborMarketUnemployment);			//Add to set to be updated in buildSchedule method
					laborMarketPlots.add(laborMarketUnemployment);
					tabSet.add(createScrollPaneFromPlots(laborMarketPlots, "Labor Market", laborMarketPlots.size()));
		    	}
			    
//				TimeSeriesSimulationPlotter exit = new TimeSeriesSimulationPlotter("Firm Exits", "# per time-step");
//				exit.addSeries("Capital Firms", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.Exit_kFirms));
//				exit.addSeries("Consumption Firms", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.Exit_cFirms));
//				exit.addSeries("Consumption Firms: Liquidity Issue", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.ExitLiquidityIssue_cFirms));
//				exit.addSeries("Consumption Firms: Market Share Issue", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.ExitMarketShareIssue_cFirms));
//				exit.addSeries("Consumption Firms: Liquidity & Market Share Issues", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.ExitAssetMarket_cFirms));
//				exit.setName("Firm Exits");
//				updateChartSet.add(exit);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(exit);					//Tab will be created for this chart
////				GuiUtils.addWindow(exit, 900, 500, 300, 250);

				if(firmExitRates) {
					Set<JInternalFrame> exitPlots = new LinkedHashSet<JInternalFrame>();
					TimeSeriesSimulationPlotter cFirmsExitPlot = new TimeSeriesSimulationPlotter("Consumption Good Firms Exit Rate", "%");
					cFirmsExitPlot.addSeries("Total", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ExitPercent_cFirms));
					cFirmsExitPlot.addSeries("Liquidity Issue", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ExitPercentLiquidityIssue_cFirms));
					cFirmsExitPlot.addSeries("Market Share Issue", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ExitPercentMarketShareIssue_cFirms));
					cFirmsExitPlot.addSeries("Liquidity & Market Share Issues", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ExitPercentAssetMarket_cFirms));
	//				cFirmsExitPlot.setName("Firm Exits");
					updateChartSet.add(cFirmsExitPlot);			//Add to set to be updated in buildSchedule method
					exitPlots.add(cFirmsExitPlot);
	//			    tabSet.add(cFirmsExitPlot);					//Tab will be created for this chart
	//				GuiUtils.addWindow(exit, 900, 500, 300, 250);
					TimeSeriesSimulationPlotter kFirmsExitPlot = new TimeSeriesSimulationPlotter("Capital Good Firms Exit Rate", "%");
					kFirmsExitPlot.addSeries("Total", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ExitPercent_kFirms));
					updateChartSet.add(kFirmsExitPlot);			//Add to set to be updated in buildSchedule method
					exitPlots.add(kFirmsExitPlot);
					tabSet.add(createScrollPaneFromPlots(exitPlots, "Firm Exit Rates", exitPlots.size()));
		    	}
				
				if(assets) {
					TimeSeriesSimulationPlotter asset = new TimeSeriesSimulationPlotter("Assets", "Log");
					asset.addSeries("Capital Firms: Total Liquid Assets", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalLiquidAssets_kFirms));
					asset.setName("Assets");
					updateChartSet.add(asset);			//Add to set to be updated in buildSchedule method
				    tabSet.add(asset);					//Tab will be created for this chart
	//				GuiUtils.addWindow(asset, 500, 500, 300, 250); 
				}
				
//				TimeSeriesSimulationPlotter flag = new TimeSeriesSimulationPlotter("Flag", " # ");
//				// flag.addSeries("U Rate", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.UnemploymentRate));
//				flag.addSeries("Kfirms neg. asset", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.Flag));
//				flag.setName("Flag");
//				updateChartSet.add(flag);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(flag);					//Tab will be created for this chart
////				GuiUtils.addWindow(flag, 300, 300, 300, 250); 
			    
			    
//			    //-------------------------------------------------------------------------------------------------------
//			    //
//		    	//	BUILD A TABBED PANE HOLDING ALL THE CHARTS THAT ONLY UPDATE AT EACH TIME-STEP
//			    //
//		    	//-------------------------------------------------------------------------------------------------------
//			    
//		        //Create tabbed pane to hold all the charts and add to the JAS-mine GUI window
//		    	JInternalFrame chartsFrame = new JInternalFrame("Charts");
//				JTabbedPane tabbedPane = new JTabbedPane();
//				chartsFrame.add(tabbedPane);
//				
//				for(JComponent plot: tabSet) {
//					tabbedPane.addTab(plot.getName(), plot);
//				}
//				tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
//		    	chartsFrame.setResizable(true);
//		    	chartsFrame.setMaximizable(true);
//				GuiUtils.addWindow(chartsFrame, 300, 0, 1560, 660);
//			    
				
			} else {
				
																
//				TimeSeriesSimulationPlotter logInvestments = new TimeSeriesSimulationPlotter("Log Investments", "Log");
//				logInvestments.addSeries("Log Inv. Sub.", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalSubsitionaryInvestment_cFirms));
//				logInvestments.addSeries("Log Inv. Exp.", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalExpansionaryInvestment_cFirms));
//				logInvestments.setName("Investment");
//				updateChartSet.add(logInvestments);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(logInvestments);					//Tab will be created for this chart
////				GuiUtils.addWindow(logInvestments, 0, 400, 500, 200);

				if(meanProductivity) {
					Set<JInternalFrame> productivityPlots = new LinkedHashSet<JInternalFrame>();
					TimeSeriesSimulationPlotter logProductivityMomentsC = new TimeSeriesSimulationPlotter("cFirms: Mean Log Productivity", "Log");
					logProductivityMomentsC.addSeries("Mean Log Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.meanLogProductivity_cFirms));
	//				logProductivityMomentsC.setName("Moments prod. C-firms");
					updateChartSet.add(logProductivityMomentsC);			//Add to set to be updated in buildSchedule method
					productivityPlots.add(logProductivityMomentsC);
	//			    tabSet.add(logProductivityMomentsC);					//Tab will be created for this chart
	//				GuiUtils.addWindow(logProductivityMomentsC, 500, 0, 400, 200);				
					TimeSeriesSimulationPlotter logProductivityMomentsK = new TimeSeriesSimulationPlotter("kFirms: Mean Log Productivity", "Log");
					logProductivityMomentsK.addSeries("Mean Log Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.meanLogProductivity_kFirms));
	//				logProductivityMomentsK.setName("Moments prod. K-firms");
					updateChartSet.add(logProductivityMomentsK);			//Add to set to be updated in buildSchedule method
					productivityPlots.add(logProductivityMomentsK);
	//			    tabSet.add(logProductivityMomentsK);					//Tab will be created for this chart
	//				GuiUtils.addWindow(logProductivityMomentsK, 500, 200, 400, 200);
					tabSet.add(createScrollPaneFromPlots(productivityPlots, "Productivity", productivityPlots.size()));
				}
				
//				TimeSeriesSimulationPlotter investmentLumpinessLow = new TimeSeriesSimulationPlotter("Inv. lumpiness", "%");
//				investmentLumpinessLow.addSeries("I/K < 0.02", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LowInv));
//				investmentLumpinessLow.setName("Inv. lumpiness (Low)");
//				updateChartSet.add(investmentLumpinessLow);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(investmentLumpinessLow);					//Tab will be created for this chart
////				GuiUtils.addWindow(investmentLumpinessLow, 900, 0, 400, 200);
//				
//				TimeSeriesSimulationPlotter investmentLumpinessHigh = new TimeSeriesSimulationPlotter("Inv. lumpiness", "%");
//				investmentLumpinessHigh.addSeries("I/K > 0.35", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.HighInv));
//				investmentLumpinessHigh.setName("Inv. lumpiness (High)");
//				updateChartSet.add(investmentLumpinessHigh);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(investmentLumpinessHigh);					//Tab will be created for this chart
////				GuiUtils.addWindow(investmentLumpinessHigh, 900, 200, 400, 200);
				
			}

		    if(inflation) {
			    TimeSeriesSimulationPlotter inflation = new TimeSeriesSimulationPlotter("Inflation", "%");
				inflation.addSeries("CPI inflation", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumerInflationPercent_cFirms));
				inflation.addSeries("PPI inflation", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProducerInflationPercent_kFirms));
				inflation.setName("Inflation");
				updateChartSet.add(inflation);			//Add to set to be updated in buildSchedule method
			    tabSet.add(inflation);					//Tab will be created for this chart
	//				GuiUtils.addWindow(inflations, 500, 400, 400, 200);
		    }
		    
//				TimeSeriesSimulationPlotter government = new TimeSeriesSimulationPlotter("Government", "% of GDP");
//				government.addSeries("Balance to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovBalanceToGDPpercent));
//				government.addSeries("Stock to GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovStockToGDPpercent));
//				government.setName("Government");
//				updateChartSet.add(government);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(government);					//Tab will be created for this chart
////				GuiUtils.addWindow(government, 900, 400, 400, 200);
			    			
			
			
			
		    //-------------------------------------------------------------------------------------------------------
		    //
	    	//	BUILD A TABBED PANE HOLDING ALL THE CHARTS THAT ONLY UPDATE AT EACH TIME-STEP
		    //
	    	//-------------------------------------------------------------------------------------------------------
		    
	        //Create tabbed pane to hold all the charts and add to the JAS-mine GUI window
	    	JInternalFrame chartsFrame = new JInternalFrame("Charts");
			JTabbedPane tabbedPane = new JTabbedPane();
			chartsFrame.add(tabbedPane);
			
			for(JComponent plot: tabSet) {
				tabbedPane.addTab(plot.getName(), plot);
			}
			tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	    	chartsFrame.setResizable(true);
	    	chartsFrame.setMaximizable(true);
			GuiUtils.addWindow(chartsFrame, 300, 0, 1560, 660);
			    			 
		}
		
		log.debug("Object created");
		
	}

	@Override
	public void buildSchedule() {
//		EventGroup eventGroup = new EventGroup();

		if(charts){
			if(!dosiComparison){
				
				EventGroup observerEventGroup = new EventGroup();
				for(JInternalFrame plot: updateChartSet) {
					observerEventGroup.addEvent(plot, CommonEventType.Update);
				}
				getEngine().getEventQueue().scheduleRepeat(observerEventGroup, 0, Parameters.OBSERVER_ORDERING, chartUpdatePeriod);
				
//				eventGroup.addEvent(logOutputConsumptionInvestment, CommonEventType.Update);
//				eventGroup.addEvent(bankMarket, CommonEventType.Update);
//				eventGroup.addEvent(debt, CommonEventType.Update);
//				eventGroup.addEvent(laborMarket, CommonEventType.Update);
//				eventGroup.addEvent(capitalMarket, CommonEventType.Update);
//				eventGroup.addEvent(goodMarket, CommonEventType.Update);
//				eventGroup.addEvent(rd, CommonEventType.Update);
//				eventGroup.addEvent(profit, CommonEventType.Update);
//				eventGroup.addEvent(clientAndAge, CommonEventType.Update);
//				eventGroup.addEvent(profit, CommonEventType.Update);
//				eventGroup.addEvent(inventories, CommonEventType.Update);
//				eventGroup.addEvent(government, CommonEventType.Update);
//				eventGroup.addEvent(exit, CommonEventType.Update);
//				eventGroup.addEvent(asset, CommonEventType.Update);
//				// eventGroup.addEvent(creditRationing, CommonEventType.Update);
//				eventGroup.addEvent(capital, CommonEventType.Update);
//				eventGroup.addEvent(flag, CommonEventType.Update);
				
			} else {
				
				EventGroup observerEventGroup = new EventGroup();
				for(JInternalFrame plot: updateChartSet) {
					observerEventGroup.addEvent(plot, CommonEventType.Update);
				}
				getEngine().getEventQueue().scheduleRepeat(observerEventGroup, 0, Parameters.OBSERVER_ORDERING, chartUpdatePeriod);
				
//				eventGroup.addEvent(logOutputConsumptionInvestment, CommonEventType.Update);
//				eventGroup.addEvent(logInvestments, CommonEventType.Update);
//				eventGroup.addEvent(logProductivityMomentsC, CommonEventType.Update);
//				eventGroup.addEvent(logProductivityMomentsK, CommonEventType.Update);
//				eventGroup.addEvent(investmentLumpinessLow, CommonEventType.Update);
//				eventGroup.addEvent(investmentLumpinessHigh, CommonEventType.Update);
//				eventGroup.addEvent(inflations, CommonEventType.Update);
//				eventGroup.addEvent(government, CommonEventType.Update);
//				// eventGroup.addEvent(flag, CommonEventType.Update);
				
			}
		}
		

	}
	
	// ---------------------------------------------------------------------
	// EventListener
	// ---------------------------------------------------------------------

	public enum Processes {
	}
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		}
	}

	// ---------------------------------------------------------------------
	// Own methods
	// ---------------------------------------------------------------------

	/**
	 * Method to re-arrange JInternalFrames such as JFreeChart plots into 
	 * a single JInternalFrame (e.g. to use in a TabbedPane of plots).
	 * 
	 * @param internalFrames - a set of JInternalFrames such as JFreeChart plots 
	 * @param name - the name of the JScrollPane returned
	 * @param columns - the number of columns with which the JInternalFrames will be laid out 
	 * @return A JScrollPane laying of a set of JInternalFrames 
	 */
	private JScrollPane createScrollPaneFromPlots(Set<JInternalFrame> internalFrames, String name, int columns) {		
		
		String layoutConstraints = "wrap " + columns;
		MigLayout layout = new MigLayout(layoutConstraints, "fill, grow", "fill, grow");
		JPanel panel = new JPanel(layout);

		for(JInternalFrame internalFrame: internalFrames) {
			internalFrame.setVisible(true);
			internalFrame.setResizable(false);	//The components (charts) are not able to expand beyond their assigned row/column, so the only way to resize is to resize the whole pane. 
			panel.add(internalFrame);
		}		
		JScrollPane frame = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frame.setName(name);
		return frame;
	}
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public Double getChartUpdatePeriod() {
		return chartUpdatePeriod;
	}

	public void setChartUpdatePeriod(Double chartUpdatePeriod) {
		this.chartUpdatePeriod = chartUpdatePeriod;
	}

	public Boolean getCharts() {
		return charts;
	}

	public void setCharts(Boolean charts) {
		this.charts = charts;
	}

	public Boolean getDosiComparison() {
		return dosiComparison;
	}

	public void setDosiComparison(Boolean dosiComparison) {
		this.dosiComparison = dosiComparison;
	}

	public boolean isConsumptionComponents() {
		return consumptionComponents;
	}

	public void setConsumptionComponents(boolean consumptionComponents) {
		this.consumptionComponents = consumptionComponents;
	}

	public boolean isGdpComponents() {
		return gdpComponents;
	}

	public void setGdpComponents(boolean gdpComponents) {
		this.gdpComponents = gdpComponents;
	}

	public boolean isNominalGDPcomponents() {
		return nominalGDPcomponents;
	}

	public void setNominalGDPcomponents(boolean nominalGDPcomponents) {
		this.nominalGDPcomponents = nominalGDPcomponents;
	}

	public boolean isNationalAccounts() {
		return nationalAccounts;
	}

	public void setNationalAccounts(boolean nationalAccounts) {
		this.nationalAccounts = nationalAccounts;
	}

	public boolean isGdpConsumptionInvestment() {
		return gdpConsumptionInvestment;
	}

	public void setGdpConsumptionInvestment(boolean gdpConsumptionInvestment) {
		this.gdpConsumptionInvestment = gdpConsumptionInvestment;
	}

	public boolean isExpenditureComponentsToGDP() {
		return expenditureComponentsToGDP;
	}

	public void setExpenditureComponentsToGDP(boolean expenditureComponentsToGDP) {
		this.expenditureComponentsToGDP = expenditureComponentsToGDP;
	}

	public boolean isCreditActivity() {
		return creditActivity;
	}

	public void setCreditActivity(boolean creditActivity) {
		this.creditActivity = creditActivity;
	}

	public boolean isCreditRationing() {
		return creditRationing;
	}

	public void setCreditRationing(boolean creditRationing) {
		this.creditRationing = creditRationing;
	}

	public boolean isDebt() {
		return debt;
	}

	public void setDebt(boolean debt) {
		this.debt = debt;
	}

	public boolean isCapitalStock() {
		return capitalStock;
	}

	public void setCapitalStock(boolean capitalStock) {
		this.capitalStock = capitalStock;
	}

	public boolean isCostPriceMarkup() {
		return costPriceMarkup;
	}

	public void setCostPriceMarkup(boolean costPriceMarkup) {
		this.costPriceMarkup = costPriceMarkup;
	}

	public boolean isInvestment() {
		return investment;
	}

	public void setInvestment(boolean investment) {
		this.investment = investment;
	}

	public boolean isInvestmentToGDP() {
		return investmentToGDP;
	}

	public void setInvestmentToGDP(boolean investmentToGDP) {
		this.investmentToGDP = investmentToGDP;
	}

	public boolean isConsumptionGoodsMarket() {
		return consumptionGoodsMarket;
	}

	public void setConsumptionGoodsMarket(boolean consumptionGoodsMarket) {
		this.consumptionGoodsMarket = consumptionGoodsMarket;
	}

	public boolean isProductivity() {
		return productivity;
	}

	public void setProductivity(boolean productivity) {
		this.productivity = productivity;
	}

	public boolean isProfit() {
		return profit;
	}

	public void setProfit(boolean profit) {
		this.profit = profit;
	}

	public boolean isProfitToGDP() {
		return profitToGDP;
	}

	public void setProfitToGDP(boolean profitToGDP) {
		this.profitToGDP = profitToGDP;
	}

	public boolean isMachines() {
		return machines;
	}

	public void setMachines(boolean machines) {
		this.machines = machines;
	}

	public boolean isGovernmentRatios() {
		return governmentRatios;
	}

	public void setGovernmentRatios(boolean governmentRatios) {
		this.governmentRatios = governmentRatios;
	}

	public boolean isLaborMarket() {
		return laborMarket;
	}

	public void setLaborMarket(boolean laborMarket) {
		this.laborMarket = laborMarket;
	}

	public boolean isFirmExitRates() {
		return firmExitRates;
	}

	public void setFirmExitRates(boolean firmExitRates) {
		this.firmExitRates = firmExitRates;
	}

	public boolean isAssets() {
		return assets;
	}

	public void setAssets(boolean assets) {
		this.assets = assets;
	}

	public boolean isMeanProductivity() {
		return meanProductivity;
	}

	public void setMeanProductivity(boolean meanProductivity) {
		this.meanProductivity = meanProductivity;
	}

	public boolean isInflation() {
		return inflation;
	}

	public void setInflation(boolean inflation) {
		this.inflation = inflation;
	}

	public boolean isAcountingIdentities() {
		return acountingIdentities;
	}

	public void setAcountingIdentities(boolean acountingIdentities) {
		this.acountingIdentities = acountingIdentities;
	}

}
