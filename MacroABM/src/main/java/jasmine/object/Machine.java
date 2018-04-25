package jasmine.object;

import javax.persistence.Transient;

import jasmine.model.KFirm;

public class Machine implements Cloneable {
	
	// ---------------------------------------------------------------------
	// Variables
	// ---------------------------------------------------------------------
	
	@Transient
	double[] machineProductivity; // price of the machine.  productivity of the machine.
	@Transient
	double cost; // cost of the machine, "Unit labor cost of production entailed by [the] machine" (Dosi et al. 2013, eq. 13.5). 
	@Transient
	double vintage; // vintage of the machine, this increases in increments of 1 whenever the KFirm innovates and creates a more efficient machine.
	@Transient
	KFirm madeBy;
	
	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------
	
	public Machine(){
	}
	
	public Machine(KFirm madeBy){
		this.madeBy 			= madeBy;
	}
	
	// ---------------------------------------------------------------------
	// Methods
	// ---------------------------------------------------------------------
	
    @Override
    public Machine clone(){
    	try{
    		Machine machine 	= (Machine)super.clone();
    		return machine;	
    	} catch(CloneNotSupportedException e){
    		return null;
    	} 
    }
	
	public void kill(){
		this.madeBy 			= null;
	}
	
	public void update(){
		this.machineProductivity[0] 				= machineProductivity[1];
	}
	
	// ---------------------------------------------------------------------
	// Getters / Setters 
	// ---------------------------------------------------------------------

	public double[] getMachineProductivity() {
		return machineProductivity;
	}

	public void setMachineProductivity(double[] a) {
		this.machineProductivity 				= a;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost 				= cost;
	}

	public double getVintage() {
		return vintage;
	}

	public void setVintage(double vintage) {
		this.vintage 			= vintage;
	}

	public KFirm getMadeBy() {
		return madeBy;
	}

	public void setMadeBy(KFirm madeBy) {
		this.madeBy 		= madeBy;
	}

}
