package io.allitov.dataconverter;

import lombok.experimental.UtilityClass;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@UtilityClass
public final class Converter {

    public String convertText(String input, String inputExtension, String outputExtension) {
        String inExt = normalizeExt(inputExtension);
        String outExt = normalizeExt(outputExtension);
        if (inExt.equals(outExt)) {
            throw new ConversionException("Input and output formats must differ");
        }
        throw new ConversionException("Conversion from " + inExt + " to " + outExt + " is not implemented");
    }

    public void convertFile(String inputPath, String outputPath) {
        String inputExtension = extension(inputPath);
        String outputExtension = extension(outputPath);
        String input = readInput(inputPath);
        String converted = convertText(input, inputExtension, outputExtension);
        writeOutput(outputPath, converted);
    }

    private String normalizeExt(String extension) {
        String ext = extension.toLowerCase(Locale.ROOT);
        if (!ext.isEmpty() && ext.charAt(0) == '.') {
            ext = ext.substring(1);
        }
        if (ext.equals("yml")) {
            ext = "yaml";
        }
        if (!ext.equals("csv") && !ext.equals("json") && !ext.equals("yaml")) {
            throw new ConversionException("Unsupported file extension: " + ext);
        }
        return ext;
    }

    private String extension(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            throw new ConversionException("File has no extension: " + path);
        }
        return path.substring(dot + 1);
    }

    private String readInput(String path) {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException _) {
            throw new ConversionException("Cannot open input file: " + path);
        }
    }

    private void writeOutput(String path, String content) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (IOException _) {
            throw new ConversionException("Cannot open output file: " + path);
        }
    }
}
