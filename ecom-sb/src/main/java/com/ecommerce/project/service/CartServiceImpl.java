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
import jakarta.transaction.Transactional;
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



    /// First, I fetch the logged-in user’s cart securely using email authentication.
    ///  Then I validate product availability and inventory stock. After retrieving the corresponding CartItem,
    ///  I update its quantity and cart total price accordingly. If the updated quantity becomes zero,
    ///  the item is removed from the cart. Finally, I map the updated cart and cart items into DTOs before returning the response.

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId=authUtil.loggedInEmail();
        Cart userCart=cartRepository.findCartByEmail(emailId);
        Long cartId=userCart.getCartId();
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));//First it should check a cart exist or not

        Product product=productRepository.findById(productId)//Retrieve Product Details
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        if(product.getQuantity()==0){
            throw new APIException(product.getProductName() + "not available");
        } //In the db product is not available

        if(product.getQuantity()<quantity){
            throw new APIException("Please make an order of the " + product.getProductName() +
                    " less than or equal to the quantity" + product.getQuantity());
        }  //Checking the inventory

        CartItem cartItem=cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);  //Does this particular product already exist inside this particular cart?” if yes returning cartitem object
        if (cartItem == null) {
            throw new APIException("Product" + product.getProductName() + "Not available in the cart");
        }

        //Calculate new quantity
        int newQuantity=cartItem.getQuantity()+quantity;

        //Validation to prevent negative quantities
        if(newQuantity<0){
            throw new APIException("The resulting quantity can not be negetive");
        }

        if(newQuantity==0){
            deleteProductFromCart(cartId,productId);
        }else {

            cartItem.setProductPrice(product.getSpecialPrice());  //Store latest product price inside CartItem.
            cartItem.setQuantity(cartItem.getQuantity() + quantity);  //Increase/decrease cart quantity.
            cartItem.setDiscount(product.getDiscount());  //Update latest discount in cart item.
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));  //Recalculate cart total after quantity update.
            cartRepository.save(cart);  //Needed because:total price changed Without save:DB will not update
        }
        CartItem updatedItem=cartItemRepository.save(cartItem);  //Needed because:quantity changed,price changed,discount changed,Without save:cart item updates not persisted

        if(updatedItem.getQuantity()==0){  //if user reduced quantity to zero,remove item from cart
            cartItemRepository.deleteById(updatedItem.getCartItemId());  //Deletes corresponding row from cart_item table. we are not deleting product we will delete the row in cartitem table
        }

        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
        List<CartItem> cartItems=cart.getCartItems();

        Stream<ProductDTO> productStream=cartItems.stream().map(item ->{
            ProductDTO prd=modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity((item.getQuantity()));//imp line because we are setting the item quantity otherwise it will show the actual product quantity that is available in the db we are directly setting it with productdto next the productdto will be set with cartdto clean architecture
            return prd;
        });

        cartDTO.setProducts(productStream.toList());
        return cartDTO;

    }

    /// The cart’s total price is derived from all products present in the cart.
    /// Before removing a CartItem, I subtract that product’s total contribution from the cart total to maintain consistency.
    /// If I delete the CartItem first, the quantity and price information required for calculation would be lost.

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()->new ResourceNotFoundException("Cart","cartId",cartId));

        CartItem cartItem=cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if(cartItem==null){
            throw new ResourceNotFoundException("Product","productId",productId);
        }

        cart.setTotalPrice(cart.getTotalPrice()-
                (cartItem.getProductPrice()*cartItem.getQuantity())); //we update the cart first because: The cart’s total price depends on the products inside it.So before removing the product,we must subtract that product amount from the cart total. if we directly delete CartItem:then cart total still remains:

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        return "Product " + cartItem.getProduct().getProductName() + "removed from cart";
    }

    /// CartItem stores the product price at the time it was added to the cart.
    ///  When the product price changes, I update the corresponding CartItem price and recalculate the cart total
    ///  by first removing the old contribution and then adding the updated price contribution.
    ///  This ensures that cart totals remain consistent with the latest product pricing.

    @Override
    public void updateProductInCarts(Long cartId,Long productId) {
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));//First it should check a cart exist or not

        Product product=productRepository.findById(productId)//Retrieve Product Details
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        CartItem cartItem=cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);  //Specific CartItem representing:This product inside this cart

        if (cartItem == null) {

            throw new APIException("Product " + product.getProductName() + "not available in the cart");//product not present in cart So cannot update it.

        }

        double cartPrice=cart.getTotalPrice()-(cartItem.getProductPrice()*cartItem.getQuantity());//Very important logic. Remove Old Contribution suppose cart price 120000 & 2 laptop is there with each cost 50000 so total 100000 then later price change so from 120000 we will minus the current price of laptop so cart price will remain 20000 later this price will be added with new price
        cartItem.setProductPrice(product.getSpecialPrice());//Update CartItem Price with new price if price changed later Now CartItem stores updated price.
        cart.setTotalPrice(cartPrice+(cartItem.getProductPrice()*cartItem.getQuantity()));//Add New Contribution this is updated cart calculation after price change

        cartItem=cartItemRepository.save(cartItem);
    }

    private Cart createCart() {   // Helper method to create cart if not present or return present cart
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
