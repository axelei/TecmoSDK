package net.krusher.tecmosdk;

public record LogData (int offset, int compressedSize, int uncompressedSize){

    public static LogData parseLine(String line) {
        //03C1B3 (01302 / 08192)
        String[] parts = line.split(" ");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid line format: " + line);
        }
        String offsetStr = parts[0];
        String compressedSizeStr = parts[1].replace("(", "").trim();
        String uncompressedSizeStr = parts[3].replace(")", "").trim();
        int offset = Integer.parseInt(offsetStr, 16);
        int compressedSize = Integer.parseInt(compressedSizeStr);
        int uncompressedSize = Integer.parseInt(uncompressedSizeStr);
        return new LogData(offset, compressedSize, uncompressedSize);
    }
}
