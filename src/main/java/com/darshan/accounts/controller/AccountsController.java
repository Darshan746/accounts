package com.darshan.accounts.controller;

import com.darshan.accounts.config.AccountsServiceConfig;
import com.darshan.accounts.model.*;
import com.darshan.accounts.repository.AccountsRepository;
import com.darshan.accounts.service.CardsFeignClient;
import com.darshan.accounts.service.LoanFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class AccountsController {

    @Autowired
    private AccountsRepository accountsRepository;
    @Autowired
    AccountsServiceConfig accountsConfig;

    @Autowired
    public LoanFeignClient loanFeignClient;

    @Autowired
    public CardsFeignClient cardsFeignClient;
    @PostMapping("/myAccount")
    public Accounts getAccountDetails(@RequestBody Customer customer) {
       return accountsRepository.findByCustomerId(customer.getCustomerId());
    }

    @GetMapping("/account/properties")
    public String getPropertyDetails() throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        Properties properties = new Properties(accountsConfig.getMsg(), accountsConfig.getBuildVersion(),
                accountsConfig.getMailDetails(), accountsConfig.getActiveBranches());
       return ow.writeValueAsString(properties);
    }


    @PostMapping("/myCustomerDetails")
    @CircuitBreaker(name = "detailsForCustomerSupportApp", fallbackMethod ="myCustomerDetailsFallBack")
    @Retry(name = "retryForCustomerDetails", fallbackMethod = "myCustomerDetailsFallBack")
    public CustomerDetails getCustomerDetails(@RequestBody Customer customer){
       // Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
        Accounts accounts =  new Accounts();
        accounts.setCustomerId(1);
        List<Loans> loans = loanFeignClient.getLoansDetail(customer);
        List<Cards> cards = cardsFeignClient.getCardDetails(customer);
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setAccounts(accounts);
        customerDetails.setCards(cards);
        customerDetails.setLoans(loans);
        return customerDetails;
    }

    /**
     * this is the fallback implementation method, where we need to have the method parameter same as controller method parameter and one extra throwable
     * param as to track the original exception
     * @param customer
     * @param t
     * @return
     */
    private CustomerDetails myCustomerDetailsFallBack(@RequestBody Customer customer, Throwable t) {
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
        List<Loans> loans = loanFeignClient.getLoansDetail(customer);
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setAccounts(accounts);
        customerDetails.setLoans(loans);
        return customerDetails;

    }

    @GetMapping("/sayHello")
    @RateLimiter(name = "sayHello", fallbackMethod = "sayHelloFallback")
    public String sayHello() {
        return "Hello, Welcome to EazyBank";
    }

    private String sayHelloFallback(Throwable t) {
        return "Hi, Welcome to EazyBank";
    }

}
