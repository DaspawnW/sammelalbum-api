package com.daspawnw.sammelalbum;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.validation-codes=CODE-1111",
		"app.jwt.secret=K7gNU3kef8297wnsJvbdw/Ba49bmGW76NFh70fE0ZeM=",
		"app.jwt.expiration=86400000"
})
class SammelalbumApplicationTests {

	@Test
	void contextLoads() {
	}

}
