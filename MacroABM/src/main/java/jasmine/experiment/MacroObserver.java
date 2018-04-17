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
	
	//For Dosi comparison
	private Set<JInternalFrame> updateChartSetDosiCompare;		//Collection of graphical objects to be updated on schedule
	private Set<JComponent> tabSetDosiCompare;					//Collection of graphical objects to be displayed in a tabbed Frame

	

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
						
			if(!dosiComparison){
				
				updateChartSet = new LinkedHashSet<JInternalFrame>();	//Set of all charts needed to be scheduled for updating
				tabSet = new LinkedHashSet<JComponent>();		//Set of all JInternalFrames each having a tab.  Each tab frame will potentially contain more than one chart each.

		        //Create chart containing time-series' of log GDP, log consumption and log total investment
				TimeSeriesSimulationPlotter logOutputConsumptionInvestment = new TimeSeriesSimulationPlotter("Aggregate Time Series", "Log");
				logOutputConsumptionInvestment.addSeries("Log GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogGDP));
				logOutputConsumptionInvestment.addSeries("Log Consumption", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumptionLog));
				logOutputConsumptionInvestment.addSeries("Log Investment", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalInvestment));
				logOutputConsumptionInvestment.setName("Time-series");
				updateChartSet.add(logOutputConsumptionInvestment);			//Add to set to be updated in buildSchedule method
			    tabSet.add(logOutputConsumptionInvestment);
//				GuiUtils.addWindow(logOutputConsumptionInvestment, 0, 0, 300, 250);
				
			    //Create chart containing time-series' of credit supply and demand
				TimeSeriesSimulationPlotter bankMarket = new TimeSeriesSimulationPlotter("Credit Activity", "Log");
				bankMarket.addSeries("Credit Supply (log)", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.LogCreditSupply));
				bankMarket.addSeries("Credit Demand (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogCreditDemand));
				bankMarket.setName("Credit activity");
				updateChartSet.add(bankMarket);			//Add to set to be updated in buildSchedule method
			    tabSet.add(bankMarket);
//				GuiUtils.addWindow(bankMarket, 300, 0, 300, 250);
				
				TimeSeriesSimulationPlotter creditRationing = new TimeSeriesSimulationPlotter("Credit rationing", "%");
				creditRationing.addSeries("% rationed", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.CreditRationingRate_cFirms));
				creditRationing.setName("Credit rationing");
				updateChartSet.add(creditRationing);			//Add to set to be updated in buildSchedule method
			    tabSet.add(creditRationing);
//				GuiUtils.addWindow(creditRationing, 300, 0, 300, 250);
				
			    
			    //Two charts under one tab, because data in charts have different scales
			    Set<JInternalFrame> debtPlots = new LinkedHashSet<JInternalFrame>();
				TimeSeriesSimulationPlotter debt = new TimeSeriesSimulationPlotter("Debt", "");
				debt.addSeries("Bad debt (cFirm sector)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.BadDebt));
				debt.addSeries("Bad debt (bank)", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.BadDebt)); 
				debt.addSeries("Debt", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.Debt));
//				debt.setName("Debt");
				updateChartSet.add(debt);			//Add to set to be updated in buildSchedule method
				debtPlots.add(debt);
//			    tabSet.add(debt);
//				GuiUtils.addWindow(debt, 600, 0, 300, 250);							    			    			    
			    TimeSeriesSimulationPlotter badDebt = new TimeSeriesSimulationPlotter("Bad Debt", "");
				badDebt.addSeries("Bad Debt (cFirm sector)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.BadDebt));
				badDebt.addSeries("Bad debt (bank)", (IDoubleSource) new MultiTraceFunction.Double(bank, Bank.Variables.BadDebt));
				updateChartSet.add(badDebt);			//Add to set to be updated in buildSchedule method
				debtPlots.add(badDebt);
				//The observer's createScrollPaneFromPlots() method arranges layout for multiple charts side-by-side
				tabSet.add(createScrollPaneFromPlots(debtPlots, "Debt", debtPlots.size()));		//Add to set of charts to feature in tabbed pane


			    
			    
			    
				TimeSeriesSimulationPlotter capital = new TimeSeriesSimulationPlotter("Capital Stock of Consumption Firms", "Log");
//				capital.addSeries("", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Empty));					//XXX: Ross: Not sure what Hugo was hoping to achieve here
				capital.addSeries("Actual Stock (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogCapitalStock_cFirms));
				capital.addSeries("Desired Stock (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredCapitalStock_cFirms));
				capital.addSeries("Top Limit of Stock (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogCapitalStockTopLimit));
				capital.setName("Capital");
				updateChartSet.add(capital);			//Add to set to be updated in buildSchedule method
			    tabSet.add(capital);
//				GuiUtils.addWindow(capital, 600, 0, 300, 250);

				TimeSeriesSimulationPlotter costPriceMarkUp = new TimeSeriesSimulationPlotter("Cost, Price & Mark-up Averages", "");		//was Labor & mu	//XXX: Why was this named 'Labor'?
//				laborMarket.addSeries("", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Empty));				//XXX: Ross: Not sure what Hugo was hoping to achieve here
				costPriceMarkUp.addSeries("Avg Mark-up", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TotalMarkUp_cFirms));
				costPriceMarkUp.addSeries("CPI", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.CPI));
				costPriceMarkUp.addSeries("Avg Cost", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.MeanCostWeightedByMarketShare_cFirms));
				costPriceMarkUp.setName("Cost, Price & Mark-up");
				updateChartSet.add(costPriceMarkUp);			//Add to set to be updated in buildSchedule method
			    tabSet.add(costPriceMarkUp);					//Tab will be created for this chart
//				GuiUtils.addWindow(laborMarket, 900, 0, 300, 250);
				
				TimeSeriesSimulationPlotter capitalMarket = new TimeSeriesSimulationPlotter("Investment (expansionary)", "Log");			//Was Capital Market
				capitalMarket.addSeries("Actual (log).", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalExpansionaryInvestment_cFirms));
				// capitalMarket.addSeries("Sub. inv.", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.InvestmentSub));
				capitalMarket.addSeries("Desired (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredExpansionaryInvestmentTotal_cFirms));
				capitalMarket.addSeries("Desired star (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogDesiredExpansionaryInvestmentTotalStar_cFirms));	//XXX: What does star mean???
				capitalMarket.setName("Investment (expansionary)");
				updateChartSet.add(capitalMarket);			//Add to set to be updated in buildSchedule method
			    tabSet.add(capitalMarket);					//Tab will be created for this chart
//				GuiUtils.addWindow(capitalMarket, 0, 250, 300, 250);
				
				TimeSeriesSimulationPlotter goodMarket = new TimeSeriesSimulationPlotter("Goods Market (Consumption Firms)", "Log");
				goodMarket.addSeries("Production (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogProduction_cFirms));
				goodMarket.addSeries("Consumption (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogConsumption));
				goodMarket.addSeries("Total Desired Production (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalDesiredProduction_cFirms));
				goodMarket.setName("Goods Market");
				updateChartSet.add(goodMarket);			//Add to set to be updated in buildSchedule method
			    tabSet.add(goodMarket);					//Tab will be created for this chart
//				GuiUtils.addWindow(goodMarket, 300, 250, 300, 250);

			    
			    //XXX: Ross - I don't understand the labelling of these time-series!!!  Need to check
			    Set<JInternalFrame> productivityPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter machineProd = new TimeSeriesSimulationPlotter("Machine Productivity (Capital Firms)", ""); 
				// rd.addSeries("R&D expenditures", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.RdExpenditures));
//				rdAll.addSeries("Avg Firm Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.MeanProductivityWeightedByMarketShare_kFirms));	//was labelled 'Mean K'.
				machineProd.addSeries("Avg Machine Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.MeanMachineProductivityWeightedByMarketShare));	//was labelled 'Mean Machine'. 
				machineProd.addSeries("Top Prod Labor", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TopProdLabor));		//was labelled 'Top Prod Labor'.  XXX: Not sure if this variable actually is related to Labor or to Machine productivity???
//				rdAll.addSeries("Top Machine Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TopProdMachine));	//was labelled 'Top Prod Capital'.
				updateChartSet.add(machineProd);			//Add to set to be updated in buildSchedule method
				productivityPlots.add(machineProd); 							    			    			    
			    TimeSeriesSimulationPlotter capitalProd = new TimeSeriesSimulationPlotter("Capital Firm Productivity", "");
				capitalProd.addSeries("Avg Firm Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.MeanProductivityWeightedByMarketShare_kFirms));	//was labelled 'Mean K'.
				capitalProd.addSeries("Top Machine Productivity", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TopProdMachine));	//was labelled 'Top Prod Capital'. 
				updateChartSet.add(capitalProd);			//Add to set to be updated in buildSchedule method
				productivityPlots.add(capitalProd);
				tabSet.add(createScrollPaneFromPlots(productivityPlots, "Technology (R&D)", productivityPlots.size()));
			    
			    
				TimeSeriesSimulationPlotter profit = new TimeSeriesSimulationPlotter("Total Profit", ""); 
				profit.addSeries("Consumption Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Profit_cFirms));
				profit.addSeries("Capital Firms sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Profit_kFirms));
				profit.setName("Profit");
				updateChartSet.add(profit);			//Add to set to be updated in buildSchedule method
			    tabSet.add(profit);					//Tab will be created for this chart
//				GuiUtils.addWindow(profit, 900, 250, 300, 250);
				
				TimeSeriesSimulationPlotter clientAndAge = new TimeSeriesSimulationPlotter("Machines: Clients & Age", "#"); 
				clientAndAge.addSeries("Max Clients per Capital Firm", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.MaxClientsPerFirm_kFirms));
				clientAndAge.addSeries("Avg Age of Machines owned by Consumption Firms (time-steps)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.AverageAgeMachine_cFirms));
				clientAndAge.setName("Machines: Clients & Age");
				updateChartSet.add(clientAndAge);			//Add to set to be updated in buildSchedule method
			    tabSet.add(clientAndAge);					//Tab will be created for this chart
//				GuiUtils.addWindow(clientAndAge, 0, 500, 300, 250);
				
				TimeSeriesSimulationPlotter inventories = new TimeSeriesSimulationPlotter("Inventories", ""); 
				inventories.addSeries("Total Consumption Sector", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.TotalInventories));
				inventories.setName("Inventories");
				updateChartSet.add(inventories);			//Add to set to be updated in buildSchedule method
			    tabSet.add(inventories);					//Tab will be created for this chart
//				GuiUtils.addWindow(inventories, 300, 500, 300, 250);
				
				TimeSeriesSimulationPlotter government = new TimeSeriesSimulationPlotter("Government Ratios", "");
				government.addSeries("Balance (revenues - spending) to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovBalanceToGdp));
				government.addSeries("Stock (accumulated Balance over time) to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovStockToGdp));
				government.addSeries("Spending to GDP ratio", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovSpendingToGdp));
				government.setName("Government");
				updateChartSet.add(government);			//Add to set to be updated in buildSchedule method
			    tabSet.add(government);					//Tab will be created for this chart
//				GuiUtils.addWindow(government, 600, 500, 300, 250);
			    

			    Set<JInternalFrame> laborMarketPlots = new LinkedHashSet<JInternalFrame>();			    
			    TimeSeriesSimulationPlotter laborMarketWage = new TimeSeriesSimulationPlotter("Market Wage", "");
				laborMarketWage.addSeries("Market Wage", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.Wage));
				updateChartSet.add(laborMarketWage);			//Add to set to be updated in buildSchedule method
				laborMarketPlots.add(laborMarketWage);
			    TimeSeriesSimulationPlotter laborMarketUnemployment = new TimeSeriesSimulationPlotter("Unemployment Rate", "");
				laborMarketUnemployment.addSeries("Unemployment Rate", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.UnemploymentRate));
				updateChartSet.add(laborMarketUnemployment);			//Add to set to be updated in buildSchedule method
				laborMarketPlots.add(laborMarketUnemployment);
				tabSet.add(createScrollPaneFromPlots(laborMarketPlots, "Labor Market", laborMarketPlots.size()));
			    
			    
				TimeSeriesSimulationPlotter exit = new TimeSeriesSimulationPlotter("Firm Exits", "# per time-step");
				exit.addSeries("Capital Firms", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.Exit_kFirms));
				exit.addSeries("Consumption Firms", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.Exit_cFirms));
				exit.addSeries("Consumption Firms: Liquidity Issue", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.ExitLiquidityIssue_cFirms));
				exit.addSeries("Consumption Firms: Market Share Issue", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.ExitMarketShareIssue_cFirms));
				exit.addSeries("Consumption Firms: Liquidity & Market Share Issues", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.ExitAssetMarket_cFirms));
				exit.setName("Firm Exits");
				updateChartSet.add(exit);			//Add to set to be updated in buildSchedule method
			    tabSet.add(exit);					//Tab will be created for this chart
//				GuiUtils.addWindow(exit, 900, 500, 300, 250);
				
				TimeSeriesSimulationPlotter asset = new TimeSeriesSimulationPlotter("Assets", "Log");
				asset.addSeries("Capital Firms: Total Liquid Assets (log)", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalLiquidAssets_kFirms));
				asset.setName("Assets");
				updateChartSet.add(asset);			//Add to set to be updated in buildSchedule method
			    tabSet.add(asset);					//Tab will be created for this chart
//				GuiUtils.addWindow(asset, 500, 500, 300, 250); 
				
//				TimeSeriesSimulationPlotter flag = new TimeSeriesSimulationPlotter("Flag", " # ");
//				// flag.addSeries("U Rate", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.UnemploymentRate));
//				flag.addSeries("Kfirms neg. asset", (IIntSource) new MultiTraceFunction.Integer(collector, MacroCollector.Variables.Flag));
//				flag.setName("Flag");
//				updateChartSet.add(flag);			//Add to set to be updated in buildSchedule method
//			    tabSet.add(flag);					//Tab will be created for this chart
////				GuiUtils.addWindow(flag, 300, 300, 300, 250); 
			    
			    
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
			    
				
			} else {
				
				updateChartSetDosiCompare = new LinkedHashSet<JInternalFrame>();	//Set of all charts needed to be scheduled for updating (NOT the convergence plot!)
				tabSetDosiCompare = new LinkedHashSet<JComponent>();		//Set of all JInternalFrames each having a tab.  Each tab frame will potentially contain more than one chart each.
				
								
				TimeSeriesSimulationPlotter logOutputConsumptionInvestment = new TimeSeriesSimulationPlotter("Time Series", "Log");
				logOutputConsumptionInvestment.addSeries("Log GDP", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogGDP));
				logOutputConsumptionInvestment.addSeries("Log Consumption", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumptionLog));
				logOutputConsumptionInvestment.addSeries("Log Investment", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalInvestment));
				logOutputConsumptionInvestment.setName("Time-series");
				updateChartSetDosiCompare.add(logOutputConsumptionInvestment);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(logOutputConsumptionInvestment);					//Tab will be created for this chart
//				GuiUtils.addWindow(logOutputConsumptionInvestment, 0, 0, 500, 400);
								
				TimeSeriesSimulationPlotter logInvestments = new TimeSeriesSimulationPlotter("Log Investments", "");
				logInvestments.addSeries("Log Inv. Sub.", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalSubsitionaryInvestment_cFirms));
				logInvestments.addSeries("Log Inv. Exp.", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LogTotalExpansionaryInvestment_cFirms));
				logInvestments.setName("Investment");
				updateChartSetDosiCompare.add(logInvestments);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(logInvestments);					//Tab will be created for this chart
//				GuiUtils.addWindow(logInvestments, 0, 400, 500, 200);
				
				TimeSeriesSimulationPlotter logProductivityMomentsC = new TimeSeriesSimulationPlotter("Moments prod. C-firms", "Log");
				logProductivityMomentsC.addSeries("Mean", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.meanLogProductivity_cFirms));
				logProductivityMomentsC.setName("Moments prod. C-firms");
				updateChartSetDosiCompare.add(logProductivityMomentsC);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(logProductivityMomentsC);					//Tab will be created for this chart
//				GuiUtils.addWindow(logProductivityMomentsC, 500, 0, 400, 200);
				
				TimeSeriesSimulationPlotter logProductivityMomentsK = new TimeSeriesSimulationPlotter("Moments prod. K-firms", "Log");
				logProductivityMomentsK.addSeries("Mean", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.meanLogProductivity_kFirms));
				logProductivityMomentsK.setName("Moments prod. K-firms");
				updateChartSetDosiCompare.add(logProductivityMomentsK);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(logProductivityMomentsK);					//Tab will be created for this chart
//				GuiUtils.addWindow(logProductivityMomentsK, 500, 200, 400, 200);
				
				TimeSeriesSimulationPlotter investmentLumpinessLow = new TimeSeriesSimulationPlotter("Inv. lumpiness", "%");
				investmentLumpinessLow.addSeries("I/K < 0.02", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.LowInv));
				investmentLumpinessLow.setName("Inv. lumpiness (Low)");
				updateChartSetDosiCompare.add(investmentLumpinessLow);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(investmentLumpinessLow);					//Tab will be created for this chart
//				GuiUtils.addWindow(investmentLumpinessLow, 900, 0, 400, 200);
				
				TimeSeriesSimulationPlotter investmentLumpinessHigh = new TimeSeriesSimulationPlotter("Inv. lumpiness", "%");
				investmentLumpinessHigh.addSeries("I/K > 0.35", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.HighInv));
				investmentLumpinessHigh.setName("Inv. lumpiness (High)");
				updateChartSetDosiCompare.add(investmentLumpinessHigh);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(investmentLumpinessHigh);					//Tab will be created for this chart
//				GuiUtils.addWindow(investmentLumpinessHigh, 900, 200, 400, 200);
				
				TimeSeriesSimulationPlotter inflations = new TimeSeriesSimulationPlotter("Inflations", "%");
				inflations.addSeries("Cpi inflation", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ConsumerInflation_cFirms));
				inflations.addSeries("Ppi inflation", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.ProducerInflation_kFirms));
				inflations.setName("Inflation");
				updateChartSetDosiCompare.add(inflations);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(inflations);					//Tab will be created for this chart
//				GuiUtils.addWindow(inflations, 500, 400, 400, 200);
				
				TimeSeriesSimulationPlotter government = new TimeSeriesSimulationPlotter("Government", "% of GDP");
				government.addSeries("Balance", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovBalanceToGdp));
				government.addSeries("Debt", (IDoubleSource) new MultiTraceFunction.Double(collector, MacroCollector.Variables.GovStockToGdp));
				government.setName("Government");
				updateChartSetDosiCompare.add(government);			//Add to set to be updated in buildSchedule method
			    tabSetDosiCompare.add(government);					//Tab will be created for this chart
//				GuiUtils.addWindow(government, 900, 400, 400, 200);

			    
			    //-------------------------------------------------------------------------------------------------------
			    //
		    	//	BUILD A TABBED PANE HOLDING ALL THE CHARTS THAT ONLY UPDATE AT EACH TIME-STEP (not convergence plots)
			    //
		    	//-------------------------------------------------------------------------------------------------------
			    
		        //Create tabbed pane to hold all the charts and add to the JAS-mine GUI window
		    	JInternalFrame chartsFrame = new JInternalFrame("Charts");
				JTabbedPane tabbedPane = new JTabbedPane();
				chartsFrame.add(tabbedPane);
				
				for(JComponent plot: tabSetDosiCompare) {
					tabbedPane.addTab(plot.getName(), plot);
				}
				tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		    	chartsFrame.setResizable(true);
		    	chartsFrame.setMaximizable(true);
				GuiUtils.addWindow(chartsFrame, 300, 0, 1560, 660);
			    
			    
			}
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
				for(JInternalFrame plot: updateChartSetDosiCompare) {
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

}
