package health.ere.ps.model.erixa.api.credentials;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class BasicAuthCredentialsTest {

    BasicAuthCredentials basicAuthCredentials = new BasicAuthCredentials(
        "email",
        "password");

    @Test
    public void testBasicAuthCredentialObjectEmail() {
        assertEquals(basicAuthCredentials.getEmail(), "email", "Wrong email");
    }

    @Test
    public void testBasicAuthCredentialObjectPassword() {
        assertEquals(basicAuthCredentials.getPassword(), "password", "Wrong password");
    }
}
