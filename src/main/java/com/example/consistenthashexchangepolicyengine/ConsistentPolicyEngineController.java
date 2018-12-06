/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.consistenthashexchangepolicyengine;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Eleni Fotopoulou <efotopoulou@ubitech.eu>
 */
@RestController
public class ConsistentPolicyEngineController {

//    @Autowired
//    private RabbitTemplate template;
//
//    @Autowired
//    private FanoutExchange fanout;

    @RequestMapping("/hello")
    public String hello() {
        return "Hello from Spring Boot!";
    }

  
}
