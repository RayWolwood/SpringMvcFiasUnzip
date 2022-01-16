package com.fedorov.fias.SpringMvcFiasUnzip.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ArmController {

    @GetMapping("/arm")
    public String arm() {
        return "index";
    }

}
