package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.reception.ConfirmReceptionDTO;

public interface IReceptionService {
    AppointmentResponseDTO initiateReception(Long appointmentId);

    AppointmentResponseDTO confirmReception(ConfirmReceptionDTO dto);
}

