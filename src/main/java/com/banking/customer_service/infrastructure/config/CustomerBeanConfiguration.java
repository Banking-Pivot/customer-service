package com.banking.customer_service.infrastructure.config;

import com.banking.customer_service.application.handler.command.CreateCustomerHandler;
import com.banking.customer_service.application.handler.command.DeactivateCustomerHandler;
import com.banking.customer_service.application.handler.command.UpdateCustomerHandler;
import com.banking.customer_service.application.handler.query.GetCustomerHandler;
import com.banking.customer_service.application.handler.query.ListCustomersHandler;
import com.banking.customer_service.application.port.in.CreateCustomerUseCase;
import com.banking.customer_service.application.port.in.DeactivateCustomerUseCase;
import com.banking.customer_service.application.port.in.GetCustomerUseCase;
import com.banking.customer_service.application.port.in.ListCustomersUseCase;
import com.banking.customer_service.application.port.in.UpdateCustomerUseCase;
import com.banking.customer_service.application.port.out.CustomerReadPort;
import com.banking.customer_service.application.port.out.CustomerWritePort;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the application handlers without putting Spring annotations on them.
 * Command handlers depend only on the write port; query handlers only on the read port.
 */
@Configuration
public class CustomerBeanConfiguration {

    @Bean
    public Clock customerClock() {
        return Clock.systemUTC();
    }

    @Bean
    public CreateCustomerUseCase createCustomerUseCase(CustomerWritePort writePort, Clock clock) {
        return new CreateCustomerHandler(writePort, clock);
    }

    @Bean
    public UpdateCustomerUseCase updateCustomerUseCase(CustomerWritePort writePort, Clock clock) {
        return new UpdateCustomerHandler(writePort, clock);
    }

    @Bean
    public DeactivateCustomerUseCase deactivateCustomerUseCase(CustomerWritePort writePort, Clock clock) {
        return new DeactivateCustomerHandler(writePort, clock);
    }

    @Bean
    public GetCustomerUseCase getCustomerUseCase(CustomerReadPort readPort) {
        return new GetCustomerHandler(readPort);
    }

    @Bean
    public ListCustomersUseCase listCustomersUseCase(CustomerReadPort readPort) {
        return new ListCustomersHandler(readPort);
    }
}
