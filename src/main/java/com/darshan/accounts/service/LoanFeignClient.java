package com.darshan.accounts.service;

import com.darshan.accounts.model.Customer;
import com.darshan.accounts.model.Loans;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("loans")
public interface LoanFeignClient {

@PostMapping(value = "/myLoans")
List<Loans> getLoansDetail(@RequestBody Customer customer);

}
