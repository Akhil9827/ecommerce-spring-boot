package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "payments")
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne(mappedBy = "payment",cascade ={CascadeType.PERSIST,CascadeType.MERGE})
    private Order order;

    @NotBlank
    @Size(min = 4,message = "Payment method must contain at least 4 characters")
    private String paymentMethod;

    private String pgPaymentId;
    private String pgStatus;
    private String pgResponseMessage;
    private String pgName;

    public Payment(Long paymentId,String pgPaymentId,String pgStatus,String pgResponseMessage,String pgName){ //A custom constructor is used for partial object initialization when only specific fields are required instead of all entity fields.
        this.paymentId=paymentId;
        this.pgPaymentId=pgPaymentId;
        this.pgStatus=pgStatus;
        this.pgResponseMessage=pgResponseMessage;
        this.pgName=pgName;

    }
}
