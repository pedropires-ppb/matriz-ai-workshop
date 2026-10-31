package com.matriz;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains an {@link EnvironmentVariablesException} in the start-up failure report. */
class EnvironmentVariablesFailureAnalyzer extends AbstractFailureAnalyzer<EnvironmentVariablesException> {

	static final String ACTION = "Set each variable in the environment, or in the file .env in the directory "
			+ "from which Matriz is started: copy .env.example to .env and fill it in. See README.md.";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, EnvironmentVariablesException cause) {
		return new FailureAnalysis("Matriz cannot start. " + cause.getMessage(), ACTION, cause);
	}

}
