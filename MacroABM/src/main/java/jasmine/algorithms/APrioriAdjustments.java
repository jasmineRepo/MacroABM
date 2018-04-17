package jasmine.algorithms;

import jasmine.data.Parameters;
import jasmine.model.CFirm;
import jasmine.model.MacroModel;

import org.apache.log4j.Logger;

public class APrioriAdjustments {
	/* INTRODUCTORY NOTES: the adjustments (a priori or a posteriori) follows a kind of dynamic programming approach. Because firms have preferences (production
	 over investment; internal funds over external borrowing), they start to adjust first on the variable that they like the least. If they still do not match the
	 payment condition, then they move to the second least preferred variables, and so on. The objective is therefore payment = 0, the constraint are the resource
	 constraints (i.e. the maximal possible loan and the stock of liquid assets are given), and the choice variables are (a) substitutionnary investment, (b) 
	 expansionary investments, (c) production -- ranked from the least to the most preferred. 
	
	 Remark: the payment function is linear in these three variables, however not continuous because it features several jumps. It is therefore easily possible
	 to come up with closed-form expressions; however, in each adjustments, for each variables, several scenarios are to be considered (especially in the
	 adjustmentsWithNilLiquidAsset method).
	 */
	
	private final static Logger log = Logger.getLogger(APrioriAdjustments.class);
	
	// list of static variables (use same notation as in CFirm class)
	static double dQStar;
	static double dInvExpStar;
	static double dInvSubStar;
	static double liquidAssetPrime;
	static double loanProd;
	static double loanDebt;

	public static void adjustmentsWithPositiveLiquidAsset(CFirm cFirm){
		
		log.debug("Adjustments With Positive Liquid Asset for CFirm " + cFirm.getKey().getId());
		
		// initialize the static variables with the initial values of production, investment and loan (i.e. before any adjustment)
		dQStar = cFirm.getProductionStar();
		dInvExpStar = cFirm.getdInvestmentExpansionaryStar();
		dInvSubStar = cFirm.getDesiredInvestmentSubstitionaryStar();
		liquidAssetPrime = cFirm.getLiquidAssetRemainingAfterProductionAndInvestment();
		loanProd = cFirm.getLoanForProductionAndInvestment();
		loanDebt = cFirm.getLoanForDebtRepayment();
		
		// collection of parameters to ease the computation of closed-form expression
		double param1 = (1. - MacroModel.getTax()) * MacroModel.getrDepo(); // net interest rate, i.e. deducting from the government's tax on profit
		double param2 = Parameters.getDebtRepaymentSharePerPeriod_cFirms() + (1. - MacroModel.getTax()) * MacroModel.getrDebt(); /* net debt service, i.e. debt repayments + 
		interest rate on the debt, taking into account the government's tax on profit */
		
		/* because compute closed-form solutions, the payment after adjustments tend to 0 but might be slightly different from 0. 
		To make sure that no-more-than-needed adjustments will be done, create this boolean */
		boolean exitAdjustment = false;
		
		/* As explained in the introductory note, c-firms will first try to adjust by reducing their level of substitutionnary investments, if possible. 
		NOTE: if we are in this adjustment method, it implies that the investment were initially funded through internal funds. Hence the reduction of investment
		goes to an increase in the first deposit, which yield a net return of param1, with param1 > 0. For more details on the intuition behind, see the 
		code documentation pdf. 
		 */
		
		if(dInvSubStar > 0.){
			
			/* The goal is to invoke the mean value theorem: payment is monotonically decreasing in inv. (regardless of their nature). Hence check whether at the minimal 
			level of sub. inv., i.e. dInvSubStar = 0, the payment is expected to be positive. If so, there exists an interior solution such that payment = 0. Otherwise,
			adjusting on sub. inv. is not sufficient and c-firms move to the next adjustment round.
			*/
			dInvSubStar = 0.;
			if(cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, 0.) >= 0.){
				// then can use the mean value theorem
				
				// redefine some variables to make the closed-form solution look less long
				double priceMachine = cFirm.getSupplier().getPriceOfGoodProducedNow();
				double sales = cFirm.getPriceOfGoodProducedNow() * Math.min(dQStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				// closed-form expression for the 'optimal' level of sub. inv.
				dInvSubStar = Parameters.getMachineSizeInCapital_cFirms() / (priceMachine * (1. + param1) ) * ( (cFirm.getLiquidAsset()[0] - dQStar * cFirm.getCostToProduceGood() - dInvExpStar * priceMachine / Parameters.getMachineSizeInCapital_cFirms()) * (1. + param1) + 
						loanDebt * (1. - param2) + (1. - MacroModel.getTax()) * sales - param2 * cFirm.getDebt()[0] );
				dInvSubStar = Math.floor(dInvSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms(); // do not forget that investment always need to be expressed in terms of machines
				
				exitAdjustment = true;
				log.debug("Adjustments through inv. sub. possible. New level of inv. sub = " + dInvSubStar + " and payment " + cFirm.getBalanceSheet().payment(dQStar, (long) (dInvExpStar + dInvSubStar), loanDebt, 0));
					
			} // else: because payment is monotically decreasing in investment, leave dInvSubStar = 0
		}
		
		/* if the previous round of adjustment was not sufficient (or not possible), then c-firms consider reducing their expansionary investment, if possible. The structure of the 
		adjustment is identical to the previous one */
		if(dInvExpStar > 0. && !exitAdjustment){
			
			//NOTE: if c-firms reach this round, then de facto dInvSubStar = 0
			dInvExpStar = 0.;
			if(cFirm.getBalanceSheet().payment(dQStar, 0l, loanDebt, 0.) >= 0.){
				
				double priceMachine = cFirm.getSupplier().getPriceOfGoodProducedNow();
				double sales = cFirm.getPriceOfGoodProducedNow() * Math.min(dQStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				// closed-form expression for the 'optimal' level of sub. exp.
				dInvExpStar = Parameters.getMachineSizeInCapital_cFirms() / (priceMachine * (1. + param1) ) * ( (cFirm.getLiquidAsset()[0] - dQStar * cFirm.getCostToProduceGood() ) * (1. + param1) + 
						loanDebt * (1. - param2) + (1. - MacroModel.getTax()) * sales - param2 * cFirm.getDebt()[0] );
				dInvExpStar = Math.floor(dInvExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				
				exitAdjustment = true;
				log.debug("Adjustments through inv. exp. possible. New level of inv. exp = " + dInvExpStar + " and payment " + cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, 0.));
				
			} // else: because payment is monotically decreasing in investment, leave dInvSubStar = 0
			
		}
		
		/* The following rounds are based on production adjustment. To the contrary on investment, payment is not necessarily decreasing in production. Specifically, the first derivative
		 of payment with respect to production depends on the (net) return to production relative to the (net) return on deposit. 
		 However, recall that sales = p * min (expected demand, stock of final good), and that the stock of final good desired by c-firms is always above the expected demand, because
		 c-firms want to have some inventories (equation (19) in Dosi et derivation in the code documentation pdf). It follows that as long as the stock of final good (which is a function
		 of the current production) is above the expected demand, the payment function is decreasing in quantity produced, the intuition being that the (expected) inventories are expected
		 not to be sold. Hence c-firms only bear the cost of their production in the current period. 
		 Therefore, the first way for firms to adjust is to reduce their desired inventories, and this is independent on the net return to production relative to the return to deposit.
		 
		 NOTE: As before, because production has originally been funded through internal funds, its reduction correspond to an increase in deposit.
		 NOTE bis: if c-firms reach the following rounds, then de facto investment = 0.
		 */
		
		if(!exitAdjustment){
			// It is not certain that the expected demand is greater than the current stock of inventories (in which case optimal production is equal to 0 and adjustments are not feasible)
			if(cFirm.getExpectedDemand() > cFirm.getInventories()[0]){
				// It is not certain that the achievable level of production yields a stock of final good greater than the expected demand. Thus have to check first whether this condition is met 
				if(dQStar + cFirm.getInventories()[0] > cFirm.getExpectedDemand()){
					
					/* As before, in order to invoke the mean value theorem, need to check whether the payment condition is satisfied at the point where the stock of final good is equal to
					the expected demand */
					dQStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
					if(cFirm.getBalanceSheet().payment(dQStar, 0l, loanDebt, 0.) >= 0.){
						/* Then can invoke the mean value theorem to find an interior solution. This interior solution is such that the stock of goods will be greater than the expected demand,
						 yet smaller than the initial stock of final good, and makes the payment = 0
						 NOTE: the interior solution is to the right (in a production - payment plane) of the point where the stock of final good = expected demand. Hence 
						 sales = p(t) * expected demand (t)
						 */
						
						dQStar = 1 / (cFirm.getCostToProduceGood() * (1 + param1)) * ( cFirm.getLiquidAsset()[0] * (1 + param1) + (1 - param2) * loanDebt +
								(1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand() - param2 * cFirm.getDebt()[0] );
						exitAdjustment = true;
						
						log.debug("Could adjustment on inventories. New production = " + dQStar + " and payment -->0 : " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, 0)) ;
						
						
					} // else: reducing production up to the point where the stock of final good is equal to the expected demand is not sufficient. Hence require the final round of adjustment	
				} // else: dQStar + cFirm.getN()[0] <= cFirm.geteD(), i.e. the firm had to scale down its production plan earlier and was not planning to produce any inventories anyway. 
				
			} else { /* cFirm.geteD() < cFirm.getN()[0], i.e. the firm's current production is aimed only at increasing the stock of inventories, if positive. That is to say, if dQStar > 0,
			 	then it is possible for c-firms to come closer to the payment condition by reducing their production (i.e. reducing their expected stock of inventories). Otherwise, it 
			 	is impossible to adjust further
			 	*/
				if(dQStar > 0){
					
					/* As before, we are trying to invoke the mean value theorem. The only difference now is that the level of production such that the stock of final good = expected demand
					 is nil */
					dQStar = 0;
					if(cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, 0) >= 0){
						// Then can invoke the mean value theorem. Once more, sales in this case are a function of the expected demand, not the production. 
						
						dQStar = 1 / (cFirm.getCostToProduceGood() * (1 + param1)) * ( cFirm.getLiquidAsset()[0] * (1 + param1) + (1 - param2) * loanDebt +
								(1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand() - param2 * cFirm.getDebt()[0] );
						exitAdjustment = true;
						
						log.debug("Inventories are > expected demand, yet dQStar > 0. Hence could reduce production to " + dQStar + " and payment --> 0: " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, 0));
						
					} else {
						/* This else statement is different from the above case (when expected demand > inventories), because at this point, production is nil, investment is nil, and the firm cannot
						 meet the condition. At this point we are making the assumption that firms do not want to leverage further when they are in this situation, and therefore set their credit 
						 demand = 0
						 */
						
						loanProd = loanDebt = 0;
						exitAdjustment = true;
						
						log.debug("The expected demand < inventories and setting production = 0 is not sufficient. Therefore do not leverage further");
						
					}
				} else {
					/* The firm was already planning to not produce anything. This scenario is identical to the else statement above: because the firm cannot reach the condition with nil production
					 and nil investment, it decides to not leverage further
					 */
					
					dQStar = 0;
					loanProd = loanDebt = 0;
					exitAdjustment = true;
					log.debug("The expected demand < inventories and setting production = 0 is not sufficient. Therefore do not leverage further");
					
				}
			}
		}
		
		/* The final round of adjustment concerns decreasing production when sales are a function of production, i.e. sales(t) = p(t) * (q(t) + n(t)). This time, the adjustment
		depends on the net return to production relative to the net return to deposit */
		if(!exitAdjustment){
			
			/* This adjustment involves two possible scenarios:
			 		(a) the return to production is relatively lower than the return to deposits. Hence decreasing production to increase the amount of internal funds increase
			 		the payment function, and the firm might be able to find an optimal level of production such that payment = 0
			 		(b) the return to production is relatively bigger than the return to deposits. In this case, any reduction of production would further decrease the expected
			 		payment (because it reduces the firm's sales), and pushes the firm further away from the payment constraint. Hence in this case the firm cannot adjust through
			 		production reduction
			Whether the firm is in case (a) or (b) depends on the first derivative of payment with respect to production
			 */
			
			// (a)
			if(cFirm.getPriceOfGoodProducedNow() < cFirm.getCostToProduceGood() * (1 + param1) / (1 - MacroModel.getTax())){
				/* Payment is monotically decreasing in production. Hence there exists a level of production such that payment = 0. However, it is not certain than 
				this level of production is positive */
				
				dQStar = 1 / ( (1 - MacroModel.getTax()) * ( cFirm.getPriceOfGoodProducedNow() - MacroModel.getrDepo() * cFirm.getCostToProduceGood() ) - cFirm.getCostToProduceGood() ) * (
						param2 * cFirm.getDebt()[0] - (1 + param1) * cFirm.getLiquidAsset()[0] - (1 - param2) * loanDebt - 
						(1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] );
				
				log.debug("Payment is decreasing in production and optimal production is = " + dQStar);
				if(dQStar > 0){
					log.debug("Through reduction in production, payment should --> 0: " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, 0));
				} else {
					/* The optimal level of production is negative and therefore not reachable. As before, (a) the firm tries to minimize its losses, (b) the firm does not
					leverage further. Because payment is monotically decreasing in production, the level of production that minimizes the loss is 0
					 */
					
					dQStar = 0;
					loanDebt = loanProd = 0;
					
				}
			} // (b) 
			else {
				/* No adjustment are feasible. Once more, the firm (a) tries to minimize its loss, (b) does not leverage further. The level of production that would
				 min. the firm's loss is the level of production such that the stock of final good is equal to the expected demand, if reachable with internal funds. 
				 */
				
				dQStar = Math.max(0, cFirm.getExpectedDemand() - cFirm.getInventories()[0]);
				// This as a cost of production of c * dQStar, and c * dQStar <= liquidAsset[0]: 
				if(cFirm.getCostToProduceGood() * dQStar > cFirm.getLiquidAsset()[0]){
					dQStar = cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood();
				}
				
				loanProd = loanDebt = 0;
				log.debug("Firm's payment is increasing in production. Therefore pointless to adjust downard production, and dQStar = " + dQStar);
				
			}
		}
		
		// Update the c-firms variables with the outcomes of the adjustment
		cFirm.setDesiredProductionStar(dQStar);
		cFirm.setDesiredInvestmentExpansionaryStar(dInvExpStar);
		cFirm.setDesiredInvestmentSubstitionaryStar(dInvSubStar);
		cFirm.setLoanForDebtRepayment(loanDebt);
		cFirm.setLoanForProductionAndInvestment(loanProd);
		cFirm.setCreditDemand(loanProd + loanDebt);
		
	}
	
	public static void adjustmentsWithNilLiquidAsset(CFirm cFirm){
		
		/* SUB-INTRODUCTORY NOTE: the general structure of this adjustment process follows closely the adjustment when liquid asset are positive. The only differences is that 
		 loan was involved in the funding of production and / or investment. Hence, when decreasing one or the other, the firm has to ask itself whether the decrease in production /
		 investment is going to increase the loan used in debt repayments, or the deposit. Hence more cases has to be considered in this adjustment.
		 Once more, all the details, intuitions and math derivations are in the code documentation pdf.
		 
		 NOTE: just the novelty of this adjustment process are commented. For the general structure, see the adjustmentsWithPositiveLiquidAsset method
		 */
		
		log.debug("Adjustments With Nil Liquid Asset for CFirm " + cFirm.getKey().getId());
		
		// initialize the static variables with the initial values of production, investment and loan (i.e. before any adjustment)
		dQStar = cFirm.getProductionStar();
		dInvExpStar = cFirm.getdInvestmentExpansionaryStar();
		dInvSubStar = cFirm.getDesiredInvestmentSubstitionaryStar();
		liquidAssetPrime = cFirm.getLiquidAssetRemainingAfterProductionAndInvestment();
		loanProd = cFirm.getLoanForProductionAndInvestment();
		loanDebt = cFirm.getLoanForDebtRepayment();
		
		// collection of parameters to ease the computation of closed-form expression
		double param1 = (1 - MacroModel.getTax()) * MacroModel.getrDepo(); // net interest rate, i.e. deducting from the government's tax on profit
		double param2 = Parameters.getDebtRepaymentSharePerPeriod_cFirms() + (1 - MacroModel.getTax()) * MacroModel.getrDebt(); /* net debt service, i.e. debt repayments + 
		interest rate on the debt, taking into account the government's tax on profit */
		double machinePrice = cFirm.getSupplier().getPriceOfGoodProducedNow();
		
		/* because compute closed-form solutions, the payment after adjustments tend to 0 but might be slightly different from 0. 
		To make sure that no-more-than-needed adjustments will be done, create this boolean */
		boolean exitAdjustment = false;
		
		// adjustment through inv. sub., if possible
		if(dInvSubStar > 0){
			/* Global strategy: we first look at the expected financial position of the firm when dInvSubStar = 0. If it is positive, it implies that there exists an interior solution
			 for inv. sub. Yet, do not if this optimal level of inv. sub. is achievable only with internal funds, or if it requires borrowing. To know this, we look at the firm's financial 
			 position when sub. inv. are funded only through internal funds, if possible. Then:
					(1) if payment() > 0 at this point, the interior solution is to the right of this point, i.e. funded through loan
					(2) if payment() < 0, then the optimal level of inv. sub. is to the left of this and is funded through interna funds. 
		 	A graphical representation is presented in the code documentation pdf, making it easier to see the intuition 
		 	NOTE: an identical strategy is followed for dInvExpStar 
		 	*/
			
			// If set dInvSubStar = 0, then the associated savings are equal to the number of machines that were in the sub. inv, times the price of these machiens
			double savings = machinePrice * dInvSubStar / Parameters.getMachineSizeInCapital_cFirms();
			saving(savings, cFirm); // see the saving() method for explenation on what it does
			dInvSubStar = 0;
			
			if(cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, loanProd) > 0){
				/* Then can invoke the mean value theorem; and can enter the second phase of the adjustment through inv. sub. when we have to find whether the optimal level if funded
				through internal or external fund */
				
				double salesTemp = cFirm.getPriceOfGoodProducedNow() * Math.min(dQStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				if(loanProd > 0){
					/* if loan prod is positive when dInvSubStar, it implies than any positive amount of sub. inv. will also be funded through loan. Hence we know that liquidAssetPrime = 0
					 and that the entire amount of sub. inv. will be funded through external fund
					 */
					
					dInvSubStar = Parameters.getMachineSizeInCapital_cFirms() / machinePrice * ( cFirm.getLiquidAsset()[0] + (1 - param2) * cFirm.getMaxPossibleLoan() + 
							(1 - MacroModel.getTax()) * salesTemp - cFirm.getCostToProduceGood() * dQStar - param2 * cFirm.getDebt()[0] - 
							machinePrice * dInvExpStar / Parameters.getMachineSizeInCapital_cFirms() );
					
					// update financial variables with the new level of inv.
					loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar + (dInvExpStar + dInvSubStar) * machinePrice / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
					loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
					liquidAssetPrime = 0;
					
					log.debug("Can adjust through inv. sub.; new level of inv. = " + dInvSubStar + " and payment should --> 0 " + 
							cFirm.getBalanceSheet().payment(dQStar, (long) (dInvExpStar + dInvSubStar), loanDebt, loanProd));
					
					// because inv. need to be expressed in terms of machines, additional savings have to be considered
					savings = (dInvSubStar - Math.floor(dInvSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
					saving(savings, cFirm);
							
				} /* else, have to use the strategy presented above. Indeed, loanProd = 0 implies that the firm can use some of its internal fund to finance sub. inv.;
				yet it is not certain that the optimal level can be obtained solely through the firm's liquid asset */
				else {
					// compute how much assets remain after paying for production and inv. exp.
					double liquidAssetUsable = cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * dQStar - machinePrice * dInvExpStar / Parameters.getMachineSizeInCapital_cFirms();
					// and then compute the level of sub. inv. that can be achieved only by using internal funds. Obviously in this case, all the loan is used for debt repayments
					dInvSubStar = Math.min(cFirm.getDesiredInvestmentSubstitionaryStar(), liquidAssetUsable * Parameters.getMachineSizeInCapital_cFirms() / machinePrice);
					loanDebt = cFirm.getMaxPossibleLoan();
					loanProd = 0;
					
					// (1)
					if(cFirm.getBalanceSheet().payment(dQStar, (long) (dInvExpStar + dInvSubStar), loanDebt, loanProd) > 0){
						/* the optimal level of sub. inv. is marginally funded through loan, s.t. loanProd > 0 and loanDebt < maxLoan. The closed-form solution, and the corresponding
						 level of loan and liquid asset remaining are identical to the ones above */
						
						dInvSubStar = ( Parameters.getMachineSizeInCapital_cFirms() / machinePrice ) * ( 
								cFirm.getLiquidAsset()[0] + (1 - param2) * cFirm.getMaxPossibleLoan() + (1 - MacroModel.getTax()) * salesTemp - 
								cFirm.getCostToProduceGood() * dQStar - param2 * cFirm.getDebt()[0] - machinePrice * dInvExpStar / Parameters.getMachineSizeInCapital_cFirms() );
						
						// update financial variables with the new level of inv.
						loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar + (dInvExpStar + dInvSubStar) * machinePrice / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
						loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
						liquidAssetPrime = 0;
						
						log.debug("Can adjust through inv. sub.; new level of inv. = " + dInvSubStar + " and payment should --> 0 " + 
								cFirm.getBalanceSheet().payment(dQStar, (long) (dInvExpStar + dInvSubStar), loanDebt, loanProd));
						
						// because inv. need to be expressed in terms of machines, additional savings have to be considered
						savings = (dInvSubStar - Math.floor(dInvSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
						
					} else {
						/* the optimal level of inv. sub. is achieved only using internal funds, s.t. loanProd = 0, loanDebt = maxLoan and the remaining liquid assets 
						 are positive.  */
						
						dInvSubStar = Parameters.getMachineSizeInCapital_cFirms() / (machinePrice * ( 1 + param1) ) * ( (1 - MacroModel.getTax()) * salesTemp + 
								(cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * dQStar - machinePrice * dInvExpStar / Parameters.getMachineSizeInCapital_cFirms() )  * ( 1 + param1) + (1 - param2) * loanDebt - 
								param2 * cFirm.getDebt()[0]);
						
						// update financial variables with the new level of inv.
						loanProd = 0;
						loanDebt = cFirm.getMaxPossibleLoan();
						liquidAssetPrime = Math.max(0, - cFirm.getCostToProduceGood() * dQStar - (dInvExpStar + dInvSubStar) * machinePrice / Parameters.getMachineSizeInCapital_cFirms() + cFirm.getLiquidAsset()[0]);
						
						log.debug("Can adjust through inv. sub.; new level of inv. = " + dInvSubStar + " and payment should --> 0 " + 
								cFirm.getBalanceSheet().payment(dQStar, (long) (dInvExpStar + dInvSubStar), loanDebt, loanProd));
						
						// because inv. need to be expressed in terms of machines, additional savings have to be considered
						savings = (dInvSubStar - Math.floor(dInvSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
					}
				}
				
				exitAdjustment = true;
				dInvSubStar = Math.floor(dInvSubStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				
			}
		}
		
		// adjustment through inv. exp., if possible. The structure is identical to the one for adjustment through inv. sub., only the closed-form expressions vary
		if(dInvExpStar > 0 && !exitAdjustment){
			
			double savings = machinePrice * dInvExpStar / Parameters.getMachineSizeInCapital_cFirms();
			saving(savings, cFirm); 
			dInvExpStar = 0;
			
			if(cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd) > 0){
				
				double salesTemp = cFirm.getPriceOfGoodProducedNow() * Math.min(dQStar + cFirm.getInventories()[0], cFirm.getExpectedDemand());
				
				if(loanProd > 0){
					
					dInvExpStar = Parameters.getMachineSizeInCapital_cFirms() / machinePrice * ( cFirm.getLiquidAsset()[0] + (1 - param2) * cFirm.getMaxPossibleLoan() + 
							(1 - MacroModel.getTax()) * salesTemp - cFirm.getCostToProduceGood() * dQStar - param2 * cFirm.getDebt()[0] );
					
					loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar + dInvExpStar * machinePrice / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
					loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
					liquidAssetPrime = 0;
					
					log.debug("Can adjust through inv. exp.; new level of inv. = " + dInvExpStar + " and payment should --> 0 " + 
							cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, loanProd));
					
					savings = (dInvExpStar - Math.floor(dInvExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
					saving(savings, cFirm);
							
				} 
				else {
					double liquidAssetUsable = cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * dQStar;

					dInvExpStar = Math.min(cFirm.getdInvestmentExpansionaryStar(), liquidAssetUsable * Parameters.getMachineSizeInCapital_cFirms() / machinePrice);
					loanDebt = cFirm.getMaxPossibleLoan();
					loanProd = 0;
					
					if(cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, loanProd) > 0){
						
						dInvExpStar = ( Parameters.getMachineSizeInCapital_cFirms() / machinePrice ) * ( 
								cFirm.getLiquidAsset()[0] + (1 - param2) * cFirm.getMaxPossibleLoan() + (1 - MacroModel.getTax()) * salesTemp - 
								cFirm.getCostToProduceGood() * dQStar - param2 * cFirm.getDebt()[0] );
						
						loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar + dInvExpStar * machinePrice / Parameters.getMachineSizeInCapital_cFirms() - cFirm.getLiquidAsset()[0]);
						loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
						liquidAssetPrime = 0;
						
						log.debug("Can adjust through inv. exp.; new level of inv. = " + dInvExpStar + " and payment should --> 0 " + 
								cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, loanProd));
						
						savings = (dInvExpStar - Math.floor(dInvExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
						
					} else {
						
						dInvSubStar = Parameters.getMachineSizeInCapital_cFirms() / (machinePrice * ( 1 + param1) ) * ( (1 - MacroModel.getTax()) * salesTemp + 
								(cFirm.getLiquidAsset()[0] - cFirm.getCostToProduceGood() * dQStar )  * ( 1 + param1) + (1 - param2) * loanDebt - 
								param2 * cFirm.getDebt()[0]);
						
						loanProd = 0;
						loanDebt = cFirm.getMaxPossibleLoan();
						liquidAssetPrime = Math.max(0, - cFirm.getCostToProduceGood() * dQStar - dInvExpStar * machinePrice / Parameters.getMachineSizeInCapital_cFirms() + cFirm.getLiquidAsset()[0]);
						
						log.debug("Can adjust through inv. exp.; new level of inv. = " + dInvExpStar + " and payment should --> 0 " + 
								cFirm.getBalanceSheet().payment(dQStar, (long) dInvExpStar, loanDebt, loanProd));
						
						savings = (dInvExpStar - Math.floor(dInvExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms()) / Parameters.getMachineSizeInCapital_cFirms() * machinePrice;
						saving(savings, cFirm);
					}
				}
				
				exitAdjustment = true;
				dInvExpStar = Math.floor(dInvExpStar / Parameters.getMachineSizeInCapital_cFirms()) * Parameters.getMachineSizeInCapital_cFirms();
				
			}
		}
		
		// adjustment through reduction in inventories
		if(!exitAdjustment){
			if(cFirm.getExpectedDemand() > cFirm.getInventories()[0]){
				if(dQStar + cFirm.getInventories()[0] > cFirm.getExpectedDemand()){
					/* adjustment by reduction of (expected) inventories is feasible. The difference with the adjustmentWithPositiveLiquidAsset method is that here, once more, 
					we are not sure if the optimal level of production is achieved using internal or external funds. */
					
					// 1. Compute first the financial variables when production is such that the stock of final good = expected demand
			        double savings = (dQStar + cFirm.getInventories()[0] - cFirm.getExpectedDemand()) * cFirm.getPriceOfGoodProducedNow();
			        dQStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0]; 
			        saving(savings, cFirm);
			        
			        // 2. Check whether this reduction in production is sufficient to meet the payment condition, to then being able to invoke the mean value theorem
			        if(cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd) > 0){
			        	/* Global strategy: let q** denote the level of production achieved solely with internal funds. 
			        	 	1. if q** < dQStar, then implies that the optimal production is achieved by using external funds
			        	 	2. if q** > dQStar,
			        	 		(a) if (payment | lProd = 0) > 0, then implies that optimal level of production is achieved with external funds
			        	 		(b) if (payment | lProd = 0) > 0, then implies that optimal level of production is achieved using internal funds
			        	 NOTE: once more, for a graphical representation of this, see the code documentation pdf.
			        	 */
			        	
			        	double qLiquidAsset = Math.min(cFirm.getdQ(), cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood()); // = q**
			        	double salesTemp = cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand();
			        	loanDebt = cFirm.getMaxPossibleLoan();
			        	loanProd = 0;
			        	
			        	// 1: 
			        	if(qLiquidAsset < dQStar){
			        		// optimal production is achieved with the help of external fund, s.t. loanProd > 0, loanDebt < maxLoan
			        		
			        		dQStar = 1 / cFirm.getCostToProduceGood() * ( cFirm.getLiquidAsset()[0] + (1 - MacroModel.getTax()) * salesTemp + 
			        				(1 - param2) * cFirm.getMaxPossibleLoan() - param2 * cFirm.getDebt()[0]);
			        		
			        	} // 2: 
			        	else {
			        		// 2. (a)
			        		if(cFirm.getBalanceSheet().payment(qLiquidAsset, 0, loanDebt, loanProd) > 0){
			        			// optimal production is achieved with the help of external fund, s.t. loanProd > 0, loanDebt < maxLoan
				        		
				        		dQStar = 1 / cFirm.getCostToProduceGood() * ( cFirm.getLiquidAsset()[0] + (1 - MacroModel.getTax()) * salesTemp + 
				        				(1 - param2) * cFirm.getMaxPossibleLoan() - param2 * cFirm.getDebt()[0]);
			        			
			        		} // 2. (b)
			        		else {
			        			// optimal production is achieved entirely with internal funds, s.t. the entire loan is used for debt repayments 
			        			
			        			dQStar  = 1 / ( cFirm.getCostToProduceGood() * (1 + param1) ) * ( cFirm.getLiquidAsset()[0] * ( 1 + param1) + (1 - MacroModel.getTax()) * salesTemp + 
				        				(1 - param2) * cFirm.getMaxPossibleLoan() - param2 * cFirm.getDebt()[0]);
			        			
			        		}
			        	}
			        	
			        	// Regardless of the scenario, the interior solution exists. 
			        	exitAdjustment = true;
			        	// And can update the financial variables
			        	loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar - cFirm.getLiquidAsset()[0]);
			        	loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
			        	
			        	log.debug("Can reduce production through inventories; new production = " + dQStar + 
		        				" and payment should --> 0: " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd) );
			        	
			        } /* else: reduction production up to the level s.t. the stock of final good is equal to the expected demand is not sufficient to meet the payment condition. 
			        Need to go in the last round of adjustment */
			        
				} // else: stock of final good < expected demand, cannot adjust 
				
			} else { // expected demand < stock of inventories. 
				if(dQStar > 0){
					/* the desired production is aimed at filling inventories. Savings correspond to the amount of cash saved when set this production = 0
					 NOTE: the reasoning follows adjustmentWithPositiveLiquidAsset regarding how the firm can adjust their production in this scenario
					 */
					double savings = dQStar + cFirm.getCostToProduceGood();
					dQStar = 0;
					double salesTemp = cFirm.getPriceOfGoodProducedNow() * cFirm.getExpectedDemand();
					saving(savings, cFirm);
					
					// We check whether the payment condition is satisfied at this point, in order to evoke the mean value theorem
					if(cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd) > 0){
						/* Thus there exists an interior solution. The question is to know whether this optimal level is reached using only internal funds or 
						 also by borrowing external funds.
						 To know this, as above, we compute the level of production that can be achieved using only internal funds. If at this point, the
						 payment > 0, then it implies that the optimal level is reached by using external fund (a). Otherwise, only through the firm's liquid asset (b).
						 NOTE: we do not have to check that the optimal production is positive; the above if condition is sufficient to guarantee it. 
						 See code documentation pdf for more details */
						
						double qLiquidAsset = Math.min(cFirm.getdQ(), cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood());
						loanProd = 0;
						loanDebt = cFirm.getMaxPossibleLoan();
						
						// (a)
						if(cFirm.getBalanceSheet().payment(qLiquidAsset, 0, loanDebt, loanProd) > 0){
							
							dQStar = 1 / cFirm.getCostToProduceGood() * ( cFirm.getLiquidAsset()[0] + (1 - MacroModel.getTax()) * salesTemp + 
			        				(1 - param2) * cFirm.getMaxPossibleLoan() - param2 * cFirm.getDebt()[0]);
							
						} // (b)
						else {
							
							dQStar = 1 / (cFirm.getCostToProduceGood() * (1 + param1)) * ( cFirm.getLiquidAsset()[0] * ( 1 + param1) + (1 - MacroModel.getTax()) * salesTemp + 
			        				(1 - param2) * cFirm.getMaxPossibleLoan() - param2 * cFirm.getDebt()[0]);
			        				
						}
						
						// update the financial variables
						loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar - cFirm.getLiquidAsset()[0]);
						loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
						exitAdjustment = true;
						
						log.debug("Can reduce production through inventories; new production = " + dQStar + 
		        				" and payment should --> 0: " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd) );
	
					} else {
						/* Even with dQStar = 0, the firm cannot reach its payment condition. As in adjustmentWithPositiveLiquidAsset(), c-firms try to minimize their losses
						and to not leverage further. Hence they opt out for a nil production, and a nil credit demand. 
						 */
						
						dQStar = loanProd = loanDebt = 0;
						exitAdjustment = true;
						
					}
				} else {
					/* The firm was already planning to not produce anything. Because it cannot meet the payment condition, and it has no variable left to adjust with,
					it minimizes its loss as well */ 
					
					dQStar = loanProd = loanDebt = 0;
					exitAdjustment = true;
					
				}
			}
		}
		
		/* adjustment through reduction in production. Recall from adjustmentWithPositiveLiquidAsset(): sales, in this final adjustment, are now a function of the 
		quantity produced; whether payment is increasing / decreasing in production depends on the net return to production compare to the net return to deposits;
		for closed-form expressions, still have to know whether these optimal levels are reached entirely via internal financing or through external borrowing
		*/ 
		if(!exitAdjustment){
			// we first check whether the production, before adjustment, was funded through loan or internal liquid asset
			if(loanProd > 0){
				/* Part of the original production was planned to be funded through external fund; thus the source of the funding to achieve the optimal production matters
				 (as in the previous adjustment). 
				 However, whether or not this optimal production exists depend, in the first place, on the relative return to the firms' activities. There
				is however here a difference with the adjustment in adjustmentWithPositiveLiquidAsset(): the payment function is non continuous as its slope
				changes when loanProd becomes equal to 0. See pdf documentation for more explenation and graphical representation  */
				
				if((1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() < cFirm.getCostToProduceGood()){
					/* The payment function is gloablly monotically decreasing in the firm's production (this condition implies a negative slope as well when loanProd = 0). 
					Hence there might an interior solution for payment = 0. The global strategy is the following:
						1. Check whether payment > 0 when produce at the level that can funded entirely thought internal funds. If it the case, then, because payment 
						is decreasing in q, then the optimal solution is located to the right of this point, and the optimal production is marginally funded through
						loan. furthermore, we can be certain that this optimal production is positive
						2. If payment < 0, then the optimal production is funded solely via internal funds. Yet, it is not sure that this optimal production is positive,
						i.e. that there exists an interior solution
					*/
					
					// Locate ourselves at the point where the production is entirely funded through internal funds.
					loanDebt = cFirm.getMaxPossibleLoan();
					loanProd = 0;
					dQStar = Math.min(cFirm.getdQ(), cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood());
					
					// 1.:
					if(cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd) > 0){
						
						dQStar = 1 / ( (1 - MacroModel.getTax() ) * cFirm.getPriceOfGoodProducedNow() - cFirm.getCostToProduceGood() ) * (param2 * cFirm.getDebt()[0] - 
								(1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - 
								(1 - param2) * cFirm.getMaxPossibleLoan() - cFirm.getLiquidAsset()[0]);
						
						// update financial variables:
						loanProd = Math.max(0, cFirm.getCostToProduceGood() * dQStar - cFirm.getLiquidAsset()[0]); // should be positive
						loanDebt = cFirm.getMaxPossibleLoan() - loanProd;
						
						log.debug("Payment is decreasing in production & there is an interior solution; "
								+ "the new level of production is : " + dQStar + " and the associated payment = " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd));
						
					} // 2.:
					else {
						
						dQStar = 1 / ((1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() - (1 + param1) * cFirm.getCostToProduceGood() ) * (
								param2 * cFirm.getDebt()[0] - (1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - (1 - param2) * cFirm.getMaxPossibleLoan() - 
								cFirm.getLiquidAsset()[0] * (1 + param1)
								);
						// However, as explained above, it is not sure that this optimal solution is positive.
						if(dQStar < 0){
							// There does not exist a way out for the firm. It tries to minimize its loss and to not leverage further; hence its set is production, and its credit demand, nil
							loanProd = loanDebt = dQStar = 0;
							
							log.debug("Payment is decreasing in production but there is not an interior solution");
						} else {
							// all the loan is used to fund debt repayments
							loanProd = 0; 
							loanDebt = cFirm.getMaxPossibleLoan();
							
							log.debug("Payment is decreasing in production & there is an interior solution; "
									+ "the new level of production is : " + dQStar + " and the associated payment = " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd));
						}	
					}
					
				} else { /*  As said earlier, because the payment condition features a break in its slope at loanProd = 0, we have to check whether the partial derivative is now positive or negative
					Intuition: in the previous if statement, decrease of production --> savings, that are reallocated to either loanDebt or deposit. Because loanDebt yields a return < 1 (a fraction of the loan
					will have to be repaid at the end of the period anyway), and deposit yield a return >= 1, the first condition implies the second. However, it could be that the return to production is bigger
					than the return to loanDebt, yet smaller than the return to deposit.  */
					if(cFirm.getPriceOfGoodProducedNow() * (1 - MacroModel.getTax()) < cFirm.getCostToProduceGood() * ( 1 + param1)){
						// The payment function is monotically decreasing in production: there might exist an interior solution. (a) compute the closed-form solution, (b) check whether it is positive
						
						//(a)
						dQStar = 1 / ((1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() - (1 + param1) * cFirm.getCostToProduceGood() ) * (
								param2 * cFirm.getDebt()[0] - (1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - (1 - param2) * cFirm.getMaxPossibleLoan() - 
								cFirm.getLiquidAsset()[0] * (1 + param1)
								);
						
						//(b)
						if(dQStar > 0){
							
							// interior solution. Because this level of production is achieved only through internal funds, all the loan is allocated to debt repayments
							loanDebt = cFirm.getMaxPossibleLoan();
							loanProd = 0;
							log.debug("There exits an interior solution: new production is " + dQStar + " and payment " + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, loanProd));
							
						} else {
							/* there does not exist an interior solution. The firm minimizes its loss and does not leverage further. However, because of the break in the function, 
							this function features two local max (see pdf). Hence the firm has to compare which one yields the payment the closest to 0 */
							
							loanProd = loanDebt = 0;
							
							// set dQStar at the point where the stock of final good = expected demand
							if(cFirm.getExpectedDemand() - cFirm.getInventories()[0] > 0)  // i.e. with no inventories
								dQStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
							else 
								dQStar = 0;
							// and check whether this point can be reach only using internal funds (because the firm is trying to not increase its debt)
							if(cFirm.getCostToProduceGood() * dQStar > cFirm.getLiquidAsset()[0] ){ 
								// if dQStar is not possible with internal resources, then scale down to what is achievable 
								dQStar = cFirm.getLiquidAsset()[0]  / cFirm.getCostToProduceGood();
							}
							
							// finally compare the two local maxima: (a) when production is nil, (b) when production is at dQStar, and pick the ``highest''
							if(cFirm.getBalanceSheet().payment(0, 0, 0, 0) > cFirm.getBalanceSheet().payment(dQStar, 0, 0, 0)){
								// not producing anything minimizes the loss
								dQStar = 0;
							} // else: keep dQStar
							
							log.debug("There does not exist an interior solution, even if payment had two local max");
							
						}
					} else {
						/* The payment function is globally increasing in production; there does not exist a way out for the firm. The firm minmizes its loss, i.e. set production at the level
						where the stock of good = expected demand, if achievable, and is credit demand is nil */
						
						dQStar = cFirm.getExpectedDemand() - cFirm.getInventories()[0];
						if(cFirm.getCostToProduceGood() * dQStar > cFirm.getLiquidAsset()[0])
							dQStar = cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood();
						
						loanProd = loanDebt = 0;
						log.debug("There does not exist an interior solution, payment is globally increasing in q");
						
						
						
					}
				}
			} else {
				/* Production, before adjustment, was already funded entirely through internal fund. Hence, because the adjustment process looks only to decrease
				production to bring the firm closer to its payment condition, the only things that can happen is that the amount of deposit will increase. Said differently,
				loanProd will remain at 0. Hence this adjustment is identical to the final round of adjustment in adjustmentWithPositiveLiquidAsset(). For explanation on this,
				see  adjustmentWithPositiveLiquidAsset() */
				
				
				if(cFirm.getPriceOfGoodProducedNow() < (1 + param1) / (1 - MacroModel.getTax()) * cFirm.getCostToProduceGood() ){
					
					dQStar = 1 / ( (1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() - (1 + param1) * cFirm.getCostToProduceGood() )  * (
							param2 * cFirm.getDebt()[0] - (1 + param1) * cFirm.getLiquidAsset()[0] - 
							(1 - MacroModel.getTax()) * cFirm.getPriceOfGoodProducedNow() * cFirm.getInventories()[0] - (1 - param2) * cFirm.getMaxPossibleLoan()
							);
					
					if(dQStar > 0){
						
						loanProd = 0;
						loanDebt = cFirm.getMaxPossibleLoan();
						
						log.debug("There exists an interior solution for optimal production (with lProd = 0); dQStar : " + dQStar + " and payment --> 0 :" + cFirm.getBalanceSheet().payment(dQStar, 0, loanDebt, 0));
						
					} else {
						
						loanProd = loanDebt = dQStar = 0;
						log.debug("The optimal solutino is below 0; no corner solution");
						
					}
				} else {
					
					dQStar = Math.max(0, cFirm.getExpectedDemand() - cFirm.getInventories()[0]); 
					if(cFirm.getCostToProduceGood() * dQStar > cFirm.getLiquidAsset()[0]){ 
						dQStar = cFirm.getLiquidAsset()[0] / cFirm.getCostToProduceGood();
					}
					loanProd = loanDebt = 0;
					
					log.debug("Payment is increasing in q");
					
				}
			}
		}
		
		// Update the c-firms variables with the outcomes of the adjustment
		cFirm.setDesiredProductionStar(dQStar);
		cFirm.setDesiredInvestmentExpansionaryStar(dInvExpStar);
		cFirm.setDesiredInvestmentSubstitionaryStar(dInvSubStar);
		cFirm.setLoanForDebtRepayment(loanDebt);
		cFirm.setLoanForProductionAndInvestment(loanProd);
		cFirm.setCreditDemand(loanProd + loanDebt);
		
	}
	
	/* the saving method re-allocate a given amount, cash, coming from a reduction in investment or production, to the original source of fund. That is if the expenditure
	was originally funded through internal funds, it increases the firm's deposit. If it was funded through loan, it increases the amount of loan used for debt repayments */ 
	static void saving (double cash, CFirm firm) {
		
		if(loanProd > cash){
			
			loanProd -= cash;
			loanDebt += cash;
			
		} else if (loanProd > 0){
			
			loanDebt += loanProd;
			liquidAssetPrime += cash - loanProd;
			loanProd = 0;
			
		} else {
			
			liquidAssetPrime += cash;
			loanProd = 0;
			loanDebt = firm.getMaxPossibleLoan();
			
		}
	}

}
