package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<Cart,Long> {
    @Query("SELECT c FROM Cart c WHERE c.user.email= ?1")  //For the given email, it is returning the corresponding Cart object.
    Cart findCartByEmail(String email);  //Here its nested field cart is associated with user & user having email

    @Query("SELECT c FROM Cart c WHERE c.user.email= ?1 AND c.id= ?2")
    Cart findCartByEmailAndCartId(String emailId, Long cartId);
}
