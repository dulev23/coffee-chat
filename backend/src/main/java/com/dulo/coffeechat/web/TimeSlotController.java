package com.dulo.coffeechat.web;

import com.dulo.coffeechat.dto.CreateSlotRequest;
import com.dulo.coffeechat.dto.SlotResponse;
import com.dulo.coffeechat.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class TimeSlotController {

    private final TimeSlotService slotService;

    public TimeSlotController(TimeSlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public List<SlotResponse> available() {
        return slotService.findAvailable().stream().map(SlotResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SlotResponse get(@PathVariable Long id) {
        return SlotResponse.from(slotService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@Valid @RequestBody CreateSlotRequest request) {
        return SlotResponse.from(slotService.create(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        slotService.delete(id);
    }
}
