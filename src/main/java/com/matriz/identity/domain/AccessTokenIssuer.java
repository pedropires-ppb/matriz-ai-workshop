package com.matriz.identity.domain;

import com.matriz.identity.PersonId;

/** Issues the signed access tokens of ADR-005 (decision 1; open point "Signing"). */
public interface AccessTokenIssuer {

	IssuedAccessToken issue(PersonId person);

}
