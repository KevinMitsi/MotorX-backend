package com.sparktech.motorx.entity;

/**
 * Tipos de cita según las reglas de negocio de Jmmotoservicio.
 * Cada tipo tiene sus propios horarios y restricciones de marca.
 */
public enum AppointmentType {

    /**
     * Revisión de garantía de manual - SOLO marca Auteco.
     * Mañana: 7:00 AM | Tarde: 1:00 PM
     */
    MANUAL_WARRANTY_REVIEW,

    /**
     * Garantía Auteco - solo motos Auteco en periodo de garantía.
     * Mañana: 7:30 AM | Tarde: 1:15 PM
     */
    AUTECO_WARRANTY,

    /**
     * Servicio rápido - cualquier marca.
     * Mañana: 7:15 AM | Tarde: 1:30 PM
     */
    QUICK_SERVICE,

    /**
     * Mantenimiento general - cualquier marca.
     * Mañana: 7:45 AM (no tiene recepción en la tarde dentro de horarios de recepción)
     */
    MAINTENANCE,

    /**
     * Cambio de aceite - cualquier marca.
     * Horarios especiales cada 30 minutos durante la jornada.
     * Mañana: 8:00, 8:30, 9:00, 9:30, 10:00
     * Tarde: 2:00, 2:30, 3:00, 3:30, 4:00, 4:30
     */
    OIL_CHANGE,

    /**
     * Cita no planeada - solo el administrador puede crearla.
     * Se llena en espacios donde no hubo cita previa.
     */
    UNPLANNED,

    /**
     * Reproceso - requiere contacto directo (WhatsApp o llamada).
     * El sistema NO permite agendar este tipo directamente.
     */
    REWORK
}