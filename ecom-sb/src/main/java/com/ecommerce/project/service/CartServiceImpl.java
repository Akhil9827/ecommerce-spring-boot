package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    AuthUtil authUtil;


    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();  //Find existing cart or create one

        Product product=productRepository.findById(productId)//Retrieve Product Details
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        CartItem cartItem=cartItemRepository.findCartItemByProductIdAndCartId(
                cart.getCartId(),
                productId);  // Perform validation  whether in the particular cart of a user(checking by giving cart id) that is associated with cartitem in this product is alreday exist or not if exist dont add again just perform operation on quantity

        if(cartItem!=null){
            throw new APIException("Product " + product.getProductName() + "alreday exists");
        }  //Product is already added in the cartItem

        if(product.getQuantity()==0){
            throw new APIException(product.getProductName() + "not available");
        } //In the db product is not available

        if(product.getQuantity()<quantity){
            throw new APIException("Please make an order of the " + product.getProductName() +
                    " less than or equal to the quantity" + product.getQuantity());
        }  //Checking the inventory

        CartItem newCartItem=new CartItem();// Create CartItem

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);   //Save Cartitem

        product.setQuantity((product.getQuantity()));  //we can reduce the stock here in the db after adding to cart doing this (product.getQuantity()-quantity)
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice()*quantity));  //updating the total price of the cart

        cartRepository.save(cart);  //saving the cart after updating the price

        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);  //Return updated cart

        //Inside CartDTO you also have: List<ProductDTO> products;
        //But inside actual entity:
        //Cart {
        //    List<CartItem> cartItems;}  So structure is different. ModelMapper cannot automatically understand: CartItem -> ProductDTO because CartItem contains: Product,Quantity,Cart So You Did Manual Mapping

        List<CartItem> cartItems=cart.getCartItems(); //This gets all cart items from cart.

        Stream<ProductDTO> productStream=cartItems.stream().map(item->{ //You are converting each CartItem into a ProductDTO.
            ProductDTO map=modelMapper.map(item.getProduct(), ProductDTO.class);  //You are converting each CartItem into a ProductDTO.
            map.setQuantity((item.getQuantity()));  //This is the most important line.Because quantity is NOT inside Product.Quantity exists inside: cartitem so for each item we are checking the quantity how many quantity added to the cart
            return map;
        });

        cartDTO.setProducts((productStream.toList()));

        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts=cartRepository.findAll();

        if(carts.size()==0){
            throw new APIException("No cart exist");
        }

        /// First, I retrieve all carts using findAll().
        ///Then I use Java Streams to map each Cart entity into CartDTO using ModelMapper. beause we are returning cartdto in that it has list<ProductDto> & products are in cartitem
        ///Since Cart contains CartItem entities instead of direct products,
        ///I stream through CartItems, extract Product objects, convert them into ProductDTOs,
        ///and attach them to CartDTO before returning the final list.

        List<CartDTO> cartDTOS=carts.stream()
                .map(cart -> {
                    CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
                    List<ProductDTO> products=cart.getCartItems().stream()
                            .map(p-> modelMapper.map(p.getProduct(),ProductDTO.class))
                            .collect(Collectors.toList());
                    cartDTO.setProducts(products);
                    return cartDTO;
                }).collect(Collectors.toList());


        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart=cartRepository.findCartByEmailAndCartId(emailId,cartId);
        if(cart==null){
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }
        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
        cart.getCartItems().forEach(c->c.getProduct().setQuantity(c.getQuantity()));//without this line in the response we will get the total product quantity thats in the db this line will copy the product quantity to cartitem quantity
        List<ProductDTO>products=cart.getCartItems().stream()
                .map(p->modelMapper.map(p.getProduct(), ProductDTO.class))
                .collect(Collectors.toList());
        cartDTO.setProducts(products);
        return cartDTO;
    }

    private Cart createCart() {   // Helper method to create cart
        Cart userCart=cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart!=null){
            return userCart;  //If found return cart
        }

        Cart cart=new Cart();//If cart not found create new cart
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart=cartRepository.save(cart);
        return newCart;
    }
}
