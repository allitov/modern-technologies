package io.allitov.dataconverter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Main {

    static void main(String[] args) {
        if (args.length != 2) {
            log.error("Usage: dataconv <input_file> <output_file>");
            System.exit(2);
        }
        try {
            Converter.convertFile(args[0], args[1]);
            System.exit(0);
        } catch (Exception e) {
            String message = e.getMessage();
            log.error("dataconv: {}", message != null ? message : e.toString());
            System.exit(1);
        }
    }
}
