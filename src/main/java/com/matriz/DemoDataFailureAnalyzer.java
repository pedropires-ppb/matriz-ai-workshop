package com.matriz;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains a {@link DemoDataException} in the start-up failure report. */
class DemoDataFailureAnalyzer extends AbstractFailureAnalyzer<DemoDataException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, DemoDataException cause) {
		return new FailureAnalysis("Matriz cannot start. " + cause.getMessage(), cause.action(), cause);
	}

}
