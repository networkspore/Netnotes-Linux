package com.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.lang.Double;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;

import java.io.FilenameFilter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import ove.crypto.digest.Blake2b;
import scala.util.Try;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.netnotes.App;
import com.netnotes.FreeMemory;
import com.netnotes.HashData;
import com.netnotes.PriceAmount;
import com.netnotes.PriceCurrency;
import com.satergo.extra.AESEncryption;

public class Utils {

    public final static String[] getAsciiStringArray(){
        String[] charStrings = new String[255];

        for(int i = 0; i < 255; i++){
            charStrings[i] = Character.toString ((char) i);
        }

        return charStrings;
    }

    // Security.addProvider(new Blake2bProvider());
    public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    public static String getBcryptHashString(String password) {
        SecureRandom sr;

        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {

            sr = new SecureRandom();
        }

        return BCrypt.with(BCrypt.Version.VERSION_2A, sr, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).hashToString(15, password.toCharArray());
    }

    public static boolean verifyBCryptPassword(String password, String hash) {
        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), hash.getBytes());

        return result.verified;
    }

    public static byte[] digestFile(File file) throws  IOException {

        return digestFileBlake2b(file,32);
        
    }

    public static boolean findPathPrefixInRoots(String filePathString){
        File roots[] = File.listRoots();

        if(roots != null && roots.length > 0 && filePathString != null && filePathString.length() > 0){

            String appDirPrefix = FilenameUtils.getPrefix(filePathString);

            for(int i = 0; i < roots.length; i++){
                String rootString = roots[i].getAbsolutePath();

                if(rootString.startsWith(appDirPrefix)){
                    return true;
                }
            }
        }

        return false;
    }

    public static byte[] digestFileBlake2b(File file, int digestLength) throws IOException {
        final Blake2b digest = Blake2b.Digest.newInstance(digestLength);

        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[8 * 1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        byte[] hashBytes = digest.digest();

        return hashBytes;

    }


    public static JsonObject getNetworkTypeObject() {
        JsonObject getExplorerObject = new JsonObject();

        getExplorerObject.addProperty("subject", "GET_NETWORK_TYPE");

        return getExplorerObject;
    }

    public static JsonObject getExplorerInterfaceIdObject() {
        JsonObject getExplorerObject = new JsonObject();

        getExplorerObject.addProperty("subject", "GET_EXPLORER_INTERFACE_ID");

        return getExplorerObject;
    }

    public static byte[] digestBytesToBytes(byte[] bytes) {
        final Blake2b digest = Blake2b.Digest.newInstance(32);

        digest.update(bytes);

        return digest.digest();
    }

    public static Map<String, List<String>> parseArgs(String args[]) {

        final Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        for (int i = 0; i < args.length; i++) {
            final String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return null;
                }

                options = new ArrayList<>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                return null;
            }
        }

        return params;
    }


    public static String getLatestFileString(String directoryString) {

        if (!Files.isDirectory(Paths.get(directoryString))) {
            return "";
        }

        String fileFormat = "netnotes-0.0.0.jar";
        int fileLength = fileFormat.length();

        File f = new File(directoryString);

        File[] matchingFiles = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("netnotes") && name.endsWith(".jar");
            }
        });

        if (matchingFiles == null) {
            return "";
        }

        int start = 7;

        String latestString = "";

        String versionA = "0.0.0";

        for (File file : matchingFiles) {

            String fileName = file.getName();

            if (fileName.equals("netnotes.jar")) {
                if (versionA.equals("0.0.0")) {
                    latestString = "netnotes.jar";
                }
            } else if (fileName.length() == fileLength) {

                int end = fileName.length() - 4;

                int i = end;
                char p = '.';

                while (i > start) {
                    char c = fileName.charAt(i);
                    if (Character.isDigit(c) || Character.compare(c, p) == 0) {
                        i--;
                    } else {
                        break;
                    }

                }

                String versionB = fileName.substring(i + 1, end);

                if (versionB.matches("[0-9]+(\\.[0-9]+)*")) {

                    Version vA = new Version(versionA);
                    Version vB = new Version(versionB);

                    if (vA.compareTo(vB) == -1) {
                        versionA = versionB;
                        latestString = fileName;
                    } else if (latestString.equals("")) {
                        latestString = fileName;
                    }
                }

            }

        }

        return latestString;
    }

    public static Version getFileNameVersion(String fileName){
        int end = fileName.length() - 4;

        int start = fileName.indexOf("-");

        int i = end;
        char p = '.';

        while (i > start) {
            char c = fileName.charAt(i);
            if (Character.isDigit(c) || Character.compare(c, p) == 0) {
                i--;
            } else {
                break;
            }

        }

        String versionString = fileName.substring(i + 1, end);

 
        if (versionString.matches("[0-9]+(\\.[0-9]+)*")) {
            Version version = null;
            try{
                version = new Version(versionString);
            }catch(IllegalArgumentException e){

            }
            return version;
        }
        return null;
    }



    public static PriceAmount getAmountByString(String text, PriceCurrency priceCurrency) {
        if (text != null && priceCurrency != null) {
            text = text.replace(",", ".");

            char[] ch = text.toCharArray();

            for (int i = 0; i < ch.length; ++i) {
                if (Character.isDigit(ch[i])) {
                    ch[i] = Character.forDigit(Character.getNumericValue(ch[i]), 10);
                }
            }

            text = new String(ch);

            try {
                double parsedDouble = Double.parseDouble(text);
                return new PriceAmount(parsedDouble, priceCurrency);
            } catch (NumberFormatException ex) {

            }
        }
        return new PriceAmount(0, priceCurrency);
    }
    
    public static String currencySymbol(String currency){
         switch (currency) {
            case "ERG":
                return "Σ";
            case "USD":
                return "$";
            case "USDT":
                return "$";
            case "EUR":
                return "€‎";
             
            case "BTC":
                return "฿";
        }
        return currency;
    }

    public static String formatCryptoString(BigDecimal price, String target, int precision, boolean valid) {
       String formatedDecimals = String.format("%."+precision+"f", price);
        String priceTotal = valid ? formatedDecimals : "-";
    
      
     
        switch (target) {
            case "ERG":
                priceTotal = priceTotal + " ERG";
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + priceTotal;
                break;
        }

        return priceTotal;
    }

    public static String formatCryptoString(double price, String target, int precision, boolean valid) {
        String formatedDecimals = String.format("%."+precision+"f", price);
        String priceTotal = valid ? formatedDecimals : "-";
    
        switch (target) {

            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            default:
                priceTotal = priceTotal + " " + target;
        }

        return priceTotal;
    }


    public static String formatCryptoString(double price, String target, boolean valid) {
        String formatedDecimals = String.format("%.2f", price);
        String priceTotal = valid ? formatedDecimals : "-.--";

        

        switch (target) {
            case "ERG":
                priceTotal = (valid ? String.format("%.3f", price) : "-.--") + " ERG";
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + (valid ? String.format("%.8f", price) : "-.--");
                break;
        }

        return priceTotal;
    }
    public static String truncateText(String text,FontMetrics metrics, double width) {
       
        String truncatedString = text.substring(0, 5) + "..";
        if (text.length() > 3) {
            int i = text.length() - 3;
            truncatedString = text.substring(0, i) + "..";

            while (metrics.stringWidth(truncatedString) > width && i > 1) {
                i = i - 1;
                truncatedString = text.substring(0, i) + "..";

            }
        }
        return truncatedString;
    }

    public static int measureString(String str, java.awt.Font font){
        
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.dispose();
        return fm.stringWidth(str);
    }

    public static String formatCryptoString(PriceAmount priceAmount, boolean valid) {
         int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

        String formatedDecimals = df.format(priceAmount.getDoubleAmount());
        String priceTotal = valid ? formatedDecimals : "-.--";

        

        switch (priceAmount.getCurrency().getSymbol()) {
            case "ERG":
                priceTotal = "Σ"+ priceTotal;
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + priceTotal;
                break;
            default:
                priceTotal = priceTotal + " " + priceAmount.getCurrency().getSymbol();
        }

        return priceTotal;
    }

    public static long getNowEpochMillis() {

        return System.currentTimeMillis();
    }

    public static long getNowEpochMillis(LocalDateTime now) {
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public static String formatDateTimeString(LocalDateTime localDateTime) {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss.SSS a");

        return formater.format(localDateTime);
    }

    public static String formatTimeString(LocalDateTime localDateTime) {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(localDateTime);
    }

    public static LocalDateTime milliToLocalTime(long timestamp) {
        Instant timeInstant = Instant.ofEpochMilli(timestamp);

        return LocalDateTime.ofInstant(timeInstant, ZoneId.systemDefault());
    }

    public static String readHexDecodeString(File file) {
        String fileHexString = null;

        try {
            fileHexString = file != null && file.isFile() ? Files.readString(file.toPath()) : null;
        } catch (IOException e) {

        }
        byte[] bytes = null;

        try {
            bytes = fileHexString != null ? Hex.decodeHex(fileHexString) : null;
        } catch (DecoderException e) {

        }

        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }
    public static int getDifference(int num1, int num2 ){
        return Math.abs(Math.max(num1, num2) - Math.min(num1, num2));
    }

    public static void returnObject(Object object, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                return object;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);

    }
    public static void returnObject(Object object,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                return object;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }

    public static void getUrlJson(String urlString, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() {
                try{
                    InputStream inputStream = null;
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    String outputString = null;

                    URL url = new URL(urlString);

                    URLConnection con = url.openConnection();

                    con.setRequestProperty("User-Agent", USER_AGENT);

                    long contentLength = con.getContentLengthLong();
                    inputStream = con.getInputStream();

                    byte[] buffer = new byte[2048];

                    int length;
                    long downloaded = 0;

                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);

                        if (progressIndicator != null) {
                            downloaded += (long) length;
                            updateProgress(downloaded, contentLength);
                        }
                    }

                    outputStream.close();
                    if(outputStream.size() > 0){
                        outputString = outputStream.toString();

                        JsonElement jsonElement = new JsonParser().parse(outputString);

                        JsonObject jsonObject = jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;

                        return jsonObject == null ? null : jsonObject;
                    }else{
                        return null;
                    }
                }catch(Exception err){
                    
                }
                return null;
            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);

    }

    public static String formatedBytes(long bytes, int decimals) {

        if (bytes == 0) {
            return "0 Bytes";
        }

        double k = 1024;
        int dm = decimals < 0 ? 0 : decimals;

        String[] sizes = new String[]{"Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

        int i = (int) Math.floor(Math.log((double) bytes) / Math.log(k));

        return String.format("%." + dm + "f", bytes / Math.pow(k, i)) + " " + sizes[i];

    }

    public static BufferedImage greyScaleImage(BufferedImage img) {

        int height = img.getHeight();
        int width = img.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);

                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                //calculate average
                int avg = (r + g + b) / 3;

                //replace RGB value with avg
                p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                img.setRGB(x, y, p);
            }
        }

        return img;
    }

    
    public static Image checkAndLoadImage(String imageString, HashData hashData) {
        if(imageString != null ){
            
            if(imageString.startsWith(App.ASSETS_DIRECTORY + "/")){
                return new Image(imageString);
            }
            File checkFile = new File(imageString);

            try {
                HashData checkFileHashData = new HashData(checkFile);
                /*try {
                    Files.writeString(logFile.toPath(), "\nhashString: " +checkFileHashData.getHashStringHex()+ " hashDataString: " + hashData.getHashStringHex(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/
                if (checkFileHashData.getHashStringHex().equals(hashData.getHashStringHex())) {
                    
                    return getImageByFile(checkFile);
                }
            } catch (Exception e) {
                try {
                    Files.writeString(new File("netnotes-log.txt").toPath(), "\nCheck and load image: " + imageString + " *" + e , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e2) {

                }
                new Image("/assets/unknown-unit.png");
            }
        }

  
        return new Image("/assets/unknown-unit.png");
        

    }

    public static Image getImageByFile(File file) throws IOException{
        if (file != null && file.isFile()) {
           
          
            String contentType  = Files.probeContentType(file.toPath());
            contentType = contentType.split("/")[0];
            if (contentType != null && contentType.equals("image")) {
                
                FileInputStream iStream = new FileInputStream(file);
                Image img = new Image(iStream);
                iStream.close();
                return img;
            }
           
        }
         return null;
    }

    public static String removeInvalidChars(String str)
    {
        return str.replaceAll("[^a-zA-Z0-9\\.\\-]", "");
    }


    public static TimeUnit stringToTimeUnit(String str) {
        switch (str.toLowerCase()) {
            case "μs":
            case "microsecond":
            case "microseconds":
                return TimeUnit.MICROSECONDS;
            case "ms":
            case "millisecond":
            case "milliseconds":
                return TimeUnit.MILLISECONDS;
            case "s":
            case "sec":
            case "second":
            case "seconds":
                return TimeUnit.SECONDS;
            case "min":
            case "minute":
            case "minutes":
                return TimeUnit.MINUTES;
            case "h":
            case "hour":
            case "hours":
                return TimeUnit.HOURS;
            case "day":
            case "days":
                return TimeUnit.DAYS;
            default:
                return null;
        }
    }

    public static String timeUnitToString(TimeUnit unit) {
        switch (unit) {
            case MICROSECONDS:
                return "μs";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "m";
            case HOURS:
                return "h";
            case DAYS:
                return "days";
            default:
                return "~";
        }
    }

    public static byte[] charsToBytes(char[] chars) {

        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static boolean checkJar(File jarFile) {
        boolean isJar = false;
        if (jarFile != null && jarFile.isFile()) {
            try {
                ZipFile zip = new ZipFile(jarFile);
                isJar = true;
                zip.close();
            } catch (Exception zipException) {

            }
        }
        return isJar;
    }

    public static void checkAddress(String addressString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<byte[]> task = new Task<byte[]>() {
            @Override
            public byte[] call() throws Exception {

                byte[] addressBytes = null;

                Try<byte[]> bytes = scorex.util.encode.Base58.decode(addressString);

                addressBytes = bytes.get();

                byte[] checksumBytes = new byte[]{addressBytes[addressBytes.length - 4], addressBytes[addressBytes.length - 3], addressBytes[addressBytes.length - 2], addressBytes[addressBytes.length - 1]};

                byte[] testBytes = new byte[addressBytes.length - 4];

                for (int i = 0; i < addressBytes.length - 4; i++) {
                    testBytes[i] = addressBytes[i];
                }

                byte[] hashBytes = Utils.digestBytesToBytes(testBytes);

                if (!(checksumBytes[0] == hashBytes[0]
                        && checksumBytes[1] == hashBytes[1]
                        && checksumBytes[2] == hashBytes[2]
                        && checksumBytes[3] == hashBytes[3])) {
                    return null;
                }

                return addressBytes;
            }
        };

        task.setOnSucceeded(onSucceeded);

        task.setOnFailed(onFailed);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public static JsonObject getCmdObject(String subject) {
        JsonObject cmdObject = new JsonObject();
        cmdObject.addProperty("subject", subject);
        cmdObject.addProperty("timeStamp", getNowEpochMillis());
        return cmdObject;
    }

    public static int getRandomInt(int min, int max) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();

        return secureRandom.nextInt(min, max);
    }

 

    public static void saveJson(SecretKey appKey, JsonObject listJson, File dataFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

        String tokenString = listJson.toString();

        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iV = new byte[12];
        secureRandom.nextBytes(iV);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

        cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

        byte[] encryptedData = cipher.doFinal(tokenString.getBytes());

        if (dataFile.isFile()) {
            Files.delete(dataFile.toPath());
        }

        FileOutputStream outputStream = new FileOutputStream(dataFile);
        FileChannel fc = outputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

        fc.write(byteBuffer);

        int written = 0;
        int bufferLength = 1024 * 8;

        while (written < encryptedData.length) {

            if (written + bufferLength > encryptedData.length) {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
            } else {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
            }

            written += fc.write(byteBuffer);
        }

        outputStream.close();

    }

   

    public static void saveJsonArray(SecretKey appKey, JsonArray jsonArray, File dataFile, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

       
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call() throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException  {
            
                    byte[] bytes = jsonArray.toString().getBytes();
                    
                    SecureRandom secureRandom = SecureRandom.getInstanceStrong();
                    byte[] iV = new byte[12];
                    secureRandom.nextBytes(iV);

                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

                    cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

                    byte[] encryptedData = cipher.doFinal(bytes);

                    try(
                        RandomAccessFile randomAccessFile = new RandomAccessFile(dataFile, "w");
                        FileChannel fc = randomAccessFile.getChannel();
                        FileLock fileLock = fc.lock(0L, Long.MAX_VALUE, false);
                    ){
                    //FileOutputStream outputStream = new FileOutputStream(dataFile);
                        fc.truncate(0);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

                        fc.write(byteBuffer);

                        int written = 0;
                        int bufferLength = 1024 * 8;

                        while (written < encryptedData.length) {

                            if (written + bufferLength > encryptedData.length) {
                                byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
                            } else {
                                byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
                            }

                            written += fc.write(byteBuffer);
                        }

                        return LocalDateTime.now();
                    }
                }
            };
    
            task.setOnFailed(onFailed);
    
            task.setOnSucceeded(onSucceeded);
    
            execService.submit(task);

    }

    /*public static JsonObject readJsonFile(SecretKey appKey, Path filePath) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        byte[] fileBytes;

        fileBytes = Files.readAllBytes(filePath);

        byte[] iv = new byte[]{
            fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
            fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
            fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
        };

        ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

        JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, encryptedData)));
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }

        return null;
    }*/
    public static JsonObject readJsonFile(SecretKey appKey, File file) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int bufferSize = 1024;
        if (bufferSize > channel.size()) {
            bufferSize = (int) channel.size();
        }
        ByteBuffer buff = ByteBuffer.allocate(bufferSize);

        while (channel.read(buff) > 0) {
            out.write(buff.array(), 0, buff.position());
            buff.clear();
        }

        channel.close();

        byte[] fileBytes = out.toByteArray();

        byte[] iv = new byte[]{
            fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
            fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
            fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
        };

        buff = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

        JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, buff)));
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }

        return null;

    }

    public static boolean readJsonArrayFile(SecretKey appKey, File file, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        if(!file.isFile()){
            return false;
        }

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {
                
               
                byte[] fileBytes;
                try(
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                    FileChannel channel = randomAccessFile.getChannel();
                    FileLock fileLock = channel.lock(0L, Long.MAX_VALUE, true);
                ){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    
                    int bufferSize = 1024 * 4;
                    
                    if (bufferSize > channel.size()) {
                        bufferSize = (int) channel.size();
                    }

                    ByteBuffer buff = ByteBuffer.allocate(bufferSize);

                    while (channel.read(buff) > 0) {
                        out.write(buff.array(), 0, buff.position());
                        buff.clear();
                    }
                    fileBytes = out.toByteArray();
               

                byte[] iv = new byte[]{
                    fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                    fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                    fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
                };

                buff  = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

                
                JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, buff)));
                if (jsonElement != null && jsonElement.isJsonArray()) {
                    return jsonElement.getAsJsonArray();
                }
                

                return null;
                }
            }
        };
    
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);
        
        return true;
    }


    public static void writeEncryptedString(SecretKey secretKey, File dataFile, String jsonString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {

        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iV = new byte[12];
        secureRandom.nextBytes(iV);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

        if (dataFile.isFile()) {
            Files.delete(dataFile.toPath());
        }

        FileOutputStream outputStream = new FileOutputStream(dataFile);
        FileChannel fc = outputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

        fc.write(byteBuffer);

        int written = 0;
        int bufferLength = 1024 * 8;

        while (written < encryptedData.length) {

            if (written + bufferLength > encryptedData.length) {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
            } else {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
            }

            written += fc.write(byteBuffer);
        }

        outputStream.close();

    }

    public static String readStringFile(SecretKey appKey, File file) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int bufferSize = 1024;
        if (bufferSize > channel.size()) {
            bufferSize = (int) channel.size();
        }
        ByteBuffer buff = ByteBuffer.allocate(bufferSize);

        while (channel.read(buff) > 0) {
            out.write(buff.array(), 0, buff.position());
            buff.clear();
        }

        channel.close();

        byte[] fileBytes = out.toByteArray();

        byte[] iv = new byte[]{
            fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
            fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
            fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
        };

        buff = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

        return new String(AESEncryption.decryptData(iv, appKey, buff));
       


    }

   

    public static void moveFileAndHash(File inputFile, File outputFile, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                long contentLength = -1;

                if (inputFile != null && inputFile.isFile() && outputFile != null && !inputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    contentLength = Files.size(inputFile.toPath());
                } else {
                    return null;
                }

                FileOutputStream outputStream = new FileOutputStream(outputFile);

                 final Blake2b digest = Blake2b.Digest.newInstance(32);

                FileInputStream inputStream = new FileInputStream(inputFile);

                byte[] buffer = new byte[8 * 1024];

                int length;
                long copied = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    digest.update(buffer, 0, length);

                    copied += (long) length;
                    updateProgress(length, contentLength);

                }
                outputStream.close();
                inputStream.close();

                byte[] hashbytes = digest.digest();

                HashData hashData = new HashData(hashbytes);

                outputStream.close();

                return contentLength == copied ? hashData : null;

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    
    public static void getUrlJsonArray(String urlString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            public JsonArray call() throws JsonParseException, MalformedURLException, IOException {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String outputString = null;

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();
                inputStream = con.getInputStream();

                byte[] buffer = new byte[2048];

                int length;
                long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);

                    if (progressIndicator != null) {
                        downloaded += (long) length;
                        updateProgress(downloaded, contentLength);
                    }
                }
                inputStream.close();
                outputStream.close();
                outputString = outputStream.toString();
                
                JsonElement jsonElement = new JsonParser().parse(outputString);

                JsonArray jsonArray = jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;

                return jsonArray == null ? null : jsonArray;

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public static void getUrlJsonArray(String urlString, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            public JsonArray call() throws JsonParseException, MalformedURLException, IOException {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String outputString = null;

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();
                inputStream = con.getInputStream();

                byte[] buffer = new byte[2048];

                int length;
                long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);

                    if (progressIndicator != null) {
                        downloaded += (long) length;
                        updateProgress(downloaded, contentLength);
                    }
                }
                inputStream.close();
                outputStream.close();
                outputString = outputStream.toString();
                
                JsonElement jsonElement = new JsonParser().parse(outputString);

                JsonArray jsonArray = jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;

                return jsonArray == null ? null : jsonArray;

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);
    }

    public static void getUrlFileHash(String urlString, File outputFile,ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator, SimpleBooleanProperty cancel) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                if (outputFile == null) {
                    return null;
                }
                Files.deleteIfExists(outputFile.toPath());

               
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                final Blake2b digest = Blake2b.Digest.newInstance(32);

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                         

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();
                InputStream inputStream = con.getInputStream();

                

                byte[] buffer = new byte[8 * 1024];

                int length;
                long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    digest.update(buffer, 0, length);
                    if (progressIndicator != null) {
                        downloaded += (long) length;
                        updateProgress(downloaded, contentLength);
                    }
                    if(cancel.get()){
                        inputStream.close();
                        outputStream.close();
                        return null;

                    }
                }

                byte[] hashbytes = digest.digest();

                HashData hashData = new HashData(hashbytes);

                outputStream.close();

                return contentLength == downloaded ? hashData : null;

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);
    }



   

    public static void centerStage(Stage stage, Rectangle screenRectangle){
        stage.setX(screenRectangle.getWidth()/2 - stage.getWidth()/2);
        stage.setY(screenRectangle.getHeight()/2 - stage.getHeight()/2);
    }

    

    public static String[] pslastPID(String jarname){
          try {
          //  File logFile = new File("wmicTerminate-log.txt");
            //Get-Process | Where {$_.ProcessName -Like "SearchIn*"}
         //   String[] wmicCmd = {"powershell", "Get-Process", "|", "Where", "{$_.ProcessName", "-Like", "'*" +  jarname+ "*'}"};
            Process psProc = Runtime.getRuntime().exec("powershell Get-WmiObject Win32_Process | WHERE {$_.CommandLine -Like '*"+jarname+"*' } | Select ProcessId");

            BufferedReader psStderr = new BufferedReader(new InputStreamReader(psProc.getErrorStream()));
            //String pserr = null;


            ArrayList<String> pids = new ArrayList<>();

            BufferedReader psStdInput = new BufferedReader(new InputStreamReader(psProc.getInputStream()));

            String psInput = null;
           // boolean gotInput = false;
            //   int pid = -1;
               
            while ((psInput = psStdInput.readLine()) != null) {
              //  
              //  gotInput = true;
                psInput.trim();
                if(!psInput.equals("") && !psInput.startsWith("ProcessId") && !psInput.startsWith("---------")){
                    
                    pids.add(psInput);
                }
            }
            
            String  pserr = null;
            while ((pserr = psStderr.readLine()) != null) {
                try {
                    Files.writeString(new File("netnotes-log.txt").toPath(), "\npsPID err: " + pserr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                
                }
               // Files.writeString(logFile.toPath(), "\nps err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
            }

            psProc.waitFor();
            if( pids.size() > 0){
                String[] pidArray = new String[pids.size()];

                pidArray =  pids.toArray(pidArray);
                
                return pidArray;
            }else{
                return null;
            }
            

        } catch (Exception e) {
              try {
                Files.writeString(new File("netnotes-log.txt").toPath(), "\npsPID: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
             
            }
             
            return null;
        }
   
    }

     public static void psStopProcess(String pid){
          try {
          //  File logFile = new File("wmicTerminate-log.txt");
            //Get-Process | Where {$_.ProcessName -Like "SearchIn*"}
         //   String[] wmicCmd = {"powershell", "Get-Process", "|", "Where", "{$_.ProcessName", "-Like", "'*" +  jarname+ "*'}"};
            Process psProc = Runtime.getRuntime().exec("powershell stop-process -id " + pid );


            psProc.waitFor();



        } catch (Exception e) {
            
        }
   
    }

     public static void cmdTaskKill(String pid){
          try {

            Process psProc = Runtime.getRuntime().exec("cmd /c taskkill /PID " + pid );


            psProc.waitFor();



        } catch (Exception e) {
            
        }
   
    }

   

    public static int findMenuItemIndex(ObservableList<MenuItem> list, String id){
        if(id != null){
            for(int i = 0; i < list.size() ; i++){
                MenuItem menuItem = list.get(i);
                Object userData = menuItem.getUserData();

                if(userData != null && userData instanceof String){
                    String menuItemId = (String) userData;
                    if(menuItemId.equals(id)){
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    public static boolean wmicTerminate(String jarName) {
        try {
          //  File logFile = new File("wmicTerminate-log.txt");
     
            String[] wmicCmd = {"cmd", "/c", "wmic", "Path", "win32_process", "Where", "\"CommandLine", "Like", "'%" + jarName + "%'\"", "Call", "Terminate"};
            Process wmicProc = Runtime.getRuntime().exec(wmicCmd);

            BufferedReader wmicStderr = new BufferedReader(new InputStreamReader(wmicProc.getErrorStream()));
            //String wmicerr = null;


         

            BufferedReader wmicStdInput = new BufferedReader(new InputStreamReader(wmicProc.getInputStream()));

           // String wmicInput = null;
            boolean gotInput = false;

            while ((wmicStdInput.readLine()) != null) {
            
            
                gotInput = true;
            }

            while ((wmicStderr.readLine()) != null) {

               // Files.writeString(logFile.toPath(), "\nwmic err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return false;
            }

            wmicProc.waitFor();

            if (gotInput) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }


    public static String[] getShellCmd(String cmd){
        
        return new String[]{"bash", "-c", cmd};
    }
    

    public static String[] findPIDs(String jarName){
        try {
            //  File logFile = new File("wmicTerminate-log.txt");
              //Get-Process | Where {$_.ProcessName -Like "SearchIn*"}
           //   String[] wmicCmd = {"powershell", "Get-Process", "|", "Where", "{$_.ProcessName", "-Like", "'*" +  jarname+ "*'}"};
            String execString = "ps -ef | grep -v grep | grep " + jarName + " | awk '{print $2}'";
            String[] cmd = new String[]{ "bash", "-c", execString};
            Process proc = Runtime.getRuntime().exec(cmd);
  
              BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
              //String pserr = null;
  
  
              ArrayList<String> pids = new ArrayList<>();
  
              BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
  
              String input = null;
                 
              while ((input = stdInput.readLine()) != null) {
                input.trim();
                pids.add(input);
                  
              }
              
              String  pserr = null;
              while ((pserr = stderr.readLine()) != null) {
                  try {
                      Files.writeString(new File("netnotes-log.txt").toPath(), "\nutils: " + execString + ": " + pserr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                  } catch (IOException e1) {
                  
                  }
                 // Files.writeString(logFile.toPath(), "\nps err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                  
              }
  
              proc.waitFor();
              if( pids.size() > 0){
             
                  String[] pidArray = new String[pids.size()];
  
                  pidArray =  pids.toArray(pidArray);
                  
                  return pidArray;
              }else{
                  return null;
              }
              
  
          } catch (Exception e) {
                try {
                  Files.writeString(new File("netnotes-log.txt").toPath(), "\npsPID: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
              } catch (IOException e1) {
               
              }
               
              return null;
          }

    }
    /*
    public static boolean sendTermSig(String pid){
        try {
            //  File logFile = new File("wmicTerminate-log.txt");
       
              //String[] wmicCmd = {"cmd", "/c", "wmic", "Path", "win32_process", "Where", "\"CommandLine", "Like", "'%" + jarName + "%'\"", "Call", "Terminate"};
              String[] cmd = new String[]{ "bash", "-c",  "kill -SIGTERM " + pid};

              Process wmicProc = Runtime.getRuntime().exec(cmd);
  
              BufferedReader wmicStderr = new BufferedReader(new InputStreamReader(wmicProc.getErrorStream()));
  
              BufferedReader wmicStdInput = new BufferedReader(new InputStreamReader(wmicProc.getInputStream()));
  
             // String wmicInput = null;
              boolean gotInput = false;
  
              while ((wmicStdInput.readLine()) != null) {
              
              
                  gotInput = true;
              }
  
              while ((wmicStderr.readLine()) != null) {
  
                 // Files.writeString(logFile.toPath(), "\nwmic err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                  return false;
              }
  
              wmicProc.waitFor();
  
              if (gotInput) {
                  return true;
              }
  
          } catch (Exception e) {
              return false;
          }
          return false;
    }*/
    public static boolean sendTermSig(String jarName) {
        try {
          //  File logFile = new File("wmicTerminate-log.txt");
     
            //String[] wmicCmd = {"cmd", "/c", "wmic", "Path", "win32_process", "Where", "\"CommandLine", "Like", "'%" + jarName + "%'\"", "Call", "Terminate"};
            String execString = "kill $(ps -ef | grep -v grep | grep " + jarName + " | awk '{print $2}')";

            String[] cmd = new String[]{ "bash", "-c", execString};

            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            //String wmicerr = null;


            BufferedReader wmicStdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

           // String wmicInput = null;
            boolean gotInput = false;

            while ((wmicStdInput.readLine()) != null) {
            
            
                gotInput = true;
            }
            String errStr = "";
            while ((errStr = stdErr.readLine()) != null) {

                Files.writeString(new File("netnotes-log.txt").toPath(), "\nsig term err: " + errStr + "\n'" + execString + "'", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                gotInput = false;
            }

            proc.waitFor();

            if (gotInput) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }
    public static void openDir(File file) throws Exception{

        String[] cmd = new String[]{ "bash", "-c",  "gio open " + file.getParentFile().getCanonicalPath()};

        Runtime.getRuntime().exec(cmd);
  
    }

    public static JsonObject getResourcesObject(String resourceString){

        if(resourceString != null){
         
            JsonElement resourceFileElement = new JsonParser().parse(resourceString);
            
            if(resourceFileElement != null && resourceFileElement.isJsonObject()){
                
                JsonObject json = resourceFileElement.getAsJsonObject();
                if(json.has("apps")){
                    if(!json.get("apps").isJsonArray()){
                        json.remove("apps");
                        json.add("apps", new JsonArray());
                    }
                }else{
                    json.add("apps", new JsonArray());
                }
                return json;
            }
            
        }
        JsonObject resourceObject = new JsonObject();
        resourceObject.add("apps", new JsonArray());
        
        return resourceObject;
    }



    public static int findJsonArrayStringIndex(JsonArray stringJson, String str){
        
        int size = stringJson.size();

        for(int i = 0; i < size ; i ++){
            if(stringJson.get(i).getAsString().equals(str)){
                return i;
            }
        }

        return -1;
    }

    public static void addAppResource(String appName) {
        File resourceFile = new File(App.RESOURCES_FILENAME);
        if(appName == null){
            return;
        }
        
        String resourceFileString = null;
        try{
            resourceFileString = resourceFile.isFile() ?  Files.readString(resourceFile.toPath()) : null ;
        }catch(IOException e){
            
        }
            
        JsonObject resourcesObject = getResourcesObject(resourceFileString);


        JsonArray appsArray = resourcesObject.get("apps").getAsJsonArray();
        
        resourcesObject.remove("apps");

        boolean isAdd = findJsonArrayStringIndex(appsArray, appName) == -1;

        if(isAdd){
            appsArray.add(appName);
            resourcesObject.add("apps", appsArray);

            try {
                Files.writeString(resourceFile.toPath(), resourcesObject.toString());
            } catch (IOException e) {
                try {
                    Files.writeString(App.logFile.toPath(), "\naddAppResource: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                }
            }
        }


    }

    public static void removeAppResource(String appName) throws IOException {
   
    
        File resourceFile = new File(App.RESOURCES_FILENAME);
        if(resourceFile.isFile() && appName != null)
        {
            String resourceFileString = null;
            try{
                resourceFileString = Files.readString(resourceFile.toPath());
            }catch(IOException e){
                
            }
            JsonObject resourcesObject = getResourcesObject(resourceFileString);
             
            JsonArray appsArray = resourcesObject.get("apps").getAsJsonArray();
            
            resourcesObject.remove("apps");

            int index = findJsonArrayStringIndex(appsArray, appName);

            if(index != -1){
                appsArray.remove(index);
                resourcesObject.add("apps", appsArray);
                Files.writeString(resourceFile.toPath(), resourcesObject.toString());
            
            }
        }
    }


    public static FreeMemory getFreeMemory() {
        try{ 
            String[] cmd = new String[]{ "bash", "-c",  "cat /proc/meminfo | awk '{print $1,$2}'"};

            Process proc = Runtime.getRuntime().exec(cmd);


            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        

            long swapTotal = -1;
            long swapFree = -1;
            long memFree = -1;
            long memAvailable = -1;
            long memTotal = -1;

            String s = null;

            String delimiter = ": ";
            int delimiterSize = delimiter.length();

            while ((s = stdInput.readLine()) != null) {
                
                int spaceIndex = s.indexOf(delimiter);
                
                
                String rowStr = s.substring(0, spaceIndex);
                long value = Long.parseLong(s.substring(spaceIndex + delimiterSize ));
                
                switch(rowStr){
                    case "SwapTotal":
                        swapTotal = value;
                    break;
                    case "SwapFree":
                        swapFree = value;
                    break;
                    case "MemTotal":
                        memTotal = value;
                    break;
                    case "MemFree":
                        memFree = value;
                    break;
                    case "MemAvailable":
                        memAvailable = value;
                    break;
                }

            }

            String errStr = stdErr.readLine();
            
            proc.waitFor();

            if(errStr == null){
                return new FreeMemory(swapTotal, swapFree, memFree, memAvailable, memTotal);
            }
        }catch(IOException | InterruptedException e){
            try {
                Files.writeString(new File("netnotes-log.txt").toPath(), "\nUtils getFreeMemory:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

        return null;
    }

    public static String getIncreseSwapUrl(){
        return "https://askubuntu.com/questions/178712/how-to-increase-swap-space";
    }


   

    public static void pingIP(String ip, SimpleObjectProperty<Ping> pingProperty, ExecutorService execService){
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws IOException {

                String[] cmd = {"bash", "-c", "ping -c 4 " + ip};



                Process proc = Runtime.getRuntime().exec(cmd);

                BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                List<String> javaOutputList = new ArrayList<String>();

                String s = null;

                boolean available = false;
                String error = "";
                int avg = -1;

                while ((s = stdInput.readLine()) != null) {
                    javaOutputList.add(s);

                 //   String timeString = "time=";
                // int indexOftimeString = s.indexOf(timeString);

                    if (s.indexOf("service not known") > -1) {
                        available = false;
                        error = "Unreachable";
                
                    }

                    if (s.indexOf("timed out") > -1) {
                        available = false;
                        error = "Timed out";
                    }


                    /*if (indexOftimeString > 0) {
                        int lengthOftime = timeString.length();

                        int indexOfms = s.indexOf("ms");

                        available = true;

                        String time = s.substring(indexOftimeString + lengthOftime, indexOfms + 2);

                        //Platform.runLater(()->status.set("Ping: " + time));
        
                    }*/

                    String avgString = "min/avg/max/mdev = ";
                    int indexOfAvgString = s.indexOf(avgString);

                    if (indexOfAvgString > 0) {
                            int lengthOfAvg = avgString.length();

                            String avgStr = s.substring(indexOfAvgString + lengthOfAvg);
                            int slashIndex = avgStr.indexOf("/");

                            avgStr = avgStr.substring(slashIndex+1, avgStr.indexOf("/",slashIndex + 1) );
                            
                            avg = (int) Math.ceil(Double.parseDouble(avgStr));
                            available = true;
                        }

                    }
        

                    return new Ping(available, error, avg);
                }
            };
    
            task.setOnFailed((onFailed)->{
                pingProperty.set(new Ping(false, "Unavailable", -1));
            });
    
            task.setOnSucceeded((onSucceeded)->{
                Object returnObject = onSucceeded.getSource().getValue();
                if(returnObject != null){
                    pingProperty.set((Ping) returnObject);
                }else{
                    pingProperty.set(new Ping(false, "Unavailable", -1));
                }
            });
    
            execService.submit(task);
            // String[] splitStr = javaOutputList.get(0).trim().split("\\s+");
            //Version jV = new Version(splitStr[1].replaceAll("/[^0-9.]/g", ""));
        

    }

    public static boolean sendKillSig(String jarName) {
        try {
          //  File logFile = new File("wmicTerminate-log.txt");
     
            //String[] wmicCmd = {"cmd", "/c", "wmic", "Path", "win32_process", "Where", "\"CommandLine", "Like", "'%" + jarName + "%'\"", "Call", "Terminate"};
            String execString = "kill $(ps -ef | grep -v grep | grep " + jarName + " | awk '{print $2}')";

            String[] cmd = new String[]{ "bash", "-c", execString};

            Process wmicProc = Runtime.getRuntime().exec(cmd);

            BufferedReader wmicStderr = new BufferedReader(new InputStreamReader(wmicProc.getErrorStream()));
            //String wmicerr = null;


         

            BufferedReader wmicStdInput = new BufferedReader(new InputStreamReader(wmicProc.getInputStream()));

           // String wmicInput = null;
            boolean gotInput = false;

            while ((wmicStdInput.readLine()) != null) {
            
            
                gotInput = true;
            }

            while ((wmicStderr.readLine()) != null) {

               // Files.writeString(logFile.toPath(), "\nwmic err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return false;
            }

            wmicProc.waitFor();

            if (gotInput) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean onlyZero(String str) {
        
        for (int i = 0 ; i < str.length() ; i++){
            String c = str.substring(i, i+1);
            if(!(c.equals("0") || c.equals("."))){
                return false;
            }
        }
        return true;
    }
    
    public static URL getLocation(final Class<?> c) {

        if (c == null) {
            return null; // could not load the class
        }
        // try the easy way first
        try {
            final URL codeSourceLocation = c.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceLocation != null) {
                return codeSourceLocation;
            }
        } catch (final SecurityException e) {
            // NB: Cannot access protection domain.
        } catch (final NullPointerException e) {
            // NB: Protection domain or code source is null.
        }

        // NB: The easy way failed, so we try the hard way. We ask for the class
        // itself as a resource, then strip the class's path from the URL string,
        // leaving the base path.
        // get the class's raw resource path
        final URL classResource = c.getResource(c.getSimpleName() + ".class");
        if (classResource == null) {
            return null; // cannot find class resource
        }
        final String url = classResource.toString();
        final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
        if (!url.endsWith(suffix)) {
            return null; // weird URL
        }
        // strip the class's path from the URL string
        final String base = url.substring(0, url.length() - suffix.length());

        String path = base;

        // remove the "jar:" prefix and "!/" suffix, if present
        if (path.startsWith("jar:")) {
            path = path.substring(4, path.length() - 2);
        }

        try {
            return new URL(path);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the given {@link URL} to its corresponding {@link File}.
     * <p>
     * This method is similar to calling {@code new File(url.toURI())} except
     * that it also handles "jar:file:" URLs, returning the path to the JAR
     * file.
     * </p>
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a
     * file.
     */
    public static File urlToFile(final URL url) {
        return url == null ? null : urlToFile(url.toString());
    }

    /**
     * Converts the given URL string to its corresponding {@link File}.
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a
     * file.
     */
    public static File urlToFile(final String url) {
        String path = url;
        if (path.startsWith("jar:")) {
            // remove "jar:" prefix and "!/" suffix
            final int index = path.indexOf("!/");
            path = path.substring(4, index);
        }

        try {

            if (path.matches("file:[A-Za-z]:.*")) {
                path = "file:/" + path.substring(5);
            }
            return new File(new URL(path).toURI());
        } catch (final MalformedURLException e) {
            // NB: URL is not completely well-formed.

        } catch (final URISyntaxException e) {
            // NB: URL is not completely well-formed.
        }
        if (path.startsWith("file:")) {
            // pass through the URL as-is, minus "file:" prefix
            path = path.substring(5);
            return new File(path);
        }
        throw new IllegalArgumentException("Invalid URL: " + url);
    }

}
