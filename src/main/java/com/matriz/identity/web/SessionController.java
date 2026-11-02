package com.matriz.identity.web;

import java.util.UUID;

import com.matriz.identity.application.SignIn;
import com.matriz.identity.application.SignOut;
import com.matriz.identity.application.SignedIn;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signing in and out (openapi.yaml, {@code signIn} and {@code signOut}; FR-013, FR-014).
 * <ul>
 * <li>Signing in is public. Its checks come in the order {@code 415}, {@code 400}, {@code 422},
 * {@code 401} (ADR-004, open point "Order of checks").</li>
 * <li>Signing out ends only the presented token; the security filter chain has already verified
 * it.</li>
 * </ul>
 */
@RestController
@RequestMapping(SecurityConfiguration.SESSION)
class SessionController {

	private final SignIn signIn;

	private final SignOut signOut;

	SessionController(SignIn signIn, SignOut signOut) {
		this.signIn = signIn;
		this.signOut = signOut;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<SignInResponse> signIn(@RequestBody SignInContent content) {
		SignInContent.Credentials credentials = content.credentials();
		SignedIn signedIn = this.signIn.signIn(credentials.loginName(), credentials.password())
			.orElseThrow(AccessProblems::incorrectCredentials);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(SignInResponse.of(signedIn));
	}

	@DeleteMapping
	ResponseEntity<Void> signOut(@AuthenticationPrincipal Jwt token) {
		this.signOut.signOut(UUID.fromString(token.getId()), token.getExpiresAt());
		return ResponseEntity.noContent().build();
	}

}
