package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.ItemController;
import com.example.demo.controllers.OrderController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class SareetaApplicationTests {

	@InjectMocks
	private CartController cartController;

	@InjectMocks
	private ItemController itemController;

	@InjectMocks
	private OrderController orderController;

	@InjectMocks
	private UserController userController;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private CartRepository cartRepository;

	@Mock
	private ItemRepository itemRepository;

	@Mock
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	private User user;
	private Item item;
	private UserOrder order;
	private CreateUserRequest validRequest;
	private CreateUserRequest invalidRequest;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		user = new User();
		user.setId(1L);
		user.setUsername("testUser");

		item = new Item();
		item.setId(1L);
		item.setName("Test Item");
		item.setPrice(BigDecimal.valueOf(10.00));
		item.setDescription("Sample Item");

		Cart cart = new Cart();
		cart.setId(1L);
		cart.setUser(user);
		cart.setItems(new ArrayList<>(List.of(item, item)));
		cart.setTotal(item.getPrice().multiply(BigDecimal.valueOf(2)));
		user.setCart(cart);

		order = UserOrder.createFromCart(cart);
		order.setUser(user);

		validRequest = new CreateUserRequest();
		validRequest.setUsername("testuser");
		validRequest.setPassword("password123");
		validRequest.setConfirmPassword("password123");

		invalidRequest = new CreateUserRequest();
		invalidRequest.setUsername("baduser");
		invalidRequest.setPassword("short");
		invalidRequest.setConfirmPassword("short");

		when(userRepository.findByUsername("testUser")).thenReturn(user);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
	}

	@Test
	public void contextLoads() {
	}

	public void testCreateUserSuccess() {
		when(bCryptPasswordEncoder.encode("password123")).thenReturn("hashedPwd");

		ResponseEntity<User> response = userController.createUser(validRequest);
		assertNotNull(response);
		assertEquals(200, response.getStatusCodeValue());

		User user = response.getBody();
		assertNotNull(user);
		assertEquals("testuser", user.getUsername());
		assertEquals("hashedPwd", user.getPassword());
		verify(cartRepository, times(1)).save(any(Cart.class));
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	public void testCreateUserWithInvalidPassword() {
		ResponseEntity<User> response = userController.createUser(invalidRequest);
		assertEquals(400, response.getStatusCodeValue());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	public void testFindByIdSuccess() {
		User user = new User();
		user.setId(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		ResponseEntity<User> response = userController.findById(1L);
		assertNotNull(response.getBody());
		assertEquals(1L, response.getBody().getId());
	}

	@Test
	public void testFindByIdNotFound() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		ResponseEntity<User> response = userController.findById(1L);
		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testFindByUsernameSuccess() {
		User user = new User();
		user.setUsername("existingUser");
		when(userRepository.findByUsername("existingUser")).thenReturn(user);

		ResponseEntity<User> response = userController.findByUserName("existingUser");
		assertNotNull(response.getBody());
		assertEquals("existingUser", response.getBody().getUsername());
	}

	@Test
	public void testFindByUsernameNotFound() {
		when(userRepository.findByUsername("ghost")).thenReturn(null);

		ResponseEntity<User> response = userController.findByUserName("ghost");
		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testAddToCart_happyPath() {
		Cart cart=new Cart();
		user.setCart(cart);
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("testUser");
		request.setItemId(1L);
		request.setQuantity(2);

		ResponseEntity<Cart> response = cartController.addTocart(request);
		assertEquals(200, response.getStatusCodeValue());
		assertEquals(2, response.getBody().getItems().size());
		verify(cartRepository, times(1)).save(any());
	}

	@Test
	public void testAddToCart_userNotFound() {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("invalidUser");
		request.setItemId(1L);
		request.setQuantity(2);

		when(userRepository.findByUsername("invalidUser")).thenReturn(null);

		ResponseEntity<Cart> response = cartController.addTocart(request);
		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testAddToCart_itemNotFound() {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("testUser");
		request.setItemId(2L); // nonexistent
		request.setQuantity(1);

		when(itemRepository.findById(2L)).thenReturn(Optional.empty());

		ResponseEntity<Cart> response = cartController.addTocart(request);
		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testRemoveFromCart_happyPath() {
		Cart cart=new Cart();
		user.setCart(cart);
		user.getCart().addItem(item);
		user.getCart().addItem(item);

		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("testUser");
		request.setItemId(1L);
		request.setQuantity(1);

		ResponseEntity<Cart> response = cartController.removeFromcart(request);
		assertEquals(200, response.getStatusCodeValue());
		assertEquals(1, response.getBody().getItems().size());
		verify(cartRepository, times(1)).save(any());
	}

	@Test
	public void testRemoveFromCart_userNotFound() {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("invalidUser");
		request.setItemId(1L);
		request.setQuantity(1);

		when(userRepository.findByUsername("invalidUser")).thenReturn(null);

		ResponseEntity<Cart> response = cartController.removeFromcart(request);
		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testRemoveFromCart_itemNotFound() {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("testUser");
		request.setItemId(999L); // nonexistent
		request.setQuantity(1);

		when(itemRepository.findById(999L)).thenReturn(Optional.empty());

		ResponseEntity<Cart> response = cartController.removeFromcart(request);
		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testRemoveFromCart_quantityExceeds() {
		user.getCart().addItem(item); // Only 1 item

		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("testUser");
		request.setItemId(1L);
		request.setQuantity(5); // Try removing more

		ResponseEntity<Cart> response = cartController.removeFromcart(request);
		assertEquals(200, response.getStatusCodeValue());
		assertTrue(response.getBody().getItems().isEmpty());
	}

	@Test
	public void testGetItems() {
		Item item1 = new Item(); item1.setId(1L);
		Item item2 = new Item(); item2.setId(2L);
		when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

		ResponseEntity<List<Item>> response = itemController.getItems();

		assertEquals(200, response.getStatusCodeValue());
		assertEquals(2, response.getBody().size());
	}

	@Test
	public void testGetItemById_found() {
		Item item = new Item(); item.setId(1L);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

		ResponseEntity<Item> response = itemController.getItemById(1L);

		assertEquals(200, response.getStatusCodeValue());
		assertNotNull(response.getBody());
		assertEquals(1L, response.getBody().getId());
	}

	@Test
	public void testGetItemById_notFound() {
		when(itemRepository.findById(1L)).thenReturn(Optional.empty());

		ResponseEntity<Item> response = itemController.getItemById(1L);

		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testGetItemsByName_found() {
		Item item = new Item(); item.setName("Test");
		when(itemRepository.findByName("Test")).thenReturn(List.of(item));

		ResponseEntity<List<Item>> response = itemController.getItemsByName("Test");

		assertEquals(200, response.getStatusCodeValue());
		assertEquals(1, response.getBody().size());
	}

	@Test
	public void testGetItemsByName_notFound() {
		when(itemRepository.findByName("Unknown")).thenReturn(Collections.emptyList());

		ResponseEntity<List<Item>> response = itemController.getItemsByName("Unknown");

		assertEquals(404, response.getStatusCodeValue());
	}

	@Test
	public void testSubmitOrder_UserExists() {
		when(userRepository.findByUsername("testuser")).thenReturn(user);
		when(orderRepository.save(any(UserOrder.class))).thenReturn(order);

		ResponseEntity<UserOrder> response = orderController.submit("testuser");

		assertEquals(200, response.getStatusCodeValue());
		assertEquals(user, response.getBody().getUser());
		verify(orderRepository, times(1)).save(any(UserOrder.class));
	}

	@Test
	public void testSubmitOrder_UserNotFound() {
		when(userRepository.findByUsername("nonexistent")).thenReturn(null);

		ResponseEntity<UserOrder> response = orderController.submit("nonexistent");

		assertEquals(404, response.getStatusCodeValue());
		verify(orderRepository, never()).save(any());
	}

	@Test
	public void testGetOrdersForUser_UserExists() {
		List<UserOrder> orders = Collections.singletonList(order);
		when(userRepository.findByUsername("testuser")).thenReturn(user);
		when(orderRepository.findByUser(user)).thenReturn(orders);

		ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser("testuser");

		assertEquals(200, response.getStatusCodeValue());
		assertEquals(1, response.getBody().size());
	}

	@Test
	public void testGetOrdersForUser_UserNotFound() {
		when(userRepository.findByUsername("ghost")).thenReturn(null);

		ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser("ghost");

		assertEquals(404, response.getStatusCodeValue());
	}
}
