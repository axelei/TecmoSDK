package net.krusher.tecmosdk;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A "texticle" is a structure that contains an address, room, and text.
 */
public record Texticle(int address, int room, String text, Integer pointerAddress) {

    public static final int PAD = 8;
    public static final String FORMAT = "%0" + PAD + "d";

    public static final byte ASCII_SPACE = 0x20;

    public String format() {
        String pointerAddressStr = Optional.ofNullable(pointerAddress).map(address -> "#" + address).orElse("");
        return String.format(FORMAT, address) + "#" + String.format(FORMAT, room) + "#" + text + pointerAddressStr;
    }

    public byte[] toAsciiBytes() {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

}
