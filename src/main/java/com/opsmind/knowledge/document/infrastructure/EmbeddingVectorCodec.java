package com.opsmind.knowledge.document.infrastructure;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class EmbeddingVectorCodec {

    public String encode(double[] vector) {
        return Arrays.stream(vector)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(","));
    }

    public double[] decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new double[0];
        }
        return Arrays.stream(encoded.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
