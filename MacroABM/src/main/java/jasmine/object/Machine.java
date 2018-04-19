package jasmine.object;

import javax.persistence.Transient;

import jasmine.model.KFirm;

public class Machine implements Cloneable {
	
	// ---------------------------------------------------------------------
	// Variables
	// ---------------------------------------------------------------------
	
	@Transient
	double[] a; // price of the machine.  XXX: Is this really a price, or more like the productivity??? 
	@Transient
	double cost; // cost of the machine 
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
		this.a[0] 				= a[1];
	}
	
	// ---------------------------------------------------------------------
	// Getters / Setters 
	// ---------------------------------------------------------------------

	public double[] getA() {
		return a;
	}

	public void setA(double[] a) {
		this.a 				= a;
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
