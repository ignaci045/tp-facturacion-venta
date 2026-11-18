package com.example.facturaventa.arca;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Caché cifrada fuera del proyecto. La clave depende de la contraseña del PKCS12. */
final class ArcaTicketStore {
    private ArcaTicketStore() {}
    static Path ruta(ArcaConfig cfg) throws Exception {
        String base=System.getenv("ARCA_TICKET_CACHE_DIR");
        Path dir=base==null||base.isBlank()?Path.of(System.getProperty("user.home"),".facturaventa","arca-homologacion"):Path.of(base);
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        hash.update(Files.readAllBytes(Path.of(cfg.pkcs12Path())));
        hash.update((cfg.cuit()+"|homologacion|wsfe").getBytes(StandardCharsets.UTF_8));
        return dir.resolve(HexFormat.of().formatHex(hash.digest())+".ta.enc");
    }
    private static Cipher cifrador(int modo,ArcaConfig cfg,byte[] salt,byte[] iv) throws Exception {
        PBEKeySpec spec=new PBEKeySpec(cfg.pkcs12Password().toCharArray(),salt,120000,256);
        byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword();
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(modo,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));
        Arrays.fill(key,(byte)0);return cipher;
    }
    static String leer(ArcaConfig cfg) throws Exception {
        Path path=ruta(cfg);if(!Files.exists(path))return null;
        byte[] data=Files.readAllBytes(path);
        if(data.length<44)throw new IllegalStateException("Caché de ticket ARCA dañada");
        try {return new String(cifrador(Cipher.DECRYPT_MODE,cfg,Arrays.copyOfRange(data,0,16),Arrays.copyOfRange(data,16,28))
            .doFinal(Arrays.copyOfRange(data,28,data.length)),StandardCharsets.UTF_8);}
        catch(GeneralSecurityException e){throw new IllegalStateException("No se pudo descifrar el ticket ARCA guardado. Verificá la contraseña PKCS12 y la caché configurada.");}
    }
    static void guardar(ArcaConfig cfg,String ticket) throws Exception {
        Path path=ruta(cfg);Files.createDirectories(path.getParent());
        byte[] salt=new byte[16],iv=new byte[12];SecureRandom random=new SecureRandom();random.nextBytes(salt);random.nextBytes(iv);
        byte[] cipher=cifrador(Cipher.ENCRYPT_MODE,cfg,salt,iv).doFinal(ticket.getBytes(StandardCharsets.UTF_8));
        byte[] data=new byte[28+cipher.length];System.arraycopy(salt,0,data,0,16);System.arraycopy(iv,0,data,16,12);System.arraycopy(cipher,0,data,28,cipher.length);
        Path temp=Files.createTempFile(path.getParent(),"ticket-",".tmp");
        try {Files.write(temp,data);try {Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}
        finally {Files.deleteIfExists(temp);}
    }
}
