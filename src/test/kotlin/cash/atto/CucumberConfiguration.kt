package cash.atto

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@CucumberContextConfiguration
@AutoConfigureTestRestTemplate
class CucumberConfiguration(
    val caches: List<CacheSupport>,
) {
    @Before
    fun before() {
        caches.forEach { it.clear() }
    }

    @After
    fun after() {
        caches.forEach { it.clear() }
    }
}
