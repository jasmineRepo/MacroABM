package jasmine.algorithms;

import org.apache.log4j.Logger;

import jasmine.data.Parameters;
import jasmine.model.CFirm;
import jasmine.model.MacroModel;

public class APosterioriAdjustments {
	
	/* INTRODUCTORY NOTES: A posteriori adjustments are highly similar to a priori adjustments. The only difference are 
	 		(a) c-firms do not base their decision on their maxLoan, but on their loan
	 		(b) when they are stuck and they cannot find a way out, because they already received the loan, they use it (compare to in the a priori case where cfirms were trying
	 		to not be more leveraged in such cases) 
	Most of the class is not commented due to its similarity with APrioriAdjustments.
	 */
	
	private final static Logger log = Logger.getLogger(APosterioriAdjustments.class);
	
	static double qStar;
	static double invExpStar;
	static double invSubStar;
	static double loanProd;
	static double loanDebt;
	static double liquidAssetPrime;
	
	public static void adjustmentWithNilLoan(CFirm cFirm){
		
		log.debug("Enter the adjustment with nil loan process");
		
		qStar = cFirm.getOptimalProduction();
		invExpStar = cFirm.getInvestmentExpansionaryStar();
		invSubStar = cFirm.getInvestmentSubstitionaryStar();
		
		liquidAssetPrime = cFirm.getLiquidAssetRemainingAfterProductionAndInvestment();
		// Because the firm does not have access to external funding:
		loanProd = 0;
		loanDebt = 0;
		
		// Collection of parameters
		double param2 = Parameters.getDebtRepaymentSharePerPeriod_cFirms() + (1 - MacroModel.getTax()) * MacroModel.getrDebt();
		double param1 = (1 - MacroModel.getTax()) * MacroModel.getrDepo();
		double machinePrice = cFirm.getSupplier().getPriceOfGoodProducedNow();
		
		boolean exitAdjustment = false;
		
		// adjustments through sub. inv.
		if(invSubStar > 0){
			
			invSubStar = 0;
			if(cFirm.getBalanceSheet().payment(qStar, (long) invExpStar, 0, 0) > 0) {
				
				double salesTemp = cFirm.getPriceOfGoodProducedNow() * Math.min(qStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				invSubStar = Parameters.getMachineSizeInCapital_cFirms() / (machinePrice * (1. + param1) ) * ( (cFirm.getLiquidAsset()[0] - qStar * cFirm.getCostToProduceGood() - invExpStar * machinePrice / Parameters.getMachineSizeInCapital_cFirms() ) * (1. + param1) +  
						 (1. - MacroModel.getTax()) * salesTemp - param2 * cFirm.getDebt()[0]);
				
				log.debug("Adjustment throuhg inv. sub. is possible; new inv. sub. = " + invSubStar + " and payment = " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar + invSubStar), 0, 0));
				invSubStar = Math.floor(invSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				exitAdjustment = true;
				
			}
		}
		
		// adjustments through sub. exp.
		if(invExpStar > 0 && !exitAdjustment){
			
			invExpStar = 0;
			if(cFirm.getBalanceSheet().payment(qStar, 0, 0, 0) > 0) {
				
				double salesTemp = cFirm.getPriceOfGoodProducedNow() * Math.min(qStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				invExpStar = Parameters.getMachineSizeInCapital_cFirms() / (machinePrice * (1. + param1) )  * ( (cFirm.getLiquidAsset()[0] - qStar * cFirm.getCostToProduceGood() ) * (1. + param1) + 
						(1. - MacroModel.getTax()) * salesTemp - param2 * cFirm.getDebt()[0]); 
						
				log.debug("Adjustment throuhg inv. exp. is possible; new inv. exp. = " + invExpStar + " and payment = " + cFirm.getBalanceSheet().payment(qStar, (long) invExpStar, 0., 0.));
				invExpStar = Math.floor(invExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				exitAdjustment = true;	
				
			}
		}
		
		// adjustments through inventories
		if(!exitAdjustment){
			if(cFirm.getExpectedDemand() > cFirm.getInventories()[0]){
				if(qStar + cFirm.getInventories()[0] > cFirm.getExpectedDemand()){
					
					qStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
					
					if(cFirm.getBalanceSheet().payment(qStar, 0l, 0., 0.) > 0){
						
						qStar = 1. / (cFirm.getCostToProduceGood() * (1. + param1) ) * (cFirm.getLiquidAsset()[0] * (1. + param1) + (1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand() -
								param2 * cFirm.getDebt()[0] ); 
						
						exitAdjustment = true;
						log.debug("Adjustment throuhg inventories. is possible; new q = " + qStar + " and payment = " + cFirm.getBalanceSheet().payment(qStar, 0l, 0., 0.));
						
					}
				}
			} else {
				exitAdjustment = true;
				if(qStar > 0){
					
					qStar = 0;
					if(cFirm.getBalanceSheet().payment(0., 0l, 0., 0.) > 0){
						
						qStar = 1. / (cFirm.getCostToProduceGood() * (1. + param1) ) * (cFirm.getLiquidAsset()[0] * (1. + param1) + (1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand() -
								param2 * cFirm.getDebt()[0] ); 
						log.debug("Adjustment throuhg inventories. is possible; new q = " + qStar + " and payment = " + cFirm.getBalanceSheet().payment(qStar, 0l, 0., 0.));
						
					} else {
						qStar = 0.;
						log.debug("Adj on inventories: does not exit an optimal solution. q = 0");
					}
				} else {
					qStar = 0.;
					log.debug("Adj on inventories: does not exit an optimal solution. q = 0");
				}
			}
		}
		
		// adjustments through production
		if(!exitAdjustment){
			if( cFirm.getPriceOfGoodProducedNow() < cFirm.getCostToProduceGood() * (1. + param1) / (1. - MacroModel.getTax()) ){
				
				qStar = 1. / ( (1. - MacroModel.getTax() ) * ( cFirm.getPriceOfGoodProducedNow() - MacroModel.getrDepo() * cFirm.getCostToProduceGood()) - cFirm.getCostToProduceGood() )  * (
						param2 * cFirm.getDebt()[0] - (1. + param1) * cFirm.getLiquidAsset()[0] - 
						(1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0]
						);
				exitAdjustment = true;
				log.debug("Payment is decreasing in q; optimal production is " + qStar + " with payment = " + cFirm.getBalanceSheet().payment(qStar, 0l, 0., 0.)); 
				
				if(qStar < 0.)
					qStar = 0.;
				
			} else {
				qStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
				if(cFirm.getCostToProduceGood() * qStar > cFirm.getLiquidAsset()[0])
					qStar =  cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood();
				
				log.debug("Payment is increasing in production; pick the level that max. the payment function. Q = " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, 0., 0.)); 
			}
		}
		
		// update the c-firms variables
		cFirm.setProductionQuantity(qStar);
		cFirm.setInvestmentExpansionary(invExpStar);
		cFirm.setdInvestmentSubstitutionary(invSubStar);
		cFirm.setLoanForDebtRepayment(0);
		cFirm.setLoanForProductionAndInvestment(0);
		
	}
	
	public static void adjustmentWithPositiveLoan(CFirm cFirm){
		
		log.debug("Enter the adjustment with positive loan process");
		
		
		qStar = cFirm.getOptimalProduction();
		invExpStar = cFirm.getInvestmentExpansionaryStar();
		invSubStar = cFirm.getInvestmentSubstitionaryStar();
		
		liquidAssetPrime = cFirm.getLiquidAssetRemainingAfterProductionAndInvestment();
		// Because the firm does not have access to external funding:
		loanProd = cFirm.getLoanForProductionAndInvestment();
		loanDebt = cFirm.getLoanForDebtRepayment();
		
		// Collection of parameters
		double param2 = Parameters.getDebtRepaymentSharePerPeriod_cFirms() + (1. - MacroModel.getTax()) * MacroModel.getrDebt();
		double param1 = (1 - MacroModel.getTax()) * MacroModel.getrDepo();
		double machinePrice = cFirm.getSupplier().getPriceOfGoodProducedNow();
		
		boolean exitAdjustment = false;
		
		// adjustments through sub. inv.
		if(invSubStar > 0){
			
			double savings = machinePrice * invSubStar / Parameters.getMachineSizeInCapital_cFirms();
			saving(savings, cFirm);
			invSubStar = 0;
			
			if(cFirm.getBalanceSheet().payment(qStar, (long) invExpStar, loanDebt, loanProd) > 0){
				
				double salesTemp = cFirm.getPriceOfGoodProducedNow() * Math.min(qStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				if(loanProd > 0){
					
					invSubStar = (Parameters.getMachineSizeInCapital_cFirms() / machinePrice) * ( 
							cFirm.getLiquidAsset()[0] + (1. - param2) * cFirm.getLoan() + (1. - MacroModel.getTax()) * salesTemp - 
							cFirm.getCostToProduceGood() * qStar - param2 * cFirm.getDebt()[0] - machinePrice * invExpStar / Parameters.getMachineSizeInCapital_cFirms()
							);
					
					loanProd = Math.max(0., cFirm.getCostToProduceGood() * qStar + cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar + invSubStar) / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
					loanDebt = cFirm.getLoan() - loanProd;
					liquidAssetPrime = Math.max(0., cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar + invSubStar) / Parameters.getMachineSizeInCapital_cFirms());
					
					log.debug("Find an optimal adj. through inv. sub. with new inv = " + invSubStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar + invSubStar), loanDebt, loanProd));
					
					savings = (invSubStar - Math.floor(invSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
					saving(savings, cFirm);
					
				} else {
					
					double liquidAssetUsable = cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - machinePrice * invExpStar / Parameters.getMachineSizeInCapital_cFirms();
					invSubStar = Math.min(cFirm.getdInvestmentSubstitutionary(), liquidAssetUsable * Parameters.getMachineSizeInCapital_cFirms() / machinePrice );
					loanProd = 0.;
					loanDebt = cFirm.getLoan();
					
					if(cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar + invSubStar), loanDebt, loanProd) > 0.){
						
						invSubStar = (Parameters.getMachineSizeInCapital_cFirms() / machinePrice) * ( 
								cFirm.getLiquidAsset()[0] + (1. - param2) * cFirm.getLoan() + (1. - MacroModel.getTax()) * salesTemp - 
								cFirm.getCostToProduceGood() * qStar - param2 * cFirm.getDebt()[0] - machinePrice * invExpStar / Parameters.getMachineSizeInCapital_cFirms()
								);
						
						loanProd = Math.max(0., cFirm.getCostToProduceGood() * qStar + cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar + invSubStar) / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
						loanDebt = cFirm.getLoan() - loanProd;
						liquidAssetPrime = Math.max(0., cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar + invSubStar) / Parameters.getMachineSizeInCapital_cFirms());
						
						log.debug("Find an optimal adj. through inv. sub. with new inv = " + invSubStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar + invSubStar), loanDebt, loanProd));
						
						savings = (invSubStar - Math.floor(invSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
						
					} else {
						
						invSubStar = Parameters.getMachineSizeInCapital_cFirms() / (machinePrice * ( 1. + param1) ) * ( (1. - MacroModel.getTax()) * salesTemp + 
								(cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - machinePrice * invExpStar / Parameters.getMachineSizeInCapital_cFirms() )  * ( 1. + param1) + (1. - param2) * loanDebt - 
								param2 * cFirm.getDebt()[0]);
						log.debug("Find an optimal adj. through inv. sub. with new inv = " + invSubStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar + invSubStar), loanDebt, loanProd));
						
						liquidAssetPrime = Math.max(0., cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - machinePrice * (invExpStar + invSubStar) / Parameters.getMachineSizeInCapital_cFirms());
						savings = (invSubStar - Math.floor(invSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
						
					}
				}
				
				exitAdjustment = true;
				invSubStar = Math.floor(invSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				
			}
			
		}
		
		// adjustments through sub. exp.
		if(invExpStar > 0. && !exitAdjustment){
			
			double savings = machinePrice * invExpStar / Parameters.getMachineSizeInCapital_cFirms();
			saving(savings, cFirm);
			invExpStar = 0.;
			
			if(cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) > 0.){
				
				double salesTemp = cFirm.getPriceOfGoodProducedNow() * Math.min(qStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				if(loanProd > 0.){
					
					invExpStar = (Parameters.getMachineSizeInCapital_cFirms() / machinePrice) * ( 
							cFirm.getLiquidAsset()[0] + (1. - param2) * cFirm.getLoan() + (1. - MacroModel.getTax()) * salesTemp - 
							cFirm.getCostToProduceGood() * qStar - param2 * cFirm.getDebt()[0] 
							);
					
					loanProd = Math.max(0., cFirm.getCostToProduceGood() * qStar + cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar) / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
					loanDebt = cFirm.getLoan() - loanProd;
					liquidAssetPrime = Math.max(0., cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar) / Parameters.getMachineSizeInCapital_cFirms());
					
					log.debug("Find an optimal adj. through inv. exp. with new inv = " + invExpStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar), loanDebt, loanProd));
					
					savings = (invExpStar - Math.floor(invExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
					saving(savings, cFirm);
					
				} else {
					
					double liquidAssetUsable = cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar;
					invExpStar = Math.min(cFirm.getdInvestmentSubstitutionary(), liquidAssetUsable * Parameters.getMachineSizeInCapital_cFirms() / machinePrice );
					loanProd = 0.;
					loanDebt = cFirm.getLoan();
					
					if(cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar), loanDebt, loanProd) > 0.){
						
						invExpStar = (Parameters.getMachineSizeInCapital_cFirms() / machinePrice) * ( 
								cFirm.getLiquidAsset()[0] + (1. - param2) * cFirm.getLoan() + (1. - MacroModel.getTax()) * salesTemp - 
								cFirm.getCostToProduceGood() * qStar - param2 * cFirm.getDebt()[0] 
								);
						
						loanProd = Math.max(0., cFirm.getCostToProduceGood() * qStar + cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar) / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
						loanDebt = cFirm.getLoan() - loanProd;
						liquidAssetPrime = Math.max(0., cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - cFirm.getSupplier().getPriceOfGoodProducedNow() * (invExpStar) / Parameters.getMachineSizeInCapital_cFirms());
						
						log.debug("Find an optimal adj. through inv. exp. with new inv = " + invExpStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar), loanDebt, loanProd));
						
						savings = (invExpStar - Math.floor(invExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
						
					} else {
						
						invExpStar = Parameters.getMachineSizeInCapital_cFirms() / (machinePrice * ( 1. + param1) ) * ( (1. - MacroModel.getTax()) * salesTemp + 
								(cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar )  * ( 1. + param1) + (1. - param2) * loanDebt - 
								param2 * cFirm.getDebt()[0]);
						log.debug("Find an optimal adj. through inv. exp. with new inv = " + invExpStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, (long) (invExpStar), loanDebt, loanProd));
						
						liquidAssetPrime = Math.max(0., cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar - machinePrice * (invExpStar) / Parameters.getMachineSizeInCapital_cFirms());
						savings = (invExpStar - Math.floor(invExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
						
					}
				}
				
				exitAdjustment = true;
				invExpStar = Math.floor(invExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				
			}
		}
		
		// adjustments through inventories
		if(!exitAdjustment){
			if(cFirm.getExpectedDemand() > cFirm.getInventories()[0]){
				if(qStar + cFirm.getInventories()[0] > cFirm.getExpectedDemand()){
					
					double savings = (qStar + cFirm.getInventories()[0] - cFirm.getExpectedDemand()) * cFirm.getCostToProduceGood();
					saving(savings, cFirm);
					qStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
					double salesTemp = cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand();
					
					if(cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) > 0.){
						
						double qLiquidAsset = Math.min(cFirm.getdQ(), cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood()); // = q** in APrioriAdjustment
						if(qLiquidAsset <= qStar){
							
							qStar = 1. / cFirm.getCostToProduceGood() * ( cFirm.getLiquidAsset()[0] + (1. - MacroModel.getTax()) * salesTemp + 
			        				(1. - param2) * cFirm.getLoan() - param2 * cFirm.getDebt()[0]);
							exitAdjustment = true;
							loanProd = cFirm.getCostToProduceGood() * qStar - cFirm.getLiquidAsset()[0]; // should be positive
							loanDebt = cFirm.getLoan() - loanProd;
							
							log.debug("Adjustment through inventories; new production = " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
							
						} else {
							if(cFirm.getBalanceSheet().payment(qLiquidAsset, 0l, cFirm.getLoan(), 0.) > 0.){
								
								qStar = 1. / cFirm.getCostToProduceGood() * ( cFirm.getLiquidAsset()[0] + (1. - MacroModel.getTax()) * salesTemp + 
				        				(1. - param2) * cFirm.getLoan() - param2 * cFirm.getDebt()[0]);
								exitAdjustment = true;
								loanProd = cFirm.getCostToProduceGood() * qStar - cFirm.getLiquidAsset()[0]; // should be positive
								loanDebt = cFirm.getLoan() - loanProd;
								
								log.debug("Adjustment through inventories; new production = " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
								
							} else {
								
								loanProd = 0.;
								loanDebt = cFirm.getLoan();
								qStar = 1. / (cFirm.getCostToProduceGood() * (1. + param1)) * ( cFirm.getLiquidAsset()[0] * ( 1. + param1) + (1. - MacroModel.getTax()) * salesTemp + 
				        				(1. - param2) * cFirm.getLoan() - param2 * cFirm.getDebt()[0]);
								
								exitAdjustment = true;
								log.debug("Adjustment through inventories; new production = " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
								
							}
						}	
					}
				}
			} else {
				exitAdjustment = true;
				if(qStar > 0.){
					
					double savings = qStar * cFirm.getCostToProduceGood();
					saving(savings, cFirm);
					qStar = 0.;
					double salesTemp = cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand();
					
					if(cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) > 0.){
						
						double qLiquidAsset = Math.min(cFirm.getdQ(), cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood()); // = q** in APrioriAdjustment
						
						if(cFirm.getBalanceSheet().payment(qLiquidAsset, 0l, cFirm.getLoan(), 0.) > 0.){
							
							qStar = 1. / cFirm.getCostToProduceGood() * ( cFirm.getLiquidAsset()[0] + (1. - MacroModel.getTax()) * salesTemp + 
			        				(1. - param2) * cFirm.getLoan() - param2 * cFirm.getDebt()[0]);
							exitAdjustment = true;
							loanProd = cFirm.getCostToProduceGood() * qStar - cFirm.getLiquidAsset()[0]; // should be positive
							loanDebt = cFirm.getLoan() - loanProd;
							
							log.debug("Adjustment through inventories; new production = " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
							
						} else {
							
							loanProd = 0.;
							loanDebt = cFirm.getLoan();
							qStar = 1. / (cFirm.getCostToProduceGood() * (1. + param1)) * ( cFirm.getLiquidAsset()[0] * ( 1. + param1) + (1. - MacroModel.getTax()) * salesTemp + 
			        				(1. - param2) * cFirm.getLoan() - param2 * cFirm.getDebt()[0]);
							
							log.debug("Adjustment through inventories; new production = " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
							
						}
						
					} else {
						
						qStar = 0.;
						loanProd = 0.;
						loanDebt = cFirm.getLoan();
						
						log.debug("Adjustment through inventories failed and cannot adjust further");
						
					}
				} else {

					qStar = 0.;
					loanProd = 0.;
					loanDebt = cFirm.getLoan();
					
					log.debug("Adjustment through inventories failed and cannot adjust further");
					
				}
			}
		}
		
		// adjustments through production
		if(!exitAdjustment){
			
			if(loanProd > 0.){
				
				if( (1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() < cFirm.getCostToProduceGood() ){
					
					loanDebt = cFirm.getLoan();
					loanProd = 0.;
					qStar = Math.min(qStar, cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood());
					
					if(cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) > 0.){
						
						qStar = 1. / ( (1. - MacroModel.getTax() ) * cFirm.getPriceOfGoodProducedNow() - cFirm.getCostToProduceGood() ) * (param2 * cFirm.getDebt()[0] - 
								(1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - 
								(1. - param2) * cFirm.getLoan() - cFirm.getLiquidAsset()[0]);
						
						loanProd = Math.max(0., cFirm.getCostToProduceGood() * qStar - cFirm.getLiquidAsset()[0]); 
						loanDebt = cFirm.getLoan() - loanProd;
						
						log.debug("loanProd > 0 & payment is decreasing in q. Optimal qty is " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
						
					} else {
						
						qStar = 1. / ((1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() - (1. + param1) * cFirm.getCostToProduceGood()) * (
								param2 * cFirm.getDebt()[0] - (1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - (1. - param2) * cFirm.getLoan() - 
								cFirm.getLiquidAsset()[0] * (1. + param1)
								);
						
						log.debug("loanProd > 0 & payment is decreasing in q but not sure if positive. Optimal qty is " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
						
						if(qStar < 0.)
							qStar = 0.;
						
					}	
				} else {
					if( cFirm.getPriceOfGoodProducedNow() * (1. - MacroModel.getTax()) < cFirm.getCostToProduceGood() * ( 1. + param1) ){
						
						qStar = 1. / ((1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() - (1. + param1) * cFirm.getCostToProduceGood()) * (
								param2 * cFirm.getDebt()[0] - (1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - (1. - param2) * cFirm.getLoan() - 
								cFirm.getLiquidAsset()[0] * (1. + param1)
								);
						
						if(qStar > 0.){
							
							loanDebt = cFirm.getLoan();
							loanProd = 0.;
							log.debug("Adjustment through production; optimal quantity is " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
							
						} else {
							
							qStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
							if(cFirm.getCostToProduceGood() * qStar <= cFirm.getLiquidAsset()[0]){
								
								liquidAssetPrime = cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar;
								loanProd = 0.;
								loanDebt = cFirm.getLoan();
								
							} else if(cFirm.getCostToProduceGood() * qStar <= cFirm.getLiquidAsset()[0] + cFirm.getLoan()){
								
								liquidAssetPrime = 0.;
								loanProd = cFirm.getLoan() + cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar;
								loanDebt = cFirm.getLoan() - loanProd;
								
							} else {
								
								qStar = (cFirm.getLoan() + cFirm.getLiquidAsset()[0]) / cFirm.getCostToProduceGood();
								loanProd = cFirm.getLoan();
								loanDebt = 0.;
								liquidAssetPrime = 0.;
								
							}
							
							if(cFirm.getBalanceSheet().payment(0., 0l, cFirm.getLoan(), 0.) > cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd)){
								
								qStar = 0.;
								loanProd = 0.;
								loanDebt = cFirm.getLoan();
								
								log.debug("We are looking at the global max. Global max is with 0 prod & full loan allocated to debt");
								
							} else {
								
								log.debug("We are looking at the global max. Global max is with positive production");
								
							}
						}
					} else {
						
						qStar = Math.min(cFirm.getExpectedDemand() - cFirm.getInventories()[0], (cFirm.getLiquidAsset()[0] + cFirm.getLoan()) / cFirm.getCostToProduceGood());
						loanProd = Math.max(0., qStar * cFirm.getCostToProduceGood()- cFirm.getLiquidAsset()[0]);
						loanDebt = cFirm.getLoan() - loanProd;
						
						log.debug("Cf + l globally increasing in Q. Set it at level the most rational && achievable");
					}
					
				}
			} else {
				
				loanDebt = cFirm.getLoan();
				loanProd = 0.;
				
				if(cFirm.getPriceOfGoodProducedNow() < (1. + param1) / (1. - MacroModel.getTax()) * cFirm.getCostToProduceGood()){
					
					qStar = 1. / ( (1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() - (1. + param1) * cFirm.getCostToProduceGood() )  * (
							param2 * cFirm.getDebt()[0] - (1. + param1) * cFirm.getLiquidAsset()[0] - 
							(1. - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - (1. - param2) * cFirm.getLoan()
							);
					
					log.debug("loan prod = 0 & payment is decreasing in production; optimal production " + qStar + " and payment " + cFirm.getBalanceSheet().payment(qStar, 0l, loanDebt, loanProd) );
					
					if(qStar < 0.)
						qStar = 0.;
					
				} else {
					
					qStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
					if(cFirm.getCostToProduceGood() * qStar <= cFirm.getLiquidAsset()[0]){
						
						liquidAssetPrime = cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar;
						loanProd = 0.;
						loanDebt = cFirm.getLoan();
						
					} else if(cFirm.getCostToProduceGood() * qStar <= cFirm.getLiquidAsset()[0] + cFirm.getLoan()){
						
						liquidAssetPrime = 0.;
						loanProd = cFirm.getLoan() + cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * qStar;
						loanDebt = cFirm.getLoan() - loanProd;
						
					} else {
						
						qStar = (cFirm.getLoan() + cFirm.getLiquidAsset()[0]) / cFirm.getCostToProduceGood();
						loanProd = cFirm.getLoan();
						loanDebt = 0.;
						liquidAssetPrime = 0.;
						
					}
					
					log.debug("Payment is increasing in production. Set production at " + qStar);
					
				}
			}
		}
		
		// update the c-firms variables
		cFirm.setProductionQuantity(qStar);
		cFirm.setInvestmentExpansionary(invExpStar);
		cFirm.setdInvestmentSubstitutionary(invSubStar);
		cFirm.setLoanForDebtRepayment(loanDebt);
		cFirm.setLoanForProductionAndInvestment(loanProd);
		
	}
	
	static void saving (double cash, CFirm firm) {
		
		if(loanProd > cash){
			
			loanProd -= cash;
			loanDebt += cash;
			
		} else if (loanProd > 0.){
			
			loanDebt += loanProd;
			liquidAssetPrime += cash - loanProd;
			loanProd = 0.;
			
		} else {
			
			liquidAssetPrime += cash;
			loanProd = 0.;
			loanDebt = firm.getLoan();
			
		}
	}

}
