package com.ecommerce.project.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Email
    @Column(nullable = false)  //@Column(nullable=false) creates a NOT NULL constraint at database level Protects database schema
    private String email;

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    private List<OrderItem> orderItems=new ArrayList<>();  //One order has multiple order items

    private LocalDate orderDate;  //LocalDate is a Java class from:java.time package introduced in Java 8. Used to store: date only without:time timezone

    @OneToOne
    @JoinColumn(name = "payment_id")  //Order will own the relationship one order has one paymnet the payment id will be created as foreign key in order table
    private Payment payment;

    private Double totalAmount;
    private String orderStatus;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;  //Order will own the relationship & address id will be created as foreign key in order table One order is linked to one address but orders can have the same address



}
