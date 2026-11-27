package com.banking.customerservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.customerservice.application.port.in.CreateCustomerUseCase;
import com.banking.customerservice.application.port.in.DeactivateCustomerUseCase;
import com.banking.customerservice.application.port.in.GetCustomerUseCase;
import com.banking.customerservice.application.port.in.ListCustomersUseCase;
import com.banking.customerservice.application.port.in.UpdateCustomerUseCase;
import com.banking.customerservice.support.MongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class CustomerServiceApplicationTests extends MongoIntegrationTest {

  @Autowired private ApplicationContext context;

  /** The context starts and wires exactly one handler behind every inbound port. */
  @Test
  void contextLoads() {
    assertThat(context.getBeanNamesForType(CreateCustomerUseCase.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(GetCustomerUseCase.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(ListCustomersUseCase.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(UpdateCustomerUseCase.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(DeactivateCustomerUseCase.class)).hasSize(1);
  }
}
