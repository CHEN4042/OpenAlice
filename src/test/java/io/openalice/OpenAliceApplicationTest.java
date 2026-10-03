package io.openalice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
        properties = {
            "openalice.home=${java.io.tmpdir}/openalice-context-${random.uuid}",
            "spring.main.banner-mode=off"
        })
class OpenAliceApplicationTest {

    @Autowired ApplicationContext context;

    @Test
    void springContextStartsWithoutModelCredentials() {
        assertThat(context).isNotNull();
    }
}
