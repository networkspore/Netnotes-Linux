package com.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
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
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLConnection;
import java.lang.Double;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.ergoplatform.appkit.ErgoTreeTemplate;

import java.io.FilenameFilter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import ove.crypto.digest.Blake2b;
import scala.util.Try;
import scorex.util.encode.Base16;

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

public class Utils {

    public static final int DEFAULT_BUFFER_SIZE = 2048;

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
        File roots[] = App.getRoots();
        return findPathPrefixInRoots(roots, filePathString);
    }


    public static boolean findPathPrefixInRoots(File roots[], String filePathString){
        

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

    public static JsonObject getMsgObject(int code, String msg){
        JsonObject json = new JsonObject();
        json.addProperty("code", code);
        json.addProperty("msg", msg);
        return json;
    }

    public static byte[] digestFileBlake2b(File file, int digestLength) throws IOException {
        final Blake2b digest = Blake2b.Digest.newInstance(digestLength);
        try(
            FileInputStream fis = new FileInputStream(file);
        ){
            int bufferSize = file.length() < DEFAULT_BUFFER_SIZE ? (int) file.length() : DEFAULT_BUFFER_SIZE;

            byte[] byteArray = new byte[bufferSize];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            };

            byte[] hashBytes = digest.digest();

            return hashBytes;
        }
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

    public static String formatAddressString(String addressString, double width, double characterSize){
        
        int elipsisSize = 3;
        int adrStrLen = addressString.length();
        
        if(adrStrLen > 5){
            int characters = ((int) (width / characterSize)) -elipsisSize;
            if(characters > 6 && characters < adrStrLen){
                
                int len = (int) (characters / 2);         
                String returnString = addressString.substring(0, len ) + "…" + addressString.substring(adrStrLen- len, adrStrLen) ;
            
                return returnString;
            }else{
                return addressString;
            }
        }else{
            return addressString;
        }
    }
     
    public static String parseMsgForJsonId(String msg){
        if(msg != null){
            JsonParser jsonParser = new JsonParser();

            JsonElement jsonElement = jsonParser.parse(msg);

            if(jsonElement != null && jsonElement.isJsonObject()){
                JsonObject json = jsonElement.getAsJsonObject();
                JsonElement idElement = json.get("id");
                if(idElement != null && !idElement.isJsonNull()){
                    return idElement.getAsString();
                }
            }
        }

        return null;
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
    public static double computeTextWidth(Font font, String text, double wrappingWidth) {
    
        m_helper = new Text();
        m_helper.setFont(font);
        m_helper.setText(text);
        // Note that the wrapping width needs to be set to zero before
        // getting the text's real preferred width.
        m_helper.setWrappingWidth(0);
        m_helper.setLineSpacing(0);
        double w = Math.min(m_helper.prefWidth(-1), wrappingWidth);
        m_helper.setWrappingWidth((int)Math.ceil(w));
        double textWidth = Math.ceil(m_helper.getLayoutBounds().getWidth());
        m_helper = null;
        return textWidth;
    }

    public static double computeTextWidth(Font font, String text) {
    
        m_helper = new Text();
        m_helper.setFont(font);
        m_helper.setText(text);
        double w =  Math.ceil(m_helper.getLayoutBounds().getWidth());
        m_helper = null;
        return w;
    }
    private static Text m_helper;
    private static BufferedImage m_tmpImg = null;
    private static Graphics2D m_tmpG2d = null;
    private static java.awt.Font m_tmpFont = null;
    private static FontMetrics m_tmpFm = null;

    public static int getStringWidth(String str){
        return getStringWidth(str, 14);
    }

    public static int getCharacterSize(int fontSize){
        m_tmpImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_tmpG2d = m_tmpImg.createGraphics();
        m_tmpFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, fontSize);
        m_tmpG2d.setFont(m_tmpFont);
        m_tmpFm = m_tmpG2d.getFontMetrics();
        
        int width = m_tmpFm.charWidth(' ');

        m_tmpFm = null;
        m_tmpG2d.dispose();
        m_tmpG2d = null;
        m_tmpFont = null;

        m_tmpImg = null;


        return width;
    }

    public static int getStringWidth(String str, int fontSize){
        return getStringWidth(str, fontSize, "OCR A Extended", java.awt.Font.PLAIN);
    }

    public static int getStringWidth(String str, int fontSize, String fontName, int fontStyle){
        m_tmpImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        m_tmpG2d = m_tmpImg.createGraphics();
        m_tmpFont = new java.awt.Font(fontName, fontStyle, fontSize);
        m_tmpG2d.setFont(m_tmpFont);
        m_tmpFm = m_tmpG2d.getFontMetrics();
        
        int width = m_tmpFm.stringWidth(str);

        m_tmpFm = null;
        m_tmpG2d.dispose();
        m_tmpG2d = null;
        m_tmpFont = null;

        m_tmpImg = null;


        return width;
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



    public static void checkDrive(File file, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Boolean> task = new Task<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                
                return findPathPrefixInRoots(file.getCanonicalPath());
             
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);
    }

    public static Future<?> returnException(String errorString, ExecutorService execService, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception {
                throw new Exception(errorString);
            }
        };

        task.setOnFailed(onFailed);

        return execService.submit(task);

    }

    public static Future<?> returnObject(Object object, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                return object;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);

    }

    public static void returnObject(Object object, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                return object;
            }
        };

        task.setOnSucceeded(onSucceeded);

        execService.submit(task);

    }


    public static int getJsonElementType(JsonElement jsonElement){
        return jsonElement.isJsonNull() ? -1 : jsonElement.isJsonObject() ? 1 : jsonElement.isJsonArray() ? 2 : jsonElement.isJsonPrimitive() ? 3 : 0;
    }

    public static String getUrlOutputStringSync(String urlString) throws IOException{
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);

        try(
            InputStream inputStream = con.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ){

            byte[] buffer = new byte[2048];

            int length;

            while ((length = inputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, length);
              
            }
     

            return outputStream.toString();
   
        }
    }
    

    public static Future<?> getUrlJson(String urlString, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
  
        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() throws IOException {
           
                String outputString = getUrlOutputStringSync(urlString);
    
                JsonElement jsonElement = outputString != null ? new JsonParser().parse(outputString) : null;

                return jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;

            }

        };

      
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);

    }

    public static BigInteger[] decimalToFractional(BigDecimal decimal){
        String number = decimal.toPlainString();
        int index = number.indexOf(".");
        String leftSide = index != -1 ? number.substring(0, index) : number;
        String rightSide = index != -1 ?  number.substring(index + 1) : "";
        int numDecimals = rightSide.length();
        BigInteger denominator = BigInteger.valueOf(10).pow(numDecimals);
        BigInteger numerator = new BigInteger(leftSide).multiply(denominator).add(new BigInteger(rightSide));
        return new BigInteger[]{numerator, denominator};
     }

    public static String formatStringToNumber(String number, int decimals){
        number = number.replaceAll("[^0-9.]", "");
        int index = number.indexOf(".");
        String leftSide = index != -1 ? number.substring(0, index + 1) : number;
        leftSide = leftSide.equals(".") ? "0." : leftSide;
        String rightSide = index != -1 ?  number.substring(index + 1) : "";
        rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
        rightSide = rightSide.length() > decimals ? rightSide.substring(0, decimals) : rightSide;

        return leftSide + rightSide;
    
    }

    public static Future<?> getUrlJsonArray(String urlString, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            public JsonArray call() throws JsonParseException, MalformedURLException, IOException {
              
                String outputString = getUrlOutputStringSync(urlString);

                JsonElement jsonElement = outputString != null ? new JsonParser().parse(outputString) : null;

                return jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;

            }

        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }

    public static JsonObject getJsonFromUrlSync(String urlString) throws IOException{
                                             
        String outputString = getUrlOutputStringSync(urlString);

        JsonElement jsonElement = outputString != null ? new JsonParser().parse(outputString) : null;

        JsonObject jsonObject = jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;

        return jsonObject == null ? null : jsonObject;
           

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

    public static Future<?> checkAddress(String addressString, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
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

        return execService.submit(task);
    }



    public static JsonObject getJsonObject(String name, String property){
        JsonObject json = new JsonObject();
        json.addProperty(name, property);
        return json;
    }


    public static JsonObject getJsonObject(String name, int property){
        JsonObject json = new JsonObject();
        json.addProperty(name, property);
        return json;
    }

    public static JsonObject getCmdObject(String subject) {
        JsonObject cmdObject = new JsonObject();
        cmdObject.addProperty(App.CMD, subject);
        cmdObject.addProperty("timeStamp", getNowEpochMillis());
        return cmdObject;
    }

    public static JsonObject getMsgObject(int code, long timeStamp, String networkId){
        JsonObject json = new JsonObject();
        json.addProperty("timeStamp", timeStamp);
        json.addProperty("networkId", networkId);
        json.addProperty("code", code);
        return json;
    }



    public static int getRandomInt(int min, int max) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();

        return secureRandom.nextInt(min, max);
    }

 

    public static void saveJson(SecretKey appKey, JsonObject json, File dataFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {


        Utils.writeEncryptedString(appKey, dataFile, json.toString());

    }

    public static void saveEncryptedData(SecretKey appKey, byte[] data, File dataFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, ShortBufferException {

        byte[] iV = getIv();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);
        cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);
        

        if (dataFile.isFile()) {
            Files.delete(dataFile.toPath());
        }

        try(
            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
            FileOutputStream fileStream = new FileOutputStream(dataFile);
        ){
            fileStream.write(iV);    
            int bufferSize = data.length < DEFAULT_BUFFER_SIZE ? (int) data.length : DEFAULT_BUFFER_SIZE;

            byte[] byteArray = new byte[bufferSize];
            byte[] output;

            int length = 0;
            while((length = byteStream.read(byteArray)) != -1){

                output = cipher.update(byteArray, 0, length);
                if(output != null){
                    fileStream.write(output);
                }
            }

            output = cipher.doFinal();
            if(output != null){
                fileStream.write(output);
            }



        }
        
    }




    public static Future<?> saveJsonArray(SecretKey appKey, JsonArray jsonArray, File dataFile, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        if(appKey != null && jsonArray != null && dataFile != null){
       
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call() throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException  {
                    
                    String jsonString = jsonArray.toString();
                    writeEncryptedString(appKey, dataFile, jsonString);
                    
                    return true;
                }
            };
    
            task.setOnFailed(onFailed);
    
            task.setOnSucceeded(onSucceeded);
    
            return execService.submit(task);
        }
        return null;
    }

    public static Future<?> decryptBytesFromFile(SecretKey appKey, File file, ExecutorService execService,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException{
        Task<byte[]> task = new Task<byte[]>() {
            @Override
            public byte[] call() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException{
                
                return decryptFileToBytes(appKey, file);

            }
        };
        

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }

    public static boolean encryptBytesToFile(SecretKey appKey, byte[] bytes, File outputFile)throws NoSuchAlgorithmException, MalformedURLException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
        long contentLength = -1;

                 
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iV = new byte[12];
        secureRandom.nextBytes(iV);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

        cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

       

        try(
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ){
            long bytesSize = bytes.length;
            int bufferSize = bytesSize < 1024 ? (int) bytesSize :1024;
            
            byte[] buffer = new byte[bufferSize];
            byte[] output;
            int length;
            long copied = 0;
   
            outputStream.write(iV);

            while ((length = inputStream.read(buffer)) != -1) {

               

                output = cipher.update(buffer, 0, length);
                if(output != null){
                    outputStream.write(output);
                }
                copied += (long) length;
                
           
            }

            output = cipher.doFinal();

            if(output != null){
                outputStream.write(output);
            }

       
            if( contentLength == copied){
                return true;
            }

        }

      
        return false;
    }

    

    public static Future<?> encryptBytesToFile(SecretKey appKey, byte[] bytes, File outputFile, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws NoSuchAlgorithmException, MalformedURLException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
                return encryptBytesToFile(appKey, bytes, outputFile);
            }

        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }

    public static byte[] createKeyBytes(String password) throws NoSuchAlgorithmException, InvalidKeySpecException  {

        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);

    

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(password.toCharArray(), bytes, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();

    }

    

    
    public static boolean updateFileEncryption(SecretKey oldAppKey, SecretKey newAppKey, File file, File tmpFile) throws FileNotFoundException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        if(file != null && file.isFile()){
       
            try(
                FileInputStream inputStream = new FileInputStream(file);
                FileOutputStream outStream = new FileOutputStream(tmpFile);
            ){
                
                byte[] oldIV = new byte[12];

                byte[] newIV = getIv();

                inputStream.read(oldIV);
                outStream.write(newIV);

                Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, oldIV);
                decryptCipher.init(Cipher.DECRYPT_MODE, oldAppKey, parameterSpec);

                Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec newSpec = new GCMParameterSpec(128, newIV);
                encryptCipher.init(Cipher.ENCRYPT_MODE, newAppKey, newSpec);

                long fileSize = file.length();
                int bufferSize = fileSize < DEFAULT_BUFFER_SIZE ? (int) fileSize : DEFAULT_BUFFER_SIZE;

                byte[] readBuffer = new byte[bufferSize];
                byte[] decryptedBuffer;
                byte[] encryptedBuffer;

                int length = 0;

                while ((length = inputStream.read(readBuffer)) != -1) {
                    decryptedBuffer = decryptCipher.update(readBuffer, 0, length);
                    if(decryptedBuffer != null){
                        encryptedBuffer = encryptCipher.update(decryptedBuffer);
                        if(encryptedBuffer != null){
                            outStream.write(encryptedBuffer);
                        }
                    }
                }

                decryptedBuffer = decryptCipher.doFinal();

                if(decryptedBuffer != null){
                    encryptedBuffer = encryptCipher.update(decryptedBuffer);
                    if(encryptedBuffer != null){
                        outStream.write(encryptedBuffer);
                    }
                }

                encryptedBuffer = encryptCipher.doFinal();

                if(encryptedBuffer != null){
                    outStream.write(encryptedBuffer);
                }
                
            }

            Path filePath = file.toPath();
            Files.delete(filePath);
            FileUtils.moveFile(tmpFile, file);

            return true;
        }
        return false;
    }

    public static String getStringFromResource(String resourceLocation) throws IOException{
        URL location = resourceLocation != null ? App.class.getResource(resourceLocation) : null;
        if(location != null){
            try(
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                BufferedInputStream inStream = new BufferedInputStream(location.openStream());
            ){
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int length = 0;

                while ((length = inStream.read(buffer)) != -1){
                    outStream.write(buffer, 0, length);
                }

                return outStream.toString();
            }
        }else{
            return null;
        }
    }

    public static byte[] getHexBytesFromResource(String resourceLocation) throws IOException{
        String hex = getStringFromResource(resourceLocation);
        return Base16.decode(hex).get();
    }

    public static ErgoTreeTemplate getErgoTemplateFromResource(String resourceLocation) throws IOException{
        byte[] bytes = getHexBytesFromResource(resourceLocation);
        return ErgoTreeTemplate.fromErgoTreeBytes(bytes);
    }

    public static String getErgoTemplateHashFromResource(String resourceLocation) throws IOException{
        ErgoTreeTemplate ergoTreeTemplate = getErgoTemplateFromResource(resourceLocation);
        return ergoTreeTemplate.getTemplateHashHex();
    }

    public static Future<?> getErgoTemplateHashFromResource(String resourceLocation, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception{
                return Utils.getErgoTemplateHashFromResource(resourceLocation);
            }
        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
            
    }

    public static Future<?> getErgoTemplateFromResource(String resourceLocation, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception{
                return Utils.getErgoTemplateFromResource(resourceLocation);
            }
        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
            
    }

    public static Future<?> getHexBytesFromResource(String resourceLocation, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception{
                return Utils.getHexBytesFromResource(resourceLocation);
            }
        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
            
    }

    public static Future<?> getStringFromResource(String resourceLocation, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception{
                return Utils.getStringFromResource(resourceLocation);
            }
        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
            
    }

    public static boolean decryptFileToFile(SecretKey appKey, File encryptedFile, File decryptedFile) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException{
        if(encryptedFile != null && encryptedFile.isFile() && encryptedFile.length() > 12){
            
            try(
                FileInputStream inputStream = new FileInputStream(encryptedFile);
                FileOutputStream outStream = new FileOutputStream(decryptedFile);
            ){
                
                byte[] iV = new byte[12];

                inputStream.read(iV);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);
        
                cipher.init(Cipher.DECRYPT_MODE, appKey, parameterSpec);

                long fileSize = encryptedFile.length();
                int bufferSize = fileSize < (8 * 1024) ? (int) fileSize :(8 * 1024);

                byte[] buffer = new byte[bufferSize];
                byte[] decryptedBuffer;
                int length = 0;
                long decrypted = 0;

                while ((length = inputStream.read(buffer)) != -1) {
                    decryptedBuffer = cipher.update(buffer, 0, length);
                    if(decryptedBuffer != null){
                        outStream.write(decryptedBuffer);
                    }
                    decrypted += length;
                }

                decryptedBuffer = cipher.doFinal();

                if(decryptedBuffer != null){
                    outStream.write(decryptedBuffer);
                }

                if(decrypted == fileSize){
                    return true;
                }
            }

      
        }
        return false;
    }

  
    
    public static byte[] decryptFileToBytes(SecretKey appKey, File file) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException{
        if(file != null && file.isFile()){
            
            try(
                FileInputStream inputStream = new FileInputStream(file);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ){
                
                byte[] iV = new byte[12];

                int length = inputStream.read(iV);

                if(length < 12){
                    return null;
                }

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);
        
                cipher.init(Cipher.DECRYPT_MODE, appKey, parameterSpec);

                long fileSize = file.length();
                int bufferSize = fileSize < (long) DEFAULT_BUFFER_SIZE ? (int) fileSize : DEFAULT_BUFFER_SIZE;

                byte[] buffer = new byte[bufferSize];
                byte[] decryptedBuffer;
               

                while ((length = inputStream.read(buffer)) != -1) {
                    decryptedBuffer = cipher.update(buffer, 0, length);
                    if(decryptedBuffer != null){
                        outStream.write(decryptedBuffer);
                    }
                }

                decryptedBuffer = cipher.doFinal();

                if(decryptedBuffer != null){
                    outStream.write(decryptedBuffer);
                }

                return outStream.toByteArray();
               
            }
        }
        return null;

    }

    public static Future<?> getImageFromBytes(byte[] bytes, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        
        Task<Image> task = new Task<Image>() {
            @Override
            public Image call() throws IOException{

                Image image = new Image (new ByteArrayInputStream(bytes));
                return image;
            }
        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }

    public static Image downloadImageAndEncryptFile(String urlString, SecretKey appKey, File downloadFile) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
        if(downloadFile != null){
            byte[] iV = Utils.getIv();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);
            cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

            URL url = new URL(urlString);
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", Utils.USER_AGENT);
            long contentLength = con.getContentLengthLong();

            if(downloadFile.isFile()){
                Files.delete(downloadFile.toPath());
            }

            try(
                InputStream inputStream = con.getInputStream();
                ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                FileOutputStream fileStream = new FileOutputStream(downloadFile);
            ){
            
                
                byte[] output;
                byte[] buffer = new byte[contentLength != -1 && contentLength < DEFAULT_BUFFER_SIZE ? (int) contentLength : DEFAULT_BUFFER_SIZE];

                int length;

                while ((length = inputStream.read(buffer)) != -1) {
                    byteOutputStream.write(buffer, 0 ,length);
                    output = cipher.update(buffer, 0, length);

                    if(output != null){
                        fileStream.write(output);
                    }
                }

                output = cipher.doFinal();

                if(output != null){
                    fileStream.write(output);
                }

                return new Image(new ByteArrayInputStream(byteOutputStream.toByteArray()));
            }
        }
        return null;
    }

    public static Future<?> dowloadAndEncryptFile(String urlString, SecretKey appKey, File downloadFile, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator){
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
                byte[] iV = Utils.getIv();

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);
                cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

                URL url = new URL(urlString);
                URLConnection con = url.openConnection();
                con.setRequestProperty("User-Agent", Utils.USER_AGENT);
                long contentLength = con.getContentLengthLong();
                try(
                    InputStream inputStream = con.getInputStream();
                    FileOutputStream fileStream = new FileOutputStream(downloadFile);
                ){
              
                    
                    byte[] output;
                    byte[] buffer = new byte[contentLength != -1 && contentLength < DEFAULT_BUFFER_SIZE ? (int) contentLength : DEFAULT_BUFFER_SIZE];

                    int length;
                    long downloaded = 0;
                    if(progressIndicator != null && contentLength != -1){
                        updateProgress(downloaded, contentLength);
                    }

                    while ((length = inputStream.read(buffer)) != -1) {

                        output = cipher.update(buffer, 0, length);

                        if(output != null){
                            fileStream.write(output);
                        }
                        downloaded += (long) length;

                        if(progressIndicator != null && contentLength != -1){
                            updateProgress(downloaded, contentLength);
                        }
                    }

                    output = cipher.doFinal();

                    if(output != null){
                        fileStream.write(output);
                    }

                    if(downloaded == contentLength){
                        return true;
                    }
                }

                return false;
            }

        };
      
        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }


        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }

    public static Future<?> encryptFileAndHash(SecretKey appKey, ExecutorService execService, File inputFile, File outputFile, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
                long contentLength = -1;

                 
                SecureRandom secureRandom = SecureRandom.getInstanceStrong();
                byte[] iV = new byte[12];
                secureRandom.nextBytes(iV);
                
                Blake2b digest = Blake2b.Digest.newInstance(32);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

                cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

                if (inputFile != null && inputFile.isFile() && outputFile != null && !inputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    contentLength = Files.size(inputFile.toPath());
                } else {
                    return null;
                }

                if (outputFile.isFile()) {
                    Files.delete(outputFile.toPath());
                }

                try(
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    FileInputStream inputStream = new FileInputStream(inputFile);
                ){
                    long fileSize = inputFile.length();
                    int bufferSize = fileSize < (8 * 1024) ? (int) fileSize :(8 * 1024);
                    
                    byte[] buffer = new byte[bufferSize];
                    byte[] output;
                    int length;
                    long copied = 0;
           
                    outputStream.write(iV);

                    while ((length = inputStream.read(buffer)) != -1) {

                        digest.update(buffer, 0, length);

                        output = cipher.update(buffer, 0, length);
                        if(output != null){
                            outputStream.write(output);
                        }
                        copied += (long) length;
                        
                        if(progressIndicator != null){
                            updateProgress(copied, contentLength);
                        }
                    }

                    output = cipher.doFinal();

                    if(output != null){
                        outputStream.write(output);
                    }

                    byte[] hashbytes = digest.digest();

                    HashData hashData = new HashData(hashbytes);
        
                   
                    return hashData;
                    

                }

            }

        };
      
        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }
    
    public static byte[] getIv() throws NoSuchAlgorithmException{
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iV = new byte[12];
        secureRandom.nextBytes(iV);
        return iV;
    }

    public static boolean encryptFile(SecretKey appKey, File inputFile, File outputFile ) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException, IllegalBlockSizeException, BadPaddingException {

        long contentLength = -1;
      
        byte[] iV = getIv();
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

        cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

        if (inputFile != null && inputFile.isFile() && outputFile != null && !inputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
            contentLength = Files.size(inputFile.toPath());
        } else {
            return false;
        }

        if (outputFile.isFile()) {
            Files.delete(outputFile.toPath());
        }

        try(
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            FileInputStream inputStream = new FileInputStream(inputFile);
        ){
            long fileSize = inputFile.length();
            int bufferSize = fileSize < (8 * 1024) ? (int) fileSize :(8 * 1024);
            
            byte[] buffer = new byte[bufferSize];
            byte[] output;
            int length;
            long copied = 0;
    
            outputStream.write(iV);

            while ((length = inputStream.read(buffer)) != -1) {


                output = cipher.update(buffer, 0, length);
                if(output != null){
                    outputStream.write(output);
                }
                copied += (long) length;
                
                
            }

            output = cipher.doFinal();

            if(output != null){
                outputStream.write(output);
            }

            if( contentLength == copied){
                return true;
            }

        }

        
        return false;
            
   
    }
    

    public static JsonObject readJsonFile(SecretKey appKey, File file) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        byte[] bytes = decryptFileToBytes(appKey,file);
        
        if(bytes != null){

            JsonElement jsonElement = new JsonParser().parse(new String(bytes));

            return jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
            
        }
        return null;

    }

    public static Future<?> readJsonFile(SecretKey appKey, File file, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        if(appKey != null && file != null && file.isFile()  && execService != null){
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call() throws InterruptedException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException{
    
                    JsonObject json = readJsonFile(appKey, file);

                    return json;
                }
            };
            task.setOnFailed(onFailed);

            task.setOnSucceeded(onSucceeded);

            return execService.submit(task);
        }
        return null;
    }
    
    
    public static Future<?> readJsonArrayFile(SecretKey appKey, File file, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        
        if(appKey != null && file != null){
            Task<Object> task = new Task<Object>() {
                @Override
                public Object call() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException, InterruptedException {
                    
                    byte[] bytes = decryptFileToBytes(appKey, file);
                    if(bytes != null){
                        JsonElement jsonElement = new JsonParser().parse(new String(bytes));
                        if (jsonElement != null && jsonElement.isJsonArray()) {
                            return jsonElement.getAsJsonArray();
                        }
                    }

                    return null;
                    
                }
            };
        
            task.setOnFailed(onFailed);

            task.setOnSucceeded(onSucceeded);

            return execService.submit(task);
        }
        return null;
    }


    public static void writeEncryptedString(SecretKey secretKey, File dataFile, String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
        if( secretKey != null && dataFile != null && str != null){

            byte[] iV = getIv();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            
            if (dataFile.isFile()) {
                Files.delete(dataFile.toPath());
            }
  
            
            try(
                ReaderInputStream inputStream = new ReaderInputStream(new StringReader(str), StandardCharsets.UTF_8);
                FileOutputStream outputStream = new FileOutputStream(dataFile);
            ){
                
                outputStream.write(iV);

                //int written = 0;
                int bufferLength =  1024;
                int length = 0;

                byte[] intputBuffer = new byte[bufferLength];
                byte[] output;

                while ((length = inputStream.read(intputBuffer)) != -1) {
                    output = cipher.update(intputBuffer, 0, length);
                    if(output != null){
                        outputStream.write(output);
                    }
                    //written += length;
                }

                output = cipher.doFinal();

                if(output != null){
                    outputStream.write(output);
                }

                
            }

 
        }

    }

    public static String readStringFile(SecretKey appKey, File file) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        byte[] bytes = decryptFileToBytes(appKey, file);
        if(bytes != null){
            return new String(bytes);
        }else{
            return null;
        }
    }

   

    public static Future<?> copyFileAndHash(File inputFile, File outputFile, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                long contentLength = -1;

                if (inputFile != null && inputFile.isFile() && outputFile != null && !inputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    contentLength = Files.size(inputFile.toPath());
                } else {
                    return null;
                }
                final Blake2b digest = Blake2b.Digest.newInstance(32);

                try(
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    FileInputStream inputStream = new FileInputStream(inputFile);
                ){
                    byte[] buffer = new byte[contentLength < (long) DEFAULT_BUFFER_SIZE ? (int) contentLength : DEFAULT_BUFFER_SIZE];

                    int length;
                    long copied = 0;

                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);
                        digest.update(buffer, 0, length);

                        copied += (long) length;
                        if(progressIndicator != null){
                            updateProgress(copied, contentLength);
                        }
                    }

                }

                return new HashData(digest.digest());

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }
    

    public static void getUrlFileHash(String urlString, File outputFile, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator, AtomicBoolean cancel) {
        
        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
               
                if (outputFile == null) {
                    return null;
                }
                Files.deleteIfExists(outputFile.toPath());
                URL url = new URL(urlString);
                Blake2b digest = Blake2b.Digest.newInstance(32);
                
                URLConnection con = url.openConnection();
                con.setRequestProperty("User-Agent", USER_AGENT);
                long contentLength = con.getContentLengthLong();
               
                if(progressIndicator != null){
                    updateProgress(0, contentLength);
                }
               try(  
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    InputStream inputStream = con.getInputStream();
                ){                

                    byte[] buffer = new byte[contentLength != -1 && contentLength < DEFAULT_BUFFER_SIZE ? (int) contentLength : DEFAULT_BUFFER_SIZE];

                    int length = 0;
                    long downloaded = 0;
                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);
                        digest.update(buffer, 0, length);
                    
                        downloaded += (long) length;
                        if(contentLength != -1 && progressIndicator != null){
                            updateProgress(downloaded, contentLength);
                        }

                        if(cancel.compareAndSet(true, false)){ 
                           
                            break;
                        }
                    }
                    if(contentLength == -1 || (contentLength == downloaded)){
                        byte[] hashbytes = digest.digest();

                        return new HashData(hashbytes);
                    }
                }
              
              
                Files.deleteIfExists(outputFile.toPath());
               
                return null;

            }

        };

        if(progressIndicator != null){
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


    public static void pingIP(String ip, int timeout, ExecutorService execService, EventHandler< WorkerStateEvent> onSucceeded, EventHandler< WorkerStateEvent> onFailed){
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            public Boolean call() {

                try{
                    InetAddress address = InetAddress.getByName(ip);
                    return address.isReachable(timeout);

                
                }catch(IOException e){
                   
                }
                return false;
            }

        };

        task.setOnSucceeded(onSucceeded);
        task.setOnFailed(onFailed);

    
        execService.submit(task);
    }
   

    public static void pingIPconsole(String ip, int pingTimes, ExecutorService execService, EventHandler< WorkerStateEvent> onSucceeded, EventHandler< WorkerStateEvent> onFailed){

        Task<Ping> task = new Task<Ping>() {
            @Override
            public Ping call()throws IOException {

                String[] cmd = {"bash", "-c", "ping -c 3 " + ip};
                Ping ping = new Ping(false, "", -1);

                String line;
        

                Process proc = Runtime.getRuntime().exec(cmd);

                try(
                    BufferedReader wmicStderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                ){

                    while ((line = wmicStderr.readLine()) != null) {
                        ping.setError(ping.getError() + line + " ");
                    }

                    if(!ping.getError().equals(""))
                    {
                        return ping;
                    }
                   
                }catch(IOException e){
                    ping.setError(e.toString());
                    return ping;
                }
                
                try(
            
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                ){

                
                    String timeString = "time=";

                    while (true) {
                    
                        line = stdInput.readLine();
                

                        if(line == null){
                            break;
                        }

                        
                        
                    
                
                        int indexOftimeString = line.indexOf(timeString);

                        if (line.indexOf("service not known") > -1) {
                            ping.setAvailable(false);
                            ping.setError("Unreachable");
                            break;
                        }

                        if (line.indexOf("timed out") > -1) {

                            ping.setAvailable(false);
                            ping.setError( "Timed out");
                            break;
                        }

                        if (indexOftimeString > -1) {
                            int lengthOftime = timeString.length();

                            int indexOfms = line.indexOf("ms");

                            ping.setAvailable(true);

                            String time = line.substring(indexOftimeString + lengthOftime, indexOfms).trim();
        

                            ping.setAvgPing(Double.parseDouble(time));
                        
                        }

                        String avgString = "min/avg/max/mdev = ";
                        int indexOfAvgString = line.indexOf(avgString);

                        if (indexOfAvgString > -1) {
                            int lengthOfAvg = avgString.length();

                            String avgStr = line.substring(indexOfAvgString + lengthOfAvg);
                            int slashIndex = avgStr.indexOf("/");

                            avgStr = avgStr.substring(slashIndex+1, avgStr.indexOf("/",slashIndex + 1) ).trim();
                        
                            ping.setAvailable(true);
                            ping.setAvgPing(Double.parseDouble(avgStr));
                    
                        }

                    }
                }catch(Exception e){
                    try {
                        Files.writeString(App.logFile.toPath(),e.toString() +"\n" , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {
            
                    }
            
                }

                try {
                    Files.writeString(App.logFile.toPath(), ping.getJsonObject().toString() +"\n" , StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
        
                }


                return ping;
            }
        };
     
        task.setOnSucceeded(onSucceeded);
        task.setOnFailed(onFailed);

    
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

    public static boolean isTextZero(String str){
        str = str.strip();
        
        if(str.length() == 0){
            return true;
        }

        int index = str.indexOf(".");

        String leftSide = index != -1 ? str.substring(0, index) : str;
        
        String rightSide = index != -1 ? str.substring(index + 1) : "";
        
        for (int i = 0 ; i < leftSide.length() ; i++){
            String c = leftSide.substring(i, i+1);
            if(!c.equals("0")){
                return false;
            }
        }

        for (int i = 0 ; i < rightSide.length() ; i++){
            String c = rightSide.substring(i, i+1);
            if(!c.equals("0")){
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
