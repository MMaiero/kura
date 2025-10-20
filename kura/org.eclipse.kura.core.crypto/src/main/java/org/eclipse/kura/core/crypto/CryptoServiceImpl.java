/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.crypto;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoServiceImpl implements CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);

    private static final String PARAMETER_EXCEPTION_CAUSE = "parameter";
    private static final String DECRYPT_EXCEPTION_CAUSE = "decrypt";
    private static final String VALUE_EXCEPTION_CAUSE = "value";

    private static final String ALGORITHM = "AES";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int AUTH_TAG_LENGTH_BIT = 128;
    private static final int IV_SIZE = 12;
    private static final byte[] SECRET_KEY = System
            .getProperty("org.eclipse.kura.core.crypto.secretKey", "").getBytes(StandardCharsets.UTF_8);
    private static final String ENCRYPTED_STRING_SEPARATOR = "-";
    private static final char[] DEFAULT_KEYSTORE_PASSWORD = "changeit".toCharArray();

    private String keystorePasswordPath;

    private final SecureRandom random = new SecureRandom();
    private SystemService systemService;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    protected void activate() {
        if (this.systemService == null) {
            throw new IllegalStateException("Unable to get instance of: " + SystemService.class.getName());
        }

        this.keystorePasswordPath = this.systemService.getKuraDataDirectory() + File.separator + "store.save";

        if (!isEncryptionEnabled()) {
            logger.error("Encryption key not configured. Data will be stored without encryption.");
        }
    }

    private static boolean isEncryptionEnabled() {
        return SECRET_KEY.length > 0;
    }

    @Override
    public char[] encryptAes(char[] value) throws KuraException {

        if (!isEncryptionEnabled()) {
            logger.error("Encryption key not set. Returning value without encryption.");
            return value;
        }

        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(CIPHER);
            byte[] iv = new byte[IV_SIZE];
            this.random.nextBytes(iv);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(AUTH_TAG_LENGTH_BIT, iv));
            byte[] encryptedBytes = c.doFinal(charArrayToByteArray(value));
            String ivString = base64Encode(iv);
            String encryptedMessage = base64Encode(encryptedBytes);

            return (ivString + ENCRYPTED_STRING_SEPARATOR + encryptedMessage).toCharArray();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "encrypt");
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | CharacterCodingException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, VALUE_EXCEPTION_CAUSE);
        } catch (InvalidAlgorithmParameterException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, PARAMETER_EXCEPTION_CAUSE);
        }

    }

    @Override
    public OutputStream aesEncryptingStream(OutputStream stream) throws KuraException {
        if (!isEncryptionEnabled()) {
            logger.error("Encryption key not set. Returning stream without encryption.");
            return stream;
        }

        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(CIPHER);

            byte[] iv = new byte[IV_SIZE];
            this.random.nextBytes(iv);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(AUTH_TAG_LENGTH_BIT, iv));

            stream.write(base64Encode(iv).getBytes(StandardCharsets.UTF_8));
            stream.write(ENCRYPTED_STRING_SEPARATOR.getBytes(StandardCharsets.UTF_8));

            final OutputStream base64Encoder = Base64.getEncoder().wrap(stream);

            return new CipherOutputStream(base64Encoder, c);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "encrypt");
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, PARAMETER_EXCEPTION_CAUSE);
        } catch (InvalidKeyException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, VALUE_EXCEPTION_CAUSE);
        }
    }

    private byte[] charArrayToByteArray(char[] value) throws CharacterCodingException {

        ByteBuffer byteBuffer;
        try {
            byteBuffer = getUtf8Encoder().encode(CharBuffer.wrap(value));
        } catch (CharacterCodingException e) {
            // fallback for backward compatibility
            byteBuffer = ByteBuffer.wrap(new String(value).getBytes());
        }
        byte[] encodedBytes = new byte[byteBuffer.limit()];
        byteBuffer.get(encodedBytes);

        return encodedBytes;
    }

    private char[] byteArrayToCharArray(byte[] value) throws CharacterCodingException {
        CharBuffer charBuffer;
        try {
            charBuffer = getUtf8Decoder().decode(ByteBuffer.wrap(value));
        } catch (CharacterCodingException e) {
            // fallback for backward compatibility
            charBuffer = CharBuffer.wrap(new String(value).toCharArray());
        }
        char[] decodedChar = new char[charBuffer.limit()];
        charBuffer.get(decodedChar);

        return decodedChar;
    }

    private CharsetEncoder getUtf8Encoder() {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        return encoder;
    }

    private CharsetDecoder getUtf8Decoder() {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        return decoder;
    }

    private byte[] base64Decode(String internalStringValue) {
        return Base64.getDecoder().decode(internalStringValue);
    }

    private String base64Encode(byte[] encryptedBytes) {
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private byte[] decodeIvIfPresent(String candidate) {
        try {
            byte[] iv = base64Decode(candidate);
            if (iv.length == IV_SIZE) {
                return iv;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    private static char[] defaultKeyStorePassword() {
        return DEFAULT_KEYSTORE_PASSWORD.clone();
    }

    @Override
    public char[] decryptAes(char[] encryptedValue) throws KuraException {
        if (encryptedValue.length == 0) {
            return new char[0];
        }

        String internalStringValue = new String(encryptedValue);
        String[] internalStringValueArray = internalStringValue.split(ENCRYPTED_STRING_SEPARATOR, 2);
        byte[] ivCandidate = internalStringValueArray.length == 2 ? decodeIvIfPresent(internalStringValueArray[0]) : null;
        boolean looksLikeEncrypted = ivCandidate != null;

        if (!isEncryptionEnabled()) {
            if (looksLikeEncrypted) {
                logger.error("Encryption key not set but data appears to be encrypted. Cannot decrypt without key.");
                throw new KuraException(KuraErrorCode.DECODER_ERROR, "Encryption key not configured");
            }
            logger.error("Encryption key not set. Returning value without decryption.");
            return encryptedValue;
        }

        try {
            if (!looksLikeEncrypted) {
                throw new KuraException(KuraErrorCode.DECODER_ERROR, VALUE_EXCEPTION_CAUSE);
            }
            String encodedValue = internalStringValueArray[1];
            byte[] decodedValue = base64Decode(encodedValue);
            if (encryptedValue.length > 0 && decodedValue.length == 0) {
                throw new KuraException(KuraErrorCode.DECODER_ERROR, VALUE_EXCEPTION_CAUSE);
            }

            Cipher c = Cipher.getInstance(CIPHER);
            c.init(Cipher.DECRYPT_MODE, generateKey(), new GCMParameterSpec(AUTH_TAG_LENGTH_BIT, ivCandidate));
            byte[] decryptedBytes = c.doFinal(decodedValue);

            return byteArrayToCharArray(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, DECRYPT_EXCEPTION_CAUSE);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | CharacterCodingException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, VALUE_EXCEPTION_CAUSE);
        } catch (InvalidAlgorithmParameterException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, PARAMETER_EXCEPTION_CAUSE);
        }
    }

    @Override
    public InputStream aesDecryptingStream(InputStream encryptedStream) throws KuraException {
        try {
            final BufferedInputStream buffered = new BufferedInputStream(encryptedStream);
            final ByteArrayOutputStream encodedIv = new ByteArrayOutputStream();

            int b;

            for (b = buffered.read(); b != -1 && b != '-'; b = buffered.read()) {
                encodedIv.write(b);
            }

            String encodedIvString = new String(encodedIv.toByteArray(), StandardCharsets.UTF_8);
            byte[] ivCandidate = b == '-' ? decodeIvIfPresent(encodedIvString) : null;
            boolean looksLikeEncrypted = ivCandidate != null;

            if (!isEncryptionEnabled()) {
                if (looksLikeEncrypted) {
                    logger.error("Encryption key not set but stream appears to be encrypted. Cannot decrypt without key.");
                    throw new KuraException(KuraErrorCode.DECODER_ERROR, "Encryption key not configured");
                }
                logger.error("Encryption key not set. Returning stream without decryption.");
                
                final byte[] firstBytes = encodedIv.toByteArray();
                final int separatorByte = b;
                return new InputStream() {
                    private int index = 0;
                    private boolean separatorReturned = false;

                    @Override
                    public int read() throws IOException {
                        if (index < firstBytes.length) {
                            return firstBytes[index++] & 0xFF;
                        }
                        if (!separatorReturned && separatorByte != -1) {
                            separatorReturned = true;
                            return separatorByte;
                        }
                        return buffered.read();
                    }
                };
            }

            if (!looksLikeEncrypted) {
                throw new KuraException(KuraErrorCode.DECODER_ERROR, VALUE_EXCEPTION_CAUSE);
            }

            buffered.mark(1);

            if (buffered.read() == -1) {
                throw new KuraException(KuraErrorCode.DECODER_ERROR, VALUE_EXCEPTION_CAUSE);
            }

            buffered.reset();

            final InputStream decodedStream = Base64.getDecoder().wrap(buffered);

            Cipher c = Cipher.getInstance(CIPHER);
            c.init(Cipher.DECRYPT_MODE, generateKey(), new GCMParameterSpec(AUTH_TAG_LENGTH_BIT, ivCandidate));

            return new CipherInputStream(decodedStream, c);

        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, e);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, DECRYPT_EXCEPTION_CAUSE);
        } catch (InvalidKeyException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, VALUE_EXCEPTION_CAUSE);
        } catch (InvalidAlgorithmParameterException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, PARAMETER_EXCEPTION_CAUSE);
        }
    }

    @Override
    @Deprecated
    public String encryptAes(String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        char[] encryptedValue = null;
        try {
            encryptedValue = encryptAes(value.toCharArray());
        } catch (KuraException e) {
            Throwable t = e.getCause();
            if (t instanceof NoSuchAlgorithmException) {
                throw (NoSuchAlgorithmException) t;
            } else if (t instanceof NoSuchPaddingException) {
                throw (NoSuchPaddingException) t;
            } else if (t instanceof InvalidKeyException) {
                throw (InvalidKeyException) t;
            } else if (t instanceof IllegalBlockSizeException) {
                throw (IllegalBlockSizeException) t;
            } else if (t instanceof BadPaddingException) {
                throw (BadPaddingException) t;
            }
        }

        return new String(encryptedValue);
    }

    @Override
    @Deprecated
    public String decryptAes(String encryptedValue) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
        try {
            return new String(decryptAes(encryptedValue.toCharArray()));
        } catch (KuraException e) {
            throw new IOException();
        }
    }

    @Override
    public String encodeBase64(String stringValue) throws UnsupportedEncodingException {
        if (stringValue == null) {
            return null;
        }

        return base64Encode(stringValue.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decodeBase64(String encodedValue) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (encodedValue == null) {
            return null;
        }

        return new String(base64Decode(encodedValue), StandardCharsets.UTF_8);
    }

    @Override
    public char[] getKeyStorePassword(String keyStorePath) {
        Properties props = new Properties();
        char[] password = defaultKeyStorePassword();

        File f = new File(this.keystorePasswordPath);
        if (!f.exists()) {
            return defaultKeyStorePassword();
        }

        try (FileInputStream fis = new FileInputStream(this.keystorePasswordPath);) {
            props.load(fis);
            Object value = props.get(keyStorePath);
            if (value != null) {
                String encryptedPassword = (String) value;
                password = decryptAes(encryptedPassword.toCharArray());
            }
        } catch (FileNotFoundException e) {
            logger.warn("File not found exception while getting keystore password - ", e);
        } catch (IOException e) {
            logger.warn("IOException while getting keystore password - ", e);
        } catch (KuraException e) {
            logger.warn("KuraException while getting keystore password - ", e);
        }

        return password;
    }

    @Override
    public void setKeyStorePassword(String keyStorePath, char[] password) throws KuraException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(this.keystorePasswordPath)) {
            props.load(fis);
        } catch (IOException e) {
            // Not loading from an existing file
        }
        char[] encryptedPassword = encryptAes(password);
        props.put(keyStorePath, new String(encryptedPassword));

        try (FileOutputStream fos = new FileOutputStream(this.keystorePasswordPath);) {
            props.store(fos, "Do not edit this file. It's automatically generated by Kura");
            fos.flush();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    @Deprecated
    public void setKeyStorePassword(String keyStorePath, String password) throws IOException {
        try {
            setKeyStorePassword(keyStorePath, password.toCharArray());
        } catch (KuraException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isFrameworkSecure() {
        return false;
    }

    private static Key generateKey() {
        return new SecretKeySpec(SECRET_KEY, ALGORITHM);
    }

    @Override
    public String hash(String s, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.reset();
        messageDigest.update(s.getBytes(StandardCharsets.UTF_8));

        byte[] encodedBytes = messageDigest.digest();
        return base64Encode(encodedBytes);
    }
}