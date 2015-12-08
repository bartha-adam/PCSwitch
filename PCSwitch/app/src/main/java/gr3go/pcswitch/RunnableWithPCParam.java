package gr3go.pcswitch;

public abstract class RunnableWithPCParam implements Runnable {

	PC pc;
	public RunnableWithPCParam(PC pc) {
        this.pc = pc;
	  }

}
