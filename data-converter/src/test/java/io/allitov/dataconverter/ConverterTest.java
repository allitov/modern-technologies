package io.allitov.dataconverter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void convertTextThrowsForUnsupportedExtension() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("x", "xml", "json"));

        assertEquals("Unsupported file extension: xml", ex.getMessage());
    }

    @Test
    void convertTextLowercasesAndStripsLeadingDotInUnsupportedExtensionMessage() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("x", ".XML", "json"));

        assertEquals("Unsupported file extension: xml", ex.getMessage());
    }

    @Test
    void convertTextNormalizesYmlAliasToYaml() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("x", "yml", "json"));

        assertEquals("Conversion from yaml to json is not implemented", ex.getMessage());
    }

    @Test
    void convertTextThrowsForIdenticalFormats() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("[]", "json", "json"));

        assertEquals("Input and output formats must differ", ex.getMessage());
    }

    @Test
    void convertTextThrowsForIdenticalFormatsCaseInsensitive() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("[]", "JSON", "json"));

        assertEquals("Input and output formats must differ", ex.getMessage());
    }

    @Test
    void convertTextThrowsNotImplementedForCsvToJson() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("a,b\n", "csv", "json"));

        assertEquals("Conversion from csv to json is not implemented", ex.getMessage());
    }

    @Test
    void convertTextThrowsNotImplementedForJsonToCsv() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("[]", "json", "csv"));

        assertEquals("Conversion from json to csv is not implemented", ex.getMessage());
    }

    @Test
    void convertTextThrowsNotImplementedForYamlToJson() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("- a: 1\n", "yaml", "json"));

        assertEquals("Conversion from yaml to json is not implemented", ex.getMessage());
    }

    @Test
    void convertTextThrowsNotImplementedForYamlToCsv() {
        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertText("- a: 1\n", "yaml", "csv"));

        assertEquals("Conversion from yaml to csv is not implemented", ex.getMessage());
    }

    @Test
    void convertFileThrowsForInputPathWithoutExtension() {
        String input = tempDir.resolve("input-no-extension").toString();
        String output = tempDir.resolve("output.json").toString();

        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertFile(input, output));

        assertEquals("File has no extension: " + input, ex.getMessage());
    }

    @Test
    void convertFileThrowsForMissingInputFile() {
        String input = tempDir.resolve("missing.csv").toString();
        String output = tempDir.resolve("out.json").toString();

        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertFile(input, output));

        assertEquals("Cannot open input file: " + input, ex.getMessage());
    }

    @Test
    void convertFileFailsBeforeCreatingOutputFile() throws Exception {
        Path input = tempDir.resolve("in.csv");
        Files.writeString(input, "a,b\n1,2\n");
        Path output = tempDir.resolve("out.json");

        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertFile(input.toString(), output.toString()));

        assertEquals("Conversion from csv to json is not implemented", ex.getMessage());
        assertFalse(Files.exists(output));
    }

    @Test
    void convertFileChecksExtensionBeforeReadingInput() {
        String input = tempDir.resolve("missing-no-extension").toString();
        String output = tempDir.resolve("out.json").toString();

        ConversionException ex = assertThrows(ConversionException.class,
                () -> Converter.convertFile(input, output));

        assertEquals("File has no extension: " + input, ex.getMessage());
    }
}
