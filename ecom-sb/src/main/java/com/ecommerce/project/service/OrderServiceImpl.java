package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    CartRepository cartRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartService cartService;

    @Autowired
    ModelMapper modelMapper;

    /// We need to convert the cart to order
    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        //getting User cart
        Cart cart=cartRepository.findCartByEmail(emailId);
        if(cart==null){
            throw new ResourceNotFoundException("Cart","email",emailId);
        }

        Address address=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("Address","addressId",addressId));

        ///Create a new order with payment info

        Order order=new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted");
        order.setAddress(address);

        Payment payment=new Payment(paymentMethod,pgPaymentId,pgStatus,pgResponseMessage,pgName);//We are creating paymnet object
        payment.setOrder(order);//We are setting order to the paymnet
        payment=paymentRepository.save(payment);//Then saving the paymnet
        order.setPayment(payment);//Then setting paymnet to the order beacuse its bidirectional mapping paymnet to order so we need to update both the sides beacuse here first time relationship is establishing so for the first time we need to update both the sides

        Order savedOrder=orderRepository.save(order);

        ///Get items from cart into order item     (Because:Cart = temporary Order = permanent purchase record)

        List<CartItem> cartItems=cart.getCartItems();
        if(cartItems.isEmpty()){
            throw new APIException("Cart is empty");
        }

        List<OrderItem>orderItems=new ArrayList<>();//Create Empty OrderItem List Now you are preparing:CartItem  →  OrderItem conversion
        for (CartItem cartItem:cartItems){//Loop Through Cart Items
            OrderItem orderItem=new OrderItem();//In the cartitems many products are there so for each product we are creating a orderitem object Each CartItem represents:
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);//Each OrderItem MUST know:Which order do I belong to?” orderitem<-->order orderitem owns the relationship so after this orderid will be created as foreign key in orderitem table

            orderItems.add(orderItem);
        }

        orderItems=orderItemRepository.saveAll(orderItems);//after save it will return list of saved orderitems

        /// This is the post order related task

        ///Update product stock
        cart.getCartItems().forEach(item->{
            int quantity=item.getQuantity();
            Product product=item.getProduct();
            product.setQuantity(product.getQuantity()-quantity);
            productRepository.save(product);

            ///Clear the cart
            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
        });

        ///Send back the order summary
        OrderDTO orderDTO=modelMapper.map(savedOrder,OrderDTO.class);
        orderItems.forEach(item-> //orderdto has list of orderitemdto so we are converting each item to orderitemdto & adding it to orderdto
                orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));
        orderDTO.setAddressId(addressId);

        return orderDTO;


    }
}
