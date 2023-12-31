package com.gl.BulkProcessing;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.LongStream;

@SpringBootApplication
public class BulkProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BulkProcessingApplication.class, args);
    }

}

@RestController
class CustomerController {
    private final CustomerRepo repo;

    public CustomerController(CustomerRepo repo) {
        this.repo = repo;
    }

    @GetMapping("/customers")
    public List<Customer> all() {
        return repo.findAll();
    }

    @GetMapping("/customersByPagination")
    public Page<Customer> customersByPagination(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String orderBy) {
        Sort sort = null;
        if (orderBy.equalsIgnoreCase("desc")) {
            sort = Sort.by(sortBy).descending();
        } else {
            sort = Sort.by(sortBy).ascending();
        }
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        System.out.println("pageRequest = " + pageRequest);
        return repo.findAll(pageRequest);
    }

    @PostMapping("/customer")
    public Customer createOne(@RequestBody Customer customer) {
        return repo.save(customer);
    }

    @PostMapping("/bulkCreateCustomers")
    public List<Customer> createMany(@RequestBody List<Customer> customers) {
        return repo.saveAll(customers);
    }

    @GetMapping("/customersByIds")
    public List<Customer> customersByIds(@RequestParam List<Long> ids) {
        return repo.findAllById(ids);
    }

    @GetMapping("/customersById/{id}")
    public Customer customersById(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer id: [" + id + "] is not found"));
    }

    @GetMapping("/generatedCustomers")
    public List<Customer> generatedCustomer(@RequestParam int limit) {
        List<Customer> customers =
                LongStream
                        .rangeClosed(1, limit)
                        .mapToObj(id -> Customer
                                .builder()
                                .name("EndPointGeneratedCustomerName_" + id)
                                .build())
                        .toList();
        return repo.saveAll(customers);
    }
}

interface CustomerRepo extends JpaRepository<Customer, Long> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
class Customer {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
}

@Configuration
class LoadDatabase {
    @Bean
    CommandLineRunner loadDB(CustomerRepo repo) {
        return args -> {
            List<Customer> customers =
                    LongStream
                            .rangeClosed(1, 100)
                            .mapToObj(id -> Customer
                                    .builder()
                                    .name("CommandLineRunnerGeneratedCustomerName_" + id)
                                    .build())
                            .toList();
            repo.saveAll(customers);
            System.out.println("Initial loading data is completed.");
        };
    }
}

class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}

@ControllerAdvice
class MyExceptionHandling {
    @ResponseBody
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String getMessage(CustomerNotFoundException exception) {
        return exception.getMessage();
    }
}