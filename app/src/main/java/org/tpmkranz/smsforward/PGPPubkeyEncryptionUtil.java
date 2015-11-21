package org.tpmkranz.smsforward;

import android.util.Log;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.spongycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by tpm on 11/20/15.
 */
public class PGPPubkeyEncryptionUtil {

    protected static String LOGCATTAG = "PGPPubkeyEncryptionUtil";
    private PGPPublicKey publicKey = null;

    public PGPPubkeyEncryptionUtil(String pubkeyBlock){
        InputStream in = new ByteArrayInputStream(pubkeyBlock.getBytes());
        publicKey = null;
        try {
            in = PGPUtil.getDecoderStream(in);
            JcaPGPPublicKeyRingCollection pgpPub = new JcaPGPPublicKeyRingCollection(in);
            in.close();
            Iterator<PGPPublicKeyRing> rIt = pgpPub.getKeyRings();
            while (publicKey == null && rIt.hasNext()) {
                PGPPublicKeyRing keyRing = rIt.next();
                Iterator<PGPPublicKey> kIt = keyRing.getPublicKeys();
                while (publicKey == null && kIt.hasNext()) {
                    PGPPublicKey k = kIt.next();
                    if (k.isEncryptionKey()) {
                        publicKey = k;
                    }
                }
            }
        } catch (Exception e){
            Log.e(LOGCATTAG, e.toString());
        }
    }

    public boolean hasKey(){
        return publicKey != null;
    }


    public String encrypt(String plainText) throws NoSuchAlgorithmException, IOException, PGPException {
        byte[] rawText = plainText.getBytes();
        //This needs, like, three metric fucktons of explanation and/or cleaning up
        ByteArrayOutputStream encOut = new ByteArrayOutputStream();
        OutputStream out = new ArmoredOutputStream(encOut);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        OutputStream cos = comData.open(bOut);
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        OutputStream pOut = lData.open(cos, PGPLiteralData.BINARY, PGPLiteralData.CONSOLE, rawText.length, new Date());
        pOut.write(rawText);
        lData.close();
        comData.close();
        BcPGPDataEncryptorBuilder builder = new BcPGPDataEncryptorBuilder(PGPEncryptedData.AES_256);
        builder.setWithIntegrityPacket(true);
        builder.setSecureRandom(new SecureRandom());
        PGPEncryptedDataGenerator cpk = new PGPEncryptedDataGenerator(builder);
        cpk.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(publicKey));
        byte[] bytes = bOut.toByteArray();
        OutputStream cOut = cpk.open(out, bytes.length);
        cOut.write(bytes);
        cOut.close();
        out.close();
        return new String(encOut.toByteArray(), Charset.forName("UTF-8"));
    }
}
