package de.shadowsoft.centaurus.server.identity;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServerIdentityService implements ApplicationRunner {

    private final ServerIdentityKeyRepository serverIdentityKeyRepository;

    public ServerIdentityService(ServerIdentityKeyRepository serverIdentityKeyRepository) {
        this.serverIdentityKeyRepository = serverIdentityKeyRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (serverIdentityKeyRepository.findFirstByActiveTrueOrderByCreatedAtDesc().isEmpty()) {
            serverIdentityKeyRepository.save(createIdentityKey());
        }
    }

    @Transactional(readOnly = true)
    public ServerIdentityKey activeKey() {
        return serverIdentityKeyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
            .orElseThrow(() -> new IllegalStateException("No active server identity key exists"));
    }

    public String sign(String payload, ServerIdentityKey key) {
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(key.getPrivateKeyReference());
            PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign server payload", exception);
        }
    }

    private ServerIdentityKey createIdentityKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            KeyPair keyPair = generator.generateKeyPair();
            String keyId = "srvkey_" + UUID.randomUUID();
            String publicKey = Base64.getEncoder().encodeToString(((EdECPublicKey) keyPair.getPublic()).getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(((EdECPrivateKey) keyPair.getPrivate()).getEncoded());
            return new ServerIdentityKey(keyId, publicKey, privateKey);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not create server identity key at " + Instant.now(), exception);
        }
    }
}
