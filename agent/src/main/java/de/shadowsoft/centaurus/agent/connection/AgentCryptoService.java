package de.shadowsoft.centaurus.agent.connection;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class AgentCryptoService {

    public String sign(String privateKeyValue, String payload) {
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyValue);
            PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign agent payload", exception);
        }
    }

    public boolean verify(String publicKeyValue, String payload, String signatureValue) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyValue);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureValue));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }
}
