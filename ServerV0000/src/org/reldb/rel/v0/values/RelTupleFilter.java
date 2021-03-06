package org.reldb.rel.v0.values;

import org.reldb.rel.v0.vm.Context;
import org.reldb.rel.v0.vm.Operator;

public class RelTupleFilter extends TupleFilter {

	private Context context;
	private Operator filterOperator;
	
	public RelTupleFilter(Context context, Operator filterOperator) {
		this.context = context;
		this.filterOperator = filterOperator;
	}
	
	@Override
	public boolean filter(ValueTuple tuple) {
		context.push(tuple);
		context.call(filterOperator);
		return (((ValueBoolean)context.pop()).booleanValue());				
	}

}
