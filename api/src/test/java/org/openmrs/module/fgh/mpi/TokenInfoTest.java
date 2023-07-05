package org.openmrs.module.fgh.mpi;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;

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
	public void token_ShouldBeValid() {
		TokenInfo tokenInfo = new TokenInfo();

		tokenInfo.setTokenId(TOKEN_ID);
		tokenInfo.setAccessToken(ACCESS_TOKEN);
		tokenInfo.setRefreshToken(REFRESH_TOKEN);
		StopWatch stopWach = new StopWatch();
		stopWach.start();
		stopWach.split();

		tokenInfo.setExpiresIn((double) stopWach.getSplitTime());
		boolean isValidToken = tokenInfo.isValid();

		assertFalse(isValidToken);
	}
}
