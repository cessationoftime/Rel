/**
 * 
 */
package org.reldb.rel.vm.instructions.possrep;

import org.reldb.rel.exceptions.ExceptionSemantic;
import org.reldb.rel.values.Value;
import org.reldb.rel.values.ValueAlpha;
import org.reldb.rel.vm.Context;
import org.reldb.rel.vm.Instruction;

public final class OpPossrepGetComponent extends Instruction {

	private int offsetInValue;
	private String theComponentName;
	
	public OpPossrepGetComponent(String theComponentName, int offsetValue) {
		this.theComponentName = theComponentName;
		this.offsetInValue = offsetValue;
	}
	
	public final void execute(Context context) {
		Value v = ((ValueAlpha)context.pop()).getComponentValue(offsetInValue);
		if (v == null)
			throw new ExceptionSemantic("RS0280: The value for 'THE_" + theComponentName + "' is undefined.");
		context.push(v);
	}
	
	public String toString() {
		return super.toString() + " from " + theComponentName + " at offset " + offsetInValue;
	}
}