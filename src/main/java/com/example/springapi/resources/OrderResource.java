package com.example.springapi.resources;
import com.example.springapi.dtos.OrderDTO;
import com.example.springapi.entities.*;
import com.example.springapi.entities.enums.OrderStatus;
import com.example.springapi.services.OrderItemService;
import com.example.springapi.services.OrderService;
import com.example.springapi.services.ProductService;
import com.example.springapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping(value = "/api")
public class OrderResource {

    @Autowired
    private OrderService service;

    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderService orderService;


    @GetMapping("/orders")
    public ResponseEntity<List<Order>> findAll() {
        List<Order> list = service.findAll();
                return ResponseEntity.ok().body(list);
    }

    @GetMapping(value = "/orders/{id}")
    public ResponseEntity<Order> findById(@PathVariable Long id) {
        Order obj = service.findById(id);
        return ResponseEntity.ok().body(obj);
    }

    @PostMapping(value = "/orders/save")
    public ResponseEntity<Object> insert(@RequestBody @Valid OrderDTO orderDTO){
        Optional<User> clientId = Optional.ofNullable(userService.findById(orderDTO.getClientId()));
        if (clientId.isPresent()){
            var client = userService.findById(orderDTO.getClientId());
            var order = new Order(null, orderDTO.getMoment(), orderDTO.getOrderStatus(), client);
            order = service.save(order);

            for (OrderItem orderItem : orderDTO.getOrderItems()){
                orderItem.setOrder(order);
                orderItemService.save(orderItem);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
    }

    @PostMapping(value = "/orders/save-cascade")
    public ResponseEntity<Object> insertCascade(@RequestBody @Valid OrderDTO orderDTO){
        Optional<User> clientId = Optional.ofNullable(userService.findById(orderDTO.getClient().getId()));
        if (clientId.isPresent()){

            Order order = new Order();
            order.setMoment(orderDTO.getMoment());
            order.setClient(orderDTO.getClient());
            order.setOrderStatus(orderDTO.getOrderStatus() != null ? orderDTO.getOrderStatus() : OrderStatus.WAITING_PAYMENT);
            order.setPayment(order.getOrderStatus() != null ? new Payment(null, orderDTO.getMoment(), order) : null);

            Set<OrderItem> orderItems = new HashSet<>(orderDTO.getOrderItems());

            for (OrderItem orderItem : orderItems){
                orderItem.setOrder(order);
            }

            order.setItems(orderItems);
            order = service.save(order);


            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
    }

    @PutMapping(value = "/orders/{id}")
    public ResponseEntity<Object> update(@PathVariable(value = "id") Long id, @RequestBody @Valid OrderDTO orderDTO){
        Optional<Order> orderOptional = Optional.ofNullable(service.findById(id));

        if (orderOptional.isPresent()){
            Order order = orderOptional.get();
            order.setOrderStatus(orderDTO.getOrderStatus());
            if (order.getOrderStatus() == 2){
                if (order.getPayment() == null){
                    order.setPayment(new Payment(null, orderDTO.getMoment(), order));
                }
            }

            order = service.save(order);

            return ResponseEntity.status(HttpStatus.OK).body(order);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
    }
}
