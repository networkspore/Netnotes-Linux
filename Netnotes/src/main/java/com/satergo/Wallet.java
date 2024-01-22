package com.satergo;

import com.netnotes.AddressData;
import com.satergo.ergo.Balance;
import com.satergo.ergo.ErgoInterface;
import com.satergo.extra.AESEncryption;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.stage.FileChooser;

import org.ergoplatform.ErgoAddress;
import org.ergoplatform.appkit.*;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class Wallet {

    public static final String FILE_EXTENSION = "erg";

    public static FileChooser.ExtensionFilter extensionFilter() {
        return new FileChooser.ExtensionFilter("Wallet file", "*." + Wallet.FILE_EXTENSION);
    }

    public static final int MAGIC_NUMBER = 0x36003600;
    public static final long NEWEST_SUPPORTED_FORMAT = 1;

    @SuppressWarnings("FieldCanBeLocal")
    private final long formatVersion = NEWEST_SUPPORTED_FORMAT;

    public final Path path;

    public final SimpleStringProperty name;

    // Idea: local notes on transactions
    private final TreeMap<Integer, String> internalMyAddresses = new TreeMap<>();
    // index<->name; EIP3 addresses belonging to this wallet
    public final ObservableMap<Integer, String> myAddresses = FXCollections.observableMap(internalMyAddresses);
    // name<->address
    public final ObservableMap<String, Address> addressBook = FXCollections.observableMap(new HashMap<>());

    public int nextAddressIndex() {
        return internalMyAddresses.lastKey() + 1;
    }

    private Wallet(Path path, WalletKey key, String name, Map<Integer, String> myAddresses, byte[] detailsIv, SecretKey detailsSecretKey) {
        this.path = path;
        this.key = key;
        this.name = new SimpleStringProperty(name);
        this.detailsIv = detailsIv;
        this.detailsSecretKey = detailsSecretKey;

        this.myAddresses.putAll(myAddresses);
        this.myAddresses.addListener((MapChangeListener<Integer, String>) change -> saveToFile());
        this.name.addListener((observable, oldValue, newValue) -> saveToFile());
    }

    private WalletKey key;

    public WalletKey key() {
        return key;
    }

    public final SimpleObjectProperty<Balance> lastKnownBalance = new SimpleObjectProperty<>();

    public Address publicAddress(NetworkType networkType, int index) throws WalletKey.Failure {
        return key.derivePublicAddress(networkType, index);
    }

    public Stream<Address> addressStream(NetworkType networkType) {
        return myAddresses.keySet().stream().map(index -> {
            try {
                return publicAddress(networkType, index);
            } catch (WalletKey.Failure e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void changePassword(char[] currentPassword, char[] newPassword) throws Exception {
        try {
            key = key.changedPassword(currentPassword, newPassword);
            detailsIv = AESEncryption.generateNonce12();
            detailsSecretKey = AESEncryption.generateSecretKey(newPassword, detailsIv);
        } catch (WalletKey.Failure e) {
            throw new Exception();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        saveToFile();
    }

    /**
     * creates a new wallet with local key and master address and saves it
     */
    public static Wallet create(Path path, Mnemonic mnemonic, String name, char[] password, boolean nonstandardDerivation) {
        byte[] detailsIv = AESEncryption.generateNonce12();
        SecretKey detailsSecretKey;
        try {
            detailsSecretKey = AESEncryption.generateSecretKey(password, detailsIv);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        Wallet wallet = new Wallet(path, WalletKey.Local.create(nonstandardDerivation, mnemonic, password), name, Map.of(0, "Master"), detailsIv, detailsSecretKey);
        wallet.saveToFile();
        return wallet;
    }

    public static Wallet create(Path path, Mnemonic mnemonic, String name, char[] password) {
        return create(path, mnemonic, name, password, false);
    }

    // ENCRYPTION, SERIALIZATION & STORING
    private byte[] detailsIv;
    private SecretKey detailsSecretKey;

    public byte[] serializeEncrypted() throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC_NUMBER);
            out.writeLong(formatVersion);

            out.writeInt(key.encrypted().length);
            out.write(key.encrypted());

            byte[] rawDetailsData;
            try (ByteArrayOutputStream bytesInfo = new ByteArrayOutputStream(); DataOutputStream outInfo = new DataOutputStream(bytesInfo)) {
                outInfo.writeUTF(name.get());
                outInfo.writeInt(myAddresses.size());
                for (Map.Entry<Integer, String> entry : myAddresses.entrySet()) {
                    outInfo.writeInt(entry.getKey());
                    outInfo.writeUTF(entry.getValue());
                }
                outInfo.writeInt(addressBook.size());
                for (Map.Entry<String, Address> entry : addressBook.entrySet()) {
                    outInfo.writeUTF(entry.getKey());
                    outInfo.writeUTF(entry.getValue().toString());
                }
                outInfo.flush();
                rawDetailsData = bytesInfo.toByteArray();
            }
            // contains the IV and the encrypted data
            byte[] encryptedDetailsData = AESEncryption.encryptData(detailsIv, detailsSecretKey, rawDetailsData);
            out.writeInt(encryptedDetailsData.length);
            out.write(encryptedDetailsData);
            out.flush();
            return bytes.toByteArray();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveToFile() {
        try {
            Files.write(path, serializeEncrypted());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Wallet deserializeDecryptedLegacy(long formatVersion, byte[] bytes, Path path, char[] password) throws UnsupportedOperationException, IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (formatVersion == 0) {
                String name = in.readUTF();
                SecretString seedPhrase = SecretString.create(in.readUTF());
                SecretString mnemonicPassword = SecretString.create(in.readUTF());
                int myAddressesSize = in.readInt();
                TreeMap<Integer, String> myAddresses = new TreeMap<>();
                for (int i = 0; i < myAddressesSize; i++) {
                    myAddresses.put(in.readInt(), in.readUTF());
                }
                int addressBookSize = in.readInt();
                HashMap<String, Address> addressBook = new HashMap<>();
                for (int i = 0; i < addressBookSize; i++) {
                    addressBook.put(in.readUTF(), Address.create(in.readUTF()));
                }
                // nonstandardDerivation is always true because the bug in the ergo-wallet cryptography library
                // was not discovered yet when formatVersion 0 was created
                Wallet wallet = Wallet.create(path, Mnemonic.create(seedPhrase, mnemonicPassword), name, password, true);
                wallet.myAddresses.putAll(myAddresses);
                wallet.addressBook.putAll(addressBook);
                // Upgrade wallet file right away
                wallet.saveToFile();
                return wallet;
            } else {
                throw new UnsupportedOperationException("Unsupported format version " + formatVersion + " (this release only supports " + NEWEST_SUPPORTED_FORMAT + " and older)");
            }
        }
    }

    /**
     * @throws UnsupportedOperationException Cannot deserialize this
     * formatVersion, it is too new
     */
    private static Wallet deserializeNew(long formatVersion, DataInputStream in, Path path, char[] password) throws Exception, UnsupportedOperationException, IOException {
        if (formatVersion == 1) {
            WalletKey key;
            byte[] decryptedDetails;
            byte[] detailsEncryptionIv;
            SecretKey detailsEncryptionKey;
            try {
                byte[] encryptedKey = in.readNBytes(in.readInt());
                byte[] encryptedDetails = in.readNBytes(in.readInt());

                key = WalletKey.deserialize(encryptedKey, ByteBuffer.wrap(AESEncryption.decryptData(password, ByteBuffer.wrap(encryptedKey))));
                decryptedDetails = AESEncryption.decryptData(password, ByteBuffer.wrap(encryptedDetails));

                detailsEncryptionIv = AESEncryption.generateNonce12();
                detailsEncryptionKey = AESEncryption.generateSecretKey(password, detailsEncryptionIv);
            } catch (AEADBadTagException e) {
                throw new Exception();
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }

            try (DataInputStream din = new DataInputStream(new ByteArrayInputStream(decryptedDetails))) {
                String name = din.readUTF();
                int myAddressesSize = din.readInt();
                TreeMap<Integer, String> myAddresses = new TreeMap<>();
                for (int i = 0; i < myAddressesSize; i++) {
                    myAddresses.put(din.readInt(), din.readUTF());
                }
                int addressBookSize = din.readInt();
                HashMap<String, Address> addressBook = new HashMap<>();
                for (int i = 0; i < addressBookSize; i++) {
                    addressBook.put(din.readUTF(), Address.create(din.readUTF()));
                }
                Wallet wallet = new Wallet(path, key, name, myAddresses, detailsEncryptionIv, detailsEncryptionKey);
                wallet.addressBook.putAll(addressBook);
                return wallet;
            }
        } else {
            throw new UnsupportedOperationException("Unsupported format version " + formatVersion + " (this release only supports " + NEWEST_SUPPORTED_FORMAT + " and older)");
        }
    }

    public static Wallet deserializeEncrypted(byte[] bytes, Path path, char[] password) throws Exception, IOException {
        try {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (in.readInt() == MAGIC_NUMBER) {
                    return deserializeNew(in.readLong(), in, path, password);
                }
            }
            // old format (version 0)
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            // skip nonce length field which is always 12 (int)
            buffer.position(4);
            byte[] decrypted = AESEncryption.decryptData(password, buffer);
            try (ObjectInputStream old = new ObjectInputStream(new ByteArrayInputStream(decrypted))) {
                long formatVersion = old.readLong();
                return deserializeDecryptedLegacy(formatVersion, old.readAllBytes(), path, password);
            } catch (StreamCorruptedException | EOFException e) {
                throw new IllegalArgumentException("Invalid wallet data");
            }
        } catch (AEADBadTagException e) {
            throw new Exception();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static Wallet load(Path path, String password) throws Exception {
        try {
            Wallet wallet = deserializeEncrypted(Files.readAllBytes(path), path, password.toCharArray());
            wallet.saveToFile();
            return wallet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

  

    public String transact(ErgoClient ergoClient, SignedTransaction signedTx) {
       
	
		return ergoClient.execute(ctx -> {
			String quoted = ctx.sendTransaction(signedTx);
			return quoted.substring(1, quoted.length() - 1);
		});
	}

}
