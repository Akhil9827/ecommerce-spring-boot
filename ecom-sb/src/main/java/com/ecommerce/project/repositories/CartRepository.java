package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface CartRepository extends JpaRepository<Cart,Long> {
    @Query("SELECT c FROM Cart c WHERE c.user.email= ?1")  //For the given email, it is returning the corresponding Cart object.
    Cart findCartByEmail(String email);  //Here its nested field cart is associated with user & user having email

    @Query("SELECT c FROM Cart c WHERE c.user.email= ?1 AND c.id= ?2")
    Cart findCartByEmailAndCartId(String emailId, Long cartId);

    /// JOIN FETCH c.cartItems ci->Join Cart with CartItems and fetch them immediately.
    /// Normally relationships may be:LAZY loading.Without FETCH:cart loads first cartItems load later separately This causes:extra SQL queries N+1 problem
    /// With JOIN FETCH Everything loads in single query.
    /// JOIN FETCH ci.product p->From CartItem also fetch Product immediately.

    @Query("SELECT c FROM Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product p WHERE p.id= ?1")
    List<Cart> findCartsByProductId(Long productId);  //this is a custom JPQL query in Spring Data JPA.Find all carts that contain a particular product

    //@Query("DELETE FROM CartItem ci WHERE ci.cart.id= ?1 AND ci.product.id= ?2")
    //void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);
}
