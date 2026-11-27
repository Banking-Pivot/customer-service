package com.banking.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the customer service, which registers and maintains the personal and business
 * customers of the bank.
 */
@SpringBootApplication
public class CustomerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CustomerServiceApplication.class, args);
  }
}
