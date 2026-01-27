package com.kori.application.port.out;

public interface CodeGeneratorPort {
    /**
     * @return exactly 6 digits, left-padded (000000..999999)
     */
    String next6Digits();
}
