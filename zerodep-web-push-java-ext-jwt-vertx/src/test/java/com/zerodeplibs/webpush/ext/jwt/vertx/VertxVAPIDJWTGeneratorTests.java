package com.zerodeplibs.webpush.ext.jwt.vertx;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.zerodeplibs.webpush.jwt.VAPIDJWTGenerator;
import com.zerodeplibs.webpush.jwt.VAPIDJWTParam;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;

public class VertxVAPIDJWTGeneratorTests {

    @Test
    public void generate()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JoseException,
        IOException {

        Vertx vertx = Vertx.vertx();
        KeyPair keyPair = generateKeyPair();

        VAPIDJWTParam param = VAPIDJWTParam.getBuilder()
            .resourceURLString("https://example.com")
            .expiresAfter(60, TimeUnit.SECONDS)
            .subject("mailto:test@example.com")
            .additionalClaim("adClaim", new MyAdditionalClaim("hello"))
            .build();

        VertxVAPIDJWTGeneratorFactory factory =
            new VertxVAPIDJWTGeneratorFactory(() -> vertx);
        VAPIDJWTGenerator generator =
            factory.create((ECPrivateKey) keyPair.getPrivate(), (ECPublicKey) keyPair.getPublic());

        String jwt = generator.generate(param);
        verifySign(jwt, keyPair.getPublic());
        assertHeader(jwt);
        assertPayload(jwt);
    }


    private void assertHeader(String jwt) throws IOException {
        byte[] decoded = splitAndDecode(0, jwt);
        JsonObject jsonObject = new JsonObject(Buffer.buffer(decoded));
        TestingJWTHeader actual = jsonObject.mapTo(TestingJWTHeader.class);

        assertThat(actual.getTyp(), equalTo("JWT"));
        assertThat(actual.getAlg(), equalTo("ES256"));
    }

    private void assertPayload(String jwt) throws IOException {
        byte[] decoded = splitAndDecode(1, jwt);
        JsonObject jsonObject = new JsonObject(Buffer.buffer(decoded));

        assertThat(jsonObject.getString("aud"), equalTo("https://example.com"));
        assertThat(jsonObject.getInteger("exp") > System.currentTimeMillis() / 1000, equalTo(true));
        assertThat(jsonObject.getString("sub"), equalTo("mailto:test@example.com"));

        JsonObject additionalClaims = jsonObject.getJsonObject("adClaim");
        assertThat(additionalClaims.getString("myClaim"), equalTo("hello"));
    }

    private void verifySign(String jwt, PublicKey publicKey) throws JoseException {

        JsonWebSignature verifier = new JsonWebSignature();
        verifier.setAlgorithmConstraints(new AlgorithmConstraints(
            AlgorithmConstraints.ConstraintType.PERMIT,
            AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256));

        verifier.setCompactSerialization(jwt);
        verifier.setKey(publicKey);

        assertThat(verifier.verifySignature(), equalTo(true));
    }

    private KeyPair generateKeyPair()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        KeyPairGenerator keyPairGeneragor = KeyPairGenerator.getInstance("EC");
        keyPairGeneragor.initialize(new ECGenParameterSpec("secp256r1"));
        return keyPairGeneragor.generateKeyPair();
    }

    private byte[] splitAndDecode(int pos, String jwt) {
        String content = jwt.split("\\.")[pos];
        return Base64.getUrlDecoder().decode(content.getBytes(StandardCharsets.UTF_8));
    }

    public static class TestingJWTHeader {
        private String typ;
        private String alg;

        public String getTyp() {
            return typ;
        }

        public void setTyp(String typ) {
            this.typ = typ;
        }

        public String getAlg() {
            return alg;
        }

        public void setAlg(String alg) {
            this.alg = alg;
        }
    }

    public static class MyAdditionalClaim {

        private String myClaim;

        public MyAdditionalClaim() {
        }

        public MyAdditionalClaim(String myClaim) {
            this.myClaim = myClaim;
        }

        public String getMyClaim() {
            return myClaim;
        }

        public void setMyClaim(String myClaim) {
            this.myClaim = myClaim;
        }

    }
}
