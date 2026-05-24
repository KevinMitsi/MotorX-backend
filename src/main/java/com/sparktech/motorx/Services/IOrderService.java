package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.order.AddProcedureToOrderDTO;
import com.sparktech.motorx.dto.order.AddSpareToOrderDTO;
import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.dto.order.UpdateOrderProcedureCostDTO;

public interface IOrderService {
    OrderResponseDTO createOrder(Long appointmentId);

    OrderResponseDTO getOrderByAppointment(Long appointmentId);

    OrderResponseDTO addProcedure(Long orderId, AddProcedureToOrderDTO dto);

    OrderResponseDTO updateProcedureCost(Long orderId, Long procedureId, UpdateOrderProcedureCostDTO dto);

    OrderResponseDTO addSpare(Long orderId, AddSpareToOrderDTO dto);

    OrderResponseDTO completeOrder(Long orderId);

    void sendServiceDetails(Long orderId);

    java.util.List<com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO> getMyTodayOrders();

    java.util.List<com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO> getMyActiveOrders();
}
