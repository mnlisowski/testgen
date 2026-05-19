package com.mlisows.testgen.infrastructure.parser;

import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserTypeIndexAnalyzerTest {

    @Test
    void shouldAnalyzeClassesInterfacesAndEnums() {
        JavaParserTypeIndexAnalyzer analyzer = new JavaParserTypeIndexAnalyzer();

        ProjectTypeIndex index = analyzer.analyze(List.of(
                Path.of("src/test/resources/sample/PaymentGateway.java"),
                Path.of("src/test/resources/sample/Order.java"),
                Path.of("src/test/resources/sample/CustomerType.java")
        ));

        Optional<TypeInfo> paymentGateway = index.findByName("PaymentGateway");
        Optional<TypeInfo> order = index.findByName("Order");
        Optional<TypeInfo> customerType = index.findByName("CustomerType");

        assertTrue(paymentGateway.isPresent());
        assertTrue(order.isPresent());
        assertTrue(customerType.isPresent());

        assertEquals(TypeKind.INTERFACE, paymentGateway.get().getKind());
        assertEquals(TypeKind.CLASS, order.get().getKind());
        assertEquals(TypeKind.ENUM, customerType.get().getKind());
    }
}
