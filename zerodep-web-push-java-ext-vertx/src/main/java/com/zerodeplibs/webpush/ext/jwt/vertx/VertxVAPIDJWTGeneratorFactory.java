package com.zerodeplibs.webpush.ext.jwt.vertx;

import com.zerodeplibs.webpush.jwt.VAPIDJWTGenerator;
import com.zerodeplibs.webpush.jwt.VAPIDJWTGeneratorFactory;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.function.Supplier;

/**
 * The factory class for {@link VAPIDJWTGenerator}.
 * {@link VAPIDJWTGenerator}s generated by this class utilizes <a href="https://vertx.io/docs/vertx-auth-jwt/java/">JWT Auth - Vert.x</a>.
 *
 * @author Tomoki Sato
 */
public class VertxVAPIDJWTGeneratorFactory implements VAPIDJWTGeneratorFactory {

    private final Supplier<Vertx> vertxObtainStrategy;

    /**
     * Creates a new {@link VertxVAPIDJWTGeneratorFactory}.
     *
     * <p>
     * The returned generator invokes <code>vertxObtainStrategy#get</code> each time
     * its 'generate' method is called in order to get an instance of Vertx.
     *
     * this instance is passed to the JWT authentication provider
     * and used to generate the JWT.
     * </p>
     *
     * @param vertxObtainStrategy a strategy used to obtain a Vertx instance.
     * @see JWTAuth#create(Vertx, JWTAuthOptions)
     */
    public VertxVAPIDJWTGeneratorFactory(Supplier<Vertx> vertxObtainStrategy) {
        this.vertxObtainStrategy = vertxObtainStrategy;
    }

    @Override
    public VAPIDJWTGenerator create(ECPrivateKey privateKey,
                                    ECPublicKey publicKey) {
        return new VertxVAPIDJWTGenerator(this.vertxObtainStrategy, privateKey);
    }
}
