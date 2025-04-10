package net.krusher.tecmosdk;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static final String TEXT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?.,0123456789:'\" ";
    private static final int MIN_CHARS = 4;
    private static final int CHECKSUM_OFFSET = 398; // 256 + 142

    public static void main( String[] args ) throws IOException, InterruptedException {

        Log.pnl("TecmoSDK by Krusher - Programa bajo licencia GPL 3");

        // if there are no arguments, print usage
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        // check mode
        if (args[0].equals("x")) {
            extract(args[1]);
        } else if (args[0].equals("i")) {
            inject(args[1]);
        } else {
            printUsage();
            System.exit(1);
        }

        Log.pnl("Termina ejecución");

        System.exit(0);

    }

    private static void extract(String file) throws IOException {
        Log.pnl("Modo: Extraer:");
        Log.pnl("Leyendo archivo: " + file);
        byte[] fileData = Files.readAllBytes(Paths.get(file));
        Log.pnl("Extrayendo textos...");
        List<Texticle> texts = extractTexts(fileData);
        Log.pnl("Extracción terminada, escribiendo salida...");
        writeTexts(texts, file);
        Log.pnl("Salida escrita en: " + file + ".txt");
    }

    public static void writeTexts(List<Texticle> texticles, String file) throws IOException {
        File outputFile = new File(file + ".txt");
        FileWriter fileWriter = new FileWriter(outputFile);
        PrintWriter printWriter = new PrintWriter(fileWriter);

        for (Texticle texticle : texticles) {
            printWriter.println(texticle.format());
        }

        printWriter.close();
    }

    public static List<Texticle> extractTexts(byte[] fileData) {
        List<Texticle> texts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean inText = false;
        int length = 0;
        for (int i = 0; i < fileData.length; i++) {
            if (isChar(fileData[i])) {
                inText = true;
                buffer.append((char) fileData[i]);
                length++;
            } else if (inText) {
                if (length > MIN_CHARS) {
                    texts.add(new Texticle(i - length, length, buffer.toString()));
                }
                length = 0;
                buffer = new StringBuilder();
            }
        }
        return texts;
    }

    private static boolean isChar(byte fileDatum) {
        for (byte theChar : TEXT_CHARS.getBytes(StandardCharsets.ISO_8859_1)) {
            if (fileDatum == theChar) {
                return true;
            }
        }
        return false;
    }

    private static void inject(String file) throws IOException, InterruptedException {
        Log.pnl("Modo: Inyectar:");
        Log.pnl("Leyendo archivo: " + file);
        byte[] fileData = Files.readAllBytes(Paths.get(file));
        Log.pnl("Inyectando bloques comprimidos...");
        injectCompressedBlocks(fileData);
        Log.pnl("Inyectando textos...");
        List<Texticle> texticles = extractTexts(file);
        for (Texticle texticle : texticles) {
            System.arraycopy(texticle.toAsciiBytes(), 0, fileData, texticle.address(), texticle.size());
        }
        Log.pnl("Inyección terminada.");
        Log.pnl("Arreglando checksum...");
        fixChecksum(fileData);
        Log.pnl("Escribiendo salida...");
        File outputFile = new File(file + ".patched.bin");
        Files.write(outputFile.toPath(), fileData);
        Log.pnl("Salida escrita en: " + outputFile.getAbsolutePath());
    }

    private static void injectCompressedBlocks(byte[] fileData) throws IOException, InterruptedException {
        // get list of files in dumps_dir directory
        File[] files = new File("dumps_dir").listFiles();
        if (files == null || files.length == 0) {
            Log.pnl("No se encontraron archivos comprimidos en la carpeta 'dumps_dir'");
            return;
        }
        for (File file : files) {
            if (!file.getName().startsWith("dump_") || file.getName().contains(".cmp.")) {
                continue;
            }
            Log.p(" " + file.getName());
            execute("cui_lzcaptsu.exe", "dumps_dir\\" + file.getName());
            String addressHex = file.getName().substring(5, file.getName().lastIndexOf('.'));
            int addressDecimal = Integer.parseInt(addressHex, 16);
            byte[] compressedData = Files.readAllBytes(Paths.get("dumps_dir/" + file.getName().replace(".bin", ".cmp.bin")));
            System.arraycopy(compressedData, 0, fileData, addressDecimal, compressedData.length);
        }
        Log.pnl();
    }

        public static List<Texticle> extractTexts(String file) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(file + ".txt"));
        List<Texticle> texticles = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("#");
            if (parts.length == 3) {
                int address = Integer.parseInt(parts[0]);
                int size = Integer.parseInt(parts[1]);
                String text = parts[2];
                if (text.length() > size) {
                    text = text.substring(0, size);
                }
                texticles.add(new Texticle(address, size, text));
            }
        }
        return texticles;
    }

    private static void fixChecksum(byte[] romBytes) {

        int previousChecksum = readWord(romBytes, CHECKSUM_OFFSET);
        Log.pf("Checksum existente: 0x%04x%n", previousChecksum);

        int checksum = calculateChecksum(romBytes);
        Log.pf("Checksum válido: 0x%04x%n", checksum);

        if (previousChecksum != checksum) {
            Log.pnl("El checksum ha cambiado, arreglando...");
            writeWord(romBytes, CHECKSUM_OFFSET, checksum);
        } else {
            Log.pnl("¡El checksum no ha cambiado!");
        }
    }

    private static int calculateChecksum(byte[] rom) {
        int checksum = 0;
        for (int i = 512; i + 1 < rom.length; i += 2) {
            int word = ((rom[i] & 0xFF) << 8) | (rom[i + 1] & 0xFF);
            checksum = (checksum + word) & 0xFFFF;
        }
        return checksum;
    }

    private static int readWord(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static void writeWord(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    private static void printUsage() {
        Log.pnl("Debe especificarse modo y archivo");
        Log.pnl("Ejemplos: x \"rom a extraer.bin\"");
        Log.pnl("          i \"rom a inyectar.bin\"");
    }

    public static void execute(String... parameters) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(parameters);
        processBuilder
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            Log.pnl("Error al ejecutar el comando: " + exitCode);
            System.exit(exitCode);
        }
    }
}
