package org.openmrs.module.fgh.mpi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, MpiUtils.class, FhirUtils.class })
public class TokenInfoTest {
	
	private static final String TOKEN_ID = "TOKEN-ID";
	
	private static final String ACCESS_TOKEN = "ACCESS-TOKEN";
	
	private static final String REFRESH_TOKEN = "REFRESH_TOKEN";
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(MpiUtils.class);
		PowerMockito.mockStatic(FhirUtils.class);
	}
	
	@Test
	public void isValid_tokenShouldBeValid() {
		TokenInfo tokenInfo = new TokenInfo();
		
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setExpiresIn(3l);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now().plus(15, ChronoUnit.MILLIS));
		boolean isValidToken = tokenInfo.isValid(LocalDateTime.now());
		
		assertTrue(isValidToken);
	}
	
	@Test
	public void isValid_tokenShouldBeInValid() throws InterruptedException {
		TokenInfo tokenInfo = new TokenInfo();
		
		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		tokenInfo.setExpiresIn(10l);
		tokenInfo.setTokenExpirationDateTime(LocalDateTime.now());
		boolean isValidToken = tokenInfo.isValid(LocalDateTime.now().plus(10, ChronoUnit.MILLIS));
		
		assertFalse(isValidToken);
	}
}
