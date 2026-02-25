package com.sparktech.motorx.config;

import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constantes de horarios y reglas de negocio para el agendamiento de citas.
 * Basado en las reglas formales de Jmmotoservicio.
 */
public final class AppointmentScheduleConfig {

    private AppointmentScheduleConfig() {}

    // ---------------------------------------------------------------
    // Horario laboral
    // ---------------------------------------------------------------
    public static final LocalTime WORK_START         = LocalTime.of(7, 0);
    public static final LocalTime WORK_END           = LocalTime.of(17, 30);
    public static final LocalTime LUNCH_START        = LocalTime.of(12, 0);
    public static final LocalTime LUNCH_END          = LocalTime.of(13, 0);

    // ---------------------------------------------------------------
    // Límites de recepción (llegada física de la moto)
    // Una moto con cita en la mañana debe llegar máximo a las 8:00 AM.
    // Una moto con cita en la tarde debe llegar máximo a las 1:50 PM.
    // ---------------------------------------------------------------
    public static final LocalTime MORNING_RECEPTION_DEADLINE   = LocalTime.of(8, 0);
    public static final LocalTime AFTERNOON_RECEPTION_DEADLINE = LocalTime.of(13, 50);

    // ---------------------------------------------------------------
    // Horarios fijos de recepción - MAÑANA
    // ---------------------------------------------------------------
    public static final LocalTime MORNING_MANUAL_WARRANTY_REVIEW = LocalTime.of(7, 0);
    public static final LocalTime MORNING_QUICK_SERVICE           = LocalTime.of(7, 15);
    public static final LocalTime MORNING_AUTECO_WARRANTY         = LocalTime.of(7, 30);
    public static final LocalTime MORNING_MAINTENANCE             = LocalTime.of(7, 45);

    // ---------------------------------------------------------------
    // Horarios fijos de recepción - TARDE
    // ---------------------------------------------------------------
    public static final LocalTime AFTERNOON_MANUAL_WARRANTY_REVIEW = LocalTime.of(13, 0);
    public static final LocalTime AFTERNOON_AUTECO_WARRANTY         = LocalTime.of(13, 15);
    public static final LocalTime AFTERNOON_QUICK_SERVICE           = LocalTime.of(13, 30);
    // MAINTENANCE no tiene recepción en la tarde según las reglas de negocio

    // ---------------------------------------------------------------
    // Horarios especiales - CAMBIO DE ACEITE (30 min c/u)
    // ---------------------------------------------------------------
    public static final List<LocalTime> OIL_CHANGE_MORNING_SLOTS = List.of(
            LocalTime.of(8, 0),
            LocalTime.of(8, 30),
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0)
    );

    public static final List<LocalTime> OIL_CHANGE_AFTERNOON_SLOTS = List.of(
            LocalTime.of(14, 0),
            LocalTime.of(14, 30),
            LocalTime.of(15, 0),
            LocalTime.of(15, 30),
            LocalTime.of(16, 0),
            LocalTime.of(16, 30)
    );

    public static final int OIL_CHANGE_DURATION_MINUTES = 30;

    // ---------------------------------------------------------------
    // Mapa de horarios válidos por tipo de cita (mañana + tarde)
    // Permite consultar los slots disponibles para cada tipo.
    // ---------------------------------------------------------------
    public static final Map<AppointmentType, List<LocalTime>> VALID_SLOTS_BY_TYPE = Map.of(
            AppointmentType.MANUAL_WARRANTY_REVIEW, List.of(
                    MORNING_MANUAL_WARRANTY_REVIEW,
                    AFTERNOON_MANUAL_WARRANTY_REVIEW
            ),
            AppointmentType.AUTECO_WARRANTY, List.of(
                    MORNING_AUTECO_WARRANTY,
                    AFTERNOON_AUTECO_WARRANTY
            ),
            AppointmentType.QUICK_SERVICE, List.of(
                    MORNING_QUICK_SERVICE,
                    AFTERNOON_QUICK_SERVICE
            ),
            AppointmentType.MAINTENANCE, List.of(
                    MORNING_MAINTENANCE
                    // Sin slot en la tarde para mantenimiento
            ),
            AppointmentType.OIL_CHANGE, List.of(
                    // Mañana
                    LocalTime.of(8, 0),
                    LocalTime.of(8, 30),
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 30),
                    LocalTime.of(10, 0),
                    // Tarde
                    LocalTime.of(14, 0),
                    LocalTime.of(14, 30),
                    LocalTime.of(15, 0),
                    LocalTime.of(15, 30),
                    LocalTime.of(16, 0),
                    LocalTime.of(16, 30)
            )
    );

    // ---------------------------------------------------------------
    // Tipos que SOLO aplican para motos marca Auteco
    // ---------------------------------------------------------------
    public static final Set<AppointmentType> AUTECO_ONLY_TYPES = Set.of(
            AppointmentType.MANUAL_WARRANTY_REVIEW,
            AppointmentType.AUTECO_WARRANTY
    );

    // ---------------------------------------------------------------
    // Tipos que el usuario cliente puede agendar directamente.
    // UNPLANNED y REWORK quedan fuera del autoservicio.
    // ---------------------------------------------------------------
    public static final Set<AppointmentType> USER_BOOKABLE_TYPES = Set.of(
            AppointmentType.MANUAL_WARRANTY_REVIEW,
            AppointmentType.AUTECO_WARRANTY,
            AppointmentType.QUICK_SERVICE,
            AppointmentType.MAINTENANCE,
            AppointmentType.OIL_CHANGE
    );
}