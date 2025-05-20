import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Main {
    // Configurable constants
    public static final String PATCH_NAME = "EuphoriaPatches";
    public static final String VERSION = "_r5.5.1";
    public static final String PATCH_VERSION = "_1.6.3";
    public static final List<String> STYLES = Arrays.asList("Reimagined", "Unbound");

    // Encryption constants
    private static final byte[] KEY_BYTES = { -93, 70, -5, -49, -51, -113, 103, 109, 69, 18, -13, 63, -106, -18, 115, 6 };
    private static final byte[] IV_BYTES = { -91, -62, 93, 55, 58, 21, -60, -82, 82, -54, 87, -96, -88, 112, 45, -105 };

    // Global variables for input and output paths
    public static Path baseDir = Paths.get("").toAbsolutePath();
    public static Path sourceFolder = baseDir.resolve("SourceFolder");
    public static Path destinationFolder = baseDir.resolve("DestinationFolder");

    // ANSI escape codes for colored output
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";

    public static void main(String[] args) throws Exception {
        createFolders();
        deleteNonMatchingFiles();
        clearCommonFolder();
        encodeFiles();
    }

    public static void createFolders() throws Exception {
        Files.createDirectories(sourceFolder);
        System.out.println("Created or verified Source folder: " + BLUE + sourceFolder + RESET);
        Files.createDirectories(destinationFolder);
        System.out.println("Created or verified Destination folder: " + BLUE + destinationFolder + RESET);

        // Create common folder (without version subfolder)
        Path commonFolder = destinationFolder.resolve("common");
        Files.createDirectories(commonFolder);
        System.out.println("Created or verified Common folder: " + BLUE + commonFolder + RESET);

        for (String style : STYLES) {
            Path styleFolder = destinationFolder.resolve(style + "/" + PATCH_VERSION);
            Files.createDirectories(styleFolder);
            System.out.println("Created or verified Style folder: " + BLUE + styleFolder + RESET);
        }
    }

    public static void clearCommonFolder() {
        Path commonFolder = destinationFolder.resolve("common");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(commonFolder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    System.out.println("Clearing file from common folder: " + file);
                    Files.delete(file);
                }
            }
        } catch (Exception e) {
            System.err.println(RED + "Error clearing common folder: " + e.getMessage() + RESET);
        }
    }

    public static void deleteNonMatchingFiles() {
        // List files in the source folder and delete those that don't contain PATCH_VERSION
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && !file.getFileName().toString().contains(PATCH_VERSION)) {
                    System.out.println(RED + "Deleting file: " + file + RESET);
                    Files.delete(file);
                }
            }
        } catch (Exception e) {
            System.err.println(RED + "Error occurred while deleting files: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    public static void encodeFiles() throws Exception {
        final SecretKeySpec KEY = new SecretKeySpec(KEY_BYTES, "AES");
        final IvParameterSpec IV = new IvParameterSpec(IV_BYTES);

        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, KEY, IV);

        boolean filesFound = false;

        for (String style : STYLES) {
            String fileName = "Complementary" + style + VERSION + " + " + PATCH_NAME + PATCH_VERSION + ".zip";
            Path filePath = sourceFolder.resolve(fileName);

            if (Files.exists(filePath)) {
                // Save to style-specific folder
                Path styleOutputPath = destinationFolder.resolve(style + "/" + PATCH_VERSION);
                encodeFile(aesCipher, filePath, styleOutputPath);

                // Also save to common folder (without version subfolder)
                Path commonOutputPath = destinationFolder.resolve("common");
                encodeFile(aesCipher, filePath, commonOutputPath);

                filesFound = true;
            } else {
                System.out.println(RED + "File not found: " + filePath + RESET);
            }
        }

        if (!filesFound) {
            System.out.println(RED + "No files were found to encode." + RESET);
        }
    }

    private static void encodeFile(Cipher aesCipher, Path inputPath, Path outputDirectory) throws Exception {
        byte[] clearText = Files.readAllBytes(inputPath);
        byte[] ciphertext = aesCipher.doFinal(clearText);

        String encodedFileName = Base64.getEncoder().withoutPadding().encodeToString(inputPath.getFileName().toString().getBytes());
        Path outputPath = outputDirectory.resolve(encodedFileName);

        Files.write(outputPath, ciphertext);
        System.out.println(GREEN + "File encoded successfully: " + BLUE + outputPath + RESET);
    }
}
