package com.savvaServices.customer;

import com.savvaServices.amqp.RabbitMQMessageProducer;
import com.savvaServices.clients.fraud.FraudCheckReponse;
import com.savvaServices.clients.fraud.FraudClient;
import com.savvaServices.clients.notification.NotificationClient;
import com.savvaServices.clients.notification.NotificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public record CustomerService(CustomerRepository customerRepository, RestTemplate restTemplate,
                              FraudClient fraudClient,
                              RabbitMQMessageProducer rabbitMQMessageProducer) {
    public void registerCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        Customer customer = Customer.builder()
                .firstName(customerRegistrationRequest.firstName())
                .lastName(customerRegistrationRequest.lastName())
                .email(customerRegistrationRequest.email())
                .build();
        customerRepository.saveAndFlush(customer);
//        FraudCheckReponse fraudCheckReponse = restTemplate.getForObject(
//                "http://FRAUD/api/v1/fraud-check/{customerId}",
//                FraudCheckReponse.class,
//                customer.getId());

        FraudCheckReponse fraudCheckReponse =
                fraudClient.isFraudster(customer.getId());
        if (fraudCheckReponse.isFraudster()) {
            throw new IllegalStateException("fraudster");
        }


        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to savvaMey...",
                        customer.getFirstName())
        );
//        notificationClient.sendNotification(notificationRequest);
        rabbitMQMessageProducer.publish(notificationRequest,
                "internal.exchange", "internal.notification.routing-key");

    }
}
