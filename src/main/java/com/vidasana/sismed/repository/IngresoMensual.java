package com.vidasana.sismed.repository;

import java.math.BigDecimal;

public interface IngresoMensual {

    Integer getAnio();

    Integer getMes();

    BigDecimal getTotal();
}
