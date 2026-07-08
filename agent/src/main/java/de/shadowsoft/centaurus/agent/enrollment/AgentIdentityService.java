package de.shadowsoft.centaurus.agent.enrollment;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentIdentityService {

    public AgentIdentity generateIdentity() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            KeyPair keyPair = generator.generateKeyPair();
            String keyId = "agtkey_" + UUID.randomUUID();
            String publicKey = Base64.getEncoder().encodeToString(((EdECPublicKey) keyPair.getPublic()).getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(((EdECPrivateKey) keyPair.getPrivate()).getEncoded());
            return new AgentIdentity(keyId, publicKey, privateKey);
        } catch (GeneralSecurityException exception) {
            throw new EnrollmentException("Could not generate agent identity", exception);
        }
    }
}
